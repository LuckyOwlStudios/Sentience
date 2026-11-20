package net.luckyowlstudios.sentience.api;

import net.luckyowlstudios.sentience.impl.ReputationData;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Helpers for reading and changing a player's reputation with a race. Server side only.
 * <p>
 * Every change is clamped to the race's {@code min}/{@code max}, fires {@link ReputationEvents}, and tells an
 * online player about it in their action bar. Methods that take a {@link MinecraftServer} and {@link UUID}
 * also work for offline players.
 * <pre>{@code
 * Reputation.add(player, Races.PIGLINS, 5);
 * Reputation.remove(player, Races.VILLAGERS, 10, ReputationCause.KILLED_MEMBER);
 * if (Reputation.isAtLeast(player, Races.ILLAGERS, 50)) { ... }
 * Reputation.addForEntity(player, piglin, 2); // whichever race the piglin belongs to
 * }</pre>
 * Unknown races are ignored: getters return 0 and changes do nothing.
 */
public final class Reputation {

    private Reputation() {
    }

    // ---- Reading ----

    public static int get(ServerPlayer player, ResourceKey<Race> race) {
        return get(player.server, player.getUUID(), race);
    }

    public static int get(MinecraftServer server, UUID player, ResourceKey<Race> race) {
        return Races.get(server.registryAccess(), race)
                .map(holder -> ReputationData.get(server).get(player, race.location(), holder.value().reputation().initial()))
                .orElse(0);
    }

    /** Reputation with the race this entity belongs to, if it has one. */
    public static Optional<Integer> getForEntity(ServerPlayer player, Entity entity) {
        return Races.getRace(entity).map(holder -> get(player, holder.key()));
    }

    /** Reputation with every loaded race, in registry order. */
    public static Map<ResourceKey<Race>, Integer> getAll(ServerPlayer player) {
        Map<ResourceKey<Race>, Integer> result = new LinkedHashMap<>();
        Races.all(player.server.registryAccess()).forEach(holder -> result.put(holder.key(), get(player, holder.key())));
        return result;
    }

    public static boolean isAtLeast(ServerPlayer player, ResourceKey<Race> race, int threshold) {
        return get(player, race) >= threshold;
    }

    public static boolean isAtMost(ServerPlayer player, ResourceKey<Race> race, int threshold) {
        return get(player, race) <= threshold;
    }

    // ---- Changing ----

    /** Sets reputation, clamped to the race's bounds. Returns the new value. */
    public static int set(ServerPlayer player, ResourceKey<Race> race, int value) {
        return set(player, race, value, ReputationCause.OTHER);
    }

    public static int set(ServerPlayer player, ResourceKey<Race> race, int value, ReputationCause cause) {
        return set(player.server, player.getUUID(), race, value, cause);
    }

    public static int set(MinecraftServer server, UUID player, ResourceKey<Race> race, int value, ReputationCause cause) {
        Optional<Holder.Reference<Race>> holder = Races.get(server.registryAccess(), race);
        if (holder.isEmpty()) {
            return 0;
        }
        Race def = holder.get().value();
        ReputationData data = ReputationData.get(server);
        int oldValue = data.get(player, race.location(), def.reputation().initial());
        int newValue = def.reputation().clamp(value);
        if (newValue == oldValue) {
            return newValue;
        }
        data.set(player, race.location(), newValue);
        ReputationEvents.fireChanged(server, player, race, oldValue, newValue, cause);
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            notify(online, def, oldValue, newValue);
        }
        return newValue;
    }

    /** Adds to reputation (use a negative amount to lower it). Returns the new value. */
    public static int add(ServerPlayer player, ResourceKey<Race> race, int amount) {
        return add(player, race, amount, ReputationCause.OTHER);
    }

    public static int add(ServerPlayer player, ResourceKey<Race> race, int amount, ReputationCause cause) {
        return add(player.server, player.getUUID(), race, amount, cause);
    }

    public static int add(MinecraftServer server, UUID player, ResourceKey<Race> race, int amount, ReputationCause cause) {
        if (amount == 0) {
            return get(server, player, race);
        }
        return set(server, player, race, get(server, player, race) + amount, cause);
    }

    /** Lowers reputation by {@code amount}. Returns the new value. */
    public static int remove(ServerPlayer player, ResourceKey<Race> race, int amount) {
        return add(player, race, -amount, ReputationCause.OTHER);
    }

    public static int remove(ServerPlayer player, ResourceKey<Race> race, int amount, ReputationCause cause) {
        return add(player, race, -amount, cause);
    }

    /** Changes reputation with whichever race this entity belongs to. Returns false if it has no race. */
    public static boolean addForEntity(ServerPlayer player, Entity entity, int amount) {
        return addForEntity(player, entity, amount, ReputationCause.OTHER);
    }

    public static boolean addForEntity(ServerPlayer player, Entity entity, int amount, ReputationCause cause) {
        Optional<Holder.Reference<Race>> race = Races.getRace(entity);
        race.ifPresent(holder -> add(player, holder.key(), amount, cause));
        return race.isPresent();
    }

    /** Puts reputation back to the race's default. */
    public static void reset(ServerPlayer player, ResourceKey<Race> race) {
        set(player, race, Races.get(player.server.registryAccess(), race)
                .map(holder -> holder.value().reputation().initial()).orElse(0), ReputationCause.OTHER);
        ReputationData.get(player.server).remove(player.getUUID(), race.location());
    }

    /** Puts reputation with every race back to its default. */
    public static void resetAll(ServerPlayer player) {
        Races.all(player.server.registryAccess()).forEach(holder -> reset(player, holder.key()));
    }

    private static void notify(ServerPlayer player, Race race, int oldValue, int newValue) {
        int delta = newValue - oldValue;
        String key = delta > 0 ? "sentience.reputation.increased" : "sentience.reputation.decreased";
        player.displayClientMessage(Component.translatable(key, race.coloredName(), Math.abs(delta), newValue), true);
    }
}
