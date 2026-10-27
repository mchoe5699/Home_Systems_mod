package kr.mchoe.homesystems.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.mchoe.homesystems.VideoFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Renders a live server scene into its own framebuffer, without moving the player or world camera. */
public final class CameraView implements AutoCloseable {
    private RenderTarget target;
    private VideoFrame frame;
    private VideoFrame renderedFrame;
    private long receivedAt;
    private final Map<UUID, Entity> actors = new HashMap<>();
    private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(new BufferBuilder(262144));

    public static void receive(VideoFrame frame) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DeviceScreen screen && screen.getMenu().containerId == frame.containerId()) screen.camera().accept(frame);
    }

    public void accept(VideoFrame frame) { this.frame = frame; receivedAt = System.currentTimeMillis(); }
    public boolean hasFrame() { return frame != null && System.currentTimeMillis() - receivedAt < 2500; }

    private boolean prepare() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasFrame() || minecraft.level == null) return false;
        if (target == null) target = new TextureTarget(384, 216, true, Minecraft.ON_OSX);
        if (frame != renderedFrame) {
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        try {
            target.setClearColor(.12F, .19F, .24F, 1);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(75), 384F / 216F, .05F, 40F), VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            PoseStack pose = new PoseStack();
            var direction = frame.facing();
            Vector3f eye = new Vector3f(.5F + direction.getStepX() * .35F, .78F, .5F + direction.getStepZ() * .35F);
            Vector3f look = new Vector3f(eye).add(direction.getStepX(), -.08F, direction.getStepZ());
            pose.mulPoseMatrix(new Matrix4f().lookAt(eye, look, new Vector3f(0, 1, 0)));
            for (var cell : frame.blocks()) {
                var state = Block.stateById(cell.state());
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                pose.pushPose();
                pose.translate(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
                minecraft.getBlockRenderer().renderSingleBlock(state, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            buffers.endBatch();
            var dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            for (var actor : frame.actors()) {
                Entity entity = actors.computeIfAbsent(actor.uuid(), ignored -> {
                    if (actor.type().equals("minecraft:player")) return new RemotePlayer(minecraft.level, new GameProfile(actor.uuid(), actor.name()));
                    // Other mods' custom entity renderers may depend on data not present in a camera frame.
                    ResourceLocation type = ResourceLocation.tryParse(actor.type());
                    if (type == null || !type.getNamespace().equals("minecraft")) return null;
                    return BuiltInRegistries.ENTITY_TYPE.getOptional(type).map(value -> value.create(minecraft.level)).orElse(null);
                });
                if (!(entity instanceof LivingEntity living)) continue;
                entity.setPos(actor.x(), actor.y(), actor.z());
                entity.setYRot(actor.yaw());
                entity.setXRot(actor.pitch());
                entity.yRotO = actor.yaw();
                entity.xRotO = actor.pitch();
                entity.tickCount = (int)frame.time();
                living.yBodyRot = living.yBodyRotO = actor.yaw();
                living.yHeadRot = living.yHeadRotO = actor.yaw();
                entity.setPose(actor.crouching() ? Pose.CROUCHING : Pose.STANDING);
                dispatcher.render(entity, actor.x(), actor.y(), actor.z(), actor.yaw(), 0, pose, buffers, LightTexture.FULL_BRIGHT);
            }
            buffers.endBatch();
            actors.keySet().removeIf(uuid -> frame.actors().stream().noneMatch(actor -> actor.uuid().equals(uuid)));
        } finally {
            minecraft.getEntityRenderDispatcher().setRenderShadow(true);
            minecraft.getMainRenderTarget().bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, sorting);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        }
        renderedFrame = frame;
        }
        return true;
    }
    private net.minecraft.client.renderer.texture.DynamicTexture worldTexture;
    private ResourceLocation worldTextureId;
    private VideoFrame uploadedFrame;
    public ResourceLocation worldTexture(){
        if(!prepare())return null;
        if(worldTexture==null){worldTexture=new net.minecraft.client.renderer.texture.DynamicTexture(384,216,false);worldTextureId=Minecraft.getInstance().getTextureManager().register("homesystems_camera",worldTexture);}
        if(uploadedFrame!=frame){RenderSystem.bindTexture(target.getColorTextureId());worldTexture.getPixels().downloadTexture(0,false);worldTexture.upload();uploadedFrame=frame;}
        return worldTextureId;
    }
    public void render(GuiGraphics graphics,int x,int y,int width,int height){
        graphics.flush();if(!prepare())return;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder quad = Tesselator.getInstance().getBuilder();
        quad.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quad.vertex(matrix, x, y + height, 0).uv(0, 0).endVertex();
        quad.vertex(matrix, x + width, y + height, 0).uv(1, 0).endVertex();
        quad.vertex(matrix, x + width, y, 0).uv(1, 1).endVertex();
        quad.vertex(matrix, x, y, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(quad.end());
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
    }

    public void clear() { frame = null; renderedFrame = null; actors.clear(); }
    @Override public void close() {
        clear();
        if(worldTextureId!=null){Minecraft.getInstance().getTextureManager().release(worldTextureId);worldTexture=null;worldTextureId=null;uploadedFrame=null;}
        if (target != null) { target.destroyBuffers(); target = null; }
    }
}
