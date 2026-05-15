package com.dipilodopilasaurus.leashablecollars.fabric.item;

import com.dipilodopilasaurus.leashablecollars.fabric.LeashableCollarsFabric;
import net.minecraft.block.MapColor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.MapColorComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class CollarItem extends Item {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LeashableCollarsFabric.MOD_ID, "collar"));
    public static final RegistryKey<Item> TAGLESS_REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(LeashableCollarsFabric.MOD_ID, "tagless_collar"));

    public CollarItem(boolean tagless) {
        super(new Item.Settings()
                .maxCount(1)
                .registryKey(tagless ? TAGLESS_REGISTRY_KEY : REGISTRY_KEY)
                .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(MapColor.RED.color))
                .component(DataComponentTypes.MAP_COLOR, new MapColorComponent(MapColor.BLUE.color)));
    }
}