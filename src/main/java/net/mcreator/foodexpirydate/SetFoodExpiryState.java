package net.mcreator.foodexpirydate;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;
import net.mcreator.foodexpirydate.Settings;
import net.mcreator.foodexpirydate.ExpiryData;

import org.stringtemplate.v4.ST;

import java.util.List;

/**
 * The code in this class handles setting and updating the expiry state of food items.
 * It is now more robust to prevent inventory synchronization issues.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SetFoodExpiryState {
    /**
     * Updates the expiry state of a single food item.
     * This method handles both setting the initial expiry date and replacing the item with a rotten version.
     *
     * @param days The current number of days passed in the game.
     * @param stack The item stack to update. This is the original stack from the inventory handler.
     * @param world The world accessor.
     * @param handler The item handler (IItemHandlerModifiable) that contains the stack.
     * @param slot The slot index of the stack within the handler.
     * @param changeDateToToday If true, forces the creation date to be set to the current day. This is used for freezers.
     */
    public static void updateExpiryState(double days, ItemStack stack, LevelAccessor world, IItemHandlerModifiable handler, int slot, boolean changeDateToToday) {
        // Only proceed if the stack is not empty.
        if (stack.isEmpty())
            return;

        boolean isFood = ThingsThatCanExpire.isFood(stack);
        boolean isDried = stack.getOrCreateTag().getBoolean("dried");
        boolean dateSet = stack.getOrCreateTag().getBoolean("dateSet");

        // The expiry logic only applies to food that has not been dried.
        if (!isDried && isFood) {
            // Case 1: The item has no expiry date tag.
            // This is a new item, so we set its creation date.
            if (!dateSet) {
                stack.getOrCreateTag().putDouble("creationDate", days);
                stack.getOrCreateTag().putBoolean("dateSet", true);
                handler.setStackInSlot(slot, stack);
            }
            // Case 2: The item is in a freezer.
            // We force the creation date to be reset to the current day.
            else if (changeDateToToday) {
                stack.getOrCreateTag().putDouble("creationDate", days);
                handler.setStackInSlot(slot, stack);
            }
            // Case 3: The item is expired and not in a freezer.
            // Replace the food with its rotten version.
            else if (days - stack.getOrCreateTag().getDouble("creationDate") > Settings.getDaysBeforeItExpires()) {
                ItemStack moldyStack = ThingsThatCanExpire.getRotten(stack);
                moldyStack.setCount(stack.getCount()); // Retain the original stack size.
                handler.setStackInSlot(slot, moldyStack);
            }
        }
    }

	/**
     * Updates the expiry state of a food block.
     * This method handles setting the initial expiry date and replacing the block with a rotten version.
     *
     * @param days  The current number of days passed in the game.
     * @param state The BlockState of the block.
     * @param pos   The BlockPos of the block.
     * @param level The Level (world) where the block is located.
     */
    public static void updateExpiryState(double days, BlockState state, BlockPos pos, Level level) {
	    if (level.isClientSide()) return;
	    if (!(level instanceof ServerLevel serverLevel)) return;
	    if (!ThingsThatCanExpire.isBlockFood(level, state, pos)) return;
	    
	    ExpiryData expiryData = ExpiryData.get(serverLevel);
	    CompoundTag existing = expiryData.getExpiryDataOrNull(pos);
	    
	    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
	    String currentBlockId = id != null ? id.toString() : "";
	    
	    long now = serverLevel.getGameTime();
	    
	    if (existing == null) {
	        // First-time placement: only create data, no spoil
	        CompoundTag fresh = new CompoundTag();
	        fresh.putString("blockId", currentBlockId);
	        fresh.putDouble("creationDate", days);
	        fresh.putLong("placedGameTime", now);
	        fresh.putBoolean("dateSet", true);
	        expiryData.setExpiryData(pos, fresh);
	        return;
	    }
	    
	    String savedBlockId = existing.getString("blockId");
	    boolean dateSet = existing.getBoolean("dateSet");
	    long placedGameTime = existing.contains("placedGameTime") ? existing.getLong("placedGameTime") : now;
	    
	    if (!dateSet || !currentBlockId.equals(savedBlockId)) {
	        existing.putString("blockId", currentBlockId);
	        existing.putDouble("creationDate", days);
	        existing.putLong("placedGameTime", now);
	        existing.putBoolean("dateSet", true);
	        expiryData.setExpiryData(pos, existing);
	        return;
	    }
	    
	    if (now - placedGameTime < 2L) return;
	    
	    double creationDate = existing.getDouble("creationDate");
	    if (creationDate < 0 || creationDate > days) {
	        existing.putDouble("creationDate", days);
	        expiryData.setExpiryData(pos, existing);
	        creationDate = days;
	    }
	    
	    if (days - creationDate <= Settings.getDaysBeforeItExpires()) return;
	    
	    // Time to spoil, just replace the block.
	    // We should not drop the block, as it's meant to be replaced by the moldy version.
	    BlockState moldyState = ThingsThatCanExpire.getRotten(level, state, pos);
	    level.setBlock(pos, moldyState, 3);
	    
	    // Remove the expiry data since the block has been spoiled
	    expiryData.removeExpiryData(pos);
	}
}