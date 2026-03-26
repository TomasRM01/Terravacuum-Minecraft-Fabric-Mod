package net.aurise.terravacuummod.item.custom;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.aurise.terravacuummod.component.ModDataComponentTypes;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;


public class TerravacuumItem extends Item {

    public static final int breakAreaRadius = 5;
    public static final int breakAreaHeight = 3;

    static final int shulkerBoxInventorySize = ShulkerBoxBlockEntity.CONTAINER_SIZE;
    
    public TerravacuumItem(Properties settings) {
        super(settings);
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean canBeEnchantedWith(ItemStack stack, Holder<Enchantment> enchantment, EnchantingContext context) {

        // Only unbreaking enchantment is allowed
        return enchantment.is(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
    }

    @Override
    public ItemStackTemplate getCraftingRemainder(ItemStack stack) {
        ItemStack remainder = stack.get(ModDataComponentTypes.ATTACHED_SHULKER);
        if (remainder == null) {
            return null;
        }
        return ItemStackTemplate.fromNonEmptyStack(remainder);
    }

    // Custom tooltip that explains how to attach and detach shulker boxes
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        if (stack.get(ModDataComponentTypes.ATTACHED_SHULKER) != null) {
            textConsumer.accept(Component.translatable("itemTooltip.terravacuum-mod.terravacuum_deattach_info").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        else {
            textConsumer.accept(Component.translatable("itemTooltip.terravacuum-mod.terravacuum_attach_info").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {

        if(world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // The item stops working when it is about to break to prevent shulkerbox loss
        if (user.getItemInHand(hand).getDamageValue() == user.getItemInHand(hand).getMaxDamage() - 1) {
            return InteractionResult.FAIL;
        }
        
        calculateBreakAreaAndDestroyBlocks(world, user, hand);

        // Durability loss
        user.getItemInHand(hand).hurtAndBreak(1, ((ServerLevel) world), ((ServerPlayer) user), item -> user.onEquippedItemBroken(item, EquipmentSlot.MAINHAND));
        
        world.playSound(null, user.blockPosition(), SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS);
        world.playSound(null, user.blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS);

        return InteractionResult.SUCCESS;

    }

    // Called when clicked the item
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        return handleShulkerInteraction(stack, otherStack, null, clickType, player, cursorStackReference);
    }

    // Called when clicked WITH the item on the cursor
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, Player player) {
        return handleShulkerInteraction(stack, slot.getItem(), slot, clickType, player, null);
    }

    private void calculateBreakAreaAndDestroyBlocks(Level world, Player user, InteractionHand hand) {

        // Get player position and look vector
        BlockPos playerPos = user.blockPosition();
        Vec3 lookVec = user.getViewVector(1.0F);

        // Calculate the center of the break area
        BlockPos targetCenter = playerPos.offset(
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
                        BlockPos targetPos = targetCenter.offset(x, y, z);

                        // Adjust the player position on the y-axis to keep the cylinder shape
                        BlockPos adjustedPlayerPos = playerPos.offset(0, y, 0);
                        
                        // Check if the target block is within the break radius
                        double distance = Math.sqrt(targetPos.distSqr(adjustedPlayerPos));
                        if (distance <= breakAreaRadius) {
                            Block targetBlock = world.getBlockState(targetPos).getBlock();
                            float blockHardness = targetBlock.defaultDestroyTime();
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
    private void handleBlockDestruction(Level world, Player user, InteractionHand hand, BlockPos targetPos, Block targetBlock) {

        ItemStack terravacuumStack = user.getItemInHand(hand);
        ItemStack attachedShulker = terravacuumStack.get(ModDataComponentTypes.ATTACHED_SHULKER);

        boolean shouldDrop = true;

        if (attachedShulker == null) {
            world.destroyBlock(targetPos, true, user);
            return;
        }
        
        NonNullList<ItemStack> shulkerContent = NonNullList.withSize(shulkerBoxInventorySize, ItemStack.EMPTY);
        Objects.requireNonNull(attachedShulker.get(DataComponents.CONTAINER)).copyInto(shulkerContent);
        
        for (int i = 0; i < shulkerContent.size(); i++) {
            if (shulkerContent.get(i).isEmpty()) {
                shulkerContent.set(i, new ItemStack(targetBlock));
                shouldDrop = false;
                break;
            }
            else if (shulkerContent.get(i).is(targetBlock.asItem())) {
                int count = shulkerContent.get(i).getCount();
                if (count < 64) {
                    shulkerContent.get(i).setCount(count + 1);
                    shouldDrop = false;
                    break;
                }
            }
        }

        attachedShulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(shulkerContent));
        terravacuumStack.set(ModDataComponentTypes.ATTACHED_SHULKER, attachedShulker);
        
        world.destroyBlock(targetPos, shouldDrop, user);
    }

    // Attach and detach shulker boxes to the item like a bundle
    private boolean handleShulkerInteraction(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        ItemStack shulker = stack.get(ModDataComponentTypes.ATTACHED_SHULKER);

        // Attach shulker box to the item
        if (clickType == ClickAction.PRIMARY && isShulkerBox(otherStack.getItem())) {
            if (shulker == null) {
                addShulkerToTerravacuum(player, stack, slot, cursorStackReference, otherStack);
                return true;
            }
            player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
        }
        // Detach shulker box from the item
        else if (clickType == ClickAction.SECONDARY && otherStack.isEmpty() && shulker != null) {
            removeShulkerFromTerravacuum(player, stack, slot, cursorStackReference, shulker);
            return true;
        }

        return false;
    }

    private void addShulkerToTerravacuum(Player player, ItemStack stack, Slot slot, SlotAccess cursorStackReference, ItemStack shulker) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);

        stack.set(ModDataComponentTypes.SHULKER_COLOR, shulker.getItem().toString());

        stack.set(ModDataComponentTypes.ATTACHED_SHULKER, shulker);
        if (cursorStackReference != null) cursorStackReference.set(ItemStack.EMPTY);
        if (slot != null) slot.setByPlayer(ItemStack.EMPTY);

        this.onContentChanged(player);
    }

    private void removeShulkerFromTerravacuum(Player player, ItemStack stack, Slot slot, SlotAccess cursorStackReference, ItemStack shulker) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.getRandom().nextFloat() * 0.4F);

        stack.remove(ModDataComponentTypes.SHULKER_COLOR);

        stack.remove(ModDataComponentTypes.ATTACHED_SHULKER);
        if (cursorStackReference != null) cursorStackReference.set(shulker);
        if (slot != null) slot.safeInsert(shulker);

        this.onContentChanged(player);
    }

    // Check if the item is a shulker box (any color)
    private boolean isShulkerBox(Item item) {
        List<Item> shulkerBoxes = List.of(Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX);
        return shulkerBoxes.contains(item);
    }

    // Update the player inventory
    private void onContentChanged(Player user) {
        AbstractContainerMenu screenHandler = user.containerMenu;
        if (screenHandler != null) {
            screenHandler.slotsChanged(user.getInventory());
        }
    }

}
