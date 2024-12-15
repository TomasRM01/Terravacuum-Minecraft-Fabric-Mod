package net.aurise.terravacuummod.item.custom;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TerravacuumItem extends Item{
    public TerravacuumItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {

        if(!world.isClient()){
            
            // Break blocks in a cone in front of the player
            int radius = 5;
            int height = 2;
            
            BlockPos playerPos = user.getBlockPos();
            BlockPos targetPos;
            for (int y = 0; y <= height; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        targetPos = playerPos.add(x, y, z);
                        if (user.getRotationVec(1.0F).dotProduct(targetPos.subtract(playerPos).toCenterPos().normalize()) > 0.5) {
                            // If the block is not breakable, dont break it
                            float blockHardness = world.getBlockState(targetPos).getBlock().getHardness();
                            if (blockHardness >= 0 && blockHardness <= 1.5) {
                                world.breakBlock(targetPos, true, user);
                            }
                        }
                    }
                }
            }

            // Durability loss
            user.getStackInHand(hand).damage(1, ((ServerWorld) world), ((ServerPlayerEntity) user), item -> user.sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));
            
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS);

        };

        return ActionResult.SUCCESS;
    }

    // Attach shulker box to the item and detach it like a bundle
    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {

        // Attach shulker box to the item
        if (clickType == ClickType.LEFT && isShulkerBox(otherStack.getItem()) && stack.getComponents().get(DataComponentTypes.CONTAINER) == null) {
            stack.set(DataComponentTypes.CONTAINER, otherStack.getComponents().get(DataComponentTypes.CONTAINER));
            otherStack.setCount(0);
            return true;
        }

        // Detach shulker box from the item
        if (clickType == ClickType.RIGHT && otherStack.getItem() == Items.AIR && stack.getComponents().get(DataComponentTypes.CONTAINER) != null) {
            ItemStack shulker = Items.SHULKER_BOX.getDefaultStack();
            shulker.set(DataComponentTypes.CONTAINER, stack.getComponents().get(DataComponentTypes.CONTAINER));
            stack.remove(DataComponentTypes.CONTAINER);
            cursorStackReference.set(shulker);
            return true;
        }

        return false;
	}

    // Check if the item is a shulker box (any color)
    private boolean isShulkerBox(Item item) {
        List<Item> shulkerBoxes = List.of(Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX);
        if (shulkerBoxes.contains(item)) {
            return true;
        }
        return false;
    }

}
