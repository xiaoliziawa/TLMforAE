package com.lirxowo.tlmforae.task.ai;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.parts.AEBasePart;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.parts.reporting.ItemTerminalPart;
import appeng.util.Platform;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import com.google.common.collect.ImmutableMap;
import com.lirxowo.tlmforae.init.ModTaskData;
import com.lirxowo.tlmforae.task.TaskAEAutocraft;
import com.lirxowo.tlmforae.task.data.AEAutocraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MaidAEAutocraftTask extends MaidCheckRateTask {
    private final float speed;
    private final int closeEnoughDist;
    private Future<ICraftingPlan> pendingPlan;
    private TerminalTarget pendingTarget;
    private AEAutocraftConfig.Request pendingRequest;

    public MaidAEAutocraftTask(float speed, int closeEnoughDist) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.closeEnoughDist = closeEnoughDist;
        this.setMaxCheckRate(20);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid)) {
            return false;
        }
        if (!TaskAEAutocraft.UID.equals(maid.getTask().getUid())) {
            clearPendingPlan();
            return false;
        }
        if (this.pendingPlan != null) {
            finishPendingPlan(maid);
            return false;
        }
        AEAutocraftConfig config = getConfig(maid);
        if (!config.isConfigured()) {
            return false;
        }
        Optional<CraftTarget> craftTarget = findTarget(level, maid, config);
        if (craftTarget.isEmpty()) {
            return false;
        }
        TerminalTarget target = craftTarget.get().target();
        if (!target.requiresProximity()) {
            return shouldCraft(target.grid(), craftTarget.get().request());
        }
        BlockPos pos = target.pos();
        if (!maid.isWithinRestriction(pos)) {
            return false;
        }
        if (pos.distToCenterSqr(maid.position()) <= this.closeEnoughDist * this.closeEnoughDist) {
            return shouldCraft(target.grid(), craftTarget.get().request());
        }
        if (maid.canBrainMoving()) {
            BehaviorUtils.setWalkAndLookTargetMemories(maid, pos, this.speed, this.closeEnoughDist);
            this.setNextCheckTickCount(5);
        }
        return false;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        AEAutocraftConfig config = getConfig(maid);
        if (!config.isConfigured()) {
            return;
        }
        findTarget(level, maid, config).ifPresent(craftTarget -> {
            TerminalTarget target = craftTarget.target();
            if (!target.requiresProximity() || target.pos().distToCenterSqr(maid.position()) <= this.closeEnoughDist * this.closeEnoughDist) {
                submitCraft(level, target, craftTarget.request());
                if (target.requiresProximity()) {
                    maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos()));
                }
            }
        });
    }

    private AEAutocraftConfig getConfig(EntityMaid maid) {
        return maid.getOrCreateData(ModTaskData.AE_AUTOCRAFT_CONFIG, AEAutocraftConfig.empty());
    }

    private boolean shouldCraft(IGrid grid, AEAutocraftConfig.Request request) {
        AEItemKey key = request.key();
        if (key == null) {
            return false;
        }
        long stored = grid.getStorageService().getCachedInventory().get(key);
        if (stored >= request.threshold()) {
            return false;
        }
        return grid.getCraftingService().isCraftable(key) && !grid.getCraftingService().isRequesting(key);
    }

    private Optional<CraftTarget> findTarget(ServerLevel level, EntityMaid maid, AEAutocraftConfig config) {
        Optional<CraftTarget> wirelessTarget = findWirelessTarget(level, maid, config);
        if (wirelessTarget.isPresent()) {
            return wirelessTarget;
        }
        return findNearbyTarget(level, maid, config);
    }

    private Optional<CraftTarget> findWirelessTarget(ServerLevel level, EntityMaid maid, AEAutocraftConfig config) {
        return createWirelessTarget(level, maid.getMainHandItem())
                .or(() -> createWirelessTarget(level, maid.getOffhandItem()))
                .flatMap(target -> createCraftTarget(target, config));
    }

    private Optional<TerminalTarget> createWirelessTarget(ServerLevel level, ItemStack stack) {
        if (!(stack.getItem() instanceof WirelessTerminalItem terminal)) {
            return Optional.empty();
        }
        var linkedPos = terminal.getLinkedPosition(stack);
        if (linkedPos == null) {
            return Optional.empty();
        }
        ServerLevel linkedLevel = level.getServer().getLevel(linkedPos.dimension());
        BlockEntity blockEntity = Platform.getTickingBlockEntity(linkedLevel, linkedPos.pos());
        if (!(blockEntity instanceof IWirelessAccessPoint accessPoint) || !accessPoint.isActive()) {
            return Optional.empty();
        }
        IGrid grid = accessPoint.getGrid();
        IGridNode node = accessPoint.getActionableNode();
        if (grid == null || node == null || !node.isOnline()) {
            return Optional.empty();
        }
        return Optional.of(new TerminalTarget(linkedPos.pos(), grid, accessPoint, false));
    }

    private Optional<CraftTarget> findNearbyTarget(ServerLevel level, EntityMaid maid, AEAutocraftConfig config) {
        AABB box = maid.searchDimension();
        List<CraftTarget> candidates = new ArrayList<>();
        forEachBlockEntityInBox(level, box, blockEntity -> createTarget(blockEntity)
                .filter(target -> isNearbyTargetCandidate(maid, box, target))
                .flatMap(target -> createCraftTarget(target, config))
                .ifPresent(candidates::add));
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        BlockPos center = maid.hasRestriction() ? maid.getRestrictCenter() : maid.blockPosition();
        float horizontalRange = (float) Math.max(box.maxX - center.getX(), center.getX() - box.minX);
        int verticalRange = (int) Math.ceil(Math.max(box.maxY - center.getY(), center.getY() - box.minY));
        MaidPathFindingBFS pathFinding = new MaidPathFindingBFS(
                maid.getNavigation().getNodeEvaluator(),
                level,
                maid,
                horizontalRange,
                verticalRange);
        try {
            return candidates.stream()
                    .filter(craftTarget -> canReachTarget(pathFinding, craftTarget.target().pos()))
                    .min(Comparator.comparingDouble(craftTarget -> craftTarget.target().pos().distSqr(maid.blockPosition())));
        } finally {
            pathFinding.finish();
        }
    }

    private boolean canReachTarget(MaidPathFindingBFS pathFinding, BlockPos pos) {
        if (pathFinding.canPathReach(pos)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (pathFinding.canPathReach(pos.relative(direction))) {
                return true;
            }
        }
        return pathFinding.canPathReach(pos.above());
    }

    private void forEachBlockEntityInBox(ServerLevel level, AABB box, java.util.function.Consumer<BlockEntity> consumer) {
        int minChunkX = ((int) Math.floor(box.minX)) >> 4;
        int maxChunkX = ((int) Math.floor(box.maxX)) >> 4;
        int minChunkZ = ((int) Math.floor(box.minZ)) >> 4;
        int maxChunkZ = ((int) Math.floor(box.maxZ)) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                chunk.getBlockEntities().values().forEach(consumer);
            }
        }
    }

    private boolean isNearbyTargetCandidate(EntityMaid maid, AABB box, TerminalTarget target) {
        BlockPos pos = target.pos();
        return box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                && maid.isWithinRestriction(pos);
    }

    private Optional<CraftTarget> createCraftTarget(TerminalTarget target, AEAutocraftConfig config) {
        return config.requests().stream()
                .filter(request -> shouldCraft(target.grid(), request))
                .findFirst()
                .map(request -> new CraftTarget(target, request));
    }

    private Optional<TerminalTarget> createTarget(BlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity instanceof WirelessAccessPointBlockEntity wireless && wireless.isActive()) {
            IGrid grid = wireless.getGrid();
            if (grid != null) {
                return Optional.of(new TerminalTarget(pos, grid, wireless, true));
            }
        }
        if (blockEntity instanceof CableBusBlockEntity cableBus) {
            for (Direction direction : Direction.values()) {
                var part = cableBus.getPart(direction);
                if (part instanceof AEBasePart host && (part instanceof CraftingTerminalPart || part instanceof ItemTerminalPart)) {
                    IGridNode node = host.getActionableNode();
                    if (node != null && node.isOnline()) {
                        return Optional.of(new TerminalTarget(pos, node.getGrid(), host, true));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void submitCraft(ServerLevel level, TerminalTarget target, AEAutocraftConfig.Request request) {
        IGrid grid = target.grid();
        AEItemKey key = request.key();
        if (key == null) {
            return;
        }
        if (!shouldCraft(grid, request)) {
            return;
        }
        this.pendingPlan = grid.getCraftingService().beginCraftingCalculation(
                level,
                new SimulationRequester(target),
                key,
                request.craftAmount(),
                CalculationStrategy.REPORT_MISSING_ITEMS);
        this.pendingTarget = target;
        this.pendingRequest = request;
        this.setNextCheckTickCount(5);
    }

    private void finishPendingPlan(EntityMaid maid) {
        if (this.pendingPlan == null || !this.pendingPlan.isDone()) {
            this.setNextCheckTickCount(5);
            return;
        }
        try {
            ICraftingPlan plan = this.pendingPlan.get();
            TerminalTarget target = this.pendingTarget;
            AEAutocraftConfig.Request request = this.pendingRequest;
            if (plan != null && target != null && request != null && stillValid(target) && plan.missingItems().isEmpty() && shouldCraft(target.grid(), request)) {
                if (target.requiresProximity()) {
                    maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos()));
                }
                ICraftingSubmitResult result = target.grid().getCraftingService()
                        .submitJob(plan, null, null, true, IActionSource.ofMachine(target.host()));
                if (result != null && result.successful()) {
                    this.setNextCheckTickCount(100);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
        } finally {
            clearPendingPlan();
        }
    }

    private void clearPendingPlan() {
        this.pendingPlan = null;
        this.pendingTarget = null;
        this.pendingRequest = null;
    }

    private boolean stillValid(TerminalTarget target) {
        IGridNode node = target.host().getActionableNode();
        return node != null && node.isOnline() && node.getGrid() == target.grid();
    }

    private record TerminalTarget(BlockPos pos, IGrid grid, IActionHost host, boolean requiresProximity) {
    }

    private record CraftTarget(TerminalTarget target, AEAutocraftConfig.Request request) {
    }

    private record SimulationRequester(TerminalTarget target) implements ICraftingSimulationRequester {
        @Override
        public IActionSource getActionSource() {
            return IActionSource.ofMachine(target.host());
        }

        @Override
        public IGridNode getGridNode() {
            return target.host().getActionableNode();
        }
    }
}
