package team.creative.cmdcam.common.packet;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.creativecore.common.network.CreativePacket;

public class SetPathPacket extends CreativePacket {
    
    public String id;
    
    public CompoundTag nbt;
    
    public SetPathPacket() {
        
    }
    
    public SetPathPacket(String id, CamPath path) {
        this.id = id;
        this.nbt = path.writeToNBT(new CompoundTag());
    }
    
    @Override
    public void executeClient(Player player) {
        CamPath path = new CamPath(nbt);
        path.overwriteClientConfig();
        player.sendMessage(new TextComponent("Loaded path '" + id + "' successfully!"), Util.NIL_UUID);
    }
    
    @Override
    public void executeServer(ServerPlayer player) {
        CamPath path = new CamPath(nbt);
        if (player.hasPermissions(4)) {
            CMDCamServer.setPath(player.level, id, path);
            player.sendMessage(new TextComponent("Saved path '" + id + "' successfully!"), Util.NIL_UUID);
        } else
            player.sendMessage(new TextComponent("You do not have the permission to edit the path list!"), Util.NIL_UUID);
    }
}
