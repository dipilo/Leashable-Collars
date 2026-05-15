package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class SpatulaItem extends Item {
    public SpatulaItem(Properties properties) {
        super(properties.stacksTo(1).durability(8));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isCrouching()) {
            InteractionResult result = interactLivingEntity(player.getItemInHand(hand), player, player, hand);
            if (result.consumesAction()) {
                return result;
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        int removed = CuriosApi.getCuriosInventory(interactionTarget).map(handler -> {
            int count = 0;
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                count += removeLocked(stacksHandler.getStacks(), interactionTarget);
                count += removeLocked(stacksHandler.getCosmeticStacks(), interactionTarget);
            }
            return count;
        }).orElse(0);

        if (removed == 0) {
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = (ServerLevel) interactionTarget.level();
        stack.hurtAndBreak(removed, serverLevel, user, item -> {});
        interactionTarget.playSound(SoundEvents.ITEM_BREAK.value(), 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static int removeLocked(top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks, LivingEntity target) {
        int removed = 0;
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            ItemStack equipped = stacks.getStackInSlot(slot);
            if (!LeashableCollarsNeoForge.isLocked(equipped)) {
                continue;
            }
            removed++;
            target.spawnAtLocation((ServerLevel) target.level(), equipped.copy());
            stacks.setStackInSlot(slot, ItemStack.EMPTY);
        }
        return removed;
    }
}