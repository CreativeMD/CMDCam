package team.creative.cmdcam.common.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.creativecore.common.network.CreativePacket;

public class StartPathPacket extends CreativePacket {
    
    public CompoundTag nbt;
    
    public StartPathPacket() {
        
    }
    
    public StartPathPacket(CamPath path) {
        this.nbt = path.writeToNBT(new CompoundTag());
    }
    
    @Override
    public void executeClient(Player player) {
        CamPath path = new CamPath(nbt);
        path.serverPath = true;
        if (CMDCamClient.getCurrentPath() != null)
            CMDCamClient.stopPath();
        
        try {
            CMDCamClient.startPath(path);
        } catch (PathParseException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void executeServer(ServerPlayer player) {
        
    }
    
}
