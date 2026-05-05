package cn.breezeth.ordertocook.core;

import cn.breezeth.ordertocook.config.ConfigManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public final class CustomerProfileLibrary {
    private static final double EASTER_EGG_RATE = 0.08D;
    private static final List<EasterEggCustomer> EASTER_EGGS = List.of(
            new EasterEggCustomer("Breezeth", "微风(作者)", "a1b3e264-652b-42db-b8e7-5393b409776b"),
            new EasterEggCustomer("Kuroneko_RikoP", "猫猫子(贡献)", "e2ca8e07-c1b5-46e1-86f9-932718a44e60"),
            new EasterEggCustomer("YYN0114", "以诺(贡献)", "035c0419-c744-47eb-b2a2-470d92b3bb0c"),
            new EasterEggCustomer("Yunan_ovo", "遇南(贡献)", "772fcb25-c047-42b8-9d5e-df82d7a4a233"),
            new EasterEggCustomer("Ao2333", "Ao(美术)", "25b8aea8-c9c2-41ef-8f17-3b9c0f7aa365"),
            new EasterEggCustomer("Assumine", "Assumine(贡献)", "76640ae8-c4da-44ea-9d4b-53329ea02b2d"),
            new EasterEggCustomer("zsdr", "N44(美术)", "5a33e9b0-35bc-44ed-9b4e-03e3e180a3d2")
    );

    public record CustomerProfile(String displayName, int textureVariant, String skinAccount, String skinUuid, boolean easterEgg) {
        public Component toNameText() {
            return easterEgg ? Component.literal(displayName).withStyle(ChatFormatting.RED) : Component.literal(displayName);
        }
    }

    public record EasterEggCustomer(String accountName, String displayName, String skinUuid) {
    }

    public static CustomerProfile createOrderProfile(ServerLevel world) {
        if (!EASTER_EGGS.isEmpty() && world.random.nextDouble() < EASTER_EGG_RATE) {
            return createGuaranteedEasterEggProfile(world);
        }

        double slimRate = ConfigManager.get().customerFemaleRate;
        if (slimRate > 1.0 && slimRate <= 100.0) slimRate /= 100.0;
        slimRate = Math.max(0.0, Math.min(1.0, slimRate));
        int textureVariant = world.random.nextDouble() < slimRate ? 2 : 1;
        return new CustomerProfile(NpcNames.random(world.getRandom()), textureVariant, "", "", false);
    }

    public static CustomerProfile createWalkInProfile(ServerLevel world, String customerName) {
        return new CustomerProfile(customerName, 0, "", "", false);
    }

    public static CustomerProfile createGuaranteedEasterEggProfile(ServerLevel world) {
        if (EASTER_EGGS.isEmpty()) {
            return createWalkInProfile(world, NpcNames.random(world.getRandom()));
        }
        EasterEggCustomer entry = EASTER_EGGS.get(world.random.nextInt(EASTER_EGGS.size()));
        return new CustomerProfile(entry.displayName(), 1, entry.accountName(), entry.skinUuid(), true);
    }

    public static CustomerProfile fromNbt(CompoundTag nbt, ServerLevel world) {
        String displayName = nbt.getString(ModConstants.NBT_CUSTOMER_NAME);
        if (displayName == null || displayName.isBlank()) {
            displayName = NpcNames.random(world.getRandom());
        }
        int textureVariant = nbt.contains(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT)
                ? nbt.getInt(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT)
                : 1;
        String skinAccount = nbt.contains(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT)
                ? nbt.getString(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT)
                : "";
        String skinUuid = nbt.contains(ModConstants.NBT_CUSTOMER_SKIN_UUID)
                ? nbt.getString(ModConstants.NBT_CUSTOMER_SKIN_UUID)
                : "";
        boolean easterEgg = nbt.contains(ModConstants.NBT_CUSTOMER_EASTER_EGG)
                && nbt.getBoolean(ModConstants.NBT_CUSTOMER_EASTER_EGG);
        return new CustomerProfile(displayName, textureVariant, skinAccount, skinUuid, easterEgg);
    }

    public static void writeToNbt(CompoundTag nbt, CustomerProfile profile) {
        nbt.putString(ModConstants.NBT_CUSTOMER_NAME, profile.displayName());
        nbt.putInt(ModConstants.NBT_CUSTOMER_TEXTURE_VARIANT, profile.textureVariant());
        if (profile.skinAccount() != null && !profile.skinAccount().isBlank()) {
            nbt.putString(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT, profile.skinAccount());
        } else {
            nbt.remove(ModConstants.NBT_CUSTOMER_SKIN_ACCOUNT);
        }
        if (profile.skinUuid() != null && !profile.skinUuid().isBlank()) {
            nbt.putString(ModConstants.NBT_CUSTOMER_SKIN_UUID, profile.skinUuid());
        } else {
            nbt.remove(ModConstants.NBT_CUSTOMER_SKIN_UUID);
        }
        nbt.putBoolean(ModConstants.NBT_CUSTOMER_EASTER_EGG, profile.easterEgg());
    }

    private CustomerProfileLibrary() {
    }
}
