package team.creative.cmdcam.common.math.interpolation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.Minecraft;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.interpolation.HermiteInterpolation;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.matrix.Matrix3;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.type.Color;

public class CircularCamInterpolation extends CamInterpolation {
    
    public CircularCamInterpolation() {
        super(new Color(255, 255, 0));
    }
    
    @Override
    public <T extends VecNd> Interpolation<T> create(double[] timed, CamScene scene, T before, List<T> points, T after, CamAttribute<T> attribute) {
        if (attribute == CamAttribute.POSITION && scene.lookTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            Vec3d center = scene.lookTarget.position(mc.level, mc.getDeltaFrameTime());
            if (center != null) {
                List<Vec3d> points3 = (List<Vec3d>) points;
                points.add(points.get(0));
                Vec3d firstPoint = new Vec3d(points3.get(0).x, points3.get(0).y, points3.get(0).z);
                Vec3d centerPoint = new Vec3d(center.x, center.y, center.z);
                Vec3d sphereOrigin = new Vec3d(firstPoint);
                sphereOrigin.sub(centerPoint);
                
                double radius = sphereOrigin.length();
                
                ArrayList<Vec1d> vecs = new ArrayList<>();
                ArrayList<Double> times = new ArrayList<>();
                
                times.add(0D);
                vecs.add(new Vec1d(firstPoint.y));
                
                ArrayList<Vec3d> newPointsSorted = new ArrayList<>();
                newPointsSorted.add(points3.get(0));
                
                for (int i = 1; i < points.size() - 1; i++) {
                    
                    Vec3d point = new Vec3d(points3.get(i).x, firstPoint.y, points3.get(i).z);
                    point.sub(centerPoint);
                    
                    double dot = point.dot(sphereOrigin);
                    double det = ((point.x * sphereOrigin.z) - (point.z * sphereOrigin.x));
                    double angle = Math.toDegrees(Math.atan2(det, dot));
                    
                    if (angle < 0)
                        angle += 360;
                    
                    double time = angle / 360;
                    for (int j = 0; j < times.size(); j++) {
                        if (times.get(j) > time) {
                            times.add(j, time);
                            vecs.add(j, new Vec1d(points3.get(i).y));
                            newPointsSorted.add(j, points3.get(i));
                            break;
                        }
                    }
                    newPointsSorted.add(points3.get(i));
                    times.add(time);
                    vecs.add(new Vec1d(points3.get(i).y));
                }
                
                if (scene.loop == 0)
                    newPointsSorted.add(newPointsSorted.get(0).copy());
                
                times.add(1D);
                vecs.add(new Vec1d(firstPoint.y));
                
                return (Interpolation<T>) new CircularInterpolation((List<Vec3d>) points, scene.lookTarget, sphereOrigin, radius, new HermiteInterpolation<>(ArrayUtils
                        .toPrimitive(times.toArray(new Double[0])), vecs.toArray(new Vec1d[0])));
            }
        }
        return new HermiteInterpolation<T>(points);
    }
    
    public static class CircularInterpolation extends HermiteInterpolation<Vec3d> {
        
        public Vec3d sphereOrigin;
        public double radius;
        public CamTarget target;
        public HermiteInterpolation<Vec1d> yAxis;
        
        public CircularInterpolation(List<Vec3d> points, CamTarget target, Vec3d sphereOrigin, double radius, HermiteInterpolation<Vec1d> yAxis) {
            super(points);
            this.target = target;
            this.sphereOrigin = sphereOrigin;
            this.radius = radius;
            this.yAxis = yAxis;
        }
        
        @Override
        public Vec3d valueAt(double t) {
            Minecraft mc = Minecraft.getInstance();
            Vec3d center = target.position(mc.level, mc.getDeltaFrameTime());
            if (center != null) {
                Vec3d centerPoint = new Vec3d(center.x, center.y, center.z);
                
                double angle = t * 360;
                
                Vec3d newPoint = new Vec3d(sphereOrigin);
                newPoint.y = 0;
                Matrix3 matrix = new Matrix3();
                matrix.rotY(Math.toRadians(angle));
                matrix.transform(newPoint);
                
                newPoint.y = yAxis.valueAt(t).x - center.y;
                newPoint.normalize();
                newPoint.scale(radius);
                
                newPoint.add(centerPoint);
                return newPoint;
            }
            return super.valueAt(t);
        }
        
    }
}
