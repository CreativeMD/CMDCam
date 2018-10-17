package com.creativemd.cmdcam.common.packet;

import com.creativemd.cmdcam.client.CMDCamClient;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

public class ConnectPacket extends CreativeCorePacket {
	
	public ConnectPacket() {
		
	}
	
	@Override
	public void writeBytes(ByteBuf buf) {
		
	}
	
	@Override
	public void readBytes(ByteBuf buf) {
		
	}
	
	@Override
	public void executeClient(EntityPlayer player) {
		CMDCamClient.isInstalledOnSever = true;
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		
	}
}
