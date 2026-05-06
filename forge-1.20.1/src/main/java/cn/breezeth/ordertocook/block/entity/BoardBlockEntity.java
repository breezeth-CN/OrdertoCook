package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


public class BoardBlockEntity extends BlockEntity implements MenuProvider, ImplementedInventory {
    private final NonNullList<ItemStack> templates = NonNullList.withSize(150, ItemStack.EMPTY);
    private boolean defaultsInitialized = false;
    private int sortMode = 0; // 0: by nutrition desc, 1: by id asc then nutrition desc, 2: by last-set-time desc

    public BoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOARD.get(), pos, state);
        applyDefaultTemplates();
        defaultsInitialized = true;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return templates;
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, templates);
        nbt.putBoolean("DefaultsInitialized", defaultsInitialized);
        nbt.putInt("SortMode", sortMode);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        ContainerHelper.loadAllItems(nbt, templates);
        defaultsInitialized = nbt.getBoolean("DefaultsInitialized");
        sortMode = nbt.contains("SortMode") ? nbt.getInt("SortMode") : 0;
        if (!defaultsInitialized && isTemplatesEmpty()) {
            applyDefaultTemplates();
            defaultsInitialized = true;
        }
        compactAndSort();
    }

    public CompoundTag toCompactItemNbt() {
        CompoundTag out = new CompoundTag();
        out.putBoolean("DefaultsInitialized", defaultsInitialized);
        out.putInt("SortMode", sortMode);
        ListTag list = new ListTag();
        ListTag times = new ListTag();
        HashSet<String> seen = new HashSet<>();
        for (ItemStack s : templates) {
            if (s.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
            if (seen.add(id)) {
                list.add(StringTag.valueOf(id));
                times.add(LongTag.valueOf(lastSetTimeOf(s)));
            }
        }
        out.put("OtcBoardTemplateIds", list);
        out.put("OtcBoardTemplateTimes", times);
        return out;
    }

    public void applyFromCompactItemNbt(CompoundTag nbt) {
        if (nbt == null) return;
        if (nbt.contains("SortMode")) sortMode = nbt.getInt("SortMode");
        defaultsInitialized = true;
        for (int i = 0; i < templates.size(); i++) {
            templates.set(i, ItemStack.EMPTY);
        }
        if (nbt.contains("OtcBoardTemplateIds", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("OtcBoardTemplateIds", Tag.TAG_STRING);
            ListTag times = nbt.contains("OtcBoardTemplateTimes", Tag.TAG_LIST)
                    ? nbt.getList("OtcBoardTemplateTimes", Tag.TAG_LONG) : null;
            HashSet<ResourceLocation> seen = new HashSet<>();
            int write = 0;
            for (int i = 0; i < list.size() && write < templates.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
                if (id == null) continue;
                if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
                if (!seen.add(id)) continue;
                Item item = BuiltInRegistries.ITEM.get(id);
                ItemStack stack = new ItemStack(item);
                stack.setCount(1);
                long ts = 0L;
                if (times != null && i < times.size()) {
                    ts = ((LongTag) times.get(i)).getAsLong();
                }
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putLong("OtcLastSetTime", ts);
                cn.breezeth.ordertocook.util.DataCompat.set(stack, tag);
                templates.set(write++, stack);
            }
        }
        if (isTemplatesEmpty()) {
            defaultsInitialized = false;
            applyDefaultTemplates();
            defaultsInitialized = true;
        } else {
            compactAndSort();
        }
        setChanged();
    }

    private void applyDefaultTemplates() {
        templates.set(0, new ItemStack(Items.COOKED_PORKCHOP));
        templates.set(1, new ItemStack(Items.COOKED_CHICKEN));
        templates.set(2, new ItemStack(Items.COOKED_MUTTON));
        templates.set(3, new ItemStack(Items.BREAD));
        compactAndSort();
    }

    private boolean isTemplatesEmpty() {
        for (ItemStack s : templates) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    public boolean tryAddTemplate(ItemStack source) {
        if (source.isEmpty()) return false;
        if (nutritionOf(source) <= 0) return false;
        Item item = source.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        for (ItemStack s : templates) {
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).equals(id)) {
                return false;
            }
        }
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).isEmpty()) {
                ItemStack copy = source.copy();
                copy.setCount(1);
                net.minecraft.nbt.CompoundTag tag = cn.breezeth.ordertocook.util.DataCompat.copy(copy);
                if (tag == null) tag = new net.minecraft.nbt.CompoundTag();
                tag.putLong("OtcLastSetTime", System.currentTimeMillis());
                cn.breezeth.ordertocook.util.DataCompat.set(copy, tag);
                templates.set(i, copy);
                compactAndSort();
                setChanged();
                return true;
            }
        }
        return false;
    }

    public void removeAt(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= templates.size()) return;
        templates.set(absoluteIndex, ItemStack.EMPTY);
        compactAndSort();
        setChanged();
    }

    public int getSortMode() {
        return sortMode;
    }

    public void setSortMode(int mode) {
        int next = (mode == 1) ? 1 : (mode == 2 ? 2 : 0);
        if (sortMode == next) return;
        sortMode = next;
        compactAndSort();
        setChanged();
    }

    public void toggleSortMode() {
        setSortMode(sortMode == 0 ? 1 : (sortMode == 1 ? 2 : 0));
    }

    private int nutritionOf(ItemStack s) {
        FoodProperties fc = s.getItem().getFoodProperties();
        if (fc != null && fc.getNutrition() > 0) {
            return fc.getNutrition();
        }
        return ConfigManager.getCustomMenuNutrition(s);
    }

    private String idOf(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    private long lastSetTimeOf(ItemStack s) {
        net.minecraft.nbt.CompoundTag tag = cn.breezeth.ordertocook.util.DataCompat.copy(s);
        if (tag == null) return 0L;
        return tag.contains("OtcLastSetTime") ? tag.getLong("OtcLastSetTime") : 0L;
    }

    public int getTotalNutrition() {
        int total = 0;
        for (ItemStack s : templates) {
            if (s.isEmpty()) continue;
            total += nutritionOf(s);
        }
        return total;
    }

    private void compactAndSort() {
        int write = 0;
        for (int read = 0; read < templates.size(); read++) {
            ItemStack s = templates.get(read);
            if (!s.isEmpty()) {
                if (read != write) {
                    templates.set(write, s);
                    templates.set(read, ItemStack.EMPTY);
                }
                write++;
            }
        }
        templates.sort(new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack a, ItemStack b) {
                boolean ea = a.isEmpty();
                boolean eb = b.isEmpty();
                if (ea && eb) return 0;
                if (ea) return 1;
                if (eb) return -1;
                if (sortMode == 2) {
                    return Long.compare(lastSetTimeOf(b), lastSetTimeOf(a));
                } else if (sortMode == 1) {
                    int byId = idOf(a).compareTo(idOf(b));
                    if (byId != 0) return byId;
                    return Integer.compare(nutritionOf(b), nutritionOf(a));
                }
                int byNut = Integer.compare(nutritionOf(b), nutritionOf(a));
                if (byNut != 0) return byNut;
                return idOf(a).compareTo(idOf(b));
            }
        });
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ordertocook.board");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new cn.breezeth.ordertocook.screen.BoardScreenHandler(syncId, playerInventory, this);
    }
}
