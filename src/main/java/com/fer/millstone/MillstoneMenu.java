package com.fer.millstone;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MillstoneMenu extends AbstractContainerMenu {
    public final MillstoneBlockEntity blockEntity;
    private final ContainerData data;

    // Cliente
    public MillstoneMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(2));
    }

    // Servidor
    public MillstoneMenu(int id, Inventory inv, BlockEntity be, ContainerData data) {
        super(ModRegistry.MILLSTONE_MENU.get(), id);
        this.blockEntity = (MillstoneBlockEntity) be;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getInventory(), 0, 56, 35));   // entrada
        addSlot(new SlotItemHandler(blockEntity.getInventory(), 1, 116, 35));  // salida (isItemValid=false => solo sacar)

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    /** Ancho de la flecha de progreso (0-24 px). */
    public int getScaledProgress() {
        int progress = data.get(0);
        int max = data.get(1);
        return max != 0 && progress != 0 ? progress * 24 / max : 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < 2) {
                // De la trituradora al inventario del jugador
                if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 1, false)) {
                // Del jugador a la entrada; si no entra, mover entre inventario y hotbar
                if (index < 29) {
                    if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModRegistry.MILLSTONE_BLOCK.get());
    }
}
