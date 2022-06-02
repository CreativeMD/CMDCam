package team.creative.cmdcam.common.math.follow;

import team.creative.creativecore.common.util.math.vec.VecNd;

public class CamFollowStepDistance<T extends VecNd> extends CamFollow<T> {
    
    private T aimed;
    
    public CamFollowStepDistance(CamFollowConfig<T> config) {
        super(config);
    }
    
    @Override
    public T followInternal(T target) {
        if (config.div == 1)
            return (T) target.copy();
        
        if (current == null)
            current = target;
        
        if (config.threshold > 0) {
            aimed = target;
            target = aimed;
            //target.angle(current)
        } else
            aimed = target;
        
        if (config.maxSpeed > 0) {
            T speed = (T) current.copy();
            for (int i = 0; i < speed.dimensions(); i++)
                speed.set(i, (aimed.get(i) - current.get(i)) / config.div);
            if (speed.length() > config.maxSpeed) {
                speed.normalize();
                speed.scale(config.maxSpeed);
            }
            speed.add(current);
            return speed;
        }
        
        T vec = (T) current.copy();
        for (int i = 0; i < vec.dimensions(); i++)
            vec.set(i, (aimed.get(i) - current.get(i)) / config.div + current.get(i));
        
        this.current = vec;
        return vec;
    }
    
}
