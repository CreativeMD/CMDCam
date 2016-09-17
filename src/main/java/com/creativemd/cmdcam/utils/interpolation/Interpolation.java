package com.creativemd.cmdcam.utils.interpolation;

import java.util.ArrayList;

public abstract class Interpolation<T extends Vec> {

	public ArrayList<T> points = new ArrayList<>();
	private final Class classOfT;
	
	public Interpolation(T... points) {
		if(points.length < 2)
			throw new IllegalArgumentException("At least two points are needed!");
		
		classOfT = points[0].getClass();
		for (int i = 0; i < points.length; i++) {
			this.points.add(points[i]);
		}
	}
	
	protected double getValue(int index, int dim)
	{
		return points.get(index).getValueByDim(dim);
	}
	
	/**1 <= t <= 1**/
	public T valueAt(double t)
	{
		if(t >= 0 && t <= 1)
		{
			int pointIndex = (int) ((points.size()-1)*t);
			int pointIndexNext = pointIndex+1;
			if(pointIndexNext < points.size())
			{
				T vec = (T) Vec.copyVec(points.get(pointIndex));
				double pointDistance = 1D/(points.size()-1);
				double pointPosition = pointDistance*pointIndex;
				double mu = (t-pointPosition)/pointDistance;
				
				for (int dim = 0; dim < vec.getDimensionCount(); dim++) {
					vec.setValueByDim(dim, valueAt(mu, pointIndex, pointIndexNext, dim));
				}
				return vec;
			}
		}
		return (T) Vec.createEmptyVec(classOfT);
	}
	
	public abstract double valueAt(double mu, int pointIndex, int pointIndexNext, int dim);
	
}
