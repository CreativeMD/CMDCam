package team.creative.cmdcam.server;

import java.util.Collection;
import java.util.Collections;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.StartPathPacket;
import team.creative.cmdcam.common.packet.StopPathPacket;
import team.creative.cmdcam.common.packet.TeleportPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.common.network.CreativePacket;

public class CamCommandProcessorServer implements CamCommandProcessor {
    
    @Override
    public CamScene getScene(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CamScene scene = CMDCamServer.get(context.getSource().getLevel(), name);
        return scene;
    }
    
    @Override
    public boolean canSelectTarget() {
        return false;
    }
    
    @Override
    public void selectTarget() {}
    
    @Override
    public boolean canCreatePoint(CommandContext<CommandSourceStack> context) {
        return context.getSource().getEntity() != null;
    }
    
    @Override
    public CamPoint createPoint(CommandContext<CommandSourceStack> context) {
        return CamPoint.create(context.getSource().getEntity());
    }
    
    @Override
    public boolean requiresSceneName() {
        return true;
    }
    
    @Override
    public boolean requiresPlayer() {
        return true;
    }
    
    public Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> context) {
        try {
            return EntityArgument.getPlayers(context, "players");
        } catch (CommandSyntaxException e) {
            return Collections.EMPTY_LIST;
        }
    }
    
    @Override
    public void start(CommandContext<CommandSourceStack> context) throws PathParseException {
        CreativePacket packet = new StartPathPacket(getScene(context));
        for (ServerPlayer player : getPlayers(context))
            CMDCam.NETWORK.sendToClient(packet, player);
    }
    
    @Override
    public void teleport(CommandContext<CommandSourceStack> context, int index) {
        CreativePacket packet = new TeleportPathPacket(getScene(context).points.get(index));
        for (ServerPlayer player : getPlayers(context))
            CMDCam.NETWORK.sendToClient(packet, player);
    }
    
    @Override
    public void stop(CommandContext<CommandSourceStack> context) {
        CreativePacket packet = new StopPathPacket();
        for (ServerPlayer player : getPlayers(context))
            CMDCam.NETWORK.sendToClient(packet, player);
    }
    
}
