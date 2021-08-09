package com.creativemd.cmdcam.server;

import com.creativemd.cmdcam.common.packet.ConnectPacket;
import com.creativemd.creativecore.common.packet.PacketHandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;

public class CamEventHandler {
    
    @SubscribeEvent
    public void onPlayerConnect(PlayerLoggedInEvent event) {
        PacketHandler.sendPacketToPlayer(new ConnectPacket(), (EntityPlayerMP) event.player);
    }
    
}
