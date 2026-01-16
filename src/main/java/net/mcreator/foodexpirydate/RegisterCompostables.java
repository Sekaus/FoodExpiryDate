/**
 * The code of this mod element is always locked.
 *
 * You can register new events in this class too.
 *
 * If you want to make a plain independent class, create it using
 * Project Browser -> New... and make sure to make the class
 * outside net.mcreator.foodexpirydate as this package is managed by MCreator.
 *
 * If you change workspace package, modid or prefix, you will need
 * to manually adapt this file to these changes or remake it.
 *
 * This class will be added in the mod root package.
*/
package net.mcreator.foodexpirydate;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.block.ComposterBlock;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;
import net.mcreator.foodexpirydate.init.FoodExpiryDateModBlocks;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterCompostables {
	public RegisterCompostables() {
		ComposterBlock.COMPOSTABLES.put(FoodExpiryDateModItems.MOLDY_FOOD.get(), 0.5f);
		ComposterBlock.COMPOSTABLES.put(FoodExpiryDateModItems.CARROT_SEEDS.get(), 0.3f);
		ComposterBlock.COMPOSTABLES.put(FoodExpiryDateModItems.POTATO_SEEDS.get(), 0.3f);
	}

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		new RegisterCompostables();
	}
}
