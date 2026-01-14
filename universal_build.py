#!/usr/bin/env python3
"""
UNIVERSAL BUILD SYSTEM (MegaXPStorage)

Modeled after your UniversalMobWar script, but tailored for this mod.

Usage:
  ./universal_build.py                    # Validation only
  ./universal_build.py --check            # Validation only
  ./universal_build.py --build            # Validation + Build (no clean)
  ./universal_build.py --build --clean    # Validation + Clean + Build
    ./universal_build.py --run              # Validation + runClient (no clean)
    ./universal_build.py --deploy           # Validation + Build + git add/commit/push
    ./universal_build.py --full             # Validation + Clean + Build + git add/commit/push
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path


class Color:
    RED = "\033[91m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    MAGENTA = "\033[95m"
    CYAN = "\033[96m"
    WHITE = "\033[97m"
    RESET = "\033[0m"
    BOLD = "\033[1m"


def log(msg: str, color: str = Color.WHITE) -> None:
    print(f"{color}{msg}{Color.RESET}")


def header(msg: str) -> None:
    print()
    log("=" * 80, Color.BLUE)
    log(msg.center(80), Color.BOLD + Color.CYAN)
    log("=" * 80, Color.BLUE)


def success(msg: str) -> None:
    log(f"OK  {msg}", Color.GREEN)


def error(msg: str) -> None:
    log(f"ERR {msg}", Color.RED)


def warning(msg: str) -> None:
    log(f"WARN {msg}", Color.YELLOW)


def info(msg: str) -> None:
    log(f"INFO {msg}", Color.CYAN)


@dataclass
class BuildResult:
    ok: bool
    latest_jar: Path | None = None


class UniversalBuildSystem:
    def __init__(self) -> None:
        self.root = Path(__file__).parent.resolve()
        self.errors: list[str] = []
        self.warnings: list[str] = []
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.log_file = self.root / "universal_build.log"

    def log_to_file(self, message: str) -> None:
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(message + "\n")

    def validate_all(self) -> bool:
        header("VALIDATION SUITE")
        self.log_to_file("\n" + "=" * 80)
        self.log_to_file("VALIDATION SUITE")
        self.log_to_file("=" * 80)

        self.validate_gradle_properties()
        self.validate_fabric_metadata()
        self.validate_java_syntax()
        self.validate_mixins()

        return len(self.errors) == 0

    def validate_gradle_properties(self) -> None:
        info("Validating gradle.properties...")

        gradle_props = self.root / "gradle.properties"
        if not gradle_props.exists():
            self.errors.append("Missing gradle.properties")
            error("gradle.properties not found")
            return

        content = gradle_props.read_text(encoding="utf-8", errors="ignore")

        if "minecraft_version=1.21.1" in content:
            success("Minecraft version: 1.21.1")
        else:
            self.errors.append("Minecraft version mismatch (expected 1.21.1)")
            error("Minecraft version mismatch (expected 1.21.1)")

        if "loader_version=" in content:
            success("Fabric loader version present")
        else:
            self.warnings.append("No loader_version= in gradle.properties")
            warning("No loader_version= in gradle.properties")

    def validate_fabric_metadata(self) -> None:
        info("Validating fabric.mod.json...")

        mod_json = self.root / "src/main/resources/fabric.mod.json"
        if not mod_json.exists():
            self.errors.append("Missing src/main/resources/fabric.mod.json")
            error("fabric.mod.json not found")
            return

        content = mod_json.read_text(encoding="utf-8", errors="ignore")

        if '"id": "mega-xp-storage"' in content:
            success("Mod id: mega-xp-storage")
        else:
            self.errors.append("fabric.mod.json id is not mega-xp-storage")
            error("fabric.mod.json id is not mega-xp-storage")

        if '"minecraft": "~1.21.1"' in content:
            success("Minecraft dependency: ~1.21.1")
        else:
            self.warnings.append("fabric.mod.json minecraft dependency is not ~1.21.1")
            warning("fabric.mod.json minecraft dependency is not ~1.21.1")

    def validate_java_syntax(self) -> None:
        info("Scanning Java sources for common 1.21.1 API issues...")

        java_files = list((self.root / "src").glob("**/*.java"))
        old_identifier = 0

        for path in java_files:
            content = path.read_text(encoding="utf-8", errors="ignore")
            if "new Identifier(" in content:
                old_identifier += 1
                self.errors.append(f"Old Identifier constructor used in {path}")

        if old_identifier == 0:
            success("No old Identifier constructor usage")
        else:
            error(f"Found {old_identifier} old Identifier constructor usages")

    def validate_mixins(self) -> None:
        info("Validating mixins config and classes...")

        mixin_json = self.root / "src/main/resources/mega-xp-storage.mixins.json"
        if not mixin_json.exists():
            self.errors.append("Missing mega-xp-storage.mixins.json")
            error("Missing mega-xp-storage.mixins.json")
            return

        content = mixin_json.read_text(encoding="utf-8", errors="ignore")
        if '"package": "com.carte.megaxpstorage.mixin"' in content:
            success("Mixins package correct")
        else:
            self.errors.append("Mixins package mismatch")
            error("Mixins package mismatch")

        mixin_class = self.root / "src/main/java/com/carte/megaxpstorage/mixin/PlayerEntityMixin.java"
        if mixin_class.exists():
            success("PlayerEntityMixin present")
        else:
            self.errors.append("Missing PlayerEntityMixin.java")
            error("Missing PlayerEntityMixin.java")

    def build_project(self, clean: bool) -> BuildResult:
        header("GRADLE BUILD")
        self.log_to_file("\n" + "=" * 80)
        self.log_to_file("GRADLE BUILD")
        self.log_to_file("=" * 80)

        gradlew = self.root / ("gradlew.bat" if os.name == "nt" else "gradlew")
        if not gradlew.exists():
            error("Gradle wrapper not found (gradlew/gradlew.bat)")
            return BuildResult(False)

        # Project-local Gradle home reduces Windows file lock issues during iteration.
        gradle_user_home = self.root / ".gradle-user-home"
        gradle_user_home.mkdir(exist_ok=True)
        env = os.environ.copy()
        env["GRADLE_USER_HOME"] = str(gradle_user_home)

        if clean:
            info("Running gradle clean...")
            r = subprocess.run([str(gradlew), "clean", "--no-daemon"], cwd=self.root, text=True, env=env)
            if r.returncode != 0:
                warning("Clean failed; continuing with build")

        info("Running gradle build...")
        p = subprocess.run([str(gradlew), "build", "--no-daemon"], cwd=self.root, text=True, env=env)
        if p.returncode != 0:
            error("Build failed")
            return BuildResult(False)

        libs = self.root / "build/libs"
        jars = sorted(
            [j for j in libs.glob("*.jar") if "-sources" not in j.name and "-dev" not in j.name],
            key=lambda x: x.stat().st_mtime,
            reverse=True,
        )

        latest = jars[0] if jars else None
        if latest is not None:
            size_mb = latest.stat().st_size / (1024 * 1024)
            success(f"Build OK; jar: {latest.name} ({size_mb:.2f} MB)")
        else:
            warning("Build OK but no jar found in build/libs")

        return BuildResult(True, latest)

    def git_commit_and_push(self, message: str, branch: str = "main") -> bool:
        header("GIT COMMIT & PUSH")

        git_dir = self.root / ".git"
        if not git_dir.exists():
            error("No .git directory found; cannot deploy")
            self.errors.append("Not a git repository")
            return False

        try:
            # Keep this flow intentionally simple to match exactly:
            # git add . && git commit -m "<message>" && git push -u origin main
            info("Running: git add .")
            subprocess.run(["git", "add", "."], cwd=self.root, check=True)

            info(f"Running: git commit -m \"{message}\"")
            commit = subprocess.run(
                ["git", "commit", "-m", message],
                cwd=self.root,
                text=True,
            )
            if commit.returncode != 0:
                # Most common: nothing to commit.
                warning("git commit failed (likely nothing to commit)")

            info(f"Running: git push -u origin {branch}")
            push = subprocess.run(["git", "push", "-u", "origin", branch], cwd=self.root, text=True)
            if push.returncode != 0:
                error("git push failed (check remote/auth/branch)")
                self.errors.append("git push failed")
                return False

            success("Push successful")
            return True
        except subprocess.CalledProcessError as exc:
            error(f"Git command failed: {exc}")
            self.errors.append("Git command failed")
            return False

    def run_client(self) -> bool:
        header("RUN CLIENT")

        gradlew = self.root / ("gradlew.bat" if os.name == "nt" else "gradlew")
        if not gradlew.exists():
            error("Gradle wrapper not found (gradlew/gradlew.bat)")
            return False

        # Dedicated game dir for testing so you don't pollute your normal instance.
        instance_dir = Path.home() / ".megaxpstorage-test-instance"
        instance_dir.mkdir(parents=True, exist_ok=True)

        info(f"Using gameDir: {instance_dir}")
        args_value = f"--gameDir \"{instance_dir}\" --username MegaXPTester"

        gradle_user_home = self.root / ".gradle-user-home"
        gradle_user_home.mkdir(exist_ok=True)
        env = os.environ.copy()
        env["GRADLE_USER_HOME"] = str(gradle_user_home)

        r = subprocess.run([str(gradlew), "runClient", f"--args={args_value}"], cwd=self.root, env=env)
        return r.returncode == 0

    def generate_report(self) -> None:
        header("BUILD REPORT")

        if not self.errors and not self.warnings:
            success("All checks passed")
        elif self.errors:
            error(f"{len(self.errors)} error(s) found")
            for e in self.errors:
                log(f"  - {e}", Color.RED)
        if self.warnings:
            warning(f"{len(self.warnings)} warning(s)")
            for w in self.warnings:
                log(f"  - {w}", Color.YELLOW)

        success(f"Log file: {self.log_file.name}")


def main() -> int:
    parser = argparse.ArgumentParser(description="MegaXPStorage - Universal Build System")
    parser.add_argument("--check", action="store_true", help="Validation only")
    parser.add_argument("--build", action="store_true", help="Validation + Build")
    parser.add_argument("--run", action="store_true", help="Validation + runClient")
    parser.add_argument("--deploy", action="store_true", help="Validation + Build + git add/commit/push")
    parser.add_argument("--full", action="store_true", help="Validation + Clean + Build + git add/commit/push")
    parser.add_argument("--message", default="fix", help="Git commit message (used with --deploy/--full)")

    clean_group = parser.add_mutually_exclusive_group()
    clean_group.add_argument("--clean", action="store_true", help="Run 'gradle clean' before building")
    clean_group.add_argument("--no-clean", action="store_true", help="Skip 'gradle clean'")

    args = parser.parse_args()

    # Default behavior: validation only
    if not (args.check or args.build or args.run or args.deploy or args.full):
        args.check = True

    builder = UniversalBuildSystem()

    if not builder.validate_all():
        builder.generate_report()
        return 1

    if args.build or args.deploy or args.full:
        if args.full:
            clean = True
        else:
            clean = bool(args.clean) if (args.clean or args.no_clean) else False
        res = builder.build_project(clean=clean)
        if not res.ok:
            builder.generate_report()
            return 1

    if args.run:
        if not builder.run_client():
            builder.generate_report()
            return 1

    if args.deploy or args.full:
        if not builder.git_commit_and_push(message=args.message, branch="main"):
            builder.generate_report()
            return 1

    builder.generate_report()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
