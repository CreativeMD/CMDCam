package team.creative.cmdcam.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class KeyHandler {
	
	public static KeyBinding zoomIn = new KeyBinding("key.zoomin", GLFW.GLFW_KEY_V, "key.categories.cmdcam");
	public static KeyBinding zoomCenter = new KeyBinding("key.centerzoom", GLFW.GLFW_KEY_B, "key.categories.cmdcam");
	public static KeyBinding zoomOut = new KeyBinding("key.zoomout", GLFW.GLFW_KEY_N, "key.categories.cmdcam");
	
	public static KeyBinding rollLeft = new KeyBinding("key.rollleft", GLFW.GLFW_KEY_G, "key.categories.cmdcam");
	public static KeyBinding rollCenter = new KeyBinding("key.rollcenter", GLFW.GLFW_KEY_H, "key.categories.cmdcam");
	public static KeyBinding rollRight = new KeyBinding("key.rollright", GLFW.GLFW_KEY_J, "key.categories.cmdcam");
	
	public static KeyBinding pointKey = new KeyBinding("key.point", GLFW.GLFW_KEY_P, "key.categories.cmdcam");
	public static KeyBinding startStop = new KeyBinding("key.startStop", GLFW.GLFW_KEY_U, "key.categories.cmdcam");
	
	public static void initKeys() {
		ClientRegistry.registerKeyBinding(zoomIn);
		ClientRegistry.registerKeyBinding(zoomCenter);
		ClientRegistry.registerKeyBinding(zoomOut);
		
		ClientRegistry.registerKeyBinding(rollLeft);
		ClientRegistry.registerKeyBinding(rollCenter);
		ClientRegistry.registerKeyBinding(rollRight);
		
		ClientRegistry.registerKeyBinding(pointKey);
		ClientRegistry.registerKeyBinding(startStop);
	}
}
