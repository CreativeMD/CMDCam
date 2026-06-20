package team.creative.cmdcam.client.scene.run;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.CamEventHandlerClient;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.math.point.CamPoints;
import team.creative.cmdcam.common.mod.minema.MinemaAddon;
import team.creative.cmdcam.common.mod.minema.MinemaTimer;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.cmdcam.common.scene.timer.RealTimeTimer;
import team.creative.cmdcam.common.scene.timer.RunTimer;

public class CamRunImpl implements CamRun {
    
    public static final CamAttribute[] PATH_ATTRIBUTES = new CamAttribute[] { CamAttribute.POSITION, CamAttribute.PITCH, CamAttribute.YAW, CamAttribute.ZOOM, CamAttribute.ROLL };
    
    public final CamScene scene;
    protected final List<CamRunStage> stages = new ArrayList<>();
    
    public double sizeOfIteration;
    
    private RunTimer timer;
    private boolean running;
    private int currentStage;
    private boolean finished;
    
    public CamRunImpl(Level level, CamScene scene) {
        var mc = Minecraft.getInstance();
        Entity camera = mc.player;
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
            stages.add(new CamRunStage(this, CamInterpolation.HERMITE, (long) Mth.clampedLerp(points.estimateLength() / 10000, 1000, 20000), 0, points));
        }
        
        { // First sequence
            CamPoints points = new CamPoints(scene.points);
            
            if (scene.loop != 0 && scene.points.size() > 1) {
                points.add(scene.points.get(0).copy());
                points.after(scene.points.get(1).copy());
            }
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.interpolation, scene.duration, 0, points) {
                
                @Override
                public void start() {
                    super.start();
                    if (MinemaAddon.installed())
                        MinemaAddon.startCapture();
                }
                
            });
        }
        
        if (scene.loop != 0 && scene.loop != 1) { // actual loop
            CamPoints points = new CamPoints(scene.points);
            points.before(scene.points.get(scene.points.size() - 1).copy());
            
            points.add(scene.points.get(0).copy());
            points.after(scene.points.get(1).copy());
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.interpolation, scene.duration, scene.loop > 0 ? scene.loop - 1 : scene.loop, points));
        }
        
        if (scene.loop > 0) { // end loop
            CamPoints points = new CamPoints(scene.points);
            points.before(scene.points.get(scene.points.size() - 1).copy());
            points.after(scene.points.get(scene.points.size() - 1).copy()); // For a slow stop
            
            points.fixSpinning(scene.pitchMode);
            
            stages.add(new CamRunStage(this, scene.interpolation, scene.duration, 0, points));
        }
        
        this.currentStage = 0;
        this.timer = MinemaAddon.installed() ? new MinemaTimer() : new RealTimeTimer();
        this.finished = false;
        this.running = true;
    }
    
    @Override
    public void renderTick(Level level, float deltaTime) {
        CamRunStage stage = stages.get(currentStage);
        
        if (!stage.hasStarted())
            stage.start();
        
        long time = position(deltaTime);
        if (!stage.endless() && time >= stage.duration) {
            
            timer.stageCompleted();
            if (stage.looped < stage.loops || stage.loops < 0)
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
        
        var hud = Minecraft.getInstance().gui.hud;
        if (!hud.isHidden())
            hud.toggle();
        scene.mode.process(this, stage.calculatePoint(level, time, deltaTime));
    }
    
    @Override
    public void gameTick(Level level) {
        timer.tick(running);
    }
    
    public CamAttribute[] attributes() {
        return PATH_ATTRIBUTES;
    }
    
    @Override
    public void finish() {
        if (MinemaAddon.installed())
            MinemaAddon.stopCapture();
    }
    
    public long position(float partialTick) {
        return timer.position(running, partialTick);
    }
    
    @Override
    public boolean playing() {
        return running;
    }
    
    public boolean done() {
        return finished;
    }
    
    @Override
    public void pause() {
        running = false;
        timer.pause();
    }
    
    @Override
    public void resume() {
        running = true;
        timer.resume();
    }
    
    @Override
    public void stop() {
        finished = true;
        running = false;
        if (MinemaAddon.installed())
            MinemaAddon.stopCapture();
    }
    
    @Override
    public void resetFOV() {
        CamEventHandlerClient.resetFOV();
    }
    
    @Override
    public void setFOV(double value) {
        CamEventHandlerClient.fov(value);
    }
    
    @Override
    public void resetRoll() {
        CamEventHandlerClient.resetRoll();
    }
    
    @Override
    public void setRoll(double value) {
        CamEventHandlerClient.roll((float) value);
    }
    
    @Override
    public void grabMouse() {
        Minecraft.getInstance().mouseHandler.grabMouse();
    }
    
    @Override
    public Player clientPlayer() {
        return Minecraft.getInstance().player;
    }
    
    @Override
    public void setCameraEntity(Entity entity) {
        Minecraft.getInstance().setCameraEntity(entity);
    }
    
    @Override
    public Level level() {
        return Minecraft.getInstance().level;
    }
    
    @Override
    public float tickTime() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
    
}
