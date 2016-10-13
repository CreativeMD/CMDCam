package com.creativemd.cmdcam.utils;

import com.creativemd.cmdcam.CMDCam;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class CamPoint {
	
	public static Minecraft mc = Minecraft.getMinecraft();
	
	public double x;
	public double y;
	public double z;
	
	public double rotationYaw;
	public double rotationPitch;
	
	public double roll;
	public double zoom;
	
	public CamPoint(double x, double y, double z, double rotationYaw, double rotationPitch, double roll, double zoom)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotationYaw = rotationYaw;
		this.rotationPitch = rotationPitch;
		this.roll = roll;
		this.zoom = zoom;
	}
	
	public CamPoint()
	{
		this.x = mc.thePlayer.posX;
		this.y = mc.thePlayer.posY;
		this.z = mc.thePlayer.posZ;
		
		this.rotationYaw = mc.thePlayer.rotationYawHead;//MathHelper.wrapDegrees(mc.thePlayer.rotationYawHead);
		this.rotationPitch = mc.thePlayer.rotationPitch;
		
		this.roll = CMDCam.roll;
		this.zoom = CMDCam.fov;
	}
	
	public CamPoint getPointBetween(CamPoint point, double percent)
	{
		//player.rotationYaw);
		/*double yawDifference1 = (point.rotationYaw - this.rotationYaw);
		double yawDifference2 = (point.rotationYaw - 360 - this.rotationYaw);
		double diff = yawDifference1;
		if(Math.abs(yawDifference2) < Math.abs(yawDifference1))
			diff = yawDifference2;*/		
		return new CamPoint(
				this.x + (point.x - this.x) * percent,
				this.y + (point.y - this.y) * percent,
				this.z + (point.z - this.z) * percent,
				this.rotationYaw + (point.rotationYaw - this.rotationYaw) * percent,
				this.rotationPitch + (point.rotationPitch - this.rotationPitch) * percent,
				this.roll + (point.roll - this.roll) * percent,
				this.zoom + (point.zoom - this.zoom) * percent);
	}
	
	public void faceEntity(Vec3 pos, float minYaw, float minPitch, double ticks)
    {
        double d0 = pos.xCoord - this.x;
        double d2 = pos.zCoord - this.z;
        double d1 = pos.yCoord - this.y;

        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        double f2 = (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0D;
        double f3 = (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
        //System.out.println("ticks=" + ticks);
        this.rotationPitch = updateRotation(this.rotationPitch, f3, minPitch, ticks);
        this.rotationYaw = updateRotation(this.rotationYaw, f2, minYaw, ticks);
    }

    /**
     * Arguments: current rotation, intended rotation, max increment.
     */
    private double updateRotation(double rotation, double intended, double min, double ticks)
    {
    	double f3 = MathHelper.wrapAngleTo180_double(intended - rotation);
        
        if(f3 > 0)
        	f3 = Math.min(Math.abs(f3*ticks), f3);//Math.min(Math.max(min, Math.abs(f3*ticks)), f3);
    	else
    		f3 = Math.max(-Math.abs(f3*ticks), f3);//Math.max(-Math.max(min, Math.abs(f3*ticks)), f3);
        //System.out.println("f3=" + f3);
        /*if (f3 > max)
        {
            f3 = max;
        }

        if (f3 < -max)
        {
            f3 = -max;
        }*/

        return rotation + f3;
    }
	
	public CamPoint copy()
	{
		return new CamPoint(x, y, z, rotationYaw, rotationPitch, roll, zoom);
	}
	
	@Override
	public String toString()
	{
		return "x:" + x + ",y:" + y + ",z:" + z + ",yaw:" + rotationYaw + ",pitch:" + rotationPitch + ",roll:" + roll + ",zoom:" + zoom;
	}
	
}
