package com.lirxowo.tlmforae.task.ai;

import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

import java.util.List;

public class AEAutocraftExtraBrain implements IExtraMaidBrain {
    private static List<Pair<Integer, BehaviorControl<? super EntityMaid>>> autocraftBehaviors() {
        return List.of(Pair.of(9, new MaidAEAutocraftTask(0.6f, 2)));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getIdleBehaviors() {
        return autocraftBehaviors();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRestBehaviors() {
        return autocraftBehaviors();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideIdleBehaviors() {
        return autocraftBehaviors();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideRestBehaviors() {
        return autocraftBehaviors();
    }
}
