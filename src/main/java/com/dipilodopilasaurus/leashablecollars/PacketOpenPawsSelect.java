package com.dipilodopilasaurus.leashablecollars;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketOpenPawsSelect {
    private final UUID targetUuid;
    private final String targetName;

    public PacketOpenPawsSelect(UUID targetUuid, String targetName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public PacketOpenPawsSelect(FriendlyByteBuf buffer) {
        this.targetUuid = buffer.readUUID();
        this.targetName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(targetUuid);
        buffer.writeUtf(targetName);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.openPawsSelect(targetUuid, targetName)));
        context.get().setPacketHandled(true);
    }
}