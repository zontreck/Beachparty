package net.satisfyu.beachparty.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.satisfyu.beachparty.client.gui.handler.TikiBarGuiHandler;
import net.satisfyu.beachparty.recipe.TikiBarRecipe;
import net.satisfyu.beachparty.registry.EntityRegistry;
import net.satisfyu.beachparty.registry.RecipeRegistry;
import org.jetbrains.annotations.Nullable;

public class TikiBarBlockEntity extends BlockEntity implements Inventory, BlockEntityTicker<TikiBarBlockEntity>, NamedScreenHandlerFactory {

    private DefaultedList<ItemStack> inventory;
    public static final int CAPACITY = 5;
    public static final int COOKING_TIME_IN_TICKS = 1800; // 90s or 3 minutes
    private static final int OUTPUT_SLOT = 0;
    private int fermentationTime = 0;
    private int totalFermentationTime;
    protected float experience;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> TikiBarBlockEntity.this.fermentationTime;
                case 1 -> TikiBarBlockEntity.this.totalFermentationTime;
                default -> 0;
            };
        }


        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> TikiBarBlockEntity.this.fermentationTime = value;
                case 1 -> TikiBarBlockEntity.this.totalFermentationTime = value;
            }
        }

        @Override
        public int size() {
            return 2;
        }
    };

    public TikiBarBlockEntity(BlockPos pos, BlockState state) {
        super(EntityRegistry.TIKI_BAR_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(CAPACITY, ItemStack.EMPTY);
    }

    public void dropExperience(ServerWorld world, Vec3d pos) {
        ExperienceOrbEntity.spawn(world, pos, (int) experience);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.inventory);
        this.fermentationTime = nbt.getShort("FermentationTime");
        this.experience = nbt.getFloat("Experience");

    }


    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, this.inventory);
        nbt.putFloat("Experience", this.experience);
        nbt.putShort("FermentationTime", (short) this.fermentationTime);
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state, TikiBarBlockEntity blockEntity) {
        if (world.isClient) return;
        boolean dirty = false;
        final var recipeType = world.getRecipeManager()
                .getFirstMatch(RecipeRegistry.TIKI_BAR_RECIPE_RECIPE_TYPE, blockEntity, world)
                .orElse(null);
        if (canCraft(recipeType)) {
            this.fermentationTime++;

            if (this.fermentationTime == this.totalFermentationTime) {
                this.fermentationTime = 0;
                craft(recipeType);
                dirty = true;
            }
        } else {
            this.fermentationTime = 0;
        }
        if (dirty) {
            markDirty();
        }

    }

    private boolean canCraft(TikiBarRecipe recipe) {
        if (recipe == null || recipe.getOutput().isEmpty()) {
            return false;
        } else if (areInputsEmpty()) {
            return false;
            }
            return this.getStack(OUTPUT_SLOT).isEmpty();
        }


    private boolean areInputsEmpty() {
        int emptyStacks = 0;
        for (int i = 2; i < 6; i++) {
            if (this.getStack(i).isEmpty()) emptyStacks++;
        }
        return emptyStacks == 4;
    }
    private void craft(TikiBarRecipe recipe) {
        if (!canCraft(recipe)) {
            return;
        }
        final ItemStack recipeOutput = recipe.getOutput();
        final ItemStack outputSlotStack = this.getStack(OUTPUT_SLOT);
        if (outputSlotStack.isEmpty()) {
            ItemStack output = recipeOutput.copy();
            setStack(OUTPUT_SLOT, output);
        }
        for (Ingredient entry : recipe.getIngredients()) {
            if (entry.test(this.getStack(1))) {
                removeStack(1, 1);
            }
            if (entry.test(this.getStack(2))) {
                removeStack(2, 1);
            }
        }
    }


    @Override
    public int size() {
        return CAPACITY;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        final ItemStack stackInSlot = this.inventory.get(slot);
        boolean dirty = !stack.isEmpty() && stack.isItemEqualIgnoreDamage(stackInSlot) && ItemStack.areNbtEqual(stack, stackInSlot);
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        if (slot == 1 || slot == 2) {
            if (!dirty) {
                this.totalFermentationTime = 50;
                this.fermentationTime = 0;
                markDirty();
            }
        }
    }
    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.world.getBlockEntity(this.pos) != this) {
            return false;
        } else {
            return player.squaredDistanceTo((double)this.pos.getX() + 0.5, (double)this.pos.getY() + 0.5, (double)this.pos.getZ() + 0.5) <= 64.0;
        }
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }


    @Override
    public Text getDisplayName() {
        return Text.translatable(this.getCachedState().getBlock().getTranslationKey());
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new TikiBarGuiHandler(syncId, inv, this, this.propertyDelegate);
    }
}