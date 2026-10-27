package kr.mchoe.homesystems;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class DashboardMenu extends AbstractContainerMenu {
    long lastAction = -40;
    public DashboardMenu(int id, Inventory inventory) { super(HomeSystems.DASHBOARD_MENU.get(), id); }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.getMainHandItem().is(HomeSystems.ESTATE_DASHBOARD.get()) || player.getOffhandItem().is(HomeSystems.ESTATE_DASHBOARD.get());
    }
}
