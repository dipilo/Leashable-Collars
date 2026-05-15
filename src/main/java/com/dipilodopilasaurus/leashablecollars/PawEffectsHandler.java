package com.dipilodopilasaurus.leashablecollars;

import com.dipilodopilasaurus.leashablecollars.item.PawsItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = LeashableCollars.MOD_ID)
public final class PawEffectsHandler {
    private PawEffectsHandler() {
    }

    private static List<SlotResult> getHandPaws(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.findCurios("hands")).orElse(Collections.emptyList());
    }

    private static List<SlotResult> getFootPaws(Player player) {
        return CuriosApi.getCuriosInventory(player).map(handler -> handler.findCurios("feet")).orElse(Collections.emptyList());
    }

    public static boolean shouldForceFootPawsCrawl(Player player) {
        return !player.getAbilities().flying && !getFootPaws(player).isEmpty();
    }

    private static boolean hasPawsInHands(Player player) {
        for (SlotResult slotResult : getHandPaws(player)) {
            if (slotResult.stack().getItem() instanceof PawsItem) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (!hasPawsInHands(player)) {
            return;
        }

        BlockState state = event.getState();
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            event.setNewSpeed(Tiers.IRON.getSpeed());
            return;
        }

        float currentSpeed = event.getNewSpeed();
        event.setNewSpeed((currentSpeed - 1.0F) * 0.125F + 1.0F);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !hasPawsInHands(player)) {
            return;
        }

        float amount = event.getAmount();
        event.setAmount((amount - 1.0F) * 0.75F + 1.0F);
    }

    @SubscribeEvent
    public static void onAttackOwner(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (player.level().getGameRules().getBoolean(LeashableCollars.ALLOW_ATTACK_OWNER)) {
            return;
        }
        if (LeashableCollars.findOwnedCollar(player, event.getTarget().getUUID(), player.getUUID()) == null) {
            return;
        }

        player.displayClientMessage(Component.translatable("message.playercollars.no_attack_owner").withStyle(ChatFormatting.RED), true);
        double damage = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        damage = (damage - 1.0D) * 0.75D + 1.0D;
        player.hurt(player.damageSources().playerAttack(player), (float) Math.ceil(damage));
    }

    @SubscribeEvent
    public static void onAttackLeashKnot(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (event.getTarget() instanceof LeashFenceKnotEntity knot && LeashableCollars.blockLeashKnotBreak(player, knot)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockState state = player.level().getBlockState(event.getPos());
        for (SlotResult slotResult : getHandPaws(player)) {
            if (slotResult.stack().getItem() instanceof PawsItem && PawsItem.shouldPreventBlockInteraction(slotResult.stack(), state)) {
                event.setUseBlock(Event.Result.DENY);
                event.setUseItem(Event.Result.DENY);
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        boolean forceCrawl = shouldForceFootPawsCrawl(player);
        player.setForcedPose(forceCrawl ? Pose.SWIMMING : null);

        for (SlotResult slotResult : getHandPaws(player)) {
            if (!(slotResult.stack().getItem() instanceof PawsItem)) {
                continue;
            }

            ItemStack mainHand = player.getMainHandItem();
            if (PawsItem.shouldDrop(slotResult.stack(), mainHand)) {
                ItemStack dropped = mainHand.copy();
                player.getInventory().setItem(player.getInventory().selected, ItemStack.EMPTY);
                player.drop(dropped, true);
            }

            ItemStack offHand = player.getOffhandItem();
            if (PawsItem.shouldDrop(slotResult.stack(), offHand)) {
                ItemStack dropped = offHand.copy();
                player.getInventory().offhand.set(0, ItemStack.EMPTY);
                player.drop(dropped, true);
            }
        }
    }
}