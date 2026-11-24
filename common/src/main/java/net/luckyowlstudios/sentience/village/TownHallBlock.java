package net.luckyowlstudios.sentience.village;

import net.luckyowlstudios.sentience.kingdom.Kingdoms;
import net.minecraft.core.BlockPos;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            Kingdoms.onTownHallUsed(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
