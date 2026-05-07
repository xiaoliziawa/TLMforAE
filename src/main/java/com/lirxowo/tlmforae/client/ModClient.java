package com.lirxowo.tlmforae.client;

import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.client.gui.AEAutocraftConfigScreen;
import com.lirxowo.tlmforae.init.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Tlmforae.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModClient {
    private ModClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.AE_AUTOCRAFT_CONFIG.get(), AEAutocraftConfigScreen::new));
    }
}
