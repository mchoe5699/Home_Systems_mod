package kr.mchoe.homesystems.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import kr.mchoe.homesystems.DeviceBlockEntity;
import kr.mchoe.homesystems.HomeSystems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** A closed curved receiver surface, without overlapping cuboid faces or a cord. */
final class Sht308ReceiverRenderer {
    private static final ResourceLocation OUTER = new ResourceLocation(HomeSystems.MODID,
        "textures/block/sht_308_receiver_outer_mesh.png");
    private static final ResourceLocation INNER = new ResourceLocation(HomeSystems.MODID,
        "textures/block/sht_308_receiver_mesh.png");
    private static final int LENGTH_STEPS = 64, RING_STEPS = 24;

    static void render(DeviceBlockEntity device, PoseStack pose, MultiBufferSource buffers, int light) {
        var vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(device.offHook() ? INNER : OUTER));
        for (int row = 0; row < LENGTH_STEPS; row++) {
            float top = row / (float)LENGTH_STEPS, bottom = (row + 1) / (float)LENGTH_STEPS;
            for (int col = 0; col < RING_STEPS; col++) {
                double left = col * Math.PI * 2 / RING_STEPS;
                double right = (col + 1) * Math.PI * 2 / RING_STEPS;
                vertex(vertices, pose, device.offHook(), top, left, light);
                vertex(vertices, pose, device.offHook(), bottom, left, light);
                vertex(vertices, pose, device.offHook(), bottom, right, light);
                vertex(vertices, pose, device.offHook(), top, right, light);
            }
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack pose, boolean raised,
                               float t, double angle, int light) {
        double cap = t < .07 ? Math.sqrt(Math.max(0, 1 - Math.pow((.07-t)/.07, 2)))
            : t > .93 ? Math.sqrt(Math.max(0, 1 - Math.pow((t-.93)/.07, 2))) : 1;
        double half = (46 + 9 * Math.pow(Math.abs(2*t-1), .65)) * cap * 116 / 128;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        float x = (float)((15 - 61*14./512 + half*14/512*cos + (raised?4.2:0)) / 16);
        float y = (float)((14.5 - (3 + 505*t)*13.5/512 + (raised?1:0)) / 16);
        float z = (float)((11.63 + (.94 + .08*Math.abs(2*t-1))*cap*sin + (raised?-1.8:0)) / 16);
        vertices.vertex(pose.last().pose(), x, y, z).color(255,255,255,255)
            .uv((float)((1-cos)/2), t).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
            .normal(pose.last().normal(), (float)cos, 0, (float)sin).endVertex();
    }
}
