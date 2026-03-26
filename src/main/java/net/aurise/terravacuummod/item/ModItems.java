package net.aurise.terravacuummod.item;

import net.aurise.terravacuummod.TerravacuumMod;
import net.aurise.terravacuummod.item.custom.TerravacuumItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;


public class ModItems {

    public static final Item TERRAVACUUM = registerItem("terravacuum", new TerravacuumItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TerravacuumMod.MOD_ID,"terravacuum"))).durability(51).repairable(Items.BREEZE_ROD).rarity(Rarity.RARE).useCooldown(1.0F)));
    
    private static Item registerItem(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(TerravacuumMod.MOD_ID, name), item);
    }


    public static void registerModItems() {
        TerravacuumMod.LOGGER.info("Registering Mod Items for " + TerravacuumMod.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(TERRAVACUUM);
        });
    }

}
