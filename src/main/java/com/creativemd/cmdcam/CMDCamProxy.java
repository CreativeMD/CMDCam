package com.creativemd.cmdcam;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public abstract class CMDCamProxy {
    
    public abstract void init(FMLInitializationEvent event);
    
    public abstract void serverStarting(FMLServerStartingEvent event);
}
