package net.luckyowlstudios.sentience;

import net.luckyowlstudios.sentience.api.Race;
import net.luckyowlstudios.sentience.api.Races;
import net.luckyowlstudios.sentience.api.TradeTable;
import net.luckyowlstudios.sentience.impl.ReputationCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(Constants.MOD_ID)
public class SentienceNeoForge {

    public SentienceNeoForge(IEventBus modBus) {
        SentienceCommon.init();
        modBus.addListener(SentienceNeoForge::registerDatapackRegistries);
        NeoForge.EVENT_BUS.addListener(SentienceNeoForge::registerCommands);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Races.REGISTRY, Race.CODEC, Race.CODEC);
        event.dataPackRegistry(TradeTable.REGISTRY, TradeTable.CODEC);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ReputationCommand.register(event.getDispatcher());
    }
}
