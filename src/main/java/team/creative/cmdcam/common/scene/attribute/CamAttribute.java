package team.creative.cmdcam.common.scene.attribute;

import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.VecNd;

public abstract class CamAttribute<T extends VecNd> {
    
    public static final CamAttribute<Vec3d> POSITION = new CamAttribute<Vec3d>() {
        
        @Override
        public Vec3d get(CamPoint point) {
            return point;
        }
        
        @Override
        public void set(CamPoint point, Vec3d vec) {
            point.set(vec);
        }
    };
    
    public static final CamAttribute<Vec1d> YAW = new CamAttribute<Vec1d>() {
        
        @Override
        public Vec1d get(CamPoint point) {
            return new Vec1d(point.rotationYaw);
        }
        
        @Override
        public void set(CamPoint point, Vec1d vec) {
            point.rotationYaw = vec.x;
        }
    };
    
    public static final CamAttribute<Vec1d> PITCH = new CamAttribute<Vec1d>() {
        
        @Override
        public Vec1d get(CamPoint point) {
            return new Vec1d(point.rotationPitch);
        }
        
        @Override
        public void set(CamPoint point, Vec1d vec) {
            point.rotationPitch = vec.x;
        }
    };
    
    public static final CamAttribute<Vec1d> ZOOM = new CamAttribute<Vec1d>() {
        
        @Override
        public Vec1d get(CamPoint point) {
            return new Vec1d(point.zoom);
        }
        
        @Override
        public void set(CamPoint point, Vec1d vec) {
            point.zoom = vec.x;
        }
    };
    
    public static final CamAttribute<Vec1d> ROLL = new CamAttribute<Vec1d>() {
        
        @Override
        public Vec1d get(CamPoint point) {
            return new Vec1d(point.roll);
        }
        
        @Override
        public void set(CamPoint point, Vec1d vec) {
            point.roll = vec.x;
        }
    };
    
    public abstract T get(CamPoint point);
    
    public abstract void set(CamPoint point, T vec);
    
}
