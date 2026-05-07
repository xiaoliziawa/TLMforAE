package com.lirxowo.tlmforae;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.lirxowo.tlmforae.init.ModTaskData;
import com.lirxowo.tlmforae.task.TaskAEAutocraft;
import com.lirxowo.tlmforae.task.ai.AEAutocraftExtraBrain;

@LittleMaidExtension
public class TlmforaeMaidExtension implements ILittleMaid {
    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new TaskAEAutocraft());
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new AEAutocraftExtraBrain());
    }

    @Override
    public void registerTaskData(TaskDataRegister register) {
        ModTaskData.register(register);
    }
}
