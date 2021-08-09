package com.creativemd.cmdcam.common.packet;

import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.server.CMDCamServer;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;

public class SetPathPacket extends CreativeCorePacket {
    
    public String id;
    public CamPath path;
    
    public SetPathPacket() {
        
    }
    
    public SetPathPacket(String id, CamPath path) {
        this.id = id;
        this.path = path;
    }
    
    @Override
    public void writeBytes(ByteBuf buf) {
        writeString(buf, id);
        writeNBT(buf, path.writeToNBT(new NBTTagCompound()));
    }
    
    @Override
    public void readBytes(ByteBuf buf) {
        id = readString(buf);
        path = new CamPath(readNBT(buf));
    }
    
    @Override
    public void executeClient(EntityPlayer player) {
        path.overwriteClientConfig();
        player.sendMessage(new TextComponentString("Loaded path '" + id + "' successfully!"));
    }
    
    @Override
    public void executeServer(EntityPlayer player) {
        if (player.canUseCommand(4, "cam-server")) {
            CMDCamServer.setPath(player.world, id, path);
            player.sendMessage(new TextComponentString("Saved path '" + id + "' successfully!"));
        } else
            player.sendMessage(new TextComponentString("You do not have the permission to edit the path list!"));
    }
}
