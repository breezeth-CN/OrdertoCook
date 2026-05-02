package cn.breezeth.ordertocook.block.entity;

import cn.breezeth.ordertocook.config.ConfigManager;
import cn.breezeth.ordertocook.registry.ModBlockEntities;
import cn.breezeth.ordertocook.util.ImplementedInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtLong;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;


public class BoardBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, ImplementedInventory {
    private final DefaultedList<ItemStack> templates = DefaultedList.ofSize(150, ItemStack.EMPTY);
    private boolean defaultsInitialized = false;
    private int sortMode = 0; // 0: by nutrition desc, 1: by id asc then nutrition desc, 2: by last-set-time desc

    public BoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOARD, pos, state);
        applyDefaultTemplates();
        defaultsInitialized = true;
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return templates;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, templates, registryLookup);
        nbt.putBoolean("DefaultsInitialized", defaultsInitialized);
        nbt.putInt("SortMode", sortMode);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, templates, registryLookup);
        defaultsInitialized = nbt.getBoolean("DefaultsInitialized");
        sortMode = nbt.contains("SortMode") ? nbt.getInt("SortMode") : 0;
        if (!defaultsInitialized && isTemplatesEmpty()) {
            applyDefaultTemplates();
            defaultsInitialized = true;
        }
        compactAndSort();
    }

    public NbtCompound toCompactItemNbt() {
        NbtCompound out = new NbtCompound();
        out.putBoolean("DefaultsInitialized", defaultsInitialized);
        out.putInt("SortMode", sortMode);
        NbtList list = new NbtList();
        NbtList times = new NbtList();
        HashSet<String> seen = new HashSet<>();
        for (ItemStack s : templates) {
            if (s.isEmpty()) continue;
            String id = Registries.ITEM.getId(s.getItem()).toString();
            if (seen.add(id)) {
                list.add(NbtString.of(id));
                times.add(NbtLong.of(lastSetTimeOf(s)));
            }
        }
        out.put("OtcBoardTemplateIds", list);
        out.put("OtcBoardTemplateTimes", times);
        return out;
    }

    public void applyFromCompactItemNbt(NbtCompound nbt) {
        if (nbt == null) return;
        if (nbt.contains("SortMode")) sortMode = nbt.getInt("SortMode");
        defaultsInitialized = true;
        for (int i = 0; i < templates.size(); i++) {
            templates.set(i, ItemStack.EMPTY);
        }
        if (nbt.contains("OtcBoardTemplateIds", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("OtcBoardTemplateIds", NbtElement.STRING_TYPE);
            NbtList times = nbt.contains("OtcBoardTemplateTimes", NbtElement.LIST_TYPE)
                    ? nbt.getList("OtcBoardTemplateTimes", NbtElement.LONG_TYPE) : null;
            HashSet<Identifier> seen = new HashSet<>();
            int write = 0;
            for (int i = 0; i < list.size() && write < templates.size(); i++) {
                Identifier id = Identifier.tryParse(list.getString(i));
                if (id == null) continue;
                if (!Registries.ITEM.containsId(id)) continue;
                if (!seen.add(id)) continue;
                Item item = Registries.ITEM.get(id);
                ItemStack stack = new ItemStack(item);
                stack.setCount(1);
                long ts = 0L;
                if (times != null && i < times.size()) {
                    ts = ((NbtLong) times.get(i)).longValue();
                }
                net.minecraft.nbt.NbtCompound tag = new net.minecraft.nbt.NbtCompound();
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
        markDirty();
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
        Identifier id = Registries.ITEM.getId(item);
        for (ItemStack s : templates) {
            if (!s.isEmpty() && Registries.ITEM.getId(s.getItem()).equals(id)) {
                return false;
            }
        }
        for (int i = 0; i < templates.size(); i++) {
            if (templates.get(i).isEmpty()) {
                ItemStack copy = source.copy();
                copy.setCount(1);
                net.minecraft.nbt.NbtCompound tag = cn.breezeth.ordertocook.util.DataCompat.copy(copy);
                if (tag == null) tag = new net.minecraft.nbt.NbtCompound();
                tag.putLong("OtcLastSetTime", System.currentTimeMillis());
                cn.breezeth.ordertocook.util.DataCompat.set(copy, tag);
                templates.set(i, copy);
                compactAndSort();
                markDirty();
                return true;
            }
        }
        return false;
    }

    public void removeAt(int absoluteIndex) {
        if (absoluteIndex < 0 || absoluteIndex >= templates.size()) return;
        templates.set(absoluteIndex, ItemStack.EMPTY);
        compactAndSort();
        markDirty();
    }

    public int getSortMode() {
        return sortMode;
    }

    public void setSortMode(int mode) {
        int next = (mode == 1) ? 1 : (mode == 2 ? 2 : 0);
        if (sortMode == next) return;
        sortMode = next;
        compactAndSort();
        markDirty();
    }

    public void toggleSortMode() {
        setSortMode(sortMode == 0 ? 1 : (sortMode == 1 ? 2 : 0));
    }

    private int nutritionOf(ItemStack s) {
        FoodComponent fc = s.get(net.minecraft.component.DataComponentTypes.FOOD);
        if (fc != null && fc.nutrition() > 0) {
            return fc.nutrition();
        }
        return ConfigManager.getCustomMenuNutrition(s);
    }

    private String idOf(ItemStack s) {
        return Registries.ITEM.getId(s.getItem()).toString();
    }

    private long lastSetTimeOf(ItemStack s) {
        net.minecraft.nbt.NbtCompound tag = cn.breezeth.ordertocook.util.DataCompat.copy(s);
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
    public Text getDisplayName() {
        return Text.translatable("block.ordertocook.board");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new cn.breezeth.ordertocook.screen.BoardScreenHandler(syncId, playerInventory, this);
    }
}
