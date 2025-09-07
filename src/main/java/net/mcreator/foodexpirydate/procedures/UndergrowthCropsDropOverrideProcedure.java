package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;

@Mod.EventBusSubscriber
public class UndergrowthCropsDropOverrideProcedure {
	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent event) {
		Level level = (Level) event.getLevel();
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

		try {
            BlockPos pos = event.getPos();
            if (pos == null) return;

            execute(level, pos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
	
    public static void execute(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof CropBlock)) return;
        int age = state.getValue(CropBlock.AGE);
        if (age >= 7) return; // fully grown -> keep vanilla behavior

        // not fully grown -> remove without default drops and spawn our item
        world.destroyBlock(pos, false);
        ItemStack drop = block == Blocks.CARROTS ? new ItemStack(FoodExpiryDateModItems.CARROT_SEEDS.get())
                         : block == Blocks.POTATOES ? new ItemStack(FoodExpiryDateModItems.POTATO_SEEDS.get())
                         : ItemStack.EMPTY;
        if (!drop.isEmpty()) Block.popResource(world, pos, drop);
    }
}