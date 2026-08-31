package com.finndog.locatenext.neoforge;

// Pre-1.20.5 NeoForge client entrypoint (1.20.4 only in practice). @Mod there has no dist
// parameter to gate a constructor-based class the way LocateNextNeoForgeClient does, and there is
// no ClientTickEvent.Post yet — ticks are TickEvent.ClientTickEvent, filtered by phase. Both
// pieces are wired the classic way instead: @Mod.EventBusSubscriber scanning static
// @SubscribeEvent methods, one class per bus since the annotation only names one.
//? if neoforge && <1.20.5 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.LocateNextKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.TickEvent;

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LocateNextNeoForgeClientLegacy {

    private LocateNextNeoForgeClientLegacy() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LocateNextKeys.create();
        LocateNextKeys.registerAll(event);
    }
}

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
final class LocateNextNeoForgeClientTickLegacy {

    private LocateNextNeoForgeClientTickLegacy() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocateNextKeys.tick();
    }
}
*///?}
