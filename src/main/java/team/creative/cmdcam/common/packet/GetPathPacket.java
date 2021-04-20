package team.creative.cmdcam.common.packet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativePacket;

public class GetPathPacket extends CreativePacket {
    
    public String id;
    
    public GetPathPacket() {
        
    }
    
    public GetPathPacket(String id) {
        this.id = id;
    }
    
    @Override
    public void executeClient(PlayerEntity player) {
        
    }
    
    @Override
    public void executeServer(PlayerEntity player) {
        CamPath path = CMDCamServer.getPath(player.level, id);
        if (path != null)
            CMDCam.NETWORK.sendToClient(new SetPathPacket(id, path), (ServerPlayerEntity) player);
        else
            player.sendMessage(new StringTextComponent("Path '" + id + "' could not be found!"), Util.NIL_UUID);
    }
    
}
