package com.example.villagerremote.item;

import com.example.villagerremote.network.packet.OpenRemoteScreenS2C;
import com.example.villagerremote.orders.VillagerOrders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class VillagerRemoteItem extends Item {
    public static final int SCAN_RADIUS = 32;
    public static final int REQUIRED_VILLAGERS = 6;
    public static final String PENDING_MINE_KEY = "VillagerRemotePendingMine";

    public VillagerRemoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("افتح القائمة بزر الفأرة الأيمن في الهواء.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("يحتاج إلى 6 قرويين على الأقل ضمن 32 بلوكًا.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("أمر التكسير: اختر القروي ثم انقر على البلوك بالريموت.").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            net.minecraft.world.entity.player.Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            AABB scanBox = player.getBoundingBox().inflate(SCAN_RADIUS);
            List<Villager> villagers = level.getEntitiesOfClass(
                    Villager.class,
                    scanBox,
                    villager -> villager.isAlive()
                            && villager.distanceToSqr(player) <= (double) SCAN_RADIUS * SCAN_RADIUS
            );

            if (villagers.size() < REQUIRED_VILLAGERS) {
                serverPlayer.displayClientMessage(
                        Component.literal("§cيحتاج الريموت إلى 6 قرويين على الأقل ضمن 32 بلوكًا."),
                        true
                );
            } else {
                villagers.sort(Comparator.comparingDouble((Villager villager) -> villager.distanceToSqr(player)));
                List<OpenRemoteScreenS2C.VillagerInfo> entries = villagers.stream()
                        .limit(64)
                        .map(villager -> new OpenRemoteScreenS2C.VillagerInfo(
                                villager.getId(),
                                villager.getDisplayName().getString(),
                                villager.getHealth(),
                                villager.getMaxHealth()
                        ))
                        .toList();

                NetworkHandler.sendToPlayer(new OpenRemoteScreenS2C(entries), serverPlayer);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        if (!player.getPersistentData().contains(PENDING_MINE_KEY)) {
            return InteractionResult.PASS;
        }

        int villagerId = player.getPersistentData().getInt(PENDING_MINE_KEY);
        player.getPersistentData().remove(PENDING_MINE_KEY);
        Entity entity = player.serverLevel().getEntity(villagerId);

        if (!(entity instanceof Villager villager) || !villager.isAlive()) {
            player.displayClientMessage(Component.literal("§cلم أعد أجد القروي المحدد. افتح الريموت واختره من جديد."), true);
            return InteractionResult.CONSUME;
        }

        BlockPos target = context.getClickedPos();
        if (VillagerOrders.startMining(player, villager, target)) {
            player.displayClientMessage(Component.literal("§aتم إرسال أمر التكسير للقروي. سيحاول الوصول إلى البلوك المحدد."), true);
        } else {
            player.displayClientMessage(Component.literal("§cلا يمكن لهذا القروي تكسير هذا البلوك أو أنه بعيد جدًا."), true);
        }

        return InteractionResult.CONSUME;
    }
}
