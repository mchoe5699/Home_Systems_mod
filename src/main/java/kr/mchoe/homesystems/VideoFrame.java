package kr.mchoe.homesystems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server-authorized, bounded camera scene. No client-selected remote coordinates. */
public record VideoFrame(int containerId, Direction facing, long time, List<Cell> blocks, List<Actor> actors) {
    public static final int RADIUS = 8, MIN_Y = -3, MAX_Y = 5, MAX_BLOCKS = 2601, MAX_ACTORS = 32;
    public record Cell(BlockPos pos, int state) {}
    public record Actor(String type, UUID uuid, String name, float x, float y, float z, float yaw, float pitch, boolean crouching) {}

    public static void encode(VideoFrame frame, FriendlyByteBuf buffer) {
        buffer.writeVarInt(frame.containerId).writeEnum(frame.facing).writeLong(frame.time);
        buffer.writeVarInt(frame.blocks.size());
        for (Cell cell : frame.blocks) {
            buffer.writeByte(cell.pos.getX()).writeByte(cell.pos.getY()).writeByte(cell.pos.getZ());
            buffer.writeVarInt(cell.state);
        }
        buffer.writeVarInt(frame.actors.size());
        for (Actor actor : frame.actors) {
            buffer.writeUtf(actor.type, 128).writeUUID(actor.uuid).writeUtf(actor.name, 32)
                .writeFloat(actor.x).writeFloat(actor.y).writeFloat(actor.z).writeFloat(actor.yaw).writeFloat(actor.pitch).writeBoolean(actor.crouching);
        }
    }

    public static VideoFrame decode(FriendlyByteBuf buffer) {
        int container = buffer.readVarInt();
        Direction facing = buffer.readEnum(Direction.class);
        long time = buffer.readLong();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_BLOCKS || !facing.getAxis().isHorizontal()) throw new IllegalArgumentException("Invalid camera frame");
        List<Cell> blocks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = new BlockPos(buffer.readByte(), buffer.readByte(), buffer.readByte());
            int state = buffer.readVarInt();
            if (Math.abs(pos.getX()) > RADIUS || Math.abs(pos.getZ()) > RADIUS || pos.getY() < MIN_Y || pos.getY() > MAX_Y || state < 0)
                throw new IllegalArgumentException("Invalid camera cell");
            blocks.add(new Cell(pos, state));
        }
        count = buffer.readVarInt();
        if (count < 0 || count > MAX_ACTORS) throw new IllegalArgumentException("Invalid camera actor count");
        List<Actor> actors = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Actor actor = new Actor(buffer.readUtf(128), buffer.readUUID(), buffer.readUtf(32), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
            if (!Float.isFinite(actor.x) || !Float.isFinite(actor.y) || !Float.isFinite(actor.z)
                || !Float.isFinite(actor.yaw) || !Float.isFinite(actor.pitch) || Math.abs(actor.x) > 10 || Math.abs(actor.z) > 10
                || actor.y < MIN_Y - 2 || actor.y > MAX_Y + 2) throw new IllegalArgumentException("Invalid camera actor");
            actors.add(actor);
        }
        return new VideoFrame(container, facing, time, List.copyOf(blocks), List.copyOf(actors));
    }

    public static void handle(VideoFrame frame, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> kr.mchoe.homesystems.client.CameraView.receive(frame)));
        context.setPacketHandled(true);
    }
}
