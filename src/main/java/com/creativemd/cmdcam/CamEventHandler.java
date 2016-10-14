package com.creativemd.cmdcam;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.creativemd.cmdcam.key.KeyHandler;
import com.creativemd.cmdcam.movement.Movement;
import com.creativemd.cmdcam.movement.OutsidePath;
import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;
import com.creativemd.cmdcam.utils.interpolation.CubicInterpolation;
import com.creativemd.cmdcam.utils.interpolation.HermiteInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Interpolation;
import com.creativemd.cmdcam.utils.interpolation.LinearInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Vec3;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemNameTag;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class CamEventHandler {

	public static Minecraft mc = Minecraft.getMinecraft();
	public static float defaultfov = 70.0F;
	public static final float amountZoom = 0.1F;
	public static final float amountroll = 0.5F;
	
	public static boolean selectEntityMode = false;
	
	public static long lastRenderTime;
	
	@SubscribeEvent
	public void onPlayerEntityInteract(EntityInteractEvent event)
	{
		if(!selectEntityMode)
			return ;
		
		CMDCam.target = event.target;
		event.entityPlayer.addChatMessage(new ChatComponentText("Target is set to " + event.target.getUniqueID() + "."));
		selectEntityMode = false;
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if(!selectEntityMode)
			return ;
		
		if(event.action == Action.RIGHT_CLICK_BLOCK)
		{
			CMDCam.target = new ChunkCoordinates(event.x, event.y, event.z);
			event.entityPlayer.addChatMessage(new ChatComponentText("Target is set to " +  CMDCam.target + "."));
			selectEntityMode = false;
		}
	}
	
	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event)
	{
		if(mc.thePlayer != null && mc.theWorld != null)
		{
			if(mc.inGameHasFocus) //&& event.phase == Phase.START)
			{
				if(CMDCam.currentPath == null)
				{
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomIn))
					{
						if(mc.thePlayer.isSneaking())
							mc.gameSettings.fovSetting -= amountZoom*10;
						else
							mc.gameSettings.fovSetting -= amountZoom;
					}
					
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomOut))
					{
						if(mc.thePlayer.isSneaking())
							mc.gameSettings.fovSetting += amountZoom*10;
						else
							mc.gameSettings.fovSetting += amountZoom;
					}
					
					if(mc.gameSettings.isKeyDown(KeyHandler.zoomCenter))
					{
						mc.gameSettings.fovSetting = defaultfov;
					}
					CMDCam.fov = mc.gameSettings.fovSetting;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollLeft))
						CMDCam.roll -= amountroll;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollRight))
						CMDCam.roll += amountroll;
					
					if(mc.gameSettings.isKeyDown(KeyHandler.rollCenter))
						CMDCam.roll = 0;
					
					if(KeyHandler.pointKey.isPressed())
					{
						CMDCam.points.add(new CamPoint());
						mc.thePlayer.addChatMessage(new ChatComponentText("Registered " + CMDCam.points.size() + ". Point!"));
					}
					
					
				}else{
					CMDCam.currentPath.tick(event.renderTickTime);
				}
				
				ReflectionHelper.setPrivateValue(EntityRenderer.class, mc.entityRenderer, CMDCam.roll, "camRoll", "field_78495_O"); 
				if(KeyHandler.startStop.isPressed())
				{
					if(CMDCam.currentPath != null)
					{
						CMDCam.currentPath.onPathFinished();
						CMDCam.currentPath = null;
					}
					else
						CMDCam.createPath();
				}
			} 
		}
		lastRenderTime = System.nanoTime();
	}
	
	@SubscribeEvent
	public void worldRender(RenderWorldLastEvent event)
	{
		boolean shouldRender = false;
		for (Movement movement : Movement.movements.values()) {
			if(movement.isRenderingEnabled)
			{
				shouldRender = true;
				break;
			}
		}
		if(CMDCam.currentPath == null && shouldRender && CMDCam.points.size() > 2)
		{
			GL11.glEnable(GL11.GL_BLEND);
			//GlStateManager.enableBlend();
	        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
	        GL11.glDisable(GL11.GL_TEXTURE_2D);
	        //GlStateManager.disableTexture2D();
	        GL11.glDepthMask(false);
	        
			Vec3[] points = new Vec3[CMDCam.points.size()];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Vec3(CMDCam.points.get(i).x, CMDCam.points.get(i).y, CMDCam.points.get(i).z);
				GL11.glPushMatrix();
				//GlStateManager.pushMatrix();
				//GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
				GL11.glTranslated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
				renderBlock(points[i].x, points[i].y, points[i].z, 0.1, 0.1, 0.1, 0, 0, 0, 1, 1, 1, 1);
				float f = TileEntityRendererDispatcher.instance.field_147562_h;
	            float f1 = TileEntityRendererDispatcher.instance.field_147563_i;
	            boolean flag = false;
	            
	            renderTag((i+1) + "", (float)points[i].x, (float)points[i].y + 0.4F, (float)points[i].z, 1000);
	            //EntityRenderer.func_189692_a(mc.fontRenderer, (i+1) + "", (float)points[i].x, (float)points[i].y + 0.4F, (float)points[i].z, 0, f, f1, false, false);
	            GL11.glDepthMask(false);
	            GL11.glDisable(GL11.GL_LIGHTING);
	            //GlStateManager.disableLighting();
	            GL11.glDisable(GL11.GL_TEXTURE_2D);
	            //GlStateManager.disableTexture2D();
	            GL11.glPopMatrix();
	            //GlStateManager.popMatrix();
			}
	        
			if(Movement.hermite.isRenderingEnabled)
		        renderInterpolation(new HermiteInterpolation<>(points), new Vec3(1,1,1));
	        
			if(Movement.cubic.isRenderingEnabled)
		        renderInterpolation(new CubicInterpolation<>(points), new Vec3(1,0,0));
			
			if(Movement.cosine.isRenderingEnabled)
		        renderInterpolation(new CosineInterpolation<>(points), new Vec3(0,1,0));
			
			if(Movement.linear.isRenderingEnabled)
		        renderInterpolation(new LinearInterpolation<>(points), new Vec3(0,0,1));
	        
			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
            //GlStateManager.enableTexture2D();
			GL11.glEnable(GL11.GL_BLEND);
            //GlStateManager.enableBlend();
			
            //GlStateManager.resetColor();
            
			
		}
	}
	
	public static void renderTag(String text, double x, double y, double z, int distance)
    {
        double d3 = Math.sqrt(RenderManager.instance.getDistanceToCamera(x, y, z));

        //if (d3 <= (double)(distance * distance))
        //{
        FontRenderer fontrenderer = mc.fontRenderer;
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GL11.glPushMatrix();
        GL11.glTranslatef((float)x + 0.0F, (float)y, (float)z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-f1, -f1, f1);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.instance;
        byte b0 = 0;

        if (text.equals("deadmau5"))
        {
            b0 = -10;
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        int j = fontrenderer.getStringWidth(text) / 2;
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        tessellator.addVertex((double)(-j - 1), (double)(-1 + b0), 0.0D);
        tessellator.addVertex((double)(-j - 1), (double)(8 + b0), 0.0D);
        tessellator.addVertex((double)(j + 1), (double)(8 + b0), 0.0D);
        tessellator.addVertex((double)(j + 1), (double)(-1 + b0), 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        fontrenderer.drawString(text, -fontrenderer.getStringWidth(text) / 2, b0, 553648127);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        fontrenderer.drawString(text, -fontrenderer.getStringWidth(text) / 2, b0, -1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
        //}
    }
	
	public static void renderBlock(double x, double y, double z, double width, double height, double length, double rotateX, double rotateY, double rotateZ, double red, double green, double blue, double alpha)
	{
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glRotated(rotateX, 1, 0, 0);
		GL11.glRotated(rotateY, 0, 1, 0);
		GL11.glRotated(rotateZ, 0, 0, 1);
		GL11.glScaled(width, height, length);
		GL11.glColor4d(red, green, blue, alpha);
		
		GL11.glBegin(GL11.GL_POLYGON);
		//GL11.glColor4d(red, green, blue, alpha);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
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
		
		
        GL11.glPopMatrix();
	}
	
	public void renderInterpolation(Interpolation<Vec3> interpolation, Vec3 color)
	{
		double steps = 20*(interpolation.points.size()-1);
        
		GL11.glPushMatrix();
		//GlStateManager.pushMatrix();
		GL11.glColor3d(color.x, color.y, color.z);
		GL11.glTranslated(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
		//GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY+mc.thePlayer.getEyeHeight()-0.1, -TileEntityRendererDispatcher.staticPlayerZ);
		GL11.glLineWidth(1.0F);
		//GlStateManager.glLineWidth(1.0F);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		//GlStateManager.glBegin(GL11.GL_LINE_STRIP);
		for (int i = 0; i < steps; i++) {
			double t = i/(double)steps;
			//System.out.println("t=" + t);
			Vec3 pos = interpolation.valueAt(t);
			GL11.glVertex3d(pos.x, pos.y, pos.z);
		}
		Vec3 last = interpolation.points.get(interpolation.points.size()-1);
		GL11.glVertex3d(last.x, last.y, last.z);
		GL11.glEnd();
		//GlStateManager.glEnd();
		GL11.glPopMatrix();
		//GlStateManager.popMatrix();
	}
	
	public static boolean shouldPlayerTakeInput()
	{
		return true;
	}
	
	public Entity renderEntity;
	
	@SubscribeEvent
	public void renderPlayerPre(RenderPlayerEvent.Pre event)
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			renderEntity = mc.renderViewEntity;
			
			mc.renderViewEntity = mc.thePlayer;
		}
	}
	
	@SubscribeEvent
	public void renderPlayerPost(RenderPlayerEvent.Post event)
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			mc.renderViewEntity = (EntityLivingBase) renderEntity;
		}
	}
	
}
