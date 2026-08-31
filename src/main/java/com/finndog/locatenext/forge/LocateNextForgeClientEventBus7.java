package com.finndog.locatenext.forge;

// Forge 1.21.6+ client entrypoint (1.21.11 only). EventBus 7 moved RegisterKeyMappingsEvent and
// TickEvent off the mod bus entirely — confirmed against the real 1.21.11 sources, both are now
// records/sealed interfaces with their own dedicated static EventBus<T> field
// (RegisterKeyMappingsEvent.BUS, TickEvent.ClientTickEvent.Post.BUS), no longer discoverable by
// @Mod.EventBusSubscriber's classic method-scanning the way they were pre-1.21.6. @Mod.
// EventBusSubscriber itself is unchanged, though, and FMLClientSetupEvent is still a genuine
// mod-bus event dispatched through it (the official 1.21.11 example mod uses this exact split:
// FMLCommonSetupEvent wired via the new bus-group API in the constructor, FMLClientSetupEvent
// still scanned via @Mod.EventBusSubscriber) — so that dist-gated, FML-discovered class remains
// the safe place to wire the two dedicated buses above, rather than inventing a new gating
// mechanism. Kept as a whole separate file rather than an inner version split, since Stonecutter
// does not resolve //? markers nested inside another //? block.
//? if forge && >=1.21.6 {

/*import com.finndog.locatenext.LocateNext;
import com.finndog.locatenext.client.LocateNextKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = LocateNext.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LocateNextForgeClientEventBus7 {

    private LocateNextForgeClientEventBus7() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        LocateNextKeys.create();
        RegisterKeyMappingsEvent.BUS.addListener(LocateNextKeys::registerAll);
        TickEvent.ClientTickEvent.Post.BUS.addListener(tick -> LocateNextKeys.tick());
    }
}
*///?}
