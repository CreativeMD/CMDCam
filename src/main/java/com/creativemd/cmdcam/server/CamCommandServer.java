package com.creativemd.cmdcam.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.creativemd.cmdcam.client.interpolation.CamInterpolation;
import com.creativemd.cmdcam.client.mode.CamMode;
import com.creativemd.cmdcam.common.packet.SelectTargetPacket;
import com.creativemd.cmdcam.common.packet.StartPathPacket;
import com.creativemd.cmdcam.common.packet.StopPathPacket;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.cmdcam.common.utils.CamPoint;
import com.creativemd.cmdcam.common.utils.CamTarget;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.mc.ChatFormatting;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

public class CamCommandServer extends CommandBase {
    
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
    
    @Override
    public String getName() {
        return "cam-server";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "used to control the camera (server side)";
    }
    
    public static long StringToDuration(String input) {
        String replacement = null;
        int factor = 0;
        if (input.endsWith("ms")) {
            replacement = "ms";
            factor = 1;
        } else if (input.endsWith("s")) {
            replacement = "s";
            factor = 1000;
        } else if (input.endsWith("m")) {
            replacement = "m";
            factor = 1000 * 60;
        } else if (input.endsWith("h")) {
            replacement = "h";
            factor = 1000 * 60 * 60;
        } else if (input.endsWith("d")) {
            replacement = "d";
            factor = 1000 * 60 * 60 * 24;
        }
        
        try {
            if (replacement == null) {
                replacement = "";
                factor = 1000;
            }
            return Long.parseLong(input.replaceAll(replacement, "")) * factor;
        } catch (Exception e) {
            
        }
        return -1;
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [" + String
                .join(":", CamMode.modes.keySet()) + "] [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server interpolation <path> <" + String
                .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
            sender
                .sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server target <path> <none:self:pos:entity> " + ChatFormatting.RED + "set the camera target"));
            sender
                .sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server add <path> <x> <y> <z> [yaw] [pitch] [roll] [zoom] " + ChatFormatting.RED + "adds a new point to path or creates a new one"));
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server stop <player> " + ChatFormatting.RED + "stops the animation"));
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server list " + ChatFormatting.RED + "lists all saved paths"));
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server remove <name> " + ChatFormatting.RED + "removes the given path"));
            sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server clear " + ChatFormatting.RED + "clears all saved paths"));
        } else {
            String subCommand = args[0];
            if (subCommand.equals("start")) {
                if (args.length >= 3) {
                    List<EntityPlayerMP> players = getPlayers(server, sender, args[1]);
                    CamPath path = CMDCamServer.getPath(sender.getEntityWorld(), args[2]);
                    if (path != null) {
                        if (args.length >= 4) {
                            path.mode = args[3];
                            if (args.length >= 5) {
                                path = path.copy();
                                long duration = CamCommandServer.StringToDuration(args[4]);
                                if (duration > 0)
                                    path.duration = duration;
                                else {
                                    sender.sendMessage(new TextComponentString("Invalid time '" + args[4] + "'!"));
                                    return;
                                }
                                if (args.length >= 6)
                                    path.currentLoop = Integer.parseInt(args[5]);
                            }
                        }
                        
                        PacketHandler.sendPacketToPlayers(new StartPathPacket(path), players);
                    } else
                        sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
                    
                } else
                    sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [" + String
                        .join(":", CamMode.modes.keySet()) + "] [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
            } else if (subCommand.equals("stop")) {
                if (args.length >= 2)
                    PacketHandler.sendPacketToPlayers(new StopPathPacket(), getPlayers(server, sender, args[1]));
                else
                    sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server stop <player> " + ChatFormatting.RED + "stops the animation"));
            } else if (subCommand.equals("list")) {
                Collection<String> names = CMDCamServer.getSavedPaths(sender.getEntityWorld());
                String output = "There are " + names.size() + " path(s) in total. ";
                for (String key : names) {
                    output += key + ", ";
                }
                sender.sendMessage(new TextComponentString(output));
            } else if (subCommand.equals("remove")) {
                if (args.length >= 2) {
                    if (CMDCamServer.removePath(sender.getEntityWorld(), args[1]))
                        sender.sendMessage(new TextComponentString("Path '" + args[2] + "' has been removed!"));
                    else
                        sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
                } else
                    sender
                        .sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server remove <name> " + ChatFormatting.RED + "lists all saved paths"));
            } else if (subCommand.equals("interpolation")) {
                if (args.length >= 3) {
                    CamPath path = CMDCamServer.getPath(sender.getEntityWorld(), args[1]);
                    if (path != null) {
                        String interpolation = args[2];
                        CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
                        if (move != null) {
                            path.interpolation = interpolation;
                            path.cachedInterpolation = move;
                            CMDCamServer.setPath(sender.getEntityWorld(), args[1], path);
                            sender.sendMessage(new TextComponentString("Interpolation has been set to '" + interpolation + "'!"));
                        } else
                            sender.sendMessage(new TextComponentString("Interpolation '" + interpolation + "' not found!"));
                    } else
                        sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
                } else
                    sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server interpolation <path> <" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
            } else if (subCommand.equals("target")) {
                if (args.length >= 2) {
                    CamPath path = CMDCamServer.getPath(sender.getEntityWorld(), args[1]);
                    if (path != null) {
                        if (args.length >= 3) {
                            String target = args[2];
                            if (target.equals("self")) {
                                path.target = new CamTarget.SelfTarget();
                                sender.sendMessage(new TextComponentString("The camera will point towards you!"));
                            } else if (target.equals("none")) {
                                path.target = null;
                                sender.sendMessage(new TextComponentString("Removed target!"));
                            } else if (target.equals("pos")) {
                                if (args.length >= 6) {
                                    Vec3d vec3d = sender.getPositionVector();
                                    int j = 3;
                                    CommandBase.CoordinateArg x = parseCoordinate(vec3d.x, args[j++], true);
                                    CommandBase.CoordinateArg y = parseCoordinate(vec3d.y, args[j++], -4096, 4096, false);
                                    CommandBase.CoordinateArg z = parseCoordinate(vec3d.z, args[j++], true);
                                    
                                    path.target = new CamTarget.VecTarget(new Vec3d(x.getResult(), y.getResult(), z.getResult()));
                                    sender.sendMessage(new TextComponentString("Camera will point towards " + x.getResult() + ", " + y.getResult() + ", " + z.getResult()));
                                } else
                                    sender.sendMessage(new TextComponentString("Invalid position"));
                            } else if (target.equals("entity")) {
                                if (args.length >= 4) {
                                    try {
                                        UUID uuid = UUID.fromString(args[3]);
                                        path.target = new CamTarget.EntityTarget(uuid.toString());
                                        sender.sendMessage(new TextComponentString("Camera will point towards " + args[3]));
                                    } catch (IllegalArgumentException e) {
                                        sender.sendMessage(new TextComponentString("Invalid uuid"));
                                    }
                                } else
                                    sender.sendMessage(new TextComponentString("Missing uuid"));
                            } else
                                sender.sendMessage(new TextComponentString("Target '" + target + "' not found!"));
                            CMDCamServer.setPath(sender.getEntityWorld(), args[1], path);
                        } else if (sender instanceof EntityPlayerMP)
                            PacketHandler.sendPacketToPlayer(new SelectTargetPacket(subCommand, null), (EntityPlayerMP) sender);
                    } else
                        sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
                } else
                    sender
                        .sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server target <path> <none:self:pos:entity> " + ChatFormatting.RED + "set the camera target"));
            } else if (subCommand.equals("add")) {
                if (args.length >= 5) {
                    CamPath path = CMDCamServer.getPath(sender.getEntityWorld(), args[1]);
                    
                    Vec3d vec3d = sender.getPositionVector();
                    int j = 1;
                    CommandBase.CoordinateArg x = parseCoordinate(vec3d.x, args[j++], true);
                    CommandBase.CoordinateArg y = parseCoordinate(vec3d.y, args[j++], -4096, 4096, false);
                    CommandBase.CoordinateArg z = parseCoordinate(vec3d.z, args[j++], true);
                    CommandBase.CoordinateArg yaw = parseCoordinate(0, args.length > j ? args[j] : "~", false);
                    ++j;
                    CommandBase.CoordinateArg pitch = parseCoordinate(0, args.length > j ? args[j] : "~", false);
                    ++j;
                    CommandBase.CoordinateArg roll = parseCoordinate(0, args.length > j ? args[j] : "~", false);
                    ++j;
                    CommandBase.CoordinateArg zoom = parseCoordinate(75, args.length > j ? args[j] : "~", false);
                    if (path == null) {
                        path = new CamPath(0, 10000, "default", "hermite", null, new ArrayList<>(), 1);
                        sender.sendMessage(new TextComponentString("New path was created successfully"));
                    } else
                        sender.sendMessage(new TextComponentString("New point was added successfully"));
                    path.points.add(new CamPoint(x.getResult(), y.getResult(), z.getResult(), yaw.getResult(), pitch.getResult(), roll.getResult(), zoom.getResult()));
                    
                    CMDCamServer.setPath(sender.getEntityWorld(), args[1], path);
                } else
                    sender
                        .sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server add <path> <x> <y> <z> [yaw] [pitch] [roll] [zoom] " + ChatFormatting.RED + "adds a new point to path or creates a new one"));
            } else if (subCommand.equals("clear")) {
                CMDCamServer.clearPaths(sender.getEntityWorld());
                sender.sendMessage(new TextComponentString("Removed all existing paths (in this world)!"));
            }
        }
    }
    
}
