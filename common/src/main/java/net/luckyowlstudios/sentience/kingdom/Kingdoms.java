package net.luckyowlstudios.sentience.kingdom;

import net.luckyowlstudios.sentience.network.CreateKingdomPayload;
import net.luckyowlstudios.sentience.network.OpenCreateKingdomPayload;
import net.luckyowlstudios.sentience.village.SentienceVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

import java.util.Optional;
import java.util.UUID;

/**
 * Kingdom helpers and the server side of founding one. Server side only.
 */
public final class Kingdoms {

    /** How close a player has to be to a Town Hall to found a kingdom there. */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private Kingdoms() {
    }

    public static Optional<Kingdom> ofPlayer(ServerPlayer player) {
        return KingdomData.get(player.server).ofPlayer(player.getUUID());
    }

    public static Optional<Kingdom> ofPlayer(MinecraftServer server, UUID player) {
        return KingdomData.get(server).ofPlayer(player);
    }

    /** The kingdom whose seat is this Town Hall, if it has been set up. */
    public static Optional<Kingdom> atTownHall(MinecraftServer server, GlobalPos pos) {
        return KingdomData.get(server).atTownHall(pos);
    }

    /** A player right-clicked a Town Hall. */
    public static void onTownHallUsed(ServerPlayer player, BlockPos pos) {
        GlobalPos hall = GlobalPos.of(player.level().dimension(), pos);
        Optional<Kingdom> owner = atTownHall(player.server, hall);
        if (owner.isPresent()) {
            player.displayClientMessage(Component.translatable("sentience.kingdom.town_hall_of", owner.get().displayName()), true);
            return;
        }
        Optional<Kingdom> own = ofPlayer(player);
        if (own.isPresent()) {
            player.displayClientMessage(Component.translatable("sentience.kingdom.already_member", own.get().displayName()), true);
            return;
        }
        player.connection.send(new ClientboundCustomPayloadPacket(new OpenCreateKingdomPayload(pos)));
    }

    /** Cleans up a name typed by a player: trims it and collapses spaces. Empty if it isn't usable. */
    public static Optional<String> cleanName(String raw) {
        String name = raw.strip().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > Kingdom.MAX_NAME_LENGTH || name.chars().anyMatch(Character::isISOControl)) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    /** A player filled in the "Create Your Kingdom" screen. Checks everything again before founding it. */
    public static Optional<Kingdom> tryCreate(ServerPlayer player, CreateKingdomPayload request) {
        BlockPos pos = request.townHall();
        KingdomData data = KingdomData.get(player.server);
        GlobalPos hall = GlobalPos.of(player.level().dimension(), pos);
        if (!player.level().getBlockState(pos).is(SentienceVillage.townHall.get())
                || player.distanceToSqr(pos.getCenter()) > MAX_DISTANCE_SQR) {
            return Optional.empty();
        }
        if (data.atTownHall(hall).isPresent() || data.ofPlayer(player.getUUID()).isPresent()) {
            player.displayClientMessage(Component.translatable("sentience.kingdom.cannot_found"), false);
            return Optional.empty();
        }
        Optional<String> name = cleanName(request.name());
        if (name.isEmpty()) {
            player.displayClientMessage(Component.translatable("sentience.kingdom.invalid_name"), false);
            return Optional.empty();
        }
        if (data.isNameTaken(name.get())) {
            player.displayClientMessage(Component.translatable("sentience.kingdom.name_taken", name.get()), false);
            return Optional.empty();
        }
        DyeColor color = request.color();
        Kingdom kingdom = data.create(name.get(), color, player.getUUID(), hall);
        player.displayClientMessage(Component.translatable("sentience.kingdom.founded", kingdom.displayName()), false);
        return Optional.of(kingdom);
    }
}
