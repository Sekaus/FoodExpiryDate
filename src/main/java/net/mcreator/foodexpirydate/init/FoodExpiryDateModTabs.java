/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import net.mcreator.foodexpirydate.FoodExpiryDateMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class FoodExpiryDateModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FoodExpiryDateMod.MODID);
	public static final RegistryObject<CreativeModeTab> FOOD_EXPIRY_DATE = REGISTRY.register("food_expiry_date",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.food_expiry_date.food_expiry_date")).icon(() -> new ItemStack(FoodExpiryDateModItems.MOLDY_MILK.get())).displayItems((parameters, tabData) -> {
				tabData.accept(FoodExpiryDateModItems.MOLDY_MILK.get());
				tabData.accept(FoodExpiryDateModItems.MOLDY_FOOD.get());
				tabData.accept(FoodExpiryDateModBlocks.FOOD_DRYING_RACK.get().asItem());
				tabData.accept(FoodExpiryDateModBlocks.FREEZER.get().asItem());
				tabData.accept(FoodExpiryDateModBlocks.DEAD_CROPS.get().asItem());
				tabData.accept(FoodExpiryDateModBlocks.MOLDY_BLOCK.get().asItem());
				tabData.accept(FoodExpiryDateModItems.CARROT_SEEDS.get());
				tabData.accept(FoodExpiryDateModItems.POTATO_SEEDS.get());
			}).build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			tabData.accept(FoodExpiryDateModItems.MOLDY_MILK.get());
			tabData.accept(FoodExpiryDateModItems.MOLDY_FOOD.get());
			tabData.accept(FoodExpiryDateModBlocks.MOLDY_BLOCK.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			tabData.accept(FoodExpiryDateModBlocks.DEAD_CROPS.get().asItem());
		}
	}
}