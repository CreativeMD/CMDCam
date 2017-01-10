package com.creativemd.cmdcam.transform;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.movement.OutsidePath;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class CamMouseOverHandler {
	
	public static Minecraft mc = Minecraft.getMinecraft();
	
	public static Entity camera = null;
	
	public static void setupMouseHandlerBefore()
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			camera = mc.getRenderViewEntity();
			mc.setRenderViewEntity(mc.player);
		}
	}
	
	public static void setupMouseHandlerAfter()
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			mc.setRenderViewEntity(camera);
			//camera = null;
		}
	}
	
}
