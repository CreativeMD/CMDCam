package team.creative.cmdcam.common.math.follow;

import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.registry.NamedTypeRegistry;

public abstract class CamFollow<T extends VecNd> {
    
    public static final NamedTypeRegistry<CamFollow> REGISTRY = new NamedTypeRegistry<CamFollow>().addConstructorPattern(CamFollowConfig.class);
    
    static {
        REGISTRY.register("step", CamFollowStepDistance.class);
    }
    
    protected T current;
    public CamFollowConfig<T> config;
    
    public CamFollow(CamFollowConfig<T> config) {
        this.config = config;
    }
    
    public void setInitial(T initial) {
        this.current = initial;
    }
    
    private void makeInBounds(T vec) {
        for (int i = 0; i < vec.dimensions(); i++) {
            double value = vec.get(i);
            double min = config.attribute.getMin().get(i);
            double max = config.attribute.getMax().get(i);
            if (value >= min && value <= max)
                continue;
            
            value -= min;
            value %= max - min;
            if (value < 0)
                value += max - min;
            value += min;
            
            vec.set(i, value);
        }
    }
    
    public T follow(T target) {
        if (config.attribute.getMin() != null) {
            makeInBounds(target);
            makeInBounds(current);
            
            for (int i = 0; i < target.dimensions(); i++) {
                double valueT = target.get(i);
                double valueC = current.get(i);
                
                double min = config.attribute.getMin().get(i);
                double max = config.attribute.getMax().get(i);
                
                if (valueC > valueT) {
                    if (valueC - valueT > max - valueC + valueT - min)
                        valueT += max - min;
                } else if (valueC < valueT) {
                    if (valueT - valueC > max - valueT + valueC - min)
                        valueT -= max - min;
                }
                
                target.set(i, valueT);
            }
        }
        
        return followInternal(target);
    }
    
    protected abstract T followInternal(T target);
    
}
