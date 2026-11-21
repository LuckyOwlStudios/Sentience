package net.luckyowlstudios.sentience.api;

import net.luckyowlstudios.sentience.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * Why a reputation changed. Passed to {@link ReputationEvents} listeners.
 * Mods can make their own: {@code new ReputationCause(ResourceLocation.fromNamespaceAndPath("mymod", "gift"))}.
 */
public record ReputationCause(ResourceLocation id) {

    public static final ReputationCause KILLED_MEMBER = new ReputationCause(Constants.id("killed_member"));
    public static final ReputationCause TRADED = new ReputationCause(Constants.id("traded"));
    public static final ReputationCause BARTERED = new ReputationCause(Constants.id("bartered"));
    public static final ReputationCause RAID_VICTORY = new ReputationCause(Constants.id("raid_victory"));
    public static final ReputationCause KILLED_RAIDER = new ReputationCause(Constants.id("killed_raider"));
    public static final ReputationCause HURT_PROTECTED = new ReputationCause(Constants.id("hurt_protected"));
    public static final ReputationCause KILLED_PROTECTED = new ReputationCause(Constants.id("killed_protected"));
    public static final ReputationCause COMMAND = new ReputationCause(Constants.id("command"));
    /** Default cause for helper calls that don't pass one. */
    public static final ReputationCause OTHER = new ReputationCause(Constants.id("other"));
}
