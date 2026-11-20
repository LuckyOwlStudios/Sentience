package net.luckyowlstudios.races.mixin;

import net.luckyowlstudios.races.impl.BarterTracker;
import net.minecraft.world.entity.monster.piglin.Piglin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(Piglin.class)
public abstract class PiglinMixin implements BarterTracker {

    @Unique
    @Nullable
    private UUID races$barterPlayer;

    @Override
    public @Nullable UUID races$getBarterPlayer() {
        return this.races$barterPlayer;
    }

    @Override
    public void races$setBarterPlayer(@Nullable UUID player) {
        this.races$barterPlayer = player;
    }
}
