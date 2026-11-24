package com.example.villagerremote.network;

import com.example.villagerremote.VillagerRemoteMod;
import com.example.villagerremote.network.packet.ArmMiningC2S;
import com.example.villagerremote.network.packet.OpenRemoteScreenS2C;
import com.example.villagerremote.network.packet.VillagerCommandC2S;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VillagerRemoteMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.messageBuilder(OpenRemoteScreenS2C.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenRemoteScreenS2C::encode)
                .decoder(OpenRemoteScreenS2C::new)
                .consumerMainThread(OpenRemoteScreenS2C::handle)
                .add();

        CHANNEL.messageBuilder(VillagerCommandC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(VillagerCommandC2S::encode)
                .decoder(VillagerCommandC2S::new)
                .consumerMainThread(VillagerCommandC2S::handle)
                .add();

        CHANNEL.messageBuilder(ArmMiningC2S.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ArmMiningC2S::encode)
                .decoder(ArmMiningC2S::new)
                .consumerMainThread(ArmMiningC2S::handle)
                .add();
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    private NetworkHandler() {
    }
}
