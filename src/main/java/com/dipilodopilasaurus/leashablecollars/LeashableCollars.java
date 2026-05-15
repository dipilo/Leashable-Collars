package com.dipilodopilasaurus.leashablecollars;

import com.dipilodopilasaurus.leashablecollars.block.DogBedBlock;
import com.dipilodopilasaurus.leashablecollars.block.DogBowlBlock;
import com.dipilodopilasaurus.leashablecollars.block.InvisibleFenceBlock;
import com.dipilodopilasaurus.leashablecollars.client.screen.PawsConfigMenu;
import com.dipilodopilasaurus.leashablecollars.enchantment.ClickerEnchantment;
import com.dipilodopilasaurus.leashablecollars.enchantment.CollarEnchantment;
import com.dipilodopilasaurus.leashablecollars.item.ClickerItem;
import com.dipilodopilasaurus.leashablecollars.item.CollarLockerItem;
import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import com.dipilodopilasaurus.leashablecollars.item.DeedItem;
import com.dipilodopilasaurus.leashablecollars.item.FootPawsItem;
import com.dipilodopilasaurus.leashablecollars.item.OwnershipCraftingRecipe;
import com.dipilodopilasaurus.leashablecollars.item.PawSetupItem;
import com.dipilodopilasaurus.leashablecollars.item.PawsItem;
import com.dipilodopilasaurus.leashablecollars.item.SpatulaItem;
import com.dipilodopilasaurus.leashablecollars.item.StampedDeedItem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.dipilodopilasaurus.leashablecollars.leash.LeashImpl;
import com.dipilodopilasaurus.leashablecollars.leash.LeashProxyEntity;

@Mod(LeashableCollars.MOD_ID)
public class LeashableCollars {
    public static final String MOD_ID = "playercollars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final TagKey<Item> COLLAR_TAG = ItemTags.create(new ResourceLocation("c", "collars"));
    public static final TagKey<Item> PAWS_TAG = ItemTags.create(id("paws"));
    public static final TagKey<Item> FOOT_PAWS_TAG = ItemTags.create(id("foot_paws"));
    public static final TagKey<Block> PAWS_ALLOW_INTERACT = BlockTags.create(id("paws_allow_interact"));
    public static final GameRules.Key<GameRules.BooleanValue> PLAYER_LEASHES_BREAK_RULE = GameRules.register("playerLeashesBreak", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_UNLEASH_OTHER = GameRules.register("allowUnleashUnownedPlayer", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_ATTACK_OWNER = GameRules.register("playerAllowAttackOwner", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);

    public static final RegistryObject<CollarItem> COLLAR_ITEM = ITEMS.register("collar", CollarItem::new);
    public static final RegistryObject<CollarItem> TAGLESS_COLLAR_ITEM = ITEMS.register("tagless_collar", () -> new CollarItem(true));
    public static final RegistryObject<ClickerItem> CLICKER_ITEM = ITEMS.register("clicker", ClickerItem::new);
    public static final RegistryObject<Item> DEED_OF_OWNERSHIP = ITEMS.register("deed_of_ownership", DeedItem::new);
    public static final RegistryObject<Item> STAMPED_DEED_OF_OWNERSHIP = ITEMS.register("stamped_deed_of_ownership", StampedDeedItem::new);
    public static final RegistryObject<Item> PAW_CONFIGURATION_ITEM = ITEMS.register("paw_configurator", PawSetupItem::new);
    public static final RegistryObject<Item> COLLAR_LOCKER_ITEM = ITEMS.register("collar_locker", CollarLockerItem::new);
    public static final RegistryObject<Item> SPATULA_ITEM = ITEMS.register("golden_spatula", SpatulaItem::new);

    @SuppressWarnings("unchecked")
    private static final RegistryObject<PawsItem>[] PAWS_ITEMS = new RegistryObject[7];
    @SuppressWarnings("unchecked")
    private static final RegistryObject<FootPawsItem>[] FOOT_PAWS_ITEMS = new RegistryObject[7];
    private static final DyeColor[] PAWS_DYE_COLORS = new DyeColor[]{DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BLUE, DyeColor.RED, DyeColor.PURPLE};

    @SuppressWarnings("unchecked")
    private static final RegistryObject<DogBedBlock>[] DOG_BEDS = new RegistryObject[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] DOG_BED_ITEMS = new RegistryObject[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final RegistryObject<DogBowlBlock>[] DOG_BOWLS = new RegistryObject[DyeColor.values().length];
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] DOG_BOWL_ITEMS = new RegistryObject[DyeColor.values().length];

    public static final RegistryObject<InvisibleFenceBlock> INVISIBLE_FENCE_BLOCK = BLOCKS.register("invisible_fence", InvisibleFenceBlock::new);
    public static final RegistryObject<Item> INVISIBLE_FENCE_BLOCK_ITEM = ITEMS.register("invisible_fence", () -> new BlockItem(INVISIBLE_FENCE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<DogBowlBlock.DogBowlBlockEntity>> DOG_BOWL_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("dog_bowl", () ->
            BlockEntityType.Builder.of(DogBowlBlock.DogBowlBlockEntity::new, Arrays.stream(DOG_BOWLS).map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<?>> OWNERSHIP_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("owner_transfer", OwnershipCraftingRecipe.Serializer::new);
        public static final RegistryObject<MenuType<PawsConfigMenu>> PAWS_CONFIG_MENU = MENU_TYPES.register("paws_config", () -> IForgeMenuType.create(PawsConfigMenu::fromNetwork));
    public static final RegistryObject<Enchantment> SHORT_LEASH_ENCHANTMENT = ENCHANTMENTS.register("short_leash", () -> new CollarEnchantment(Enchantment.Rarity.UNCOMMON, 2));
    public static final RegistryObject<Enchantment> REGENERATION_ENCHANTMENT = ENCHANTMENTS.register("regeneration", () -> new CollarEnchantment(Enchantment.Rarity.RARE, 1));
    public static final RegistryObject<Enchantment> THORNS_ENCHANTMENT = ENCHANTMENTS.register("thorns", () -> new CollarEnchantment(Enchantment.Rarity.VERY_RARE, 3));
    public static final RegistryObject<Enchantment> CLICKER_ENCHANTMENT = ENCHANTMENTS.register("clicker", () -> new ClickerEnchantment(Enchantment.Rarity.UNCOMMON));
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register(MOD_ID, () -> CreativeModeTab.builder()
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
                Arrays.stream(PAWS_ITEMS).map(RegistryObject::get).forEach(output::accept);
                Arrays.stream(FOOT_PAWS_ITEMS).map(RegistryObject::get).forEach(output::accept);
                Arrays.stream(DOG_BED_ITEMS).map(RegistryObject::get).forEach(output::accept);
                Arrays.stream(DOG_BOWL_ITEMS).map(RegistryObject::get).forEach(output::accept);
                output.accept(INVISIBLE_FENCE_BLOCK_ITEM.get());
            })
            .build());

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final RegistryObject<SoundEvent> CLICKER_ON = SOUNDS.register("clicker_on", () -> SoundEvent.createVariableRangeEvent(id("clicker_on")));
    public static final RegistryObject<SoundEvent> CLICKER_OFF = SOUNDS.register("clicker_off", () -> SoundEvent.createVariableRangeEvent(id("clicker_off")));

    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(id("collar_channel"), () -> "", String::isEmpty, String::isEmpty);

    static {
        for (int i = 0; i < PAWS_DYE_COLORS.length; i++) {
            DyeColor color = PAWS_DYE_COLORS[i];
            int pawColor = 0xF196CF;
            PAWS_ITEMS[i] = ITEMS.register(color.getName() + "_paws", () -> new PawsItem(color.getFireworkColor(), pawColor));
            FOOT_PAWS_ITEMS[i] = ITEMS.register(color.getName() + "_foot_paws", () -> new FootPawsItem(color.getFireworkColor(), pawColor));
        }

        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();
            DOG_BEDS[color.ordinal()] = BLOCKS.register(name + "_dog_bed", () -> new DogBedBlock(color));
            DOG_BED_ITEMS[color.ordinal()] = ITEMS.register(name + "_dog_bed", () -> new BedItem(DOG_BEDS[color.ordinal()].get(), new Item.Properties().stacksTo(1)));
            DOG_BOWLS[color.ordinal()] = BLOCKS.register(name + "_dog_bowl", () -> new DogBowlBlock(color));
            DOG_BOWL_ITEMS[color.ordinal()] = ITEMS.register(name + "_dog_bowl", () -> new BlockItem(DOG_BOWLS[color.ordinal()].get(), new Item.Properties()));
        }
    }

    public LeashableCollars() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
        MENU_TYPES.register(eventBus);
        ENCHANTMENTS.register(eventBus);
        SOUNDS.register(eventBus);
        MinecraftForge.EVENT_BUS.addListener(this::blockAttachedFenceBreak);

        NETWORK.registerMessage(1, PacketUpdateCollar.class, PacketUpdateCollar::encode, PacketUpdateCollar::new, PacketUpdateCollar::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        NETWORK.registerMessage(2, PacketLookAtLerped.class, PacketLookAtLerped::write, PacketLookAtLerped::new, PacketLookAtLerped::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NETWORK.registerMessage(3, PacketOpenPawsSelect.class, PacketOpenPawsSelect::encode, PacketOpenPawsSelect::new, PacketOpenPawsSelect::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NETWORK.registerMessage(4, PacketOpenPawsConfig.class, PacketOpenPawsConfig::encode, PacketOpenPawsConfig::new, PacketOpenPawsConfig::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        NETWORK.registerMessage(5, PacketStampDeed.class, PacketStampDeed::encode, PacketStampDeed::new, PacketStampDeed::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static ItemStack filterStacksByOwner(IDynamicStackHandler stacks, UUID playerUuid) {
        return filterStacksByOwner(stacks, playerUuid, null);
    }

    public static List<RegistryObject<PawsItem>> getPawsItems() {
        return List.of(PAWS_ITEMS);
    }

    public static List<RegistryObject<FootPawsItem>> getFootPawsItems() {
        return List.of(FOOT_PAWS_ITEMS);
    }

    public static RegistryObject<Item> getDogBedItem(DyeColor color) {
        return DOG_BED_ITEMS[color.ordinal()];
    }

    public static ItemStack filterStacksByOwner(IDynamicStackHandler stacks, UUID playerUuid, UUID ownedUuid) {
        for (int i = 0; i < stacks.getSlots(); i++) {
            ItemStack stack = stacks.getStackInSlot(i);
            if (stack.getItem() instanceof CollarItem item) {
                Pair<UUID, String> owner = item.getOwner(stack);
                if (owner == null || !owner.getFirst().equals(playerUuid)) {
                    continue;
                }
                OwnerData ownerData = CollarItem.getOwnerData(stack);
                if (ownerData == null || ownerData.owned().isEmpty() || ownedUuid == null || ownerData.owned().get().equals(ownedUuid)) {
                    return stack;
                }
            }
        }
        return null;
    }

    public static ItemStack findOwnedCollar(LivingEntity entity, UUID ownerUuid) {
        return findOwnedCollar(entity, ownerUuid, entity.getUUID());
    }

    public static ItemStack findOwnedCollar(LivingEntity entity, UUID ownerUuid, UUID ownedUuid) {
        return CuriosApi.getCuriosInventory(entity).resolve().flatMap(handler -> handler.getStacksHandler("necklace").map(slot -> {
            ItemStack stack = filterStacksByOwner(slot.getStacks(), ownerUuid, ownedUuid);
            if (stack == null) {
                stack = filterStacksByOwner(slot.getCosmeticStacks(), ownerUuid, ownedUuid);
            }
            return stack;
        })).orElse(null);
    }

    public static boolean blockLeashKnotBreak(Player player, LeashFenceKnotEntity knot) {
        for (LeashProxyEntity proxy : knot.level().getEntitiesOfClass(LeashProxyEntity.class, knot.getBoundingBox().inflate(7.0D), entity -> knot.equals(entity.getLeashHolder()))) {
            if (!proxy.canUnleash(player)) {
                return true;
            }
        }

        return false;
    }

    public void blockAttachedFenceBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        for (LeashFenceKnotEntity knot : player.level().getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(event.getPos()), entity -> event.getPos().equals(entity.getPos()))) {
            if (blockLeashKnotBreak(player, knot)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
