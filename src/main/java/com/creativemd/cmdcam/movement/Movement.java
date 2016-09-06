package com.creativemd.cmdcam.movement;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import com.creativemd.cmdcam.utils.CamPoint;

public abstract class Movement {
	
	public static HashMap<String, Movement> movements = new HashMap<>();
	
	public static Movement getMovementById(String lastMovement) {
		Movement movement = movements.get(lastMovement);
		if(movement != null)
		{
			try {
				return movement.getClass().getConstructor().newInstance();
			} catch (Exception e){
				
			}
		}
		return null;
	}
	
	public static void registerMovement(String name, Movement movement)
	{
		movements.put(name, movement);
	}
	
	public static void initMovements()
	{
		registerMovement("linear", new LinearMovement());
		registerMovement("cubic", new CubicMovement());
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
	
	public abstract void initMovement(ArrayList<CamPoint> points);
	
	public abstract CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress);
	
}
