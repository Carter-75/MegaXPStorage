package com.carte.megaxpstorage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carte.megaxpstorage.item.MegaXpStorageItem;
import com.carte.megaxpstorage.recipe.MegaXpStorageCreateRecipe;
import com.carte.megaxpstorage.recipe.MegaXpStorageMendingUpgradeRecipe;

public class MegaXpStorageMod implements ModInitializer {
	public static final String MOD_ID = "mega-xp-storage";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item MEGA_XP_STORAGE = Registry.register(
			Registries.ITEM,
			id("mega_xp_storage"),
			new MegaXpStorageItem(new Item.Settings().maxCount(1))
	);

	@Override
	public void onInitialize() {
		Registry.register(Registries.RECIPE_SERIALIZER, id("crafting_special_mega_xp_storage"), MegaXpStorageCreateRecipe.SERIALIZER);
		Registry.register(Registries.RECIPE_SERIALIZER, id("crafting_special_mega_xp_storage_mending"), MegaXpStorageMendingUpgradeRecipe.SERIALIZER);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MEGA_XP_STORAGE));
		LOGGER.info("Mega XP Storage loaded");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}