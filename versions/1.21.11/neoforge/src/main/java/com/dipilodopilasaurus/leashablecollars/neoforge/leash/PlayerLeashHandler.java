package com.dipilodopilasaurus.leashablecollars.neoforge.leash;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

        if (target instanceof LeashFenceKnotEntity knot && LeashableCollarsNeoForge.blockLeashKnotBreak(player, knot)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
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

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (isAttachedFenceBreak(player, event.getPos())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.playercollars.no_break_fence").withStyle(net.minecraft.ChatFormatting.RED), true);
            event.setCanceled(true);
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

        private LeashState(net.minecraft.server.level.ServerPlayer target) {
            this.target = target;
        }

        private void attach(Entity newHolder) {
            holder = newHolder;
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
            if (holder != null && (!holder.isAlive() || !target.isAlive() || target.hasDisconnected() || target.isVehicle())) {
                detach();
                dropLead();
                return true;
            }

            if (proxy != null) {
                if (proxy.proxyIsRemoved()) {
                    proxy = null;
                } else {
                    Entity actualHolder = holder;
                    Entity targetHolder = proxy.getLeashHolder();
                    if (targetHolder == null && actualHolder != null) {
                        detach();
                        dropLead();
                        return true;
                    }
                    if (targetHolder != null && targetHolder != actualHolder) {
                        attach(targetHolder);
                    }
                }
            }

            applyPull();
            return holder == null;
        }

        private boolean isAttachedFenceBreak(BlockPos pos) {
            return holder instanceof LeashFenceKnotEntity knot && pos.equals(knot.blockPosition());
        }

        private void applyPull() {
            if (holder == null || holder.level() != target.level()) {
                return;
            }

            float distance = target.distanceTo(holder);
            double minDistance = 4.0D;
            double maxDistance = 10.0D;
            if (Math.abs(target.getY() - holder.getY()) > maxDistance || distance > maxDistance) {
                if (target.level().getGameRules().get(LeashableCollarsNeoForge.PLAYER_LEASHES_BREAK_RULE)) {
                    detach();
                    dropLead();
                    return;
                }

                target.setDeltaMovement(Vec3.ZERO);
                if (proxy != null) {
                    proxy.setPos(holder.getX(), holder.getY(), holder.getZ());
                }
                target.teleportTo(holder.getX(), holder.getY(), holder.getZ());
                return;
            }

            if (distance < minDistance) {
                return;
            }

            double dx = holder.getX() - target.getX();
            double dz = holder.getZ() - target.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance < minDistance) {
                return;
            }

            double factor = Math.min(0.15D * (horizontalDistance - minDistance), 0.375D) / horizontalDistance;
            target.setDeltaMovement(target.getDeltaMovement().add(dx * factor, 0.0D, dz * factor));
            target.hurtMarked = true;
        }
    }
}