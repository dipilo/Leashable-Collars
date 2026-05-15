package com.dipilodopilasaurus.leashablecollars.neoforge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.PawsItem;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;
import java.util.List;

public final class PawsEventHandler {
    private PawsEventHandler() {
    }

    private static List<SlotResult> getHandPaws(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.findCurios("hands")).orElse(Collections.emptyList());
    }

    private static List<SlotResult> getFootPaws(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.findCurios("feet")).orElse(Collections.emptyList());
    }

    private static boolean hasPawsInHands(Player player) {
        for (SlotResult slotResult : getHandPaws(player)) {
            if (slotResult.stack().getItem() instanceof PawsItem) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldForceFootPawsCrawl(Player player) {
        return !player.getAbilities().flying && !getFootPaws(player).isEmpty();
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!hasPawsInHands(player)) {
            return;
        }

        BlockState state = event.getState();
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            event.setNewSpeed(6.0F);
            return;
        }

        float currentSpeed = event.getNewSpeed();
        event.setNewSpeed((currentSpeed - 1.0F) * 0.125F + 1.0F);
    }

    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !hasPawsInHands(player)) {
            return;
        }

        float amount = event.getNewDamage();
        event.setNewDamage((amount - 1.0F) * 0.75F + 1.0F);
    }

    public static void onAttackOwner(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameRules().get(LeashableCollarsNeoForge.ALLOW_ATTACK_OWNER)) {
            return;
        }
        if (LeashableCollarsNeoForge.findOwnedCollar(player, event.getTarget().getUUID(), player.getUUID()) == null) {
            return;
        }

        player.displayClientMessage(Component.translatable("message.playercollars.no_attack_owner").withStyle(ChatFormatting.RED), true);
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        damage = (damage - 1.0D) * 0.75D + 1.0D;
        player.hurt(player.damageSources().playerAttack(player), (float) Math.ceil(damage));
    }

    public static void onAttackLeashKnot(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (event.getTarget() instanceof LeashFenceKnotEntity knot && LeashableCollarsNeoForge.blockLeashKnotBreak(player, knot)) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockState state = player.level().getBlockState(event.getPos());
        for (SlotResult slotResult : getHandPaws(player)) {
            if (slotResult.stack().getItem() instanceof PawsItem
                    && PawsItem.shouldPreventBlockInteraction(slotResult.stack(), state)) {
                event.setUseBlock(TriState.FALSE);
                event.setUseItem(TriState.FALSE);
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        player.setForcedPose(shouldForceFootPawsCrawl(player) ? Pose.SWIMMING : null);

        if (player.level().isClientSide()) {
            return;
        }

        for (SlotResult slotResult : getHandPaws(player)) {
            if (!(slotResult.stack().getItem() instanceof PawsItem)) {
                continue;
            }

            ItemStack mainHand = player.getMainHandItem();
            if (PawsItem.shouldDrop(slotResult.stack(), mainHand)) {
                ItemStack dropped = mainHand.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.drop(dropped, true);
            }

            ItemStack offHand = player.getOffhandItem();
            if (PawsItem.shouldDrop(slotResult.stack(), offHand)) {
                ItemStack dropped = offHand.copy();
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.drop(dropped, true);
            }
        }
    }
}