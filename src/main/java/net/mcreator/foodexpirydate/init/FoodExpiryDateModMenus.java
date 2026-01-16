/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.Minecraft;

import net.mcreator.foodexpirydate.world.inventory.FreezerGUIMenu;
import net.mcreator.foodexpirydate.world.inventory.FoodDryingRackGUIMenu;
import net.mcreator.foodexpirydate.network.MenuStateUpdateMessage;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;

import java.util.Map;

public class FoodExpiryDateModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, FoodExpiryDateMod.MODID);
	public static final RegistryObject<MenuType<FoodDryingRackGUIMenu>> FOOD_DRYING_RACK_GUI = REGISTRY.register("food_drying_rack_gui", () -> IForgeMenuType.create(FoodDryingRackGUIMenu::new));
	public static final RegistryObject<MenuType<FreezerGUIMenu>> FREEZER_GUI = REGISTRY.register("freezer_gui", () -> IForgeMenuType.create(FreezerGUIMenu::new));

	public interface MenuAccessor {
		Map<String, Object> getMenuState();

		Map<Integer, Slot> getSlots();

		default void sendMenuStateUpdate(Player player, int elementType, String name, Object elementState, boolean needClientUpdate) {
			getMenuState().put(elementType + ":" + name, elementState);
			if (player instanceof ServerPlayer serverPlayer) {
				FoodExpiryDateMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new MenuStateUpdateMessage(elementType, name, elementState));
			} else if (player.level().isClientSide) {
				if (Minecraft.getInstance().screen instanceof FoodExpiryDateModScreens.ScreenAccessor accessor && needClientUpdate)
					accessor.updateMenuState(elementType, name, elementState);
				FoodExpiryDateMod.PACKET_HANDLER.sendToServer(new MenuStateUpdateMessage(elementType, name, elementState));
			}
		}

		default <T> T getMenuState(int elementType, String name, T defaultValue) {
			try {
				return (T) getMenuState().getOrDefault(elementType + ":" + name, defaultValue);
			} catch (ClassCastException e) {
				return defaultValue;
			}
		}
	}
}