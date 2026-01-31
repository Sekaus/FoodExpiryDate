package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import net.mcreator.foodexpirydate.ExpiryData;
import net.mcreator.foodexpirydate.Settings;
import net.mcreator.foodexpirydate.SetFoodExpiryState;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;
import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class FoodExpiryDateForBlocksProcedure {

    private static final long RESTORE_WINDOW_TICKS = 20L * 60L;
    private static final long TOMBSTONE_KEEP_TICKS = 20L * 300L;
    private static final int STEM_CHECK_RADIUS = 2;

    /**
     * Called every world tick
     */
    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent event) {
        if (event.phase != LevelTickEvent.Phase.END || event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;

        ExpiryData expiryData = ExpiryData.get(serverLevel);
        double days = FoodExpiryDateModVariables.MapVariables.get(serverLevel).daysPassed;

        // 1) Process pending block placements
        OnBlockPlaceProcedure.processPending(serverLevel);

        // 2) Scan around players for food blocks/crops
        int radius = Settings.getRadiusOfTheAreaToScanForBlocks();
        if (radius < 1) return;

        for (Player player : new ArrayList<>(serverLevel.players())) {
            BlockPos center = player.blockPosition();

            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = serverLevel.getBlockState(pos);
                        if (state == null) continue;

                        if (state.isAir() || !ThingsThatCanExpire.isBlockFood(serverLevel, state, pos)) continue; 

                        Block block = state.getBlock();

                        // --- Fully grown crops ---
                        if ((block instanceof CropBlock) && ThingsThatCanExpire.isFullyGrown(serverLevel, state, pos)) {
                            processCrop(serverLevel, expiryData, pos, block, state, days);
                        }

                        // --- Food blocks (pumpkin, melon, cake, candle cake, modded food) ---
                        else if (ThingsThatCanExpire.isBlockFood(serverLevel, state, pos)) {
                            processFoodBlock(serverLevel, expiryData, pos, block, state, days);
                        }

                        // --- Non-food blocks: remove expiry data ---
                        else if (expiryData.hasExpiryData(pos)) {
                            expiryData.removeExpiryData(pos);
                        }
                    }
                }
            }
        }
    }

    // ---------------- Helper Methods ----------------

    private static void processCrop(ServerLevel serverLevel, ExpiryData expiryData, BlockPos pos, Block block, BlockState state, double days) {
		if (!expiryData.hasExpiryData(pos)) {
		    // Check if it's a naturally grown fruit or a valid player placement
		    if (isStemNearby(serverLevel, pos)) {
		        CompoundTag fresh = new CompoundTag(); // No 'entry' needed here
		        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
		        
		        fresh.putString("blockId", id != null ? id.toString() : "");
		        fresh.putDouble("creationDate", days); // Naturally grown blocks get the current day
		        fresh.putBoolean("dateSet", true);
		        fresh.putLong("placedGameTime", serverLevel.getGameTime());
		        
		        expiryData.setExpiryData(pos, fresh);
		    }
		}

        CompoundTag data = expiryData.getExpiryData(pos);
        double creationDate = data.getDouble("creationDate");
        if (days - creationDate > Settings.getDaysBeforeItExpires()) {
            SetFoodExpiryState.updateExpiryState(days, state, pos, serverLevel);
        }
    }

	private static void processFoodBlock(ServerLevel serverLevel, ExpiryData expiryData, BlockPos pos, Block block, BlockState state, double days) {
		// 1. Check if we are currently waiting for a placement to finish at this position
	    for (OnBlockPlaceProcedure.PendingEntry e : OnBlockPlaceProcedure.PENDING) {
	        if (pos.equals(e.pos)) return; // STOP: Placement is still processing, don't assign "Today" yet!
	    }
	    
	    if (!expiryData.hasExpiryData(pos)) {
	        // ONLY if it is truly empty do we assign "today"
	        System.out.println("[FoodExpiry] TICK found new block at " + pos + " | Assigning Today: " + days);
	        
	        CompoundTag fresh = new CompoundTag();
	        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
	        fresh.putString("blockId", id != null ? id.toString() : "");
	        fresh.putDouble("creationDate", days);
	        fresh.putBoolean("dateSet", true);
	        expiryData.setExpiryData(pos, fresh);
	    }
	
	    // 2. Check for expiration
	    if (expiryData.hasExpiryData(pos)) {
	        CompoundTag data = expiryData.getExpiryData(pos);
	        double creationDate = data.getDouble("creationDate");
	
	        if (days - creationDate > Settings.getDaysBeforeItExpires()) {
	            BlockState moldy = ThingsThatCanExpire.getRotten(serverLevel, state, pos);
	            ItemStack drop = new ItemStack(state.getBlock().asItem());
	            
	            // Transfer NBT to the item drop so the ITEM remembers the date
	            drop.setTag(data.copy()); 
	            
	            serverLevel.setBlock(pos, moldy, 3);
	            expiryData.removeExpiryData(pos);
	        }
	    }
	}

    private static boolean isStemNearby(ServerLevel serverLevel, BlockPos pos) {
	    for (int dx = -STEM_CHECK_RADIUS; dx <= STEM_CHECK_RADIUS; dx++) {
	        for (int dz = -STEM_CHECK_RADIUS; dz <= STEM_CHECK_RADIUS; dz++) {
	            if (dx == 0 && dz == 0) continue;
	            BlockPos adj = pos.offset(dx, 0, dz);
	            Block adjBlock = serverLevel.getBlockState(adj).getBlock();
	            net.minecraft.resources.ResourceLocation adjId = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(adjBlock);
	            if (adjId != null && (adjId.toString().contains("melon_stem") || adjId.toString().contains("pumpkin_stem"))) {
	                return true;
	            }
	        }
	    }
	    return false;
	}
}