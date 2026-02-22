package net.aurise.terravacuummod.item;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.aurise.terravacuummod.TerravacuumMod;
import net.aurise.terravacuummod.item.custom.TerravacuumItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;


public class ModItems {

    public static final Item TERRAVACUUM = registerItem("terravacuum", new TerravacuumItem(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(TerravacuumMod.MOD_ID,"terravacuum"))).maxDamage(51).repairable(Items.BREEZE_ROD).rarity(Rarity.RARE).useCooldown(1.0F)));
    
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(TerravacuumMod.MOD_ID, name), item);
    }


    public static void registerModItems() {
        TerravacuumMod.LOGGER.info("Registering Mod Items for " + TerravacuumMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(TERRAVACUUM);
        });
    }

}
