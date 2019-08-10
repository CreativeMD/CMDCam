package team.creative.creativecore.common.network;

import net.minecraft.entity.player.EntityPlayer;

public abstract class CreativePacket {
	
	public CreativePacket() {
		
	}
	
	public void execute(EntityPlayer player) {
		if (player.world.isRemote)
			executeClient(player);
		else
			executeServer(player);
	}
	
	public abstract void executeClient(EntityPlayer player);
	
	public abstract void executeServer(EntityPlayer player);
	
}
