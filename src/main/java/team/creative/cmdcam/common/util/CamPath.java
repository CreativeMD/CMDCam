package team.creative.cmdcam.common.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.client.mode.CamMode;

public class CamPath {
	
	public int loop;
	public long duration;
	public String mode;
	public String interpolation;
	public CamTarget target;
	public List<CamPoint> points;
	public double cameraFollowSpeed;
	public boolean serverPath = false;
	
	public CamPath(CompoundNBT nbt) {
		this.loop = nbt.getInt("loop");
		this.duration = nbt.getLong("duration");
		this.mode = nbt.getString("mode");
		this.interpolation = nbt.getString("interpolation");
		if (nbt.contains("target"))
			this.target = CamTarget.readFromNBT(nbt.getCompound("target"));
		ListNBT list = nbt.getList("points", 10);
		this.points = new ArrayList<>();
		for (INBT point : list) {
			points.add(new CamPoint((CompoundNBT) point));
		}
		this.cameraFollowSpeed = nbt.getDouble("cameraFollowSpeed");
	}
	
	public CamPath(int loop, long duration, String mode, String interpolation, CamTarget target, List<CamPoint> points, double cameraFollowSpeed) {
		this.loop = loop;
		this.duration = duration;
		this.mode = mode;
		this.interpolation = interpolation;
		this.target = target;
		this.points = points;
		this.cameraFollowSpeed = cameraFollowSpeed;
	}
	
	public CompoundNBT writeToNBT(CompoundNBT nbt) {
		nbt.putInt("loop", loop);
		nbt.putLong("duration", duration);
		nbt.putString("mode", mode);
		nbt.putString("interpolation", interpolation);
		if (target != null)
			nbt.put("target", target.writeToNBT(new CompoundNBT()));
		ListNBT list = new ListNBT();
		for (CamPoint point : points) {
			list.add(point.writeToNBT(new CompoundNBT()));
		}
		nbt.put("points", list);
		nbt.putDouble("cameraFollowSpeed", cameraFollowSpeed);
		return nbt;
	}
	
	@OnlyIn(Dist.CLIENT)
	public void overwriteClientConfig() {
		CMDCamClient.lastLoop = this.loop;
		CMDCamClient.lastDuration = this.duration;
		CMDCamClient.lastMode = this.mode;
		CMDCamClient.lastInterpolation = this.interpolation;
		CMDCamClient.target = this.target;
		CMDCamClient.points = new ArrayList<>(this.points);
		CMDCamClient.cameraFollowSpeed = this.cameraFollowSpeed;
	}
	
	@OnlyIn(Dist.CLIENT)
	private boolean hideGui;
	@OnlyIn(Dist.CLIENT)
	public CamInterpolation cachedInterpolation;
	@OnlyIn(Dist.CLIENT)
	public CamMode cachedMode;
	
	public long timeStarted = System.currentTimeMillis();
	public int currentLoop;
	private boolean finished;
	private boolean running;
	public List<CamPoint> tempPoints;
	
	public void start(World world) throws PathParseException {
		this.finished = false;
		this.running = true;
		
		this.timeStarted = System.currentTimeMillis();
		this.currentLoop = 0;
		this.tempPoints = new ArrayList<>(points);
		if (loop != 0)
			this.tempPoints.add(this.tempPoints.get(this.tempPoints.size() - 1).copy());
		
		if (world.isRemote) {
			CamMode parser = CamMode.getMode(mode);
			
			this.cachedMode = parser.createMode(this);
			this.cachedMode.onPathStart();
			
			this.cachedInterpolation = CamInterpolation.getInterpolationOrDefault(interpolation);
			this.cachedInterpolation.initMovement(tempPoints, loop, target);
			
			this.hideGui = Minecraft.getInstance().gameSettings.hideGUI;
		}
	}
	
	public void finish(World world) {
		this.finished = true;
		this.running = false;
		
		if (world.isRemote) {
			this.cachedMode.onPathFinish();
			this.tempPoints = null;
			
			this.cachedMode = null;
			this.cachedInterpolation = null;
			
			Minecraft.getInstance().gameSettings.hideGUI = hideGui;
		}
	}
	
	public void tick(World world, float renderTickTime) {
		long time = System.currentTimeMillis() - timeStarted;
		if (time >= duration) {
			if (currentLoop < loop || loop < 0) {
				timeStarted = System.currentTimeMillis();
				currentLoop++;
			} else
				finish(world);
		} else {
			if (world.isRemote)
				Minecraft.getInstance().gameSettings.hideGUI = true;
			
			long durationOfPoint = duration / (tempPoints.size() - 1);
			int currentPoint = Math.min((int) (time / durationOfPoint), tempPoints.size() - 2);
			CamPoint point1 = tempPoints.get(currentPoint);
			CamPoint point2 = tempPoints.get(currentPoint + 1);
			double percent = (time % durationOfPoint) / (double) durationOfPoint;
			CamPoint newPoint = cachedMode.getCamPoint(point1, point2, percent, (double) time / duration, renderTickTime, currentLoop == 0, currentLoop == loop);
			
			if (newPoint != null)
				cachedMode.processPoint(newPoint);
			
		}
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public boolean hasFinished() {
		return finished;
	}
	
	public CamPath copy() {
		CamPath path = new CamPath(currentLoop, duration, mode, interpolation, target, new ArrayList<>(points), cameraFollowSpeed);
		path.serverPath = this.serverPath;
		return path;
	}
	
}
