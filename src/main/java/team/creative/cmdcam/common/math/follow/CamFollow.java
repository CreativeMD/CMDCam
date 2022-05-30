package team.creative.cmdcam.common.math.follow;

import net.minecraft.nbt.CompoundTag;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.registry.NamedTypeRegistry;

public abstract class CamFollow<T extends VecNd> {
    
    public static final NamedTypeRegistry<CamFollow> REGISTRY = new NamedTypeRegistry<CamFollow>().addConstructorPattern(VecNd.class, CamFollowConfig.class);
    
    public static <T extends VecNd> CamFollow<T> load(CompoundTag nbt, Class<? extends T> clazz) {
        CamFollow follow = REGISTRY.createSafe(CamFollowStepDistance.class, nbt.getString("id"));
        follow.loadExtra(nbt);
        return follow;
    }
    
    static {
        REGISTRY.register("step_distance", CamFollowStepDistance.class);
    }
    
    protected T current;
    
    public CamFollow(T initial) {}
    
    public abstract T follow(T target);
    
    protected abstract void loadExtra(CompoundTag nbt);
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("id", REGISTRY.getId(this));
        saveExtra(nbt);
        return nbt;
    }
    
    protected abstract CompoundTag saveExtra(CompoundTag nbt);
    
}
