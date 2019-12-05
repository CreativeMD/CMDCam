package team.creative.cmdcam.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

public class CamEventHandler {
	
	@SubscribeEvent
	public void onPlayerConnect(PlayerLoggedInEvent event) {
		CMDCam.NETWORK.sendToClient(new ConnectPacket(), (ServerPlayerEntity) event.getPlayer());
	}
	
}
