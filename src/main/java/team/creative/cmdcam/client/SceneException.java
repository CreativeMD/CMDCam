package team.creative.cmdcam.client;

import net.minecraft.network.chat.TranslatableComponent;

public class SceneException extends Exception {
    
    public SceneException(String msg) {
        super(msg);
    }
    
    public TranslatableComponent getComponent() {
        return new TranslatableComponent(getMessage());
    }
    
}
