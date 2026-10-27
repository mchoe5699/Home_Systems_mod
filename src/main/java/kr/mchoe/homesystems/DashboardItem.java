package kr.mchoe.homesystems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class DashboardItem extends Item {
    public DashboardItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) NetworkHooks.openScreen(serverPlayer,
            new SimpleMenuProvider((id, inventory, ignored) -> new DashboardMenu(id, inventory), Component.translatable("item.homesystems.estate_dashboard")));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
