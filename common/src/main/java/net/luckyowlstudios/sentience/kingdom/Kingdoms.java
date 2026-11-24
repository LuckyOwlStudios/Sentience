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
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

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
        if (own.isPresent() && own.get().townHall().isEmpty() && own.get().isLeader(player.getUUID())) {
            // A kingdom that lost its Town Hall can take this one as its new seat.
            KingdomData.get(player.server).setTownHall(own.get(), hall);
            player.displayClientMessage(Component.translatable("sentience.kingdom.town_hall_claimed", own.get().displayName()), true);
            return;
        }
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
        Kingdom kingdom = data.create(player.server, name.get(), color, player.getUUID(), hall);
        player.displayClientMessage(Component.translatable("sentience.kingdom.founded", kingdom.displayName()), false);
        return Optional.of(kingdom);
    }

    // ---- Membership, like vanilla teams ----

    private static void tell(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args));
    }

    private static Optional<Kingdom> ledBy(ServerPlayer player) {
        Optional<Kingdom> kingdom = ofPlayer(player);
        if (kingdom.isEmpty()) {
            tell(player, "sentience.kingdom.not_in_kingdom");
            return Optional.empty();
        }
        if (!kingdom.get().isLeader(player.getUUID())) {
            tell(player, "sentience.kingdom.not_leader", kingdom.get().displayName());
            return Optional.empty();
        }
        return kingdom;
    }

    /** The leader invites someone. They can then join with {@code /kingdom join}. */
    public static boolean invite(ServerPlayer leader, ServerPlayer target) {
        Optional<Kingdom> kingdom = ledBy(leader);
        if (kingdom.isEmpty()) {
            return false;
        }
        KingdomData data = KingdomData.get(leader.server);
        if (data.ofPlayer(target.getUUID()).isPresent()) {
            tell(leader, "sentience.kingdom.invite.already_in_one", target.getDisplayName());
            return false;
        }
        data.invite(kingdom.get(), target.getUUID());
        tell(leader, "sentience.kingdom.invite.sent", target.getDisplayName());
        Component join = Component.translatable("sentience.kingdom.invite.join_button").withStyle(style -> style
                .withColor(net.minecraft.ChatFormatting.GREEN)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                        "/kingdom join " + kingdom.get().name())));
        target.sendSystemMessage(Component.translatable("sentience.kingdom.invite.received",
                leader.getDisplayName(), kingdom.get().displayName(), join));
        return true;
    }

    /** Joins a kingdom the player was invited to. */
    public static boolean join(ServerPlayer player, String kingdomName) {
        KingdomData data = KingdomData.get(player.server);
        if (data.ofPlayer(player.getUUID()).isPresent()) {
            tell(player, "sentience.kingdom.already_member", data.ofPlayer(player.getUUID()).get().displayName());
            return false;
        }
        Optional<Kingdom> kingdom = data.byName(kingdomName);
        if (kingdom.isEmpty()) {
            tell(player, "sentience.kingdom.unknown", kingdomName);
            return false;
        }
        if (!kingdom.get().isInvited(player.getUUID())) {
            tell(player, "sentience.kingdom.join.not_invited", kingdom.get().displayName());
            return false;
        }
        data.addMember(player.server, kingdom.get(), player.getUUID());
        broadcast(player.server, kingdom.get(), Component.translatable("sentience.kingdom.join.joined", player.getDisplayName(), kingdom.get().displayName()));
        return true;
    }

    /** The leader removes a member. */
    public static boolean kick(ServerPlayer leader, UUID target) {
        Optional<Kingdom> kingdom = ledBy(leader);
        if (kingdom.isEmpty()) {
            return false;
        }
        String targetName = KingdomTeams.playerName(leader.server, target).orElse(target.toString());
        if (target.equals(leader.getUUID())) {
            tell(leader, "sentience.kingdom.kick.self");
            return false;
        }
        if (!kingdom.get().isMember(target)) {
            tell(leader, "sentience.kingdom.kick.not_member", targetName, kingdom.get().displayName());
            return false;
        }
        KingdomData.get(leader.server).removeMember(leader.server, kingdom.get(), target);
        broadcast(leader.server, kingdom.get(), Component.translatable("sentience.kingdom.kick.kicked", targetName, kingdom.get().displayName()));
        ServerPlayer kicked = leader.server.getPlayerList().getPlayer(target);
        if (kicked != null) {
            tell(kicked, "sentience.kingdom.kick.you_were_kicked", kingdom.get().displayName());
        }
        return true;
    }

    /** Leaves the player's kingdom. A leader hands over to the next member; the last one out disbands it. */
    public static boolean leave(ServerPlayer player) {
        Optional<Kingdom> kingdom = ofPlayer(player);
        if (kingdom.isEmpty()) {
            tell(player, "sentience.kingdom.not_in_kingdom");
            return false;
        }
        boolean disbanded = KingdomData.get(player.server).removeMember(player.server, kingdom.get(), player.getUUID());
        if (disbanded) {
            tell(player, "sentience.kingdom.leave.disbanded", kingdom.get().displayName());
        } else {
            tell(player, "sentience.kingdom.leave.left", kingdom.get().displayName());
            broadcast(player.server, kingdom.get(), Component.translatable("sentience.kingdom.leave.member_left",
                    player.getDisplayName(), kingdom.get().displayName()));
        }
        return true;
    }

    /** The leader sets the kingdom's banner from a banner item. */
    public static boolean setBanner(ServerPlayer player, ItemStack banner) {
        if (!(banner.getItem() instanceof BannerItem)) {
            tell(player, "sentience.kingdom.banner.not_a_banner");
            return false;
        }
        Optional<Kingdom> kingdom = ledBy(player);
        if (kingdom.isEmpty()) {
            return false;
        }
        KingdomData.get(player.server).setBanner(kingdom.get(), banner);
        tell(player, "sentience.kingdom.banner.set", kingdom.get().displayName());
        return true;
    }

    /** A Town Hall block was removed. Its kingdom carries on, without a seat until it gets a new one. */
    public static void onTownHallRemoved(MinecraftServer server, GlobalPos pos) {
        KingdomData data = KingdomData.get(server);
        data.atTownHall(pos).ifPresent(kingdom -> {
            data.clearTownHall(kingdom);
            broadcast(server, kingdom, Component.translatable("sentience.kingdom.town_hall_lost", kingdom.displayName()));
        });
    }

    private static void broadcast(MinecraftServer server, Kingdom kingdom, Component message) {
        for (UUID member : kingdom.members()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(message);
            }
        }
    }
}
