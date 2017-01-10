package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.creativemd.cmdcam.utils.CamPoint;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;

public class OutsidePath extends Path{
	
	public EntityLivingBase camPlayer;
	
	public OutsidePath(ArrayList<CamPoint> points, long duration, Movement movement, Object target) {
		super(points, duration, movement, target);
		if(points != null)
			//this.camPlayer = mc.playerController.func_147493_a(mc.theWorld, new StatFileWriter());
			this.camPlayer = new EntityZombie(mc.world);
	}

	@Override
	public Path createPath(ArrayList<CamPoint> points, long duration, Movement movement, Object target) {
		return new OutsidePath(points, duration, movement, target);
	}

	@Override
	public String getDescription() {
		return "the player isn't the camera, you can control him at every time";
	}
	
	@Override
	public void onPathFinished()
	{
		super.onPathFinished();
		mc.setRenderViewEntity(mc.player);
	}
	
	@Override
	public EntityLivingBase getCamera()
	{
		return camPlayer;
	}
	
	@Override
	public void processPoint(CamPoint point)
	{
		super.processPoint(point);
		
		mc.setRenderViewEntity(camPlayer);
		if(camPlayer instanceof EntityPlayer)
			((EntityPlayer)camPlayer).capabilities.isFlying = true;
		camPlayer.setPositionAndRotation(point.x, point.y, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
		camPlayer.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
	}

}
