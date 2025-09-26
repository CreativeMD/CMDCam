package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeFov;
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

public class CamEventHandlerClient {
    
    private static void renderHitbox(PoseStack pMatrixStack, VertexConsumer pBuffer, AABB aabb, float eyeHeight, Vec3d origin, Vec3d view) {
        ShapeRenderer.renderLineBox(pMatrixStack, pBuffer, aabb, 1.0F, 1.0F, 1.0F, 1.0F);
        float f = 0.01F;
        ShapeRenderer.renderLineBox(pMatrixStack, pBuffer, aabb.minX, aabb.minY + (eyeHeight - f), aabb.minZ, aabb.maxX, aabb.minY + (eyeHeight + f), aabb.maxZ, 1.0F, 0.0F, 0.0F,
            1.0F);
        
        Matrix4f matrix4f = pMatrixStack.last().pose();
        pBuffer.addVertex(matrix4f, (float) origin.x, (float) origin.y, (float) origin.z).setColor(0, 0, 255, 255).setNormal(pMatrixStack.last(), (float) view.x, (float) view.y,
            (float) view.z);
        pBuffer.addVertex(matrix4f, (float) (origin.x + view.x * 2), (float) (origin.y + view.y * 2), (float) (origin.z + view.z * 2)).setColor(0, 0, 255, 255).setNormal(
            pMatrixStack.last(), (float) view.x, (float) view.y, (float) view.z);
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
    
    public static final Minecraft MC = Minecraft.getInstance();
    
    public static final double ZOOM_STEP = 0.005;
    public static final float ROLL_STEP = 1.5F;
    public static final double MAX_FOV = 170;
    public static final double MIN_FOV = 0.1;
    public static final double FOV_RANGE = MAX_FOV - MIN_FOV;
    public static final double FOV_RANGE_HALF = FOV_RANGE / 2;
    
    public static Entity camera = null;
    
    public static boolean SHOW_ACTIVE_INTERPOLATION = false;
    
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
    public void onClientTick(ClientTickEvent.Pre event) {
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
    public void onRenderTick(RenderFrameEvent.Pre event) {
        if (MC.level == null) {
            CMDCamClient.resetServerAvailability();
            CMDCamClient.resetTargetMarker();
        }
        
        renderingHand = false;
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        
        if (MC.player != null && MC.level != null) {
            if (!MC.isPaused()) {
                if (CMDCamClient.isPlaying()) {
                    while (MC.options.keyJump.consumeClick()) {
                        if (CMDCamClient.isPlaying() && !CMDCamClient.getScene().mode.outside())
                            CMDCamClient.getScene().togglePause();
                    }
                    
                    CMDCamClient.renderTickPath(MC.level, partialTicks);
                } else {
                    CMDCamClient.noTickPath(MC.level, partialTicks);
                    double timeFactor = event.getPartialTick().getRealtimeDeltaTicks();
                    double vanillaFov = fovExactVanilla(partialTicks);
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
                                MC.player.displayClientMessage(Component.translatable("scene.follow.no_marker", CMDCamClient.getPoints().size()), false);
                                continue;
                            }
                            point.sub(vec);
                        }
                        CMDCamClient.getPoints().add(point);
                        MC.player.displayClientMessage(Component.translatable("scene.add", CMDCamClient.getPoints().size()), false);
                    }
                }
                
                if (KeyHandler.startStop.consumeClick()) {
                    if (CMDCamClient.isPlaying())
                        CMDCamClient.stop();
                    else
                        try {
                            CMDCamClient.start(CMDCamClient.createScene());
                        } catch (SceneException e) {
                            MC.player.displayClientMessage(Component.translatable(e.getMessage()), false);
                        }
                }
                
                while (KeyHandler.clearPoint.consumeClick()) {
                    CMDCamClient.getPoints().clear();
                    MC.player.displayClientMessage(Component.translatable("scene.clear"), false);
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
                event.setFOV((float) fov);
            else
                event.setFOV((float) (event.getFOV() + fov));
            event.setFOV((float) Mth.clamp(event.getFOV(), MIN_FOV, MAX_FOV));
        }
        renderingHand = !renderingHand;
    }
    
    @SubscribeEvent
    public void worldRender(RenderLevelStageEvent.AfterEntities event) {
        
        /*RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();*/
        
        Vec3 view = MC.gameRenderer.getMainCamera().getPosition();
        
        //RenderSystem.setProjectionMatrix(event.getProjectionMatrix(), ProjectionType.ORTHOGRAPHIC);
        PoseStack pose = event.getPoseStack();
        
        pose.pushPose();
        pose.translate((float) -view.x(), (float) -view.y(), (float) -view.z());
        
        //RenderSystem.depthMask(false);
        
        if (CMDCamClient.hasTargetMarker()) {
            CamPoint point = CMDCamClient.getTargetMarker();
            renderHitbox(pose, MC.renderBuffers().bufferSource().getBuffer(RenderType.lines()),
                new AABB(point.x - 0.3, point.y - 1.62, point.z - 0.3, point.x + 0.3, point.y + 0.18, point.z + 0.3), MC.player.getEyeHeight(), point, point.calculateViewVector());
        }
        
        boolean shouldRender = false;
        for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
            if (movement.isRenderingEnabled || (SHOW_ACTIVE_INTERPOLATION && movement == CMDCamClient.getConfigScene().interpolation)) {
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
                
                DebugRenderer.renderFilledBox(pose, MC.renderBuffers().bufferSource(), point.x - 0.05, point.y - 0.05, point.z - 0.05, point.x + 0.05, point.y + 0.05,
                    point.z + 0.05, 1, 1, 1, 1);
                DebugRenderer.renderFloatingText(pose, MC.renderBuffers().bufferSource(), (i + 1) + "", point.x + view.x, point.y + 0.2 + view.y, point.z + view.z, -1);
                
                //RenderSystem.depthMask(false);
            }
            
            MC.renderBuffers().bufferSource().endLastBatch();
            
            try {
                pose.pushPose();
                //if (CMDCamClient.hasTargetMarker())
                //mat.translate(CMDCamClient.getTargetMarker().x, CMDCamClient.getTargetMarker().y, CMDCamClient.getTargetMarker().z);
                CamScene scene = CMDCamClient.createScene();
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    if (movement.isRenderingEnabled || (SHOW_ACTIVE_INTERPOLATION && movement == CMDCamClient.getConfigScene().interpolation))
                        renderPath(pose, movement, scene);
                    
                pose.popPose();
            } catch (SceneException e) {}
            
        }
        
        pose.popPose();
        
        //RenderSystem.depthMask(true);
        //RenderSystem.enableBlend();
        
    }
    
    public void renderPath(PoseStack mat, CamInterpolation inter, CamScene scene) {
        double steps = 20 * (scene.points.size() - 1);
        /*RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);*/
        
        var bufferbuilder = MC.renderBuffers().bufferSource().getBuffer(RenderType.debugLineStrip(1));
        //BufferBuilder bufferbuilder = tessellator.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        RenderSystem.lineWidth(1.0F);
        Vec3d color = inter.color.toVec();
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
            bufferbuilder.addVertex(mat.last(), (float) pos.x, (float) pos.y, (float) pos.z).setColor((float) color.x, (float) color.y, (float) color.z, 1);
        }
        Vec3d last = interpolation.valueAt(1);
        if (CMDCamClient.hasTargetMarker())
            last.add(CMDCamClient.getTargetMarker());
        bufferbuilder.addVertex(mat.last(), (float) last.x, (float) last.y, (float) last.z).setColor((float) color.x, (float) color.y, (float) color.z, 1);
        
        //BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        
        if (scene.lookTarget != null)
            scene.lookTarget.finish();
        if (scene.posTarget != null)
            scene.posTarget.finish();
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
            event.getEntity().displayClientMessage(Component.translatable("scene.look.target.entity", ((EntityInteract) event).getTarget().getStringUUID()), false);
            selectingTarget = null;
        }
        
        if (event instanceof RightClickBlock) {
            selectingTarget.accept(new CamTarget.BlockTarget(event.getPos()));
            event.getEntity().displayClientMessage(Component.translatable("scene.look.target.pos", event.getPos().toShortString()), false);
            selectingTarget = null;
        }
    }
    
}
