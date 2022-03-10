package team.creative.cmdcam.client.interpolation;

import java.util.List;

import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.CosineInterpolation;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;

public class SmoothMovement extends CamInterpolation {
    
    public CosineInterpolation<Vec1d> rollSpline;
    public CosineInterpolation<Vec1d> zoomSpline;
    public CosineInterpolation<Vec1d> pitchSpline;
    public CosineInterpolation<Vec1d> yawSpline;
    public CosineInterpolation<Vec3d> positionSpline;
    
    public double sizeOfIteration;
    
    @Override
    public void initMovement(List<CamPoint> points, int loops, CamTarget target) throws PathParseException {
        if (points.size() == 1)
            throw new PathParseException("At least two points are required");
        
        int iterations = loops == 0 ? 1 : loops == 1 ? 2 : 3;
        
        sizeOfIteration = 1D / iterations;
        
        int size = points.size() * iterations;
        if (iterations > 1)
            size++;
        
        Vec1d[] rollPoints = new Vec1d[size];
        Vec1d[] zoomPoints = new Vec1d[size];
        Vec1d[] yawPoints = new Vec1d[size];
        Vec1d[] pitchPoints = new Vec1d[size];
        
        Vec3d[] positionPoints = new Vec3d[size];
        
        for (int j = 0; j < iterations; j++) {
            for (int i = 0; i < points.size(); i++) {
                rollPoints[i + j * points.size()] = new Vec1d(points.get(i).roll);
                zoomPoints[i + j * points.size()] = new Vec1d(points.get(i).zoom);
                yawPoints[i + j * points.size()] = new Vec1d(points.get(i).rotationYaw);
                pitchPoints[i + j * points.size()] = new Vec1d(points.get(i).rotationPitch);
                
                positionPoints[i + j * points.size()] = new Vec3d(points.get(i).x, points.get(i).y, points.get(i).z);
            }
        }
        
        if (iterations > 1) {
            rollPoints[points.size() * iterations] = new Vec1d(points.get(0).roll);
            zoomPoints[points.size() * iterations] = new Vec1d(points.get(0).zoom);
            yawPoints[points.size() * iterations] = new Vec1d(points.get(0).rotationYaw);
            pitchPoints[points.size() * iterations] = new Vec1d(points.get(0).rotationPitch);
            positionPoints[points.size() * iterations] = new Vec3d(points.get(0).x, points.get(0).y, points.get(0).z);
        }
        
        rollSpline = new CosineInterpolation<>(rollPoints);
        zoomSpline = new CosineInterpolation<>(zoomPoints);
        pitchSpline = new CosineInterpolation<>(pitchPoints);
        yawSpline = new CosineInterpolation<>(yawPoints);
        positionSpline = new CosineInterpolation<>(positionPoints);
    }
    
    @Override
    public CamPoint getPointInBetween(CamPoint point1, CamPoint point2, double percent, double wholeProgress, boolean isFirstLoop, boolean isLastLoop) {
        CamPoint point = point1.getPointBetween(point2, percent);
        
        int iteration = isFirstLoop ? 0 : isLastLoop && sizeOfIteration < 0.5 ? 2 : 1;
        double additionalProgress = iteration * sizeOfIteration;
        wholeProgress = additionalProgress + wholeProgress * sizeOfIteration;
        
        if (rollSpline != null)
            point.roll = rollSpline.valueAt(wholeProgress).x;
        if (zoomSpline != null)
            point.zoom = zoomSpline.valueAt(wholeProgress).x;
        if (yawSpline != null)
            point.rotationYaw = yawSpline.valueAt(wholeProgress).x;
        if (pitchSpline != null)
            point.rotationPitch = pitchSpline.valueAt(wholeProgress).x;
        if (positionSpline != null) {
            Vec3d position = positionSpline.valueAt(wholeProgress);
            point.x = position.x;
            point.y = position.y;
            point.z = position.z;
        }
        return point;
    }
    
    @Override
    public Vec3d getColor() {
        return new Vec3d(0, 1, 0);
    }
    
}
