package com.carte.megaxpstorage.mixin;

import com.carte.megaxpstorage.MegaXpStorageMod;
import com.carte.megaxpstorage.item.MegaXpStorageItem;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
	@Unique
	private int megaXpStorage$levelsBeforeAddExperience;

	@Inject(method = "addExperience", at = @At("HEAD"))
	private void megaXpStorage$captureLevelBefore(int experience, CallbackInfo ci) {
		this.megaXpStorage$levelsBeforeAddExperience = ((PlayerEntity) (Object) this).experienceLevel;
	}

	@Inject(method = "addExperience", at = @At("TAIL"))
	private void megaXpStorage$afterAddExperience(int experience, CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		if (player.getWorld().isClient) {
			return;
		}

		int gainedLevels = player.experienceLevel - this.megaXpStorage$levelsBeforeAddExperience;
		if (gainedLevels <= 0) {
			return;
		}

		RegistryEntry<Enchantment> mending = player.getWorld().getRegistryManager()
				.getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
				.getOrThrow(Enchantments.MENDING);

		ItemStack target = null;
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (!stack.isOf(MegaXpStorageMod.MEGA_XP_STORAGE)) {
				continue;
			}

			if (stack.getEnchantments().getLevel(mending) <= 0) {
				continue;
			}

			target = stack;
			break;
		}

		if (target == null) {
			return;
		}

		long stored = MegaXpStorageItem.getStoredLevels(target);
		MegaXpStorageItem.setStoredLevels(target, stored + (long) gainedLevels);
		player.addExperienceLevels(-gainedLevels);
	}
}