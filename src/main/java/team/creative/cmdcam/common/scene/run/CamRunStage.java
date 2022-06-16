package team.creative.cmdcam.common.scene.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.common.math.follow.CamFollow;
import team.creative.cmdcam.common.math.follow.CamFollowConfig;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.creativecore.common.util.math.interpolation.Interpolation;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.math.vec.VecNd;

public class CamRunStage {
    
    public final CamRun run;
    public final long duration;
    public final int loops;
    public int looped = 0;
    private boolean started = false;
    private HashMap<CamAttribute, Interpolation> attributes = new HashMap<>();
    private HashMap<CamAttribute, CamFollow> followAttributes;
    
    public CamRunStage(CamRun run, long duration, int loops, CamPoints points) {
        this.run = run;
        this.duration = duration;
        this.loops = loops;
        
        double[] times = points.createTimes(run.scene);
        
        CamAttribute[] toStore = run.attributes();
        for (int i = 0; i < toStore.length; i++) {
            List vecs = new ArrayList(points.size());
            for (CamPoint point : points)
                vecs.add(toStore[i].get(point));
            attributes.put(toStore[i], points.interpolate(times, run.scene, toStore[i]));
        }
        
    }
    
    public boolean hasStarted() {
        return started;
    }
    
    private <T extends VecNd> void addFollow(CamAttribute<T> attribute, CamFollowConfig<T> config, CamPoint point) {
        followAttributes.put(attribute, config.create(attribute.get(point)));
    }
    
    public void start() {
        followAttributes = new HashMap<>();
        CamPoint initial = CamPoint.create(run.scene.mode.getCamera());
        
        if (run.scene.lookTarget != null) {
            addFollow(CamAttribute.PITCH, run.scene.pitchFollowConfig, initial);
            addFollow(CamAttribute.YAW, run.scene.yawFollowConfig, initial);
        }
        
        if (run.scene.posTarget != null)
            addFollow(CamAttribute.POSITION, run.scene.posFollowConfig, initial);
        
        started = true;
    }
    
    public CamPoint calculatePoint(Level level, long position, float partialTicks) {
        double progress = position / (double) duration;
        
        HashMap<CamAttribute, VecNd> generated = new HashMap<>();
        for (Entry<CamAttribute, Interpolation> entry : attributes.entrySet())
            generated.put(entry.getKey(), entry.getValue().valueAt(progress));
        
        CamPoint point = new CamPoint(generated);
        
        CamPoint targetPoint = new CamPoint(0, 0, 0, 0, 0, 0, 0);
        Entity camera = run.scene.mode.getCamera();
        Vec3d camPos = new Vec3d(camera.getPosition(partialTicks));
        
        if (run.scene.lookTarget != null) {
            Vec3d vec = run.scene.lookTarget.position(level, partialTicks);
            
            if (vec != null) {
                double d0 = vec.x - camPos.x;
                double d1 = vec.y - camPos.y;
                double d2 = vec.z - camPos.z;
                
                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                targetPoint.rotationPitch = (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
                targetPoint.rotationYaw = (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0D;
            }
        }
        
        if (run.scene.posTarget != null) {
            targetPoint.set(point);
            targetPoint.add(new Vec3d(run.scene.posTarget.position(level, partialTicks)));
        }
        
        for (Entry<CamAttribute, CamFollow> entry : followAttributes.entrySet())
            entry.getKey().set(point, entry.getValue().follow(entry.getKey().get(targetPoint)));
        
        return point;
    }
    
    public boolean endless() {
        return loops < 0;
    }
    
}
