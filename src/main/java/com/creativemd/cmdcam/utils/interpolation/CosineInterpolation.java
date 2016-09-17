package com.creativemd.cmdcam.utils.interpolation;

import java.util.ArrayList;
import java.util.Arrays;

public class CosineInterpolation<T extends Vec> extends Interpolation<T> {
	
	public CosineInterpolation(T... points)
	{
		super(points);
	}

	@Override
	public double valueAt(double mu, int pointIndex, int pointIndexNext, int dim) {
		double mu2 = (1-Math.cos(mu*Math.PI))/2;
		return(getValue(pointIndex, dim)*(1-mu2)+getValue(pointIndexNext, dim)*mu2);
	}
}
