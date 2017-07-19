package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.creativemd.cmdcam.movement.Movement.MovementParseException;
import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.Vec3;

public class LinearMovement extends Movement {

	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		return point1.getPointBetween(point2, percent);
	}

	@Override
	public void initMovement(ArrayList<CamPoint> points, int loops, Object target) throws MovementParseException {
		
	}

	@Override
	public Vec3 getColor() {
		return new Vec3(0,0,1);
	}
}
