package team.creative.cmdcam.common.packet;

import net.minecraft.entity.player.PlayerEntity;
import team.creative.cmdcam.client.CMDCamClient;
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
