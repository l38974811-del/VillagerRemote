package com.example.villagerremote.network.packet;

import com.example.villagerremote.client.ClientScreenOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class OpenRemoteScreenS2C {
    private final List<VillagerInfo> villagers;

    public OpenRemoteScreenS2C(List<VillagerInfo> villagers) {
        this.villagers = List.copyOf(villagers);
    }

    public OpenRemoteScreenS2C(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 64);
        List<VillagerInfo> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new VillagerInfo(
                    buffer.readVarInt(),
                    buffer.readUtf(128),
                    buffer.readFloat(),
                    buffer.readFloat()
            ));
        }
        this.villagers = List.copyOf(entries);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(villagers.size());
        for (VillagerInfo villager : villagers) {
            buffer.writeVarInt(villager.entityId());
            buffer.writeUtf(villager.name(), 128);
            buffer.writeFloat(villager.health());
            buffer.writeFloat(villager.maxHealth());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> ClientScreenOpener.open(villagers)
        ));
        context.setPacketHandled(true);
    }

    public record VillagerInfo(int entityId, String name, float health, float maxHealth) {
    }
}
