package com.example.villagerremote.network.packet;

import com.example.villagerremote.item.ModItems;
import com.example.villagerremote.orders.VillagerOrders;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class VillagerCommandC2S {
    public static final int FOLLOW = 0;
    public static final int STOP = 1;

    private final int entityId;
    private final int command;

    public VillagerCommandC2S(int entityId, int command) {
        this.entityId = entityId;
        this.command = command;
    }

    public VillagerCommandC2S(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.command = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(command);
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
                if (command == FOLLOW) {
                    VillagerOrders.follow(sender, villager);
                    sender.displayClientMessage(Component.literal("§aالقروي بدأ باتباعك."), true);
                } else if (command == STOP) {
                    VillagerOrders.stop(villager);
                    sender.displayClientMessage(Component.literal("§eتم إيقاف أوامر هذا القروي."), true);
                }
            } else {
                sender.displayClientMessage(Component.literal("§cتعذر تنفيذ الأمر: القروي بعيد أو الريموت ليس في يدك."), true);
            }
        }
        context.setPacketHandled(true);
    }
}
