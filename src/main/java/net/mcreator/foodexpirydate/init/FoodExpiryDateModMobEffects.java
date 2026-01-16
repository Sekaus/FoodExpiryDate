/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.foodexpirydate.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.effect.MobEffect;

import net.mcreator.foodexpirydate.potion.FoodPoisoningMobEffect;
import net.mcreator.foodexpirydate.FoodExpiryDateMod;

public class FoodExpiryDateModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FoodExpiryDateMod.MODID);
	public static final RegistryObject<MobEffect> FOOD_POISONING = REGISTRY.register("food_poisoning", () -> new FoodPoisoningMobEffect());
}