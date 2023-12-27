package team.creative.cmdcam.common.scene.mode;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CamEventHandlerClient;
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
    
    @OnlyIn(Dist.CLIENT)
    public void started(CamRun run) {}
    
    @OnlyIn(Dist.CLIENT)
    public void finished(CamRun run) {
        CamEventHandlerClient.resetFOV();
        CamEventHandlerClient.resetRoll();
    }
    
    @OnlyIn(Dist.CLIENT)
    public abstract Entity getCamera();
    
    @OnlyIn(Dist.CLIENT)
    public void process(CamPoint point) {
        CamEventHandlerClient.roll((float) point.roll);
        CamEventHandlerClient.fov(point.zoom);
        
        Entity camera = getCamera();
        if (camera instanceof Player)
            ((Player) camera).getAbilities().flying = true;
        
        camera.absMoveTo(point.x, point.y - camera.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        camera.yRotO = (float) point.rotationYaw;
        camera.xRotO = (float) point.rotationPitch;
        camera.moveTo(point.x, point.y - camera.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
    public abstract boolean outside();
    
    @OnlyIn(Dist.CLIENT)
    public void correctTargetPosition(Vec3d vec) {}
    
}
