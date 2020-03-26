package team.creative.cmdcam.client.interpolation;

import java.util.HashMap;
import java.util.List;

import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vector3;

public abstract class CamInterpolation {
	
	public static HashMap<String, CamInterpolation> interpolationTypes = new HashMap<>();
	
	public static CamInterpolation getInterpolationOrDefault(String interpolation) {
		return interpolationTypes.getOrDefault(interpolation, hermite);
	}
	
	public static CamInterpolation getInterpolation(String interpolation) {
		return interpolationTypes.get(interpolation.toLowerCase());
	}
	
	public static void registerInterpolation(String name, CamInterpolation interpolation) {
		interpolationTypes.put(name, interpolation);
	}
	
	public static LinearMovement linear = new LinearMovement();
	public static SmoothMovement cosine = new SmoothMovement();
	public static CubicMovement cubic = new CubicMovement();
	public static HermiteMovement hermite = new HermiteMovement();
	public static CircularMovement circular = new CircularMovement();
	
	static {
		registerInterpolation("linear", linear);
		registerInterpolation("cubic", cubic);
		registerInterpolation("hermite", hermite);
		registerInterpolation("cosine", cosine);
		registerInterpolation("circular", circular);
	}
	
	public static String[] getMovementNames() {
		String[] names = new String[interpolationTypes.size()];
		int i = 0;
		for (String name : interpolationTypes.keySet()) {
			names[i] = name;
			i++;
		}
		return names;
	}
	
	public boolean isRenderingEnabled = false;
	
	public abstract Vector3 getColor();
	
	public abstract void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException;
	
	public abstract CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop);
	
}
