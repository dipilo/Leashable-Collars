package com.dipilodopilasaurus.leashablecollars.neoforge.network;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StampDeedPayload() implements CustomPacketPayload {
    public static final Type<StampDeedPayload> PAYLOAD_TYPE = new Type<>(LeashableCollarsNeoForge.id("stamp_deed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StampDeedPayload> STREAM_CODEC = StreamCodec.unit(new StampDeedPayload());

    @Override
    public Type<StampDeedPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!stack.is(LeashableCollarsNeoForge.DEED_OF_OWNERSHIP.get())) {
                return;
            }

            OwnerData ownerData = LeashableCollarsNeoForge.getOwnerData(stack);
            if (ownerData == null || ownerData.owned().isPresent()) {
                return;
            }

            if (ownerData.uuid().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.no_self_own"), true);
                return;
            }

            ItemStack stamped = new ItemStack(LeashableCollarsNeoForge.STAMPED_DEED_OF_OWNERSHIP.get());
            LeashableCollarsNeoForge.setOwnerData(stamped, ownerData.withOwned(player.getUUID(), player.getName().getString()));
            player.getInventory().setItem(player.getInventory().selected, stamped);
            player.displayClientMessage(Component.translatable("item.playercollars.deed_of_ownership.stamped", player.getName()), true);
        });
    }
}
