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

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

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
			
			Path.initPaths();
			Movement.initMovements();
		}
    }
	
	public static void createPath()
	{
		if(points.size() < 2)
		{
			mc.thePlayer.addChatMessage(new TextComponentString("You have to register at least 2 points!"));
			return ;
		}
		Movement movement = Movement.getMovementById(lastMovement);
		Path parser = Path.getPathById(lastPath);
		Object target = CMDCam.target;
		if(target != null && target.equals("self"))
		{
			target = mc.thePlayer;
		}
		currentPath = parser.createPath(points, lastDuration, movement, target);
		currentPath.movement.initMovement(points);
	}
}
