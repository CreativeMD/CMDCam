package com.creativemd.cmdcam.client.interpolation;

import java.util.List;

import com.creativemd.cmdcam.client.PathParseException;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.cmdcam.common.utils.math.Vec3;

public class LinearMovement extends CamInterpolation {

	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		return point1.getPointBetween(point2, percent);
	}

	@Override
	public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
		
	}

	@Override
	public Vec3 getColor() {
		return new Vec3(0,0,1);
	}
}
