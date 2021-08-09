package com.creativemd.cmdcam.common.packet;

import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.server.CMDCamServer;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.creativecore.common.packet.PacketHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public class GetPathPacket extends CreativeCorePacket {
    
    public String id;
    
    public GetPathPacket() {
        
    }
    
    public GetPathPacket(String id) {
        this.id = id;
    }
    
    @Override
    public void writeBytes(ByteBuf buf) {
        writeString(buf, id);
    }
    
    @Override
    public void readBytes(ByteBuf buf) {
        id = readString(buf);
    }
    
    @Override
    public void executeClient(EntityPlayer player) {
        
    }
    
    @Override
    public void executeServer(EntityPlayer player) {
        CamPath path = CMDCamServer.getPath(player.getEntityWorld(), id);
        if (path != null)
            PacketHandler.sendPacketToPlayer(new SetPathPacket(id, path), (EntityPlayerMP) player);
        else
            player.sendMessage(new TextComponentString("Path '" + id + "' could not be found!"));
    }
    
}
