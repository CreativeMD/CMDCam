package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.FieldOfView;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.scene.mode.OutsideMode;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.Vec3d;

public class CamEventHandlerClient {
    
    public static Minecraft mc = Minecraft.getInstance();
    
    public static final float amountZoom = 0.1F;
    public static final float amountroll = 1.5F;
    
    private static int previousFOV = mc.options.fov().get();
    public static double currentFOV = previousFOV;
    public static float roll = 0;
    
    public static long lastRenderTime;
    
    private static Consumer<CamTarget> selectingTarget = null;
    
    public static Entity camera = null;
    
    public static void startSelectionMode(Consumer<CamTarget> selectingTarget) {
        CamEventHandlerClient.selectingTarget = selectingTarget;
    }
    
    public static void resetFOV() {
        currentFOV = previousFOV;
    }
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END)
            return;
        if (mc.player != null && mc.level != null && !mc.isPaused() && CMDCamClient.isPlaying())
            CMDCamClient.mcTickPath(mc.level);
    }
    
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (mc.level == null) {
            CMDCamClient.resetServerAvailability();
            CMDCamClient.resetTargetMarker();
        }
        if (event.phase == Phase.END)
            return;
        
        if (previousFOV != mc.options.fov().get())
            currentFOV = previousFOV = mc.options.fov().get();
        
        if (mc.player != null && mc.level != null) {
            if (!mc.isPaused()) {
                if (CMDCamClient.isPlaying()) {
                    while (mc.options.keyJump.consumeClick()) {
                        if (CMDCamClient.isPlaying())
                            CMDCamClient.getScene().togglePause();
                    }
                    
                    CMDCamClient.tickPath(mc.level, event.renderTickTime);
                } else {
                    CMDCamClient.noTickPath(mc.level, event.renderTickTime);
                    double timeFactor = (System.nanoTime() - lastRenderTime) / 10000000D;
                    if (KeyHandler.zoomIn.isDown()) {
                        if (mc.player.isCrouching())
                            currentFOV -= timeFactor * amountZoom * 10;
                        else
                            currentFOV -= timeFactor * amountZoom;
                    }
                    
                    if (KeyHandler.zoomOut.isDown()) {
                        if (mc.player.isCrouching())
                            currentFOV += timeFactor * amountZoom * 10;
                        else
                            currentFOV += timeFactor * amountZoom;
                    }
                    
                    if (KeyHandler.zoomCenter.isDown())
                        currentFOV = previousFOV;
                    
                    if (KeyHandler.rollLeft.isDown())
                        roll -= timeFactor * amountroll;
                    
                    if (KeyHandler.rollRight.isDown())
                        roll += timeFactor * amountroll;
                    
                    if (KeyHandler.rollCenter.isDown())
                        roll = 0;
                    
                    while (KeyHandler.pointKey.consumeClick()) {
                        CamPoint point = CamPoint.createLocal();
                        if (CMDCamClient.getScene().posTarget != null) {
                            Vec3d vec = CMDCamClient.getTargetMarker();
                            if (vec == null) {
                                mc.player.sendSystemMessage(Component.translatable("scene.follow.no_marker", CMDCamClient.getPoints().size()));
                                continue;
                            }
                            point.sub(vec);
                        }
                        CMDCamClient.getPoints().add(point);
                        mc.player.sendSystemMessage(Component.translatable("scene.add", CMDCamClient.getPoints().size()));
                    }
                }
                
                if (KeyHandler.startStop.consumeClick()) {
                    if (CMDCamClient.isPlaying())
                        CMDCamClient.stop();
                    else
                        try {
                            CMDCamClient.start(CMDCamClient.createScene());
                        } catch (SceneException e) {
                            mc.player.sendSystemMessage(Component.translatable(e.getMessage()));
                        }
                }
                
                while (KeyHandler.clearPoint.consumeClick()) {
                    CMDCamClient.getPoints().clear();
                    mc.player.sendSystemMessage(Component.translatable("scene.clear"));
                }
            }
        }
        lastRenderTime = System.nanoTime();
    }
    
    @SubscribeEvent
    public void fov(FieldOfView event) {
        event.setFOV(currentFOV);
    }
    
    @SubscribeEvent
    public void worldRender(RenderLevelLastEvent event) {
        if (CMDCamClient.isPlaying())
            return;
        RenderSystem.enableBlend();
        RenderSystem
                .blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        
        Vec3 view = mc.gameRenderer.getMainCamera().getPosition();
        
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix());
        PoseStack mat = RenderSystem.getModelViewStack();
        mat.pushPose();
        mat.setIdentity();
        mat.mulPoseMatrix(event.getPoseStack().last().pose());
        mat.translate(-view.x(), -view.y(), -view.z());
        
        RenderSystem.applyModelViewMatrix();
        
        RenderSystem.depthMask(false);
        RenderSystem.disableTexture();
        
        if (CMDCamClient.hasTargetMarker()) {
            CamPoint point = CMDCamClient.getTargetMarker();
            renderHitbox(mat, mc.renderBuffers().bufferSource()
                    .getBuffer(RenderType.lines()), new AABB(point.x - 0.3, point.y - 1.62, point.z - 0.3, point.x + 0.3, point.y + 0.18, point.z + 0.3), mc.player
                            .getEyeHeight(), point, point.calculateViewVector());
        }
        
        boolean shouldRender = false;
        for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
            if (movement.isRenderingEnabled) {
                shouldRender = true;
                break;
            }
        
        if (shouldRender && CMDCamClient.getPoints().size() > 0) {
            for (int i = 0; i < CMDCamClient.getPoints().size(); i++) {
                CamPoint point = CMDCamClient.getPoints().get(i);
                if (CMDCamClient.hasTargetMarker()) {
                    point = point.copy();
                    point.add(CMDCamClient.getTargetMarker());
                }
                
                DebugRenderer.renderFloatingText((i + 1) + "", point.x + view.x, point.y + 0.2 + view.y, point.z + view.z, -1);
                DebugRenderer.renderFilledBox(point.x - 0.05, point.y - 0.05, point.z - 0.05, point.x + 0.05, point.y + 0.05, point.z + 0.05, 1, 1, 1, 1);
                
                RenderSystem.depthMask(false);
                RenderSystem.disableTexture();
            }
            
            try {
                mat.pushPose();
                //if (CMDCamClient.hasTargetMarker())
                //mat.translate(CMDCamClient.getTargetMarker().x, CMDCamClient.getTargetMarker().y, CMDCamClient.getTargetMarker().z);
                CamScene scene = CMDCamClient.createScene();
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    if (movement.isRenderingEnabled)
                        renderPath(mat, movement, scene);
                    
                mat.popPose();
            } catch (SceneException e) {}
            
        }
        
        mat.popPose();
        
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        
    }
    
    public void renderPath(PoseStack mat, CamInterpolation inter, CamScene scene) {
        double steps = 20 * (scene.points.size() - 1);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        
        RenderSystem.lineWidth(1.0F);
        Vec3d color = inter.color.toVec();
        bufferbuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        CamPoints points = new CamPoints(scene.points);
        double[] times = points.createTimes(scene);
        Interpolation<Vec3d> interpolation = inter.create(times, scene, null, new ArrayList<Vec3d>(scene.points), null, CamAttribute.POSITION);
        for (int i = 0; i < steps; i++) {
            Vec3d pos = interpolation.valueAt(i / steps);
            if (CMDCamClient.hasTargetMarker())
                pos.add(CMDCamClient.getTargetMarker());
            bufferbuilder.vertex((float) pos.x, (float) pos.y, (float) pos.z).color((float) color.x, (float) color.y, (float) color.z, 1).endVertex();
        }
        Vec3d last = scene.points.get(scene.points.size() - 1).copy();
        if (CMDCamClient.hasTargetMarker())
            last.add(CMDCamClient.getTargetMarker());
        bufferbuilder.vertex((float) last.x, (float) last.y, (float) last.z).color((float) color.x, (float) color.y, (float) color.z, 1).endVertex();
        
        tessellator.end();
    }
    
    private static void renderHitbox(PoseStack pMatrixStack, VertexConsumer pBuffer, AABB aabb, float eyeHeight, Vec3d origin, Vec3d view) {
        LevelRenderer.renderLineBox(pMatrixStack, pBuffer, aabb, 1.0F, 1.0F, 1.0F, 1.0F);
        
        float f = 0.01F;
        LevelRenderer
                .renderLineBox(pMatrixStack, pBuffer, aabb.minX, aabb.minY + (eyeHeight - f), aabb.minZ, aabb.maxX, aabb.minY + (eyeHeight + f), aabb.maxZ, 1.0F, 0.0F, 0.0F, 1.0F);
        
        Matrix4f matrix4f = pMatrixStack.last().pose();
        Matrix3f matrix3f = pMatrixStack.last().normal();
        pBuffer.vertex(matrix4f, (float) origin.x, (float) origin.y, (float) origin.z).color(0, 0, 255, 255).normal(matrix3f, (float) view.x, (float) view.y, (float) view.z)
                .endVertex();
        pBuffer.vertex(matrix4f, (float) (origin.x + view.x * 2), (float) (origin.y + view.y * 2), (float) (origin.z + view.z * 2)).color(0, 0, 255, 255)
                .normal(matrix3f, (float) view.x, (float) view.y, (float) view.z).endVertex();
    }
    
    @SubscribeEvent
    public void cameraRoll(CameraSetup event) {
        event.setRoll(roll);
    }
    
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (selectingTarget != null || !event.getWorld().isClientSide)
            return;
        
        if (event instanceof EntityInteract) {
            selectingTarget.accept(new CamTarget.EntityTarget(((EntityInteract) event).getTarget()));
            event.getPlayer().sendSystemMessage(Component.translatable("scene.look.target.entity", ((EntityInteract) event).getTarget().getStringUUID()));
            selectingTarget = null;
        }
        
        if (event instanceof RightClickBlock) {
            selectingTarget.accept(new CamTarget.BlockTarget(event.getPos()));
            event.getPlayer().sendSystemMessage(Component.translatable("scene.look.target.pos", event.getPos().toShortString()));
            selectingTarget = null;
        }
    }
    
    public static void setupMouseHandlerBefore() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            camera = mc.cameraEntity;
            mc.cameraEntity = mc.player;
        }
    }
    
    public static void setupMouseHandlerAfter() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            mc.cameraEntity = camera;
            camera = null;
        }
    }
}
