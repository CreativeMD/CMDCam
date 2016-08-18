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
		registerPath("default", new DefaultPath(null, 0, null, null));
		registerPath("outside", new OutsidePath(null, 0, null, null));		
	}
	
	public long started = System.currentTimeMillis();
	
	public long duration;
	public Movement movement;
	
	public boolean hideGui;
	
	public Object target;
	
	public double lastYaw;
	public double lastPitch;
	
	public ArrayList<CamPoint> points = new ArrayList<>();
	
	public Path(ArrayList<CamPoint> points, long duration, Movement movement, Object target)
	{
		this.points = points;
		this.duration = duration;
		this.movement = movement;
		if(points != null)
		{
			this.hideGui = mc.gameSettings.hideGUI;
			lastPitch = points.get(0).rotationPitch;
			lastYaw = points.get(0).rotationYaw;
		}
		this.target = target;
	}
	
	public abstract Path createPath(ArrayList<CamPoint> points, long duration, Movement movement, Object target);
	
	public CamPoint getCamPoint(CamPoint point1, CamPoint point2, double percent, float renderTickTime)
	{
		CamPoint newPoint = movement.getPointInBetween(point1, point2, percent);
		if(target != null)
		{
			newPoint.rotationPitch = lastPitch;
			newPoint.rotationYaw = lastYaw;
			Vec3d pos = null;
			if(target instanceof Entity)
			{
				//double y;
				if (target instanceof EntityLivingBase)
		        {
					pos = ((EntityLivingBase) target).getPositionEyes(renderTickTime);
					
					//if(target instanceof EntityPlayer)
						//pos.yCoord -= ((Entity) target).yOffset/2;
		            //EntityLivingBase entitylivingbase = (EntityLivingBase)target;
		            //pos.yCoord = ((EntityLivingBase) target).posY + (double)((EntityLivingBase) target).getEyeHeight();
		        }
		        else
		        {
		            pos = new Vec3d(((Entity) target).posX, (((Entity) target).getEntityBoundingBox().minY + ((Entity) target).getEntityBoundingBox().maxY) / 2.0D, ((Entity) target).posZ);
		        }
				
				
				//System.out.println(pos);
			}
			if(target instanceof BlockPos)
				pos = new Vec3d((BlockPos) target);
			if(pos != null)
			{
				newPoint.faceEntity(pos, 0.01F, 0.01F, 100);
			}
			lastPitch = newPoint.rotationPitch;
			lastYaw = newPoint.rotationYaw;
		}
		//newPoint.rotationPitch = Math.cos(Math.toRadians(System.nanoTime()/1000000))*20;
		return newPoint;
	}
	
	public void tick(float renderTickTime)
	{
		long time = System.currentTimeMillis() - started;
		if(time >= duration)
		{
			CMDCam.currentPath = null;
			onPathFinished();
			try {
				finalize();
			} catch (Throwable e) {
				e.printStackTrace();
			}
			System.out.println("Finished Path");
		}else{
			long durationOfPoint = duration / (points.size()-1);
			int currentPoint = Math.min((int) (time / durationOfPoint), points.size()-2);
			CamPoint point1 = points.get(currentPoint);
			CamPoint point2 = points.get(currentPoint+1);
			double percent = (time % durationOfPoint) / (double)durationOfPoint;
			//System.out.println(percent);
			CamPoint newPoint = getCamPoint(point1, point2, percent, renderTickTime);
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
		/*System.out.println("fov=" + point.zoom + " from fov=" + mc.gameSettings.fovSetting);
		try{
			Class clientPlayerAPI = Class.forName("api.player.client.ClientPlayerAPI");
			Class IClientPlayerAPI = Class.forName("api.player.client.IClientPlayerAPI");
			List list = (List) ReflectionHelper.findMethod(clientPlayerAPI, null, new String[]{"getAllInstancesList"}).invoke(null);
			for(Object instance : list)
			{
				if(clientPlayerAPI != null)
				{
					Object playerAPI = ReflectionHelper.findMethod(IClientPlayerAPI, instance, new String[]{"getClientPlayerAPI"}).invoke(instance);
					clientPlayerAPI.getField("isGetFOVMultiplierModded").set(playerAPI, false);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}*/
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
