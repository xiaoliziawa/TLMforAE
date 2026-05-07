package com.lirxowo.tlmforae.network;

import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.network.message.SaveAEAutocraftConfigMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class ModNetwork {
    private static final String VERSION = "1.0.0";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Tlmforae.MODID, "network"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private ModNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(
                0,
                SaveAEAutocraftConfigMessage.class,
                SaveAEAutocraftConfigMessage::encode,
                SaveAEAutocraftConfigMessage::decode,
                SaveAEAutocraftConfigMessage::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
