package com.dipilodopilasaurus.leashablecollars.neoforge.network;

import com.dipilodopilasaurus.leashablecollars.neoforge.LeashableCollarsNeoForge;
import com.dipilodopilasaurus.leashablecollars.neoforge.OwnerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.MapItemColor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateCollarPayload(int color, int pawColor, OwnerState ownerState) implements CustomPacketPayload {
    public static final Type<UpdateCollarPayload> PAYLOAD_TYPE = new Type<>(LeashableCollarsNeoForge.id("update_collar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateCollarPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            UpdateCollarPayload::color,
            ByteBufCodecs.INT,
            UpdateCollarPayload::pawColor,
            ByteBufCodecs.idMapper(index -> OwnerState.values()[index], OwnerState::ordinal),
            UpdateCollarPayload::ownerState,
            UpdateCollarPayload::new);

    @Override
    public Type<UpdateCollarPayload> type() {
        return PAYLOAD_TYPE;
    }

    public static void handle(UpdateCollarPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!stack.is(LeashableCollarsNeoForge.COLLAR_TAG)) {
                return;
            }

            stack.set(DataComponents.DYED_COLOR, new DyedItemColor(payload.color() & 0xFFFFFF, true));
            stack.set(DataComponents.MAP_COLOR, new MapItemColor(payload.pawColor() & 0xFFFFFF));

            if (payload.ownerState() == OwnerState.DEL) {
                LeashableCollarsNeoForge.setOwnerData(stack, null);
            } else if (payload.ownerState() == OwnerState.ADD) {
                LeashableCollarsNeoForge.setOwnerData(stack, new OwnerData(player.getUUID(), player.getName().getString()));
            }
        });
    }

    public enum OwnerState {
        NOP,
        DEL,
        ADD
    }
}
