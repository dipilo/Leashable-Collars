package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.OwnerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StampedDeedItem extends Item {
    public StampedDeedItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        OwnerData ownerData = CollarItem.getOwnerData(stack);
        if (ownerData == null || ownerData.ownedName().isEmpty()) {
            return Component.translatable("item.playercollars.stamped_deed_of_ownership.invalid");
        }
        return Component.translatable("item.playercollars.stamped_deed_of_ownership", ownerData.ownedName().get());
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        OwnerData ownerData = CollarItem.getOwnerData(stack);
        if (ownerData != null) {
            tooltip.add(Component.translatable("item.playercollars.collar.owner", ownerData.name()).withStyle(ChatFormatting.GRAY));
        }
    }
}
