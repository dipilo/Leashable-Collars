package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.ClientScreenHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class DeedItem extends Item {
    public DeedItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
            if (ownerData != null && ownerData.owned().isEmpty()) {
                if (ownerData.uuid().equals(player.getUUID())) {
                    player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.no_self_own"), true);
                    return InteractionResultHolder.pass(stack);
                }
                ClientScreenHooks.openDeedItemScreen(stack, player);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide() && LeashableCollarsNeoForge.getOwnerData(stack) == null) {
            LeashableCollarsNeoForge.setOwnerData(stack, new OwnerData(player.getUUID(), player.getName().getString()));
            player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.filled_out"), true);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return LeashableCollarsNeoForge.getOwnerData(stack) != null
                ? Component.translatable("item.playercollars.deed_of_ownership.filled")
                : super.getName(stack);
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