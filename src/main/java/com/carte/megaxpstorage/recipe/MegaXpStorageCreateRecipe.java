package com.carte.megaxpstorage.recipe;

import com.carte.megaxpstorage.MegaXpStorageMod;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class MegaXpStorageCreateRecipe extends SpecialCraftingRecipe {
	public static final RecipeSerializer<MegaXpStorageCreateRecipe> SERIALIZER = new SpecialRecipeSerializer<>(MegaXpStorageCreateRecipe::new);

	public MegaXpStorageCreateRecipe(CraftingRecipeCategory category) {
		super(category);
	}

	@Override
	public boolean matches(CraftingRecipeInput input, World world) {
		if (input.getWidth() != 3 || input.getHeight() != 3) {
			return false;
		}

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				ItemStack stack = input.getStackInSlot(x + y * 3);
				boolean isCenter = x == 1 && y == 1;

				if (isCenter) {
					if (!stack.isOf(Items.LAPIS_LAZULI) || stack.getCount() != 64) {
						return false;
					}
					continue;
				}

				if (!stack.isOf(Items.BOOK) || stack.getCount() != 64) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
		return new ItemStack(MegaXpStorageMod.MEGA_XP_STORAGE);
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
