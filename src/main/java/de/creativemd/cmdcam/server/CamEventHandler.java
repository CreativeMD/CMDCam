package de.creativemd.cmdcam.server;

import de.creativemd.cmdcam.CMDCam;
import de.creativemd.cmdcam.common.packet.ConnectPacket;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CamEventHandler {
	
	@SubscribeEvent
	public void onPlayerConnect(PlayerLoggedInEvent event) {
		CMDCam.NETWORK.sendToClient(new ConnectPacket(), event.getPlayer());
	}
	
}
