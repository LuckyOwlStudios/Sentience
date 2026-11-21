package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "hurt", at = @At("RETURN"))
    private void sentience$onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ReputationTriggers.onHurt((LivingEntity) (Object) this, source);
        }
    }
}
