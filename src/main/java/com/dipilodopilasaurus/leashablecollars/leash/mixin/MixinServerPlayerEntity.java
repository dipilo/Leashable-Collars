package com.dipilodopilasaurus.leashablecollars.leash.mixin;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import com.dipilodopilasaurus.leashablecollars.leash.LeashImpl;
import com.dipilodopilasaurus.leashablecollars.leash.LeashProxyEntity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerEntity implements LeashImpl {
    @Unique
    private final ServerPlayer leashPlayersSelf = (ServerPlayer) (Object) this;
    @Unique
    private LeashProxyEntity leashPlayersProxy;
    @Unique
    private Entity leashPlayersHolder;
    @Unique
    private int leashPlayersLastAge;
    @Unique
    private int leashPlayersLoyalty;

    @Unique
    private void updateLeashPlayersState() {
        if (leashPlayersHolder != null && (!leashPlayersHolder.isAlive() || !leashPlayersSelf.isAlive() || leashPlayersSelf.hasDisconnected() || leashPlayersSelf.isVehicle())) {
            detachLeashPlayers();
            dropLeashPlayersLead();
        }

        if (leashPlayersProxy != null) {
            if (leashPlayersProxy.proxyIsRemoved()) {
                leashPlayersProxy = null;
            } else {
                Entity actualHolder = leashPlayersHolder;
                Entity targetHolder = leashPlayersProxy.getLeashHolder();
                if (targetHolder == null && actualHolder != null) {
                    detachLeashPlayers();
                    dropLeashPlayersLead();
                } else if (targetHolder != actualHolder) {
                    attachLeashPlayers(targetHolder);
                }
            }
        }

        applyLeashPlayersPull();
    }

    @Unique
    private void applyLeashPlayersPull() {
        ServerPlayer player = leashPlayersSelf;
        Entity holder = leashPlayersHolder;
        if (holder == null || holder.level() != player.level()) {
            return;
        }

        float distance = player.distanceTo(holder);
        double minDistance = Math.max(1.5D, 4.0D - leashPlayersLoyalty);
        double maxDistance = 10.0D - leashPlayersLoyalty;
        if (Math.abs(player.getY() - holder.getY()) > maxDistance) {
            handleLeashPlayersOverstretch(holder);
            return;
        }
        if (distance < minDistance) {
            return;
        }
        if (distance > maxDistance) {
            handleLeashPlayersOverstretch(holder);
            return;
        }

        double dx = holder.getX() - player.getX();
        double dz = holder.getZ() - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < minDistance) {
            return;
        }

        double factor = Math.min(0.15D * (horizontalDistance - minDistance), 0.375D) / horizontalDistance;
        player.setDeltaMovement(player.getDeltaMovement().add(dx * factor, 0.0D, dz * factor));

        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.hasImpulse = true;
    }

    @Unique
    private void handleLeashPlayersOverstretch(Entity holder) {
        if (leashPlayersSelf.level().getGameRules().getBoolean(LeashableCollars.PLAYER_LEASHES_BREAK_RULE)) {
            detachLeashPlayers();
            dropLeashPlayersLead();
            return;
        }

        leashPlayersSelf.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (leashPlayersProxy != null) {
            leashPlayersProxy.setPos(holder.getX(), holder.getY(), holder.getZ());
        }
        leashPlayersSelf.connection.teleport(holder.getX(), holder.getY(), holder.getZ(), leashPlayersSelf.getYRot(), leashPlayersSelf.getXRot());
    }

    @Unique
    private void attachLeashPlayers(Entity entity) {
        leashPlayersHolder = entity;
        if (leashPlayersProxy == null) {
            leashPlayersProxy = new LeashProxyEntity(leashPlayersSelf);
            leashPlayersProxy.setPos(leashPlayersSelf.getX(), leashPlayersSelf.getY(), leashPlayersSelf.getZ());
            leashPlayersSelf.level().addFreshEntity(leashPlayersProxy);
        }
        leashPlayersProxy.setLeashedTo(leashPlayersHolder, true);
        if (leashPlayersSelf.isVehicle()) {
            leashPlayersSelf.stopRiding();
        }
        leashPlayersLastAge = leashPlayersSelf.tickCount;
    }

    @Unique
    private void detachLeashPlayers() {
        leashPlayersHolder = null;
        if (leashPlayersProxy != null) {
            if (leashPlayersProxy.isAlive() || !leashPlayersProxy.proxyIsRemoved()) {
                leashPlayersProxy.proxyRemove();
            }
            leashPlayersProxy = null;
        }
    }

    @Unique
    private void dropLeashPlayersLead() {
        leashPlayersSelf.drop(new ItemStack(Items.LEAD), false, true);
    }

    @Unique
    private boolean tryBeginLeash(Player player, ItemStack stack) {
        if (stack.getItem() != Items.LEAD || leashPlayersHolder != null) {
            return false;
        }

        AtomicBoolean found = new AtomicBoolean(false);
        CuriosApi.getCuriosInventory((Player) (Object) this).ifPresent(handler -> handler.getStacksHandler("necklace").ifPresent(slot -> {
            ItemStack collarStack = LeashableCollars.filterStacksByOwner(slot.getStacks(), player.getUUID());
            if (collarStack == null) {
                collarStack = LeashableCollars.filterStacksByOwner(slot.getCosmeticStacks(), player.getUUID());
            }
            if (collarStack != null) {
                found.set(true);
                leashPlayersLoyalty = Mth.clamp(LeashableCollars.COLLAR_ITEM.get().getEnchantmentLevel(collarStack, LeashableCollars.SHORT_LEASH_ENCHANTMENT.get()), 0, 2);
            }
        }));
        if (!found.get()) {
            return false;
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        attachLeashPlayers(player);
        return true;
    }

    @Unique
    private boolean tryReleaseLeash(Player player) {
        if (leashPlayersHolder != player || leashPlayersLastAge + 20 >= leashPlayersSelf.tickCount) {
            return false;
        }
        if (leashPlayersProxy != null && !leashPlayersProxy.canUnleash(player)) {
            return true;
        }
        if (!player.isCreative()) {
            dropLeashPlayersLead();
        }
        detachLeashPlayers();
        return true;
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void tickLeashPlayers(CallbackInfo ci) {
        updateLeashPlayersState();
    }

    @Override
    public InteractionResult leashPlayersInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (tryBeginLeash(player, stack)) {
            return InteractionResult.SUCCESS;
        }
        if (tryReleaseLeash(player)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Entity leashPlayersGetProxyLeashHolder() {
        return leashPlayersProxy == null ? null : leashPlayersProxy.getLeashHolder();
    }

    @Inject(method = "hurt", at = @At("TAIL"))
    private void checkCollarThorns(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (damageSource.getEntity() != null) {
            LivingEntity self = (LivingEntity) (Object) this;
            CollarItem item = LeashableCollars.COLLAR_ITEM.get();
            CuriosApi.getCuriosInventory(self).ifPresent(handler -> {
                for (SlotResult slotResult : handler.findCurios("necklace")) {
                    int level = item.getEnchantmentLevel(slotResult.stack(), LeashableCollars.THORNS_ENCHANTMENT.get());
                    if (level > 0) {
                        net.minecraft.world.item.enchantment.Enchantments.THORNS.doPostHurt(self, damageSource.getEntity(), level);
                    }
                }
            });
        }
    }
}
