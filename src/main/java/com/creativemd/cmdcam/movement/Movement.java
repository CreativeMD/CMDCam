package com.creativemd.cmdcam.movement;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import com.creativemd.cmdcam.utils.CamPoint;
import com.creativemd.cmdcam.utils.interpolation.CosineInterpolation;
import com.creativemd.cmdcam.utils.interpolation.Vec3;

import net.minecraft.util.math.Vec3d;

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
	public static CircularMovement circular = new CircularMovement();
	
	public static void initMovements()
	{
		registerMovement("linear", linear);
		registerMovement("cubic", cubic);
		registerMovement("hermite", hermite);
		registerMovement("cosine", cosine);
		registerMovement("circular", circular);
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
	
	public abstract Vec3 getColor();
	
	public abstract void initMovement(ArrayList<CamPoint> points, int loops, Object target) throws MovementParseException;
	
	public abstract CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop);
	
	public static class MovementParseException extends Exception {
		
		public MovementParseException(String msg) {
			super(msg);
		}
		
	}
	
}
