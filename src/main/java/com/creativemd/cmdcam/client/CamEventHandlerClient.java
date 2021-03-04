package com.creativemd.cmdcam.client;

import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.client.mode.CamMode;
import com.creativemd.cmdcam.client.mode.OutsideMode;
import com.creativemd.cmdcam.common.packet.SelectTargetPacket;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.math.vec.Vec3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class CamEventHandlerClient {
    
    public static Minecraft mc = Minecraft.getMinecraft();
    public static float defaultfov = 70.0F;
    public static float lastFOV = 0;
    public static final float amountZoom = 0.1F;
    public static final float amountroll = 0.5F;
    
    public static float fov;
    public static float roll = 0;
    
    @SubscribeEvent
    public void onRenderTick(RenderTickEvent event) {
        if (mc.world == null)
            CMDCamClient.isInstalledOnSever = false;
        
        if (lastFOV != 0 && lastFOV != mc.gameSettings.fovSetting)
            defaultfov = mc.gameSettings.fovSetting;
        
        if (mc.player != null && mc.world != null) {
            if (mc.inGameHasFocus) {
                if (CMDCamClient.getCurrentPath() == null) {
                    if (mc.gameSettings.isKeyDown(KeyHandler.zoomIn)) {
                        if (mc.player.isSneaking())
                            mc.gameSettings.fovSetting -= amountZoom * 10;
                        else
                            mc.gameSettings.fovSetting -= amountZoom;
                    }
                    
                    if (mc.gameSettings.isKeyDown(KeyHandler.zoomOut)) {
                        if (mc.player.isSneaking())
                            mc.gameSettings.fovSetting += amountZoom * 10;
                        else
                            mc.gameSettings.fovSetting += amountZoom;
                    }
                    
                    if (mc.gameSettings.isKeyDown(KeyHandler.zoomCenter)) {
                        mc.gameSettings.fovSetting = defaultfov;
                    }
                    fov = mc.gameSettings.fovSetting;
                    
                    if (mc.gameSettings.isKeyDown(KeyHandler.rollLeft))
                        roll -= amountroll;
                    
                    if (mc.gameSettings.isKeyDown(KeyHandler.rollRight))
                        roll += amountroll;
                    
                    if (mc.gameSettings.isKeyDown(KeyHandler.rollCenter))
                        roll = 0;
                    
                    if (KeyHandler.pointKey.isPressed()) {
                        CMDCamClient.points.add(new CamPoint());
                        mc.player.sendMessage(new TextComponentString("Registered " + CMDCamClient.points.size() + ". Point!"));
                    }
                    
                } else {
                    CMDCamClient.tickPath(mc.world, event.renderTickTime);
                }
                
                if (KeyHandler.startStop.isPressed()) {
                    if (CMDCamClient.getCurrentPath() != null) {
                        CMDCamClient.stopPath();
                    } else
                        try {
                            CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                        } catch (PathParseException e) {
                            mc.player.sendMessage(new TextComponentString(e.getMessage()));
                        }
                }
                
                while (KeyHandler.clearPoint.isPressed()) {
                    CMDCamClient.points.clear();
                    mc.player.sendMessage(new TextComponentString("Cleared all registered points!"));
                }
            }
        }
        lastFOV = mc.gameSettings.fovSetting;
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
            GlStateManager.enableBlend();
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            GlStateManager.disableTexture2D();
            GL11.glDepthMask(false);
            
            Vec3[] points = new Vec3[CMDCamClient.points.size()];
            for (int i = 0; i < points.length; i++) {
                points[i] = new Vec3(CMDCamClient.points.get(i).x, CMDCamClient.points.get(i).y, CMDCamClient.points.get(i).z);
                GlStateManager.pushMatrix();
                GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player
                    .getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
                renderBlock(points[i].x, points[i].y, points[i].z, 0.1, 0.1, 0.1, 0, 0, 0, 1, 1, 1, 1);
                float f = TileEntityRendererDispatcher.instance.entityYaw;
                float f1 = TileEntityRendererDispatcher.instance.entityPitch;
                boolean flag = false;
                EntityRenderer.drawNameplate(mc.fontRenderer, (i + 1) + "", (float) points[i].x, (float) points[i].y + 0.4F, (float) points[i].z, 0, f, f1, false, false);
                GL11.glDepthMask(false);
                GlStateManager.disableLighting();
                GlStateManager.disableTexture2D();
                GlStateManager.popMatrix();
            }
            
            for (Iterator<CamInterpolation> iterator = CamInterpolation.interpolationTypes.values().iterator(); iterator.hasNext();) {
                CamInterpolation movement = iterator.next();
                if (movement.isRenderingEnabled)
                    renderMovement(movement, new ArrayList<>(CMDCamClient.points));
            }
            
            GL11.glDepthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.resetColor();
            
        }
    }
    
    public static void renderBlock(double x, double y, double z, double width, double height, double length, double rotateX, double rotateY, double rotateZ, double red, double green, double blue, double alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.rotate((float) rotateX, 1, 0, 0);
        GlStateManager.rotate((float) rotateY, 0, 1, 0);
        GlStateManager.rotate((float) rotateZ, 0, 0, 1);
        GlStateManager.scale(width, height, length);
        GlStateManager.color((float) red, (float) green, (float) blue, (float) alpha);
        
        GlStateManager.glBegin(GL11.GL_POLYGON);
        GlStateManager.glNormal3f(0.0f, 1.0f, 0.0f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(0.5f, 0.5f, -0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, -0.5f);
        GlStateManager.glEnd();
        
        GL11.glBegin(GL11.GL_POLYGON);
        //GL11.glColor4d(red, green, blue, alpha);
        GlStateManager.glNormal3f(0.0f, 0.0f, 1.0f);
        GlStateManager.glVertex3f(0.5f, -0.5f, 0.5f);
        GlStateManager.glVertex3f(0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, 0.5f);
        GlStateManager.glEnd();
        
        GL11.glBegin(GL11.GL_POLYGON);
        //GL11.glColor4d(red, green, blue, alpha);
        GlStateManager.glNormal3f(1.0f, 0.0f, 0.0f);
        GlStateManager.glVertex3f(0.5f, 0.5f, -0.5f);
        GlStateManager.glVertex3f(0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(0.5f, -0.5f, 0.5f);
        GlStateManager.glVertex3f(0.5f, -0.5f, -0.5f);
        GlStateManager.glEnd();
        
        GL11.glBegin(GL11.GL_POLYGON);
        //GL11.glColor4d(red, green, blue, alpha);
        GlStateManager.glNormal3f(-1.0f, 0.0f, 0.0f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, -0.5f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, -0.5f);
        GlStateManager.glEnd();
        
        GL11.glBegin(GL11.GL_POLYGON);
        //GL11.glColor4d(red, green, blue, alpha);
        GlStateManager.glNormal3f(0.0f, -1.0f, 0.0f);
        GlStateManager.glVertex3f(0.5f, -0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, 0.5f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, -0.5f);
        GlStateManager.glVertex3f(0.5f, -0.5f, -0.5f);
        GlStateManager.glEnd();
        
        GL11.glBegin(GL11.GL_POLYGON);
        //GL11.glColor4d(red, green, blue, alpha);
        GlStateManager.glNormal3f(0.0f, 0.0f, -1.0f);
        GlStateManager.glVertex3f(0.5f, 0.5f, -0.5f);
        GlStateManager.glVertex3f(0.5f, -0.5f, -0.5f);
        GlStateManager.glVertex3f(-0.5f, -0.5f, -0.5f);
        GlStateManager.glVertex3f(-0.5f, 0.5f, -0.5f);
        GlStateManager.glEnd();
        
        GlStateManager.popMatrix();
    }
    
    public void renderMovement(CamInterpolation movement, ArrayList<CamPoint> points) {
        try {
            movement.initMovement(points, 0, CMDCamClient.target);
        } catch (PathParseException e) {
            return;
        }
        
        double steps = 20 * (points.size() - 1);
        
        GlStateManager.pushMatrix();
        Vec3 color = movement.getColor();
        GL11.glColor3d(color.x, color.y, color.z);
        GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player
            .getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < steps; i++) {
            CamPoint pos = CamMode.getPoint(movement, points, i / steps, 0, 0);
            GL11.glVertex3d(pos.x, pos.y, pos.z);
        }
        GL11.glVertex3d(points.get(points.size() - 1).x, points.get(points.size() - 1).y, points.get(points.size() - 1).z);
        GlStateManager.glEnd();
        GlStateManager.popMatrix();
    }
    
    public Entity renderEntity;
    
    @SubscribeEvent
    public void renderPlayerPre(RenderPlayerEvent.Pre event) {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            renderEntity = mc.getRenderManager().renderViewEntity;
            
            mc.getRenderManager().renderViewEntity = mc.player;
        }
    }
    
    @SubscribeEvent
    public void renderPlayerPost(RenderPlayerEvent.Post event) {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            mc.getRenderManager().renderViewEntity = renderEntity;
        }
    }
    
    @SubscribeEvent
    public void cameraRoll(CameraSetup event) {
        event.setRoll(roll);
    }
    
    public static long lastRenderTime;
    
    public static boolean shouldPlayerTakeInput() {
        return true;
    }
    
    public static void startSelectingTarget(String serverPath) {
        selectEntityMode = true;
        CamEventHandlerClient.serverPath = serverPath;
        mc.player.sendMessage(new TextComponentString("Please select a target either an entity or a block!"));
    }
    
    private static boolean selectEntityMode = false;
    private static String serverPath = null;
    
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!selectEntityMode)
            return;
        
        if (event instanceof EntityInteract) {
            if (serverPath != null)
                PacketHandler.sendPacketToServer(new SelectTargetPacket(serverPath, new CamTarget.EntityTarget(((EntityInteract) event).getTarget())));
            else
                CMDCamClient.target = new CamTarget.EntityTarget(((EntityInteract) event).getTarget());
            event.getEntityPlayer().sendMessage(new TextComponentString("Target is set to " + ((EntityInteract) event).getTarget().getCachedUniqueIdString() + "."));
            selectEntityMode = false;
            serverPath = null;
        }
        
        if (event instanceof RightClickBlock) {
            if (serverPath != null)
                PacketHandler.sendPacketToServer(new SelectTargetPacket(serverPath, new CamTarget.BlockTarget(event.getPos())));
            else
                CMDCamClient.target = new CamTarget.BlockTarget(event.getPos());
            event.getEntityPlayer().sendMessage(new TextComponentString("Target is set to " + event.getPos() + "."));
            selectEntityMode = false;
            serverPath = null;
        }
    }
}
