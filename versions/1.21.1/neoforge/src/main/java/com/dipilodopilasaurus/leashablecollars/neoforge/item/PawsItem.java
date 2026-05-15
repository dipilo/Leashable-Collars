package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.PawConfigEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import java.util.List;

public class PawsItem extends FootPawsItem {
    public PawsItem(Properties properties, int defaultColor) {
        super(properties, defaultColor);
    }

    public List<PawConfigEntry> getHeldItemsConfig(ItemStack stack) {
        return PawConfigEntry.fromStored(stack.get(LeashableCollarsNeoForge.PAWS_HELD_ITEMS_CONFIG.get()));
    }

    public void setHeldItemsConfig(ItemStack stack, @Nullable List<PawConfigEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            stack.remove(LeashableCollarsNeoForge.PAWS_HELD_ITEMS_CONFIG.get());
            return;
        }
        stack.set(LeashableCollarsNeoForge.PAWS_HELD_ITEMS_CONFIG.get(), PawConfigEntry.toStored(entries));
    }

    public List<PawConfigEntry> getCanInteractConfig(ItemStack stack) {
        return PawConfigEntry.fromStored(stack.get(LeashableCollarsNeoForge.PAWS_INTERACT_CONFIG.get()));
    }

    public void setCanInteractConfig(ItemStack stack, @Nullable List<PawConfigEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            stack.remove(LeashableCollarsNeoForge.PAWS_INTERACT_CONFIG.get());
            return;
        }
        stack.set(LeashableCollarsNeoForge.PAWS_INTERACT_CONFIG.get(), PawConfigEntry.toStored(entries));
    }

    public static boolean shouldPreventBlockInteraction(ItemStack stack, BlockState block) {
        if (block.is(LeashableCollarsNeoForge.PAWS_ALLOW_INTERACT)) {
            return false;
        }

        if (!(stack.getItem() instanceof PawsItem pawsItem)) {
            return false;
        }

        List<PawConfigEntry> config = pawsItem.getCanInteractConfig(stack);
        if (config.isEmpty()) {
            return false;
        }
        for (PawConfigEntry entry : config) {
            if (entry.matchesBlock(block)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSlippery(ItemStack stack) {
        if (!(stack.getItem() instanceof PawsItem pawsItem)) {
            return false;
        }
        return !pawsItem.getHeldItemsConfig(stack).isEmpty();
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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!getHeldItemsConfig(stack).isEmpty()) {
            tooltip.add(Component.translatable("item.playercollars.paws.slippery").withStyle(ChatFormatting.GRAY));
        }
        if (!getCanInteractConfig(stack).isEmpty()) {
            tooltip.add(Component.translatable("item.playercollars.paws.interaction").withStyle(ChatFormatting.GRAY));
        }
    }
}