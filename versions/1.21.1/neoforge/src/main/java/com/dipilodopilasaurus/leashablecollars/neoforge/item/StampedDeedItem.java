package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class StampedDeedItem extends Item {
    public StampedDeedItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData == null || ownerData.ownedName().isEmpty()) {
            return Component.translatable("item.playercollars.stamped_deed_of_ownership.invalid");
        }
        return Component.translatable("item.playercollars.stamped_deed_of_ownership", ownerData.ownedName().get());
    }

    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData != null) {
            tooltip.add(Component.translatable("item.playercollars.collar.owner", ownerData.name()).withStyle(ChatFormatting.GRAY));
        }
    }
}