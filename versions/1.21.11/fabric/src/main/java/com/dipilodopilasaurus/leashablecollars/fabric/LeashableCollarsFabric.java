package com.dipilodopilasaurus.leashablecollars.fabric;

import com.dipilodopilasaurus.leashablecollars.fabric.block.DogBedBlock;
import com.dipilodopilasaurus.leashablecollars.fabric.block.DogBowlBlock;
import com.dipilodopilasaurus.leashablecollars.fabric.block.InvisibleFenceBlock;
import com.dipilodopilasaurus.leashablecollars.fabric.item.CollarItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BedItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LeashableCollarsFabric implements ModInitializer {
    public static final String MOD_ID = "playercollars";
    public static final Logger LOGGER = LoggerFactory.getLogger("playercollars-fabric");
    public static final TagKey<Item> COLLAR_TAG = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "collars"));

    private static final DyeColor[] PAWS_DYE_COLORS = {
            DyeColor.WHITE,
            DyeColor.LIGHT_GRAY,
            DyeColor.GRAY,
            DyeColor.BLACK,
            DyeColor.BLUE,
            DyeColor.RED,
            DyeColor.PURPLE
    };

    public static final CollarItem COLLAR_ITEM = registerItem("collar", new CollarItem(false));
    public static final CollarItem TAGLESS_COLLAR_ITEM = registerItem("tagless_collar", new CollarItem(true));
    public static final Item CLICKER_ITEM = registerItem("clicker");
    public static final Item DEED_OF_OWNERSHIP = registerItem("deed_of_ownership");
    public static final Item STAMPED_DEED_OF_OWNERSHIP = registerItem("stamped_deed_of_ownership");
    public static final Item PAW_CONFIGURATION_ITEM = registerItem("paw_configurator");
    public static final Item COLLAR_LOCKER_ITEM = registerItem("collar_locker");
    public static final Item SPATULA_ITEM = registerItem("golden_spatula", new Item.Settings().maxCount(1));

    private static final Item[] PAWS_ITEMS = new Item[PAWS_DYE_COLORS.length];
    private static final Item[] FOOT_PAWS_ITEMS = new Item[PAWS_DYE_COLORS.length];
    private static final DogBedBlock[] DOG_BEDS = new DogBedBlock[DyeColor.values().length];
    private static final BedItem[] DOG_BED_ITEMS = new BedItem[DyeColor.values().length];
    private static final DogBowlBlock[] DOG_BOWLS = new DogBowlBlock[DyeColor.values().length];
    private static final BlockItem[] DOG_BOWL_ITEMS = new BlockItem[DyeColor.values().length];

    public static final InvisibleFenceBlock INVISIBLE_FENCE_BLOCK = Registry.register(Registries.BLOCK, InvisibleFenceBlock.REGISTRY_KEY,
            new InvisibleFenceBlock(AbstractBlock.Settings.create()
                    .breakInstantly()
                    .sounds(BlockSoundGroup.GLASS)
                    .nonOpaque()
                    .dynamicBounds()));
    public static final Item INVISIBLE_FENCE_BLOCK_ITEM = registerBlockItem("invisible_fence", INVISIBLE_FENCE_BLOCK);
    public static final BlockEntityType<DogBowlBlock.DogBowlBlockEntity> DOG_BOWL_BLOCK_ENTITY;

    public static final ItemGroup GROUP;

    static {
        for (int i = 0; i < PAWS_DYE_COLORS.length; i++) {
            String colorName = PAWS_DYE_COLORS[i].asString();
            PAWS_ITEMS[i] = registerItem(colorName + "_paws");
            FOOT_PAWS_ITEMS[i] = registerItem(colorName + "_foot_paws");
        }

        for (DyeColor color : DyeColor.values()) {
            RegistryKey<Block> bedKey = DogBedBlock.getRegistryKey(color);
            DOG_BEDS[color.ordinal()] = Registry.register(Registries.BLOCK, bedKey,
                    new DogBedBlock(color, bedKey));
            RegistryKey<Item> bedItemKey = RegistryKey.of(RegistryKeys.ITEM, bedKey.getValue());
            DOG_BED_ITEMS[color.ordinal()] = Registry.register(Registries.ITEM, bedItemKey,
                    new BedItem(DOG_BEDS[color.ordinal()], new Item.Settings().maxCount(1).registryKey(bedItemKey)));

            RegistryKey<Block> bowlKey = DogBowlBlock.getRegistryKey(color);
            DOG_BOWLS[color.ordinal()] = Registry.register(Registries.BLOCK, bowlKey,
                    new DogBowlBlock(AbstractBlock.Settings.create()
                            .sounds(BlockSoundGroup.STONE)
                            .strength(0.6F)
                            .nonOpaque()
                            .pistonBehavior(PistonBehavior.DESTROY)
                            .registryKey(bowlKey)));
            RegistryKey<Item> bowlItemKey = RegistryKey.of(RegistryKeys.ITEM, bowlKey.getValue());
            DOG_BOWL_ITEMS[color.ordinal()] = Registry.register(Registries.ITEM, bowlItemKey,
                    new BlockItem(DOG_BOWLS[color.ordinal()], new Item.Settings().registryKey(bowlItemKey)));
        }

        DOG_BOWL_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                id("dog_bowl"),
                FabricBlockEntityTypeBuilder.create(DogBowlBlock.DogBowlBlockEntity::new, DOG_BOWLS).build()
        );

        GROUP = Registry.register(Registries.ITEM_GROUP, id("group"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.playercollars"))
                .icon(() -> new ItemStack(COLLAR_ITEM))
                .entries((displayContext, entries) -> {
                    entries.add(COLLAR_ITEM);
                    entries.add(TAGLESS_COLLAR_ITEM);
                    entries.add(CLICKER_ITEM);
                    entries.add(DEED_OF_OWNERSHIP);
                    entries.add(STAMPED_DEED_OF_OWNERSHIP);
                    entries.add(PAW_CONFIGURATION_ITEM);
                    entries.add(COLLAR_LOCKER_ITEM);
                    entries.add(SPATULA_ITEM);
                    for (Item item : PAWS_ITEMS) {
                        entries.add(item);
                    }
                    for (Item item : FOOT_PAWS_ITEMS) {
                        entries.add(item);
                    }
                    for (Item item : DOG_BED_ITEMS) {
                        entries.add(item);
                    }
                    for (Item item : DOG_BOWL_ITEMS) {
                        entries.add(item);
                    }
                    entries.add(INVISIBLE_FENCE_BLOCK_ITEM);
                })
                .build());
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Registered Leashable Collars Fabric content in compatibility mode: {} items, {} beds, {} bowls", countRegisteredItems(), DOG_BED_ITEMS.length, DOG_BOWL_ITEMS.length);
    }

    private static int countRegisteredItems() {
        return 8 + PAWS_ITEMS.length + FOOT_PAWS_ITEMS.length + DOG_BED_ITEMS.length + DOG_BOWL_ITEMS.length + 1;
    }

    private static Item registerItem(String path) {
        return registerItem(path, new Item.Settings());
    }

    private static <T extends Item> T registerItem(String path, T item) {
        return Registry.register(Registries.ITEM, id(path), item);
    }

    private static Item registerItem(String path, Item.Settings settings) {
        Identifier id = id(path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Registry.register(Registries.ITEM, id, new Item(settings.registryKey(key)));
    }

    private static Item registerBlockItem(String path, Block block) {
        Identifier id = id(path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings().registryKey(key)));
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
