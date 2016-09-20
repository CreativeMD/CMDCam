package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.utils.CamPoint;

import net.minecraft.util.MouseHelper;

public class DefaultPath extends Path{
	
	public DefaultPath(ArrayList<CamPoint> points, long duration, Movement movement, Object target)
	{		
		super(points, duration, movement, target);
		if(target != null && mc != null && target.equals(mc.thePlayer))
			this.target = null;
	}
	
	@Override
	public Path createPath(ArrayList<CamPoint> points, long duration, Movement movement, Object target) {
		return new DefaultPath(points, duration, movement, target);
	}

	@Override
	public String getDescription() {
		return "the player acts as the camera";
	}
	
	@Override
	public void processPoint(CamPoint point)
	{
		super.processPoint(point);
		
		//Mouse.setGrabbed(false);
		Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
	    Mouse.setGrabbed(true);
		
		mc.thePlayer.capabilities.isFlying = true;
		
		mc.thePlayer.setPositionAndRotation(point.x, point.y, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		mc.thePlayer.prevRotationYaw = (float) point.rotationYaw;
		mc.thePlayer.prevRotationPitch = (float) point.rotationPitch;
		mc.thePlayer.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		//mc.thePlayer.setPositionAndRotation(point.x, point.y, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		
	}

}
