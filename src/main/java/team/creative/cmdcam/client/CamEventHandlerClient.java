package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
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
import team.creative.cmdcam.client.mixin.CameraAccessor;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.scene.mode.OutsideMode;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.mc.ColorUtils;

public class CamEventHandlerClient {
    
    private static void renderHitbox(AABB aabb, float eyeHeight, Vec3d origin, Vec3d view) {
        float f = 0.01F;
        Gizmos.cuboid(aabb, GizmoStyle.stroke(ColorUtils.WHITE));
        Gizmos.cuboid(new AABB(aabb.minX, aabb.minY + (eyeHeight - f), aabb.minZ, aabb.maxX, aabb.minY + (eyeHeight + f), aabb.maxZ), GizmoStyle.stroke(ColorUtils.WHITE));
        
        //Gizmos.line(origin.toVanilla(), view.toVanilla().scale(2).add(origin.toVanilla()), 0);
    }
    
    public static void setupMouseHandlerBefore() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            camera = MC.getCameraEntity();
            MC.setCameraEntity(MC.player);
        }
    }
    
    public static void setupMouseHandlerAfter() {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode instanceof OutsideMode) {
            MC.setCameraEntity(camera);
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
            return ((CameraAccessor) MC.gameRenderer.mainCamera()).callCalculateFov(partialTickTime);
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
                    
                    if (KeyHandler.ZOOM_IN.isDown())
                        fov = transformFov(multiplier * timeFactor * -ZOOM_STEP + x) - vanillaFov;
                    
                    if (KeyHandler.ZOOM_OUT.isDown())
                        fov = transformFov(multiplier * timeFactor * ZOOM_STEP + x) - vanillaFov;
                    
                    if (KeyHandler.ZOOM_RESET.isDown())
                        resetFOV();
                    
                    if (KeyHandler.ROLL_LEFT.isDown())
                        roll -= timeFactor * ROLL_STEP;
                    
                    if (KeyHandler.ROLL_RIGHT.isDown())
                        roll += timeFactor * ROLL_STEP;
                    
                    if (KeyHandler.ROLL_RESET.isDown())
                        resetRoll();
                    
                    while (KeyHandler.POINT_ADD.consumeClick()) {
                        CamPoint point = CMDCamClient.createLocalPoint();
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
                
                if (KeyHandler.START_STOP.consumeClick()) {
                    if (CMDCamClient.isPlaying())
                        CMDCamClient.stop();
                    else
                        try {
                            CMDCamClient.start(CMDCamClient.createScene());
                        } catch (SceneException e) {
                            MC.player.sendSystemMessage(Component.translatable(e.getMessage()));
                        }
                }
                
                while (KeyHandler.CLEAR_POINT.consumeClick()) {
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
                event.setFOV((float) fov);
            else
                event.setFOV((float) (event.getFOV() + fov));
            event.setFOV((float) Mth.clamp(event.getFOV(), MIN_FOV, MAX_FOV));
        }
        renderingHand = !renderingHand;
    }
    
    @SubscribeEvent
    public void worldRender(RenderLevelStageEvent.AfterLevel event) {
        Vec3 view = MC.gameRenderer.mainCamera().position();
        
        PoseStack pose = event.getPoseStack();
        
        pose.pushPose();
        pose.translate((float) -view.x(), (float) -view.y(), (float) -view.z());
        
        if (CMDCamClient.hasTargetMarker()) {
            CamPoint point = CMDCamClient.getTargetMarker();
            renderHitbox(new AABB(point.x - 0.3, point.y - 1.62, point.z - 0.3, point.x + 0.3, point.y + 0.18, point.z + 0.3), MC.player.getEyeHeight(), point, point
                    .calculateViewVector());
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
                
                Gizmos.cuboid(new AABB(point.x - 0.05, point.y - 0.05, point.z - 0.05, point.x + 0.05, point.y + 0.05, point.z + 0.05), GizmoStyle.fill(ColorUtils.WHITE));
                Gizmos.billboardText((i + 1) + "", new Vec3(point.x, point.y + 0.2, point.z), TextGizmo.Style.forColor(ColorUtils.WHITE));
            }
            
            //MC.renderBuffers().bufferSource().endLastBatch();
            
            try {
                pose.pushPose();
                CamScene scene = CMDCamClient.createScene();
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    if (movement.isRenderingEnabled || (SHOW_ACTIVE_INTERPOLATION && movement == CMDCamClient.getConfigScene().interpolation))
                        renderPath(pose, movement, scene);
                    
                pose.popPose();
            } catch (SceneException e) {}
            
        }
        
        pose.popPose();
        
    }
    
    public void renderPath(PoseStack mat, CamInterpolation inter, CamScene scene) {
        double steps = 20 * (scene.points.size() - 1);
        int color = inter.color.toInt();
        CamPoints points = new CamPoints(scene.points);
        
        if (scene.lookTarget != null)
            scene.lookTarget.start(MC.level);
        if (scene.posTarget != null)
            scene.posTarget.start(MC.level);
        
        double[] times = points.createTimes(scene);
        Interpolation<Vec3d> interpolation = inter.create(times, scene, null, new ArrayList<Vec3d>(scene.points), null, CamAttribute.POSITION);
        Vec3 previous = null;
        for (int i = 0; i < steps; i++) {
            Vec3d pos = interpolation.valueAt(i / steps);
            if (CMDCamClient.hasTargetMarker())
                pos.add(CMDCamClient.getTargetMarker());
            var vec = pos.toVanilla();
            if (previous != null)
                Gizmos.line(previous, vec, color, 1);
            previous = vec;
        }
        Vec3d last = interpolation.valueAt(1);
        if (CMDCamClient.hasTargetMarker())
            last.add(CMDCamClient.getTargetMarker());
        
        if (previous != null)
            Gizmos.line(previous, last.toVanilla(), color, 1);
        
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
        if (selectingTarget == null || !event.getLevel().isClientSide())
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
    
}
