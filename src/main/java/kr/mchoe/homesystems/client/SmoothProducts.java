package kr.mchoe.homesystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import kr.mchoe.homesystems.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

/** One cached smooth mesh per product, shared by placed devices and inventory rendering. */
final class SmoothProducts {
    private record Part(SmoothGeometry.Box mesh,ResourceLocation front,ResourceLocation material) {
        void draw(PoseStack pose,MultiBufferSource buffers,int light){mesh.face().draw(front,pose,buffers,light);mesh.shell().draw(material,pose,buffers,light);}
    }
    private static final Map<ProductDesign,SmoothProducts> CACHE=new EnumMap<>(ProductDesign.class);
    private final List<Part> parts=new ArrayList<>();
    private final ProductDesign design;
    private SmoothGeometry.Mesh receiver;
    private final ResourceLocation material,front;
    private SmoothProducts(ProductDesign d){
        design=d;material=texture(d.id+"_side");front=texture(d==ProductDesign.SHT308?"sht_308_mesh":d.id+"_base");
        if(d==ProductDesign.SHT308){
            add(151,5,507,505,13.3f,16,20,3,null);
            add(20,24,157,492,12.9f,15.6f,17,3,null);
            add(31,177,143,318,12.57f,12.94f,12,2,null);
            add(29,34,140,162,12.3f,12.94f,12,2,new float[]{35,40,134,156});
            add(31,354,143,480,12.35f,12.94f,12,2,new float[]{38,362,138,474});
            add(66,48,103,91,12.18f,12.4f,2,1,null);
            add(172,25,489,218,13.12f,13.34f,8,1,new float[]{200,56,386,202});
            for(int y:new int[]{286,330,374,431}){int r=y==431?18:14;add(467-r,y-r,467+r,y+r,13.015f,13.31f,r,1,null);}
            receiver=SmoothGeometry.receiver(15-61*14f/512,14.5f-3*13.5f/512,14.5f-508*13.5f/512,11.63f,55*116f/128*14/512,.98f,true);
        }else{
            parts.add(new Part(SmoothGeometry.box(d.x0,d.y0,d.z0,d.x1,d.y1,d.z1,Math.min(d.width(),d.height())*.045f,.10f,bounds(),null),front,material));
            // The reference guard station has raised rubber number keys and a metal release key.
            // The lobby/video/wallpad reference controls are printed/touch surfaces, kept flush.
            if(d==ProductDesign.GUARD){
                for(int row=0;row<4;row++)for(int col=0;col<3;col++){
                    float x=86+24*col,y=151+20*row;genericPart(x-8,y-8,x+8,y+8,.11f,7.5f);
                }
                genericPart(171,166,221,216,.15f,24);
            }
            if(d.handset){
                float hx0=d==ProductDesign.INTERPHONE?d.x0+.15f:d.x1-d.width()*.22f;
                float hx1=d==ProductDesign.INTERPHONE?d.x1-.15f:d.x1-.25f;
                receiver=SmoothGeometry.receiver((hx0+hx1)/2,d.y1-(d==ProductDesign.INTERPHONE?1:.5f),d.y0+.5f,d.z0-(d==ProductDesign.INTERPHONE?.45f:d==ProductDesign.VIDEO?.28f:.2f),(hx1-hx0)/2,d==ProductDesign.INTERPHONE?.99f:d==ProductDesign.VIDEO?.4463f:.3524f,false);
            }
        }
    }
    private void genericPart(float u0,float v0,float u1,float v1,float depth,float radius){
        float sx=design.width()/256,sy=design.height()/256;
        var box=SmoothGeometry.box(design.x1-u1*sx,design.y1-v1*sy,design.z0-depth,
            design.x1-u0*sx,design.y1-v0*sy,design.z0+.005f,radius*Math.min(sx,sy),.025f,bounds(),null);
        parts.add(new Part(box,front,material));
    }
    private static ResourceLocation texture(String id){return new ResourceLocation(HomeSystems.MODID,"textures/block/"+id+".png");}
    private float[] bounds(){return new float[]{design.x0,design.y0,design.x1,design.y1};}
    private void add(float u0,float v0,float u1,float v1,float near,float far,float radius,float bevel,float[] hole){
        float scale=14f/512,yscale=13.5f/512;
        float[] cut=hole==null?null:new float[]{15-hole[2]*scale,14.5f-hole[3]*yscale,15-hole[0]*scale,14.5f-hole[1]*yscale};
        var mesh=SmoothGeometry.box(15-u1*scale,14.5f-v1*yscale,near,15-u0*scale,14.5f-v0*yscale,far,radius*Math.min(scale,yscale),bevel*Math.min(scale,yscale),bounds(),cut);
        parts.add(new Part(mesh,front,material));
    }
    static void render(ProductDesign design,boolean offHook,PoseStack pose,MultiBufferSource buffers,int light){
        var product=CACHE.computeIfAbsent(design,SmoothProducts::new);
        pose.pushPose();
        if(design.desktop){
            var base=pedestal();
            base.face().draw(texture("guard_station_side"),pose,buffers,light);
            base.shell().draw(texture("guard_station_side"),pose,buffers,light);
            tilt(pose);
        }
        for(var part:product.parts)part.draw(pose,buffers,light);
        if(product.receiver!=null){
            pose.pushPose();if(offHook)pose.translate((design==ProductDesign.SHT308?4.2:1.3)/16,1/16.,(design==ProductDesign.SHT308?-1.8:-1)/16);
            var texture=texture(design==ProductDesign.SHT308?(offHook?"sht_308_receiver_mesh":"sht_308_receiver_outer_mesh"):design.id+"_receiver_smooth");
            product.receiver.draw(texture,pose,buffers,light);pose.popPose();
        }
        pose.popPose();
    }
    private static SmoothGeometry.Box pedestal;
    private static SmoothGeometry.Box pedestal(){
        if(pedestal==null){var box=SmoothGeometry.box(1.5f,0,8.5f,14.5f,1.2f,15,.35f,.15f,new float[]{1.5f,0,14.5f,1.2f},null);
            // The pedestal is untextured casing on both sides.
            pedestal=box;}
        return pedestal;
    }
    static void tilt(PoseStack pose){pose.translate(.5,1.2/16.,10/16.);pose.mulPose(Axis.XP.rotationDegrees(22.5f));pose.translate(-.5,-1.2/16.,-10/16.);}
}
