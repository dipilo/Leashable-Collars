package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ClickerItem extends Item {
    private static final int CLICKER_DISTANCE = 4;

    public ClickerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static boolean shouldForceTurn(ItemStack stack) {
        return stack.has(DataComponents.INTANGIBLE_PROJECTILE);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                if (shouldForceTurn(stack)) {
                    stack.remove(DataComponents.INTANGIBLE_PROJECTILE);
                    player.displayClientMessage(Component.translatable("item.playercollars.clicker.turn_disable"), true);
                } else {
                    stack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
                    player.displayClientMessage(Component.translatable("item.playercollars.clicker.turn_enable"), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            if (shouldForceTurn(stack)) {
                forceTurnNearbyPlayers((ServerLevel) level, player);
            }
            level.playSound(null, player, LeashableCollarsNeoForge.CLICKER_ON.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.FAIL;
    }

    private static void forceTurnNearbyPlayers(ServerLevel level, Player owner) {
        for (ServerPlayer other : level.players()) {
            if (other == owner || !other.closerThan(owner, CLICKER_DISTANCE)) {
                continue;
            }
            if (LeashableCollarsNeoForge.findOwnedCollar(other, owner.getUUID()) != null) {
                other.lookAt(EntityAnchorArgument.Anchor.EYES, owner.getEyePosition());
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide()) {
            level.playSound(null, entity, LeashableCollarsNeoForge.CLICKER_OFF.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return false;
    }
}