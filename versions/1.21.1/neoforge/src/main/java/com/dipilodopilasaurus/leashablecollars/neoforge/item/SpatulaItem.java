package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class SpatulaItem extends Item {
    public SpatulaItem(Properties properties) {
        super(properties.stacksTo(1).durability(8));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isCrouching()) {
            InteractionResult result = interactLivingEntity(held, player, player, hand);
            if (result.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        int removedArmor = removeBoundArmor(interactionTarget);
        int removedCurios = CuriosApi.getCuriosInventory(interactionTarget).map(handler -> {
            int count = 0;
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                count += removeLocked(stacksHandler.getStacks(), interactionTarget);
                count += removeLocked(stacksHandler.getCosmeticStacks(), interactionTarget);
            }
            return count;
        }).orElse(0);
        int removed = removedArmor + removedCurios;

        if (removed == 0) {
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = (ServerLevel) interactionTarget.level();
        stack.hurtAndBreak(removed, serverLevel, user, item -> {});
        interactionTarget.playSound(SoundEvents.ITEM_BREAK, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static int removeLocked(top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks, LivingEntity target) {
        int removed = 0;
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack equipped = stacks.getStackInSlot(slot);
            if (shouldRemove(equipped, target)) {
                removed++;
                LeashableCollarsNeoForge.setLocked(equipped, false);
                target.spawnAtLocation(unlockedCopy(equipped));
                stacks.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        return removed;
    }

    private static int removeBoundArmor(LivingEntity target) {
        int removed = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                ItemStack equipped = target.getItemBySlot(slot);
                if (isCurseBound(equipped, target)) {
                    removed++;
                    target.spawnAtLocation(unlockedCopy(equipped));
                    target.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        return removed;
    }

    private static boolean shouldRemove(ItemStack stack, LivingEntity target) {
        return LeashableCollarsNeoForge.isLocked(stack) || isCurseBound(stack, target);
    }

    private static ItemStack unlockedCopy(ItemStack stack) {
        ItemStack copy = stack.copy();
        LeashableCollarsNeoForge.setLocked(copy, false);
        return copy;
    }

    private static boolean isCurseBound(ItemStack stack, LivingEntity target) {
        if (stack.isEmpty()) {
            return false;
        }
        var bindingCurse = target.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BINDING_CURSE);
        return stack.getEnchantments().getLevel(bindingCurse) > 0;
    }
}