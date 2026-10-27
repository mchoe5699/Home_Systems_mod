package kr.mchoe.homesystems;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

public final class DeviceDirectory extends SavedData {
    private final Map<String, GlobalPos> addresses = new HashMap<>();
    private final java.util.Set<GlobalPos> devices = new java.util.LinkedHashSet<>();
    private record GuardInfo(int number, int presence, int type) {}
    private final Map<GlobalPos, GuardInfo> guardInfo = new HashMap<>();

    public static DeviceDirectory get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DeviceDirectory::load, DeviceDirectory::new, "homesystems_directory");
    }

    public static DeviceDirectory load(CompoundTag tag) {
        var directory = new DeviceDirectory();
        ListTag entries = tag.getList("Devices", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            String number = entry.getString("Number");
            if (dimension != null && validNumber(number)) directory.addresses.put(number,
                GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("Pos"))));
        }
        directory.devices.addAll(directory.addresses.values());
        for (Tag value : tag.getList("RegisteredDevices", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag)value;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension != null) directory.devices.add(GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("Pos"))));
        }
        for (Tag value : tag.getList("GuardPresence", Tag.TAG_COMPOUND)) {
            CompoundTag entry=(CompoundTag)value;ResourceLocation dimension=ResourceLocation.tryParse(entry.getString("Dimension"));
            int number=entry.getInt("Number"),presence=entry.getInt("Presence");
            if(dimension!=null&&DeviceRouting.valid(number)&&presence>=0&&presence<=2)directory.guardInfo.put(GlobalPos.of(ResourceKey.create(Registries.DIMENSION,dimension),BlockPos.of(entry.getLong("Pos"))),new GuardInfo(number,presence,entry.getInt("Type")==1?1:0));
        }
        return directory;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        addresses.forEach((number, pos) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Number", number);
            entry.putString("Dimension", pos.dimension().location().toString());
            entry.putLong("Pos", pos.pos().asLong());
            entries.add(entry);
        });
        tag.put("Devices", entries);
        ListTag registered = new ListTag();
        for (GlobalPos pos : devices) {
            CompoundTag entry = new CompoundTag();entry.putString("Dimension", pos.dimension().location().toString());entry.putLong("Pos", pos.pos().asLong());registered.add(entry);
        }
        tag.put("RegisteredDevices", registered);
        ListTag presence=new ListTag();guardInfo.forEach((pos,info)->{
            CompoundTag entry=new CompoundTag();entry.putString("Dimension",pos.dimension().location().toString());entry.putLong("Pos",pos.pos().asLong());entry.putInt("Number",info.number);entry.putInt("Presence",info.presence);entry.putInt("Type",info.type);presence.add(entry);
        });tag.put("GuardPresence",presence);
        return tag;
    }

    public static boolean validNumber(String number) { return number.matches("[0-9]{1,12}"); }
    public boolean claim(DeviceBlockEntity device, String number) {
        if (!validNumber(number)) return false;
        GlobalPos location = device.location();
        GlobalPos existing = addresses.get(number);
        if (existing != null && !existing.equals(location)) return false;
        if (!device.legacyNumber().isEmpty() && !device.legacyNumber().equals(number)) addresses.remove(device.legacyNumber(), location);
        addresses.put(number, location);
        devices.add(location);
        setDirty();
        return true;
    }

    public void remove(DeviceBlockEntity device) {
        device.endCall();
        addresses.values().removeIf(pos -> pos.equals(device.location()));
        devices.remove(device.location());setDirty();
        guardInfo.remove(device.location());
    }

    public void registerAddress(DeviceBlockEntity device) {
        addresses.values().removeIf(pos -> pos.equals(device.location()));
        devices.add(device.location());setDirty();
        if(DeviceRouting.guard(device))guardInfo.put(device.location(),new GuardInfo(device.guardNumber(),device.guardPresence(),device.communicationType()));
        else guardInfo.remove(device.location());
    }
    public DeviceBlockEntity cameraFor(DeviceBlockEntity indoor){
        if(!(indoor.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return null;
        return devices.stream().map(pos->resolve(level.getServer(),pos)).filter(java.util.Objects::nonNull)
            .filter(d->d.getBlockState().is(HomeSystems.DOOR_CAMERA.get())&&d.links().isLinked(indoor)&&DeviceRouting.allowed(d,indoor))
            .min(java.util.Comparator.comparingDouble(d->d.getBlockPos().distSqr(indoor.getBlockPos()))).orElse(null);
    }
    public boolean canBypass(DeviceBlockEntity caller) {
        if(!caller.addressConfigured()||!(caller.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return false;
        var estate=EstateData.get(level.getServer());String dim=level.dimension().location().toString();
        var own=guardInfo.entrySet().stream().filter(e->e.getValue().number==caller.guardNumber()&&e.getValue().type==caller.communicationType()
            &&estate.same(dim,caller.getBlockPos(),e.getKey().dimension().location().toString(),e.getKey().pos())).toList();
        return !own.isEmpty()&&own.stream().allMatch(e->e.getValue().presence!=DeviceBlockEntity.GUARD_PRESENT);
    }
    public void refreshPresenceClients(net.minecraft.server.MinecraftServer server) {
        for(var pos:java.util.List.copyOf(devices)){var d=resolve(server,pos);if(d!=null)d.changed();}
    }
    public java.util.List<DeviceBlockEntity> household(DeviceBlockEntity caller, int building, int unit) {
        if (!DeviceRouting.valid(building) || !DeviceRouting.valid(unit) || !(caller.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return java.util.List.of();
        var result = devices.stream().map(pos -> resolve(level.getServer(), pos)).filter(java.util.Objects::nonNull)
            .filter(d -> d != caller && d.addressConfigured() && DeviceRouting.indoor(d) && d.buildingNumber() == building && d.unitNumber() == unit && DeviceRouting.allowed(caller, d))
            .sorted(java.util.Comparator.comparingInt((DeviceBlockEntity d) -> d.guardNumber() == caller.guardNumber() ? 0 : 1).thenComparingLong(d -> d.getBlockPos().asLong())).toList();
        if (DeviceRouting.guard(caller) && result.stream().anyMatch(d -> d.guardNumber() == caller.guardNumber()))
            return result.stream().filter(d -> d.guardNumber() == caller.guardNumber()).toList();
        return result;
    }
    public java.util.List<DeviceBlockEntity> guards(DeviceBlockEntity caller, int number, boolean fallback) {
        if (!DeviceRouting.valid(number) || !(caller.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return java.util.List.of();
        boolean bypass=canBypass(caller);
        if(number!=caller.guardNumber()&&!bypass)return java.util.List.of();
        var candidates = devices.stream().map(pos -> resolve(level.getServer(), pos)).filter(java.util.Objects::nonNull)
            .filter(d -> d != caller && d.addressConfigured() && DeviceRouting.guard(d) && DeviceRouting.allowed(caller, d) && d.state() == DeviceBlockEntity.IDLE)
            .sorted(java.util.Comparator.comparingInt((DeviceBlockEntity d) -> d.guardNumber() == number ? 0 : 1).thenComparingLong(d -> d.getBlockPos().asLong())).toList();
        var preferred = candidates.stream().filter(d -> d.guardNumber() == number).toList();
        return !preferred.isEmpty() ? preferred : fallback && bypass && !candidates.isEmpty() ? candidates.stream().filter(d -> d.guardNumber() == candidates.get(0).guardNumber()).toList() : java.util.List.of();
    }

    public DeviceBlockEntity find(MinecraftServer server, String number) { return resolve(server, addresses.get(number)); }
    public static DeviceBlockEntity resolve(MinecraftServer server, GlobalPos pos) {
        if (pos == null) return null;
        var level = server.getLevel(pos.dimension());
        if (level == null || !level.hasChunkAt(pos.pos())) return null;
        return level.getBlockEntity(pos.pos()) instanceof DeviceBlockEntity device && !device.isRemoved() ? device : null;
    }

    public DeviceBlockEntity connectedNear(ServerPlayer sender) {
        DeviceBlockEntity nearest = null;
        double distance = 64;
        for (GlobalPos address : devices) {
            if (!address.dimension().equals(sender.level().dimension())) continue;
            double candidate = sender.distanceToSqr(address.pos().getX() + .5, address.pos().getY() + .5, address.pos().getZ() + .5);
            if (candidate > distance) continue;
            DeviceBlockEntity device = resolve(sender.server, address);
            if (device != null && device.state() == DeviceBlockEntity.CONNECTED && EstateData.playerMayUse(sender, device)) { nearest = device; distance = candidate; }
        }
        if (nearest == null) return null;
        DeviceBlockEntity remote = nearest.peer();
        return remote != null && DeviceRouting.allowed(nearest, remote) && remote.state() == DeviceBlockEntity.CONNECTED && remote.peer() == nearest ? nearest : null;
    }

    public boolean sendMessage(ServerPlayer sender, String message) {
        if (message.isBlank() || message.length() > 256) return false;
        DeviceBlockEntity nearest = connectedNear(sender);
        if (nearest == null) return false;
        DeviceBlockEntity remote = nearest.peer();
        Component text = Component.translatable("message.homesystems.chat", nearest.number(), sender.getDisplayName(), Component.literal(message));
        // Union the two listening areas so a nearby player receives a message only once.
        deliver(nearest, text, sender.server.getPlayerList().getPlayers());
        return true;
    }

    static int deliver(DeviceBlockEntity nearest, Component text, Iterable<ServerPlayer> recipients) {
        DeviceBlockEntity remote = nearest.peer();
        if (remote == null) return 0;
        int count = 0;
        for (var recipient : recipients) {
            if (near(recipient, nearest, 8) || near(recipient, remote, 16)) { recipient.sendSystemMessage(text); count++; }
        }
        return count;
    }

    private static boolean near(ServerPlayer player, DeviceBlockEntity device, int radius) {
        var pos = device.getBlockPos();
        return player.level() == device.getLevel() && EstateData.playerMayUse(player, device)
            && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= radius * radius;
    }
}
