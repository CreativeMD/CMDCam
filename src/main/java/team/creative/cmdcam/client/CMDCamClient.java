package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;
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
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam stop " + ChatFormatting.RED + "stops the animation"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String.join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"));
			mc.player.sendMessage(new StringTextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"));
			return 0;
		}).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("clear").executes((x) -> { // cam clear
			mc.player.sendMessage(new StringTextComponent("Cleared all registered points!"));
			CMDCamClient.points.clear();
			return 0;
		})).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("add").executes((x) -> { // cam add
			CMDCamClient.points.add(new CamPoint());
			mc.player.sendMessage(new StringTextComponent("Registered " + CMDCamClient.points.size() + ". Point!"));
			return 0;
		}).then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> { // cam add <index>
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CMDCamClient.points.add(index, new CamPoint());
				mc.player.sendMessage(new StringTextComponent("Inserted " + index + ". Point!"));
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"));
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("start").executes((x) -> { // cam start
			try {
				CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
			} catch (PathParseException e) {
				mc.player.sendMessage(new StringTextComponent(e.getMessage()));
			}
			return 0;
		}).then(RequiredArgumentBuilder.<ISuggestionProvider, Long>argument("duration", DurationArgument.duration()).executes((x) -> {
			try {
				long duration = DurationArgument.getDuration(x, "duration");
				if (duration > 0)
					CMDCamClient.lastDuration = duration;
				CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
			} catch (PathParseException e) {
				mc.player.sendMessage(new StringTextComponent(e.getMessage()));
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
				mc.player.sendMessage(new StringTextComponent(e.getMessage()));
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
				mc.player.sendMessage(new StringTextComponent("Removed " + (index + 1) + ". point!"));
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"));
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("set").then(RequiredArgumentBuilder.<ISuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
			Integer index = IntegerArgumentType.getInteger(x, "index");
			index--;
			if (index >= 0 && index < CMDCamClient.points.size()) {
				CMDCamClient.points.set(index, new CamPoint());
				mc.player.sendMessage(new StringTextComponent("Updated " + (index + 1) + ". point!"));
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"));
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
				mc.player.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
			} else
				mc.player.sendMessage(new StringTextComponent("The given index '" + index + "' is too high/low!"));
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("mode").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("mode", CamModeArgument.mode()).executes((x) -> {
			String mode = StringArgumentType.getString(x, "mode");
			CMDCamClient.lastMode = mode;
			mc.player.sendMessage(new StringTextComponent("Changed to " + mode + " path!"));
			return 0;
		}))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("target").executes((x) -> {
			CamEventHandlerClient.selectEntityMode = true;
			mc.player.sendMessage(new StringTextComponent("Please select a target either an entity or a block!"));
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
				        mc.player.sendMessage(new StringTextComponent("The camera will point towards you!"));
			        } else if (target.equals("none")) {
				        CMDCamClient.target = null;
				        mc.player.sendMessage(new StringTextComponent("Removed target!"));
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("interpolation").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CMDCamClient.lastInterpolation = interpolation;
			        mc.player.sendMessage(new StringTextComponent("Interpolation is set to '" + interpolation + "'!"));
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("show").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
			        if (move != null) {
				        move.isRenderingEnabled = true;
				        mc.player.sendMessage(new StringTextComponent("Showing '" + interpolation + "' interpolation path!"));
			        } else if (interpolation.equalsIgnoreCase("all")) {
				        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
					        movement.isRenderingEnabled = true;
				        mc.player.sendMessage(new StringTextComponent("Showing all interpolation paths!"));
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("hide").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
			        String interpolation = StringArgumentType.getString(x, "interpolation");
			        CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
			        if (move != null) {
				        move.isRenderingEnabled = false;
				        mc.player.sendMessage(new StringTextComponent("Hiding '" + interpolation + "' interpolation path!"));
			        } else if (interpolation.equalsIgnoreCase("all")) {
				        for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
					        movement.isRenderingEnabled = false;
				        mc.player.sendMessage(new StringTextComponent("Hiding all interpolation paths!"));
			        }
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("follow-speed").then(RequiredArgumentBuilder.<ISuggestionProvider, Double>argument("factor", DoubleArgumentType.doubleArg()).executes((x) -> {
			        double factor = DoubleArgumentType.getDouble(x, "factor");
			        CMDCamClient.cameraFollowSpeed = factor;
			        mc.player.sendMessage(new StringTextComponent("Camera follow speed is set to  '" + factor + "'. Default is 1.0!"));
			        return 0;
		        }))).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("list").executes((x) -> {
			        if (CMDCamClient.isInstalledOnSever) {
				        mc.player.sendMessage(new StringTextComponent("Use /cam-server list instead!"));
				        return 0;
			        }
			        String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
			        for (String key : CMDCamClient.savedPaths.keySet()) {
				        output += key + ", ";
			        }
			        mc.player.sendMessage(new StringTextComponent(output));
			        return 0;
		        })).then(LiteralArgumentBuilder.<ISuggestionProvider>literal("load").then(RequiredArgumentBuilder.<ISuggestionProvider, String>argument("path", StringArgumentType.string()).executes((x) -> {
			        String pathArg = StringArgumentType.getString(x, "path");
			        if (CMDCamClient.isInstalledOnSever) {
				        CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
			        } else {
				        CamPath path = CMDCamClient.savedPaths.get(pathArg);
				        if (path != null) {
					        path.overwriteClientConfig();
					        mc.player.sendMessage(new StringTextComponent("Loaded path '" + pathArg + "' successfully!"));
				        } else
					        mc.player.sendMessage(new StringTextComponent("Could not find path '" + pathArg + "'!"));
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
					        mc.player.sendMessage(new StringTextComponent("Saved path '" + pathArg + "' successfully!"));
				        }
			        } catch (PathParseException e) {
				        mc.player.sendMessage(new StringTextComponent(e.getMessage()));
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
		currentPath = path;
		currentPath.start(mc.world);
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
