package com.creativemd.cmdcam;

import com.creativemd.cmdcam.common.packet.ConnectPacket;
import com.creativemd.cmdcam.common.packet.GetPathPacket;
import com.creativemd.cmdcam.common.packet.SetPathPacket;
import com.creativemd.cmdcam.common.packet.StartPathPacket;
import com.creativemd.cmdcam.common.packet.StopPathPacket;
import com.creativemd.cmdcam.server.CamCommandServer;
import com.creativemd.cmdcam.server.CamEventHandler;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = CMDCam.modid, version = CMDCam.version, name = "CMDCam")
public class CMDCam {
	
	public static final String modid = "cmdcam";
	
	public static final String version = "1.4.0";
	
	@SidedProxy(clientSide = "com.creativemd.cmdcam.client.CMDCamClient", serverSide = "com.creativemd.cmdcam.server.CMDCamServer")
	public static CMDCamProxy proxy;
	
	@EventHandler
	public void Init(FMLInitializationEvent event) {
		CreativeCorePacket.registerPacket(ConnectPacket.class, "cmdconnect");
		CreativeCorePacket.registerPacket(StartPathPacket.class, "cmdstart");
		CreativeCorePacket.registerPacket(StopPathPacket.class, "cmdstop");
		CreativeCorePacket.registerPacket(GetPathPacket.class, "cmdget");
		CreativeCorePacket.registerPacket(SetPathPacket.class, "cmdset");
		
		MinecraftForge.EVENT_BUS.register(new CamEventHandler());
		
		proxy.init(event);
	}
	
	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		proxy.serverStarting(event);
		
		event.registerServerCommand(new CamCommandServer());
	}
}
