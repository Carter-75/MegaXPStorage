package com.carte.megaxpstorage.recipe;

import com.carte.megaxpstorage.MegaXpStorageMod;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

public class MegaXpStorageMendingUpgradeRecipe extends SpecialCraftingRecipe {
	public static final RecipeSerializer<MegaXpStorageMendingUpgradeRecipe> SERIALIZER = new SpecialRecipeSerializer<>(MegaXpStorageMendingUpgradeRecipe::new);

	public MegaXpStorageMendingUpgradeRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() != 3 || input.getHeight() != 3) {
			return false;
		}

		ItemStack center = input.getStackInSlot(4);
		if (!center.isOf(MegaXpStorageMod.MEGA_XP_STORAGE)) {
			return false;
		}

		RegistryEntry<Enchantment> mending = world.getRegistryManager()
				.getWrapperOrThrow(RegistryKeys.ENCHANTMENT)
				.getOrThrow(Enchantments.MENDING);

		for (int slot = 0; slot < 9; slot++) {
			if (slot == 4) {
				continue;
			}

			ItemStack stack = input.getStackInSlot(slot);
			if (!stack.isOf(Items.ENCHANTED_BOOK)) {
				return false;
			}

			ItemEnchantmentsComponent stored = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
			if (stored == null || stored.getLevel(mending) <= 0) {
				return false;
			}
		}

		return true;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		ItemStack out = input.getStackInSlot(4).copy();
		RegistryEntry<Enchantment> mending = registries.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
		out.addEnchantment(mending, 1);
		return out;
	}

	@Override
	public boolean fits(int width, int height) {
		return width == 3 && height == 3;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
