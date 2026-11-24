package net.luckyowlstudios.sentience.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A player-founded kingdom. Works like a vanilla team (and is backed by one, so names show in its colour):
 * a leader, members, and invitations. Founded at a Town Hall, which becomes its seat.
 * <p>
 * Read it through {@link Kingdoms}; changes go through {@link Kingdoms} or {@link KingdomData} so they are saved
 * and the team stays in sync.
 */
public final class Kingdom {

    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<Kingdom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Kingdom::id),
            Codec.STRING.fieldOf("name").forGetter(Kingdom::name),
            DyeColor.CODEC.fieldOf("color").forGetter(Kingdom::color),
            UUIDUtil.CODEC.fieldOf("founder").forGetter(Kingdom::founder),
            UUIDUtil.CODEC.optionalFieldOf("leader").forGetter(kingdom -> Optional.of(kingdom.leader)),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(kingdom -> List.copyOf(kingdom.members)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("invites", List.of()).forGetter(kingdom -> List.copyOf(kingdom.invites)),
            GlobalPos.CODEC.optionalFieldOf("town_hall").forGetter(Kingdom::townHall),
            ItemStack.CODEC.optionalFieldOf("banner").forGetter(Kingdom::banner)
    ).apply(instance, (id, name, color, founder, leader, members, invites, townHall, banner) -> {
        Kingdom kingdom = new Kingdom(id, name, color, founder, leader.orElse(founder), new LinkedHashSet<>(members), townHall.orElse(null));
        kingdom.invites.addAll(invites);
        kingdom.banner = banner.orElse(ItemStack.EMPTY);
        return kingdom;
    }));

    private final UUID id;
    private final String name;
    private DyeColor color;
    private final UUID founder;
    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> invites = new HashSet<>();
    private GlobalPos townHall;
    private ItemStack banner = ItemStack.EMPTY;

    Kingdom(UUID id, String name, DyeColor color, UUID founder, UUID leader, Set<UUID> members, GlobalPos townHall) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.founder = founder;
        this.leader = leader;
        this.members = members;
        this.townHall = townHall;
    }

    public UUID id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public DyeColor color() {
        return this.color;
    }

    public UUID founder() {
        return this.founder;
    }

    /** The player who can invite, kick and change the banner. Starts as the founder. */
    public UUID leader() {
        return this.leader;
    }

    public boolean isLeader(UUID player) {
        return this.leader.equals(player);
    }

    /** Everyone in the kingdom, leader included, in the order they joined. */
    public Set<UUID> members() {
        return Set.copyOf(this.members);
    }

    public boolean isMember(UUID player) {
        return this.members.contains(player);
    }

    public boolean isInvited(UUID player) {
        return this.invites.contains(player);
    }

    public Optional<GlobalPos> townHall() {
        return Optional.ofNullable(this.townHall);
    }

    /** The kingdom's banner, a single banner item, if the leader has set one. */
    public Optional<ItemStack> banner() {
        return this.banner.isEmpty() ? Optional.empty() : Optional.of(this.banner.copy());
    }

    /** The kingdom's name, in its colour. */
    public Component displayName() {
        return Component.literal(this.name).withStyle(style -> style.withColor(TextColor.fromRgb(this.color.getTextColor())));
    }

    // ---- Changes, used by KingdomData ----

    Set<UUID> mutableMembers() {
        return this.members;
    }

    Set<UUID> mutableInvites() {
        return this.invites;
    }

    void setLeader(UUID leader) {
        this.leader = leader;
    }

    void setColor(DyeColor color) {
        this.color = color;
    }

    void setBanner(ItemStack banner) {
        this.banner = banner.copyWithCount(1);
    }

    void setTownHall(GlobalPos townHall) {
        this.townHall = townHall;
    }

    void clearTownHall() {
        this.townHall = null;
    }
}
