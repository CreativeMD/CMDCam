package com.creativemd.cmdcam.common.packet;

import com.creativemd.cmdcam.client.CamEventHandlerClient;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.cmdcam.server.CMDCamServer;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;

public class SelectTargetPacket extends CreativeCorePacket {
    
    public String path;
    public CamTarget target;
    
    public SelectTargetPacket(String path, CamTarget target) {
        this.path = path;
        this.target = target;
    }
    
    public SelectTargetPacket() {
        
    }
    
    @Override
    public void writeBytes(ByteBuf buf) {
        writeString(buf, path);
        if (target != null) {
            buf.writeBoolean(true);
            writeNBT(buf, target.writeToNBT(new NBTTagCompound()));
        }
    }
    
    @Override
    public void readBytes(ByteBuf buf) {
        path = readString(buf);
        if (buf.readBoolean()) {
            target = CamTarget.readFromNBT(readNBT(buf));
        } else
            target = null;
    }
    
    @Override
    public void executeClient(EntityPlayer player) {
        CamEventHandlerClient.startSelectingTarget(path);
    }
    
    @Override
    public void executeServer(EntityPlayer player) {
        if (player.canUseCommand(4, "cam-server")) {
            CamPath loaded = CMDCamServer.getPath(player.world, path);
            if (loaded != null) {
                loaded.target = target;
                CMDCamServer.setPath(player.world, path, loaded);
                player.sendMessage(new TextComponentString("Set target for path '" + path + "' successfully!"));
            } else
                player.sendMessage(new TextComponentString("Path could not be found"));
        } else
            player.sendMessage(new TextComponentString("You do not have the permission for that"));
    }
    
}
