package kr.mchoe.homesystems;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DeviceNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(HomeSystems.MODID, "devices"), () -> "6", "6"::equals, "6"::equals);
    public enum Action { CALL, CALL_GUARD, CALL_OTHER_GUARD, ANSWER, END, UNLOCK, HOME_LIGHTS, HOME_DOOR, HOME_GAS, HOME_OTHER, GUARD_PRESENT, GUARD_AWAY, GUARD_OUT }
    public record Request(int containerId, Action action, int building, int unit, int guard) {
        public static void encode(Request request, FriendlyByteBuf buffer) {
            buffer.writeVarInt(request.containerId).writeEnum(request.action).writeVarInt(request.building).writeVarInt(request.unit).writeVarInt(request.guard);
        }
        public static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readVarInt(), buffer.readEnum(Action.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
        public static void handle(Request request, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null || !(player.containerMenu instanceof DeviceMenu menu)
                    || menu.containerId != request.containerId || !menu.stillValid(player)) return;
                menu.device().handle(player, request);
            });
            context.setPacketHandled(true);
        }
    }
    public record AddressRequest(int containerId, int guard, int building, int unit) {
        static void encode(AddressRequest r, FriendlyByteBuf b) { b.writeVarInt(r.containerId).writeVarInt(r.guard).writeVarInt(r.building).writeVarInt(r.unit); }
        static AddressRequest decode(FriendlyByteBuf b) { return new AddressRequest(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()); }
        static void handle(AddressRequest request, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null || !(player.containerMenu instanceof DeviceMenu menu) || menu.containerId != request.containerId || !menu.stillValid(player)) return;
                menu.device().saveAddress(player, request.guard, request.building, request.unit);
            });context.setPacketHandled(true);
        }
    }
    public static void register() {
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(Request::encode).decoder(Request::decode).consumerNetworkThread(Request::handle).add();
        CHANNEL.messageBuilder(VideoFrame.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(VideoFrame::encode).decoder(VideoFrame::decode).consumerNetworkThread(VideoFrame::handle).add();
        CHANNEL.messageBuilder(DashboardNetwork.Request.class, 2, NetworkDirection.PLAY_TO_SERVER)
            .encoder(DashboardNetwork.Request::encode).decoder(DashboardNetwork.Request::decode).consumerNetworkThread(DashboardNetwork.Request::handle).add();
        CHANNEL.messageBuilder(DashboardNetwork.State.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(DashboardNetwork.State::encode).decoder(DashboardNetwork.State::decode).consumerNetworkThread(DashboardNetwork.State::handle).add();
        CHANNEL.messageBuilder(AddressRequest.class, 4, NetworkDirection.PLAY_TO_SERVER)
            .encoder(AddressRequest::encode).decoder(AddressRequest::decode).consumerNetworkThread(AddressRequest::handle).add();
    }
}
