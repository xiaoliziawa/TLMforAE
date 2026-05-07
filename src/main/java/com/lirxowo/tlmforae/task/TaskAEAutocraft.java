package com.lirxowo.tlmforae.task;

import appeng.core.definitions.AEItems;
import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.lirxowo.tlmforae.Tlmforae;
import com.lirxowo.tlmforae.inventory.AEAutocraftConfigContainer;
import com.lirxowo.tlmforae.task.ai.MaidAEAutocraftTask;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class TaskAEAutocraft implements IMaidTask {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Tlmforae.MODID, "ae_autocraft");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return AEItems.WIRELESS_CRAFTING_TERMINAL.stack();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return InitSounds.MAID_FURNACE.get();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(5, new MaidAEAutocraftTask(0.6f, 2)));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        return createBrainTasks(maid);
    }

    @Override
    public boolean enableLookAndRandomWalk(EntityMaid maid) {
        return false;
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        int entityId = maid.getId();
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.tlmforae.ae_autocraft_config");
            }

            @Override
            public AEAutocraftConfigContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new AEAutocraftConfigContainer(index, playerInventory, entityId);
            }
        };
    }

    @Override
    public String getMaidActionSummary() {
        return "Ask the maid to watch nearby AE2 networks and request auto-crafting when stock is below the configured threshold.";
    }
}
