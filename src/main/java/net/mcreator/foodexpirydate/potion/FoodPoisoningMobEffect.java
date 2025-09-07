
package net.mcreator.foodexpirydate.potion;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.mcreator.foodexpirydate.procedures.FoodPoisoningProcedureProcedure;

public class FoodPoisoningMobEffect extends MobEffect {
	public FoodPoisoningMobEffect() {
		super(MobEffectCategory.HARMFUL, -6711040);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		FoodPoisoningProcedureProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
