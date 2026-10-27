package kr.mchoe.homesystems.client;

import kr.mchoe.homesystems.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class ProductRenderer implements BlockEntityRenderer<DeviceBlockEntity> {
    private static final ResourceLocation WHITE=new ResourceLocation(HomeSystems.MODID,"textures/block/panel_white.png");
    public ProductRenderer(BlockEntityRendererProvider.Context context){}
    private static void quad(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float x,float y,float width,float height,int color,boolean flip){
        var v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));var p=pose.last();int r=color>>16&255,g=color>>8&255,b=color&255;
        v.vertex(p.pose(),x,y,0).color(r,g,b,255).uv(0,flip?0:1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(p.normal(),0,0,-1).endVertex();
        v.vertex(p.pose(),x,y+height,0).color(r,g,b,255).uv(0,flip?1:0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(p.normal(),0,0,-1).endVertex();
        v.vertex(p.pose(),x+width,y+height,0).color(r,g,b,255).uv(1,flip?1:0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(p.normal(),0,0,-1).endVertex();
        v.vertex(p.pose(),x+width,y,0).color(r,g,b,255).uv(1,flip?0:1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(p.normal(),0,0,-1).endVertex();
    }
    private static void text(PoseStack pose,MultiBufferSource buffers,String text,float x,float y,int color){pose.pushPose();pose.translate(x,y,-.2);pose.scale(.65f,.65f,.65f);Minecraft.getInstance().font.drawInBatch(text,0,0,color,false,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);pose.popPose();}
    @Override public void render(DeviceBlockEntity d,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(d.getLevel()==null)return;var design=ProductDesign.of(d.getBlockState());
        // A registered sibling's own renderer/assets must define its front layout.
        if(d.getBlockState().getValue(DeviceBlock.MODEL)!=0)return;
        pose.pushPose();pose.translate(.5,0,.5);float angle=switch(d.getBlockState().getValue(DeviceBlock.FACING)){case EAST->-90;case SOUTH->-180;case WEST->-270;default->0;};pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        if(design.desktop){pose.translate(.5,1.2/16.,10/16.);pose.mulPose(Axis.XP.rotationDegrees(22.5f));pose.translate(-.5,-1.2/16.,-10/16.);}
        pose.translate(design.x1/16.,design.y1/16.,design.z0/16.-.002);pose.scale(-design.width()/4096f,-design.height()/4096f,1/4096f);
        if(design.sx!=design.ex){
            var texture=WorldCameras.texture(d);int x=design.sx,y=design.sy,w=design.ex-x,h=design.ey-y;
            quad(pose,buffers,texture==null?WHITE:texture,x,y,w,h,texture==null?0x10222d:0xffffff,texture==null);
            if(texture==null){text(pose,buffers,d.addressConfigured()?d.buildingNumber()+" - "+d.unitNumber():"HOME SYSTEMS",x+6,y+12,0xe3edf1);text(pose,buffers,d.state()==0?"STANDBY":d.state()==1?"CALLING":d.state()==2?"INCOMING":"CONNECTED",x+6,y+25,0x76dfc4);}
            if(design==ProductDesign.WALLPAD&&d.panelPage()==1){for(int i=0;i<4;i++){int cx=30+(i%2)*103,cy=55+(i/2)*44;quad(pose,buffers,WHITE,cx,cy,92,34,d.homeNetwork().enabled(i)?0x327e72:0x204c5e,true);text(pose,buffers,new String[]{"LIGHT","DOOR","GAS","OTHER"}[i],cx+5,cy+10,0xffffff);}}
        }
        if(design==ProductDesign.WALLPAD){
            String[] labels={"monitor","panel_guard","panel_home","answer","unlock","end"};
            for(int i=0;i<6;i++){int cx=30+(i%3)*68,cy=135+(i/3)*39;quad(pose,buffers,WHITE,cx,cy,63,31,0x204c5e,true);text(pose,buffers,net.minecraft.network.chat.Component.translatable("screen.homesystems."+labels[i]).getString(),cx+3,cy+10,0xe0f0f5);}
        }
        boolean blink=d.state()!=DeviceBlockEntity.RINGING||d.getLevel().getGameTime()%20<10;
        quad(pose,buffers,WHITE,238,10,6,6,d.state()==0?0x37614e:blink?0x5affbc:0x183d36,true);
        quad(pose,buffers,new ResourceLocation(HomeSystems.MODID,"textures/logo/"+design.id+".png"),83,239,90,10,0xffffff,true);
        pose.popPose();
    }
    @Override public int getViewDistance(){return 24;}
}
