package team.creative.cmdcam.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

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

    public static void registerKeys() {
        KeyBindingHelper.registerKeyBinding(zoomIn);
        KeyBindingHelper.registerKeyBinding(zoomCenter);
        KeyBindingHelper.registerKeyBinding(zoomOut);
        
        KeyBindingHelper.registerKeyBinding(rollLeft);
        KeyBindingHelper.registerKeyBinding(rollCenter);
        KeyBindingHelper.registerKeyBinding(rollRight);
        
        KeyBindingHelper.registerKeyBinding(pointKey);
        KeyBindingHelper.registerKeyBinding(startStop);
        
        KeyBindingHelper.registerKeyBinding(clearPoint);
    }
}
