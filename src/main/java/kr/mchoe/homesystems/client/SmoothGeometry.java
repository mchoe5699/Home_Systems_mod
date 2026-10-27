package kr.mchoe.homesystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Closed surfaces with shared edges and interpolated normals. Geometry is built once. */
final class SmoothGeometry {
    record Point(float x,float y,float z,float nx,float ny,float nz) {}
    static final class Mesh {
        private final List<Float> building=new ArrayList<>();
        private float[] vertices;
        void point(Point p,float u,float v){
            for(float f:new float[]{p.x/16,p.y/16,p.z/16,u,v,p.nx,p.ny,p.nz})building.add(f);
        }
        Mesh seal(){vertices=new float[building.size()];for(int i=0;i<vertices.length;i++)vertices[i]=building.get(i);building.clear();return this;}
        void draw(ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light){
            var v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));var p=pose.last();
            for(int i=0;i<vertices.length;i+=8)v.vertex(p.pose(),vertices[i],vertices[i+1],vertices[i+2])
                .color(255,255,255,255).uv(vertices[i+3],vertices[i+4]).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(p.normal(),vertices[i+5],vertices[i+6],vertices[i+7]).endVertex();
        }
    }
    record Box(Mesh face,Mesh shell) {}
    // All front UVs use the product's complete face, even on small raised parts.
    private static void front(Mesh m,Point p,float[] uv){m.point(p,(uv[2]-p.x)/(uv[2]-uv[0]),(uv[3]-p.y)/(uv[3]-uv[1]));}
    private static Point[] ring(float x0,float y0,float x1,float y1,float z,float radius,float inset,float sideNormal,float zNormal){
        Point[] points=new Point[65];float r=Math.max(.001f,radius-inset);
        float[] cx={x1-radius,x0+radius,x0+radius,x1-radius};
        float[] cy={y1-radius,y1-radius,y0+radius,y0+radius};
        for(int i=0;i<64;i++){
            int corner=i/16;double a=(corner*16+i%16)*Math.PI/32;
            float cos=(float)Math.cos(a),sin=(float)Math.sin(a);
            points[i]=new Point(cx[corner]+r*cos,cy[corner]+r*sin,z,cos*sideNormal,sin*sideNormal,zNormal);
        }
        points[64]=points[0];return points;
    }
    static Box box(float x0,float y0,float z0,float x1,float y1,float z1,float radius,float bevel,float[] uv,float[] hole){
        Mesh face=new Mesh(),shell=new Mesh();bevel=Math.min(bevel,(z1-z0)/2);
        Point[] first=ring(x0,y0,x1,y1,z0,radius,bevel,0,-1);
        Point[] inner=hole==null?null:ring(hole[0],hole[1],hole[2],hole[3],z0,.01f,0,0,-1);
        Point centre=new Point((x0+x1)/2,(y0+y1)/2,z0,0,0,-1);
        for(int i=0;i<64;i++){
            front(face,first[i],uv);front(face,first[i+1],uv);
            front(face,inner==null?centre:inner[i+1],uv);front(face,inner==null?centre:inner[i],uv);
        }
        List<Point[]> rings=new ArrayList<>();rings.add(first);
        for(int i=1;i<=6;i++){
            double a=i*Math.PI/12;float s=(float)Math.sin(a),c=(float)Math.cos(a);
            rings.add(ring(x0,y0,x1,y1,z0+bevel*(1-c),radius,bevel*(1-s),s,-c));
        }
        rings.add(ring(x0,y0,x1,y1,z1-bevel,radius,0,1,0));
        for(int i=1;i<=6;i++){
            double a=i*Math.PI/12;float s=(float)Math.sin(a),c=(float)Math.cos(a);
            rings.add(ring(x0,y0,x1,y1,z1-bevel+bevel*s,radius,bevel*(1-c),c,s));
        }
        for(int k=0;k<rings.size()-1;k++)for(int i=0;i<64;i++){
            Point[] a=rings.get(k),b=rings.get(k+1);
            for(Point p:new Point[]{a[i],b[i],b[i+1],a[i+1]})shell.point(p,.5f,.5f);
        }
        Point[] last=rings.get(rings.size()-1);Point back=new Point(centre.x,centre.y,z1,0,0,1);
        for(int i=0;i<64;i++)for(Point p:new Point[]{last[i+1],last[i],back,back})shell.point(p,.5f,.5f);
        if(inner!=null)for(int i=0;i<64;i++){
            Point a=inner[i],b=inner[i+1];
            for(Point p:new Point[]{a,b,new Point(b.x,b.y,z1,0,0,-1),new Point(a.x,a.y,z1,0,0,-1)})shell.point(p,.5f,.5f);
        }
        return new Box(face.seal(),shell.seal());
    }
    static Mesh receiver(float cx,float top,float bottom,float cz,float halfWidth,float depth,boolean sht){
        Mesh mesh=new Mesh();Point[][] points=new Point[65][33];
        for(int row=0;row<=64;row++){
            float t=row/64f;double cap=t<.07?Math.sqrt(Math.max(0,1-Math.pow((.07-t)/.07,2))):t>.93?Math.sqrt(Math.max(0,1-Math.pow((t-.93)/.07,2))):1;
            double width=(sht?(46+9*Math.pow(Math.abs(2*t-1),.65))/55:(.82+.18*Math.pow(Math.abs(2*t-1),.65)))*cap;
            for(int col=0;col<=32;col++){
                double a=col*Math.PI/16;float cos=(float)Math.cos(a),sin=(float)Math.sin(a);
                points[row][col]=new Point(cx+(float)(halfWidth*width*cos),top+(bottom-top)*t,cz+(float)(depth*cap*sin),cos,0,sin);
            }
        }
        for(int row=0;row<64;row++)for(int col=0;col<32;col++)for(int[] index:new int[][]{{row,col},{row+1,col},{row+1,col+1},{row,col+1}}){
            Point p=points[index[0]][index[1]];mesh.point(p,(float)((1-Math.cos(index[1]*Math.PI/16))/2),index[0]/64f);
        }
        return mesh.seal();
    }
}
