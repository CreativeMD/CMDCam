package de.creativemd.cmdcam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.creativemd.cmdcam.client.CMDCamClient;
import de.creativemd.cmdcam.common.packet.ConnectPacket;
import de.creativemd.cmdcam.common.packet.GetPathPacket;
import de.creativemd.cmdcam.common.packet.SetPathPacket;
import de.creativemd.cmdcam.common.packet.StartPathPacket;
import de.creativemd.cmdcam.common.packet.StopPathPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import team.creative.creativecore.common.network.CreativeNetwork;

@Mod(value = CMDCam.MODID)
public class CMDCam {
	
	public static final String MODID = "cmdcam";
	
	private static final Logger LOGGER = LogManager.getLogger();
	public static CreativeNetwork NETWORK;
	
	public CMDCam() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::client);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
	}
	
	private void client(final FMLClientSetupEvent event) {
		CMDCamClient.init(event);
	}
	
	private void init(final FMLCommonSetupEvent event) {
		NETWORK = new CreativeNetwork("1.0", LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
		NETWORK.registerType(ConnectPacket.class);
		NETWORK.registerType(GetPathPacket.class);
		NETWORK.registerType(SetPathPacket.class);
		NETWORK.registerType(StartPathPacket.class);
		NETWORK.registerType(StopPathPacket.class);
	}
	
	private void serverStart(FMLServerStartingEvent event) {
		event.getCommandDispatcher().register(command)
	}
}
