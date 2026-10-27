package kr.mchoe.homesystems;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Same-estate viewers near a physical display receive the same authorized scene as its GUI. */
public record WorldVideoFrame(BlockPos display,VideoFrame frame){
    static void encode(WorldVideoFrame frame,FriendlyByteBuf buffer){buffer.writeBlockPos(frame.display);VideoFrame.encode(frame.frame,buffer);}
    static WorldVideoFrame decode(FriendlyByteBuf buffer){return new WorldVideoFrame(buffer.readBlockPos(),VideoFrame.decode(buffer));}
    static void handle(WorldVideoFrame frame,Supplier<NetworkEvent.Context> supplier){var context=supplier.get();context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->kr.mchoe.homesystems.client.WorldCameras.receive(frame)));context.setPacketHandled(true);}
}
