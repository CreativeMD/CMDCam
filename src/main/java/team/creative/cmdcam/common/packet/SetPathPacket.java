package team.creative.cmdcam.common.packet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativePacket;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public class SetPathPacket extends CreativePacket {
    
    public String id;
    
    public CompoundTag nbt;
    
    public SetPathPacket() {}
    
    public SetPathPacket(String id, CamScene scene) {
        this.id = id;
        this.nbt = scene.save(new CompoundTag());
    }
    
    @Override
    public void executeClient(Player player) {
        
        try {
            CamScene scene = new CamScene(nbt);
            CMDCamClient.set(scene);
            player.sendSystemMessage(Component.translatable("scene.load", id));
        } catch (RegistryException e) {
            e.printStackTrace();
        }
        
    }
    
    @Override
    public void executeServer(ServerPlayer player) {
        try {
            CamScene path = new CamScene(nbt);
            if (player.hasPermissions(4)) {
                CMDCamServer.set(player.level(), id, path);
                player.sendSystemMessage(Component.translatable("scene.save", id));
            } else
                player.sendSystemMessage(Component.translatable("scene.save_perm", id));
        } catch (RegistryException e) {
            e.printStackTrace();
            player.sendSystemMessage(Component.translatable("scenes.save_fail"));
        }
        
    }
}
