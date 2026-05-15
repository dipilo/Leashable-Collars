package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.common.DropRule;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class CollarItem extends Item implements ICurioItem {
    private final boolean tagless;

    public CollarItem(Properties properties, boolean tagless) {
        super(properties.stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.RED.getTextureDiffuseColor()))
                .component(DataComponents.MAP_COLOR, new MapItemColor(DyeColor.BLUE.getTextureDiffuseColor())));
        this.tagless = tagless;
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
    }

    @Override
    public DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit, ItemStack stack) {
        return DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return !LeashableCollarsNeoForge.isLocked(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
        if (ownerData != null && ownerData.ownedName().isPresent()) {
            return Component.translatable("item.playercollars.collar.named", ownerData.ownedName().get());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (!(interactionTarget instanceof Player target) || user.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        return CuriosApi.getCuriosInventory(target).flatMap(handler -> handler.getStacksHandler("necklace")).map(slotHandler -> {
            for (int slot = 0; slot < slotHandler.getStacks().getSlots(); slot++) {
                if (!slotHandler.getStacks().getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                ItemStack equipped = stack.copyWithCount(1);
                if (!this.tagless) {
                    LeashableCollarsNeoForge.setOwnerData(equipped, new OwnerData(user.getUUID(), user.getName().getString()).withOwned(target.getUUID(), target.getName().getString()));
                }
                slotHandler.getStacks().setStackInSlot(slot, equipped);
                if (!user.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }).orElse(InteractionResult.FAIL);
    }
}