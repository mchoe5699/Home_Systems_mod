package kr.mchoe.homesystems;

import net.minecraft.world.level.block.state.BlockState;

/** Shared normalized front-panel coordinates for the block renderer and its actual clickable face. */
public enum ProductDesign {
    LOBBY("lobby_phone",2.6f,1.5f,13.5f,13.4f,13.45f,16,30,48,100,153,false,false),
    VIDEO("video_phone",.5f,3.5f,13.6f,15.5f,12.7f,16,78,48,225,174,true,false),
    INTERPHONE("interphone",6.1f,1,12.5f,9.9f,14.3f,16,0,0,0,0,true,false),
    WALLPAD("wallpad",.5f,3,14.5f,15.5f,13.45f,16,18,20,238,220,false,false),
    GUARD("guard_station",1.5f,1.2f,10,14.5f,13.5f,12.5f,76,20,236,128,true,true),
    CAMERA("door_camera",2,1,13,14,15,16,0,0,0,0,false,false),
    DOOR("door_phone",2,1,13,14,15,16,0,0,0,0,false,false),
    SHT308("sht_308",1,1,10.5f,15,14.5f,16,100,32,193,101,true,false);
    public final String id;public final float x0,y0,z0,x1,y1,z1;public final int sx,sy,ex,ey;public final boolean handset,desktop;
    ProductDesign(String id,float x0,float y0,float z0,float x1,float y1,float z1,int sx,int sy,int ex,int ey,boolean handset,boolean desktop){this.id=id;this.x0=x0;this.y0=y0;this.z0=z0;this.x1=x1;this.y1=y1;this.z1=z1;this.sx=sx;this.sy=sy;this.ex=ex;this.ey=ey;this.handset=handset;this.desktop=desktop;}
    public static ProductDesign of(BlockState state){
        if(state.is(HomeSystems.SHT_308.get()))return SHT308;
        if(state.is(HomeSystems.LOBBY_PHONE.get()))return LOBBY;if(state.is(HomeSystems.VIDEO_PHONE.get()))return VIDEO;
        if(state.is(HomeSystems.INTERPHONE.get()))return INTERPHONE;if(state.is(HomeSystems.WALLPAD.get()))return WALLPAD;
        if(state.is(HomeSystems.GUARD_STATION.get()))return GUARD;if(state.is(HomeSystems.DOOR_CAMERA.get()))return CAMERA;return DOOR;
    }
    public float faceZ(){return this==SHT308?13.3f:z0;}
    public int logoX(){return this==SHT308?88:83;}public int logoY(){return this==SHT308?119:239;}public int logoWidth(){return this==SHT308?65:90;}public int logoHeight(){return this==SHT308?12:10;}
    public float width(){return x1-x0;}public float height(){return y1-y0;}
}
