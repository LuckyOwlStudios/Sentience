package net.luckyowlstudios.sentience.kingdom;

import com.mojang.serialization.Codec;
import net.luckyowlstudios.sentience.Constants;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every kingdom in the world, saved in the overworld's data ({@code data/sentience_kingdoms.dat}).
 */
public final class KingdomData extends SavedData {

    private static final String FILE_NAME = Constants.MOD_ID + "_kingdoms";
    private static final Codec<java.util.List<Kingdom>> LIST_CODEC = Kingdom.CODEC.listOf();

    private static final SavedData.Factory<KingdomData> FACTORY =
            new SavedData.Factory<>(KingdomData::new, KingdomData::load, null);

    private final Map<UUID, Kingdom> kingdoms = new LinkedHashMap<>();

    public static KingdomData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    public Collection<Kingdom> all() {
        return Collections.unmodifiableCollection(this.kingdoms.values());
    }

    public Optional<Kingdom> get(UUID id) {
        return Optional.ofNullable(this.kingdoms.get(id));
    }

    public Optional<Kingdom> ofPlayer(UUID player) {
        return this.kingdoms.values().stream().filter(kingdom -> kingdom.isMember(player)).findFirst();
    }

    public Optional<Kingdom> atTownHall(GlobalPos pos) {
        return this.kingdoms.values().stream().filter(kingdom -> kingdom.townHall().filter(pos::equals).isPresent()).findFirst();
    }

    public boolean isNameTaken(String name) {
        return this.kingdoms.values().stream().anyMatch(kingdom -> kingdom.name().equalsIgnoreCase(name));
    }

    Kingdom create(String name, DyeColor color, UUID founder, GlobalPos townHall) {
        HashSet<UUID> members = new HashSet<>();
        members.add(founder);
        Kingdom kingdom = new Kingdom(UUID.randomUUID(), name, color, founder, members, townHall);
        this.kingdoms.put(kingdom.id(), kingdom);
        this.setDirty();
        return kingdom;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        LIST_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), java.util.List.copyOf(this.kingdoms.values()))
                .resultOrPartial(error -> Constants.LOG.error("Couldn't save kingdoms: {}", error))
                .ifPresent(list -> tag.put("kingdoms", list));
        return tag;
    }

    private static KingdomData load(CompoundTag tag, HolderLookup.Provider registries) {
        KingdomData data = new KingdomData();
        if (tag.contains("kingdoms")) {
            LIST_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("kingdoms"))
                    .resultOrPartial(error -> Constants.LOG.error("Couldn't load kingdoms: {}", error))
                    .ifPresent(list -> list.forEach(kingdom -> data.kingdoms.put(kingdom.id(), kingdom)));
        }
        return data;
    }
}
