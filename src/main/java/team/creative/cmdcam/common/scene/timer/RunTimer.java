package team.creative.cmdcam.common.scene.timer;

public abstract class RunTimer {
    
    public abstract long position(boolean running, float partialTick);
    
    public abstract void pause();
    
    public abstract void resume();
    
    public abstract void tick(boolean running);
    
    public abstract void stageCompleted();
    
}
