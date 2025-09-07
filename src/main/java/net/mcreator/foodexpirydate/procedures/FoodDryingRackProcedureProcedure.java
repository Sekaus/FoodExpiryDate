package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;
import net.mcreator.foodexpirydate.Settings;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodDryingRackProcedureProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        // Ensure this procedure only runs on the server side
        if (world.isClientSide()) {
            return;
        }

        BlockPos pos = BlockPos.containing(x, y, z);
        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (blockEntity == null) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            final int slotid = i;

            Optional<IItemHandlerModifiable> capabilityOptional = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .filter(IItemHandlerModifiable.class::isInstance)
                .map(IItemHandlerModifiable.class::cast);

            if (capabilityOptional.isPresent()) {
                IItemHandlerModifiable capability = capabilityOptional.get();
                ItemStack stack = capability.getStackInSlot(slotid);

                if (!stack.isEmpty()) {
                    double daysPassed = FoodExpiryDateModVariables.MapVariables.get(world).daysPassed - stack.getOrCreateTag().getDouble("setDate");

                    if (daysPassed > Settings.getDaysBeforeItIsDried()
                            && !(stack.getItem() == Items.MILK_BUCKET || stack.getItem() == Items.CAKE || stack.getItem() == Items.EGG)) {
                        
                        ItemStack updatedStack = stack.copy();
                        updatedStack.getOrCreateTag().putBoolean("dried", true);
                        capability.setStackInSlot(slotid, updatedStack);
                        blockEntity.setChanged(); // Mark the block entity as dirty to save the changes
                    }
                }
            }
        }
    }
}