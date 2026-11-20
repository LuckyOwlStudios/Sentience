package net.luckyowlstudios.races.impl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.luckyowlstudios.races.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores every player's reputation, in the overworld's saved data ({@code data/races_reputation.dat}).
 * Kept per world rather than on the player so it survives death and can be changed while a player is offline.
 * Only values that were changed are stored; anything missing means the race's default.
 * <p>
 * Internal: use {@link net.luckyowlstudios.races.api.Reputation} instead.
 */
public final class ReputationData extends SavedData {

    private static final String FILE_NAME = Constants.MOD_ID + "_reputation";

    private static final SavedData.Factory<ReputationData> FACTORY =
            new SavedData.Factory<>(ReputationData::new, ReputationData::load, null);

    private final Map<UUID, Object2IntMap<ResourceLocation>> values = new HashMap<>();

    public static ReputationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    /** The player's stored values, by race id. Read-only; null when nothing is stored. */
    public Object2IntMap<ResourceLocation> stored(UUID player) {
        return this.values.get(player);
    }

    public boolean has(UUID player, ResourceLocation race) {
        Object2IntMap<ResourceLocation> map = this.values.get(player);
        return map != null && map.containsKey(race);
    }

    public int get(UUID player, ResourceLocation race, int fallback) {
        Object2IntMap<ResourceLocation> map = this.values.get(player);
        return map != null && map.containsKey(race) ? map.getInt(race) : fallback;
    }

    public void set(UUID player, ResourceLocation race, int value) {
        this.values.computeIfAbsent(player, uuid -> new Object2IntOpenHashMap<>()).put(race, value);
        this.setDirty();
    }

    public void remove(UUID player, ResourceLocation race) {
        Object2IntMap<ResourceLocation> map = this.values.get(player);
        if (map != null && map.containsKey(race)) {
            map.removeInt(race);
            if (map.isEmpty()) {
                this.values.remove(player);
            }
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        this.values.forEach((uuid, map) -> {
            CompoundTag races = new CompoundTag();
            map.object2IntEntrySet().forEach(entry -> races.putInt(entry.getKey().toString(), entry.getIntValue()));
            players.put(uuid.toString(), races);
        });
        tag.put("players", players);
        return tag;
    }

    private static ReputationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReputationData data = new ReputationData();
        CompoundTag players = tag.getCompound("players");
        for (String uuidString : players.getAllKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                Constants.LOG.warn("Skipping reputation for invalid player id {}", uuidString);
                continue;
            }
            CompoundTag races = players.getCompound(uuidString);
            Object2IntMap<ResourceLocation> map = new Object2IntOpenHashMap<>();
            for (String raceId : races.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(raceId);
                if (id != null && races.contains(raceId, Tag.TAG_INT)) {
                    map.put(id, races.getInt(raceId));
                }
            }
            if (!map.isEmpty()) {
                data.values.put(uuid, map);
            }
        }
        return data;
    }
}
