package net.aurise.terravacuummod.component;

import net.aurise.terravacuummod.TerravacuumMod;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

public class ModDataComponentTypes {

    public static final ComponentType<ItemStack> ATTACHED_SHULKER = register("attached_shulker", builder -> builder.codec(ItemStack.CODEC));

    // This custom component will not be used until 1.21.5
    public static final ComponentType<String> SHULKER_COLOR = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(TerravacuumMod.MOD_ID, "shulker_color"),
		ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    private static <T>ComponentType<T> register(String name, UnaryOperator<ComponentType.Builder<T>> builderOperator){
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(TerravacuumMod.MOD_ID, name), builderOperator.apply(ComponentType.builder()).build());
    }

    public static void registerDataComponentTypes(){
        TerravacuumMod.LOGGER.info("Registering Mod Data Component Types for " + TerravacuumMod.MOD_ID);
    }

}
