package net.luckyowlstudios.races.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * A race: a group of creatures that share one reputation with each player.
 * <p>
 * Races are loaded from datapacks, one file per race at
 * {@code data/<namespace>/races/race/<name>.json}. The file's location is the race's id,
 * so {@code data/mymod/races/race/elves.json} defines the race {@code mymod:elves}.
 * <pre>{@code
 * {
 *   "name": { "translate": "race.mymod.elves" },
 *   "color": "#7FD17F",
 *   "members": "#mymod:elves",
 *   "reputation": { "default": 0, "min": -100, "max": 100 },
 *   "changes": { "killed_member": -10, "traded": 1, "raid_victory": 0 }
 * }
 * }</pre>
 * Everything except {@code members} is optional.
 *
 * @param name               display name
 * @param color              colour used when the name is shown
 * @param members            entity types (a tag, a single id, or a list) that belong to this race
 * @param reputation         starting value and bounds of a player's reputation with this race
 * @param changes            how much built-in actions change reputation with this race
 */
public record Race(Component name, TextColor color, HolderSet<EntityType<?>> members,
                   ReputationRange reputation, ReputationChanges changes) {

    public static final Codec<Race> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.optionalFieldOf("name", Component.literal("?")).forGetter(Race::name),
            TextColor.CODEC.optionalFieldOf("color", TextColor.fromRgb(0xFFFFFF)).forGetter(Race::color),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("members").forGetter(Race::members),
            ReputationRange.CODEC.optionalFieldOf("reputation", ReputationRange.DEFAULT).forGetter(Race::reputation),
            ReputationChanges.CODEC.optionalFieldOf("changes", ReputationChanges.NONE).forGetter(Race::changes)
    ).apply(instance, Race::new));

    /** Whether this entity type is listed as a member. Prefer {@link Races#getRace(Entity)} for entities. */
    public boolean hasMember(EntityType<?> type) {
        Holder<EntityType<?>> holder = type.builtInRegistryHolder();
        return this.members.contains(holder);
    }

    /** The name, coloured with this race's colour. */
    public Component coloredName() {
        return this.name.copy().withStyle(style -> style.withColor(this.color));
    }

    /**
     * Starting reputation and the bounds every change is clamped to.
     */
    public record ReputationRange(int initial, int min, int max) {

        public static final ReputationRange DEFAULT = new ReputationRange(0, -100, 100);

        public static final Codec<ReputationRange> CODEC = RecordCodecBuilder.<ReputationRange>create(instance -> instance.group(
                Codec.INT.optionalFieldOf("default", DEFAULT.initial).forGetter(ReputationRange::initial),
                Codec.INT.optionalFieldOf("min", DEFAULT.min).forGetter(ReputationRange::min),
                Codec.INT.optionalFieldOf("max", DEFAULT.max).forGetter(ReputationRange::max)
        ).apply(instance, ReputationRange::new)).validate(range -> range.min <= range.initial && range.initial <= range.max
                ? DataResult.success(range)
                : DataResult.error(() -> "Reputation must satisfy min <= default <= max, got " + range));

        public int clamp(int value) {
            return Mth.clamp(value, this.min, this.max);
        }
    }

    /**
     * Reputation changes applied by the mod's built-in triggers. Positive values raise reputation,
     * negative values lower it, zero turns the trigger off for this race.
     *
     * @param killedMember applied when a player kills a member of this race
     * @param traded       applied when a player completes a trade with, or barters with, a member of this race
     * @param raidVictory  applied to every hero of the village when a raid is defeated
     */
    public record ReputationChanges(int killedMember, int traded, int raidVictory) {

        public static final ReputationChanges NONE = new ReputationChanges(0, 0, 0);

        public static final Codec<ReputationChanges> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("killed_member", 0).forGetter(ReputationChanges::killedMember),
                Codec.INT.optionalFieldOf("traded", 0).forGetter(ReputationChanges::traded),
                Codec.INT.optionalFieldOf("raid_victory", 0).forGetter(ReputationChanges::raidVictory)
        ).apply(instance, ReputationChanges::new));
    }
}
