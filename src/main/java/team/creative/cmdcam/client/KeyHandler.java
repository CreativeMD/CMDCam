package team.creative.cmdcam.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public class KeyHandler {
    
    public static KeyMapping zoomIn = new KeyMapping("key.zoomin", GLFW.GLFW_KEY_V, "key.categories.cmdcam");
    public static KeyMapping zoomCenter = new KeyMapping("key.centerzoom", GLFW.GLFW_KEY_B, "key.categories.cmdcam");
    public static KeyMapping zoomOut = new KeyMapping("key.zoomout", GLFW.GLFW_KEY_N, "key.categories.cmdcam");
    
    public static KeyMapping rollLeft = new KeyMapping("key.rollleft", GLFW.GLFW_KEY_G, "key.categories.cmdcam");
    public static KeyMapping rollCenter = new KeyMapping("key.rollcenter", GLFW.GLFW_KEY_H, "key.categories.cmdcam");
    public static KeyMapping rollRight = new KeyMapping("key.rollright", GLFW.GLFW_KEY_J, "key.categories.cmdcam");
    
    public static KeyMapping pointKey = new KeyMapping("key.point", GLFW.GLFW_KEY_P, "key.categories.cmdcam");
    public static KeyMapping startStop = new KeyMapping("key.startStop", GLFW.GLFW_KEY_U, "key.categories.cmdcam");
    
    public static KeyMapping clearPoint = new KeyMapping("key.clearPoint", GLFW.GLFW_KEY_DELETE, "key.categories.cmdcam");
    
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(zoomIn);
        event.register(zoomCenter);
        event.register(zoomOut);
        
        event.register(rollLeft);
        event.register(rollCenter);
        event.register(rollRight);
        
        event.register(pointKey);
        event.register(startStop);
        
        event.register(clearPoint);
    }
}
