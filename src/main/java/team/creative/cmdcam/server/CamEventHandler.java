package team.creative.cmdcam.server;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

public class CamEventHandler {
    
    @SubscribeEvent
    public void onPlayerConnect(PlayerLoggedInEvent event) {
        CMDCam.NETWORK.sendToClient(new ConnectPacket(), (ServerPlayer) event.getEntity());
    }
    
}
