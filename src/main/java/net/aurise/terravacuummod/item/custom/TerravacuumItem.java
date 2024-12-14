package net.aurise.terravacuummod.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
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
        if (clickType == ClickType.LEFT && otherStack.getItem() == net.minecraft.item.Items.SHULKER_BOX && stack.getComponents().get(DataComponentTypes.CONTAINER) == null) {
            stack.set(DataComponentTypes.CONTAINER, otherStack.getComponents().get(DataComponentTypes.CONTAINER));
            otherStack.setCount(0);
            return true;
        }

        // Detach shulker box from the item
        if (clickType == ClickType.RIGHT && otherStack.getItem() == net.minecraft.item.Items.AIR && stack.getComponents().get(DataComponentTypes.CONTAINER) != null) {
            ItemStack shulker = net.minecraft.item.Items.SHULKER_BOX.getDefaultStack();
            shulker.set(DataComponentTypes.CONTAINER, stack.getComponents().get(DataComponentTypes.CONTAINER));
            stack.remove(DataComponentTypes.CONTAINER);
            cursorStackReference.set(shulker);
            return true;
        }

        return false;
	}

}
