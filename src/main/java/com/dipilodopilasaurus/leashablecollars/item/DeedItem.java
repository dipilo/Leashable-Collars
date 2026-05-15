package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.client.screen.DeedItemScreen;
import com.dipilodopilasaurus.leashablecollars.OwnerData;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class DeedItem extends Item {
    public DeedItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            OwnerData ownerData = CollarItem.getOwnerData(stack);
            if (ownerData != null && ownerData.owned().isEmpty()) {
                if (ownerData.uuid().equals(player.getUUID())) {
                    player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.no_self_own"), true);
                    return InteractionResultHolder.pass(stack);
                }
                openStampScreen(stack, player);
                return InteractionResultHolder.consume(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (CollarItem.getOwnerData(stack) == null) {
            CollarItem.setOwnerData(stack, new OwnerData(player.getUUID(), player.getName().getString()));
            player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.filled_out"), true);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (CollarItem.getOwnerData(stack) != null) {
            return Component.translatable("item.playercollars.deed_of_ownership.filled");
        }
        return super.getName(stack);
    }

    @OnlyIn(Dist.CLIENT)
    private void openStampScreen(ItemStack stack, Player player) {
        Minecraft.getInstance().setScreen(new DeedItemScreen(stack, player));
    }
}
