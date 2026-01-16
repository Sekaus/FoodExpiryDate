/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.gui.screens.MenuScreens;

import net.mcreator.foodexpirydate.client.gui.FreezerGUIScreen;
import net.mcreator.foodexpirydate.client.gui.FoodDryingRackGUIScreen;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FoodExpiryDateModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(FoodExpiryDateModMenus.FOOD_DRYING_RACK_GUI.get(), FoodDryingRackGUIScreen::new);
			MenuScreens.register(FoodExpiryDateModMenus.FREEZER_GUI.get(), FreezerGUIScreen::new);
		});
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}