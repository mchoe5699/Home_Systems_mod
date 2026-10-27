package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DeviceMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final net.minecraft.world.level.Level level;
    public DeviceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public DeviceMenu(int id, Inventory inventory, BlockPos pos) {
        super(HomeSystems.DEVICE_MENU.get(), id);
        this.pos = pos;
        this.level = inventory.player.level();
    }
    public DeviceBlockEntity device() { return level.getBlockEntity(pos) instanceof DeviceBlockEntity value ? value : null; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        DeviceBlockEntity device = device();
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && device != null && !EstateData.playerMayUse(serverPlayer, device)) return false;
        return device != null && !device.isRemoved() && player.level() == device.getLevel()
            && player.distanceToSqr(device.getBlockPos().getX() + .5, device.getBlockPos().getY() + .5, device.getBlockPos().getZ() + .5) <= 64;
    }
}
