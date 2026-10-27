package kr.mchoe.homesystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.mchoe.homesystems.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** Shares the cached world meshes instead of keeping a second segmented item model. */
public final class SmoothItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static SmoothItemRenderer renderer;
    private static final Map<ProductDesign,SmoothGeometry.Mesh> LOGOS=new EnumMap<>(ProductDesign.class);
    private SmoothItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static SmoothItemRenderer instance(){if(renderer==null)renderer=new SmoothItemRenderer();return renderer;}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.getItem() instanceof BlockItem item){
            var design=ProductDesign.of(item.getBlock().defaultBlockState());
            SmoothProducts.render(design,false,pose,buffers,light);
            pose.pushPose();if(design.desktop)SmoothProducts.tilt(pose);
            LOGOS.computeIfAbsent(design,d->{
                var mesh=new SmoothGeometry.Mesh();float x=d.x1-d.logoX()*d.width()/256;
                float y=d.y1-d.logoY()*d.height()/256,z=d.faceZ()-.015f;
                float w=d.logoWidth()*d.width()/256,h=d.logoHeight()*d.height()/256;
                mesh.point(new SmoothGeometry.Point(x,y,z,0,0,-1),0,0);
                mesh.point(new SmoothGeometry.Point(x,y-h,z,0,0,-1),0,1);
                mesh.point(new SmoothGeometry.Point(x-w,y-h,z,0,0,-1),1,1);
                mesh.point(new SmoothGeometry.Point(x-w,y,z,0,0,-1),1,0);return mesh.seal();
            }).draw(new ResourceLocation(HomeSystems.MODID,"textures/logo/"+design.id+".png"),pose,buffers,light);
            pose.popPose();
        }
    }
}
