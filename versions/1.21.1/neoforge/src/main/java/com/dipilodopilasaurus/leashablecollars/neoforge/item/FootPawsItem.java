package com.dipilodopilasaurus.leashablecollars.neoforge.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class FootPawsItem extends Item implements ICurioItem {
    private static final int DEFAULT_BEAN_COLOR = 0xF196CF;

    public FootPawsItem(Properties properties, int defaultColor) {
        super(properties.stacksTo(1)
                .component(DataComponents.DYED_COLOR, new DyedItemColor(defaultColor, true))
                .component(DataComponents.MAP_COLOR, new MapItemColor(DEFAULT_BEAN_COLOR)));
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
    public ICurio.DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return !com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge.isLocked(stack);
    }
}