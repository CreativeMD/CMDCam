package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
import net.neoforged.neoforge.event.TickEvent.ClientTickEvent;
import net.neoforged.neoforge.event.TickEvent.Phase;
import net.neoforged.neoforge.event.TickEvent.RenderTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import team.creative.cmdcam.client.mixin.GameRendererAccessor;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.scene.mode.OutsideMode;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.Vec3d;

@OnlyIn(Dist.CLIENT)
public class CamEventHandlerClient {
    
    public static final Minecraft MC = Minecraft.getInstance();
    
    public static final double ZOOM_STEP = 0.005;
    public static final float ROLL_STEP = 1.5F;
    public static final double MAX_FOV = 170;
    public static final double MIN_FOV = 0.1;
    public static final double FOV_RANGE = MAX_FOV - MIN_FOV;
    public static final double FOV_RANGE_HALF = FOV_RANGE / 2;
    
    public static Entity camera = null;
    
    private static double fov = 0;
    private static float roll = 0;
    private static Consumer<CamTarget> selectingTarget = null;
    
    private static boolean renderingHand = false;
    private static boolean skipFov = false;
    
    public static void startSelectionMode(Consumer<CamTarget> selectingTarget) {
        CamEventHandlerClient.selectingTarget = selectingTarget;
    }
    
    public static void resetRoll() {
        roll = 0;
    }
    
    public static float roll() {
        return roll;
    }
    
    public static void roll(float roll) {
        CamEventHandlerClient.roll = roll;
    }
    
    public static void resetFOV() {
        fov = 0;
    }
    
    public static double fovExactVanilla(float partialTickTime) {
        try {
            skipFov = true;
            return ((GameRendererAccessor) MC.gameRenderer).callGetFov(MC.gameRenderer.getMainCamera(), partialTickTime, true);
        } finally {
            skipFov = false;
        }
    }
    
    public static double fovExact(float partialTickTime) {
        return fovExactVanilla(partialTickTime) + fov;
    }
    
    public static void fov(double fov) {
        CamEventHandlerClient.fov = fov;
    }
    
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END)
            return;
        if (MC.player != null && MC.level != null && !MC.isPaused() && CMDCamClient.isPlaying())
            CMDCamClient.gameTickPath(MC.level);
    }
    
    private double calculatePointInCurve(double fov) {
        fov -= MIN_FOV;
        fov /= FOV_RANGE_HALF;
        fov = Mth.clamp(fov, 0, 2);
        return Math.asin(fov - 1) / Math.PI + 0.5;
    }
    
    private double transformFov(double x) {
        if (x <= 0)
            return MIN_FOV;
        if (x >= 1)
            return MAX_FOV;
        return (Math.sin((x - 0.5) * Math.PI) + 1) * FOV_RANGE_HALF + MIN_FOV;
    }
    
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (MC.level == null) {
            CMDCamClient.resetServerAvailability();
            CMDCamClient.resetTargetMarker();
        }
        if (event.phase == Phase.END)
            return;
        
        renderingHand = false;
        
        if (MC.player != null && MC.level != null) {
            if (!MC.isPaused()) {
                if (CMDCamClient.isPlaying()) {
                    while (MC.options.keyJump.consumeClick()) {
                        if (CMDCamClient.isPlaying() && !CMDCamClient.getScene().mode.outside())
                            CMDCamClient.getScene().togglePause();
                    }
                    
                    CMDCamClient.renderTickPath(MC.level, event.renderTickTime);
                } else {
                    CMDCamClient.noTickPath(MC.level, event.renderTickTime);
                    double timeFactor = MC.getDeltaFrameTime();
                    double vanillaFov = fovExactVanilla(event.renderTickTime);
                    double currentFov = vanillaFov + fov;
                    double x = calculatePointInCurve(currentFov);
                    double multiplier = MC.player.isCrouching() ? 5 : 1;
                    
                    if (KeyHandler.zoomIn.isDown())
                        fov = transformFov(multiplier * timeFactor * -ZOOM_STEP + x) - vanillaFov;
                    
                    if (KeyHandler.zoomOut.isDown())
                        fov = transformFov(multiplier * timeFactor * ZOOM_STEP + x) - vanillaFov;
                    
                    if (KeyHandler.zoomCenter.isDown())
                        resetFOV();
                    
                    if (KeyHandler.rollLeft.isDown())
                        roll -= timeFactor * ROLL_STEP;
                    
                    if (KeyHandler.rollRight.isDown())
                        roll += timeFactor * ROLL_STEP;
                    
                    if (KeyHandler.rollCenter.isDown())
                        resetRoll();
                    
                    while (KeyHandler.pointKey.consumeClick()) {
                        CamPoint point = CamPoint.createLocal();
                        if (CMDCamClient.getScene().posTarget != null) {
                            Vec3d vec = CMDCamClient.getTargetMarker();
                            if (vec == null) {
                                MC.player.sendSystemMessage(Component.translatable("scene.follow.no_marker", CMDCamClient.getPoints().size()));
                                continue;
                            }
                            point.sub(vec);
                        }
                        CMDCamClient.getPoints().add(point);
                        MC.player.sendSystemMessage(Component.translatable("scene.add", CMDCamClient.getPoints().size()));
                    }
                }
                
                if (KeyHandler.startStop.consumeClick()) {
                    if (CMDCamClient.isPlaying())
                        CMDCamClient.stop();
                    else
                        try {
                            CMDCamClient.start(CMDCamClient.createScene());
                        } catch (SceneException e) {
                            MC.player.sendSystemMessage(Component.translatable(e.getMessage()));
                        }
                }
                
                while (KeyHandler.clearPoint.consumeClick()) {
                    CMDCamClient.getPoints().clear();
                    MC.player.sendSystemMessage(Component.translatable("scene.clear"));
                }
            }
        }
    }
    
    @SubscribeEvent
    public void fov(ComputeFov event) {
        if (skipFov)
            return;
        
        if (!renderingHand) {
            if (CMDCamClient.isPlaying())
                event.setFOV(fov);
            else
                event.setFOV(event.getFOV() + fov);
            event.setFOV(Mth.clamp(event.getFOV(), MIN_FOV, MAX_FOV));
        }
        renderingHand = !renderingHand;
    }
    
    @SubscribeEvent
    public void worldRender(RenderLevelStageEvent event) {
        if (CMDCamClient.isPlaying() || event.getStage() != Stage.AFTER_ENTITIES)
            return;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        
        Vec3 view = MC.gameRenderer.getMainCamera().getPosition();
        
        RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack mat = RenderSystem.getModelViewStack();
        mat.pushPose();
        mat.setIdentity();
        mat.mulPoseMatrix(event.getPoseStack().last().pose());
        mat.translate(-view.x(), -view.y(), -view.z());
        
        RenderSystem.applyModelViewMatrix();
        
        RenderSystem.depthMask(false);
        
        if (CMDCamClient.hasTargetMarker()) {
            CamPoint point = CMDCamClient.getTargetMarker();
            renderHitbox(mat, MC.renderBuffers().bufferSource().getBuffer(RenderType.lines()),
                new AABB(point.x - 0.3, point.y - 1.62, point.z - 0.3, point.x + 0.3, point.y + 0.18, point.z + 0.3), MC.player.getEyeHeight(), point, point.calculateViewVector());
        }
        
        boolean shouldRender = false;
        for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
            if (movement.isRenderingEnabled) {
                shouldRender = true;
                break;
            }
        
        PoseStack pose = new PoseStack();
        
        if (shouldRender && CMDCamClient.getPoints().size() > 0) {
            for (int i = 0; i < CMDCamClient.getPoints().size(); i++) {
                CamPoint point = CMDCamClient.getPoints().get(i);
                if (CMDCamClient.hasTargetMarker()) {
                    point = point.copy();
                    point.add(CMDCamClient.getTargetMarker());
                }
                
                DebugRenderer.renderFilledBox(pose, MC.renderBuffers().bufferSource(), point.x - 0.05, point.y - 0.05, point.z - 0.05, point.x + 0.05, point.y + 0.05,
                    point.z + 0.05, 1, 1, 1, 1);
                DebugRenderer.renderFloatingText(pose, MC.renderBuffers().bufferSource(), (i + 1) + "", point.x + view.x, point.y + 0.2 + view.y, point.z + view.z, -1);
                
                RenderSystem.depthMask(false);
            }
            
            MC.renderBuffers().bufferSource().endLastBatch();
            
            try {
                mat.pushPose();
                //if (CMDCamClient.hasTargetMarker())
                //mat.translate(CMDCamClient.getTargetMarker().x, CMDCamClient.getTargetMarker().y, CMDCamClient.getTargetMarker().z);
                CamScene scene = CMDCamClient.createScene();
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    if (movement.isRenderingEnabled)
                        renderPath(pose, movement, scene);
                    
                mat.popPose();
            } catch (SceneException e) {}
            
        }
        
        mat.popPose();
        
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        
    }
    
    public void renderPath(PoseStack mat, CamInterpolation inter, CamScene scene) {
        double steps = 20 * (scene.points.size() - 1);
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        
        RenderSystem.lineWidth(1.0F);
        Vec3d color = inter.color.toVec();
        bufferbuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        CamPoints points = new CamPoints(scene.points);
        
        if (scene.lookTarget != null)
            scene.lookTarget.start(MC.level);
        if (scene.posTarget != null)
            scene.posTarget.start(MC.level);
        
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
        
        if (scene.lookTarget != null)
            scene.lookTarget.finish();
        if (scene.posTarget != null)
            scene.posTarget.finish();
    }
    
    private static void renderHitbox(PoseStack pMatrixStack, VertexConsumer pBuffer, AABB aabb, float eyeHeight, Vec3d origin, Vec3d view) {
        LevelRenderer.renderLineBox(pMatrixStack, pBuffer, aabb, 1.0F, 1.0F, 1.0F, 1.0F);
        
        float f = 0.01F;
        LevelRenderer.renderLineBox(pMatrixStack, pBuffer, aabb.minX, aabb.minY + (eyeHeight - f), aabb.minZ, aabb.maxX, aabb.minY + (eyeHeight + f), aabb.maxZ, 1.0F, 0.0F, 0.0F,
            1.0F);
        
        Matrix4f matrix4f = pMatrixStack.last().pose();
        Matrix3f matrix3f = pMatrixStack.last().normal();
        pBuffer.vertex(matrix4f, (float) origin.x, (float) origin.y, (float) origin.z).color(0, 0, 255, 255).normal(matrix3f, (float) view.x, (float) view.y, (float) view.z)
                .endVertex();
        pBuffer.vertex(matrix4f, (float) (origin.x + view.x * 2), (float) (origin.y + view.y * 2), (float) (origin.z + view.z * 2)).color(0, 0, 255, 255).normal(matrix3f,
            (float) view.x, (float) view.y, (float) view.z).endVertex();
    }
    
    @SubscribeEvent
    public void cameraRoll(ComputeCameraAngles event) {
        event.setRoll(roll);
    }
    
    @SubscribeEvent
    public void onPlayerInteract(EntityInteract event) {
        interact(event);
    }
    
    @SubscribeEvent
    public void onPlayerInteract(RightClickBlock event) {
        interact(event);
    }
    
    public void interact(PlayerInteractEvent event) {
        if (selectingTarget == null || !event.getLevel().isClientSide)
            return;
        
        if (event instanceof EntityInteract) {
            selectingTarget.accept(new CamTarget.EntityTarget(((EntityInteract) event).getTarget()));
            event.getEntity().sendSystemMessage(Component.translatable("scene.look.target.entity", ((EntityInteract) event).getTarget().getStringUUID()));
            selectingTarget = null;
        }
        
        if (event instanceof RightClickBlock) {
            selectingTarget.accept(new CamTarget.BlockTarget(event.getPos()));
            event.getEntity().sendSystemMessage(Component.translatable("scene.look.target.pos", event.getPos().toShortString()));
            selectingTarget = null;
        }
    }
    
    public static void setupMouseHandlerBefore() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            camera = MC.cameraEntity;
            MC.cameraEntity = MC.player;
        }
    }
    
    public static void setupMouseHandlerAfter() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            MC.cameraEntity = camera;
            camera = null;
        }
    }
}
