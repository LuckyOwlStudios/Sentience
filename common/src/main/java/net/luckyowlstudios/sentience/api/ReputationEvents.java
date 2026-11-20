package net.luckyowlstudios.sentience.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-independent hooks into reputation changes.
 */
public final class ReputationEvents {

    private static final List<Changed> CHANGED = new CopyOnWriteArrayList<>();

    private ReputationEvents() {
    }

    /** Called after a player's reputation with a race changes. Not called when the value stays the same. */
    public static void onChanged(Changed listener) {
        CHANGED.add(listener);
    }

    public static void fireChanged(MinecraftServer server, UUID player, ResourceKey<Race> race, int oldValue, int newValue, ReputationCause cause) {
        for (Changed listener : CHANGED) {
            listener.onChanged(server, player, race, oldValue, newValue, cause);
        }
    }

    @FunctionalInterface
    public interface Changed {
        /**
         * @param player the player's UUID; they may be offline. Use {@code server.getPlayerList().getPlayer(player)}
         *               to get them if they are online.
         */
        void onChanged(MinecraftServer server, UUID player, ResourceKey<Race> race, int oldValue, int newValue, ReputationCause cause);
    }
}
