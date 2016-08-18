package com.creativemd.cmdcam.key;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class KeyHandler {
	
	public static KeyBinding zoomIn = new KeyBinding("key.zoomin", Keyboard.KEY_V, "key.categories.cmdcam");
	public static KeyBinding zoomCenter = new KeyBinding("key.centerzoom", Keyboard.KEY_B, "key.categories.cmdcam");
	public static KeyBinding zoomOut = new KeyBinding("key.zoomout", Keyboard.KEY_N, "key.categories.cmdcam");
	
	public static KeyBinding rollLeft = new KeyBinding("key.rollleft", Keyboard.KEY_G, "key.categories.cmdcam");
	public static KeyBinding rollCenter = new KeyBinding("key.rollcenter", Keyboard.KEY_H, "key.categories.cmdcam");
	public static KeyBinding rollRight = new KeyBinding("key.rollright", Keyboard.KEY_J, "key.categories.cmdcam");
	
	public static KeyBinding pointKey = new KeyBinding("key.point", Keyboard.KEY_P, "key.categories.cmdcam");
	public static KeyBinding startStop = new KeyBinding("key.startStop", Keyboard.KEY_U, "key.categories.cmdcam");
	
	public static void initKeys()
	{
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
