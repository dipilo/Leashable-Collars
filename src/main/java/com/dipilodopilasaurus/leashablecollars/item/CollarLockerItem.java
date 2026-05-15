package com.dipilodopilasaurus.leashablecollars.item;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashMap;
import java.util.Map;

public class CollarLockerItem extends Item {
    public CollarLockerItem() {
        super(new Properties().stacksTo(1));
    }

    private static boolean canUseOn(Player user, LivingEntity target) {
        return target instanceof Player && !user.level().isClientSide;
    }

    private static boolean isLockable(ItemStack stack) {
        return stack.is(LeashableCollars.COLLAR_TAG) || stack.is(LeashableCollars.PAWS_TAG) || stack.is(LeashableCollars.FOOT_PAWS_TAG);
    }

    private static void updateBinding(ItemStack stack, boolean shouldLock) {
        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (shouldLock) {
            enchantments.put(Enchantments.BINDING_CURSE, 1);
        } else {
            enchantments.remove(Enchantments.BINDING_CURSE);
        }
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }

    private static boolean hasStampedOwnership(ItemStack stack) {
        return CollarItem.getOwnerData(stack) != null && CollarItem.getOwnerData(stack).owned().isPresent();
    }

    private static void updateBinding(IDynamicStackHandler stacks, boolean shouldLock) {
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack equipped = stacks.getStackInSlot(slot);
            if (isLockable(equipped)) {
                updateBinding(equipped, shouldLock);
            }
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, net.minecraft.world.InteractionHand usedHand) {
        if (!canUseOn(user, interactionTarget)) {
            return InteractionResult.PASS;
        }
        Player target = (Player) interactionTarget;

        ItemStack ownerCollar = LeashableCollars.findOwnedCollar(target, user.getUUID());
        if (ownerCollar == null) {
            user.displayClientMessage(Component.translatable("item.playercollars.collar_locker.no_set_non_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!hasStampedOwnership(ownerCollar)) {
            user.displayClientMessage(Component.translatable("item.playercollars.collar_locker.no_set_non_deed").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        boolean shouldLock = ownerCollar.getEnchantmentLevel(Enchantments.BINDING_CURSE) == 0;
        CuriosApi.getCuriosInventory(target).ifPresent(handler -> {
            for (String identifier : new String[]{"necklace", "hands", "feet"}) {
                handler.getStacksHandler(identifier).ifPresent(slot -> {
                    updateBinding(slot.getStacks(), shouldLock);
                    updateBinding(slot.getCosmeticStacks(), shouldLock);
                });
            }
        });

        Component message = Component.translatable(shouldLock ? "item.playercollars.collar_locker.locked" : "item.playercollars.collar_locker.unlocked");
        target.displayClientMessage(message, true);
        user.displayClientMessage(message, true);
        target.level().playSound(null, target.blockPosition(), shouldLock ? SoundEvents.ARMOR_EQUIP_LEATHER : SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
