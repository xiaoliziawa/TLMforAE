package com.lirxowo.tlmforae.task.data;

import appeng.api.stacks.AEItemKey;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AEAutocraftConfig(List<Request> requests) {
    public static final int MAX_REQUESTS = 32;

    private static final Codec<ItemStack> ITEM_STACK_CODEC = CompoundTag.CODEC.xmap(
            tag -> ItemStack.of(tag.copy()),
            stack -> stack.isEmpty() ? new CompoundTag() : stack.save(new CompoundTag()));

    public static final Codec<Request> REQUEST_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_STACK_CODEC.fieldOf("target").forGetter(Request::target),
            Codec.INT.fieldOf("craft_amount").forGetter(Request::craftAmount),
            Codec.LONG.fieldOf("threshold").forGetter(Request::threshold)
    ).apply(instance, Request::new));

    private static final Codec<AEAutocraftConfig> LIST_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            REQUEST_CODEC.listOf().fieldOf("requests").forGetter(AEAutocraftConfig::requests)
    ).apply(instance, AEAutocraftConfig::new));

    private static final Codec<AEAutocraftConfig> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ITEM_STACK_CODEC.fieldOf("target").forGetter(config -> config.requests().isEmpty() ? ItemStack.EMPTY : config.requests().get(0).target()),
            Codec.INT.fieldOf("craft_amount").forGetter(config -> config.requests().isEmpty() ? 1 : config.requests().get(0).craftAmount()),
            Codec.LONG.fieldOf("threshold").forGetter(config -> config.requests().isEmpty() ? 0 : config.requests().get(0).threshold())
    ).apply(instance, (target, craftAmount, threshold) -> new AEAutocraftConfig(List.of(new Request(target, craftAmount, threshold)))));

    public static final Codec<AEAutocraftConfig> CODEC = Codec.either(LIST_CODEC, LEGACY_CODEC)
            .xmap(either -> either.map(config -> config, config -> config), Either::left);

    public AEAutocraftConfig {
        requests = List.copyOf(requests.stream()
                .map(Request::sanitize)
                .filter(Request::isConfigured)
                .limit(MAX_REQUESTS)
                .toList());
    }

    public static AEAutocraftConfig empty() {
        return new AEAutocraftConfig(List.of());
    }

    public boolean isConfigured() {
        return this.requests.stream().anyMatch(Request::isConfigured);
    }

    public CompoundTag toPacketTag() {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
                .result()
                .map(tag -> (CompoundTag) tag)
                .orElseGet(CompoundTag::new);
    }

    public static AEAutocraftConfig fromPacketTag(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag)
                .result()
                .or(() -> readLegacy(tag))
                .orElseGet(AEAutocraftConfig::empty);
    }

    private static java.util.Optional<AEAutocraftConfig> readLegacy(CompoundTag tag) {
        if (!tag.contains("target")) {
            return java.util.Optional.empty();
        }
        ItemStack target = ItemStack.of(tag.getCompound("target"));
        int craftAmount = tag.contains("craft_amount") ? tag.getInt("craft_amount") : 1;
        long threshold = tag.contains("threshold") ? tag.getLong("threshold") : 0;
        return java.util.Optional.of(new AEAutocraftConfig(List.of(new Request(target, craftAmount, threshold))));
    }

    public record Request(ItemStack target, int craftAmount, long threshold) {
        public boolean isConfigured() {
            return !this.target.isEmpty() && this.craftAmount > 0 && this.key() != null;
        }

        public AEItemKey key() {
            return AEItemKey.of(this.target);
        }

        public Request sanitize() {
            ItemStack cleanTarget = this.target.copy();
            if (!cleanTarget.isEmpty()) {
                cleanTarget.setCount(1);
            }
            return new Request(cleanTarget, Math.max(1, this.craftAmount), Math.max(0, this.threshold));
        }
    }
}
