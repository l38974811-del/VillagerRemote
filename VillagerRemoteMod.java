package com.example.villagerremote;

import com.example.villagerremote.item.ModItems;
import com.example.villagerremote.network.NetworkHandler;
import com.example.villagerremote.orders.VillagerRemoteEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VillagerRemoteMod.MOD_ID)
public final class VillagerRemoteMod {
    public static final String MOD_ID = "villagerremote";

    public VillagerRemoteMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(new VillagerRemoteEvents());
    }
}
