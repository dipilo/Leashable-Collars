package com.dipilodopilasaurus.leashablecollars.neoforge.client;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.screen.PawsConfigScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.world.level.material.MapColor;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(modid = LeashableCollarsNeoForge.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LeashableCollarsClientEvents {
    private static final int PAW_BEAN_COLOR = 0xF196CF;
    private static final DyeColor[] PAW_COLORS = {
            DyeColor.WHITE,
            DyeColor.LIGHT_GRAY,
            DyeColor.GRAY,
            DyeColor.BLACK,
            DyeColor.BLUE,
            DyeColor.RED,
            DyeColor.PURPLE
    };

    private LeashableCollarsClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    LeashableCollarsNeoForge.CLICKER_ITEM.get(),
                    ResourceLocation.fromNamespaceAndPath("minecraft", "cast"),
                    (stack, level, entity, seed) -> entity != null && entity.getUseItem() == stack ? 1.0F : 0.0F);

            PawRenderer pawRenderer = new PawRenderer();
            FootPawRenderer footPawRenderer = new FootPawRenderer();
            CollarRenderer collarRenderer = new CollarRenderer();
            CuriosRendererRegistry.register(LeashableCollarsNeoForge.COLLAR_ITEM.get(), () -> collarRenderer);
            CuriosRendererRegistry.register(LeashableCollarsNeoForge.TAGLESS_COLLAR_ITEM.get(), () -> collarRenderer);
            for (DyeColor color : PAW_COLORS) {
                registerPawRenderer(color.getName() + "_paws", pawRenderer);
                registerFootPawRenderer(color.getName() + "_foot_paws", footPawRenderer);
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(LeashableCollarsNeoForge.PAWS_CONFIG_MENU.get(), PawsConfigScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        registerCollarColors(event);
        registerPawColors(event);
        registerDogBedColors(event);
    }

    private static void registerCollarColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> switch (tintIndex) {
            case 0 -> getDyedColor(stack, MapColor.COLOR_RED.col);
            case 1 -> getMapColor(stack, MapColor.COLOR_BLUE.col);
            default -> -1;
        }, LeashableCollarsNeoForge.COLLAR_ITEM.get());

        event.register((stack, tintIndex) -> tintIndex == 0
                ? getDyedColor(stack, MapColor.COLOR_RED.col)
                : -1, LeashableCollarsNeoForge.TAGLESS_COLLAR_ITEM.get());

        event.register((stack, tintIndex) -> tintIndex == 0
                ? getDyedColor(stack, 0xFFFFFFFF)
                : -1, LeashableCollarsNeoForge.CLICKER_ITEM.get());
    }

    private static void registerPawColors(RegisterColorHandlersEvent.Item event) {
        for (DyeColor color : PAW_COLORS) {
            registerPawItemColor(event, color.getName() + "_paws", color.getFireworkColor());
            registerPawItemColor(event, color.getName() + "_foot_paws", color.getFireworkColor());
        }
    }

    private static void registerDogBedColors(RegisterColorHandlersEvent.Item event) {
        for (DyeColor color : DyeColor.values()) {
            Item bed = getItem(color.getName() + "_dog_bed");
            if (bed != null) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? toOpaqueColor(color.getFireworkColor()) : -1, bed);
            }
        }
    }

    private static void registerPawRenderer(String path, PawRenderer renderer) {
        Item item = getItem(path);
        if (item != null) {
            CuriosRendererRegistry.register(item, () -> renderer);
        }
    }

    private static void registerFootPawRenderer(String path, FootPawRenderer renderer) {
        Item item = getItem(path);
        if (item != null) {
            CuriosRendererRegistry.register(item, () -> renderer);
        }
    }

    private static void registerPawItemColor(RegisterColorHandlersEvent.Item event, String path, int defaultColor) {
        Item item = getItem(path);
        if (item != null) {
            event.register((stack, tintIndex) -> switch (tintIndex) {
                case 0 -> getDyedColor(stack, defaultColor);
                case 1 -> toOpaqueColor(PAW_BEAN_COLOR);
                default -> -1;
            }, item);
        }
    }

    private static int getDyedColor(ItemStack stack, int defaultColor) {
        DyedItemColor color = stack.get(DataComponents.DYED_COLOR);
        return toOpaqueColor(color != null ? color.rgb() : defaultColor);
    }

    private static int getMapColor(ItemStack stack, int defaultColor) {
        MapItemColor color = stack.get(DataComponents.MAP_COLOR);
        return toOpaqueColor(color != null ? color.rgb() : defaultColor);
    }

    private static int toOpaqueColor(int color) {
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    private static Item getItem(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(LeashableCollarsNeoForge.MOD_ID, path)).orElse(null);
    }
}
