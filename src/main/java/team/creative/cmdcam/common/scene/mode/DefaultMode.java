package team.creative.cmdcam.common.scene.mode;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    @OnlyIn(Dist.CLIENT)
    public void process(CamPoint point) {
        super.process(point);
        Minecraft.getInstance().mouseHandler.grabMouse();
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void finished(CamRun run) {
        super.finished(run);
        Minecraft mc = Minecraft.getInstance();
        if (!mc.player.isCreative())
            mc.player.getAbilities().flying = false;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public Entity getCamera() {
        return Minecraft.getInstance().player;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void correctTargetPosition(Vec3d vec) {
        vec.y -= Minecraft.getInstance().player.getEyeHeight();
    }
    
    @Override
    public boolean outside() {
        return false;
    }
    
}
