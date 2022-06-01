package team.creative.cmdcam.common.math.follow;

import net.minecraft.nbt.CompoundTag;
import team.creative.creativecore.common.util.math.vec.VecNd;

public class CamFollowStepDistance<T extends VecNd> extends CamFollow<T> {
    
    private T aimed;
    private double stepDiv;
    private double threshold;
    private double maxSpeed;
    
    public CamFollowStepDistance(CamFollowConfig config) {
        super();
        this.stepDiv = Math.max(1, config.div);
        this.threshold = config.threshold;
        this.maxSpeed = config.maxSpeed;
    }
    
    public CamFollowStepDistance() {
        super();
        this.stepDiv = 1;
        this.threshold = -1;
        this.maxSpeed = -1;
    }
    
    @Override
    public T follow(T target) {
        if (stepDiv == 1)
            return (T) target.copy();
        
        if (current == null)
            current = target;
        
        if (threshold > 0) {
            aimed = target;
            target = aimed;
            //target.angle(current)
        }
        
        if (maxSpeed > 0) {
            T speed = (T) current.copy();
            for (int i = 0; i < speed.dimensions(); i++)
                speed.set(i, (target.get(i) - current.get(i)) / stepDiv);
            if (speed.length() > maxSpeed) {
                speed.normalize();
                speed.scale(maxSpeed);
            }
            speed.add(current);
            return speed;
        }
        
        T vec = (T) current.copy();
        for (int i = 0; i < vec.dimensions(); i++)
            vec.set(i, (target.get(i) - current.get(i)) / stepDiv + current.get(i));
        
        this.current = vec;
        return vec;
    }
    
    @Override
    protected void loadExtra(CompoundTag nbt) {
        stepDiv = Math.max(1, nbt.getDouble("div"));
        if (nbt.contains("threshold"))
            threshold = nbt.getDouble("threshold");
        else
            threshold = -1;
        if (nbt.contains("max"))
            maxSpeed = nbt.getDouble("max");
        else
            maxSpeed = -1;
    }
    
    @Override
    protected CompoundTag saveExtra(CompoundTag nbt) {
        nbt.putDouble("div", stepDiv);
        if (threshold > 0)
            nbt.putDouble("threshold", threshold);
        if (maxSpeed > 0)
            nbt.putDouble("max", maxSpeed);
        return nbt;
    }
    
}
