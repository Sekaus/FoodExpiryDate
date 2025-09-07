package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;
import net.mcreator.foodexpirydate.SetFoodExpiryState;
import net.mcreator.foodexpirydate.init.FoodExpiryDateModBlocks;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;
import net.mcreator.foodexpirydate.block.entity.FreezerBlockEntity;
import net.mcreator.foodexpirydate.Settings;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FoodExpiryDateForInventoryProcedure {
    /**
     * This event listener is subscribed to the Forge event bus and runs at the end of every world tick.
     * It ensures the expiry date logic is applied to all relevant inventories on the server side.
     */
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent event) {
        // Ensure the event is from the end of a server-side world tick.
        if (event.phase != LevelTickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        execute(event.level);
    }

    private static void execute(@Nullable LevelAccessor worldAcc) {
        if (!(worldAcc instanceof Level world)) return;

        // Radius of the area around each player to scan for block inventories.
        int radius = Settings.getRadiusOfTheAreaToScanForBlocks();
        if (radius < 1) return;

        double days = FoodExpiryDateModVariables.MapVariables.get(world).daysPassed;

        // Iterate directly over the list of players to process their inventories first.
        for (Player player : world.players()) {
            BlockPos center = player.blockPosition();

            // Update the player's own inventory.
            player.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
                if (handler instanceof IItemHandlerModifiable modHandler) {
                    for (int slot = 0; slot < modHandler.getSlots(); slot++) {
                        ItemStack stack = modHandler.getStackInSlot(slot);
                        SetFoodExpiryState.updateExpiryState(days, stack, world, modHandler, slot, false);
                    }
                }
            });

            // Scan block inventories in a cube around the player.
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        BlockEntity be = world.getBlockEntity(pos);
                        if (be == null) continue;

                        be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(handler -> {
                            if (handler instanceof IItemHandlerModifiable modHandler) {
                                boolean isFreezer = be instanceof FreezerBlockEntity;
                                for (int slot = 0; slot < modHandler.getSlots(); slot++) {
                                    ItemStack stack = modHandler.getStackInSlot(slot);
                                    // Use a boolean to indicate if this is a freezer, which requires special handling.
                                    SetFoodExpiryState.updateExpiryState(days, stack, world, modHandler, slot, isFreezer);
                                }
                            }
                        });
                    }
                }
            }
        }
    }
}