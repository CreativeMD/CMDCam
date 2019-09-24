package de.creativemd.cmdcam.common.packet;

import de.creativemd.cmdcam.client.CMDCamClient;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.creativecore.common.network.CreativePacket;

public class ConnectPacket extends CreativePacket {
	
	public ConnectPacket() {
		
	}
	
	@Override
	public void executeClient(PlayerEntity player) {
		CMDCamClient.isInstalledOnSever = true;
	}
	
	@Override
	public void executeServer(PlayerEntity player) {
		
	}
}
