package kr.mchoe.homesystems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A local, owner-controlled network with four independently switchable relay groups. */
public final class HomeNetwork {
    public static final int CHANNELS = 4, MAX_RELAYS = 32, RANGE = 64;
    private final DeviceBlockEntity controller;
    private UUID id = UUID.randomUUID();
    private final boolean[] enabled = new boolean[CHANNELS];
    private final Map<BlockPos, Integer> relays = new LinkedHashMap<>();
    public HomeNetwork(DeviceBlockEntity controller) { this.controller = controller; }
    public UUID id() { return id; }
    public boolean isWallpad() { return controller.getBlockState().is(HomeSystems.WALLPAD.get()); }
    public boolean enabled(int channel) { return channel >= 0 && channel < CHANNELS && enabled[channel]; }
    public int count(int channel) { return (int) relays.values().stream().filter(value -> value == channel).count(); }
    public boolean contains(BlockPos pos, int channel) { return relays.getOrDefault(pos, -1) == channel; }

    public boolean add(HomeRelayEntity relay) {
        if (!isWallpad() || !(controller.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || controller.getLevel() != relay.getLevel()
            || !EstateData.get(level.getServer()).same(level.dimension().location().toString(), controller.getBlockPos(), level.dimension().location().toString(), relay.getBlockPos())
            || controller.getBlockPos().distSqr(relay.getBlockPos()) > RANGE * RANGE
            || (!relays.containsKey(relay.getBlockPos()) && relays.size() >= MAX_RELAYS)) return false;
        relay.unlink();
        relays.put(relay.getBlockPos().immutable(), relay.channel());
        relay.bind(controller);
        controller.changed();
        return true;
    }
    public void remove(BlockPos pos) { if (relays.remove(pos) != null) controller.changed(); }
    public boolean toggle(int channel) {
        if (!isWallpad() || !EstateData.inside(controller) || channel < 0 || channel >= CHANNELS || count(channel) == 0 || controller.getLevel() == null || controller.getLevel().isClientSide) return false;
        enabled[channel] = !enabled[channel];
        for (var entry : relays.entrySet()) {
            if (entry.getValue() != channel || !controller.getLevel().hasChunkAt(entry.getKey())) continue;
            BlockEntity entity = controller.getLevel().getBlockEntity(entry.getKey());
            if (entity instanceof HomeRelayEntity relay && relay.matches(controller)) relay.setPowered(enabled[channel]);
        }
        controller.changed();
        return true;
    }
    public void tick() {
        if (!isWallpad() || controller.getLevel() == null || controller.getLevel().isClientSide) return;
        boolean changed = relays.entrySet().removeIf(entry -> {
            if (!controller.getLevel().hasChunkAt(entry.getKey())) return false;
            var entity = controller.getLevel().getBlockEntity(entry.getKey());
            return !(entity instanceof HomeRelayEntity relay) || !relay.matches(controller);
        });
        if (changed) controller.changed();
    }
    public void disconnectAll() {
        if (controller.getLevel() == null || controller.getLevel().isClientSide) return;
        for (BlockPos pos : java.util.List.copyOf(relays.keySet())) {
            if (controller.getLevel().hasChunkAt(pos) && controller.getLevel().getBlockEntity(pos) instanceof HomeRelayEntity relay && relay.matches(controller)) relay.unlink();
        }
        relays.clear();
        java.util.Arrays.fill(enabled, false);
        controller.changed();
    }
    public void save(CompoundTag tag) {
        tag.putUUID("HomeNetworkId", id);
        int flags = 0;
        for (int i = 0; i < CHANNELS; i++) if (enabled[i]) flags |= 1 << i;
        tag.putInt("HomeChannels", flags);
        ListTag entries = new ListTag();
        relays.forEach((pos, channel) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", pos.asLong()); entry.putInt("Channel", channel); entries.add(entry);
        });
        tag.put("HomeRelays", entries);
    }
    public void load(CompoundTag tag) {
        if (tag.hasUUID("HomeNetworkId")) id = tag.getUUID("HomeNetworkId");
        int flags = tag.getInt("HomeChannels");
        for (int i = 0; i < CHANNELS; i++) enabled[i] = (flags & (1 << i)) != 0;
        relays.clear();
        ListTag entries = tag.getList("HomeRelays", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(entries.size(), MAX_RELAYS); i++) {
            CompoundTag entry = entries.getCompound(i); int channel = entry.getInt("Channel");
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            if (channel >= 0 && channel < CHANNELS && controller.getBlockPos().distSqr(pos) <= RANGE * RANGE) relays.put(pos, channel);
        }
    }
}
