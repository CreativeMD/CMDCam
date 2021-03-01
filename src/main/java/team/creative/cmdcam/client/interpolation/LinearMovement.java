package team.creative.cmdcam.client.interpolation;

import java.util.List;

import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vector3;

public class LinearMovement extends CamInterpolation {
    
    @Override
    public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
        return point1.getPointBetween(point2, percent);
    }
    
    @Override
    public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
        
    }
    
    @Override
    public Vector3 getColor() {
        return new Vector3(0, 0, 1);
    }
}
