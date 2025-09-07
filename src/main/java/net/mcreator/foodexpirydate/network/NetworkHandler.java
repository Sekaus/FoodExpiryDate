package net.mcreator.foodexpirydate.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    private static final String MODID = "foodexpirydate"; // replace if your modid differs
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "main_channel"))
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();

    private static int id = 0;

    public static void register() {
        // Register packets here. Keep id++ order when adding more packets.
        CHANNEL.registerMessage(id++, SpawnDisplayPacket.class,
                SpawnDisplayPacket::encode, SpawnDisplayPacket::decode,
                SpawnDisplayPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(Object pkt) {
        CHANNEL.sendToServer(pkt);
    }
}
