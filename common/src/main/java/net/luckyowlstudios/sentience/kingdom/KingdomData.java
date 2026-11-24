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
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
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

    public Optional<Kingdom> byName(String name) {
        return this.kingdoms.values().stream().filter(kingdom -> kingdom.name().equalsIgnoreCase(name.strip())).findFirst();
    }

    Kingdom create(MinecraftServer server, String name, DyeColor color, UUID founder, GlobalPos townHall) {
        Set<UUID> members = new LinkedHashSet<>();
        members.add(founder);
        Kingdom kingdom = new Kingdom(UUID.randomUUID(), name, color, founder, founder, members, townHall);
        this.kingdoms.put(kingdom.id(), kingdom);
        this.kingdoms.values().forEach(other -> other.mutableInvites().remove(founder));
        this.setDirty();
        KingdomTeams.sync(server, kingdom);
        return kingdom;
    }

    void invite(Kingdom kingdom, UUID player) {
        kingdom.mutableInvites().add(player);
        this.setDirty();
    }

    void addMember(MinecraftServer server, Kingdom kingdom, UUID player) {
        kingdom.mutableMembers().add(player);
        // Joining one kingdom uses up every invitation.
        this.kingdoms.values().forEach(other -> other.mutableInvites().remove(player));
        this.setDirty();
        KingdomTeams.sync(server, kingdom);
    }

    /**
     * Takes a player out of their kingdom. If they led it, the longest-standing member takes over;
     * if nobody is left, the kingdom is disbanded and its Town Hall is free again.
     *
     * @return true if the kingdom was disbanded
     */
    boolean removeMember(MinecraftServer server, Kingdom kingdom, UUID player) {
        KingdomTeams.removePlayer(server, kingdom, player);
        kingdom.mutableMembers().remove(player);
        this.setDirty();
        if (kingdom.mutableMembers().isEmpty()) {
            this.kingdoms.remove(kingdom.id());
            KingdomTeams.remove(server, kingdom);
            return true;
        }
        if (kingdom.isLeader(player)) {
            kingdom.setLeader(kingdom.mutableMembers().iterator().next());
        }
        return false;
    }

    void setBanner(Kingdom kingdom, ItemStack banner) {
        kingdom.setBanner(banner);
        this.setDirty();
    }

    void setTownHall(Kingdom kingdom, GlobalPos pos) {
        kingdom.setTownHall(pos);
        this.setDirty();
    }

    /** The kingdom's Town Hall was broken: it keeps going, but needs a new one. */
    void clearTownHall(Kingdom kingdom) {
        kingdom.clearTownHall();
        this.setDirty();
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
