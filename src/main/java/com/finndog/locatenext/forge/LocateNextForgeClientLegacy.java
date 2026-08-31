package com.finndog.locatenext.forge;

// Forge 1.20.1 client entrypoint. @Mod there has no dist parameter to gate a constructor-based
// class the way LocateNextForgeClient does (confirmed: Forge 1.20.1's @Mod annotation carries only
// a mod id, no dist), and RegisterKeyMappingsEvent's only way to know when the client tick has
// happened is the classic phase-checked TickEvent.ClientTickEvent — Forge did not yet split it
// into Pre/Post. Both pieces are wired the classic way instead: @Mod.EventBusSubscriber scanning
// static @SubscribeEvent methods, one class per bus since the annotation only names one.
//? if forge && <1.20.4 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.LocateNextKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LocateNextForgeClientLegacy {

    private LocateNextForgeClientLegacy() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        LocateNextKeys.create();
        LocateNextKeys.registerAll(event);
    }
}

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
final class LocateNextForgeClientTickLegacy {

    private LocateNextForgeClientTickLegacy() {
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
