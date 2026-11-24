package net.luckyowlstudios.sentience.village;

import net.luckyowlstudios.sentience.kingdom.Kingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Town Hall: the Guide's job site, and the seat of a kingdom. Right-clicking one that isn't set up yet,
 * while not in a kingdom, opens the "Create Your Kingdom" screen.
 */
public class TownHallBlock extends Block {

    public TownHallBlock(Properties properties) {
        super(properties);
    }

    /** Right-clicking with a banner sets it as the kingdom's banner (leaders only). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof BannerItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            GlobalPos hall = GlobalPos.of(level.dimension(), pos);
            if (Kingdoms.atTownHall(serverPlayer.server, hall).filter(kingdom -> kingdom.isMember(player.getUUID())).isPresent()) {
                Kingdoms.setBanner(serverPlayer, stack);
            } else {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            Kingdoms.onTownHallRemoved(serverLevel.getServer(), GlobalPos.of(level.dimension(), pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            Kingdoms.onTownHallUsed(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
