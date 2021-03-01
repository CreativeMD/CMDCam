package team.creative.cmdcam.common.packet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.creativecore.common.network.CreativePacket;

public class StartPathPacket extends CreativePacket {
    
    public CompoundNBT nbt;
    
    public StartPathPacket() {
        
    }
    
    public StartPathPacket(CamPath path) {
        this.nbt = path.writeToNBT(new CompoundNBT());
    }
    
    @Override
    public void executeClient(PlayerEntity player) {
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
    public void executeServer(PlayerEntity player) {
        
    }
    
}
