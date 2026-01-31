package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import net.mcreator.foodexpirydate.ExpiryData;

import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public class OnBlockPlaceProcedure {

    public static final ConcurrentLinkedQueue<PendingEntry> PENDING = new ConcurrentLinkedQueue<>();

    public static class PendingEntry {
        public final BlockPos pos;
        public final CompoundTag tag;

        public PendingEntry(BlockPos pos, CompoundTag tag) {
            this.pos = pos;
            this.tag = tag;
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        CompoundTag tag = null;

        Entity placer = event.getEntity();
        if (placer instanceof LivingEntity living) {
            ItemStack stack = living.getMainHandItem();
            if (stack.hasTag()) {
                tag = stack.getTag().copy();
            }
        }

        // IMPORTANT:
        // If placer == null → natural placement (growth, worldgen, etc.)
        // That is intentionally handled later by your tick logic
        PENDING.add(new PendingEntry(event.getPos(), tag));
    }

    public static void processPending(ServerLevel serverLevel) {
        ExpiryData expiryData = ExpiryData.get(serverLevel);
        PendingEntry entry;

        while ((entry = PENDING.poll()) != null) {
            if (!serverLevel.hasChunkAt(entry.pos)) continue;

            if (entry.tag != null && entry.tag.contains("creationDate")) {
			    expiryData.setExpiryData(entry.pos, entry.tag.copy());
			}
        }
    }
}