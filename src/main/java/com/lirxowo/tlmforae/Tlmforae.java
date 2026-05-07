package com.lirxowo.tlmforae;

import com.lirxowo.tlmforae.init.ModMenuTypes;
import com.lirxowo.tlmforae.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Tlmforae.MODID)
public class Tlmforae {

    public static final String MODID = "tlmforae";

    public Tlmforae(FMLJavaModLoadingContext context) {
        ModMenuTypes.MENU_TYPES.register(context.getModEventBus());
        ModNetwork.init();
    }
}
