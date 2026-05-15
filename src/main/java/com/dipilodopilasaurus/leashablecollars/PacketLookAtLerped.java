package com.dipilodopilasaurus.leashablecollars;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketLookAtLerped extends ClientboundPlayerLookAtPacket {
    public PacketLookAtLerped(Entity entity) {
        super(EntityAnchorArgument.Anchor.EYES, entity.getX(), entity.getEyeY(), entity.getZ());
    }

    public PacketLookAtLerped(FriendlyByteBuf buffer) {
        super(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.beginClickTurn(this.getPosition(null))));
        context.get().setPacketHandled(true);
    }
}
