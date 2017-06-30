package com.creativemd.cmdcam;

import java.util.ArrayList;

import com.creativemd.cmdcam.command.CamCommand;
import com.creativemd.cmdcam.key.KeyHandler;
import com.creativemd.cmdcam.movement.Movement;
import com.creativemd.cmdcam.movement.Path;
import com.creativemd.cmdcam.utils.CamPoint;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = CMDCam.modid, version = CMDCam.version, name = "CMDCam")
public class CMDCam {
	
	public static final String modid = "cmdcam";
	
	public static final String version = "1.1.7";
	
	public static Minecraft mc = Minecraft.getMinecraft();
	
	public static float fov;
	public static float roll = 0;
	
	public static Path currentPath = null;
	
	public static boolean loop = false;
	
	public static long lastDuration = 10000;
	public static String lastPath = "default";
	public static String lastMovement = "hermite";
	public static Object target = null;
	public static ArrayList<CamPoint> points = new ArrayList<>();

	public static double cameraFollowSpeed = 1D;
	
	@EventHandler
    public void Init(FMLInitializationEvent event)
    {
		ClientCommandHandler.instance.registerCommand(new CamCommand());
		KeyHandler.initKeys();
		
		MinecraftForge.EVENT_BUS.register(new CamEventHandler());
		
		Path.initPaths();
		Movement.initMovements();
    }
	
	public static void createPath()
	{
		if(points.size() < 1)
		{
			mc.player.sendMessage(new TextComponentString("You have to register at least 1 point!"));
			return ;
		}
		Movement movement = Movement.getMovementById(lastMovement);
		Path parser = Path.getPathById(lastPath);
		Object target = CMDCam.target;
		if(target != null && target.equals("self"))
		{
			target = mc.player;
		}
		ArrayList<CamPoint> newPoints = new ArrayList<>(points);
		if(newPoints.size() == 1)
			newPoints.add(newPoints.get(0));
		currentPath = parser.createPath(newPoints, lastDuration, movement, target);
		currentPath.movement.initMovement(newPoints);
	}
}
