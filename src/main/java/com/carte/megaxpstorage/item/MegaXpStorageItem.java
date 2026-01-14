package com.carte.megaxpstorage.item;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MegaXpStorageItem extends Item {
	private static final String STORED_LEVELS_KEY = "StoredLevels";

	public MegaXpStorageItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient) {
			return TypedActionResult.success(stack);
		}

		if (user.isSneaking()) {
			long storedLevels = getStoredLevels(stack);
			if (storedLevels <= 0) {
				return TypedActionResult.pass(stack);
			}

			long transferable = Math.min(storedLevels, (long) Integer.MAX_VALUE - (long) user.experienceLevel);
			if (transferable <= 0) {
				return TypedActionResult.pass(stack);
			}

			addLevels(user, transferable);
			setStoredLevels(stack, storedLevels - transferable);
			return TypedActionResult.success(stack);
		}

		int playerLevels = user.experienceLevel;
		if (playerLevels <= 0) {
			return TypedActionResult.pass(stack);
		}

		long storedLevels = getStoredLevels(stack);
		setStoredLevels(stack, storedLevels + (long) playerLevels);
		user.addExperienceLevels(-playerLevels);
		return TypedActionResult.success(stack);
	}

	@Override
	public boolean hasGlint(ItemStack stack) {
		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
	}

	@Override
	public int getEnchantability() {
		return 1;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		long storedLevels = getStoredLevels(stack);
		tooltip.add(Text.translatable("item.mega-xp-storage.mega_xp_storage.stored_levels", storedLevels).formatted(Formatting.GRAY));
	}

	public static long getStoredLevels(ItemStack stack) {
		NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return 0L;
		}
		NbtCompound nbt = customData.getNbt();
		return nbt.getLong(STORED_LEVELS_KEY);
	}

	public static void setStoredLevels(ItemStack stack, long levels) {
		if (levels <= 0) {
			NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(STORED_LEVELS_KEY));
			return;
		}

		NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putLong(STORED_LEVELS_KEY, levels));
	}

	private static void addLevels(PlayerEntity player, long levels) {
		long remaining = levels;
		while (remaining > 0) {
			int chunk = (int) Math.min(remaining, 1_000_000L);
			player.addExperienceLevels(chunk);
			remaining -= chunk;
		}
	}
}
