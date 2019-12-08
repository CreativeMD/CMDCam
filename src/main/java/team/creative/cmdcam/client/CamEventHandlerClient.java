package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.client.mode.CamMode;
import team.creative.cmdcam.client.mode.OutsideMode;
import team.creative.cmdcam.common.utils.CamPoint;
import team.creative.cmdcam.common.utils.CamTarget;
import team.creative.cmdcam.common.utils.vec.Vec3;

public class CamEventHandlerClient {
	
	public static Minecraft mc = Minecraft.getInstance();
	public static float defaultfov = 70.0F;
	public static final float amountZoom = 0.1F;
	public static final float amountroll = 0.5F;
	
	public static double fov;
	public static float roll = 0;
	
	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		if (mc.world == null)
			CMDCamClient.isInstalledOnSever = false;
		
		if (mc.player != null && mc.world != null) {
			if (!mc.isGamePaused()) {
				if (CMDCamClient.getCurrentPath() == null) {
					if (KeyHandler.zoomIn.isKeyDown()) {
						if (mc.player.isSneaking())
							mc.gameSettings.fov -= amountZoom * 10;
						else
							mc.gameSettings.fov -= amountZoom;
					}
					
					if (KeyHandler.zoomOut.isKeyDown()) {
						if (mc.player.isSneaking())
							mc.gameSettings.fov += amountZoom * 10;
						else
							mc.gameSettings.fov += amountZoom;
					}
					
					if (KeyHandler.zoomCenter.isKeyDown()) {
						mc.gameSettings.fov = defaultfov;
					}
					fov = mc.gameSettings.fov;
					
					if (KeyHandler.rollLeft.isKeyDown())
						roll -= amountroll;
					
					if (KeyHandler.rollRight.isKeyDown())
						roll += amountroll;
					
					if (KeyHandler.rollCenter.isKeyDown())
						roll = 0;
					
					if (KeyHandler.pointKey.isPressed()) {
						CMDCamClient.points.add(new CamPoint());
						mc.player.sendMessage(new StringTextComponent("Registered " + CMDCamClient.points.size() + ". Point!"));
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
							mc.player.sendMessage(new StringTextComponent(e.getMessage()));
						}
				}
			}
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
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(770, 771, 1, 0);
			GlStateManager.disableTexture();
			GL11.glDepthMask(false);
			
			Vec3[] points = new Vec3[CMDCamClient.points.size()];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Vec3(CMDCamClient.points.get(i).x, CMDCamClient.points.get(i).y, CMDCamClient.points.get(i).z);
				GlStateManager.pushMatrix();
				GlStateManager.translated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player.getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
				renderBlock(points[i].x, points[i].y, points[i].z, 0.1, 0.1, 0.1, 0, 0, 0, 1, 1, 1, 1);
				ActiveRenderInfo activerenderinfo = TileEntityRendererDispatcher.instance.renderInfo;
				float f = activerenderinfo.getYaw();
				float f1 = activerenderinfo.getPitch();
				GameRenderer.drawNameplate(mc.fontRenderer, (i + 1) + "", (float) points[i].x, (float) points[i].y + 0.4F, (float) points[i].z, 0, f, f1, false);
				GL11.glDepthMask(false);
				GlStateManager.disableLighting();
				GlStateManager.disableTexture();
				GlStateManager.popMatrix();
			}
			
			for (Iterator<CamInterpolation> iterator = CamInterpolation.interpolationTypes.values().iterator(); iterator.hasNext();) {
				CamInterpolation movement = iterator.next();
				if (movement.isRenderingEnabled)
					renderMovement(movement, new ArrayList<>(CMDCamClient.points));
			}
			
			GL11.glDepthMask(true);
			GlStateManager.enableTexture();
			GlStateManager.enableBlend();
			GlStateManager.clearCurrentColor();
			
		}
	}
	
	public static void renderBlock(double x, double y, double z, double width, double height, double length, double rotateX, double rotateY, double rotateZ, double red, double green, double blue, double alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translated(x, y, z);
		GlStateManager.enableRescaleNormal();
		GlStateManager.rotatef((float) rotateX, 1, 0, 0);
		GlStateManager.rotatef((float) rotateY, 0, 1, 0);
		GlStateManager.rotatef((float) rotateZ, 0, 0, 1);
		GlStateManager.scaled(width, height, length);
		GlStateManager.color4f((float) red, (float) green, (float) blue, (float) alpha);
		
		GL11.glBegin(GL11.GL_POLYGON);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, 0.0f, 1.0f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(1.0f, 0.0f, 0.0f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(-1.0f, 0.0f, 0.0f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, -1.0f, 0.0f);
		GL11.glVertex3f(0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, 0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, 0.0f, -1.0f);
		GL11.glVertex3f(0.5f, 0.5f, -0.5f);
		GL11.glVertex3f(0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, -0.5f, -0.5f);
		GL11.glVertex3f(-0.5f, 0.5f, -0.5f);
		GL11.glEnd();
		
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
		GlStateManager.translated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY + mc.player.getEyeHeight() - 0.1, -TileEntityRendererDispatcher.staticPlayerZ);
		GlStateManager.lineWidth(1.0F);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		for (int i = 0; i < steps; i++) {
			CamPoint pos = CamMode.getPoint(movement, points, i / steps, 0, 0);
			GL11.glVertex3d(pos.x, pos.y, pos.z);
		}
		GL11.glVertex3d(points.get(points.size() - 1).x, points.get(points.size() - 1).y, points.get(points.size() - 1).z);
		GL11.glEnd();
		GlStateManager.popMatrix();
	}
	
	@SubscribeEvent
	public void cameraRoll(CameraSetup event) {
		event.setRoll(roll);
	}
	
	public static long lastRenderTime;
	
	public static boolean shouldPlayerTakeInput() {
		return true;
	}
	
	public static boolean selectEntityMode = false;
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!selectEntityMode)
			return;
		
		if (event instanceof EntityInteract) {
			CMDCamClient.target = new CamTarget.EntityTarget(((EntityInteract) event).getTarget());
			event.getPlayer().sendMessage(new StringTextComponent("Target is set to " + ((EntityInteract) event).getTarget().getCachedUniqueIdString() + "."));
			selectEntityMode = false;
		}
		
		if (event instanceof RightClickBlock) {
			CMDCamClient.target = new CamTarget.BlockTarget(event.getPos());
			event.getPlayer().sendMessage(new StringTextComponent("Target is set to " + event.getPos() + "."));
			selectEntityMode = false;
		}
	}
	
	public static Entity camera = null;
	
	public static void setupMouseHandlerBefore() {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			camera = mc.renderViewEntity;
			mc.renderViewEntity = mc.player;
			//mc.setRenderViewEntity(mc.player);
		}
	}
	
	public static void setupMouseHandlerAfter() {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			mc.renderViewEntity = camera;
			//mc.setRenderViewEntity(camera);
		}
	}
}
