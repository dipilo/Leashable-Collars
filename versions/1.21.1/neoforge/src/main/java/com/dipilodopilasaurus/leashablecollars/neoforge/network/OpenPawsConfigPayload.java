package com.dipilodopilasaurus.leashablecollars.neoforge.network;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.PawConfigEntry;
import com.dipilodopilasaurus.leashablecollars.neoforge.client.screen.PawsConfigMenu;
import com.dipilodopilasaurus.leashablecollars.neoforge.item.PawsItem;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record OpenPawsConfigPayload(UUID targetUuid, boolean heldItems) implements CustomPacketPayload {
    public static final Type<OpenPawsConfigPayload> PAYLOAD_TYPE = new Type<>(LeashableCollarsNeoForge.id("open_paws_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPawsConfigPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            OpenPawsConfigPayload::targetUuid,
            ByteBufCodecs.BOOL,
            OpenPawsConfigPayload::heldItems,
            OpenPawsConfigPayload::new);

    @Override
    public Type<OpenPawsConfigPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(OpenPawsConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Player target = player.serverLevel().getPlayerByUUID(payload.targetUuid());
            if (target == null) {
                return;
            }

            if (LeashableCollarsNeoForge.findOwnedCollar(target, player.getUUID(), payload.targetUuid()) == null) {
                player.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_set_non_owner"), true);
                return;
            }

            List<ItemStack> pawStacks = findPawStacks(target);
            if (pawStacks.isEmpty()) {
                player.displayClientMessage(Component.translatable("item.playercollars.paw_configurator.no_paws"), true);
                return;
            }

            PawsItem pawsItem = (PawsItem) pawStacks.get(0).getItem();
            List<PawConfigEntry> initialData = payload.heldItems()
                    ? pawsItem.getHeldItemsConfig(pawStacks.get(0))
                    : pawsItem.getCanInteractConfig(pawStacks.get(0));
            List<PawConfigEntry> menuData = List.copyOf(initialData);
            Component title = Component.translatable(payload.heldItems()
                    ? "gui.playercollars.paw_configurator.item.title"
                    : "gui.playercollars.paw_configurator.block.title", target.getName());

            player.openMenu(new PawsConfigMenu.Provider(title, payload.heldItems(), menuData, pawStacks), buffer -> {
                buffer.writeBoolean(payload.heldItems());
                PawConfigEntry.writeList(buffer, menuData);
            });
        });
    }

    private static List<ItemStack> findPawStacks(Player target) {
        List<ItemStack> pawStacks = new ArrayList<>();
        for (SlotResult slotResult : CuriosApi.getCuriosInventory(target).map(handler -> handler.findCurios("hands")).orElse(List.of())) {
            if (slotResult.stack().getItem() instanceof PawsItem) {
                pawStacks.add(slotResult.stack());
            }
        }
        return pawStacks;
    }
}
