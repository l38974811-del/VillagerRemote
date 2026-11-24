package com.example.villagerremote.client;

import com.example.villagerremote.network.packet.OpenRemoteScreenS2C;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientScreenOpener {
    public static void open(List<OpenRemoteScreenS2C.VillagerInfo> villagers) {
        Minecraft.getInstance().setScreen(new VillagerRemoteScreen(villagers));
    }

    private ClientScreenOpener() {
    }
}
