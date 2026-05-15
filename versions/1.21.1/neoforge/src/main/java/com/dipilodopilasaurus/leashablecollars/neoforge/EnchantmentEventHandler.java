package com.dipilodopilasaurus.leashablecollars.neoforge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Collections;
import java.util.List;

public final class EnchantmentEventHandler {
    private EnchantmentEventHandler() {
    }

    private static List<SlotResult> getNecklaceItems(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).map(handler -> handler.findCurios("necklace")).orElse(Collections.emptyList());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (SlotResult slotResult : getNecklaceItems(player)) {
            int amplifier = getRegenerationAmplifier(serverLevel, player, slotResult.stack());
            if (amplifier >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, amplifier, false, false, false));
                return;
            }
        }
    }

    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || victim.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == victim) {
            return;
        }

        for (SlotResult slotResult : getNecklaceItems(victim)) {
            if (tryApplyThorns(victim, attacker, slotResult.stack())) {
                return;
            }
        }
    }

    private static int getRegenerationAmplifier(ServerLevel serverLevel, Player player, ItemStack stack) {
        int level = LeashableCollarsNeoForge.getEnchantmentLevel(serverLevel, stack, LeashableCollarsNeoForge.REGENERATION_ENCHANTMENT);
        if (level <= 0) {
            return -1;
        }

        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData == null || ownerData.uuid().equals(player.getUUID())) {
            return -1;
        }

        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerData.uuid());
        return owner != null && owner.distanceTo(player) < 16.0F ? Math.max(0, level - 1) : -1;
    }

    private static boolean tryApplyThorns(LivingEntity victim, LivingEntity attacker, ItemStack stack) {
        int level = LeashableCollarsNeoForge.getEnchantmentLevel(victim.level(), stack, LeashableCollarsNeoForge.THORNS_ENCHANTMENT);
        if (level <= 0 || victim.getRandom().nextFloat() >= 0.15F * level) {
            return false;
        }

        float damage = 1.0F + victim.getRandom().nextInt(5);
        attacker.hurt(victim.damageSources().thorns(victim), damage);
        return true;
    }
}