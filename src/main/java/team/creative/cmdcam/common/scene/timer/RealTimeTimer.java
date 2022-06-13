package team.creative.cmdcam.common.scene.timer;

public class RealTimeTimer extends RunTimer {
    
    private long lastResumed;
    private long timePlayed;
    
    public RealTimeTimer() {
        this.lastResumed = System.currentTimeMillis();
    }
    
    @Override
    public long position(boolean running, float partialTick) {
        if (running)
            return timePlayed + (System.currentTimeMillis() - lastResumed);
        else
            return timePlayed;
    }
    
    @Override
    public void pause() {
        timePlayed += System.currentTimeMillis() - lastResumed;
    }
    
    @Override
    public void resume() {
        lastResumed = System.currentTimeMillis();
    }
    
    @Override
    public void stageCompleted() {
        this.lastResumed = System.currentTimeMillis();
    }
    
    @Override
    public void tick(boolean running) {}
    
}
