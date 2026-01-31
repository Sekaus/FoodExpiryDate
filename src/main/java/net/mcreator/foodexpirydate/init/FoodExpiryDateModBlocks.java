/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import net.mcreator.foodexpirydate.block.MoldyBlockBlock;
import net.mcreator.foodexpirydate.block.FreezerBlock;
import net.mcreator.foodexpirydate.block.FoodDryingRackBlock;
import net.mcreator.foodexpirydate.block.DeadCropsBlock;
import net.mcreator.foodexpirydate.block.BottleOfMoldBlockBlock;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;

public class FoodExpiryDateModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, FoodExpiryDateMod.MODID);
	public static final RegistryObject<Block> FOOD_DRYING_RACK = REGISTRY.register("food_drying_rack", () -> new FoodDryingRackBlock());
	public static final RegistryObject<Block> FREEZER = REGISTRY.register("freezer", () -> new FreezerBlock());
	public static final RegistryObject<Block> DEAD_CROPS = REGISTRY.register("dead_crops", () -> new DeadCropsBlock());
	public static final RegistryObject<Block> MOLDY_BLOCK = REGISTRY.register("moldy_block", () -> new MoldyBlockBlock());
	public static final RegistryObject<Block> BOTTLE_OF_MOLD_BLOCK = REGISTRY.register("bottle_of_mold_block", () -> new BottleOfMoldBlockBlock());
	// Start of user code block custom blocks
	// End of user code block custom blocks
}