package team.creative.cmdcam.common.scene.run;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

@OnlyIn(Dist.CLIENT)
public class CamRun {
    
    private static Minecraft mc = Minecraft.getInstance();
    
    public static final CamAttribute[] PATH_ATTRIBUTES = new CamAttribute[] { CamAttribute.POSITION, CamAttribute.PITCH, CamAttribute.YAW, CamAttribute.ZOOM, CamAttribute.ROLL };
    
    public final CamScene scene;
    protected final List<CamRunStage> stages = new ArrayList<>();
    
    public double sizeOfIteration;
    
    private long lastResumed;
    private long timePlayed;
    private boolean running;
    private int currentStage;
    private boolean finished;
    
    public CamRun(Level level, Entity camera, CamScene scene) {
        this.scene = scene;
        
        if (scene.smoothBeginning) { // Smooth start from current player position
            CamPoints points = new CamPoints();
            CamPoint camPoint = CamPoint.create(camera);
            try {
                CMDCamClient.PROCESSOR.makeRelative(scene, level, camPoint);
            } catch (SceneException e) {}
            points.add(camPoint);
            points.add(scene.points.get(0).copy());
            points.after(scene.points.get(0).copy());
            points.fixSpinning(CamPitchMode.FIX);
            stages.add(new CamRunStage(this, (long) Mth.clampedLerp(points.estimateLength() / 10, 1000, 20000), 0, points));
        }
        
        { // First sequence
            CamPoints points = new CamPoints(scene.points);
            
            if (scene.loop != 0) {
                points.add(scene.points.get(0).copy());
                points.after(scene.points.get(1).copy());
            }
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.duration, 0, points));
        }
        
        if (scene.loop != 0) { // actual loop
            CamPoints points = new CamPoints(scene.points);
            points.before(scene.points.get(scene.points.size() - 1).copy());
            
            points.add(scene.points.get(0).copy());
            points.after(scene.points.get(1).copy());
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.duration, scene.loop, points));
        }
        
        if (scene.loop > 0) { // end loop
            CamPoints points = new CamPoints(scene.points);
            points.before(scene.points.get(scene.points.size() - 1).copy());
            points.after(scene.points.get(scene.points.size() - 1).copy()); // For a slow stop
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.duration, 0, points));
        }
        
        this.currentStage = 0;
        this.lastResumed = System.currentTimeMillis();
        this.finished = false;
        this.running = true;
    }
    
    public void tick(Level level, float deltaTime) {
        CamRunStage stage = stages.get(currentStage);
        
        if (!stage.hasStarted())
            stage.start();
        
        long time = position();
        if (!stage.endless() && time >= stage.duration) {
            
            updateLastResumedTime();
            
            if (stage.looped < scene.loop || scene.loop < 0)
                stage.looped++;
            else {
                currentStage++;
                if (currentStage < stages.size()) {
                    stage = stages.get(currentStage);
                    stage.start();
                    time = 0;
                } else {
                    scene.finish(level);
                    return;
                }
            }
        }
        
        mc.options.hideGui = true;
        scene.mode.process(stage.calculatePoint(level, time, deltaTime));
    }
    
    public CamAttribute[] attributes() {
        return PATH_ATTRIBUTES;
    }
    
    private void updateLastResumedTime() {
        lastResumed = System.currentTimeMillis();
    }
    
    public void finish() {}
    
    public long position() {
        if (running)
            return timePlayed + (System.currentTimeMillis() - lastResumed);
        else
            return timePlayed;
    }
    
    public boolean playing() {
        return running;
    }
    
    public boolean done() {
        return finished;
    }
    
    public void pause() {
        running = false;
        timePlayed += System.currentTimeMillis() - lastResumed;
    }
    
    public void resume() {
        running = true;
        updateLastResumedTime();
    }
    
    public void stop() {
        finished = true;
        running = false;
    }
    
}
