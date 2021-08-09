package team.creative.cmdcam.client.mode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget.SelfTarget;

public class DefaultMode extends CamMode {
    
    public DefaultMode(CamPath path) {
        super(path);
        if (path != null && path.target != null && path.target instanceof SelfTarget)
            path.target = null;
    }
    
    @Override
    public CamMode createMode(CamPath path) {
        return new DefaultMode(path);
    }
    
    @Override
    public String getDescription() {
        return "the player acts as the camera";
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void processPoint(CamPoint point) {
        super.processPoint(point);
        //mc.mouseHelper.grabMouse();
        
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.getWindow().getWidth() / 2;
        double mouseY = mc.getWindow().getHeight() / 2;
        InputMappings.grabOrReleaseMouse(mc.getWindow().getWindow(), 212995, mouseX, mouseY);
        
        mc.player.abilities.flying = true;
        
        mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.yRotO = (float) point.rotationYaw;
        mc.player.xRotO = (float) point.rotationPitch;
        mc.player.moveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPathFinish() {
        super.onPathFinish();
        Minecraft mc = Minecraft.getInstance();
        if (!mc.player.isCreative())
            mc.player.abilities.flying = false;
    }
    
}
