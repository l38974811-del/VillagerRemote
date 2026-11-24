package com.example.villagerremote.orders;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side orders assigned to individual villagers. */
public final class VillagerOrders {
    private static final Map<UUID, Order> ACTIVE_ORDERS = new HashMap<>();
    private static final double MAX_ORDER_DISTANCE = 64.0D;

    public static void follow(ServerPlayer owner, Villager villager) {
        Order previous = ACTIVE_ORDERS.put(villager.getUUID(), Order.follow(owner.getUUID(), owner.serverLevel().dimension()));
        clearCrackProgress(villager, previous);
        villager.getNavigation().stop();
    }

    public static void stop(Villager villager) {
        Order previous = ACTIVE_ORDERS.remove(villager.getUUID());
        clearCrackProgress(villager, previous);
        villager.getNavigation().stop();
    }

    public static boolean startMining(ServerPlayer owner, Villager villager, BlockPos target) {
        if (!(villager.level() instanceof ServerLevel level) || !level.hasChunkAt(target)) {
            return false;
        }

        if (owner.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > MAX_ORDER_DISTANCE * MAX_ORDER_DISTANCE) {
            return false;
        }
        if (villager.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D)
                > MAX_ORDER_DISTANCE * MAX_ORDER_DISTANCE) {
            return false;
        }

        if (!owner.mayBuild() || !level.mayInteract(owner, target)) {
            return false;
        }

        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        BlockEntity blockEntity = level.getBlockEntity(target);

        // Containers, machines and other block-entity blocks are deliberately protected.
        if (state.isAir() || hardness < 0.0F || blockEntity != null || !state.getFluidState().isEmpty()) {
            return false;
        }

        Order previous = ACTIVE_ORDERS.put(villager.getUUID(), Order.mine(
                owner.getUUID(), level.dimension(), target.immutable(), miningDuration(hardness)
        ));
        clearCrackProgress(villager, previous);
        villager.getNavigation().stop();
        return true;
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_ORDERS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Order>> iterator = ACTIVE_ORDERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Order> entry = iterator.next();
            Order order = entry.getValue();
            if (!order.dimension.equals(level.dimension())) {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Villager villager) || !villager.isAlive()) {
                iterator.remove();
                continue;
            }

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(order.owner);
            if (owner == null || owner.level() != level) {
                clearCrackProgress(villager, order);
                villager.getNavigation().stop();
                iterator.remove();
                continue;
            }

            if (order.mineTarget == null) {
                tickFollow(level, villager, owner);
            } else if (!tickMine(level, villager, owner, order)) {
                clearCrackProgress(villager, order);
                iterator.remove();
            }
        }
    }

    private static void tickFollow(ServerLevel level, Villager villager, ServerPlayer owner) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        if (villager.distanceToSqr(owner) > 3.0D * 3.0D) {
            villager.getNavigation().moveTo(owner, 1.12D);
        } else {
            villager.getNavigation().stop();
        }
    }

    /** Returns false when the order has completed or must be cancelled. */
    private static boolean tickMine(ServerLevel level, Villager villager, ServerPlayer owner, Order order) {
        BlockPos target = order.mineTarget;
        if (!level.hasChunkAt(target)) {
            owner.displayClientMessage(Component.literal("§cتوقف التكسير لأن منطقة البلوك لم تعد محمّلة."), true);
            return false;
        }

        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        if (state.isAir() || hardness < 0.0F || level.getBlockEntity(target) != null) {
            owner.displayClientMessage(Component.literal("§eأُلغي أمر التكسير: البلوك لم يعد قابلًا للتكسير."), true);
            return false;
        }

        Vec3 approach = findApproachPoint(level, villager, target);
        double distance = villager.distanceToSqr(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        if (distance > 6.25D) {
            if (level.getGameTime() % 10L == 0L) {
                villager.getNavigation().moveTo(approach.x, approach.y, approach.z, 1.0D);
            }
            if (order.progressTicks > 0) {
                level.destroyBlockProgress(villager.getId(), target, -1);
            }
            order.progressTicks = 0;
            return true;
        }

        villager.getNavigation().stop();
        order.progressTicks++;
        int crackStage = Math.min(9, (int) (order.progressTicks * 10.0D / order.requiredTicks));
        level.destroyBlockProgress(villager.getId(), target, crackStage);
        if (order.progressTicks < order.requiredTicks) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(target);
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, target, state, owner);
        if (MinecraftForge.EVENT_BUS.post(breakEvent)) {
            owner.displayClientMessage(Component.literal("§cرفضت حماية العالم أو أحد المودات تكسير هذا البلوك."), true);
            return false;
        }

        ItemStack virtualIronPickaxe = new ItemStack(Items.IRON_PICKAXE);
        List<ItemStack> drops = Block.getDrops(state, level, target, blockEntity, owner, virtualIronPickaxe);
        boolean removed = level.destroyBlock(target, false, villager);
        if (removed) {
            for (ItemStack drop : drops) {
                if (!drop.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(
                            level,
                            target.getX() + 0.5D,
                            target.getY() + 0.4D,
                            target.getZ() + 0.5D,
                            drop.copy()
                    );
                    level.addFreshEntity(itemEntity);
                }
            }
            owner.displayClientMessage(Component.literal("§aأنهى القروي تكسير البلوك المحدد."), true);
        }
        return false;
    }

    private static Vec3 findApproachPoint(ServerLevel level, Villager villager, BlockPos target) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(target.north());
        candidates.add(target.south());
        candidates.add(target.east());
        candidates.add(target.west());
        candidates.add(target.above());

        BlockPos best = target;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : candidates) {
            if (!level.hasChunkAt(candidate)) {
                continue;
            }
            if (!level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                continue;
            }
            double distance = villager.distanceToSqr(
                    candidate.getX() + 0.5D,
                    candidate.getY(),
                    candidate.getZ() + 0.5D
            );
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return new Vec3(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
    }

    private static void clearCrackProgress(Villager villager, Order order) {
        if (order != null && order.mineTarget != null && villager.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(villager.getId(), order.mineTarget, -1);
        }
    }

    private static int miningDuration(float hardness) {
        return Math.max(35, Math.min(400, 35 + (int) Math.ceil(hardness * 22.0F)));
    }

    private static final class Order {
        private final UUID owner;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final BlockPos mineTarget;
        private final int requiredTicks;
        private int progressTicks;

        private Order(UUID owner,
                      net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                      BlockPos mineTarget,
                      int requiredTicks) {
            this.owner = owner;
            this.dimension = dimension;
            this.mineTarget = mineTarget;
            this.requiredTicks = requiredTicks;
        }

        private static Order follow(UUID owner,
                                    net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
            return new Order(owner, dimension, null, 0);
        }

        private static Order mine(UUID owner,
                                  net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                  BlockPos target,
                                  int requiredTicks) {
            return new Order(owner, dimension, target, requiredTicks);
        }
    }

    private VillagerOrders() {
    }
}
