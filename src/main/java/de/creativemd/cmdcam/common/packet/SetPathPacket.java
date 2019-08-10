package de.creativemd.cmdcam.common.packet;

import de.creativemd.cmdcam.common.utils.CamPath;
import de.creativemd.cmdcam.server.CMDCamServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import team.creative.creativecore.common.network.CreativePacket;

public class SetPathPacket extends CreativePacket {
	
	public String id;
	public NBTTagCompound nbt;
	
	public SetPathPacket() {
		
	}
	
	public SetPathPacket(String id, CamPath path) {
		this.id = id;
		this.nbt = path.writeToNBT(new NBTTagCompound());
	}
	
	@Override
	public void executeClient(EntityPlayer player) {
		CamPath path = new CamPath(nbt);
		path.overwriteClientConfig();
		player.sendMessage(new TextComponentString("Loaded path '" + id + "' successfully!"));
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		CamPath path = new CamPath(nbt);
		if (((EntityPlayerMP) player).hasPermissionLevel(4)) {
			CMDCamServer.setPath(player.world, id, path);
			player.sendMessage(new TextComponentString("Saved path '" + id + "' successfully!"));
		} else
			player.sendMessage(new TextComponentString("You do not have the permission to edit the path list!"));
	}
}
