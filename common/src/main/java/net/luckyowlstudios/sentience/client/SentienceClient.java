package net.luckyowlstudios.sentience.client;

import net.luckyowlstudios.sentience.network.OpenCreateKingdomPayload;

/** Client-side packet handling. Only ever loaded on the client. */
public final class SentienceClient {

    private SentienceClient() {
    }

    public static void handle(OpenCreateKingdomPayload payload) {
        CreateKingdomScreen.open(payload.townHall());
    }
}
