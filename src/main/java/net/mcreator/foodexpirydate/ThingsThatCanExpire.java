package net.mcreator.foodexpirydate;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;
import net.mcreator.foodexpirydate.init.FoodExpiryDateModBlocks;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * This class serves as a central registry for all food items and blocks
 * that can expire. The methods here are designed to work with both vanilla
 * and modded content in a robust and scalable way.
 */
public class ThingsThatCanExpire {

    // Immutable Maps for cleaner and more efficient lookups
    private static final Map<Item, Item> ROTTEN_ITEMS = ImmutableMap.<Item, Item>builder()
        .put(Items.MILK_BUCKET, FoodExpiryDateModItems.MOLDY_MILK.get())
        .put(Items.EGG, FoodExpiryDateModItems.MOLDY_FOOD.get())
        .put(Items.MELON, FoodExpiryDateModItems.MOLDY_BLOCK.get()) // Keep for item logic if needed
        .put(Items.PUMPKIN, FoodExpiryDateModItems.MOLDY_BLOCK.get()) // Keep for item logic if needed
        .put(Items.CAKE, FoodExpiryDateModItems.MOLDY_BLOCK.get())
        .build();

    private static final Map<Block, Block> ROTTEN_BLOCKS = ImmutableMap.<Block, Block>builder()
        .put(Blocks.MELON, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()) // This maps to your modded moldy block
        .put(Blocks.PUMPKIN, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()) // This maps to your modded moldy block
        .put(Blocks.CARVED_PUMPKIN, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()) // add carved variant
        .put(Blocks.CAKE, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()) // This maps to your modded moldy block
        .put(Blocks.CANDLE_CAKE, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()) // This maps to your modded moldy block
        .build();

    /**
     * Checks if an ItemStack is a food item that can expire.
     * This handles both vanilla and modded items based on common properties.
     * @param stack The ItemStack to check.
     * @return true if the item can expire, false otherwise.
     */
    public static boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        // A direct check against the item's food component.
        // This is a reliable way to check for most edible items.
        boolean hasFoodComponent = item.isEdible();
        // Add specific item checks for items that are not technically food but should expire.
        boolean isSpecialItem = item == Items.MILK_BUCKET || item == Items.EGG || item == Items.CAKE || item == Items.PUMPKIN || item == Items.MELON || item == Items.DRIED_KELP_BLOCK;
        // Exclude our own moldy items
        boolean isMoldy = item == FoodExpiryDateModItems.MOLDY_FOOD.get() ||
                          item == FoodExpiryDateModItems.MOLDY_MILK.get() ||
                          item == FoodExpiryDateModItems.MOLDY_BLOCK.get();

        return (hasFoodComponent || isSpecialItem) && !isMoldy;
    }

   /**
     * Checks if a Block is a food block that can expire.
     * @param level The current level.
     * @param state The BlockState to check.
     * @param pos The BlockPos of the block.
     * @return true if the block can expire, false otherwise.
     */
    public static boolean isBlockFood(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();
    
        // Explicitly check for Cake, Pumpkin, and Melon blocks.
        if (ROTTEN_BLOCKS.containsKey(block)) {
	        return true;
	    }
	
	    if (block instanceof CandleCakeBlock) {
	        return true;
	    }
	
	    // Crops
	    if (block instanceof IPlantable) {
	        PlantType type = ((IPlantable) block).getPlantType(level, pos);
	        return type == PlantType.CROP;
	    }
	    
	    return false;
	}

    /**
     * Returns the rotten version of an item stack.
     * @param stack The original item stack.
     * @return The rotten version of the item, or the original stack if no rotten version exists.
     */
    public static ItemStack getRotten(ItemStack stack) {
        Item item = stack.getItem();
        if (ROTTEN_ITEMS.containsKey(item)) {
            return new ItemStack(ROTTEN_ITEMS.get(item));
        }
        
        if (isFood(stack)) {
            // Default to moldy food for any other food item that rots
            return new ItemStack(FoodExpiryDateModItems.MOLDY_FOOD.get());
        }
        return stack;
    }

    /**
     * Returns the rotten version of a block state.
     * @param level The current level.
     * @param state The original block state.
     * @param pos The BlockPos of the block.
     * @return The rotten version of the block, or the original state if no rotten version exists.
     */
    public static BlockState getRotten(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();

        // Explicitly handle pumpkins and melons to ensure they return MOLDY_BLOCK
        if (block == Blocks.PUMPKIN || block == Blocks.MELON) {
            return FoodExpiryDateModBlocks.MOLDY_BLOCK.get().defaultBlockState();
        }

        if (block instanceof CandleCakeBlock)
            return FoodExpiryDateModBlocks.MOLDY_BLOCK.get().defaultBlockState();

        // Check our predefined map for common blocks.
        if (ROTTEN_BLOCKS.containsKey(block)) {
            return ROTTEN_BLOCKS.get(block).defaultBlockState();
        }
        
        // Check if it's a fully grown crop.
        if (isFullyGrown(level, state, pos)) {
            // For crops, you might want a specific "dead crop" block if available,
            // otherwise use a generic rotten block.
            // If FoodExpiryDateModBlocks.DEAD_CROPS does not exist, this might need adjustment.
            return FoodExpiryDateModBlocks.DEAD_CROPS.get().defaultBlockState(); 
        }

        return state;
    }

    /**
     * Checks if a block is fully grown, handling both vanilla and some modded crops.
     * @param level The current level.
     * @param state The BlockState of the block.
     * @param pos The BlockPos of the block.
     * @return true if the crop is fully grown, false otherwise.
     */
    public static boolean isFullyGrown(Level level, BlockState state, BlockPos pos) {
        Block block = state.getBlock();

        // Pumpkins and melons don't have a "fully grown" state in the same way crops do.
        // Their stems indicate growth, but the fruits themselves are placed blocks.
        // We handle them directly in isBlockFood and getRotten.
        if (block == Blocks.PUMPKIN_STEM || block == Blocks.MELON_STEM)
            return false; // These are stems, not the fruit itself

        // Standard check for vanilla crops.
        if (block instanceof CropBlock cropBlock)
            return cropBlock.isMaxAge(state);

        // Custom check for modded crops that are IPlantable but not CropBlock.
        // This is a generic check for an 'age' property.
        if (block instanceof IPlantable) {
            Optional<IntegerProperty> ageProperty = state.getProperties().stream()
                .filter(p -> p instanceof IntegerProperty && p.getName().equals("age"))
                .map(p -> (IntegerProperty) p)
                .findFirst();

            if (ageProperty.isPresent()) {
                IntegerProperty ageProp = ageProperty.get();
                return state.getValue(ageProp) >= ageProp.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
            }
        }
        return false;
    }
}