package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class CollarLockerItem extends Item {
    public CollarLockerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target) || user.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        ItemStack ownerCollar = LeashableCollarsNeoForge.findOwnedCollar(target, user.getUUID(), target.getUUID());
        if (ownerCollar == null) {
            user.displayClientMessage(Component.translatable("item.playercollars.collar_locker.no_set_non_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(ownerCollar);
        if (ownerData == null || ownerData.owned().isEmpty()) {
            user.displayClientMessage(Component.translatable("item.playercollars.collar_locker.no_set_non_deed").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        boolean shouldLock = !LeashableCollarsNeoForge.isLocked(ownerCollar);
        Holder<Enchantment> bindingCurse = user.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE);
        CuriosApi.getCuriosInventory(target).ifPresent(handler -> {
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                updateLocked(stacksHandler.getStacks(), bindingCurse, shouldLock);
                updateLocked(stacksHandler.getCosmeticStacks(), bindingCurse, shouldLock);
            }
        });

        Component message = Component.translatable(shouldLock ? "item.playercollars.collar_locker.locked" : "item.playercollars.collar_locker.unlocked");
        user.displayClientMessage(message, true);
        target.displayClientMessage(message, true);
        target.level().playSound(null, target.blockPosition(), shouldLock ? SoundEvents.ARMOR_EQUIP_LEATHER.value() : SoundEvents.ARMOR_UNEQUIP_WOLF, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void updateLocked(top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks, Holder<Enchantment> bindingCurse, boolean locked) {
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack equipped = stacks.getStackInSlot(slot);
            if (equipped.is(LeashableCollarsNeoForge.COLLAR_TAG) || equipped.is(LeashableCollarsNeoForge.PAWS_TAG) || equipped.is(LeashableCollarsNeoForge.FOOT_PAWS_TAG)) {
                LeashableCollarsNeoForge.setLocked(equipped, locked);
                LeashableCollarsNeoForge.setBindingCurse(equipped, bindingCurse, locked);
            }
        }
    }
}