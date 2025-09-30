package team.creative.cmdcam;

import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;

public class CMDCamConfig {
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public boolean syncMinema = true;
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public String defaultDuration = "10s";
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public String defaultMode = "default";
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public String defaultInterpolation = "hermite";
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public boolean defaultSmoothStart = true;
    
}
