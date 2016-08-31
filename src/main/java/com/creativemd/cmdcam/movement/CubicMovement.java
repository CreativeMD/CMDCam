package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector3;
import com.creativemd.cmdcam.utils.CamPoint;

public class CubicMovement extends Movement {
	
	public CatmullRomSpline<Vector3> spline;
	
	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress) {
		CamPoint point = point1.getPointBetween(point2, percent);
		
		if(spline != null)
		{
			Vector3 position = new Vector3();
			position = spline.valueAt(position, (float) wholeProgress);
			//spline.derivativeAt(position, (float) wholeProgress);
			System.out.println("progress: " + wholeProgress);
			point.x = position.x;
			point.y = position.y;
			point.z = position.z;
			System.out.println(position);
		}
		
		return point;
	}

	@Override
	public void initMovement(ArrayList<CamPoint> points) {
		if(points.size() <= 2)
		{
			spline = null;
			return ;
		}
		
		Vector3[] controlPoints = new Vector3[points.size()];
		for (int i = 0; i < controlPoints.length; i++) {
			controlPoints[i] = new Vector3((float)points.get(i).x, (float)points.get(i).y, (float)points.get(i).z);
		}
		spline = new CatmullRomSpline<>(controlPoints, true);
		spline.spanCount-=1;
		//spline.spanCount = 1;
		
	}

}
