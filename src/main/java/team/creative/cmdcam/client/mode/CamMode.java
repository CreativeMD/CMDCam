package team.creative.cmdcam.client.mode;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CamEventHandlerClient;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.common.util.CamPoint;

@OnlyIn(Dist.CLIENT)
public abstract class CamMode {
	
	protected static Minecraft mc = Minecraft.getInstance();
	
	public static HashMap<String, CamMode> modes = new HashMap<>();
	
	public static void registerPath(String id, CamMode path) {
		modes.put(id, path);
	}
	
	public static CamMode getMode(String id) {
		return modes.get(id.toLowerCase());
	}
	
	static {
		registerPath("default", new DefaultMode(null));
		registerPath("outside", new OutsideMode(null));
	}
	
	public final CamPath path;
	
	public double lastYaw;
	public double lastPitch;
	
	public CamMode(CamPath path) {
		this.path = path;
		
		if (path != null) {
			this.lastPitch = path.tempPoints.get(0).rotationPitch;
			this.lastYaw = path.tempPoints.get(0).rotationYaw;
		}
	}
	
	public abstract CamMode createMode(CamPath path);
	
	public CamPoint getCamPoint(CamPoint point1, CamPoint point2, double percent, double wholeProgress, float renderTickTime, boolean isFirstLoop, boolean isLastLoop) {
		CamPoint newPoint = path.cachedInterpolation.getPointInBetween(point1, point2, percent, wholeProgress, isFirstLoop, isLastLoop);
		if (path.target != null) {
			newPoint.rotationPitch = lastPitch;
			newPoint.rotationYaw = lastYaw;
			
			Vector3d pos = path.target.getTargetVec(mc.world, mc.getRenderPartialTicks());
			
			if (pos != null) {
				long timeSinceLastRenderFrame = System.nanoTime() - CamEventHandlerClient.lastRenderTime;
				newPoint.faceEntity(pos, 0.00000001F, 0.00000001F, timeSinceLastRenderFrame / 600000000D * path.cameraFollowSpeed);
			}
			lastPitch = newPoint.rotationPitch;
			lastYaw = newPoint.rotationYaw;
		}
		return newPoint;
	}
	
	public void onPathStart() {
		
	}
	
	public void onPathFinish() {
		mc.gameSettings.fov = CamEventHandlerClient.currentFOV = CamEventHandlerClient.defaultFOV;
		CamEventHandlerClient.roll = 0;
	}
	
	public abstract String getDescription();
	
	public void processPoint(CamPoint point) {
		CamEventHandlerClient.roll = (float) point.roll;
		CamEventHandlerClient.currentFOV = (float) point.zoom;
	}
	
	public static CamPoint getPoint(CamInterpolation movement, ArrayList<CamPoint> points, double percent, int currentLoop, int loops) {
		double lengthOfPoint = 1D / (points.size() - 1);
		int currentPoint = Math.min((int) (percent / lengthOfPoint), points.size() - 2);
		CamPoint point1 = points.get(currentPoint);
		CamPoint point2 = points.get(currentPoint + 1);
		return movement.getPointInBetween(point1, point2, (percent - currentPoint * lengthOfPoint), percent, currentLoop == 0, currentLoop == loops);
	}
	
}
