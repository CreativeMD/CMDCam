package team.creative.cmdcam.common.math.point;

import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import team.creative.cmdcam.client.CamEventHandlerClient;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.VecNd;
import team.creative.creativecore.common.util.mc.TickUtils;

public class CamPoint extends Vec3d {
    
    public static CamPoint create(Entity entity) {
        float partialTicks = TickUtils.getFrameTime(entity.level());
        Vec3 vec = entity.getEyePosition(partialTicks);
        if (entity.level().isClientSide())
            return new CamPoint(vec.x, vec.y, vec.z, entity.getViewYRot(partialTicks), entity.getViewXRot(partialTicks), CamEventHandlerClient.roll(), CamEventHandlerClient
                    .fovExact(partialTicks));
        else
            return new CamPoint(vec.x, vec.y, vec.z, entity.getViewYRot(partialTicks), entity.getViewXRot(partialTicks), 0, 70);
    }
    
    public double rotationYaw;
    public double rotationPitch;
    
    public double roll;
    public double zoom;
    
    public CamPoint(double x, double y, double z, double rotationYaw, double rotationPitch, double roll, double zoom) {
        super(x, y, z);
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        this.roll = roll;
        this.zoom = zoom;
    }
    
    public CamPoint(HashMap<CamAttribute, VecNd> attributes) {
        super();
        for (Entry<CamAttribute, VecNd> entry : attributes.entrySet())
            entry.getKey().set(this, entry.getValue());
    }
    
    public CamPoint(CompoundTag nbt) {
        super(nbt.getDoubleOr("x", 0), nbt.getDoubleOr("y", 0), nbt.getDoubleOr("z", 0));
        this.rotationYaw = nbt.getDoubleOr("rotationYaw", 0);
        this.rotationPitch = nbt.getDoubleOr("rotationPitch", 0);
        this.roll = nbt.getDoubleOr("roll", 0);
        this.zoom = nbt.getDoubleOr("zoom", 0);
    }
    
    public final Vec3d calculateViewVector() {
        float f = (float) (rotationPitch * (Math.PI / 180F));
        float f1 = (float) (-rotationYaw * (Math.PI / 180F));
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3d(f3 * f4, (-f5), f2 * f4);
    }
    
    @Override
    public CamPoint copy() {
        return new CamPoint(x, y, z, rotationYaw, rotationPitch, roll, zoom);
    }
    
    @Override
    public String toString() {
        return "x:" + x + ",y:" + y + ",z:" + z + ",yaw:" + rotationYaw + ",pitch:" + rotationPitch + ",roll:" + roll + ",zoom:" + zoom;
    }
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putDouble("x", x);
        nbt.putDouble("y", y);
        nbt.putDouble("z", z);
        nbt.putDouble("rotationYaw", rotationYaw);
        nbt.putDouble("rotationPitch", rotationPitch);
        nbt.putDouble("roll", roll);
        nbt.putDouble("zoom", zoom);
        return nbt;
    }
    
}
