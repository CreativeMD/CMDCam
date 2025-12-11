package team.creative.cmdcam.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import team.creative.cmdcam.CMDCam;

public class KeyHandler {
    
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(CMDCam.MODID, "cmdcam"));
    public static final KeyMapping ZOOM_IN = new KeyMapping("key.zoomin", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ZOOM_RESET = new KeyMapping("key.centerzoom", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping ZOOM_OUT = new KeyMapping("key.zoomout", GLFW.GLFW_KEY_N, CATEGORY);
    
    public static final KeyMapping ROLL_LEFT = new KeyMapping("key.rollleft", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping ROLL_RESET = new KeyMapping("key.rollcenter", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping ROLL_RIGHT = new KeyMapping("key.rollright", GLFW.GLFW_KEY_J, CATEGORY);
    
    public static final KeyMapping POINT_ADD = new KeyMapping("key.point", GLFW.GLFW_KEY_P, CATEGORY);
    public static final KeyMapping START_STOP = new KeyMapping("key.startStop", GLFW.GLFW_KEY_U, CATEGORY);
    
    public static final KeyMapping CLEAR_POINT = new KeyMapping("key.clearPoint", GLFW.GLFW_KEY_DELETE, CATEGORY);
    
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        
        event.register(ZOOM_IN);
        event.register(ZOOM_RESET);
        event.register(ZOOM_OUT);
        
        event.register(ROLL_LEFT);
        event.register(ROLL_RESET);
        event.register(ROLL_RIGHT);
        
        event.register(POINT_ADD);
        event.register(START_STOP);
        
        event.register(CLEAR_POINT);
    }
}
