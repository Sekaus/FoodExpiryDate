package net.mcreator.foodexpirydate.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;
import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;

public class FreezerBlockDestroyedByPlayerProcedure {
    public static void execute(LevelAccessor world, BlockPos pos, BlockEntity blockEntity) {
        if (world instanceof ServerLevel _level) {
            if (blockEntity != null) {
                // Create a new item stack for the freezer
                ItemStack freezerStack = new ItemStack(FoodExpiryDateModItems.FREEZER.get()); // Replace with your actual freezer item

                // Create a new NBT compound to hold the block entity data
                CompoundTag nbtTag = new CompoundTag();

                // Save the block entity's data to the NBT tag
                nbtTag = blockEntity.serializeNBT();

                // Add the NBT data to the item stack
                freezerStack.getOrCreateTag().put("BlockEntityTag", nbtTag);

                // Create and spawn the item entity with the modified item stack
                ItemEntity entityToSpawn = new ItemEntity(_level, pos.getX(), pos.getY(), pos.getZ(), freezerStack);
                entityToSpawn.setPickUpDelay(10); // A small delay to prevent instant pickup
                _level.addFreshEntity(entityToSpawn);
            }
        }
    }
}