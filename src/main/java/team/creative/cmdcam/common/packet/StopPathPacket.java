package team.creative.cmdcam.common.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.creativecore.common.network.CreativePacket;

public class StopPathPacket extends CreativePacket {
    
    public StopPathPacket() {}
    
    @Override
    public void executeClient(Player player) {
        if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().serverPath)
            CMDCamClient.stopPathServer();
    }
    
    @Override
    public void executeServer(ServerPlayer player) {}
    
}
