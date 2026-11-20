package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.BarterTracker;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(Piglin.class)
public abstract class PiglinMixin implements BarterTracker {

    @Unique
    @Nullable
    private UUID sentience$barterPlayer;

    @Override
    public @Nullable UUID sentience$getBarterPlayer() {
        return this.sentience$barterPlayer;
    }

    @Override
    public void sentience$setBarterPlayer(@Nullable UUID player) {
        this.sentience$barterPlayer = player;
    }
}
