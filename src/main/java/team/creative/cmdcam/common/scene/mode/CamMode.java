package team.creative.cmdcam.common.scene.mode;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.registry.NamedTypeRegistry;

public abstract class CamMode {
    
    public static final NamedTypeRegistry<CamMode> REGISTRY = new NamedTypeRegistry<CamMode>().addConstructorPattern(CamScene.class);
    
    static {
        REGISTRY.register("default", DefaultMode.class);
        REGISTRY.register("outside", OutsideMode.class);
    }
    
    public final CamScene scene;
    
    public CamMode(CamScene scene) {
        this.scene = scene;
    }
    
    public Component title() {
        return Component.translatable("cam.mode." + REGISTRY.getId(this));
    }
    
    public void started(CamRun run) {}
    
    public void finished(CamRun run) {
        run.resetFOV();
        run.resetRoll();
    }
    
    public abstract Entity getCamera(CamRun run);
    
    public void process(CamRun run, CamPoint point) {
        run.setFOV(point.zoom);
        run.setRoll(point.roll);
        
        Entity camera = getCamera(run);
        if (camera instanceof Player p)
            p.getAbilities().flying = true;
        
        camera.absSnapTo(point.x, point.y - camera.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        camera.yRotO = (float) point.rotationYaw;
        camera.xRotO = (float) point.rotationPitch;
        camera.absSnapTo(point.x, point.y - camera.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
    public abstract boolean outside();
    
    public void correctTargetPosition(CamRun run, Vec3d vec) {}
    
}
