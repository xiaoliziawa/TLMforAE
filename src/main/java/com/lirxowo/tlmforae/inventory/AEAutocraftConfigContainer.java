package com.lirxowo.tlmforae.inventory;

import com.github.tartaricacid.touhoulittlemaid.inventory.container.task.TaskConfigContainer;
import com.lirxowo.tlmforae.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;

public class AEAutocraftConfigContainer extends TaskConfigContainer {
    public AEAutocraftConfigContainer(int id, Inventory inventory, int entityId) {
        super(ModMenuTypes.AE_AUTOCRAFT_CONFIG.get(), id, inventory, entityId);
    }
}
