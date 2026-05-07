package com.lirxowo.tlmforae.init;

import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.inventory.AEAutocraftConfigContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Tlmforae.MODID);
    public static final RegistryObject<MenuType<AEAutocraftConfigContainer>> AE_AUTOCRAFT_CONFIG = MENU_TYPES.register(
            "ae_autocraft_config",
            () -> IForgeMenuType.create((windowId, inv, data) -> new AEAutocraftConfigContainer(windowId, inv, data.readInt())));

    private ModMenuTypes() {
    }
}
