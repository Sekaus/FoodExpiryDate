package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;

import net.mcreator.foodexpirydate.DisplayRegistry;

@Mod.EventBusSubscriber
public class OnChunkUnloadProcedure {
	@SubscribeEvent
	public static void onWorldUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
		DisplayRegistry.cleanupAll();
	}
}