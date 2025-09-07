package net.mcreator.foodexpirydate.network;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Item;
import net.minecraft.core.BlockPos;

import net.minecraftforge.registries.ForgeRegistries;

public class ClientSpawnHelper {
    public static void requestSpawnDisplayClient(BlockPos pos, int slot,
                                                 Item item,
                                                 int rotX, int rotY, int rotZ,
                                                 float scale, boolean billboard) {
        net.mcreator.foodexpirydate.network.NetworkHandler.sendToServer(new SpawnDisplayPacket(
                pos, slot, item, rotX, rotY, rotZ, scale, billboard
        ));
    }
}

