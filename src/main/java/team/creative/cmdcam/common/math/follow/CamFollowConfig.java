package team.creative.cmdcam.common.math.follow;

import net.minecraft.nbt.CompoundTag;
import team.creative.creativecore.common.util.math.vec.VecNd;

public class CamFollowConfig<T extends VecNd> {
    
    public String type;
    
    public double div;
    public double threshold;
    public double maxSpeed;
    
    public CamFollow<T> create(T initial) {
        return CamFollow.REGISTRY.createSafe(CamFollowStepDistance.class, type, initial, this);
    }
    
    public void load(CompoundTag nbt) {
        type = nbt.getString("type");
        div = nbt.getDouble("div");
        threshold = nbt.getDouble("threshold");
        maxSpeed = nbt.getDouble("max_speed");
    }
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("type", type);
        nbt.putDouble("div", div);
        nbt.putDouble("threshold", threshold);
        nbt.putDouble("max_speed", maxSpeed);
        return nbt;
    }
    
    public CamFollowConfig<T> copy() {
        CamFollowConfig<T> copy = new CamFollowConfig<T>();
        copy.type = type;
        copy.div = div;
        copy.threshold = threshold;
        copy.maxSpeed = maxSpeed;
        return copy;
    }
    
}
