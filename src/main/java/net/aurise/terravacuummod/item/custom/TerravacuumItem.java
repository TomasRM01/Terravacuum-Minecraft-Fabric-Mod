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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;


public class TerravacuumItem extends Item {

    public static final int breakAreaRadius = 5;
    public static final int breakAreaHeight = 3;

    static final int shulkerBoxInventorySize = ShulkerBoxBlockEntity.INVENTORY_SIZE;
    
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

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {

        if(world.isClient()) {
            return ActionResult.SUCCESS;
        }

        // The item stops working when it is about to break to prevent shulkerbox loss
        if (user.getStackInHand(hand).getDamage() == user.getStackInHand(hand).getMaxDamage() - 1) {
            return ActionResult.FAIL;
        }
        
        calculateBreakAreaAndDestroyBlocks(world, user, hand);

        // Durability loss
        user.getStackInHand(hand).damage(1, ((ServerWorld) world), ((ServerPlayerEntity) user), item -> user.sendEquipmentBreakStatus(item, EquipmentSlot.MAINHAND));
        
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BREEZE_IDLE_AIR, SoundCategory.PLAYERS);

        return ActionResult.SUCCESS;

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





    private void calculateBreakAreaAndDestroyBlocks(World world, PlayerEntity user, Hand hand) {

        // Get player position and look vector
        BlockPos playerPos = user.getBlockPos();
        Vec3d lookVec = user.getRotationVec(1.0F);

        // Calculate the center of the break area
        BlockPos targetCenter = playerPos.add(
            (int) ((lookVec.x) * (breakAreaRadius + 1)),
            0,
            (int) ((lookVec.z) * (breakAreaRadius + 1))
        );

        // Break blocks in the break area on a cylinder shape if they are within the break radius
        for (int x = -breakAreaRadius; x <= breakAreaRadius; x++) {
            for (int z = -breakAreaRadius; z <= breakAreaRadius; z++) {
                if (Math.sqrt(x * x + z * z) <= breakAreaRadius) { // Keeps the shape of the cylinder
                    for (int y = 0; y < breakAreaHeight; y++) { // Height of the cylinder

                        // Calculate the position of the target block
                        BlockPos targetPos = targetCenter.add(x, y, z);

                        // Adjust the player position on the y axis to keep the cylinder shape
                        BlockPos adjustedPlayerPos = playerPos.add(0, y, 0);
                        
                        // Check if the target block is within the break radius
                        double distance = Math.sqrt(targetPos.getSquaredDistance(adjustedPlayerPos));
                        if (distance <= breakAreaRadius) {
                            Block targetBlock = world.getBlockState(targetPos).getBlock();
                            float blockHardness = targetBlock.getHardness();
                            if (blockHardness >= 0 && blockHardness <= 1.5 && !world.getBlockState(targetPos).isAir()) {
                                handleBlockDestruction(world, user, hand, targetPos, targetBlock);
                            }
                        }
                    }
                }
            }
        }
    }

    // Break the targetBlock and add it to the shulkerbox if it is not full, otherwise drop it
    private void handleBlockDestruction(World world, PlayerEntity user, Hand hand, BlockPos targetPos, Block targetBlock) {

        ItemStack terravacuumStack = user.getStackInHand(hand);
        ItemStack attachedShulker = terravacuumStack.get(ModDataComponentTypes.ATTACHED_SHULKER);

        boolean shouldDrop = true;

        if (attachedShulker == null) {
            world.breakBlock(targetPos, shouldDrop, user);
            return;
        }
        
        DefaultedList<ItemStack> shulkerContent = DefaultedList.ofSize(shulkerBoxInventorySize, ItemStack.EMPTY);
        attachedShulker.get(DataComponentTypes.CONTAINER).copyTo(shulkerContent);
        
        for (int i = 0; i < shulkerContent.size(); i++) {
            if (shulkerContent.get(i).isEmpty()) {
                shulkerContent.set(i, new ItemStack(targetBlock));
                shouldDrop = false;
                break;
            }
            else if (shulkerContent.get(i).isOf(targetBlock.asItem())) {
                int count = shulkerContent.get(i).getCount();
                if (count < 64) {
                    shulkerContent.get(i).setCount(count + 1);
                    shouldDrop = false;
                    break;
                }
            }
        }

        attachedShulker.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(shulkerContent));
        terravacuumStack.set(ModDataComponentTypes.ATTACHED_SHULKER, attachedShulker);
        
        world.breakBlock(targetPos, shouldDrop, user);
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
