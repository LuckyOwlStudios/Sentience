package net.luckyowlstudios.sentience.impl;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Added to piglins by mixin: remembers who threw the gold the piglin is currently admiring,
 * so the barter can be credited to that player.
 */
public interface BarterTracker {

    @Nullable
    UUID sentience$getBarterPlayer();

    void sentience$setBarterPlayer(@Nullable UUID player);
}
