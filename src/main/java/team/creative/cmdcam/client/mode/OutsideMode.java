package team.creative.cmdcam.client.mode;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.common.util.CamPoint;

public class OutsideMode extends CamMode {
    
    public Entity camPlayer;
    
    public OutsideMode(CamPath path) {
        super(path);
        if (path != null)
            this.camPlayer = new ItemEntity(Minecraft.getInstance().level, 0, 0, 0);
    }
    
    @Override
    public CamMode createMode(CamPath path) {
        return new OutsideMode(path);
    }
    
    @Override
    public String getDescription() {
        return "the player isn't the camera, but you are still in control";
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPathFinish() {
        super.onPathFinish();
        Minecraft mc = Minecraft.getInstance();
        mc.cameraEntity = mc.player;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void processPoint(CamPoint point) {
        super.processPoint(point);
        
        Minecraft.getInstance().cameraEntity = camPlayer;
        if (camPlayer instanceof PlayerEntity)
            ((PlayerEntity) camPlayer).abilities.flying = true;
        
        camPlayer.absMoveTo(point.x, point.y - camPlayer.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        camPlayer.yRotO = (float) point.rotationYaw;
        camPlayer.xRotO = (float) point.rotationPitch;
        camPlayer.moveTo(point.x, point.y - camPlayer.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
}
