package kr.mchoe.homesystems;

/** Shared server-side address, jurisdiction and device-role rules. */
public final class DeviceRouting {
    public static boolean valid(int value) { return value >= 0 && value <= 9999; }
    public static boolean guard(DeviceBlockEntity d) { return d.getBlockState().is(HomeSystems.GUARD_STATION.get()); }
    public static boolean lobby(DeviceBlockEntity d) { return d.getBlockState().is(HomeSystems.LOBBY_PHONE.get()); }
    public static boolean indoor(DeviceBlockEntity d) {
        return d.getBlockState().is(HomeSystems.VIDEO_PHONE.get()) || d.getBlockState().is(HomeSystems.WALLPAD.get()) || d.getBlockState().is(HomeSystems.INTERPHONE.get());
    }
    public static boolean door(DeviceBlockEntity d) {
        return d.getBlockState().is(HomeSystems.DOOR_CAMERA.get()) || d.getBlockState().is(HomeSystems.DOOR_PHONE.get());
    }
    public static boolean videoSource(DeviceBlockEntity d) {
        return !d.getBlockState().is(HomeSystems.DOOR_PHONE.get()) && !d.getBlockState().is(HomeSystems.INTERPHONE.get());
    }
    public static boolean entrance(DeviceBlockEntity d) { return lobby(d) || door(d); }
    public static boolean sameHome(DeviceBlockEntity a, DeviceBlockEntity b) {
        return a.buildingNumber() == b.buildingNumber() && a.unitNumber() == b.unitNumber();
    }
    public static boolean allowed(DeviceBlockEntity caller, DeviceBlockEntity receiver) {
        if (caller == null || receiver == null || caller == receiver || !EstateData.same(caller, receiver)) return false;
        // Preserve old saved 1.2 device-to-device calls until their owner assigns a structured address.
        if (!caller.addressConfigured() && !receiver.addressConfigured()) return !caller.legacyNumber().isEmpty() && !receiver.legacyNumber().isEmpty();
        if (!caller.addressConfigured() || !receiver.addressConfigured()) return false;
        // Alternate offices are legal only while every assigned office is explicitly Away/Out.
        if (guard(caller) || guard(receiver)) {
            if (guard(caller) && caller.guardPresence()!=DeviceBlockEntity.GUARD_PRESENT || guard(receiver) && receiver.guardPresence()!=DeviceBlockEntity.GUARD_PRESENT) return false;
            if (caller.guardNumber()==receiver.guardNumber()) return true;
            var resident=guard(caller)?receiver:caller;
            return resident.getLevel() instanceof net.minecraft.server.level.ServerLevel level && DeviceDirectory.get(level.getServer()).canBypass(resident);
        }
        if (caller.guardNumber() != receiver.guardNumber()) return false;
        if (door(caller)) return indoor(receiver) && sameHome(caller, receiver) && caller.links().isLinked(receiver);
        if (door(receiver)) return indoor(caller) && sameHome(caller, receiver) && receiver.links().isLinked(caller);
        if (lobby(caller)) return (caller.buildingNumber() == 9999 || caller.buildingNumber() == receiver.buildingNumber());
        if (lobby(receiver)) return receiver.buildingNumber() == 9999 || receiver.buildingNumber() == caller.buildingNumber();
        return indoor(caller) && indoor(receiver);
    }
}
