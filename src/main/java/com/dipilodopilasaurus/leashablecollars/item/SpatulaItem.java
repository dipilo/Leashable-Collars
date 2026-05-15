package com.dipilodopilasaurus.leashablecollars.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class SpatulaItem extends Item {
    public SpatulaItem() {
        super(new Properties().stacksTo(1).durability(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isCrouching()) {
            InteractionResult result = interactLivingEntity(player.getItemInHand(hand), player, player, hand);
            if (result.consumesAction()) {
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget.level().isClientSide) {
            return InteractionResult.PASS;
        }

        int count = removeBoundArmor(interactionTarget) + removeBoundCurios(interactionTarget);

        if (count == 0) {
            return InteractionResult.PASS;
        }

        stack.hurtAndBreak(count, user, entity -> entity.broadcastBreakEvent(usedHand));
        interactionTarget.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private int removeBoundArmor(LivingEntity interactionTarget) {
        int removed = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack equipped = interactionTarget.getItemBySlot(slot);
            if (removeBoundStack(interactionTarget, equipped)) {
                removed++;
                interactionTarget.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        return removed;
    }

    private int removeBoundCurios(LivingEntity interactionTarget) {
        return CuriosApi.getCuriosInventory(interactionTarget)
                .map(ICuriosItemHandler::getCurios)
                .map(curios -> {
                    int removed = 0;
                    for (ICurioStacksHandler stacksHandler : curios.values()) {
                        removed += removeBoundStacks(interactionTarget, stacksHandler.getStacks());
                        removed += removeBoundStacks(interactionTarget, stacksHandler.getCosmeticStacks());
                    }
                    return removed;
                })
                .orElse(0);
    }

    private int removeBoundStacks(LivingEntity interactionTarget, top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks) {
        int removed = 0;
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack equipped = stacks.getStackInSlot(i);
            if (removeBoundStack(interactionTarget, equipped)) {
                removed++;
                stacks.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return removed;
    }

    private boolean removeBoundStack(LivingEntity interactionTarget, ItemStack equipped) {
        if (equipped.getEnchantmentLevel(Enchantments.BINDING_CURSE) <= 0) {
            return false;
        }
        interactionTarget.spawnAtLocation(equipped.copy());
        return true;
    }
}
