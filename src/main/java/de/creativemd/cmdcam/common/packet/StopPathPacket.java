package de.creativemd.cmdcam.common.packet;

import de.creativemd.cmdcam.client.CMDCamClient;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.creativecore.common.network.CreativePacket;

public class StopPathPacket extends CreativePacket {
	
	public StopPathPacket() {
		
	}
	
	@Override
	public void executeClient(PlayerEntity player) {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().serverPath)
			CMDCamClient.stopPath();
	}
	
	@Override
	public void executeServer(PlayerEntity player) {
		
	}
	
}
