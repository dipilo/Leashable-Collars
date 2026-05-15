package com.dipilodopilasaurus.leashablecollars.client;

import com.dipilodopilasaurus.leashablecollars.LeashableCollars;
import com.dipilodopilasaurus.leashablecollars.client.screen.PawsConfigScreen;
import com.dipilodopilasaurus.leashablecollars.item.ClickerItem;
import com.dipilodopilasaurus.leashablecollars.item.CollarItem;
import com.dipilodopilasaurus.leashablecollars.item.FootPawsItem;
import com.dipilodopilasaurus.leashablecollars.item.PawsItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RegisterClient {
    private RegisterClient() {
    }

    @SubscribeEvent
    public static void onModelBakeEvent(ModelEvent.BakingCompleted event) {
        ModelResourceLocation location = new ModelResourceLocation(new ResourceLocation(LeashableCollars.MOD_ID, "collar"), "inventory");
        CollarRenderer renderer = new CollarRenderer(event.getModels().get(location));
        CuriosRendererRegistry.register(LeashableCollars.COLLAR_ITEM.get(), () -> renderer);
        CuriosRendererRegistry.register(LeashableCollars.TAGLESS_COLLAR_ITEM.get(), () -> renderer);

        PawRenderer pawRenderer = new PawRenderer();
        for (var paws : LeashableCollars.getPawsItems()) {
            CuriosRendererRegistry.register(paws.get(), () -> pawRenderer);
        }

        FootPawRenderer footPawRenderer = new FootPawRenderer();
        for (var footPaws : LeashableCollars.getFootPawsItems()) {
            CuriosRendererRegistry.register(footPaws.get(), () -> footPawRenderer);
        }
    }

    @SubscribeEvent
    public static void propertyOverrideRegistry(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(LeashableCollars.CLICKER_ITEM.get(), new ResourceLocation("cast"),
                    (stack, level, entity, seed) -> entity != null && entity.getUseItem() == stack ? 1.0F : 0.0F);
            MenuScreens.register(LeashableCollars.PAWS_CONFIG_MENU.get(), PawsConfigScreen::new);
        });
        MinecraftForge.EVENT_BUS.register(RotationLerpHandler.class);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        CollarItem collarItem = LeashableCollars.COLLAR_ITEM.get();
        event.register((stack, tintIndex) -> switch (tintIndex) {
            case 0 -> collarItem.getColor(stack);
            case 1 -> collarItem.getPawColor(stack);
            default -> -1;
        }, collarItem);
        CollarItem taglessCollar = LeashableCollars.TAGLESS_COLLAR_ITEM.get();
        event.register((stack, tintIndex) -> tintIndex == 0 ? taglessCollar.getColor(stack) : -1, taglessCollar);

        ClickerItem clickerItem = LeashableCollars.CLICKER_ITEM.get();
        event.register((stack, tintIndex) -> tintIndex == 0 ? clickerItem.getColor(stack) : -1, clickerItem);

        for (var paws : LeashableCollars.getPawsItems()) {
            PawsItem pawsItem = paws.get();
            event.register((stack, tintIndex) -> switch (tintIndex) {
                case 0 -> pawsItem.getColor(stack);
                case 1 -> pawsItem.getBeansColor(stack);
                default -> -1;
            }, pawsItem);
        }
        for (var footPaws : LeashableCollars.getFootPawsItems()) {
            FootPawsItem footPawsItem = footPaws.get();
            event.register((stack, tintIndex) -> switch (tintIndex) {
                case 0 -> footPawsItem.getColor(stack);
                case 1 -> footPawsItem.getBeansColor(stack);
                default -> -1;
            }, footPawsItem);
        }
        for (DyeColor color : DyeColor.values()) {
            event.register((stack, tintIndex) -> tintIndex == 0 ? color.getFireworkColor() : -1,
                    LeashableCollars.getDogBedItem(color).get());
        }
    }
}
