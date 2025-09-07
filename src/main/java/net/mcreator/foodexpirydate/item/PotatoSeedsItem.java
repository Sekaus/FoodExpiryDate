package net.mcreator.foodexpirydate.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class PotatoSeedsItem extends Item {
	public PotatoSeedsItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
	}
}