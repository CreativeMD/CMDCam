package com.creativemd.cmdcam.common.packet;

import com.creativemd.cmdcam.client.CMDCamClient;
import com.creativemd.cmdcam.client.PathParseException;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class StartPathPacket extends CreativeCorePacket {
    
    public CamPath path;
    
    public StartPathPacket() {
        
    }
    
    public StartPathPacket(CamPath path) {
        this.path = path;
    }
    
    @Override
    public void writeBytes(ByteBuf buf) {
        writeNBT(buf, path.writeToNBT(new NBTTagCompound()));
    }
    
    @Override
    public void readBytes(ByteBuf buf) {
        path = new CamPath(readNBT(buf));
    }
    
    @Override
    public void executeClient(EntityPlayer player) {
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
    public void executeServer(EntityPlayer player) {
        
    }
    
}
