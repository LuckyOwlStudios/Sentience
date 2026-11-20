package net.luckyowlstudios.races;

import net.luckyowlstudios.races.api.Race;
import net.luckyowlstudios.races.api.Races;
import net.luckyowlstudios.races.impl.ReputationCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(Constants.MOD_ID)
public class RacesNeoForge {

    public RacesNeoForge(IEventBus modBus) {
        RacesCommon.init();
        modBus.addListener(RacesNeoForge::registerDatapackRegistries);
        NeoForge.EVENT_BUS.addListener(RacesNeoForge::registerCommands);
    }

    private static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Races.REGISTRY, Race.CODEC, Race.CODEC);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        ReputationCommand.register(event.getDispatcher());
    }
}
