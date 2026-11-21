package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Raider.class)
public abstract class RaiderMixin {

    /**
     * Runs before vanilla takes the raider out of its raid, so we can still tell it was raiding.
     * A second call to die() finds no raid, so a kill is only counted once.
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void sentience$onRaiderDie(DamageSource source, CallbackInfo ci) {
        Raider self = (Raider) (Object) this;
        if (self.level().isClientSide || self.isRemoved()) {
            return;
        }
        if (self.hasActiveRaid()) {
            ServerPlayer killer = ReputationTriggers.responsiblePlayer(self, source);
            if (killer != null) {
                ReputationTriggers.onRaiderKilled(killer);
            }
        }
    }
}
