package com.example.villagerremote.network.packet;

import com.example.villagerremote.item.ModItems;
import com.example.villagerremote.item.VillagerRemoteItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ArmMiningC2S {
    private final int entityId;

    public ArmMiningC2S(int entityId) {
        this.entityId = entityId;
    }

    public ArmMiningC2S(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            Entity entity = sender.serverLevel().getEntity(entityId);
            boolean holdingRemote = sender.getMainHandItem().is(ModItems.VILLAGER_REMOTE.get())
                    || sender.getOffhandItem().is(ModItems.VILLAGER_REMOTE.get());

            if (holdingRemote && entity instanceof Villager villager && villager.isAlive()
                    && villager.distanceToSqr(sender) <= 64.0D * 64.0D) {
                sender.getPersistentData().putInt(VillagerRemoteItem.PENDING_MINE_KEY, entityId);
                sender.displayClientMessage(
                        Component.literal("§eأغلق القائمة، ثم وجّه الريموت إلى البلوك المطلوب واضغط زر الفأرة الأيمن."),
                        true
                );
            } else {
                sender.displayClientMessage(Component.literal("§cتعذر تحديد القروي للتكسير."), true);
            }
        }
        context.setPacketHandled(true);
    }
}
