package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Vec1;
import com.creativemd.cmdcam.utils.interpolation.Vec3;

public class SmoothMovement extends Movement {
	
	public CosineInterpolation<Vec1> rollSpline;
	public CosineInterpolation<Vec1> zoomSpline;
	public CosineInterpolation<Vec1> pitchSpline;
	public CosineInterpolation<Vec1> yawSpline;
	public CosineInterpolation<Vec3> positionSpline;

	@Override
	public void initMovement(ArrayList<CamPoint> points) {
		Vec1[] rollPoints = new Vec1[points.size()];
		Vec1[] zoomPoints = new Vec1[points.size()];
		Vec1[] yawPoints = new Vec1[points.size()];
		Vec1[] pitchPoints = new Vec1[points.size()];
		
		Vec3[] positionPoints = new Vec3[points.size()];
		
		for (int i = 0; i < points.size(); i++) {
			rollPoints[i] = new Vec1(points.get(i).roll);
			zoomPoints[i] = new Vec1(points.get(i).zoom);
			yawPoints[i] = new Vec1(points.get(i).rotationYaw);
			pitchPoints[i] = new Vec1(points.get(i).rotationPitch);
			
			positionPoints[i] = new Vec3(points.get(i).x, points.get(i).y, points.get(i).z);
		}
		rollSpline = new CosineInterpolation<>(rollPoints);
		zoomSpline = new CosineInterpolation<>(zoomPoints);
		pitchSpline = new CosineInterpolation<>(pitchPoints);
		yawSpline = new CosineInterpolation<>(yawPoints);
		positionSpline = new CosineInterpolation<>(positionPoints);
	}

	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress) {
		CamPoint point = point1.getPointBetween(point2, percent);
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
