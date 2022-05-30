package team.creative.cmdcam.common.scene.mode;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.run.CamRun;

public class OutsideMode extends CamMode {
    
    public Entity camPlayer;
    
    public OutsideMode(CamScene scene) {
        super(scene);
        this.camPlayer = new ItemEntity(Minecraft.getInstance().level, 0, 0, 0, ItemStack.EMPTY);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void finished(CamRun run) {
        super.finished(run);
        Minecraft mc = Minecraft.getInstance();
        mc.cameraEntity = mc.player;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public Entity getCamera() {
        return camPlayer;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void process(CamPoint point) {
        super.process(point);
        Minecraft.getInstance().cameraEntity = camPlayer;
    }
    
}
