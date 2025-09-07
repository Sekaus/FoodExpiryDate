package net.mcreator.foodexpirydate.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

import net.mcreator.foodexpirydate.network.FoodExpiryDateModVariables;
import net.mcreator.foodexpirydate.SetFoodExpiryState;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;

/**
 * This event listener handles the logic for item entities that are spawned into the world.
 * It ensures that food items that are not dried are assigned an expiry date.
 */
@Mod.EventBusSubscriber
public class FoodExpiryDateForItemEntityProcedureProcedure {
    @SubscribeEvent
    public static void onEntitySpawned(EntityJoinLevelEvent event) {
        execute(event, event.getLevel(), event.getEntity());
    }

    public static void execute(LevelAccessor world, Entity entity) {
        execute(null, world, entity);
    }

    private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
        // Only proceed if the entity is a server-side ItemEntity.
        if (!(entity instanceof ItemEntity) || world.isClientSide()) {
            return;
        }

        ItemEntity itemEntity = (ItemEntity) entity;
        ItemStack stack = itemEntity.getItem();

        // Check if the item is food and its expiry date has not been set yet.
        if (ThingsThatCanExpire.isFood(stack) && !stack.getOrCreateTag().getBoolean("dateSet")) {
            // Get the current day from the world variables.
            double days = FoodExpiryDateModVariables.MapVariables.get(world).daysPassed;

            // Set the creation date and the 'dateSet' flag directly on the item's NBT.
            // This is the simplest and most efficient way to update a dropped item.
            // There is no need to discard and re-create the entity, as the NBT data
            // will be synchronized to the client automatically by the game.
            stack.getOrCreateTag().putDouble("creationDate", days);
            stack.getOrCreateTag().putBoolean("dateSet", true);
        }
    }
}
