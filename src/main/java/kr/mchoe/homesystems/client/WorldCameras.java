package kr.mchoe.homesystems.client;

import java.util.*;
import kr.mchoe.homesystems.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=HomeSystems.MODID,value=Dist.CLIENT)
public final class WorldCameras {
    private static final Map<BlockPos,CameraView> cameras=new LinkedHashMap<>();
    private static final Map<BlockPos,ResourceLocation> textures=new HashMap<>();
    private static Object currentLevel;
    public static void receive(WorldVideoFrame packet){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        if(currentLevel!=mc.level){cameras.values().forEach(CameraView::close);cameras.clear();textures.clear();currentLevel=mc.level;}
        if(!(mc.level.getBlockEntity(packet.display()) instanceof DeviceBlockEntity d)||!DeviceVideo.canView(d))return;
        if(cameras.size()>=8&&!cameras.containsKey(packet.display())){var first=cameras.keySet().iterator().next();cameras.remove(first).close();textures.remove(first);}
        cameras.computeIfAbsent(packet.display(),p->new CameraView()).accept(packet.frame());
    }
    public static ResourceLocation texture(DeviceBlockEntity device){var camera=cameras.get(device.getBlockPos());return DeviceVideo.canView(device)&&camera!=null&&camera.hasFrame()?textures.get(device.getBlockPos()):null;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_SKY)return;
        var mc=Minecraft.getInstance();
        var iterator=cameras.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next();var block=mc.level==null?null:mc.level.getBlockEntity(entry.getKey());
            if(!(block instanceof DeviceBlockEntity device)||!DeviceVideo.canView(device)||!entry.getValue().hasFrame()){
                entry.getValue().close();textures.remove(entry.getKey());iterator.remove();continue;
            }
            entry.getValue().tone(device.monochrome(),device.isSht308()?device.sht308().brightness():1,device.isSht308()?device.sht308().contrast():1);
            var texture=entry.getValue().worldTexture();if(texture!=null)textures.put(entry.getKey(),texture);
        }
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.enableCull();
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){cameras.values().forEach(CameraView::close);cameras.clear();textures.clear();currentLevel=null;}
}
