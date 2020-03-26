package team.creative.cmdcam.client.interpolation;

import java.util.List;

import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.cmdcam.common.util.interpolation.CubicInterpolation;
import team.creative.creativecore.common.util.math.vec.Vector1;
import team.creative.creativecore.common.util.math.vec.Vector3;

public class CubicMovement extends CamInterpolation {
	
	public CubicInterpolation<Vector1> rollSpline;
	public CubicInterpolation<Vector1> zoomSpline;
	public CubicInterpolation<Vector1> pitchSpline;
	public CubicInterpolation<Vector1> yawSpline;
	public CubicInterpolation<Vector3> positionSpline;
	
	public double sizeOfIteration;
	
	@Override
	public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
		if (points.size() == 1)
			throw new PathParseException("At least two points are required");
		
		int iterations = loops == 0 ? 1 : loops == 1 ? 2 : 3;
		
		sizeOfIteration = 1D / iterations;
		
		int size = points.size() * iterations;
		if (iterations > 1)
			size++;
		
		Vector1[] rollPoints = new Vector1[size];
		Vector1[] zoomPoints = new Vector1[size];
		Vector1[] yawPoints = new Vector1[size];
		Vector1[] pitchPoints = new Vector1[size];
		
		Vector3[] positionPoints = new Vector3[points.size() * iterations];
		
		for (int j = 0; j < iterations; j++) {
			for (int i = 0; i < points.size(); i++) {
				rollPoints[i + j * points.size()] = new Vector1(points.get(i).roll);
				zoomPoints[i + j * points.size()] = new Vector1(points.get(i).zoom);
				yawPoints[i + j * points.size()] = new Vector1(points.get(i).rotationYaw);
				pitchPoints[i + j * points.size()] = new Vector1(points.get(i).rotationPitch);
				
				positionPoints[i + j * points.size()] = new Vector3(points.get(i).x, points.get(i).y, points.get(i).z);
			}
		}
		
		if (iterations > 1) {
			rollPoints[points.size() * iterations] = new Vector1(points.get(0).roll);
			zoomPoints[points.size() * iterations] = new Vector1(points.get(0).zoom);
			yawPoints[points.size() * iterations] = new Vector1(points.get(0).rotationYaw);
			pitchPoints[points.size() * iterations] = new Vector1(points.get(0).rotationPitch);
			positionPoints[points.size() * iterations] = new Vector3(points.get(0).x, points.get(0).y, points.get(0).z);
		}
		
		rollSpline = new CubicInterpolation<>(rollPoints);
		zoomSpline = new CubicInterpolation<>(zoomPoints);
		pitchSpline = new CubicInterpolation<>(pitchPoints);
		yawSpline = new CubicInterpolation<>(yawPoints);
		positionSpline = new CubicInterpolation<>(positionPoints);
	}
	
	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		CamPoint point = point1.getPointBetween(point2, percent);
		
		int iteration = isFirstLoop ? 0 : isLastLoop && sizeOfIteration < 0.5 ? 2 : 1;
		double additionalProgress = iteration * sizeOfIteration;
		wholeProgress = additionalProgress + wholeProgress * sizeOfIteration;
		
		if (rollSpline != null)
			point.roll = rollSpline.valueAt(wholeProgress).x;
		if (zoomSpline != null)
			point.zoom = zoomSpline.valueAt(wholeProgress).x;
		if (yawSpline != null)
			point.rotationYaw = yawSpline.valueAt(wholeProgress).x;
		if (pitchSpline != null)
			point.rotationPitch = pitchSpline.valueAt(wholeProgress).x;
		if (positionSpline != null) {
			Vector3 position = positionSpline.valueAt(wholeProgress);
			point.x = position.x;
			point.y = position.y;
			point.z = position.z;
		}
		return point;
	}
	
	@Override
	public Vector3 getColor() {
		return new Vector3(1, 0, 0);
	}
	
}
