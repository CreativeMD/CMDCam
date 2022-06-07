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
        
        @Override
        public String name() {
            return "pos";
        }
    };
    
    public static final CamAttribute<Vec1d> YAW = new CamAttribute<Vec1d>() {
        
        private final Vec1d min = new Vec1d(-270);
        private final Vec1d max = new Vec1d(90);
        
        @Override
        public Vec1d get(CamPoint point) {
            return new Vec1d(point.rotationYaw);
        }
        
        @Override
        public void set(CamPoint point, Vec1d vec) {
            point.rotationYaw = vec.x;
        }
        
        @Override
        public String name() {
            return "yaw";
        }
        
        @Override
        public boolean hasBounds() {
            return true;
        }
        
        @Override
        public Vec1d getMin() {
            return min;
        }
        
        @Override
        public Vec1d getMax() {
            return max;
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
        
        @Override
        public String name() {
            return "pitch";
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
        
        @Override
        public String name() {
            return "zoom";
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
        
        @Override
        public String name() {
            return "roll";
        }
        
    };
    
    public abstract T get(CamPoint point);
    
    public abstract void set(CamPoint point, T vec);
    
    public boolean hasBounds() {
        return false;
    }
    
    public T getMin() {
        return null;
    }
    
    public T getMax() {
        return null;
    }
    
    public abstract String name();
    
    @Override
    public String toString() {
        return name();
    }
    
}
