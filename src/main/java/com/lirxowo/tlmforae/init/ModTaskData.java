package com.lirxowo.tlmforae.init;

import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.task.data.AEAutocraftConfig;
import net.minecraft.resources.ResourceLocation;

public final class ModTaskData {
    public static TaskDataKey<AEAutocraftConfig> AE_AUTOCRAFT_CONFIG;

    private ModTaskData() {
    }

    public static void register(TaskDataRegister register) {
        AE_AUTOCRAFT_CONFIG = register.register(ResourceLocation.fromNamespaceAndPath(Tlmforae.MODID, "ae_autocraft_config"), AEAutocraftConfig.CODEC);
    }
}
