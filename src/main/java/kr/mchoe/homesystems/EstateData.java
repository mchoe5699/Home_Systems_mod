package kr.mchoe.homesystems;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Rectangular parcels retain their identities when grouped. A group's footprint is their union. */
public final class EstateData extends SavedData {
    public static final int UNIT = 16, MAX_PARCELS = 512, MAX_SIDE = 64;
    public static final UUID NONE = new UUID(0, 0);
    public record Estate(UUID id, UUID owner, String dimension, String name) {}
    public record Parcel(UUID id, UUID estate, int minX, int minZ, int maxX, int maxZ) {
        public boolean contains(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
        public boolean overlaps(Parcel p) { return minX <= p.maxX && maxX >= p.minX && minZ <= p.maxZ && maxZ >= p.minZ; }
    }
    private final Map<UUID, Estate> estates = new LinkedHashMap<>();
    private final Map<UUID, Parcel> parcels = new LinkedHashMap<>();
    private long revision;
    public static EstateData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EstateData::load, EstateData::new, "homesystems_estates");
    }
    public long revision() { return revision; }
    public Collection<Estate> estates() { return List.copyOf(estates.values()); }
    public Collection<Parcel> parcels() { return List.copyOf(parcels.values()); }
    public Estate estate(UUID id) { return estates.get(id); }
    public Parcel parcel(UUID id) { return parcels.get(id); }
    public Parcel at(String dimension, int x, int z) {
        for (Parcel p : parcels.values()) {
            Estate estate = estates.get(p.estate);
            if (estate != null && estate.dimension.equals(dimension) && p.contains(x, z)) return p;
        }
        return null;
    }
    public UUID atBlock(String dimension, BlockPos pos) {
        Parcel p = at(dimension, Math.floorDiv(pos.getX(), UNIT), Math.floorDiv(pos.getZ(), UNIT));
        return p == null ? null : p.estate;
    }
    public boolean same(String dimA, BlockPos a, String dimB, BlockPos b) {
        UUID first = atBlock(dimA, a);
        return first != null && dimA.equals(dimB) && first.equals(atBlock(dimB, b));
    }
    public static boolean inside(DeviceBlockEntity device) {
        return device.getLevel() instanceof ServerLevel level && get(level.getServer()).atBlock(level.dimension().location().toString(), device.getBlockPos()) != null;
    }
    public static boolean same(DeviceBlockEntity a, DeviceBlockEntity b) {
        return a != null && b != null && a.getLevel() instanceof ServerLevel first && b.getLevel() instanceof ServerLevel second
            && get(first.getServer()).same(first.dimension().location().toString(), a.getBlockPos(), second.dimension().location().toString(), b.getBlockPos());
    }
    public static boolean playerMayUse(net.minecraft.server.level.ServerPlayer player, DeviceBlockEntity device) {
        return device.getLevel() instanceof ServerLevel level && get(player.server).same(player.level().dimension().location().toString(),
            player.blockPosition(), level.dimension().location().toString(), device.getBlockPos());
    }
    public static boolean editable(Estate estate, UUID actor, boolean admin) { return estate != null && (admin || estate.owner.equals(actor)); }
    private static String name(String value) {
        String result = value.strip();
        return !result.isEmpty() && result.length() <= 32 && result.codePoints().noneMatch(Character::isISOControl) ? result : null;
    }
    private void changed() { revision++; setDirty(); }

    public String create(UUID actor, String dimension, String title, int x1, int z1, int x2, int z2) {
        String clean = name(title);
        if (clean == null) return "bad_name";
        int minX = Math.min(x1, x2), minZ = Math.min(z1, z2), maxX = Math.max(x1, x2), maxZ = Math.max(z1, z2);
        if (Math.abs((long)minX * UNIT) > 29999000 || Math.abs((long)maxX * UNIT) > 29999000
            || Math.abs((long)minZ * UNIT) > 29999000 || Math.abs((long)maxZ * UNIT) > 29999000
            || (long)maxX - minX >= MAX_SIDE || (long)maxZ - minZ >= MAX_SIDE || parcels.size() >= MAX_PARCELS) return "too_large";
        UUID id = UUID.randomUUID();
        Parcel candidate = new Parcel(UUID.randomUUID(), id, minX, minZ, maxX, maxZ);
        for (Parcel existing : parcels.values()) if (estates.get(existing.estate).dimension.equals(dimension) && candidate.overlaps(existing)) return "overlap";
        estates.put(id, new Estate(id, actor, dimension, clean)); parcels.put(candidate.id, candidate); changed();
        return "created";
    }
    public String rename(UUID actor, boolean admin, UUID parcel, String title) {
        Parcel p = parcels.get(parcel); Estate e = p == null ? null : estates.get(p.estate);
        if (!editable(e, actor, admin)) return "not_owner";
        String clean = name(title); if (clean == null) return "bad_name";
        estates.put(e.id, new Estate(e.id, e.owner, e.dimension, clean)); changed(); return "renamed";
    }
    public String merge(UUID actor, boolean admin, UUID first, UUID second) {
        Parcel a = parcels.get(first), b = parcels.get(second);
        Estate x = a == null ? null : estates.get(a.estate), y = b == null ? null : estates.get(b.estate);
        if (!editable(x, actor, admin) || !editable(y, actor, admin)) return "not_owner";
        if (x.id.equals(y.id) || !x.dimension.equals(y.dimension)) return "different_estates";
        parcels.replaceAll((id, p) -> p.estate.equals(y.id) ? new Parcel(p.id, x.id, p.minX, p.minZ, p.maxX, p.maxZ) : p);
        estates.remove(y.id); changed(); return "merged";
    }
    public String split(UUID actor, boolean admin, UUID selected) {
        Parcel p = parcels.get(selected); Estate e = p == null ? null : estates.get(p.estate);
        if (!editable(e, actor, admin)) return "not_owner";
        if (parcels.values().stream().filter(part -> part.estate.equals(e.id)).count() < 2) return "single_parcel";
        UUID id = UUID.randomUUID();
        String title = e.name.substring(0, Math.min(28, e.name.length())) + " #2";
        estates.put(id, new Estate(id, e.owner, e.dimension, title));
        parcels.put(p.id, new Parcel(p.id, id, p.minX, p.minZ, p.maxX, p.maxZ)); changed(); return "split";
    }
    public String delete(UUID actor, boolean admin, UUID selected) {
        Parcel p = parcels.get(selected); Estate e = p == null ? null : estates.get(p.estate);
        if (!editable(e, actor, admin)) return "not_owner";
        parcels.remove(p.id);
        if (parcels.values().stream().noneMatch(part -> part.estate.equals(e.id))) estates.remove(e.id);
        changed(); return "deleted";
    }
    public boolean boundary(String dim, int x, int z, int dx, int dz) {
        Parcel a = at(dim, x, z), b = at(dim, x + dx, z + dz);
        return a != null && (b == null || !a.estate.equals(b.estate));
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag groups = new ListTag(), parts = new ListTag();
        for (Estate e : estates.values()) {
            CompoundTag t = new CompoundTag(); t.putUUID("Id", e.id); t.putUUID("Owner", e.owner); t.putString("Dimension", e.dimension); t.putString("Name", e.name); groups.add(t);
        }
        for (Parcel p : parcels.values()) {
            CompoundTag t = new CompoundTag(); t.putUUID("Id", p.id); t.putUUID("Estate", p.estate); t.putIntArray("Rect", new int[]{p.minX, p.minZ, p.maxX, p.maxZ}); parts.add(t);
        }
        tag.put("Estates", groups); tag.put("Parcels", parts); return tag;
    }
    public static EstateData load(CompoundTag tag) {
        EstateData data = new EstateData();
        for (Tag value : tag.getList("Estates", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)value;
            if (data.estates.size() < MAX_PARCELS && t.hasUUID("Id") && t.hasUUID("Owner") && name(t.getString("Name")) != null)
                data.estates.put(t.getUUID("Id"), new Estate(t.getUUID("Id"), t.getUUID("Owner"), t.getString("Dimension"), t.getString("Name")));
        }
        for (Tag value : tag.getList("Parcels", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag)value; int[] r = t.getIntArray("Rect");
            if (data.parcels.size() < MAX_PARCELS && t.hasUUID("Id") && t.hasUUID("Estate") && data.estates.containsKey(t.getUUID("Estate")) && r.length == 4
                && r[0] <= r[2] && r[1] <= r[3] && (long)r[2] - r[0] < MAX_SIDE && (long)r[3] - r[1] < MAX_SIDE)
                data.parcels.put(t.getUUID("Id"), new Parcel(t.getUUID("Id"), t.getUUID("Estate"), r[0], r[1], r[2], r[3]));
        }
        return data;
    }
}
