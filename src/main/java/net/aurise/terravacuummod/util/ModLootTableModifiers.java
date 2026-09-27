package net.aurise.terravacuummod.util;


import net.aurise.terravacuummod.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public class ModLootTableModifiers {

    private static final Identifier OMINOUS_UNIQUE_REWARD_CHEST_ID = Identifier.fromNamespaceAndPath("minecraft", "chests/trial_chambers/reward_ominous");

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (OMINOUS_UNIQUE_REWARD_CHEST_ID.equals(key.identifier())) {
                LootPool.Builder poolBuilder = LootPool.lootPool()
                        .setRolls(ContextIntProviders.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.5f))
                        .add(LootItem.lootTableItem(ModItems.TERRAVACUUM));

                tableBuilder.withPool(poolBuilder);
            }
        });
    }
}