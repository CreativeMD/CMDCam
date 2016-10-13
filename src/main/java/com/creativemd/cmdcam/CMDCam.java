package com.creativemd.cmdcam;

import java.util.ArrayList;
import java.util.Arrays;

import com.creativemd.cmdcam.command.CamCommand;
import com.creativemd.cmdcam.key.KeyHandler;
import com.creativemd.cmdcam.movement.Movement;
import com.creativemd.cmdcam.movement.Path;
import com.creativemd.cmdcam.utils.CamPoint;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

//@Mod(modid = CMDCam.modid, version = CMDCam.version, name = "CMDCam")
public class CMDCam extends DummyModContainer {
	
	public static final String modid = "cmdcam";
	
	public static final String version = "0.2";
	
	public CMDCam() {

		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = modid;
		meta.name = "CMDCam";
		meta.version = version; //String.format("%d.%d.%d.%d", majorVersion, minorVersion, revisionVersion, buildVersion);
		meta.credits = "CreativeMD";
		meta.authorList = Arrays.asList("CreativeMD");
		meta.description = "";
		meta.url = "";
		meta.updateUrl = "";
		meta.screenshots = new String[0];
		meta.logoFile = "";
	}
	
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
	
	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}
	
	@Subscribe
    public void Init(FMLInitializationEvent event)
    {
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			ClientCommandHandler.instance.registerCommand(new CamCommand());
			KeyHandler.initKeys();
			
			MinecraftForge.EVENT_BUS.register(new CamEventHandler());
			FMLCommonHandler.instance().bus().register(new CamEventHandler());
			
			Path.initPaths();
			Movement.initMovements();
		}
    }
	
	public static void createPath()
	{
		if(points.size() < 1)
		{
			mc.thePlayer.addChatMessage(new ChatComponentText("You have to register at least 1 point!"));
			return ;
		}
		Movement movement = Movement.getMovementById(lastMovement);
		Path parser = Path.getPathById(lastPath);
		Object target = CMDCam.target;
		if(target != null && target.equals("self"))
		{
			target = mc.thePlayer;
		}
		ArrayList<CamPoint> newPoints = new ArrayList<>(points);
		if(newPoints.size() == 1)
			newPoints.add(newPoints.get(0));
		currentPath = parser.createPath(newPoints, lastDuration, movement, target);
		currentPath.movement.initMovement(newPoints);
	}
}
