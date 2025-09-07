package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.foodexpirydate.ExpiryData;
import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;
import net.mcreator.foodexpirydate.Settings;
import net.mcreator.foodexpirydate.SetFoodExpiryState;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class FoodExpiryDateForBlocksProcedure {

    public static final ConcurrentHashMap<BlockPos, OnBlockBreakProcedure.Tombstone> RECENT_BROKEN = new ConcurrentHashMap<>();

    private static final long RESTORE_WINDOW_TICKS = 20L * 60L;
    private static final long TOMBSTONE_KEEP_TICKS = 20L * 300L;

    // Tune this: 1 = check 3x3 area, 2 = check 5x5 area, etc.
    private static final int STEM_CHECK_RADIUS = 2;

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent event) {
        if (event.phase != LevelTickEvent.Phase.END || event.level.isClientSide()) return;
        execute(event.level);
    }

    private static void execute(@Nullable LevelAccessor worldAcc) {
        if (!(worldAcc instanceof ServerLevel serverLevel)) return;

        int radius = Settings.getRadiusOfTheAreaToScanForBlocks();
        if (radius < 1) return;

        double days = FoodExpiryDateModVariables.MapVariables.get(serverLevel).daysPassed;
        ExpiryData expiryData = ExpiryData.get(serverLevel);

        // 1) Process pending block placements
        OnBlockPlaceProcedure.PendingEntry entry;
        while ((entry = OnBlockPlaceProcedure.PENDING.poll()) != null) {
            BlockPos p = entry.pos;
            if (p == null) continue;
            if (!serverLevel.hasChunkAt(p)) {
                OnBlockPlaceProcedure.PENDING.add(entry);
                continue;
            }
            BlockState st = serverLevel.getBlockState(p);
            if (st == null) continue;

            // Restore tombstone data if a block was broken and immediately re-placed.
            OnBlockBreakProcedure.Tombstone tomb = RECENT_BROKEN.remove(p.immutable());
            if (tomb != null && (serverLevel.getGameTime() - tomb.brokenGameTime) <= RESTORE_WINDOW_TICKS) {
                expiryData.setExpiryData(p, tomb.data);
            } else {
                // Otherwise, create new data if it doesn't already exist.
                if (!expiryData.hasExpiryData(p)) {
                    CompoundTag fresh = (entry.tag == null) ? new CompoundTag() : entry.tag.copy();
                    fresh.putBoolean("playerPlaced", true); // explicitly mark as player placed
                    expiryData.setExpiryData(p, fresh);
                }
            }
        }

        // 2) Prune old tombstones
        try {
            long now = serverLevel.getGameTime();
            List<BlockPos> toRemove = new ArrayList<>();
            for (Map.Entry<BlockPos, OnBlockBreakProcedure.Tombstone> e : RECENT_BROKEN.entrySet()) {
                if ((now - e.getValue().brokenGameTime) > TOMBSTONE_KEEP_TICKS) {
                    toRemove.add(e.getKey());
                }
            }
            toRemove.forEach(RECENT_BROKEN::remove);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // 3) Scan for crops and non-crop food blocks around players
        for (Player player : new ArrayList<>(serverLevel.players())) {
            BlockPos center = player.blockPosition();
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = serverLevel.getBlockState(pos);
                        if (state == null) continue;

                        Block block = state.getBlock();

                        // --- Fully grown crops ---
                        if ((block instanceof IPlantable || block instanceof CropBlock) && ThingsThatCanExpire.isFullyGrown(serverLevel, state, pos)) {
                            if (!expiryData.hasExpiryData(pos)) {
                                CompoundTag fresh = new CompoundTag();
                                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                                fresh.putString("blockId", id != null ? id.toString() : "");
                                fresh.putDouble("creationDate", days);
                                fresh.putBoolean("dateSet", true);
                                fresh.putLong("placedGameTime", serverLevel.getGameTime());
                                // keep track whether seed was player-placed
                                fresh.putBoolean("playerPlaced", wasPlacedByPlayer(serverLevel, pos));
                                expiryData.setExpiryData(pos, fresh);
                            }

                            CompoundTag data = expiryData.getExpiryData(pos);
                            double creationDate = data.getDouble("creationDate");
                            if (days - creationDate > Settings.getDaysBeforeItExpires()) {
                                SetFoodExpiryState.updateExpiryState(days, state, pos, serverLevel);
                            }
                        }

                        // --- Pumpkins/Melons & other food blocks ---
                        else if (ThingsThatCanExpire.isBlockFood(serverLevel, state, pos)) {
                            if (!expiryData.hasExpiryData(pos)) {
                                boolean isStemNearby = false;

                                // Robust stem detection: check all horizontal positions within STEM_CHECK_RADIUS
                                for (int dx = -STEM_CHECK_RADIUS; dx <= STEM_CHECK_RADIUS && !isStemNearby; dx++) {
                                    for (int dz = -STEM_CHECK_RADIUS; dz <= STEM_CHECK_RADIUS; dz++) {
                                        if (dx == 0 && dz == 0) continue; // skip the block itself
                                        BlockPos adj = pos.offset(dx, 0, dz);
                                        Block adjBlock = serverLevel.getBlockState(adj).getBlock();
                                        ResourceLocation adjId = ForgeRegistries.BLOCKS.getKey(adjBlock);
                                        if (adjId != null && (
                                            adjId.toString().equals("minecraft:melon_stem") ||
                                            adjId.toString().equals("minecraft:pumpkin_stem") ||
                                            // some versions have 'attached_*' variants; include them defensively
                                            adjId.toString().equals("minecraft:attached_melon_stem") ||
                                            adjId.toString().equals("minecraft:attached_pumpkin_stem"))) {
                                            isStemNearby = true;
                                            break;
                                        }
                                    }
                                }

                                if (isStemNearby || wasPlacedByPlayer(serverLevel, pos)) {
                                    CompoundTag fresh = new CompoundTag();
                                    ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
                                    fresh.putString("blockId", id != null ? id.toString() : "");
                                    fresh.putDouble("creationDate", days);
                                    fresh.putBoolean("dateSet", true);
                                    fresh.putLong("placedGameTime", serverLevel.getGameTime());
                                    // mark as spoilable (grown from stem or player-placed)
                                    fresh.putBoolean("playerPlaced", true);
                                    expiryData.setExpiryData(pos, fresh);
                                }
                            }

                            if (expiryData.hasExpiryData(pos)) {
                                CompoundTag data = expiryData.getExpiryData(pos);
                                double creationDate = data.getDouble("creationDate");
                                if (days - creationDate > Settings.getDaysBeforeItExpires()) {
                                	if(state.getBlock() instanceof CandleCakeBlock) {
										// Capture original drops using the Block API (ServerLevel)
							        	List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, null);
							
							        	// Replace with rotten/moldy state (getRotten should handle CandleCakeBlock generically)
							        	BlockState moldyState = ThingsThatCanExpire.getRotten(serverLevel, state, pos);
							        	serverLevel.setBlock(pos, moldyState, 3);
							
							        	// Spawn the original drops we captured earlier
										for (ItemStack drop : drops) {
								            if (!drop.isEmpty()) Block.popResource(serverLevel, pos, drop);
										}
                                	}
                                	
                                    BlockState moldy = ThingsThatCanExpire.getRotten(serverLevel, state, pos);
                                    serverLevel.setBlock(pos, moldy, 3);
                                    expiryData.removeExpiryData(pos);
                                }
                            }
                        }

                        // --- Remove expiry data for non-food blocks ---
                        else if (expiryData.hasExpiryData(pos)) {
                            expiryData.removeExpiryData(pos);
                        }
                    }
                }
            }
        }
    }

    // --- Helper to check if block was placed by player ---
    private static boolean wasPlacedByPlayer(ServerLevel serverLevel, BlockPos pos) {
        ExpiryData expiryData = ExpiryData.get(serverLevel);
        if (expiryData.hasExpiryData(pos)) {
            CompoundTag existing = expiryData.getExpiryData(pos);
            if (existing.contains("playerPlaced") && existing.getBoolean("playerPlaced")) {
                return true;
            }
        }
        if (RECENT_BROKEN.containsKey(pos)) return true;

        for (OnBlockPlaceProcedure.PendingEntry e : OnBlockPlaceProcedure.PENDING) {
            if (pos.equals(e.pos)) {
                return true;
            }
        }
        return false;
    }
}