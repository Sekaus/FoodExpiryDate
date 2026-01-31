package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.mcreator.foodexpirydate.ExpiryData;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

import java.util.List;

@Mod.EventBusSubscriber
public class OnBlockBreakProcedure {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (!ThingsThatCanExpire.isBlockFood(serverLevel, state, pos)) return;

        ExpiryData expiryData = ExpiryData.get(serverLevel);
        CompoundTag expiryTag = expiryData.hasExpiryData(pos)
                ? expiryData.getExpiryData(pos).copy()
                : null;

        // Generate vanilla drops manually
        List<ItemStack> drops = Block.getDrops(
                state,
                serverLevel,
                pos,
                null,
                player,
                player.getMainHandItem()
        );

        // Attach expiry data
        if (expiryTag != null) {
            for (ItemStack stack : drops) {
                stack.setTag(expiryTag.copy());
            }
            expiryData.removeExpiryData(pos);
        }

        // Cancel vanilla break
        event.setCanceled(true);
        serverLevel.removeBlock(pos, false);

        // Spawn modified drops
        for (ItemStack stack : drops) {
            Block.popResource(serverLevel, pos, stack);
        }
    }
}