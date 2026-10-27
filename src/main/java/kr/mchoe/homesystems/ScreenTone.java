package kr.mchoe.homesystems;

/** NativeImage pixels use ABGR. Apply a true luminance signal, never a colored overlay. */
public final class ScreenTone {
    public static int monochrome(int abgr,int brightness,int contrast){
        int r=abgr&255,g=abgr>>>8&255,b=abgr>>>16&255;
        float luminance=(54*r+183*g+19*b)/256f;
        float gain=new float[]{.65f,1f,1.35f}[Math.max(0,Math.min(2,contrast))];
        int offset=new int[]{-45,0,45}[Math.max(0,Math.min(2,brightness))];
        int value=Math.max(0,Math.min(255,Math.round((luminance-128)*gain+128+offset)));
        return abgr&0xff000000|value|value<<8|value<<16;
    }
}
