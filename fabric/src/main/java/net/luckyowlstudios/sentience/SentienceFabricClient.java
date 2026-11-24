package net.luckyowlstudios.sentience;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.luckyowlstudios.sentience.client.SentienceClient;
import net.luckyowlstudios.sentience.network.OpenCreateKingdomPayload;

public class SentienceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenCreateKingdomPayload.TYPE, (payload, context) -> SentienceClient.handle(payload));
    }
}
