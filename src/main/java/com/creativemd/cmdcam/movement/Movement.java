package com.creativemd.cmdcam.movement;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;

public abstract class Movement {
	
	public static HashMap<String, Movement> movements = new HashMap<>();
	
	public static Movement getMovementById(String lastMovement) {
		/*Movement movement = movements.get(lastMovement);
		if(movement != null)
		{
			try {
				return movement.getClass().getConstructor().newInstance();
			} catch (Exception e){
				
			}
		}
		return null;*/
		return movements.get(lastMovement);
	}
	
	public static void registerMovement(String name, Movement movement)
	{
		movements.put(name, movement);
	}
	
	public static LinearMovement linear = new LinearMovement();
	public static SmoothMovement cosine = new SmoothMovement();
	public static CubicMovement cubic = new CubicMovement();
	public static HermiteMovement hermite = new HermiteMovement();
	
	public static void initMovements()
	{
		registerMovement("linear", linear);
		registerMovement("cubic", cubic);
		registerMovement("hermite", hermite);
		registerMovement("cosine", cosine);
	}
	
	public static String[] getMovementNames()
	{
		String[] names = new String[movements.size()];
		int i = 0;
		for (String name : movements.keySet()) {
			names[i] = name;
			i++;
		}
		return names;
	}
	
	public boolean isRenderingEnabled = false;
	
	public abstract void initMovement(ArrayList<CamPoint> points);
	
	public abstract CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress);
	
}
