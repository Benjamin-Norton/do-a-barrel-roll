package nl.enjarai.doabarrelroll.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import nl.enjarai.doabarrelroll.DoABarrelRoll;
import nl.enjarai.doabarrelroll.config.SyncedModConfigServer;
import nl.enjarai.doabarrelroll.fabric.net.HandshakeServerFabric;

public class DoABarrelRollFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register server-side listeners for config syncing, this is done on
        // both client and server to ensure everything works in LAN worlds as well.
        SyncedModConfigServer.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!server.isDedicated()) {
                SyncedModConfigServer.INSTANCE.allowThrusting = true;
            }
        });
        HandshakeServerFabric.init();

        // Init server and client common code.
        DoABarrelRoll.init();
    }
}
