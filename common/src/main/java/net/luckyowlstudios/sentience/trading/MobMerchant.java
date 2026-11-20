package net.luckyowlstudios.sentience.trading;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

/**
 * Lets any mob act as a merchant for the vanilla trade screen. The state lives on the mob ({@link TradingMob});
 * this is just a view of it.
 */
public final class MobMerchant implements Merchant {

    private final Mob mob;
    private final TradingMob state;
    private final SoundEvent tradeSound;

    public MobMerchant(Mob mob, SoundEvent tradeSound) {
        this.mob = mob;
        this.state = (TradingMob) mob;
        this.tradeSound = tradeSound;
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.state.sentience$setTradingPlayer(player);
    }

    @Override
    public @Nullable Player getTradingPlayer() {
        return this.state.sentience$getTradingPlayer();
    }

    @Override
    public MerchantOffers getOffers() {
        MerchantOffers offers = this.state.sentience$getOffers();
        return offers != null ? offers : new MerchantOffers();
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        // Only used on the client side merchant.
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.mob.playSound(this.tradeSound, 1.0F, this.mob.getVoicePitch());
        if (offer.shouldRewardExp() && offer.getXp() > 0) {
            this.mob.level().addFreshEntity(new ExperienceOrb(this.mob.level(),
                    this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ(), offer.getXp()));
        }
        if (this.getTradingPlayer() instanceof ServerPlayer player) {
            SentienceTrading.onTrade(this.mob, player);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return this.tradeSound != null ? this.tradeSound : SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.mob.level().isClientSide;
    }

    // Not part of Merchant in 1.21.1 (added later); kept so the menu closes sensibly if a version asks for it.
    public boolean stillValid(Player player) {
        return this.getTradingPlayer() == player && SentienceTrading.canKeepTrading(this.mob, player);
    }
}
