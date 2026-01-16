package net.mcreator.foodexpirydate.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.network.chat.Component;

import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;

public class GetDayCountProcedureProcedure {
	public static void execute(LevelAccessor world) {
		if (!world.isClientSide() && world.getServer() != null)
			world.getServer().getPlayerList().broadcastSystemMessage(Component.literal(("Days passed: " + FoodExpiryDateModVariables.MapVariables.get(world).daysPassed)), false);
	}
}
