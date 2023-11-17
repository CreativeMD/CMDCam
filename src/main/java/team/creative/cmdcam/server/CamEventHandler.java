package team.creative.cmdcam.server;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.packet.ConnectPacket;

public class CamEventHandler {

    public CamEventHandler() {
        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            CMDCam.NETWORK.sendToClient(new ConnectPacket(), handler.getPlayer());
        }));
    }
    
}
