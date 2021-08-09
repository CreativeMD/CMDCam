package com.creativemd.cmdcam.transform;

import com.creativemd.cmdcam.client.CMDCamClient;
import com.creativemd.cmdcam.client.mode.OutsideMode;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class CamMouseOverHandler {
    
    public static Minecraft mc = Minecraft.getMinecraft();
    
    public static Entity camera = null;
    
    public static void setupMouseHandlerBefore() {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            camera = mc.getRenderViewEntity();
            mc.setRenderViewEntity(mc.player);
        }
    }
    
    public static void setupMouseHandlerAfter() {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
            mc.setRenderViewEntity(camera);
        }
    }
    
}
