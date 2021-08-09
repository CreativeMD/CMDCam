package com.creativemd.cmdcam.client.mode;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget.SelfTarget;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DefaultMode extends CamMode {
    
    public DefaultMode(CamPath path) {
        super(path);
        if (path != null && path.target != null && Minecraft.getMinecraft() != null && path.target instanceof SelfTarget)
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
    @SideOnly(Side.CLIENT)
    public void processPoint(CamPoint point) {
        super.processPoint(point);
        
        Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
        Mouse.setGrabbed(true);
        
        Minecraft mc = Minecraft.getMinecraft();
        
        mc.player.capabilities.isFlying = true;
        
        mc.player.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.prevRotationYaw = (float) point.rotationYaw;
        mc.player.prevRotationPitch = (float) point.rotationPitch;
        mc.player.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void onPathFinish() {
        super.onPathFinish();
        
        Minecraft mc = Minecraft.getMinecraft();
        
        if (!mc.player.isCreative() && !mc.player.isSpectator())
            mc.player.capabilities.isFlying = false;
        
        if (Minecraft.IS_RUNNING_ON_MAC) {
            Mouse.setGrabbed(false);
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2 - 20);
        }
    }
    
}
