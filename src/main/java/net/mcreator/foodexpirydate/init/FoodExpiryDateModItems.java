/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

import net.mcreator.foodexpirydate.item.PotatoSeedsItem;
import net.mcreator.foodexpirydate.item.MoldyMilkItem;
import net.mcreator.foodexpirydate.item.MoldyFoodItem;
import net.mcreator.foodexpirydate.item.CarrotSeedsItem;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;

public class FoodExpiryDateModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, FoodExpiryDateMod.MODID);
	public static final RegistryObject<Item> MOLDY_MILK = REGISTRY.register("moldy_milk", () -> new MoldyMilkItem());
	public static final RegistryObject<Item> MOLDY_FOOD = REGISTRY.register("moldy_food", () -> new MoldyFoodItem());
	public static final RegistryObject<Item> FOOD_DRYING_RACK = block(FoodExpiryDateModBlocks.FOOD_DRYING_RACK);
	public static final RegistryObject<Item> FREEZER = block(FoodExpiryDateModBlocks.FREEZER);
	public static final RegistryObject<Item> DEAD_CROPS = block(FoodExpiryDateModBlocks.DEAD_CROPS);
	public static final RegistryObject<Item> MOLDY_BLOCK = block(FoodExpiryDateModBlocks.MOLDY_BLOCK);
	public static final RegistryObject<Item> CARROT_SEEDS = REGISTRY.register("carrot_seeds", () -> new CarrotSeedsItem());
	public static final RegistryObject<Item> POTATO_SEEDS = REGISTRY.register("potato_seeds", () -> new PotatoSeedsItem());

	// Start of user code block custom items
	// End of user code block custom items
	private static RegistryObject<Item> block(RegistryObject<Block> block) {
		return block(block, new Item.Properties());
	}

	private static RegistryObject<Item> block(RegistryObject<Block> block, Item.Properties properties) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), properties));
	}
}