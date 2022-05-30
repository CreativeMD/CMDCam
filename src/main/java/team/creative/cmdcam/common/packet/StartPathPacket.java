package team.creative.cmdcam.common.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public class StartPathPacket extends CreativePacket {
    
    public CompoundTag nbt;
    
    public StartPathPacket() {}
    
    public StartPathPacket(CamScene scene) {
        this.nbt = scene.save(new CompoundTag());
    }
    
    @Override
    public void executeClient(Player player) {
        try {
            CamScene path = new CamScene(nbt);
            path.setServerSynced();
            if (CMDCamClient.isPlaying())
                CMDCamClient.stop();
            CMDCamClient.start(path);
        } catch (RegistryException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void executeServer(ServerPlayer player) {}
    
}
