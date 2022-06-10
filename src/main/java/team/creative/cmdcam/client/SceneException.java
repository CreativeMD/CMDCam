package team.creative.cmdcam.client;

import net.minecraft.network.chat.Component;

public class SceneException extends Exception {
    
    public SceneException(String msg) {
        super(msg);
    }
    
    public Component getComponent() {
        return Component.translatable(getMessage());
    }
    
}
