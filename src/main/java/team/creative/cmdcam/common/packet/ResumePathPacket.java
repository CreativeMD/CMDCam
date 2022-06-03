package team.creative.cmdcam.common.packet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.creativecore.common.network.CreativePacket;

public class ResumePathPacket extends CreativePacket {
    
    public ResumePathPacket() {}
    
    @Override
    public void executeClient(Player player) {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().serverSynced())
            CMDCamClient.getScene().resume();
    }
    
    @Override
    public void executeServer(ServerPlayer player) {}
    
}
