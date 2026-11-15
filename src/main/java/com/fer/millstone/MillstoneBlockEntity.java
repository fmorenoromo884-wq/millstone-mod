package com.fer.millstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Optional;

public class MillstoneBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CRUSH_TIME = 100;      // 5 segundos
    public static final int EFFECT_INTERVAL = 10;  // 0.5 segundos
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && hasRecipe(stack);
        }
    };

    /** Lo que ven las tolvas/automatización: solo se mete en la entrada, solo se saca de la salida. */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override public int getSlots() { return 2; }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_INPUT ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(slot, stack); }
    };

    private int progress = 0;
    private int tickCounter = 0;

    /** Solo cliente: ángulo de la rueda para el renderer. */
    public float wheelAngle = 0f;
    public static final float WHEEL_SPEED = 6f; // grados por tick

    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return index == 0 ? progress : CRUSH_TIME; }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int getCount() { return 2; }
    };

    public MillstoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.MILLSTONE_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public IItemHandler getAutomationHandler() {
        return automationHandler;
    }

    // ------------------------------------------------------------------ recetas

    private Optional<RecipeHolder<MillstoneRecipe>> findRecipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return Optional.empty();
        return level.getRecipeManager().getRecipeFor(ModRegistry.CRUSHING_TYPE.get(), new SingleRecipeInput(stack), level);
    }

    private boolean hasRecipe(ItemStack stack) {
        return findRecipe(stack).isPresent();
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    // ------------------------------------------------------------------ ticks

    public static void serverTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity be) {
        be.tickCounter++;

        // Absorción tipo tolva (cada 8 ticks), SIEMPRE, con o sin redstone.
        if (be.tickCounter % 8 == 0) {
            be.absorbFromAbove(level, pos);
        }

        ItemStack input = be.inventory.getStackInSlot(SLOT_INPUT);
        Optional<RecipeHolder<MillstoneRecipe>> recipe = be.findRecipe(input);

        boolean active = false;
        if (recipe.isEmpty()) {
            if (be.progress != 0) {
                be.progress = 0;
                be.setChanged();
            }
        } else if (be.canOutput(recipe.get().value().getResult())) {
            active = true;
            be.progress++;
            if (be.progress % EFFECT_INTERVAL == 0) {
                be.playEffects(level, pos, input);
                be.setChanged();
            }
            if (be.progress >= CRUSH_TIME) {
                be.finishCrush(recipe.get().value());
            }
        }
        // Si la salida está llena, el progreso se queda pausado (y la rueda se detiene).

        if (state.getValue(MillstoneBlock.LIT) != active) {
            level.setBlock(pos, state.setValue(MillstoneBlock.LIT, active), 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, MillstoneBlockEntity be) {
        if (state.getValue(MillstoneBlock.LIT)) {
            be.wheelAngle = (be.wheelAngle + WHEEL_SPEED) % 360f;
        }
    }

    private void finishCrush(MillstoneRecipe recipe) {
        ItemStack result = recipe.getResult();
        inventory.extractItem(SLOT_INPUT, 1, false);
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            inventory.setStackInSlot(SLOT_OUTPUT, out.copyWithCount(out.getCount() + result.getCount()));
        }
        progress = 0;
        setChanged();
    }

    // ------------------------------------------------------------------ efectos

    private void playEffects(Level level, BlockPos pos, ItemStack input) {
        // Sonido: el "golpe" del material que está adentro (o piedra por defecto)
        SoundEvent hit = SoundEvents.STONE_HIT;
        if (input.getItem() instanceof BlockItem bi) {
            hit = bi.getBlock().defaultBlockState().getSoundType().getHitSound();
        }
        level.playSound(null, pos, hit, SoundSource.BLOCKS, 0.7F, 0.6F + level.random.nextFloat() * 0.3F);

        // Partículas de ruptura del ítem exacto, hacia arriba
        if (level instanceof ServerLevel sl) {
            ItemParticleOption opt = new ItemParticleOption(ParticleTypes.ITEM, input.copyWithCount(1));
            for (int i = 0; i < 8; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 0.12;
                double dz = (level.random.nextDouble() - 0.5) * 0.12;
                sl.sendParticles(opt,
                        pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4,
                        0, dx, 0.22, dz, 1.0);
            }
        }
    }

    // ------------------------------------------------------------------ entrada tipo tolva

    private void absorbFromAbove(Level level, BlockPos pos) {
        // 1) Inventario justo arriba (cofre, barril, etc.)
        IItemHandler above = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (above != null) {
            for (int i = 0; i < above.getSlots(); i++) {
                ItemStack sim = above.extractItem(i, 1, true);
                if (sim.isEmpty()) continue;
                if (!inventory.insertItem(SLOT_INPUT, sim, true).isEmpty()) continue;
                ItemStack taken = above.extractItem(i, 1, false);
                inventory.insertItem(SLOT_INPUT, taken, false);
                return;
            }
        }
        // 2) Ítems tirados encima
        AABB box = new AABB(pos.getX(), pos.getY() + 0.7, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.5, pos.getZ() + 1.0);
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive)) {
            ItemStack stack = ie.getItem();
            ItemStack remainder = inventory.insertItem(SLOT_INPUT, stack.copy(), false);
            if (remainder.getCount() != stack.getCount()) {
                if (remainder.isEmpty()) ie.discard();
                else ie.setItem(remainder);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ drops / guardado

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, inventory.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    // ------------------------------------------------------------------ menú

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.millstone.millstone");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MillstoneMenu(id, inventory, this, data);
    }
}
