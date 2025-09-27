package team.creative.cmdcam.common.scene.mode;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;
import team.creative.creativecore.common.util.mc.TickUtils;

public class OutsideMode extends CamMode {
    
    public Entity camPlayer;
    
    public OutsideMode(CamScene scene) {
        super(scene);
    }
    
    @Override
    public void started(CamRun run) {
        var player = run.clientPlayer();
        Vec3 vec = player.getEyePosition(TickUtils.getFrameTime(player.level()));
        this.camPlayer = new ItemEntity(player.level(), vec.x, vec.y, vec.z, ItemStack.EMPTY);
        this.camPlayer.setOldPosAndRot();
    }
    
    @Override
    public void finished(CamRun run) {
        super.finished(run);
        run.setCameraEntity(run.clientPlayer());
    }
    
    @Override
    public Entity getCamera(CamRun run) {
        return camPlayer;
    }
    
    @Override
    public void process(CamRun run, CamPoint point) {
        super.process(run, point);
        run.setCameraEntity(camPlayer);
    }
    
    @Override
    public boolean outside() {
        return true;
    }
    
}
