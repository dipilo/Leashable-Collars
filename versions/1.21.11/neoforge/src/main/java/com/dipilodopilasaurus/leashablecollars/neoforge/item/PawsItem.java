package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PawsItem extends FootPawsItem {
    public PawsItem(Properties properties, int defaultColor) {
        super(properties, defaultColor);
    }

    public static boolean shouldPreventBlockInteraction(ItemStack stack, BlockState block) {
        if (block.is(LeashableCollarsNeoForge.PAWS_ALLOW_INTERACT)) {
            return false;
        }

        List<String> allowedBlocks = stack.get(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get());
        if (allowedBlocks == null) {
            return false;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
        return !allowedBlocks.contains(blockId);
    }

    public static boolean isSlippery(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    public static boolean shouldDrop(ItemStack pawsStack, ItemStack heldStack) {
        return isSlippery(pawsStack) && !heldStack.isEmpty();
    }

    public static void copyConfiguration(ItemStack source, ItemStack target) {
        List<String> allowedBlocks = source.get(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get());
        if (allowedBlocks == null) {
            target.remove(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get());
        } else {
            target.set(LeashableCollarsNeoForge.PAWS_ALLOWED_BLOCKS.get(), List.copyOf(allowedBlocks));
        }

        if (Boolean.TRUE.equals(source.get(LeashableCollarsNeoForge.PAWS_STRICT_MODE.get()))) {
            target.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            target.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isCrouching()) {
            return super.use(level, player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (isSlippery(stack)) {
            return super.use(level, player, hand);
        }

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!player.getItemInHand(otherHand).is(Items.HONEY_BOTTLE)) {
            return super.use(level, player, hand);
        }

        if (level.isClientSide()) {
            player.playSound(SoundEvents.HONEY_BLOCK_PLACE, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(otherHand, new ItemStack(Items.GLASS_BOTTLE));
        }
        return InteractionResult.SUCCESS;
    }
}