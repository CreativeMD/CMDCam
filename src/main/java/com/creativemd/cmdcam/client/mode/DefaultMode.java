package com.creativemd.cmdcam.client.mode;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.cmdcam.common.utils.CamTarget.SelfTarget;

import net.minecraft.util.MouseHelper;

public class DefaultMode extends CamMode {
	
	public DefaultMode(CamPath path)
	{		
		super(path);
		if(path != null && path.target != null && mc != null && path.target instanceof SelfTarget)
			path.target = null;
	}
	
	@Override
	public CamMode createMode(CamPath path) {
		return new DefaultMode(path);
	}

	@Override
	public String getDescription() {
		return "the player acts as the camera";
	}
	
	@Override
	public void processPoint(CamPoint point)
	{
		super.processPoint(point);
		
		Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
	    Mouse.setGrabbed(true);
		
		mc.player.capabilities.isFlying = true;
		
		mc.player.setPositionAndRotation(point.x, point.y, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		mc.player.prevRotationYaw = (float) point.rotationYaw;
		mc.player.prevRotationPitch = (float) point.rotationPitch;
		mc.player.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		
	}

}
