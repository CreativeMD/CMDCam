package team.creative.cmdcam.common.packet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativePacket;

public class SetPathPacket extends CreativePacket {
	
	public String id;
	public CompoundNBT nbt;
	
	public SetPathPacket() {
		
	}
	
	public SetPathPacket(String id, CamPath path) {
		this.id = id;
		this.nbt = path.writeToNBT(new CompoundNBT());
	}
	
	@Override
	public void executeClient(PlayerEntity player) {
		CamPath path = new CamPath(nbt);
		path.overwriteClientConfig();
		player.sendMessage(new StringTextComponent("Loaded path '" + id + "' successfully!"));
	}
	
	@Override
	public void executeServer(PlayerEntity player) {
		CamPath path = new CamPath(nbt);
		if (((ServerPlayerEntity) player).hasPermissionLevel(4)) {
			CMDCamServer.setPath(player.world, id, path);
			player.sendMessage(new StringTextComponent("Saved path '" + id + "' successfully!"));
		} else
			player.sendMessage(new StringTextComponent("You do not have the permission to edit the path list!"));
	}
}
