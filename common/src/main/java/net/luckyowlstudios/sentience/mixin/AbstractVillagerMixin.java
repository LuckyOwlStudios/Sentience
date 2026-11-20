package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.impl.ReputationTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin {

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void sentience$onTrade(MerchantOffer offer, CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        if (self.getTradingPlayer() instanceof ServerPlayer player) {
            ReputationTriggers.onTrade(self, player);
        }
    }
}
