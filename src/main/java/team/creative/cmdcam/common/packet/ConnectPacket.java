package team.creative.cmdcam.common.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.creativecore.common.network.CreativePacket;

public class ConnectPacket extends CreativePacket {
    
    public ConnectPacket() {}
    
    @Override
    public void executeClient(Player player) {
        CMDCamClient.setServerAvailability();
    }
    
    @Override
    public void executeServer(ServerPlayer player) {}
}
