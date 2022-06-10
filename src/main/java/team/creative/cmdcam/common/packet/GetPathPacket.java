package team.creative.cmdcam.common.packet;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativePacket;

public class GetPathPacket extends CreativePacket {
    
    public String id;
    
    public GetPathPacket() {}
    
    public GetPathPacket(String id) {
        this.id = id;
    }
    
    @Override
    public void executeClient(Player player) {}
    
    @Override
    public void executeServer(ServerPlayer player) {
        CamScene path = CMDCamServer.get(player.level, id);
        if (path != null)
            CMDCam.NETWORK.sendToClient(new SetPathPacket(id, path), player);
        else
            player.sendSystemMessage(Component.translatable("scene.load_fail", id));
    }
    
}
