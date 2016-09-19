package com.creativemd.cmdcam.command;

import com.creativemd.cmdcam.CMDCam;
import com.creativemd.cmdcam.CamEventHandler;
import com.creativemd.cmdcam.movement.Movement;
import com.creativemd.cmdcam.utils.CamPoint;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public class CamCommand extends CommandBase{

	@Override
	public String getCommandName() {
		return "cam";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "used to control the camera";
	}
	
	public static long StringToDuration(String input)
	{
		String replacement = null;
		int factor = 0;
		if(input.endsWith("ms"))
		{
			replacement = "ms";
			factor = 1;
		}
		else if(input.endsWith("s"))
		{
			replacement = "s";
			factor = 1000;
		}
		else if(input.endsWith("m"))
		{
			replacement = "m";
			factor = 1000*60;
		}
		else if(input.endsWith("h"))
		{
			replacement = "h";
			factor = 1000*60*60;
		}
		else if(input.endsWith("d"))
		{
			replacement = "d";
			factor = 1000*60*60*24;
		}
		
		try{
			if(replacement == null)
			{
				replacement = "";
				factor = 1000;
			}
			return Long.parseLong(input.replaceAll(replacement, "")) * factor;
		}catch(Exception e){
			
		}
		return -1;
	}
	
	public static Minecraft mc = Minecraft.getMinecraft();

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length == 0)
		{
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start <time|ms|s|m|h|d> " + ChatFormatting.RED + "starts the animation"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String.join(":", Movement.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
			sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
		}else{
			String subCommand = args[0];
			if(subCommand.equals("clear"))
			{
				sender.addChatMessage(new TextComponentString("Cleared all registered points!"));
				CMDCam.points.clear();
			}
			if(subCommand.equals("start"))
			{
				if(args.length >= 2)
				{
					long duration = StringToDuration(args[1]);
					if(duration > 0)
						CMDCam.lastDuration = duration;
					else
					{
						sender.addChatMessage(new TextComponentString("Invalid time '" + args[1] + "'!"));
						return ;
					}
				}
				CMDCam.createPath();
			}
			if(subCommand.equals("remove"))
			{
				if(args.length >= 2)
				{
					try{
						Integer index = Integer.parseInt(args[1])-1;
						if(index >= 0 && index < CMDCam.points.size())
						{
							CMDCam.points.remove(index);
							sender.addChatMessage(new TextComponentString("Removed " + (index+1) + ". point!"));
						}else
							sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
					}catch(Exception e){
						sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
					}
				}else
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
			}
			if(subCommand.equals("set"))
			{
				if(args.length >= 2)
				{
					try{
						Integer index = Integer.parseInt(args[1])-1;
						if(index >= 0 && index < CMDCam.points.size())
						{
							CMDCam.points.set(index, new CamPoint());
							sender.addChatMessage(new TextComponentString("Updated " + (index+1) + ". point!"));
						}else
							sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
					}catch(Exception e){
						sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
					}
				}else
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates the giveng point to the current location"));
			}
			if(subCommand.equals("goto"))
			{
				if(args.length >= 2)
				{
					try{
						Integer index = Integer.parseInt(args[1])-1;
						if(index >= 0 && index < CMDCam.points.size())
						{
							CamPoint point = CMDCam.points.get(index);
							mc.thePlayer.capabilities.isFlying = true;
							
							CMDCam.roll = (float) point.roll;
							mc.gameSettings.fovSetting = (float) point.zoom;
							mc.thePlayer.setPositionAndRotation(point.x, point.y, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
							mc.thePlayer.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float)point.rotationYaw, (float)point.rotationPitch);
						}else
							sender.addChatMessage(new TextComponentString("The given index '" + args[1] + "' is too high/low!"));
					}catch(Exception e){
						sender.addChatMessage(new TextComponentString("Invalid index '" + args[1] + "'!"));
					}
				}else{
					sender.addChatMessage(new TextComponentString("Missing point!"));
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
				}
			}
			if(subCommand.equals("mode"))
			{
				if(args.length >= 2)
				{
					if(args[1].equals("default") || args[1].equals("outside"))
					{
						CMDCam.lastPath = args[1];
						sender.addChatMessage(new TextComponentString("Changed to " + args[1] + " path!"));
					}else
						sender.addChatMessage(new TextComponentString("Path mode '" + args[1] + "' does not exit!"));
				}else
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
			}
			if(subCommand.equals("target"))
			{
				if(args.length == 2)
				{
					String target = args[1];
					if(target.equals("self"))
					{
						CMDCam.target = "self";
						sender.addChatMessage(new TextComponentString("The camera will point towards you!"));
					}else if(target.equals("none"))
					{
						CMDCam.target = null;
						sender.addChatMessage(new TextComponentString("Removed target!"));
					}else
						sender.addChatMessage(new TextComponentString("Target '" + target + "' not found!"));
				}else{
					CamEventHandler.selectEntityMode = true;
					sender.addChatMessage(new TextComponentString("Please select a target either a entity or a block!"));
				}
				
			}
			if(subCommand.equals("interpolation"))
			{
				if(args.length == 2)
				{
					String target = args[1];
					Movement move = Movement.getMovementById(target);
					if(move != null)
					{
						CMDCam.lastMovement = target;
						sender.addChatMessage(new TextComponentString("Interpolation is set to '" + target + "'!"));
					}else
						sender.addChatMessage(new TextComponentString("Interpolation '" + target + "' not found!"));
				}else
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String.join(":", Movement.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
			}
			if(subCommand.equals("follow-speed"))
			{
				if(args.length == 2)
				{
					try{
						double followspeed = Double.parseDouble(args[1]);
						CMDCam.cameraFollowSpeed = followspeed;
						sender.addChatMessage(new TextComponentString("Camera follow speed is set to  '" + followspeed + "'. Default is 1.0!"));
					}catch(NumberFormatException e){
						sender.addChatMessage(new TextComponentString("'" + args[1] + "' is an invalid number!"));
					}
				}else
					sender.addChatMessage(new TextComponentString("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
			}
		}
	}
}
