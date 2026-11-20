package net.luckyowlstudios.races.api;

import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

/**
 * Implement on your own entity class to choose its race in code instead of (or as well as) through
 * a race's {@code members} list. Takes priority over every other way of picking a race.
 */
public interface RaceMember {

    /** The race this entity belongs to right now, or {@code null} to fall back to the data-driven lookup. */
    @Nullable
    ResourceKey<Race> races$getRace();
}
