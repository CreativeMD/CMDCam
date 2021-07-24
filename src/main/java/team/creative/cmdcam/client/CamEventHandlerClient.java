package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.Iterator;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Vector3f;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.client.mode.CamMode;
import team.creative.cmdcam.client.mode.OutsideMode;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vec3d;

public class CamEventHandlerClient {
    
    public static Minecraft mc = Minecraft.getInstance();
    
    public static final float amountZoom = 0.1F;
    public static final float amountroll = 0.5F;
    
    public static double defaultFOV = 70;
    public static double currentFOV;
    public static float roll = 0;
    
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (mc.level == null)
            CMDCamClient.isInstalledOnSever = false;
        if (event.phase == Phase.END)
            return;
        
        if (currentFOV != mc.options.fov) {
            currentFOV = defaultFOV = mc.options.fov;
        }
        
        if (mc.player != null && mc.level != null) {
            if (!mc.isPaused()) {
                if (CMDCamClient.getCurrentPath() == null) {
                    if (KeyHandler.zoomIn.isDown()) {
                        if (mc.player.isCrouching())
                            currentFOV -= amountZoom * 10;
                        else
                            currentFOV -= amountZoom;
                    }
                    
                    if (KeyHandler.zoomOut.isDown()) {
                        if (mc.player.isCrouching())
                            currentFOV += amountZoom * 10;
                        else
                            currentFOV += amountZoom;
                    }
                    
                    if (KeyHandler.zoomCenter.isDown())
                        currentFOV = defaultFOV;
                    
                    if (KeyHandler.rollLeft.isDown())
                        roll -= amountroll;
                    
                    if (KeyHandler.rollRight.isDown())
                        roll += amountroll;
                    
                    if (KeyHandler.rollCenter.isDown())
                        roll = 0;
                    
                    if (KeyHandler.pointKey.consumeClick()) {
                        CMDCamClient.points.add(new CamPoint());
                        mc.player.sendMessage(new TextComponent("Registered " + CMDCamClient.points.size() + ". Point!"), Util.NIL_UUID);
                    }
                    
                } else {
                    CMDCamClient.tickPath(mc.level, event.renderTickTime);
                }
                
                if (KeyHandler.startStop.consumeClick()) {
                    if (CMDCamClient.getCurrentPath() != null) {
                        CMDCamClient.stopPath();
                    } else
                        try {
                            CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                        } catch (PathParseException e) {
                            mc.player.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
                        }
                }
                
                while (KeyHandler.clearPoint.consumeClick()) {
                    CMDCamClient.points.clear();
                    mc.player.sendMessage(new TextComponent("Cleared all registered points!"), Util.NIL_UUID);
                }
            }
            mc.options.fov = currentFOV;
        }
        lastRenderTime = System.nanoTime();
    }
    
    @SubscribeEvent
    public void worldRender(RenderWorldLastEvent event) {
        boolean shouldRender = false;
        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values()) {
            if (movement.isRenderingEnabled) {
                shouldRender = true;
                break;
            }
        }
        if (CMDCamClient.getCurrentPath() == null && shouldRender && CMDCamClient.points.size() > 0) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(770, 771, 1, 0);
            RenderSystem.disableTexture();
            RenderSystem.depthMask(false);
            RenderSystem.enableDepthTest();
            
            Vector3f view = mc.gameRenderer.getMainCamera().getLookVector();
            
            for (int i = 0; i < CMDCamClient.points.size(); i++) {
                CamPoint point = CMDCamClient.points.get(i);
                
                PoseStack mat = event.getMatrixStack();
                mat.pushPose();
                mat.translate(-view.x(), -view.y(), -view.z());
                
                RenderSystem.setProjectionMatrix(mat.last().pose());
                DebugRenderer.renderFloatingText((i + 1) + "", point.x + view.x(), point.y + 0.2 + view.y(), point.z + view.z(), -1);
                DebugRenderer.renderFilledBox(point.x - 0.05, point.y - 0.05, point.z - 0.05, point.x + 0.05, point.y + 0.05, point.z + 0.05, 1, 1, 1, 1);
                
                RenderSystem.depthMask(false);
                RenderSystem.disableTexture();
                mat.popPose();
            }
            
            for (Iterator<CamInterpolation> iterator = CamInterpolation.interpolationTypes.values().iterator(); iterator.hasNext();) {
                CamInterpolation movement = iterator.next();
                if (movement.isRenderingEnabled)
                    renderMovement(event.getMatrixStack(), movement, new ArrayList<>(CMDCamClient.points));
            }
            
            RenderSystem.depthMask(true);
            RenderSystem.enableTexture();
            RenderSystem.enableBlend();
            
        }
    }
    
    public void renderMovement(PoseStack mat, CamInterpolation movement, ArrayList<CamPoint> points) {
        try {
            movement.initMovement(points, 0, CMDCamClient.target);
        } catch (PathParseException e) {
            return;
        }
        
        double steps = 20 * (points.size() - 1);
        Vector3f view = mc.gameRenderer.getMainCamera().getLookVector();
        
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        
        mat.pushPose();
        RenderSystem.lineWidth(1.0F);
        mat.translate(-view.x(), -view.y(), -view.z());
        Vec3d color = movement.getColor();
        bufferbuilder.begin(Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        
        for (int i = 0; i < steps; i++) {
            CamPoint pos = CamMode.getPoint(movement, points, i / steps, 0, 0);
            bufferbuilder.vertex(mat.last().pose(), (float) pos.x, (float) pos.y, (float) pos.z).color((float) color.x, (float) color.y, (float) color.z, 1).endVertex();
        }
        bufferbuilder.vertex(mat.last().pose(), (float) points.get(points.size() - 1).x, (float) points.get(points.size() - 1).y, (float) points.get(points.size() - 1).z)
                .color((float) color.x, (float) color.y, (float) color.z, 1).endVertex();
        
        tessellator.end();
        mat.popPose();
    }
    
    @SubscribeEvent
    public void cameraRoll(CameraSetup event) {
        event.setRoll(roll);
    }
    
    public static long lastRenderTime;
    
    public static boolean isCurrentViewEntity() {
        if (isPathActive())
            return true;
        return mc.cameraEntity == mc.player;
    }
    
    public static boolean isPathActive() {
        return CMDCamClient.getCurrentPath() != null;
    }
    
    public static boolean selectEntityMode = false;
    
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!selectEntityMode || !event.getWorld().isClientSide)
            return;
        
        if (event instanceof EntityInteract) {
            CMDCamClient.target = new CamTarget.EntityTarget(((EntityInteract) event).getTarget());
            event.getPlayer().sendMessage(new TextComponent("Target is set to " + ((EntityInteract) event).getTarget().getStringUUID() + "."), Util.NIL_UUID);
            selectEntityMode = false;
        }
        
        if (event instanceof RightClickBlock) {
            CMDCamClient.target = new CamTarget.BlockTarget(event.getPos());
            event.getPlayer().sendMessage(new TextComponent("Target is set to " + event.getPos() + "."), Util.NIL_UUID);
            selectEntityMode = false;
        }
    }
    
    public static Entity camera = null;
    
    public static void setupMouseHandlerBefore() {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            camera = mc.cameraEntity;
            mc.cameraEntity = mc.player;
        }
    }
    
    public static void setupMouseHandlerAfter() {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            mc.cameraEntity = camera;
            camera = null;
        }
    }
}
