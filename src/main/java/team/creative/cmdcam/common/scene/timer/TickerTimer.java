package team.creative.cmdcam.common.scene.timer;

public class TickerTimer extends RunTimer {
    
    private static final int tickTime = 50;
    
    private long ticks = 0;
    
    @Override
    public long position(boolean running, float partialTick) {
        return (long) ((ticks - 1 + partialTick) * tickTime);
    }
    
    @Override
    public void pause() {}
    
    @Override
    public void resume() {}
    
    @Override
    public void tick(boolean running) {
        if (running)
            ticks++;
    }
    
    @Override
    public void stageCompleted() {
        ticks = 0;
    }
    
}
