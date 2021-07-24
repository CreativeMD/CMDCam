package team.creative.cmdcam.common.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.client.mode.CamMode;

public class CamPath {
    
    public int loop;
    public long duration;
    public String mode;
    public String interpolation;
    public CamTarget target;
    public List<CamPoint> points;
    public double cameraFollowSpeed;
    public boolean serverPath = false;
    
    public CamPath(CompoundTag nbt) {
        this.loop = nbt.getInt("loop");
        this.duration = nbt.getLong("duration");
        this.mode = nbt.getString("mode");
        this.interpolation = nbt.getString("interpolation");
        if (nbt.contains("target"))
            this.target = CamTarget.readFromNBT(nbt.getCompound("target"));
        ListTag list = nbt.getList("points", 10);
        this.points = new ArrayList<>();
        for (Tag point : list)
            points.add(new CamPoint((CompoundTag) point));
        this.cameraFollowSpeed = nbt.getDouble("cameraFollowSpeed");
    }
    
    public CamPath(int loop, long duration, String mode, String interpolation, CamTarget target, List<CamPoint> points, double cameraFollowSpeed) {
        this.loop = loop;
        this.duration = duration;
        this.mode = mode;
        this.interpolation = interpolation;
        this.target = target;
        this.points = points;
        this.cameraFollowSpeed = cameraFollowSpeed;
    }
    
    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putInt("loop", loop);
        nbt.putLong("duration", duration);
        nbt.putString("mode", mode);
        nbt.putString("interpolation", interpolation);
        if (target != null)
            nbt.put("target", target.writeToNBT(new CompoundTag()));
        ListTag list = new ListTag();
        for (CamPoint point : points) {
            list.add(point.writeToNBT(new CompoundTag()));
        }
        nbt.put("points", list);
        nbt.putDouble("cameraFollowSpeed", cameraFollowSpeed);
        return nbt;
    }
    
    @OnlyIn(Dist.CLIENT)
    public void overwriteClientConfig() {
        CMDCamClient.lastLoop = this.loop;
        CMDCamClient.lastDuration = this.duration;
        CMDCamClient.lastMode = this.mode;
        CMDCamClient.lastInterpolation = this.interpolation;
        CMDCamClient.target = this.target;
        CMDCamClient.points = new ArrayList<>(this.points);
        CMDCamClient.cameraFollowSpeed = this.cameraFollowSpeed;
    }
    
    @OnlyIn(Dist.CLIENT)
    private boolean hideGui;
    @OnlyIn(Dist.CLIENT)
    public CamInterpolation cachedInterpolation;
    @OnlyIn(Dist.CLIENT)
    public CamMode cachedMode;
    
    public long timeStarted = System.currentTimeMillis();
    public int currentLoop;
    private boolean finished;
    private boolean running;
    public List<CamPoint> tempPoints;
    
    public void start(Level level) throws PathParseException {
        this.finished = false;
        this.running = true;
        if (target != null)
            this.target.start(level);
        
        this.timeStarted = System.currentTimeMillis();
        this.currentLoop = 0;
        this.tempPoints = new ArrayList<>(points);
        if (loop != 0)
            this.tempPoints.add(this.tempPoints.get(this.tempPoints.size() - 1).copy());
        
        if (level.isClientSide) {
            CamMode parser = CamMode.getMode(mode);
            
            this.cachedMode = parser.createMode(this);
            this.cachedMode.onPathStart();
            
            this.cachedInterpolation = CamInterpolation.getInterpolationOrDefault(interpolation);
            this.cachedInterpolation.initMovement(tempPoints, loop, target);
            
            this.hideGui = Minecraft.getInstance().options.hideGui;
        }
    }
    
    public void finish(Level level) {
        this.finished = true;
        this.running = false;
        if (target != null)
            this.target.finish();
        
        if (level.isClientSide) {
            this.cachedMode.onPathFinish();
            this.tempPoints = null;
            
            this.cachedMode = null;
            this.cachedInterpolation = null;
            
            Minecraft.getInstance().options.hideGui = hideGui;
        }
    }
    
    public void tick(Level level, float renderTickTime) {
        long time = System.currentTimeMillis() - timeStarted;
        if (time >= duration) {
            if (currentLoop < loop || loop < 0) {
                timeStarted = System.currentTimeMillis();
                currentLoop++;
            } else
                finish(level);
        } else {
            if (level.isClientSide)
                Minecraft.getInstance().options.hideGui = true;
            
            long durationOfPoint = duration / (tempPoints.size() - 1);
            int currentPoint = Math.min((int) (time / durationOfPoint), tempPoints.size() - 2);
            CamPoint point1 = tempPoints.get(currentPoint);
            CamPoint point2 = tempPoints.get(currentPoint + 1);
            double percent = (time % durationOfPoint) / (double) durationOfPoint;
            CamPoint newPoint = cachedMode.getCamPoint(point1, point2, percent, (double) time / duration, renderTickTime, currentLoop == 0, currentLoop == loop);
            
            if (newPoint != null)
                cachedMode.processPoint(newPoint);
            
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public boolean hasFinished() {
        return finished;
    }
    
    public CamPath copy() {
        CamPath path = new CamPath(currentLoop, duration, mode, interpolation, target, new ArrayList<>(points), cameraFollowSpeed);
        path.serverPath = this.serverPath;
        return path;
    }
    
}
