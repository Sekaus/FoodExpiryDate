package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import net.mcreator.foodexpirydate.init.FoodExpiryDateModItems;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

import javax.annotation.Nullable;

import java.util.List;

@Mod.EventBusSubscriber
public class DisplayDateOfRegistrationProcedureProcedure {
	private static final String statusText = "\u00A7l\u00A7bStatus:\u00A7r ";
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		execute(event, event.getItemStack(), event.getToolTip());
	}

	public static void execute(ItemStack itemstack, List<Component> tooltip) {
		execute(null, itemstack, tooltip);
	}

	private static void execute(@Nullable Event event, ItemStack itemstack, List<Component> tooltip) {
		if (tooltip == null || !ThingsThatCanExpire.isFood(itemstack))
			return;

		tooltip.add(Component.literal(("\u00A7l\u00A7bCreation Date:\u00A7r " + itemstack.getOrCreateTag().getDouble("creationDate"))));

		if (itemstack.getOrCreateTag().getBoolean("dried")) 
			tooltip.add(Component.literal((statusText + "Dried"))); 
		else
			tooltip.add(Component.literal((statusText + "Fresh")));
	}
}
