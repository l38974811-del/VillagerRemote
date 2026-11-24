package com.example.villagerremote.item;

import com.example.villagerremote.VillagerRemoteMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VillagerRemoteMod.MOD_ID);

    public static final RegistryObject<Item> VILLAGER_REMOTE = ITEMS.register(
            "villager_remote",
            () -> new VillagerRemoteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
    );

    private ModItems() {
    }
}
