package de.creativemd.cmdcam.server;

import de.creativemd.cmdcam.CMDCam;
import de.creativemd.cmdcam.common.packet.ConnectPacket;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public class CamEventHandler {
	
	@SubscribeEvent
	public void onPlayerConnect(PlayerLoggedInEvent event) {
		CMDCam.NETWORK.sendToClient(new ConnectPacket(), event.getPlayer());
	}
	
}
