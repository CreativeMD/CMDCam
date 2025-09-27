package team.creative.cmdcam.common.scene.mode;

import net.minecraft.world.entity.Entity;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.cmdcam.common.target.CamTarget.SelfTarget;
import team.creative.creativecore.common.util.math.vec.Vec3d;

public class DefaultMode extends CamMode {
    
    public DefaultMode(CamScene scene) {
        super(scene);
        if (scene.lookTarget instanceof SelfTarget)
            scene.lookTarget = null;
        if (scene.posTarget instanceof SelfTarget)
            scene.posTarget = null;
    }
    
    @Override
    public void process(CamRun run, CamPoint point) {
        super.process(run, point);
        run.grabMouse();
    }
    
    @Override
    public void finished(CamRun run) {
        super.finished(run);
        var player = run.clientPlayer();
        if (!player.isCreative())
            player.getAbilities().flying = false;
    }
    
    @Override
    public Entity getCamera(CamRun run) {
        return run.clientPlayer();
    }
    
    @Override
    public void correctTargetPosition(CamRun run, Vec3d vec) {
        vec.y -= run.clientPlayer().getEyeHeight();
    }
    
    @Override
    public boolean outside() {
        return false;
    }
    
}
