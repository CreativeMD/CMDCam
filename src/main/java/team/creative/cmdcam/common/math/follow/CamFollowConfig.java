package team.creative.cmdcam.common.math.follow;

import net.minecraft.nbt.CompoundTag;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.vec.VecNd;

public class CamFollowConfig<T extends VecNd> {
    
    public final CamAttribute attribute;
    public String type = "step";
    
    public double div = 20;
    public double threshold;
    public double maxSpeed;
    
    public CamFollowConfig(CamAttribute attribute) {
        this.attribute = attribute;
    }
    
    public CamFollowConfig(CamAttribute attribute, double div) {
        this(attribute);
        this.div = div;
    }
    
    public CamFollowConfig(CamAttribute attribute, String type, double div) {
        this(attribute, div);
        this.type = type;
    }
    
    public CamFollow<T> create(T initial) {
        if (div < 1)
            div = 1;
        CamFollow<T> follow = CamFollow.REGISTRY.createSafe(CamFollowStepDistance.class, type, this);
        follow.setInitial(initial);
        return follow;
    }
    
    public void load(CompoundTag nbt) {
        type = nbt.getString("type");
        div = Math.max(1, nbt.getDouble("div"));
        threshold = nbt.getDouble("threshold");
        maxSpeed = nbt.getDouble("max_speed");
    }
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("type", type);
        nbt.putDouble("div", div);
        if (threshold > 0)
            nbt.putDouble("threshold", threshold);
        if (maxSpeed > 0)
            nbt.putDouble("max_speed", maxSpeed);
        return nbt;
    }
    
    public CamFollowConfig<T> copy() {
        CamFollowConfig<T> copy = new CamFollowConfig<T>(attribute);
        copy.type = type;
        copy.div = div;
        copy.threshold = threshold;
        copy.maxSpeed = maxSpeed;
        return copy;
    }
    
}
