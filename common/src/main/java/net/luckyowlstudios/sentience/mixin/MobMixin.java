package net.luckyowlstudios.sentience.mixin;

import net.luckyowlstudios.sentience.Constants;
import net.luckyowlstudios.sentience.trading.SentienceTrading;
import net.luckyowlstudios.sentience.trading.TradingMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin implements TradingMob {

    @Unique
    private static final String SENTIENCE_OFFERS = "SentienceOffers";
    @Unique
    private static final String SENTIENCE_LAST_RESTOCK = "SentienceLastRestock";

    @Unique
    @Nullable
    private MerchantOffers sentience$offers;
    @Unique
    private long sentience$lastRestock;
    @Unique
    @Nullable
    private Player sentience$tradingPlayer;

    // ---- TradingMob ----

    @Override
    public @Nullable MerchantOffers sentience$getOffers() {
        return this.sentience$offers;
    }

    @Override
    public void sentience$setOffers(@Nullable MerchantOffers offers) {
        this.sentience$offers = offers;
    }

    @Override
    public long sentience$getLastRestock() {
        return this.sentience$lastRestock;
    }

    @Override
    public void sentience$setLastRestock(long gameTime) {
        this.sentience$lastRestock = gameTime;
    }

    @Override
    public @Nullable Player sentience$getTradingPlayer() {
        return this.sentience$tradingPlayer;
    }

    @Override
    public void sentience$setTradingPlayer(@Nullable Player player) {
        this.sentience$tradingPlayer = player;
    }

    // ---- Trading ----

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void sentience$openTrades(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Mob self = (Mob) (Object) this;
        if (hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer) || player.isSecondaryUseActive()) {
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        // Leave leads, name tags and piglin bartering to vanilla.
        if (held.is(Items.LEAD) || held.is(Items.NAME_TAG) || (self instanceof Piglin && held.is(ItemTags.PIGLIN_LOVED))) {
            return;
        }
        if (SentienceTrading.tryOpenTrades(self, serverPlayer)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void sentience$tickTrading(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!self.level().isClientSide && this.sentience$tradingPlayer != null) {
            SentienceTrading.tickTrading(self);
        }
    }

    // ---- Peacefulness ----

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void sentience$stayPeaceful(@Nullable LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target instanceof Player player && !self.level().isClientSide && SentienceTrading.isPeacefulTowards(self, player)) {
            ci.cancel();
        }
    }

    // ---- Saving ----

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void sentience$saveOffers(CompoundTag tag, CallbackInfo ci) {
        if (this.sentience$offers != null) {
            Mob self = (Mob) (Object) this;
            MerchantOffers.CODEC.encodeStart(self.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.sentience$offers)
                    .resultOrPartial(error -> Constants.LOG.warn("Couldn't save trades of {}: {}", self, error))
                    .ifPresent(offers -> tag.put(SENTIENCE_OFFERS, offers));
            tag.putLong(SENTIENCE_LAST_RESTOCK, this.sentience$lastRestock);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void sentience$loadOffers(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(SENTIENCE_OFFERS, Tag.TAG_COMPOUND) || tag.contains(SENTIENCE_OFFERS, Tag.TAG_LIST)) {
            Mob self = (Mob) (Object) this;
            MerchantOffers.CODEC.parse(self.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get(SENTIENCE_OFFERS))
                    .resultOrPartial(error -> Constants.LOG.warn("Couldn't load trades of {}: {}", self, error))
                    .ifPresent(offers -> this.sentience$offers = offers);
            this.sentience$lastRestock = tag.getLong(SENTIENCE_LAST_RESTOCK);
        }
    }
}
