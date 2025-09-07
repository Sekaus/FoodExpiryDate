package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.IPlantable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public class OnBlockPlaceProcedure {
    public static class PendingEntry {
        public final BlockPos pos;
        public final CompoundTag tag;

        public PendingEntry(BlockPos pos, CompoundTag tag) {
            this.pos = (pos == null) ? null : pos.immutable();
            this.tag = (tag == null) ? new CompoundTag() : tag.copy();
        }
    }

    public static final ConcurrentLinkedQueue<PendingEntry> PENDING = new ConcurrentLinkedQueue<>();

    // toggle for debug logging
    private static final boolean DEBUG = true;

    @SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
	    Level level = (Level) event.getLevel();
	    if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
	
	    try {
	        BlockPos pos = event.getPos();
	        BlockState placedState = event.getPlacedBlock();
	        if (placedState == null || pos == null) return;
	
	        Block block = placedState.getBlock();
	
	        // Check if the block is a trackable food block that is not a crop.
	        // The check now specifically allows cake, melon, and pumpkin to pass.
	        boolean isTrackableNonCropFood = ThingsThatCanExpire.isBlockFood(serverLevel, placedState, pos)
	                && !(block instanceof IPlantable || block instanceof CropBlock);
	
	        // Explicitly check for cake, melon, and pumpkin blocks which are not IPlantable
	        if (block instanceof CakeBlock || block == Blocks.MELON || block == Blocks.PUMPKIN || block == Blocks.CARVED_PUMPKIN) {
	            isTrackableNonCropFood = true;
	        }
	
	        if (!isTrackableNonCropFood) return;
	
	        CompoundTag tag = new CompoundTag();
	        double days = FoodExpiryDateModVariables.MapVariables.get(serverLevel).daysPassed;
	
	        Entity entity = event.getEntity();
	        if (entity instanceof Player player) {
	            try {
	                ItemStack used = player.getMainHandItem();
	                if (used == null || used.isEmpty()) used = player.getOffhandItem();
	
	                if (used != null && !used.isEmpty() && used.hasTag()) {
	                    CompoundTag itemTag = used.getTag();
	                    if (itemTag != null && itemTag.getBoolean("dateSet")) {
	                        tag.putDouble("creationDate", itemTag.getDouble("creationDate"));
	                        tag.putBoolean("dateSet", true);
	                    }
	                }
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	
	        if (!tag.contains("dateSet")) {
	            tag.putDouble("creationDate", days);
	            tag.putBoolean("dateSet", true);
	        }
	
	        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(placedState.getBlock());
	        if (id != null) tag.putString("blockId", id.toString());
	
	        PendingEntry pending = new PendingEntry(pos, tag);
	        PENDING.add(pending);
	        if (DEBUG) System.out.println("[Expiry][Place] queued placement at " + pos + " block=" + block);
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}
}