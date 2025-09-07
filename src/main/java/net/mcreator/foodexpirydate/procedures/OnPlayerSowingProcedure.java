package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component; // For displaying messages

import net.mcreator.foodexpirydate.init.FoodExpiryDateModBlocks; 
import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class OnPlayerSowingProcedure {

    // This event is triggered when a player right-clicks a block
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Make sure the event is not canceled and the player is interacting properly
        if (event.isCanceled()) return;

        execute(event, event.getLevel(), event.getPos());
    }

    // Main execution method
    public static void execute(PlayerInteractEvent.RightClickBlock event, LevelAccessor world, BlockPos pos) {
        Entity entity = event.getEntity();
        if (entity == null || world == null)
            return;

        // Get the item the player is holding
        ItemStack heldItem = event.getEntity().getMainHandItem();
        Item item = heldItem.getItem();

        // --- Logic to prevent placing the raw potato/carrot items (if they are placeable) ---
        // If you want to prevent players from placing the actual potato or carrot item
        // as a block, you can keep this check.
        if (item == Items.POTATO || item == Items.CARROT) {
            // Check if the target block allows interaction
            BlockState targetState = world.getBlockState(pos);
            Block targetBlock = targetState.getBlock();

            if (targetBlock == Blocks.FARMLAND) { // Example: only prevent planting on farmland
                // Cancel the event to prevent the action
                event.setCanceled(true);

                // Send a message to the player's hotbar (action bar)
                if (entity instanceof Player player) {
                    player.displayClientMessage(Component.literal("§o§aYou cannot plant potatoes or carrots, you need to turn them into seeds first."), true);
                }
            }
            return; // Exit if we handled raw potato/carrot placement
        }

        // --- Logic for sowing custom seeds ---
        // Check if the held item is your custom potato seeds or carrot seeds
        if (item == FoodExpiryDateModItems.POTATO_SEEDS.get() || item == FoodExpiryDateModItems.CARROT_SEEDS.get()) {
            BlockState targetState = world.getBlockState(pos);
            Block targetBlock = targetState.getBlock();
            BlockState nextState = world.getBlockState(
            	new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ())
            );
            Block nextStatetBlock = nextState.getBlock();

            // Check if the player is trying to plant on farmland and are not used
            if (targetBlock == Blocks.FARMLAND && nextStatetBlock == Blocks.AIR) {
                // Cancel the default right-click action to prevent placing the seed item itself
                event.setCanceled(true);

                // Determine which crop block to place based on the seed
                Block cropToPlace = null;
                if (item == FoodExpiryDateModItems.POTATO_SEEDS.get())
                    cropToPlace = Blocks.POTATOES;
                else if (item == FoodExpiryDateModItems.CARROT_SEEDS.get())
                    cropToPlace = Blocks.CARROTS;

                // If we have a valid crop block to place
                if (cropToPlace != null) {
                	BlockPos newPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
                    // Place the crop block
                    world.setBlock(newPos, cropToPlace.defaultBlockState(), 3);

                    // Consume one seed from the player's hand
                    heldItem.shrink(1);
                } else {
                    // If for some reason the crop block wasn't found, inform the player
                    if (entity instanceof Player player) {
                        player.displayClientMessage(Component.literal("§o§cError: Could not find the correct crop block to plant."), true);
                    }
                }
            }
        }
    }
}