package net.luckyowlstudios.sentience.api;

import net.luckyowlstudios.sentience.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Entry point for looking up races.
 * <p>
 * Races live in a datapack registry ({@link #REGISTRY}), so the simplest way to add one is a JSON file
 * at {@code data/<namespace>/sentience/race/<name>.json} (see {@link Race}). Which race an entity belongs to is
 * decided in this order:
 * <ol>
 *     <li>the entity implements {@link RaceMember} and returns a race;</li>
 *     <li>a resolver added with {@link #registerResolver(Function)} returns a race;</li>
 *     <li>the entity's type is listed in a race's {@code members}.</li>
 * </ol>
 */
public final class Races {

    /** The datapack registry holding every race. */
    public static final ResourceKey<Registry<Race>> REGISTRY = ResourceKey.createRegistryKey(Constants.id("race"));

    public static final ResourceKey<Race> VILLAGERS = key(Constants.id("villagers"));
    public static final ResourceKey<Race> ILLAGERS = key(Constants.id("illagers"));
    public static final ResourceKey<Race> PIGLINS = key(Constants.id("piglins"));

    private static final List<Function<Entity, ResourceKey<Race>>> RESOLVERS = new CopyOnWriteArrayList<>();

    private Races() {
    }

    public static ResourceKey<Race> key(ResourceLocation id) {
        return ResourceKey.create(REGISTRY, id);
    }

    /**
     * Adds a resolver that can place entities in a race in code, e.g. based on NBT or a variant.
     * Return {@code null} to let the next resolver (or the race's {@code members}) decide.
     */
    public static void registerResolver(Function<Entity, ResourceKey<Race>> resolver) {
        RESOLVERS.add(resolver);
    }

    public static Optional<Registry<Race>> registry(RegistryAccess access) {
        return access.registry(REGISTRY);
    }

    /** Every loaded race. */
    public static Stream<Holder.Reference<Race>> all(RegistryAccess access) {
        return registry(access).stream().flatMap(Registry::holders);
    }

    public static Optional<Holder.Reference<Race>> get(RegistryAccess access, ResourceKey<Race> key) {
        return registry(access).flatMap(registry -> registry.getHolder(key));
    }

    /** The race this entity belongs to, if any. */
    public static Optional<Holder.Reference<Race>> getRace(Entity entity) {
        RegistryAccess access = entity.level().registryAccess();
        if (entity instanceof RaceMember member) {
            ResourceKey<Race> key = member.sentience$getRace();
            if (key != null) {
                return get(access, key);
            }
        }
        for (Function<Entity, ResourceKey<Race>> resolver : RESOLVERS) {
            ResourceKey<Race> key = resolver.apply(entity);
            if (key != null) {
                return get(access, key);
            }
        }
        return all(access).filter(race -> race.value().hasMember(entity.getType())).findFirst();
    }

    /** Whether this entity belongs to the given race. */
    public static boolean isMember(Entity entity, ResourceKey<Race> race) {
        return getRace(entity).map(holder -> holder.is(race)).orElse(false);
    }
}
