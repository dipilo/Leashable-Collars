package com.dipilodopilasaurus.leashablecollars.neoforge.leash;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.LeashConfig;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLeashHandler {
    private static final Map<UUID, LeashState> ACTIVE_LEASHES = new ConcurrentHashMap<>();

    private PlayerLeashHandler() {
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (player.level().isClientSide()) {
            return;
        }

        if (target instanceof LeashFenceKnotEntity knot) {
            if (LeashableCollarsNeoForge.blockLeashKnotBreak(player, knot)) {
                event.setCancellationResult(InteractionResult.FAIL);
                event.setCanceled(true);
            }
            return;
        }

        if (!(target instanceof net.minecraft.server.level.ServerPlayer serverTarget)) {
            return;
        }

        ItemStack heldStack = player.getItemInHand(event.getHand());
        LeashState leashState = ACTIVE_LEASHES.get(serverTarget.getUUID());

        if (leashState != null && leashState.tryRelease(player)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        if (heldStack.getItem() != Items.LEAD || leashState != null) {
            return;
        }
        if (LeashableCollarsNeoForge.findOwnedCollar(serverTarget, player.getUUID(), serverTarget.getUUID()) == null) {
            return;
        }

        LeashState newState = new LeashState(serverTarget);
        newState.attach(player);
        ACTIVE_LEASHES.put(serverTarget.getUUID(), newState);
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        LeashState leashState = ACTIVE_LEASHES.get(player.getUUID());
        if (leashState == null) {
            return;
        }

        if (leashState.update()) {
            ACTIVE_LEASHES.remove(player.getUUID());
        }
    }

    public static boolean isAttachedFenceBreak(Player player, BlockPos pos) {
        LeashState leashState = ACTIVE_LEASHES.get(player.getUUID());
        return leashState != null && leashState.isAttachedFenceBreak(pos);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        for (LeashFenceKnotEntity knot : player.level().getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(event.getPos()), entity -> event.getPos().equals(entity.blockPosition()))) {
            if (!LeashableCollarsNeoForge.blockLeashKnotBreak(player, knot)) {
                continue;
            }

            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        for (LeashFenceKnotEntity knot : player.level().getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(event.getPos()), entity -> event.getPos().equals(entity.blockPosition()))) {
            if (LeashableCollarsNeoForge.blockLeashKnotBreak(player, knot)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static final class LeashState {
        private final net.minecraft.server.level.ServerPlayer target;
        private LeashProxyEntity proxy;
        private Entity holder;
        private int attachTick;
        private int loyalty;

        private LeashState(net.minecraft.server.level.ServerPlayer target) {
            this.target = target;
        }

        private void attach(Entity newHolder) {
            holder = newHolder;
            if (newHolder instanceof Player owner) {
                ItemStack collarStack = LeashableCollarsNeoForge.findOwnedCollar(target, owner.getUUID(), target.getUUID());
                loyalty = collarStack == null ? 0 : Math.min(2, LeashableCollarsNeoForge.getEnchantmentLevel(target.level(), collarStack, LeashableCollarsNeoForge.SHORT_LEASH_ENCHANTMENT));
            }
            if (proxy == null || proxy.proxyIsRemoved()) {
                proxy = new LeashProxyEntity(target);
                proxy.setPos(target.getX(), target.getY(), target.getZ());
                target.level().addFreshEntity(proxy);
            }
            proxy.setLeashedTo(holder, true);
            attachTick = target.tickCount;
        }

        private void detach() {
            holder = null;
            if (proxy != null) {
                if (proxy.isAlive() || !proxy.proxyIsRemoved()) {
                    proxy.proxyRemove();
                }
                proxy = null;
            }
        }

        private void dropLead() {
            target.drop(new ItemStack(Items.LEAD), false, true);
        }

        private boolean tryRelease(Player player) {
            if (holder != player || attachTick + 20 >= target.tickCount) {
                return false;
            }
            if (proxy != null && !proxy.canUnleash(player)) {
                return true;
            }
            if (!player.getAbilities().instabuild) {
                dropLead();
            }
            detach();
            return true;
        }

        private boolean update() {
            if (syncProxyState()) {
                return true;
            }

            if (shouldDropForInvalidState()) {
                boolean shouldDropLead = shouldDropLeadForInvalidState();
                detach();
                if (shouldDropLead) {
                    dropLead();
                }
                return true;
            }

            applyPull();
            return holder == null;
        }

        private boolean shouldDropForInvalidState() {
            return holder != null && (!holder.isAlive() || !target.isAlive() || target.hasDisconnected() || target.isVehicle());
        }

        private boolean shouldDropLeadForInvalidState() {
            if (!(holder instanceof LeashFenceKnotEntity knot) || holder.isAlive()) {
                return true;
            }

            return !(knot.level().getBlockState(knot.blockPosition()).getBlock() instanceof FenceBlock);
        }

        private boolean syncProxyState() {
            if (proxy == null) {
                return false;
            }
            if (proxy.proxyIsRemoved()) {
                proxy = null;
                return false;
            }

            Entity actualHolder = holder;
            Entity targetHolder = proxy.getLeashHolder();
            if (targetHolder == null && actualHolder != null) {
                detach();
                return true;
            }
            if (targetHolder != null && targetHolder != actualHolder) {
                attach(targetHolder);
            }
            return false;
        }

        private boolean isAttachedFenceBreak(BlockPos pos) {
            return holder instanceof LeashFenceKnotEntity knot && pos.equals(knot.blockPosition());
        }

        private void applyPull() {
            if (holder == null || holder.level() != target.level()) {
                return;
            }

            float distance = target.distanceTo(holder);
            double minDistance = Math.max(LeashConfig.getMinDistanceFloor(), LeashConfig.getMinDistanceBase() - loyalty);
            double maxDistance = LeashConfig.getMaxDistanceBase() - loyalty;
            if (distance < minDistance) {
                return;
            }

            if (distance > maxDistance && target.level().getGameRules().getBoolean(LeashableCollarsNeoForge.PLAYER_LEASHES_BREAK_RULE)) {
                detach();
                dropLead();
                return;
            }

            double dx = (holder.getX() - target.getX()) / distance;
            double dy = (holder.getY() - target.getY()) / distance;
            double dz = (holder.getZ() - target.getZ()) / distance;
            double factor = LeashConfig.getPullFactorBase() + LeashConfig.getPullFactorPerLoyalty() * loyalty;
                double verticalFactor = dy > 0.0D ? LeashConfig.getVerticalFactorUp() : LeashConfig.getVerticalFactorDown();

            target.push(
                    Math.copySign(dx * dx * factor, dx),
                    Math.copySign(dy * dy * factor * verticalFactor, dy),
                    Math.copySign(dz * dz * factor, dz)
            );
            target.connection.send(new ClientboundSetEntityMotionPacket(target));
            target.hasImpulse = false;
        }
    }
}