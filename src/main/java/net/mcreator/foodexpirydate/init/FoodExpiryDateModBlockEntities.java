/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;

import net.mcreator.foodexpirydate.block.entity.FreezerBlockEntity;
import net.mcreator.foodexpirydate.block.entity.FoodDryingRackBlockEntity;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;

public class FoodExpiryDateModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FoodExpiryDateMod.MODID);
	public static final RegistryObject<BlockEntityType<FoodDryingRackBlockEntity>> FOOD_DRYING_RACK = register("food_drying_rack", FoodExpiryDateModBlocks.FOOD_DRYING_RACK, FoodDryingRackBlockEntity::new);
	public static final RegistryObject<BlockEntityType<FreezerBlockEntity>> FREEZER = register("freezer", FoodExpiryDateModBlocks.FREEZER, FreezerBlockEntity::new);

	// Start of user code block custom block entities
	// End of user code block custom block entities
	private static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> register(String registryname, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<T> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}