package net.mcreator.foodexpirydate.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import net.mcreator.foodexpirydate.CustomDisplayUtils;
import net.mcreator.foodexpirydate.block.entity.FoodDryingRackBlockEntity;

import java.util.function.Supplier;

public class SpawnDisplayPacket {
    private final BlockPos pos;
    private final int slot;
    private final Item item;
    private final int rotX, rotY, rotZ;
    private final float scale;
    private final boolean billboard;

    public SpawnDisplayPacket(BlockPos pos, int slot, Item item,
                              int rotX, int rotY, int rotZ,
                              float scale, boolean billboard) {
        this.pos = pos;
        this.slot = slot;
        this.item = item;
        this.rotX = rotX;
        this.rotY = rotY;
        this.rotZ = rotZ;
        this.scale = scale;
        this.billboard = billboard;
    }

    public static void encode(SpawnDisplayPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeVarInt(msg.slot);
        buf.writeVarInt(Item.getId(msg.item));
        buf.writeInt(msg.rotX);
        buf.writeInt(msg.rotY);
        buf.writeInt(msg.rotZ);
        buf.writeFloat(msg.scale);
        buf.writeBoolean(msg.billboard);
    }

    public static SpawnDisplayPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int slot = buf.readVarInt();
        Item item = Item.byId(buf.readVarInt());
        int rotX = buf.readInt();
        int rotY = buf.readInt();
        int rotZ = buf.readInt();
        float scale = buf.readFloat();
        boolean billboard = buf.readBoolean();
        return new SpawnDisplayPacket(pos, slot, item, rotX, rotY, rotZ, scale, billboard);
    }

    public static void handle(SpawnDisplayPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;
            ServerLevel level = sender.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (be instanceof FoodDryingRackBlockEntity tile) {
                Entity e = CustomDisplayUtils.spawnItemDisplay(
                        level,
                        msg.pos.getX() + 0.5 + (msg.slot * 0.2),
                        msg.pos.getY() + 1.5,
                        msg.pos.getZ() + 0.5,
                        msg.item, msg.rotX, msg.rotY, msg.rotZ,
                        msg.scale, msg.billboard
                );
                if (e != null) {
                    tile.displayedItems[msg.slot] = e;
                    tile.setChanged();
                    tile.syncToClient();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
