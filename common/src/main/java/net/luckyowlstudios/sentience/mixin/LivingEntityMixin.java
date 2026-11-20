package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected boolean dead;

    @Inject(method = "die", at = @At("HEAD"))
    private void sentience$onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // Same guard as vanilla, so an entity that is already dead isn't counted twice.
        if (!self.isRemoved() && !this.dead) {
            ReputationTriggers.onDeath(self, source);
        }
    }
}
