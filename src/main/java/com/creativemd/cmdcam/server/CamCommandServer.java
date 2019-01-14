package com.creativemd.cmdcam.server;

import java.util.Collection;
import java.util.List;

import com.creativemd.cmdcam.common.packet.StartPathPacket;
import com.creativemd.cmdcam.common.packet.StopPathPacket;
import com.creativemd.cmdcam.common.utils.CamPath;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.creativecore.common.utils.mc.ChatFormatting;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CamCommandServer extends CommandBase {
	
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
			sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
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
							path = path.copy();
							long duration = CamCommandServer.StringToDuration(args[1]);
							if (duration > 0)
								path.duration = duration;
							else {
								sender.sendMessage(new TextComponentString("Invalid time '" + args[1] + "'!"));
								return;
							}
							if (args.length >= 5)
								path.currentLoop = Integer.parseInt(args[2]);
						}
						
						PacketHandler.sendPacketToPlayers(new StartPathPacket(path), players);
					} else
						sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
					
				} else
					sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
			}
			if (subCommand.equals("stop")) {
				if (args.length >= 2)
					PacketHandler.sendPacketToPlayers(new StopPathPacket(), getPlayers(server, sender, args[1]));
				else
					sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server stop <player> " + ChatFormatting.RED + "stops the animation"));
			}
			if (subCommand.equals("list")) {
				Collection<String> names = CMDCamServer.getSavedPaths(sender.getEntityWorld());
				String output = "There are " + names.size() + " path(s) in total. ";
				for (String key : names) {
					output += key + ", ";
				}
				sender.sendMessage(new TextComponentString(output));
			}
			if (subCommand.equals("remove")) {
				if (args.length >= 2) {
					if (CMDCamServer.removePath(sender.getEntityWorld(), args[1]))
						sender.sendMessage(new TextComponentString("Path '" + args[2] + "' has been removed!"));
					else
						sender.sendMessage(new TextComponentString("Path '" + args[2] + "' could not be found!"));
				} else
					sender.sendMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server remove <name> " + ChatFormatting.RED + "lists all saved paths"));
			}
			if (subCommand.equals("clear")) {
				CMDCamServer.clearPaths(sender.getEntityWorld());
				sender.sendMessage(new TextComponentString("Removed all existing paths (in this world)!"));
			}
		}
	}
	
}
