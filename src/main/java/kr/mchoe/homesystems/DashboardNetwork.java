package kr.mchoe.homesystems;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class DashboardNetwork {
    public static final int WIDTH = 64, HEIGHT = 48;
    public enum Action { REFRESH, CREATE, RENAME, MERGE, SPLIT, DELETE }
    public record Request(int container, Action action, int centerX, int centerZ, int x1, int z1, int x2, int z2, UUID first, UUID second, String name) {
        static void encode(Request r, FriendlyByteBuf b) {
            b.writeVarInt(r.container).writeEnum(r.action); b.writeInt(r.centerX).writeInt(r.centerZ).writeInt(r.x1).writeInt(r.z1).writeInt(r.x2).writeInt(r.z2);
            b.writeUUID(r.first).writeUUID(r.second).writeUtf(r.name, 64);
        }
        static Request decode(FriendlyByteBuf b) {
            return new Request(b.readVarInt(), b.readEnum(Action.class), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readUUID(), b.readUUID(), b.readUtf(64));
        }
        static void handle(Request request, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.containerMenu instanceof DashboardMenu menu) || menu.containerId != request.container || !menu.stillValid(player)) return;
                long now = player.level().getGameTime();
                if (now - menu.lastAction < 4) return;
                menu.lastAction = now;
                if (Math.abs((long)request.centerX * EstateData.UNIT) > 29999000 || Math.abs((long)request.centerZ * EstateData.UNIT) > 29999000) return;
                EstateData data = EstateData.get(player.server); UUID actor = player.getUUID(); boolean admin = player.hasPermissions(2);
                String result = switch (request.action) {
                    case REFRESH -> "";
                    case CREATE -> data.create(actor, player.level().dimension().location().toString(), request.name, request.x1, request.z1, request.x2, request.z2);
                    case RENAME -> data.rename(actor, admin, request.first, request.name);
                    case MERGE -> data.merge(actor, admin, request.first, request.second);
                    case SPLIT -> data.split(actor, admin, request.first);
                    case DELETE -> data.delete(actor, admin, request.first);
                };
                send(player, request.centerX, request.centerZ, result);
            });
            context.setPacketHandled(true);
        }
    }
    public record Group(UUID id, String name, boolean editable) {}
    public record State(int container, String dimension, int centerX, int centerZ, int[] colors, List<Group> groups, List<EstateData.Parcel> parcels, String status) {
        static void encode(State s, FriendlyByteBuf b) {
            b.writeVarInt(s.container).writeUtf(s.dimension, 128); b.writeInt(s.centerX).writeInt(s.centerZ); b.writeVarIntArray(s.colors);
            b.writeVarInt(s.groups.size());
            for (Group g : s.groups) b.writeUUID(g.id).writeUtf(g.name, 64).writeBoolean(g.editable);
            b.writeVarInt(s.parcels.size());
            for (EstateData.Parcel p : s.parcels) { b.writeUUID(p.id()).writeUUID(p.estate()); b.writeInt(p.minX()).writeInt(p.minZ()).writeInt(p.maxX()).writeInt(p.maxZ()); }
            b.writeUtf(s.status, 64);
        }
        static State decode(FriendlyByteBuf b) {
            int container = b.readVarInt(); String dimension = b.readUtf(128); int x = b.readInt(), z = b.readInt();
            int[] colors = b.readVarIntArray(WIDTH * HEIGHT);
            if (colors.length != WIDTH * HEIGHT) throw new IllegalArgumentException("Bad terrain map size");
            int count = b.readVarInt(); if (count < 0 || count > EstateData.MAX_PARCELS) throw new IllegalArgumentException("Bad estate count");
            List<Group> groups = new ArrayList<>(); for (int i = 0; i < count; i++) groups.add(new Group(b.readUUID(), b.readUtf(64), b.readBoolean()));
            count = b.readVarInt(); if (count < 0 || count > EstateData.MAX_PARCELS) throw new IllegalArgumentException("Bad parcel count");
            List<EstateData.Parcel> parcels = new ArrayList<>();
            for (int i = 0; i < count; i++) parcels.add(new EstateData.Parcel(b.readUUID(), b.readUUID(), b.readInt(), b.readInt(), b.readInt(), b.readInt()));
            return new State(container, dimension, x, z, colors, List.copyOf(groups), List.copyOf(parcels), b.readUtf(64));
        }
        static void handle(State state, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> kr.mchoe.homesystems.client.DashboardScreen.receive(state)));
            context.setPacketHandled(true);
        }
    }
    static void send(ServerPlayer player, int centerX, int centerZ, String result) {
        var level = player.serverLevel(); int[] colors = new int[WIDTH * HEIGHT];
        for (int z = 0; z < HEIGHT; z++) for (int x = 0; x < WIDTH; x++) {
            int worldX = (centerX + x - WIDTH / 2) * EstateData.UNIT + EstateData.UNIT / 2;
            int worldZ = (centerZ + z - HEIGHT / 2) * EstateData.UNIT + EstateData.UNIT / 2;
            if (!level.hasChunkAt(new BlockPos(worldX, 0, worldZ))) { colors[z * WIDTH + x] = 0x192833; continue; }
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
            BlockPos pos = new BlockPos(worldX, y, worldZ);
            colors[z * WIDTH + x] = level.getBlockState(pos).getMapColor(level, pos).col;
        }
        EstateData data = EstateData.get(player.server); String dimension = level.dimension().location().toString();
        List<Group> groups = data.estates().stream().filter(e -> e.dimension().equals(dimension))
            .map(e -> new Group(e.id(), e.name(), EstateData.editable(e, player.getUUID(), player.hasPermissions(2)))).toList();
        Set<UUID> ids = new HashSet<>(); groups.forEach(g -> ids.add(g.id));
        var parcels = data.parcels().stream().filter(p -> ids.contains(p.estate())).toList();
        DeviceNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(player.containerMenu.containerId, dimension, centerX, centerZ, colors, groups, parcels, result));
    }
}
