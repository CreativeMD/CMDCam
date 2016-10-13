package com.creativemd.cmdcam.transform;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.movement.OutsidePath;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class CamMouseOverHandler {
	
	public static Minecraft mc = Minecraft.getMinecraft();
	
	public static EntityLivingBase camera = null;
	
	public static void setupMouseHandlerBefore()
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			camera = mc.renderViewEntity;
			mc.renderViewEntity = mc.thePlayer;
		}
	}
	
	public static void setupMouseHandlerAfter()
	{
		if(CMDCam.currentPath instanceof OutsidePath)
		{
			mc.renderViewEntity = camera;
			//camera = null;
		}
	}
	
}
