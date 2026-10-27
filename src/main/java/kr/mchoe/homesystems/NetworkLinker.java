package kr.mchoe.homesystems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class NetworkLinker extends Item {
    public NetworkLinker(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel(); var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var entity = level.getBlockEntity(context.getClickedPos());
        String result;
        if (entity instanceof DeviceBlockEntity device && device.homeNetwork().isWallpad()) {
            if (!EstateData.inside(device)) result = "outside_estate";
            else if (!device.canConfigure(player)) result = "owner_only";
            else {
                CompoundTag tag = context.getItemInHand().getOrCreateTag();
                tag.putString("Dimension", level.dimension().location().toString());
                tag.putLong("WallpadPos", device.getBlockPos().asLong());
                tag.putUUID("NetworkId", device.homeNetwork().id());
                result = "wallpad_selected";
            }
        } else if (entity instanceof HomeRelayEntity relay) {
            if (!relay.canConfigure(player)) result = "owner_only";
            else if (player.isShiftKeyDown()) { relay.unlink(); result = "relay_unlinked"; }
            else {
                CompoundTag tag = context.getItemInHand().getTag();
                if (tag == null || !tag.contains("WallpadPos") || !tag.hasUUID("NetworkId")) result = "select_wallpad";
                else if (!level.dimension().location().toString().equals(tag.getString("Dimension"))) result = "network_range";
                else {
                    BlockPos pos = BlockPos.of(tag.getLong("WallpadPos"));
                    if (!level.hasChunkAt(pos)) result = "wallpad_offline";
                    else if (!(level.getBlockEntity(pos) instanceof DeviceBlockEntity device)
                        || !device.homeNetwork().isWallpad() || !device.homeNetwork().id().equals(tag.getUUID("NetworkId"))) result = "select_wallpad";
                    else if (!device.canConfigure(player)) result = "owner_only";
                    else if (device.homeNetwork().add(relay)) result = "relay_linked";
                    else result = "network_range";
                }
            }
        } else result = "linker_hint";
        player.displayClientMessage(Component.translatable("message.homesystems." + result), true);
        return InteractionResult.CONSUME;
    }
}
