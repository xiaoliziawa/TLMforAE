package com.lirxowo.tlmforae.network.message;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.lirxowo.tlmforae.init.ModTaskData;
import com.lirxowo.tlmforae.task.data.AEAutocraftConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record SaveAEAutocraftConfigMessage(int entityId, AEAutocraftConfig config) {
    public static void encode(SaveAEAutocraftConfigMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeNbt(sanitize(message.config).toPacketTag());
    }

    public static SaveAEAutocraftConfigMessage decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        CompoundTag tag = buf.readNbt();
        return new SaveAEAutocraftConfigMessage(
                entityId,
                tag == null ? AEAutocraftConfig.empty() : AEAutocraftConfig.fromPacketTag(tag));
    }

    public static void handle(SaveAEAutocraftConfigMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isServer()) {
            context.enqueueWork(() -> {
                ServerPlayer sender = context.getSender();
                if (sender == null) {
                    return;
                }
                Entity entity = sender.level().getEntity(message.entityId);
                if (entity instanceof EntityMaid maid && maid.isOwnedBy(sender)) {
                    maid.setAndSyncData(ModTaskData.AE_AUTOCRAFT_CONFIG, sanitize(message.config));
                    maid.refreshBrain((ServerLevel) sender.level());
                }
            });
        }
        context.setPacketHandled(true);
    }

    private static AEAutocraftConfig sanitize(AEAutocraftConfig config) {
        if (config == null) {
            return AEAutocraftConfig.empty();
        }
        List<AEAutocraftConfig.Request> requests = config.requests().stream()
                .map(AEAutocraftConfig.Request::sanitize)
                .filter(AEAutocraftConfig.Request::isConfigured)
                .limit(AEAutocraftConfig.MAX_REQUESTS)
                .toList();
        return new AEAutocraftConfig(requests);
    }
}
