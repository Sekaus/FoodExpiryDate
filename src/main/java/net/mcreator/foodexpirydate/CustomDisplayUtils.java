package net.mcreator.foodexpirydate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;

public class CustomDisplayUtils {

    public static Entity spawnItemDisplay(Level world, double x, double y, double z, Item item, float rotX, float rotY, float rotZ, float scale, boolean showPedestal) {
        if (!(world instanceof ServerLevel serverLevel)) return null;

        // 1. Create the entity instance
        Entity entity = EntityType.ITEM_DISPLAY.create(serverLevel);
        if (entity == null) return null;

        // 2. Build the NBT Data
        CompoundTag nbt = new CompoundTag();
        
        // Position and Rotation
        ListTag pos = new ListTag();
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(x));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(y - 0.1F));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(z));
        nbt.put("Pos", pos);

        ListTag rotation = new ListTag();
        rotation.add(FloatTag.valueOf(rotY)); // Yaw
        rotation.add(FloatTag.valueOf(rotX)); // Pitch
        nbt.put("Rotation", rotation);

        // Save Item Data
        ItemStack stack = new ItemStack(item);
        CompoundTag itemTag = new CompoundTag();
        stack.save(itemTag);
        nbt.put("item", itemTag);

        // Transformation (Scale)
        // In 1.20.1, the transformation tag uses 'scale' as a list of 3 floats
        CompoundTag transformation = new CompoundTag();
        ListTag scaleList = new ListTag();
        scaleList.add(FloatTag.valueOf(scale));
        scaleList.add(FloatTag.valueOf(scale));
        scaleList.add(FloatTag.valueOf(scale));
        transformation.put("scale", scaleList);
        nbt.put("transformation", transformation);

        // Display Settings
        // "ground" makes it look like a dropped item, "fixed" is for standing still
        nbt.putString("item_display", showPedestal ? "ground" : "fixed");
        
        // Optional: Make it full bright so it's not black in shadows
        nbt.putInt("brightness", 15728880); 

        // 4. Load NBT and Spawn
        entity.load(nbt);
        serverLevel.addFreshEntity(entity);
        
        return entity;
    }
}