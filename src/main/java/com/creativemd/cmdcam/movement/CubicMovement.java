package com.creativemd.cmdcam.movement;

import java.util.ArrayList;

import org.apache.commons.math.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math.analysis.polynomials.PolynomialSplineFunction;

import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.Vector3;
import com.creativemd.cmdcam.utils.CamPoint;

public class CubicMovement extends Movement {
	
	public CatmullRomSpline<Vector3> movementSpline;
	public PolynomialSplineFunction zoomSpline;
	public PolynomialSplineFunction rollSpline;
	public PolynomialSplineFunction pitchSpline;
	public PolynomialSplineFunction yawSpline;
	
	@Override
	public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress) {
		CamPoint point = point1.getPointBetween(point2, percent);
		
		if(movementSpline != null)
		{
			Vector3 position = movementSpline.valueAt(new Vector3(), (float) wholeProgress);
			point.x = position.x;
			point.y = position.y;
			point.z = position.z;
		}
		
		if(zoomSpline != null)
		{
			double zoom = 0;
			try {
				zoom = zoomSpline.value(wholeProgress);
			} catch (Exception e) {
				e.printStackTrace();
			}
			point.zoom = zoom;
		}
		
		if(rollSpline != null)
		{
			double roll = 0;
			try {
				roll = rollSpline.value(wholeProgress);
			} catch (Exception e) {
				e.printStackTrace();
			}
			point.roll = roll;
		}
		
		if(yawSpline != null)
		{
			double yaw = 0;
			try {
				yaw = yawSpline.value(wholeProgress);
			} catch (Exception e) {
				e.printStackTrace();
			}
			point.rotationYaw = yaw;
		}
		
		if(pitchSpline != null)
		{
			double pitch = 0;
			try {
				pitch = pitchSpline.value(wholeProgress);
			} catch (Exception e) {
				e.printStackTrace();
			}
			point.rotationPitch = pitch;
		}
		
		return point;
	}

	@Override
	public void initMovement(ArrayList<CamPoint> points) {
		if(points.size() <= 2)
		{
			movementSpline = null;
			rollSpline = null;
			zoomSpline = null;
			yawSpline = null;
			pitchSpline = null;
			return ;
		}
		
		Vector3[] controlPoints = new Vector3[points.size()];
		
		double[] pointsX = new double[points.size()];
		double[] rollPoints = new double[points.size()];
		double[] zoomPoints = new double[points.size()];
		double[] yawPoints = new double[points.size()];
		double[] pitchPoints = new double[points.size()];
		for (int i = 0; i < controlPoints.length; i++) {
			controlPoints[i] = new Vector3((float)points.get(i).x, (float)points.get(i).y, (float)points.get(i).z);
			
			double time = i/((float)points.size()-1F);
			pointsX[i] = time;
			
			rollPoints[i] = points.get(i).roll;
			zoomPoints[i] = points.get(i).zoom;
			yawPoints[i] = points.get(i).rotationYaw;
			pitchPoints[i] = points.get(i).rotationPitch;
		}
		movementSpline = new CatmullRomSpline<>(controlPoints, true);
		movementSpline.spanCount--;
		zoomSpline = new SplineInterpolator().interpolate(pointsX, zoomPoints);
		rollSpline = new SplineInterpolator().interpolate(pointsX, rollPoints);
		yawSpline = new SplineInterpolator().interpolate(pointsX, yawPoints);
		pitchSpline = new SplineInterpolator().interpolate(pointsX, pitchPoints);
	}

}
