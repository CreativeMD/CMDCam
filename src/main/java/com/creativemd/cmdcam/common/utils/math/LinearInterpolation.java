package com.creativemd.cmdcam.common.utils.math;

public class LinearInterpolation<T extends Vec> extends Interpolation<T> {
	
	public LinearInterpolation(T... points) {
		super(points);
	}

	@Override
	public double valueAt(double mu, int pointIndex, int pointIndexNext, int dim) {
		return (getValue(pointIndexNext, dim) - getValue(pointIndex, dim))*mu+getValue(pointIndex, dim);
	}

}
