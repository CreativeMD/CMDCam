package team.creative.cmdcam.common.scene.run;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface CamRun {
    
    boolean playing();
    
    void pause();
    
    void resume();
    
    void stop();
    
    void finish();
    
    void renderTick(Level level, float deltaTime);
    
    void gameTick(Level level);
    
    void resetFOV();
    
    void setFOV(double value);
    
    void resetRoll();
    
    void setRoll(double value);
    
    void grabMouse();
    
    Player clientPlayer();
    
    void setCameraEntity(Entity entity);
    
    Level level();
    
    float tickTime();
    
}
