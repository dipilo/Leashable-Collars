package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.PawConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PawsItem extends FootPawsItem {
    private static final String PAWS_TAG = "playercollars_paws";
    private static final String HELD_ITEMS_TAG = "held_items";
    private static final String INTERACTION_TAG = "restrict_interaction";

    public PawsItem(int defaultColor, int defaultBeansColor) {
        super(defaultColor, defaultBeansColor);
    }

    public List<PawConfigEntry> getHeldItemsConfig(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(PAWS_TAG);
        return tag == null ? List.of() : PawConfigEntry.readListTag(tag, HELD_ITEMS_TAG);
    }

    public void setHeldItemsConfig(ItemStack stack, @Nullable List<PawConfigEntry> entries) {
        CompoundTag tag = stack.getOrCreateTagElement(PAWS_TAG);
        PawConfigEntry.writeListTag(tag, HELD_ITEMS_TAG, entries);
    }

    public List<PawConfigEntry> getCanInteractConfig(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(PAWS_TAG);
        return tag == null ? List.of() : PawConfigEntry.readListTag(tag, INTERACTION_TAG);
    }

    public void setCanInteractConfig(ItemStack stack, @Nullable List<PawConfigEntry> entries) {
        CompoundTag tag = stack.getOrCreateTagElement(PAWS_TAG);
        PawConfigEntry.writeListTag(tag, INTERACTION_TAG, entries);
    }

    public static boolean shouldDrop(ItemStack pawsStack, ItemStack heldStack) {
        if (heldStack.isEmpty() || !(pawsStack.getItem() instanceof PawsItem pawsItem)) {
            return false;
        }
        List<PawConfigEntry> config = pawsItem.getHeldItemsConfig(pawsStack);
        if (config.isEmpty()) {
            return false;
        }
        for (PawConfigEntry entry : config) {
            if (entry.matchesItem(heldStack)) {
                return false;
            }
        }
        return true;
    }

    public static boolean shouldPreventBlockInteraction(ItemStack pawsStack, BlockState blockState) {
        if (!(pawsStack.getItem() instanceof PawsItem pawsItem)) {
            return false;
        }
        if (blockState.is(com.dipilodopilasaurus.leashablecollars.LeashableCollars.PAWS_ALLOW_INTERACT)) {
            return false;
        }
        List<PawConfigEntry> config = pawsItem.getCanInteractConfig(pawsStack);
        if (config.isEmpty()) {
            return false;
        }
        for (PawConfigEntry entry : config) {
            if (entry.matchesBlock(blockState)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!getHeldItemsConfig(stack).isEmpty()) {
            tooltip.add(Component.translatable("item.playercollars.paws.slippery").withStyle(ChatFormatting.GRAY));
        }
        if (!getCanInteractConfig(stack).isEmpty()) {
            tooltip.add(Component.translatable("item.playercollars.paws.interaction").withStyle(ChatFormatting.GRAY));
        }
    }
}
