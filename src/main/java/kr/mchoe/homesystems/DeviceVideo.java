package kr.mchoe.homesystems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

public final class DeviceVideo {
    public static boolean hasDisplay(DeviceBlockEntity device) {
        var state = device.getBlockState();
        return device.isSht308() || state.is(HomeSystems.VIDEO_PHONE.get()) || state.is(HomeSystems.WALLPAD.get()) || state.is(HomeSystems.GUARD_STATION.get());
    }

    public static boolean canView(DeviceBlockEntity display) {
        return display.operational() && (!display.isSht308() || display.monitoring() || display.peerIsEntrance()) && hasDisplay(display) && (display.monitoring() || display.peerHasVideo() && (display.state() == DeviceBlockEntity.RINGING || display.state() == DeviceBlockEntity.CONNECTED));
    }

    public static VideoFrame capture(DeviceBlockEntity source, int containerId) {
        ServerLevel level = (ServerLevel) source.getLevel();
        BlockPos origin = source.getBlockPos();
        EstateData estates = EstateData.get(level.getServer());
        String dimension = level.dimension().location().toString();
        var estateId = estates.atBlock(dimension, origin);
        java.util.Map<Long, Boolean> visibleChunks = new java.util.HashMap<>();
        List<VideoFrame.Cell> blocks = new ArrayList<>();
        for (int x = -VideoFrame.RADIUS; x <= VideoFrame.RADIUS; x++) {
            for (int y = VideoFrame.MIN_Y; y <= VideoFrame.MAX_Y; y++) {
                for (int z = -VideoFrame.RADIUS; z <= VideoFrame.RADIUS; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    int chunkX = Math.floorDiv(pos.getX(), EstateData.UNIT), chunkZ = Math.floorDiv(pos.getZ(), EstateData.UNIT);
                    long key = ((long)chunkX << 32) ^ (chunkZ & 0xffffffffL);
                    if (!visibleChunks.computeIfAbsent(key, ignored -> {
                        var parcel = estates.at(dimension, chunkX, chunkZ);
                        return parcel != null && parcel.estate().equals(estateId);
                    })) continue;
                    if (!level.hasChunkAt(pos)) continue;
                    var state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    // Omit fully enclosed blocks; the visible surfaces remain actual world states.
                    boolean visible = false;
                    for (var direction : net.minecraft.core.Direction.values()) {
                        BlockPos adjacent = pos.relative(direction);
                        if (!level.hasChunkAt(adjacent) || !level.getBlockState(adjacent).canOcclude()) { visible = true; break; }
                    }
                    if (visible) blocks.add(new VideoFrame.Cell(new BlockPos(x, y, z), Block.getId(state)));
                }
            }
        }
        List<VideoFrame.Actor> actors = new ArrayList<>();
        AABB bounds = new AABB(origin.offset(-8, -3, -8), origin.offset(9, 6, 9));
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds)) {
            if (actors.size() >= VideoFrame.MAX_ACTORS) break;
            if (entity.isInvisible() || entity.isSpectator()) continue;
            if (!estates.same(dimension, origin, dimension, entity.blockPosition())) continue;
            if (!bounds.contains(entity.position())) continue;
            actors.add(new VideoFrame.Actor(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), entity.getUUID(),
                entity instanceof Player player ? player.getGameProfile().getName() : "", (float)(entity.getX() - origin.getX()),
                (float)(entity.getY() - origin.getY()), (float)(entity.getZ() - origin.getZ()), entity.getYRot(), entity.getXRot(), entity.isCrouching()));
        }
        return new VideoFrame(containerId, source.getBlockState().getValue(DeviceBlock.FACING), level.getGameTime(), List.copyOf(blocks), List.copyOf(actors));
    }

    public static void tick(DeviceBlockEntity display) {
        if (!canView(display) || !(display.getLevel() instanceof ServerLevel level)) return;
        DeviceBlockEntity source = display.monitoring() ? display.monitorSource() : display.peer();
        if (source == null || !DeviceRouting.videoSource(source) || !DeviceRouting.allowed(source, display)) return;
        var viewers = level.players().stream().filter(player -> EstateData.playerMayUse(player,display)
            && player.distanceToSqr(display.getBlockPos().getX()+.5,display.getBlockPos().getY()+.5,display.getBlockPos().getZ()+.5)<=256).toList();
        if(viewers.isEmpty())return;
        VideoFrame frame=capture(source,0);
        for(var viewer:viewers){
            DeviceNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new WorldVideoFrame(display.getBlockPos(),frame));
            if(viewer.containerMenu instanceof DeviceMenu menu&&menu.device()==display&&menu.stillValid(viewer))
                DeviceNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->viewer),new VideoFrame(menu.containerId,frame.facing(),frame.time(),frame.blocks(),frame.actors()));
        }
    }
}
