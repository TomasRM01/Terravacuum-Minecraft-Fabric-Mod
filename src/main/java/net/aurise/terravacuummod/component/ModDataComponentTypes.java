package net.aurise.terravacuummod.component;

import net.aurise.terravacuummod.TerravacuumMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.function.UnaryOperator;

import com.mojang.serialization.Codec;

public class ModDataComponentTypes {

    public static final DataComponentType<ItemStack> ATTACHED_SHULKER = register("attached_shulker", builder -> builder.persistent(ItemStack.CODEC));

    // This custom component will not be used until 1.21.5
    public static final DataComponentType<String> SHULKER_COLOR = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath(TerravacuumMod.MOD_ID, "shulker_color"),
		DataComponentType.<String>builder().persistent(Codec.STRING).build()
    );

    private static <T>DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator){
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(TerravacuumMod.MOD_ID, name), builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void registerDataComponentTypes(){
        TerravacuumMod.LOGGER.info("Registering Mod Data Component Types for " + TerravacuumMod.MOD_ID);
    }

}
