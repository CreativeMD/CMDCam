package com.creativemd.cmdcam.movement;

import java.util.ArrayList;
import java.util.HashMap;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.CamEventHandler;
import com.creativemd.cmdcam.utils.CamPoint;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public abstract class Path {
	
	public static HashMap<String, Path> paths = new HashMap<>();
	
	public static void registerPath(String name, Path path)
	{
		paths.put(name, path);
	}
	
	public static Path getPathById(String lastPath) {
		return paths.get(lastPath);
	}
	
	public static void initPaths()
	{
		registerPath("default", new DefaultPath(null, 0, 0, null, null));
		registerPath("outside", new OutsidePath(null, 0, 0, null, null));		
	}
	
	public long started = System.currentTimeMillis();
	
	public long duration;
	public Movement movement;
	
	public boolean hideGui;
	
	public Object target;
	
	public double lastYaw;
	public double lastPitch;
	
	public boolean isFirst = true;
	
	/**
	 * zero loops will mean only one iteration (number of iterations = loops + 1). Negative will mean endless loops.
	 */
	public int loops;
	public int currentLoop;
	
	public ArrayList<CamPoint> points = null;
	
	public Path(ArrayList<CamPoint> points, long duration, int loops, Movement movement, Object target)
	{
		this.duration = duration;
		this.movement = movement;
		if(points != null)
		{
			this.points = new ArrayList<>(points);
			this.hideGui = mc.gameSettings.hideGUI;
			lastPitch = points.get(0).rotationPitch;
			lastYaw = points.get(0).rotationYaw;
		}
		this.target = target;
		this.loops = loops;
		this.currentLoop = 0;
		
		if(this.loops != 0)
			this.points.add(this.points.get(this.points.size()-1).copy());
	}
	
	public abstract Path createPath(ArrayList<CamPoint> points, long duration, int loops, Movement movement, Object target);
	
	public CamPoint getCamPoint(CamPoint point1, CamPoint point2, double percent, double wholeProgress, float renderTickTime, boolean isFirstLoop, boolean isLastLoop)
	{
		CamPoint newPoint = movement.getPointInBetween(point1, point2, percent, wholeProgress, isFirstLoop, isLastLoop);
		if(target != null)
		{
			newPoint.rotationPitch = lastPitch;
			newPoint.rotationYaw = lastYaw;
			Vec3d pos = null;
			if(target instanceof Entity)
			{
				pos = ((Entity) target).getPositionEyes(renderTickTime);
				if (target instanceof EntityLivingBase)
		        {
					pos = ((EntityLivingBase) target).getPositionEyes(renderTickTime).subtract(new Vec3d(0,((EntityLivingBase) target).getEyeHeight(),0));
		        }
		        else
		        {
		        	pos = ((Entity) target).getPositionEyes(renderTickTime);
		        }
			}
			if(target instanceof BlockPos)
				pos = new Vec3d((BlockPos) target);
			if(pos != null)
			{
				//newPoint.rotationPitch = getCamera().rotationPitch;
				//newPoint.rotationYaw = getCamera().rotationYaw;
				long timeSinceLastRenderFrame = System.nanoTime() - CamEventHandler.lastRenderTime;
				newPoint.faceEntity(pos, 0.00000001F, 0.00000001F, timeSinceLastRenderFrame/600000000D*CMDCam.cameraFollowSpeed);
				//System.out.println("time between:" + timeSinceLastRenderFrame);
			}
			lastPitch = newPoint.rotationPitch;
			lastYaw = newPoint.rotationYaw;
		}
		return newPoint;
	}
	
	public void onPathStart()
	{
		
	}
	
	public EntityLivingBase getCamera()
	{
		return mc.player;
	}
	
	public void tick(float renderTickTime)
	{
		if(isFirst)
		{
			onPathStart();
			isFirst = false;
		}
		long time = System.currentTimeMillis() - started;
		if(time >= duration)
		{
			if(currentLoop < loops || loops < 0)
			{
				started = System.currentTimeMillis();
				currentLoop++;
			}else{
				CMDCam.currentPath = null;
				onPathFinished();
			}
		}else{
			long durationOfPoint = duration / (points.size()-1);
			int currentPoint = Math.min((int) (time / durationOfPoint), points.size()-2);
			CamPoint point1 = points.get(currentPoint);
			CamPoint point2 = points.get(currentPoint+1);
			double percent = (time % durationOfPoint) / (double)durationOfPoint;
			//System.out.println(percent);
			CamPoint newPoint = getCamPoint(point1, point2, percent, (double)time/duration, renderTickTime, currentLoop == 0, currentLoop == loops);
			if(newPoint != null)
			{
				processPoint(newPoint);
			}
		}
	}
	
	public abstract String getDescription();
	
	public static Minecraft mc = Minecraft.getMinecraft();
	
	public void processPoint(CamPoint point)
	{
		mc.gameSettings.hideGUI = true;
		CMDCam.roll = (float) point.roll;
		mc.gameSettings.fovSetting = (float) point.zoom;
	}
	
	public void onPathFinished()
	{
		mc.gameSettings.hideGUI = hideGui;
		CMDCam.fov = CamEventHandler.defaultfov;
		mc.gameSettings.fovSetting = CamEventHandler.defaultfov;
		CMDCam.roll = 0;
	}
	
}
