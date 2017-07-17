package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;
import com.creativemd.cmdcam.utils.interpolation.CubicInterpolation;
import com.creativemd.cmdcam.utils.interpolation.HermiteInterpolation;
import com.creativemd.cmdcam.utils.interpolation.HermiteInterpolation.Tension;
import com.creativemd.cmdcam.utils.interpolation.Vec1;
import com.creativemd.cmdcam.utils.interpolation.Vec3;

public class CubicMovement extends Movement {
	
	public CubicInterpolation<Vec1> rollSpline;
	public CubicInterpolation<Vec1> zoomSpline;
	public CubicInterpolation<Vec1> pitchSpline;
	public CubicInterpolation<Vec1> yawSpline;
	public CubicInterpolation<Vec3> positionSpline;
	
	public double sizeOfIteration;

	@Override
	public void initMovement(ArrayList<CamPoint> points, int loops) {
		int iterations = loops == 0 ? 1 : loops == 1 ? 2 : 3;
		
		sizeOfIteration = 1D/iterations;
		
		Vec1[] rollPoints = new Vec1[points.size()*iterations];
		Vec1[] zoomPoints = new Vec1[points.size()*iterations];
		Vec1[] yawPoints = new Vec1[points.size()*iterations];
		Vec1[] pitchPoints = new Vec1[points.size()*iterations];
		
		Vec3[] positionPoints = new Vec3[points.size()*iterations];
		
		for (int j = 0; j < iterations; j++) {
			for (int i = 0; i < points.size(); i++) {
				rollPoints[i+j*points.size()] = new Vec1(points.get(i).roll);
				zoomPoints[i+j*points.size()] = new Vec1(points.get(i).zoom);
				yawPoints[i+j*points.size()] = new Vec1(points.get(i).rotationYaw);
				pitchPoints[i+j*points.size()] = new Vec1(points.get(i).rotationPitch);
				
				positionPoints[i+j*points.size()] = new Vec3(points.get(i).x, points.get(i).y, points.get(i).z);
			}
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
		wholeProgress = additionalProgress+wholeProgress*sizeOfIteration;
		
		if(rollSpline != null)
			point.roll = rollSpline.valueAt(wholeProgress).x;
		if(zoomSpline != null)
			point.zoom = zoomSpline.valueAt(wholeProgress).x;
		if(yawSpline != null)
			point.rotationYaw = yawSpline.valueAt(wholeProgress).x;
		if(pitchSpline != null)
			point.rotationPitch = pitchSpline.valueAt(wholeProgress).x;
		if(positionSpline != null)
		{
			Vec3 position = positionSpline.valueAt(wholeProgress);
			point.x = position.x;
			point.y = position.y;
			point.z = position.z;
		}
		return point;
	}

}
