package team.creative.cmdcam.common.scene;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.common.math.follow.CamFollowConfig;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.mode.CamMode;
import team.creative.cmdcam.common.scene.mode.DefaultMode;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vec1d;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public class CamScene {
    
    private boolean started = false;
    
    private boolean serverSynced = false;
    
    public long duration;
    public int loop = 0;
    
    public CamMode mode;
    public CamInterpolation interpolation;
    
    public CamTarget lookTarget;
    public CamFollowConfig<Vec1d> pitchFollowConfig = new CamFollowConfig<>(10);
    public CamFollowConfig<Vec1d> yawFollowConfig = new CamFollowConfig<>(10);
    
    /** if null it will be the same as the lookTarget */
    public CamTarget posTarget;
    public CamFollowConfig<Vec3d> posFollowConfig = new CamFollowConfig<>(2);
    
    //public boolean targetBodyRotation = false;
    //public boolean targetHeadRotation = false;
    
    public List<CamPoint> points;
    
    public boolean smoothBeginning = true;
    public CamPitchMode pitchMode = CamPitchMode.FIX_KEEP_DIRECTION;
    public boolean distanceBasedTiming = false;
    
    @OnlyIn(Dist.CLIENT)
    public CamRun run;
    
    public CamScene(long duration, int loop, String mode, List<CamPoint> points, CamInterpolation interpolation) {
        this.duration = duration;
        this.mode = CamMode.REGISTRY.createSafe(DefaultMode.class, mode, this);
        this.points = points;
        this.interpolation = interpolation;
    }
    
    public CamScene(CompoundTag nbt) throws RegistryException {
        this.duration = nbt.getLong("duration");
        this.loop = nbt.getInt("loop");
        
        this.mode = CamMode.REGISTRY.createSafe(DefaultMode.class, nbt.getString("mode"), this);
        this.interpolation = CamInterpolation.REGISTRY.get(nbt.getString("inter"));
        
        this.lookTarget = nbt.contains("look_target") ? CamTarget.load(nbt.getCompound("look_target")) : null;
        this.pitchFollowConfig.load(nbt.getCompound("pitch"));
        this.yawFollowConfig.load(nbt.getCompound("yaw"));
        
        this.posTarget = nbt.contains("pos_target") ? CamTarget.load(nbt.getCompound("pos_target")) : null;
        this.posFollowConfig.load(nbt.getCompound("pos"));
        
        ListTag list = nbt.getList("points", 10);
        this.points = new ArrayList<>();
        for (Tag point : list)
            points.add(new CamPoint((CompoundTag) point));
        
        this.smoothBeginning = nbt.getBoolean("smooth_start");
        this.pitchMode = CamPitchMode.values()[nbt.getInt("pitch_mode")];
        this.distanceBasedTiming = nbt.getBoolean("d_timing");
    }
    
    public void setServerSynced() {
        serverSynced = true;
    }
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putLong("duration", duration);
        nbt.putInt("loop", loop);
        
        nbt.putString("mode", CamMode.REGISTRY.getId(mode));
        nbt.putString("inter", CamInterpolation.REGISTRY.getId(interpolation));
        
        if (lookTarget != null)
            nbt.put("look_target", lookTarget.save(new CompoundTag()));
        nbt.put("pitch", pitchFollowConfig.save(new CompoundTag()));
        nbt.put("yaw", yawFollowConfig.save(new CompoundTag()));
        
        if (posTarget != null)
            nbt.put("pos_target", posTarget.save(new CompoundTag()));
        nbt.put("pos", posFollowConfig.save(new CompoundTag()));
        
        ListTag list = new ListTag();
        for (CamPoint point : points)
            list.add(point.save(new CompoundTag()));
        nbt.put("points", list);
        
        nbt.putBoolean("smooth_start", smoothBeginning);
        nbt.putInt("pitch_mode", pitchMode.ordinal());
        nbt.putBoolean("d_timing", distanceBasedTiming);
        
        return nbt;
    }
    
    public boolean endless() {
        return loop < 0;
    }
    
    public boolean serverSynced() {
        return serverSynced;
    }
    
    public void play() {
        started = true;
    }
    
    public void pause() {
        run.pause();
    }
    
    public void resume() {
        run.resume();
    }
    
    public void stop() {
        run.stop();
    }
    
    public boolean playing() {
        return run != null && run.playing();
    }
    
    protected void started(Level level) {
        if (lookTarget != null)
            lookTarget.start(level);
        if (posTarget != null)
            posTarget.start(level);
        
        if (level.isClientSide)
            run = new CamRun(level, Minecraft.getInstance().player, this);
    }
    
    public void finish(Level level) {
        if (lookTarget != null)
            lookTarget.finish();
        if (posTarget != null)
            posTarget.finish();
        
        stop();
        if (level.isClientSide) {
            mode.finished(run);
            run.finish();
            run = null;
        }
        
        started = false;
    }
    
    public void tick(Level level, float deltaTime) {
        if (started) {
            started = false;
            started(level);
        }
        
        run.tick(level, deltaTime);
    }
    
    public void set(CamScene scene) {
        this.duration = scene.duration;
        this.loop = scene.loop;
        this.mode = CamMode.REGISTRY.createSafe(DefaultMode.class, CamMode.REGISTRY.getId(scene.mode), this);
        this.points = copyPoints();
        this.interpolation = scene.interpolation;
        this.serverSynced = scene.serverSynced;
        this.lookTarget = scene.lookTarget;
        this.pitchFollowConfig = scene.pitchFollowConfig;
        this.yawFollowConfig = scene.yawFollowConfig;
        this.posTarget = scene.posTarget;
        this.posFollowConfig = scene.posFollowConfig;
        this.smoothBeginning = scene.smoothBeginning;
        this.pitchMode = scene.pitchMode;
        this.distanceBasedTiming = scene.distanceBasedTiming;
    }
    
    private List<CamPoint> copyPoints() {
        List<CamPoint> newPoints = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++)
            newPoints.add(points.get(i).copy());
        return newPoints;
    }
    
    public CamScene copy() {
        CamScene scene = new CamScene(duration, loop, CamMode.REGISTRY.getId(mode), copyPoints(), interpolation);
        scene.set(this);
        return scene;
    }
    
}
