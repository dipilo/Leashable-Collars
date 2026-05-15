package com.dipilodopilasaurus.leashablecollars.neoforge.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkPayloads {
    private NetworkPayloads() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(StampDeedPayload.PAYLOAD_TYPE, StampDeedPayload.STREAM_CODEC, (ignoredPayload, context) -> StampDeedPayload.handle(context));
        registrar.playToServer(UpdateCollarPayload.PAYLOAD_TYPE, UpdateCollarPayload.STREAM_CODEC, UpdateCollarPayload::handle);
        registrar.playToServer(OpenPawsConfigPayload.PAYLOAD_TYPE, OpenPawsConfigPayload.STREAM_CODEC, OpenPawsConfigPayload::handle);
    }
}
