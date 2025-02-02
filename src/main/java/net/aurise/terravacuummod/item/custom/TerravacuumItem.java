package net.aurise.terravacuummod.item.custom;

import java.util.List;
import java.util.function.Consumer;

import net.aurise.terravacuummod.component.ModDataComponentTypes;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class TerravacuumItem extends Item {

    final int shulkerBoxInventorySize = ShulkerBoxBlockEntity.INVENTORY_SIZE;
    
    public TerravacuumItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, RegistryEntry<Enchantment> enchantment, EnchantingContext context) {

        // Only unbreaking enchantment is allowed
        if (enchantment.matchesKey(net.minecraft.enchantment.Enchantments.UNBREAKING)) {
            return true;
        }

        return false;
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        if (stack.get(ModDataComponentTypes.ATTACHED_SHULKER) != null)
        {
            return stack.get(ModDataComponentTypes.ATTACHED_SHULKER);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {

        if(!world.isClient()){

            // if the item durability is 1, do nothing
            if (user.getStackInHand(hand).getDamage() == user.getStackInHand(hand).getMaxDamage() - 1) {
                return ActionResult.FAIL;
            }
            
            // Break blocks in a cone in front of the player
            int radius = 5;
            int height = 2;
            
            BlockPos playerPos = user.getBlockPos();
            BlockPos targetPos;

            ItemStack attachedShulker = user.getStackInHand(hand).get(ModDataComponentTypes.ATTACHED_SHULKER);

            for (int y = 0; y <= height; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        targetPos = playerPos.add(x, y, z);
                        if (user.getRotationVec(1.0F).dotProduct(targetPos.subtract(playerPos).toCenterPos().normalize()) > 0.5) {
                            // If the block is not breakable, dont break it
                            Block block = world.getBlockState(targetPos).getBlock();
                            float blockHardness = block.getHardness();
                            if (blockHardness >= 0 && blockHardness <= 1.5 && !world.getBlockState(targetPos).isAir()) {
                                handleBlockDestruction(world, user, targetPos, block, attachedShulker, user.getStackInHand(hand));
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

    // Break the block and add it to the shulker box if it is not full, otherwise drop it
    private void handleBlockDestruction(World world, PlayerEntity user, BlockPos targetPos, Block block, ItemStack shulker, ItemStack stack) {

        boolean added = false;

        if (shulker != null) {

            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(shulkerBoxInventorySize, ItemStack.EMPTY);
            shulker.get(DataComponentTypes.CONTAINER).copyTo(stacks);
            
            for (int i = 0; i < stacks.size(); i++) {
                if (stacks.get(i).isEmpty()) {
                    stacks.set(i, new ItemStack(block));
                    added = true;
                    break;
                }
                else if (stacks.get(i).isOf(block.asItem())) {
                    int count = stacks.get(i).getCount();
                    if (count < 64) {
                        stacks.get(i).setCount(count + 1);
                        added = true;
                        break;
                    }
                }
            }

            shulker.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
            stack.set(ModDataComponentTypes.ATTACHED_SHULKER, shulker);

        }

        world.breakBlock(targetPos, !added, user);
    }

    // Custom tooltip that explains how to attach and detach shulker boxes
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        if (stack.get(ModDataComponentTypes.ATTACHED_SHULKER) != null) {
            textConsumer.accept(Text.translatable("itemTooltip.terravacuum-mod.terravacuum_deattach_info").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        else {
            textConsumer.accept(Text.translatable("itemTooltip.terravacuum-mod.terravacuum_attach_info").formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }

    // Called when clicked the item
    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        return handleShulkerInteraction(stack, otherStack, null, clickType, player, cursorStackReference);
    }

    // Called when clicked WITH the item on the cursor
    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        return handleShulkerInteraction(stack, slot.getStack(), slot, clickType, player, null);
    }

    // Attach and detach shulker boxes to the item like a bundle
    private boolean handleShulkerInteraction(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        ItemStack shulker = stack.get(ModDataComponentTypes.ATTACHED_SHULKER);

        // Attach shulker box to the item
        if (clickType == ClickType.LEFT && isShulkerBox(otherStack.getItem())) {
            if (shulker == null) {
                addShulkerToTerravacuum(player, stack, slot, cursorStackReference, otherStack);
                return true;
            }
            player.playSound(SoundEvents.ITEM_BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
        }
        // Detach shulker box from the item
        else if (clickType == ClickType.RIGHT && otherStack.isEmpty() && shulker != null) {
            removeShulkerFromTerravacuum(player, stack, slot, cursorStackReference, shulker);
            return true;
        }

        return false;
    }

    private void addShulkerToTerravacuum(PlayerEntity player, ItemStack stack, Slot slot, StackReference cursorStackReference, ItemStack shulker) {
        player.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);

        stack.set(ModDataComponentTypes.SHULKER_COLOR, shulker.getItem().toString());

        stack.set(ModDataComponentTypes.ATTACHED_SHULKER, shulker);
        if (cursorStackReference != null) cursorStackReference.set(ItemStack.EMPTY);
        if (slot != null) slot.setStack(ItemStack.EMPTY);

        this.onContentChanged(player);
    }

    private void removeShulkerFromTerravacuum(PlayerEntity player, ItemStack stack, Slot slot, StackReference cursorStackReference, ItemStack shulker) {
        player.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getWorld().getRandom().nextFloat() * 0.4F);

        stack.remove(ModDataComponentTypes.SHULKER_COLOR);

        stack.remove(ModDataComponentTypes.ATTACHED_SHULKER);
        if (cursorStackReference != null) cursorStackReference.set(shulker);
        if (slot != null) slot.insertStack(shulker);

        this.onContentChanged(player);
    }

    // Check if the item is a shulker box (any color)
    private boolean isShulkerBox(Item item) {
        List<Item> shulkerBoxes = List.of(Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX);
        if (shulkerBoxes.contains(item)) {
            return true;
        }
        return false;
    }

    // Update the player inventory
    private void onContentChanged(PlayerEntity user) {
        ScreenHandler screenHandler = user.currentScreenHandler;
        if (screenHandler != null) {
            screenHandler.onContentChanged(user.getInventory());
        }
    }

}
