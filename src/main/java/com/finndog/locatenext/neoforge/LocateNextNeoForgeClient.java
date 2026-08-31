package com.finndog.locatenext.neoforge;

// Below 1.20.5, @Mod has no dist parameter and there is no ClientTickEvent.Post — see
// LocateNextNeoForgeClientLegacy instead.
//? if neoforge && >=1.20.5 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.LocateNextKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

// NeoForge entrypoint, client only — dist-gated, so FML never constructs it on a dedicated server.
// The client-bound packet handlers are registered from the common entrypoint instead (see
// LocateNextNeoForge); this class only owns what is genuinely client-only: the keybinds and the
// tick hook that reads them.
@Mod(value = LocateNext.MOD_ID, dist = Dist.CLIENT)
public final class LocateNextNeoForgeClient {

    public LocateNextNeoForgeClient(IEventBus modBus) {
        LocateNextKeys.create();
        modBus.addListener(LocateNextNeoForgeClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener(LocateNextNeoForgeClient::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LocateNextKeys.registerAll(event);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LocateNextKeys.tick();
    }
}
*///?}
