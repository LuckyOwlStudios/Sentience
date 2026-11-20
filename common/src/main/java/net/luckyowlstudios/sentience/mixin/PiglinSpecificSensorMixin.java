package net.luckyowlstudios.sentience.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.luckyowlstudios.sentience.trading.SentienceTrading;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.PiglinSpecificSensor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PiglinSpecificSensor.class)
public abstract class PiglinSpecificSensorMixin {

    /**
     * Piglins only go after players who aren't wearing gold. A player the piglin's race trusts
     * counts as wearing gold, so the piglin leaves them alone.
     */
    @WrapOperation(method = "doTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;isWearingGold(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean sentience$trustedCountsAsGold(LivingEntity target, Operation<Boolean> original,
                                                  @Local(argsOnly = true) LivingEntity piglin) {
        if (original.call(target)) {
            return true;
        }
        return piglin instanceof Mob mob && target instanceof Player player && SentienceTrading.isPeacefulTowards(mob, player);
    }
}
