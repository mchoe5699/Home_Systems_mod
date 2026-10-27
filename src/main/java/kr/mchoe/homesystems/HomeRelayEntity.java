package kr.mchoe.homesystems;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class HomeRelayEntity extends BlockEntity {
    private UUID owner, networkId;
    private BlockPos controllerPos;
    private long lastEstateRevision = -1;
    public HomeRelayEntity(BlockPos pos, BlockState state) { super(HomeSystems.RELAY.get(), pos, state); }
    public int channel() { return ((HomeRelayBlock)getBlockState().getBlock()).channel(); }
    public void setOwner(UUID owner) { this.owner = owner; setChanged(); }
    public boolean canConfigure(Player player) { return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2); }
    public boolean linked() { return controllerPos != null && networkId != null; }
    public boolean matches(DeviceBlockEntity device) {
        return linked() && device.getLevel() == level && device.getBlockPos().equals(controllerPos) && device.homeNetwork().id().equals(networkId);
    }
    private DeviceBlockEntity controller() {
        if (level == null || !linked() || !level.hasChunkAt(controllerPos)) return null;
        return level.getBlockEntity(controllerPos) instanceof DeviceBlockEntity device && matches(device) ? device : null;
    }
    public void bind(DeviceBlockEntity device) {
        controllerPos = device.getBlockPos().immutable();
        networkId = device.homeNetwork().id();
        setChanged();
        setPowered(device.homeNetwork().enabled(channel()));
    }
    public void unlink() { unlink(true); }
    public void unlink(boolean powerOff) {
        DeviceBlockEntity controller = controller();
        if (controller != null) controller.homeNetwork().remove(worldPosition);
        controllerPos = null; networkId = null;
        setChanged();
        if (powerOff) setPowered(false);
    }
    public void setPowered(boolean powered) {
        if (level == null || level.isClientSide || !(getBlockState().getBlock() instanceof HomeRelayBlock block)) return;
        if (powered) {
            DeviceBlockEntity controller = controller();
            if (controller == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !EstateData.get(serverLevel.getServer()).same(level.dimension().location().toString(), worldPosition, level.dimension().location().toString(), controller.getBlockPos())) powered = false;
        }
        if (getBlockState().getValue(HomeRelayBlock.POWERED) == powered) return;
        level.setBlock(worldPosition, getBlockState().setValue(HomeRelayBlock.POWERED, powered), 3);
        level.updateNeighborsAt(worldPosition, block);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, HomeRelayEntity relay) {
        long revision = EstateData.get(((net.minecraft.server.level.ServerLevel)level).getServer()).revision();
        if (level.getGameTime() % 20 != 0 && relay.lastEstateRevision == revision) return;
        relay.lastEstateRevision = revision;
        if (!relay.linked()) { relay.setPowered(false); return; }
        // An unloaded controller stops output but retains the pairing for its return.
        if (!level.hasChunkAt(relay.controllerPos)) { relay.setPowered(false); return; }
        DeviceBlockEntity controller = relay.controller();
        if (controller == null || !controller.homeNetwork().isWallpad() || !controller.homeNetwork().contains(pos, relay.channel())) {
            relay.unlink(); return;
        }
        relay.setPowered(controller.homeNetwork().enabled(relay.channel()));
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        if (linked()) { tag.putLong("ControllerPos", controllerPos.asLong()); tag.putUUID("NetworkId", networkId); }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        networkId = tag.hasUUID("NetworkId") ? tag.getUUID("NetworkId") : null;
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
    }
}
