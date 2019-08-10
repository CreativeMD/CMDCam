package de.creativemd.cmdcam.common.packet;

import de.creativemd.cmdcam.client.CMDCamClient;
import de.creativemd.cmdcam.client.PathParseException;
import de.creativemd.cmdcam.common.utils.CamPath;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import team.creative.creativecore.common.network.CreativePacket;

public class StartPathPacket extends CreativePacket {
	
	public NBTTagCompound nbt;
	
	public StartPathPacket() {
		
	}
	
	public StartPathPacket(CamPath path) {
		this.nbt = path.writeToNBT(new NBTTagCompound());
	}
	
	@Override
	public void executeClient(EntityPlayer player) {
		CamPath path = new CamPath(nbt);
		path.serverPath = true;
		if (CMDCamClient.getCurrentPath() != null)
			CMDCamClient.stopPath();
		
		try {
			CMDCamClient.startPath(path);
		} catch (PathParseException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		
	}
	
}
