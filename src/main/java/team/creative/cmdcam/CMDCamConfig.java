package team.creative.cmdcam;

import team.creative.creativecore.common.config.api.CreativeConfig;
import team.creative.creativecore.common.config.sync.ConfigSynchronization;

public class CMDCamConfig {
    
    @CreativeConfig(type = ConfigSynchronization.CLIENT)
    public boolean syncMinema = true;
    
}
