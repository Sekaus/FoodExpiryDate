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
import net.minecraft.world.level.block.CandleCakeBlock;

import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

/**
 * This class serves as a central registry for all food items and blocks
 * that can expire. The methods here are designed to work with both vanilla
 * and modded content in a robust and scalable way.
 */
public class ThingsThatCanExpire {
	private static final Map<Item, Item> ROTTEN_ITEMS = new HashMap<>();
	private static final Map<Block, Block> ROTTEN_BLOCKS = new HashMap<>();

	public static void loadDefaults() {
		// Items
		ROTTEN_ITEMS.put(Items.MILK_BUCKET, FoodExpiryDateModItems.MOLDY_MILK.get());
        ROTTEN_ITEMS.put(Items.EGG, FoodExpiryDateModItems.MOLDY_FOOD.get());
        ROTTEN_ITEMS.put(Items.MELON, FoodExpiryDateModItems.MOLDY_BLOCK.get()); // Keep for item logic if needed
        ROTTEN_ITEMS.put(Items.PUMPKIN, FoodExpiryDateModItems.MOLDY_BLOCK.get()); // Keep for item logic if needed
        ROTTEN_ITEMS.put(Items.CAKE, FoodExpiryDateModItems.MOLDY_BLOCK.get());

        //Blocks
        ROTTEN_BLOCKS.put(Blocks.MELON, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()); // This maps to your modded moldy block
        ROTTEN_BLOCKS.put(Blocks.PUMPKIN, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()); // This maps to your modded moldy block
        ROTTEN_BLOCKS.put(Blocks.CARVED_PUMPKIN, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()); // add carved variant
        ROTTEN_BLOCKS.put(Blocks.CAKE, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()); // This maps to your modded moldy block
        ROTTEN_BLOCKS.put(Blocks.CANDLE_CAKE, FoodExpiryDateModBlocks.MOLDY_BLOCK.get()); // This maps to your modded moldy block
	}

	public static void rebuildRegistry() {
	    ROTTEN_ITEMS.clear();
	    ROTTEN_BLOCKS.clear();
	
	    loadDefaults();
	    loadFromConfig();
	}

	private static void loadFromConfig() {
	    // Defensive: handle possible null or empty lists
	    var items = Settings.getExtraItems();
	    var blocks = Settings.getExtraBlocks();
	
	    if (items != null) {
	        for (String raw : items) {
	            System.out.println("[FoodExpiryDate] extraItem entry -> " + raw);
	            parseItem(raw);
	        }
	    }
	    if (blocks != null) {
	        for (String raw : blocks) {
	            System.out.println("[FoodExpiryDate] extraBlock entry -> " + raw);
	            parseBlock(raw);
	        }
	    }
	}
	
	private static void parseItem(String line) {
	    if (line == null) return;
	    String trimmed = line.trim();
	    if (trimmed.isEmpty()) return;
	
	    // split on first '=' only
	    int eq = trimmed.indexOf('=');
	    if (eq <= 0 || eq == trimmed.length() - 1) {
	        System.out.println("[FoodExpiryDate] Invalid extraItems entry (bad format): '" + line + "'");
	        return;
	    }
	
	    String left = trimmed.substring(0, eq).trim();
	    String right = trimmed.substring(eq + 1).trim();
	
	    try {
	        ResourceLocation inRL = new ResourceLocation(left);
	        ResourceLocation outRL = new ResourceLocation(right);
	
	        Item input = ForgeRegistries.ITEMS.getValue(inRL);
	        Item rotten = ForgeRegistries.ITEMS.getValue(outRL);
	
	        if (input == null) {
	            System.out.println("[FoodExpiryDate] extraItems: unknown input item '" + left + "'");
	            return;
	        }
	        if (rotten == null) {
	            System.out.println("[FoodExpiryDate] extraItems: unknown rotten item '" + right + "'");
	            return;
	        }
	
	        ROTTEN_ITEMS.put(input, rotten);
	    } catch (Exception e) {
	        System.out.println("[FoodExpiryDate] Exception parsing extraItems entry '" + line + "': " + e.getMessage());
	    }
	}
	
	private static void parseBlock(String line) {
	    if (line == null) return;
	    String trimmed = line.trim();
	    if (trimmed.isEmpty()) return;
	
	    int eq = trimmed.indexOf('=');
	    if (eq <= 0 || eq == trimmed.length() - 1) {
	        System.out.println("[FoodExpiryDate] Invalid extraBlocks entry (bad format): '" + line + "'");
	        return;
	    }
	
	    String left = trimmed.substring(0, eq).trim();
	    String right = trimmed.substring(eq + 1).trim();
	
	    try {
	        ResourceLocation inRL = new ResourceLocation(left);
	        ResourceLocation outRL = new ResourceLocation(right);
	
	        Block input = ForgeRegistries.BLOCKS.getValue(inRL);
	        Block rotten = ForgeRegistries.BLOCKS.getValue(outRL);
	
	        if (input == null) {
	            System.out.println("[FoodExpiryDate] extraBlocks: unknown input block '" + left + "'");
	            return;
	        }
	        if (rotten == null) {
	            System.out.println("[FoodExpiryDate] extraBlocks: unknown rotten block '" + right + "'");
	            return;
	        }
	
	        ROTTEN_BLOCKS.put(input, rotten);
	    } catch (Exception e) {
	        System.out.println("[FoodExpiryDate] Exception parsing extraBlocks entry '" + line + "': " + e.getMessage());
	    }
	}

    /**
     * Checks if an ItemStack is a food item that can expire.
     * This handles both vanilla and modded items based on common properties.
     * @param stack The ItemStack to check.
     * @return true if the item can expire, false otherwise.
     */
    public static boolean isFood(ItemStack stack) {
	    Item item = stack.getItem();
	    
	    // 1. Check for Moldy Items FIRST to prevent infinite loops
	    if (item == FoodExpiryDateModItems.MOLDY_FOOD.get() ||
	        item == FoodExpiryDateModItems.MOLDY_MILK.get() ||
	        item == FoodExpiryDateModItems.MOLDY_BLOCK.get() ||
	        item == FoodExpiryDateModItems.BOTTLE_OF_MOLD.get()) {
	        return false;
	    }
	
	    // 2. Check the ROTTEN_ITEMS map (covers your special items and config)
	    if (ROTTEN_ITEMS.containsKey(item)) return true;
	
	    // 3. Fallback to Minecraft's built-in food component
	    return item.isEdible();
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