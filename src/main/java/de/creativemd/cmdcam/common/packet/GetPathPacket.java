package de.creativemd.cmdcam.common.packet;

import de.creativemd.cmdcam.CMDCam;
import de.creativemd.cmdcam.common.utils.CamPath;
import de.creativemd.cmdcam.server.CMDCamServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import team.creative.creativecore.common.network.CreativePacket;

public class GetPathPacket extends CreativePacket {
	
	public String id;
	
	public GetPathPacket() {
		
	}
	
	public GetPathPacket(String id) {
		this.id = id;
	}
	
	@Override
	public void executeClient(EntityPlayer player) {
		
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		CamPath path = CMDCamServer.getPath(player.getEntityWorld(), id);
		if (path != null)
			CMDCam.NETWORK.sendToClient(new SetPathPacket(id, path), (EntityPlayerMP) player);
		else
			player.sendMessage(new TextComponentString("Path '" + id + "' could not be found!"));
	}
	
}
