package kr.mchoe.homesystems;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class SiblingSelectorItem extends Item {
    public SiblingSelectorItem(Properties properties) { super(properties); }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) { return useOn(context); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof DeviceBlockEntity device)) return InteractionResult.PASS;
        if (context.getPlayer() instanceof ServerPlayer player) {
            String error = !EstateData.playerMayUse(player,device) ? "outside_estate" : !device.canConfigure(player) ? "owner_only" : device.state()!=DeviceBlockEntity.IDLE ? "sibling_busy" : SiblingModels.options(device.getBlockState().getBlock()).size()<2 ? "no_siblings" : null;
            if (error != null) player.displayClientMessage(Component.translatable("message.homesystems."+error), true);
            else {
                var options = SiblingModels.options(device.getBlockState().getBlock());
                NetworkHooks.openScreen(player, new SimpleMenuProvider((id,inventory,p)->new SiblingMenu(id,inventory,pos,options), Component.translatable("screen.homesystems.siblings")), buffer -> {
                    buffer.writeBlockPos(pos); buffer.writeVarInt(options.size());
                    for (var option:options) { buffer.writeVarInt(option.slot()); buffer.writeComponent(option.name()); }
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flags) { lines.add(Component.translatable("tooltip.homesystems.sibling_selector")); }
}
