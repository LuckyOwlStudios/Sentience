package net.luckyowlstudios.sentience.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.DyeColor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A player-founded kingdom. Founded at a Town Hall, which becomes its seat.
 */
public final class Kingdom {

    public static final int MAX_NAME_LENGTH = 32;

    public static final Codec<Kingdom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Kingdom::id),
            Codec.STRING.fieldOf("name").forGetter(Kingdom::name),
            DyeColor.CODEC.fieldOf("color").forGetter(Kingdom::color),
            UUIDUtil.CODEC.fieldOf("founder").forGetter(Kingdom::founder),
            UUIDUtil.CODEC.listOf().fieldOf("members").forGetter(kingdom -> List.copyOf(kingdom.members)),
            GlobalPos.CODEC.optionalFieldOf("town_hall").forGetter(Kingdom::townHall)
    ).apply(instance, (id, name, color, founder, members, townHall) ->
            new Kingdom(id, name, color, founder, new HashSet<>(members), townHall.orElse(null))));

    private final UUID id;
    private String name;
    private DyeColor color;
    private final UUID founder;
    private final Set<UUID> members;
    private GlobalPos townHall;

    Kingdom(UUID id, String name, DyeColor color, UUID founder, Set<UUID> members, GlobalPos townHall) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.founder = founder;
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

    /** Everyone in the kingdom, founder included. */
    public Set<UUID> members() {
        return Set.copyOf(this.members);
    }

    public boolean isMember(UUID player) {
        return this.members.contains(player);
    }

    public Optional<GlobalPos> townHall() {
        return Optional.ofNullable(this.townHall);
    }

    /** The kingdom's name, in its colour. */
    public Component displayName() {
        return Component.literal(this.name).withStyle(style -> style.withColor(TextColor.fromRgb(this.color.getTextColor())));
    }

    Set<UUID> mutableMembers() {
        return this.members;
    }
}
