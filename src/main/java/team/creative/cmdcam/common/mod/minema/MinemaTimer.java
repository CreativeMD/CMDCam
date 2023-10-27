package team.creative.cmdcam.common.mod.minema;

import java.util.concurrent.TimeUnit;

import team.creative.cmdcam.common.scene.timer.RunTimer;

public class MinemaTimer extends RunTimer {
    
    private long lastResumed;
    private long timeSinceLastStage = 0;
    private long timePlayed;
    
    public MinemaTimer() {
        this.lastResumed = System.currentTimeMillis();
    }
    
    @Override
    public long position(boolean running, float partialTick) {
        if (MinemaAddon.isCapturing())
            return TimeUnit.NANOSECONDS.toMillis(MinemaAddon.getVideoTime() - timeSinceLastStage);
        if (running)
            return timePlayed + (System.currentTimeMillis() - lastResumed);
        else
            return timePlayed;
    }
    
    @Override
    public void pause() {
        if (MinemaAddon.isCapturing())
            MinemaAddon.pauseCapture();
        else
            timePlayed += System.currentTimeMillis() - lastResumed;
    }
    
    @Override
    public void resume() {
        if (MinemaAddon.isCapturing())
            MinemaAddon.resumeCapture();
        else
            lastResumed = System.currentTimeMillis();
    }
    
    @Override
    public void tick(boolean running) {}
    
    @Override
    public void stageCompleted() {
        if (MinemaAddon.isCapturing())
            timeSinceLastStage = MinemaAddon.getVideoTime();
        else
            this.lastResumed = System.currentTimeMillis();
    }
    
}
