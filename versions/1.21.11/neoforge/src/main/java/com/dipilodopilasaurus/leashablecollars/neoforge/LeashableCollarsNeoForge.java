package com.dipilodopilasaurus.leashablecollars.neoforge;

import com.dipilodopilasaurus.leashablecollars.neoforge.block.DogBedBlock;
import com.dipilodopilasaurus.leashablecollars.neoforge.block.DogBowlBlock;
import com.dipilodopilasaurus.leashablecollars.neoforge.block.InvisibleFenceBlock;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.ClickerItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.CollarLockerItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.CollarItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.DeedItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.FootPawsItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.PawsItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.PawConfiguratorItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.SpatulaItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.StampedDeedItem;
import com.dipilodopilasaurus.leashablecollars.neoforge.leash.LeashProxyEntity;
import com.dipilodopilasaurus.leashablecollars.neoforge.leash.PlayerLeashHandler;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod(LeashableCollarsNeoForge.MOD_ID)
public final class LeashableCollarsNeoForge {
    public static final String MOD_ID = "playercollars";
    private static final Logger LOGGER = LoggerFactory.getLogger("playercollars-neoforge");
    public static final TagKey<Item> COLLAR_TAG = ItemTags.create(Identifier.fromNamespaceAndPath("c", "collars"));
    public static final TagKey<Item> PAWS_TAG = ItemTags.create(id("paws"));
    public static final TagKey<Item> FOOT_PAWS_TAG = ItemTags.create(id("foot_paws"));
    public static final TagKey<Block> PAWS_ALLOW_INTERACT = BlockTags.create(id("paws_allow_interact"));
    public static final GameRule<Boolean> PLAYER_LEASHES_BREAK_RULE = GameRules.registerBoolean("playerLeashesBreak", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> ALLOW_UNLEASH_OTHER = GameRules.registerBoolean("allowUnleashUnownedPlayer", GameRuleCategory.PLAYER, true);
    public static final GameRule<Boolean> ALLOW_ATTACK_OWNER = GameRules.registerBoolean("playerAllowAttackOwner", GameRuleCategory.PLAYER, false);

    private static final DyeColor[] PAWS_DYE_COLORS = {
            DyeColor.WHITE,
            DyeColor.LIGHT_GRAY,
            DyeColor.GRAY,
            DyeColor.BLACK,
            DyeColor.BLUE,
            DyeColor.RED,
            DyeColor.PURPLE
    };

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    public static final Supplier<DataComponentType<OwnerData>> OWNER_DATA = DATA_COMPONENTS.registerComponentType("owner", builder -> builder.persistent(OwnerData.CODEC));
    public static final Supplier<DataComponentType<Boolean>> LOCKED = DATA_COMPONENTS.registerComponentType("locked", builder -> builder.persistent(Codec.BOOL));
    public static final Supplier<DataComponentType<List<String>>> PAWS_ALLOWED_BLOCKS = DATA_COMPONENTS.registerComponentType("paws_allowed_blocks", builder -> builder.persistent(Codec.STRING.listOf()));
    public static final Supplier<DataComponentType<Boolean>> PAWS_STRICT_MODE = DATA_COMPONENTS.registerComponentType("paws_strict_mode", builder -> builder.persistent(Codec.BOOL));

    public static final Supplier<SoundEvent> CLICKER_ON = SOUNDS.register("clicker_on", () -> SoundEvent.createVariableRangeEvent(id("clicker_on")));
    public static final Supplier<SoundEvent> CLICKER_OFF = SOUNDS.register("clicker_off", () -> SoundEvent.createVariableRangeEvent(id("clicker_off")));

    public static final Supplier<CollarItem> COLLAR_ITEM = ITEMS.registerItem("collar", properties -> new CollarItem(properties, false));
    public static final Supplier<CollarItem> TAGLESS_COLLAR_ITEM = ITEMS.registerItem("tagless_collar", properties -> new CollarItem(properties, true));
    public static final Supplier<Item> CLICKER_ITEM = ITEMS.registerItem("clicker", ClickerItem::new);
    public static final Supplier<Item> DEED_OF_OWNERSHIP = ITEMS.registerItem("deed_of_ownership", DeedItem::new);
    public static final Supplier<Item> STAMPED_DEED_OF_OWNERSHIP = ITEMS.registerItem("stamped_deed_of_ownership", StampedDeedItem::new);
    public static final Supplier<Item> PAW_CONFIGURATION_ITEM = ITEMS.registerItem("paw_configurator", PawConfiguratorItem::new);
    public static final Supplier<Item> COLLAR_LOCKER_ITEM = ITEMS.registerItem("collar_locker", CollarLockerItem::new);
    public static final Supplier<Item> SPATULA_ITEM = ITEMS.registerItem("golden_spatula", SpatulaItem::new);

    @SuppressWarnings("unchecked")
    private static final Supplier<Item>[] PAWS_ITEMS = new Supplier[PAWS_DYE_COLORS.length];
    @SuppressWarnings("unchecked")
    private static final Supplier<Item>[] FOOT_PAWS_ITEMS = new Supplier[PAWS_DYE_COLORS.length];
    @SuppressWarnings("unchecked")
    private static final Supplier<DogBedBlock>[] DOG_BEDS = new Supplier[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final Supplier<BedItem>[] DOG_BED_ITEMS = new Supplier[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final Supplier<DogBowlBlock>[] DOG_BOWLS = new Supplier[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final Supplier<BlockItem>[] DOG_BOWL_ITEMS = new Supplier[DyeColor.values().length];

    public static final Supplier<InvisibleFenceBlock> INVISIBLE_FENCE_BLOCK = BLOCKS.registerBlock("invisible_fence", InvisibleFenceBlock::new);
    public static final Supplier<BlockItem> INVISIBLE_FENCE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("invisible_fence", INVISIBLE_FENCE_BLOCK);
    public static final Supplier<BlockEntityType<DogBowlBlock.DogBowlBlockEntity>> DOG_BOWL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("dog_bowl", () ->
            new BlockEntityType<>(DogBowlBlock.DogBowlBlockEntity::new, Arrays.stream(DOG_BOWLS).map(Supplier::get).toArray(Block[]::new)));

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register(MOD_ID, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.playercollars"))
            .icon(() -> new ItemStack(COLLAR_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(COLLAR_ITEM.get());
                output.accept(TAGLESS_COLLAR_ITEM.get());
                output.accept(CLICKER_ITEM.get());
                output.accept(DEED_OF_OWNERSHIP.get());
                output.accept(STAMPED_DEED_OF_OWNERSHIP.get());
                output.accept(PAW_CONFIGURATION_ITEM.get());
                output.accept(COLLAR_LOCKER_ITEM.get());
                output.accept(SPATULA_ITEM.get());
                for (Supplier<Item> item : PAWS_ITEMS) {
                    output.accept(item.get());
                }
                for (Supplier<Item> item : FOOT_PAWS_ITEMS) {
                    output.accept(item.get());
                }
                for (Supplier<BedItem> item : DOG_BED_ITEMS) {
                    output.accept(item.get());
                }
                for (Supplier<BlockItem> item : DOG_BOWL_ITEMS) {
                    output.accept(item.get());
                }
                output.accept(INVISIBLE_FENCE_BLOCK_ITEM.get());
            })
            .build());

    static {
        for (int i = 0; i < PAWS_DYE_COLORS.length; i++) {
            DyeColor color = PAWS_DYE_COLORS[i];
            String colorName = color.getName();
            PAWS_ITEMS[i] = ITEMS.registerItem(colorName + "_paws", properties -> new PawsItem(properties, color.getTextureDiffuseColor()));
            FOOT_PAWS_ITEMS[i] = ITEMS.registerItem(colorName + "_foot_paws", properties -> new FootPawsItem(properties, color.getTextureDiffuseColor()));
        }

        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getName();
            DOG_BEDS[color.ordinal()] = BLOCKS.registerBlock(colorName + "_dog_bed", properties -> new DogBedBlock(color, properties));
            DOG_BED_ITEMS[color.ordinal()] = ITEMS.registerItem(colorName + "_dog_bed", properties -> new BedItem(DOG_BEDS[color.ordinal()].get(), properties.stacksTo(1)));
            DOG_BOWLS[color.ordinal()] = BLOCKS.registerBlock(colorName + "_dog_bowl", properties -> new DogBowlBlock(color, properties));
            DOG_BOWL_ITEMS[color.ordinal()] = ITEMS.registerSimpleBlockItem(colorName + "_dog_bowl", DOG_BOWLS[color.ordinal()]);
        }
    }

    public LeashableCollarsNeoForge(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onAttackOwner);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onAttackLeashKnot);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(PawsEventHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerLeashHandler::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(PlayerLeashHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerLeashHandler::onBlockBreak);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LOGGER.info("Registered Leashable Collars NeoForge content with Curios support: {} items, {} beds, {} bowls", countRegisteredItems(), DOG_BED_ITEMS.length, DOG_BOWL_ITEMS.length));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Nullable
    public static OwnerData getOwnerData(ItemStack stack) {
        return stack.get(OWNER_DATA.get());
    }

    public static void setOwnerData(ItemStack stack, @Nullable OwnerData ownerData) {
        if (ownerData == null) {
            stack.remove(OWNER_DATA.get());
        } else {
            stack.set(OWNER_DATA.get(), ownerData);
        }
    }

    public static boolean isLocked(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(LOCKED.get()));
    }

    public static void setLocked(ItemStack stack, boolean locked) {
        if (locked) {
            stack.set(LOCKED.get(), true);
        } else {
            stack.remove(LOCKED.get());
        }
    }

    public static boolean blockLeashKnotBreak(Player player, LeashFenceKnotEntity knot) {
        for (LeashProxyEntity proxy : knot.level().getEntitiesOfClass(LeashProxyEntity.class, knot.getBoundingBox().inflate(7.0D), entity -> knot.equals(entity.getLeashHolder()))) {
            if (!proxy.canUnleash(player)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAttachedFenceBreak(Player player, BlockPos pos) {
        return PlayerLeashHandler.isAttachedFenceBreak(player, pos);
    }

    @Nullable
    public static ItemStack findOwnedCollar(net.minecraft.world.entity.LivingEntity entity, UUID ownerUuid) {
        return findOwnedCollar(entity, ownerUuid, entity.getUUID());
    }

    @Nullable
    public static ItemStack findOwnedCollar(net.minecraft.world.entity.LivingEntity entity, UUID ownerUuid, UUID ownedUuid) {
        Optional<ItemStack> result = CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(stack -> isOwnedBy(stack, ownerUuid, ownedUuid)).map(slot -> slot.stack().copy()));
        return result.orElse(null);
    }

    private static boolean isOwnedBy(ItemStack stack, UUID ownerUuid, UUID ownedUuid) {
        if (!stack.is(COLLAR_TAG)) {
            return false;
        }
        OwnerData ownerData = getOwnerData(stack);
        if (ownerData == null || !ownerData.uuid().equals(ownerUuid)) {
            return false;
        }
        return ownerData.owned().isEmpty() || ownerData.owned().get().equals(ownedUuid);
    }

    private static int countRegisteredItems() {
        return 8 + PAWS_ITEMS.length + FOOT_PAWS_ITEMS.length + DOG_BED_ITEMS.length + DOG_BOWL_ITEMS.length + 1;
    }
}
