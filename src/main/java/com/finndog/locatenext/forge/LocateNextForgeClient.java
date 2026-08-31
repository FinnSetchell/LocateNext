package com.finndog.locatenext.forge;

// Forge 1.20.4-1.21.5 client entrypoint (>=1.20.4, below the 1.21.6 EventBus 7 rewrite). Forge's
// own @Mod annotation never gained a dist parameter to gate a constructor-based class the way
// NeoForge's does (confirmed against the actual 1.20.4 Forge sources — still just a mod id), so
// this stays wired the classic way: @Mod.EventBusSubscriber scanning static @SubscribeEvent
// methods, one class per bus since the annotation only names one. The one change from the <1.20.4
// sibling is TickEvent.ClientTickEvent.Post replacing the phase-checked base event — Forge split
// ClientTickEvent into Pre/Post nested classes at the same point its networking moved (see
// LocateNextForgeClientLegacy).
//? if forge && >=1.20.4 && <1.21.6 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.LocateNextKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LocateNextForgeClient {

    private LocateNextForgeClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LocateNextKeys.create();
        LocateNextKeys.registerAll(event);
    }
}

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
final class LocateNextForgeClientTick {

    private LocateNextForgeClientTick() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        LocateNextKeys.tick();
    }
}
*///?}
