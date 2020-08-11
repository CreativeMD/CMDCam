package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.client.interpolation.CamInterpolation;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.TargetArgument;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.common.util.CamPoint;
import team.creative.cmdcam.common.util.CamTarget;
import team.creative.creativecore.client.command.ClientCommandRegistry;

public class CMDCamClient {
	
	//private static final Field argumentField = ObfuscationReflectionHelper.findField(CommandContext.class, "arguments");
	
	public static Minecraft mc;
	
	public static int lastLoop = 0;
	
	public static long lastDuration = 10000;
	public static String lastMode = "default";
	public static String lastInterpolation = "hermite";
	public static CamTarget target = null;
	public static ArrayList<CamPoint> points = new ArrayList<>();
	
	public static double cameraFollowSpeed = 1D;
	
	public static HashMap<String, CamPath> savedPaths = new HashMap<>();
	
	public static boolean isInstalledOnSever = false;
	
	private static CamPath currentPath;
	
	public static void init(FMLClientSetupEvent event) {
		MinecraftForge.EVENT_BUS.register(new CamEventHandlerClient());
		mc = event.getMinecraftSupplier().get();
		KeyHandler.initKeys();
		
		ClientCommandRegistry.register(LiteralArgumentBuilder.<ISuggestionProvider>literal("cam").executes((x) -> {
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam add [number] " + TextFormatting.RED + "register a point at the current position"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam clear " + TextFormatting.RED + "delete all registered points"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + TextFormatting.RED + "starts the animation"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam stop " + TextFormatting.RED + "stops the animation"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam goto <index> " + TextFormatting.RED + "tp to the given point"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam set <index> " + TextFormatting.RED + "updates point to current location"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam remove <index> " + TextFormatting.RED + "removes the given point"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam target <none:self> " + TextFormatting.RED + "set the camera target"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam mode <default:outside> " + TextFormatting.RED + "set current mode"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam interpolation <" + String.join(":", CamInterpolation.getMovementNames()) + "> " + TextFormatting.RED + "set the camera interpolation"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam follow-speed <number> " + TextFormatting.RED + "default is 1.0"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam show <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + TextFormatting.RED + "shows the path using the given interpolation"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam hide <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + TextFormatting.RED + "hides the path using the given interpolation"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam save <name> " + TextFormatting.RED + "saves the current path (including settings) with the given name"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam load <name> " + TextFormatting.RED + "tries to load the saved path with the given name"), Util.field_240973_b_);
			mc.player.sendMessage(new StringTextComponent("" + TextFormatting.BOLD + TextFormatting.YELLOW + "/cam list " + TextFormatting.RED + "lists all saved paths"), Util.field_240973_b_);
			return 0;
		}).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("clear").executes((x) -> { // cam clear
			mc.player.sendMessage(new StringTextComponent("Cleared all registered points!"), Util.field_240973_b_);
			CMDCamClient.points.clear();
			return 0;
		})).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("add").executes((x) -> { // cam add
			CMDCamClient.points.add(new CamPoint());
			mc.player.sendMessage(new StringTextComponent("Registered " + CMDCamClient.points.size() + ". Point!"), Util.field_240973_b_);
			return 0;
		}).then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> { // cam add <index>
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CMDCamClient.points.add(index, new CamPoint());
				mc.player.sendMessage(new StringTextComponent("Inserted " + index + ". Point!"), Util.field_240973_b_);
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"), Util.field_240973_b_);
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("start").executes((x) -> { // cam start
			try {
				CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
			} catch (PathParseException e) {
				mc.player.sendMessage(new StringTextComponent(e.getMessage()), Util.field_240973_b_);
			}
			return 0;
		}).then(RequiredArgumentBuilder.<ISuggestionProvider, Long>argument("duration", DurationArgument.duration()).executes((x) -> {
			try {
				long duration = DurationArgument.getDuration(x, "duration");
				if (duration > 0)
					CMDCamClient.lastDuration = duration;
				CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
			} catch (PathParseException e) {
				mc.player.sendMessage(new StringTextComponent(e.getMessage()), Util.field_240973_b_);
			}
			return 0;
		}).then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
			try {
				long duration = DurationArgument.getDuration(x, "duration");
				if (duration > 0)
					CMDCamClient.lastDuration = duration;
				
				CMDCamClient.lastLoop = IntegerArgumentType.getInteger(x, "loop");
				CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
			} catch (PathParseException e) {
				mc.player.sendMessage(new StringTextComponent(e.getMessage()), Util.field_240973_b_);
			}
			return 0;
		})))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("stop").executes((x) -> {
			CMDCamClient.stopPath();
			return 0;
		})).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("remove").then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CMDCamClient.points.remove((int) index);
				mc.player.sendMessage(new StringTextComponent("Removed " + (index + 1) + ". point!"), Util.field_240973_b_);
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"), Util.field_240973_b_);
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("set").then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CMDCamClient.points.set(index, new CamPoint());
				mc.player.sendMessage(new StringTextComponent("Updated " + (index + 1) + ". point!"), Util.field_240973_b_);
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"), Util.field_240973_b_);
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("goto").then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CamPoint point = CMDCamClient.points.get(index);
				mc.player.abilities.isFlying = true;
				
				CamEventHandlerClient.roll = (float) point.roll;
				mc.gameSettings.fov = (float) point.zoom;
				mc.player.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
				mc.player.setLocationAndAngles(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + (index + 1) + "' is too high/low!"), Util.field_240973_b_);
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("mode").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("mode", CamModeArgument.mode()).executes((x) -> {
			String mode = StringArgumentType.getString(x, "mode");
			CMDCamClient.lastMode = mode;
			mc.player.sendMessage(new StringTextComponent("Changed to " + mode + " path!"), Util.field_240973_b_);
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("target").executes((x) -> {
			CamEventHandlerClient.selectEntityMode = true;
			mc.player.sendMessage(new StringTextComponent("Please select a target either an entity or a block!"), Util.field_240973_b_);
			return 0;
		})/*.then(RequiredArgumentBuilder.<ISuggestionProvider, EntitySelector>argument("entity", EntityArgument.entity()).executes((x) -> {
		  CommandContext<CommandSource> context = new CommandContext<>(mc.player.getCommandSource(), x.getInput(), argumentField.get(x), x.getCommand(), x.getRootNode(), x.getNodes(), x.getRange(), x.getChild(), x.getRedirectModifier(), x.isForked());
		  Entity entity = EntityArgument.getEntity(context, "entity");
		  return 0;
		  }))*/
		        .then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("target", TargetArgument.target()).executes((x) -> {
			        String target = StringArgumentType.getString(x, "target");
			        if (target.equalsIgnoreCase("self")) {
				        CMDCamClient.target = new CamTarget.SelfTarget();
				        mc.player.sendMessage(new StringTextComponent("The camera will point towards you!"), Util.field_240973_b_);
			        } else if (target.equals("none")) {
				        CMDCamClient.target = null;
				        mc.player.sendMessage(new StringTextComponent("Removed target!"), Util.field_240973_b_);
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("interpolation").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CMDCamClient.lastInterpolation = interpolation;
			        mc.player.sendMessage(new StringTextComponent("Interpolation is set to '" + interpolation + "'!"), Util.field_240973_b_);
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("show").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
			        if (move != null) {
				        move.isRenderingEnabled = true;
				        mc.player.sendMessage(new StringTextComponent("Showing '" + interpolation + "' interpolation path!"), Util.field_240973_b_);
			        } else if (interpolation.equalsIgnoreCase("all")) {
				        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
					        movement.isRenderingEnabled = true;
				        mc.player.sendMessage(new StringTextComponent("Showing all interpolation paths!"), Util.field_240973_b_);
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("hide").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
			        if (move != null) {
				        move.isRenderingEnabled = false;
				        mc.player.sendMessage(new StringTextComponent("Hiding '" + interpolation + "' interpolation path!"), Util.field_240973_b_);
			        } else if (interpolation.equalsIgnoreCase("all")) {
				        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
					        movement.isRenderingEnabled = false;
				        mc.player.sendMessage(new StringTextComponent("Hiding all interpolation paths!"), Util.field_240973_b_);
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("follow-speed").then(RequiredArgumentBuilder.<ISuggestionProvider, Double>argument("factor", DoubleArgumentType.doubleArg()).executes((x) -> {
			        double factor = DoubleArgumentType.getDouble(x, "factor");
			        CMDCamClient.cameraFollowSpeed = factor;
			        mc.player.sendMessage(new StringTextComponent("Camera follow speed is set to  '" + factor + "'. Default is 1.0!"), Util.field_240973_b_);
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("list").executes((x) -> {
			        if (CMDCamClient.isInstalledOnSever) {
				        mc.player.sendMessage(new StringTextComponent("Use /cam-server list instead!"), Util.field_240973_b_);
				        return 0;
			        }
			        String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
			        for (String key : CMDCamClient.savedPaths.keySet()) {
				        output += key + ", ";
			        }
			        mc.player.sendMessage(new StringTextComponent(output), Util.field_240973_b_);
			        return 0;
		        })).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("load").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("path", StringArgumentType.string()).executes((x) -> {
			        String pathArg = StringArgumentType.getString(x, "path");
			        if (CMDCamClient.isInstalledOnSever) {
				        CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
			        } else {
				        CamPath path = CMDCamClient.savedPaths.get(pathArg);
				        if (path != null) {
					        path.overwriteClientConfig();
					        mc.player.sendMessage(new StringTextComponent("Loaded path '" + pathArg + "' successfully!"), Util.field_240973_b_);
				        } else
					        mc.player.sendMessage(new StringTextComponent("Could not find path '" + pathArg + "'!"), Util.field_240973_b_);
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("save").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("path", StringArgumentType.string()).executes((x) -> {
			        String pathArg = StringArgumentType.getString(x, "path");
			        try {
				        CamPath path = CMDCamClient.createPathFromCurrentConfiguration();
				        
				        if (CMDCamClient.isInstalledOnSever) {
					        CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, path));
				        } else {
					        CMDCamClient.savedPaths.put(pathArg, path);
					        mc.player.sendMessage(new StringTextComponent("Saved path '" + pathArg + "' successfully!"), Util.field_240973_b_);
				        }
			        } catch (PathParseException e) {
				        mc.player.sendMessage(new StringTextComponent(e.getMessage()), Util.field_240973_b_);
			        }
			        return 0;
		        }))));
		
	}
	
	public static void renderBefore(RenderPlayerEvent.Pre event) {
		
	}
	
	public static CamPath getCurrentPath() {
		return currentPath;
	}
	
	public static void startPath(CamPath path) throws PathParseException {
		try {
			currentPath = path;
			currentPath.start(mc.world);
		} catch (PathParseException e) {
			currentPath = null;
			throw e;
		}
	}
	
	public static void stopPath() {
		if (currentPath.serverPath)
			return;
		currentPath.finish(mc.world);
		currentPath = null;
	}
	
	public static void tickPath(World world, float renderTickTime) {
		currentPath.tick(world, renderTickTime);
		if (currentPath.hasFinished())
			currentPath = null;
	}
	
	public static CamPath createPathFromCurrentConfiguration() throws PathParseException {
		if (points.size() < 1)
			throw new PathParseException("You have to register at least 1 point!");
		
		List<CamPoint> newPoints = new ArrayList<>(points);
		if (newPoints.size() == 1)
			newPoints.add(newPoints.get(0));
		
		return new CamPath(lastLoop, lastDuration, lastMode, lastInterpolation, target, newPoints, cameraFollowSpeed);
	}
	
}
