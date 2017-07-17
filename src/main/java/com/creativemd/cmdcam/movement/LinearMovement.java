package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.creativemd.cmdcam.utils.CamPoint;

public class LinearMovement extends Movement {

	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
		return point1.getPointBetween(point2, percent);
	}

	@Override
	public void initMovement(ArrayList<CamPoint> points, int loops) {
		
	}

}
