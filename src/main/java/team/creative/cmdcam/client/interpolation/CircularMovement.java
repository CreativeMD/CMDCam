package team.creative.cmdcam.client.interpolation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.cmdcam.common.util.interpolation.HermiteInterpolation;
import team.creative.creativecore.common.util.math.matrix.Matrix3;
import team.creative.creativecore.common.util.math.vec.Vector1;
import team.creative.creativecore.common.util.math.vec.Vector3;

public class CircularMovement extends HermiteMovement {
	
	private static Minecraft mc = Minecraft.getInstance();
	
	public Vector3 sphereOrigin;
	public double radius;
	public CamTarget target;
	public HermiteInterpolation<Vector1> yAxis;
	
	@Override
	public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
		if (target == null)
			throw new PathParseException("No target found");
		
		Vector3d center = target.getTargetVec(mc.world, mc.getRenderPartialTicks());
		if (center != null) {
			points.add(points.get(0));
			
			this.target = target;
			Vector3 firstPoint = new Vector3(points.get(0).x, points.get(0).y, points.get(0).z);
			Vector3 centerPoint = new Vector3(center.x, center.y, center.z);
			this.sphereOrigin = new Vector3(firstPoint);
			sphereOrigin.sub(centerPoint);
			
			this.radius = sphereOrigin.length();
			
			ArrayList<Vector1> vecs = new ArrayList<>();
			ArrayList<Double> times = new ArrayList<>();
			
			times.add(0D);
			vecs.add(new Vector1(firstPoint.y));
			
			ArrayList<CamPoint> newPointsSorted = new ArrayList<>();
			newPointsSorted.add(points.get(0));
			
			for (int i = 1; i < points.size() - 1; i++) {
				
				Vector3 point = new Vector3(points.get(i).x, firstPoint.y, points.get(i).z);
				point.sub(centerPoint);
				
				double dot = point.dot(sphereOrigin);
				double det = ((point.x * sphereOrigin.z) - (point.z * sphereOrigin.x));
				double angle = Math.toDegrees(Math.atan2(det, dot));
				
				if (angle < 0)
					angle += 360;
				
				double time = angle / 360;
				for (int j = 0; j < times.size(); j++) {
					if (times.get(j) > time) {
						times.add(j, time);
						vecs.add(j, new Vector1(points.get(i).y));
						newPointsSorted.add(j, points.get(i));
						break;
					}
				}
				newPointsSorted.add(points.get(i));
				times.add(time);
				vecs.add(new Vector1(points.get(i).y));
			}
			
			if (loops == 0)
				newPointsSorted.add(newPointsSorted.get(0).copy());
			
			times.add(1D);
			vecs.add(new Vector1(firstPoint.y));
			
			this.yAxis = new HermiteInterpolation<>(ArrayUtils.toPrimitive(times.toArray(new Double[0])), vecs.toArray(new Vector1[0]));
			
			super.initMovement(times.toArray(new Double[0]), newPointsSorted, loops, target);
		} else
			throw new PathParseException("Invalid target");
	}
	
	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		CamPoint newCamPoint = super.getPointInBetween(point1, point2, percent, wholeProgress, isFirstLoop, isLastLoop);
		
		double angle = wholeProgress * 360;
		
		Vector3d center = target.getTargetVec(mc.world, mc.getRenderPartialTicks());
		if (center != null) {
			Vector3 centerPoint = new Vector3(center.x, center.y, center.z);
			
			Vector3 newPoint = new Vector3(sphereOrigin);
			newPoint.y = 0;
			Matrix3 matrix = new Matrix3();
			matrix.rotY(Math.toRadians(angle));
			matrix.transform(newPoint);
			
			newPoint.y = yAxis.valueAt(wholeProgress).x - center.y;
			newPoint.normalize();
			newPoint.scale(radius);
			
			newPoint.add(centerPoint);
			newCamPoint.x = newPoint.x;
			newCamPoint.y = newPoint.y;
			newCamPoint.z = newPoint.z;
		}
		
		return newCamPoint;
	}
	
	@Override
	public Vector3 getColor() {
		return new Vector3(1, 1, 0);
	}
	
}
