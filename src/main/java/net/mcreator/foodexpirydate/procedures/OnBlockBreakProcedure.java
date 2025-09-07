package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.foodexpirydate.ExpiryData;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

/**
 * Handles tombstone creation when food blocks are broken.
 */
@Mod.EventBusSubscriber(modid = "foodexpirydate")
public class OnBlockBreakProcedure {
    public static class Tombstone {
        public final CompoundTag data;
        public final long brokenGameTime;

        public Tombstone(CompoundTag data, long brokenGameTime) {
            this.data = (data == null) ? new CompoundTag() : data.copy();
            this.brokenGameTime = brokenGameTime;
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        try {
            BlockPos pos = event.getPos();
            if (pos == null) return;

            BlockState state = serverLevel.getBlockState(pos);
            if (state == null) return;

            // Only process blocks that can expire
            if (!ThingsThatCanExpire.isBlockFood(serverLevel, state, pos)) return;

            ExpiryData expiryData = ExpiryData.get(serverLevel);
            if (!expiryData.hasExpiryData(pos)) return;

            // Grab and remove expiry data
            CompoundTag old = expiryData.getExpiryDataOrNull(pos);
            expiryData.removeExpiryData(pos);

            // Store tombstone in shared map
            long now = serverLevel.getGameTime();
            FoodExpiryDateForBlocksProcedure.RECENT_BROKEN.put(pos.immutable(), new Tombstone(old, now));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
