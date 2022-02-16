package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
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
        mc = Minecraft.getInstance();
        KeyHandler.initKeys();
    }
    
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("cam").executes((x) -> {
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam stop " + ChatFormatting.RED + "stops the animation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String
                    .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String
                    .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String
                    .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"), false);
            return 0;
        }).then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear").executes((x) -> { // cam clear
            x.getSource().sendSuccess(new TextComponent("Cleared all registered points!"), false);
            CMDCamClient.points.clear();
            return 0;
        })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("add").executes((x) -> { // cam add
            CMDCamClient.points.add(new CamPoint());
            x.getSource().sendSuccess(new TextComponent("Registered " + CMDCamClient.points.size() + ". Point!"), false);
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> { // cam add <index>
            Integer index = IntegerArgumentType.getInteger(x, "index");
            index--;
            if (index >= 0 && index < CMDCamClient.points.size()) {
                CMDCamClient.points.add(index, new CamPoint());
                x.getSource().sendSuccess(new TextComponent("Inserted " + index + ". Point!"), false);
            } else
                x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
            return 0;
        }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("start").executes((x) -> { // cam start
            try {
                CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("duration", DurationArgument.duration()).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    CMDCamClient.lastDuration = duration;
                CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    CMDCamClient.lastDuration = duration;
                
                CMDCamClient.lastLoop = IntegerArgumentType.getInteger(x, "loop");
                CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        })))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("stop").executes((x) -> {
            CMDCamClient.stopPath();
            return 0;
        })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("remove")
                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                    Integer index = IntegerArgumentType.getInteger(x, "index");
                    index--;
                    if (index >= 0 && index < CMDCamClient.points.size()) {
                        CMDCamClient.points.remove((int) index);
                        x.getSource().sendSuccess(new TextComponent("Removed " + (index + 1) + ". point!"), false);
                    } else
                        x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
                    return 0;
                }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                            Integer index = IntegerArgumentType.getInteger(x, "index");
                            index--;
                            if (index >= 0 && index < CMDCamClient.points.size()) {
                                CMDCamClient.points.set(index, new CamPoint());
                                x.getSource().sendSuccess(new TextComponent("Updated " + (index + 1) + ". point!"), false);
                            } else
                                x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("goto")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                            Integer index = IntegerArgumentType.getInteger(x, "index");
                            index--;
                            if (index >= 0 && index < CMDCamClient.points.size()) {
                                CamPoint point = CMDCamClient.points.get(index);
                                mc.player.getAbilities().flying = true;
                                
                                CamEventHandlerClient.roll = (float) point.roll;
                                mc.options.fov = (float) point.zoom;
                                mc.player.absMoveTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
                                mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
                            } else
                                x.getSource().sendFailure(new TextComponent("The given index '" + (index + 1) + "' is too high/low!"));
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("mode")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("mode", CamModeArgument.mode()).executes((x) -> {
                            String mode = StringArgumentType.getString(x, "mode");
                            CMDCamClient.lastMode = mode;
                            x.getSource().sendSuccess(new TextComponent("Changed to " + mode + " path!"), false);
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("target").executes((x) -> {
                    CamEventHandlerClient.selectEntityMode = true;
                    x.getSource().sendSuccess(new TextComponent("Please select a target either an entity or a block!"), false);
                    return 0;
                })/*.then(RequiredArgumentBuilder.<ISuggestionProvider, EntitySelector>argument("entity", EntityArgument.entity()).executes((x) -> {
                  CommandContext<CommandSource> context = new CommandContext<>(mc.player.getCommandSource(), x.getInput(), argumentField.get(x), x.getCommand(), x.getRootNode(), x.getNodes(), x.getRange(), x.getChild(), x.getRedirectModifier(), x.isForked());
                  Entity entity = EntityArgument.getEntity(context, "entity");
                  return 0;
                  }))*/
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("target", TargetArgument.target()).executes((x) -> {
                            String target = StringArgumentType.getString(x, "target");
                            if (target.equalsIgnoreCase("self")) {
                                CMDCamClient.target = new CamTarget.SelfTarget();
                                x.getSource().sendSuccess(new TextComponent("The camera will point towards you!"), false);
                            } else if (target.equals("none")) {
                                CMDCamClient.target = null;
                                x.getSource().sendSuccess(new TextComponent("Removed target!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("interpolation")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CMDCamClient.lastInterpolation = interpolation;
                            x.getSource().sendSuccess(new TextComponent("Interpolation is set to '" + interpolation + "'!"), false);
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("show")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
                            if (move != null) {
                                move.isRenderingEnabled = true;
                                x.getSource().sendSuccess(new TextComponent("Showing '" + interpolation + "' interpolation path!"), false);
                            } else if (interpolation.equalsIgnoreCase("all")) {
                                for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
                                    movement.isRenderingEnabled = true;
                                x.getSource().sendSuccess(new TextComponent("Showing all interpolation paths!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("hide")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
                            if (move != null) {
                                move.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TextComponent("Hiding '" + interpolation + "' interpolation path!"), false);
                            } else if (interpolation.equalsIgnoreCase("all")) {
                                for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
                                    movement.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TextComponent("Hiding all interpolation paths!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("follow-speed")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("factor", DoubleArgumentType.doubleArg()).executes((x) -> {
                            double factor = DoubleArgumentType.getDouble(x, "factor");
                            CMDCamClient.cameraFollowSpeed = factor;
                            x.getSource().sendSuccess(new TextComponent("Camera follow speed is set to  '" + factor + "'. Default is 1.0!"), false);
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("list").executes((x) -> {
                    if (CMDCamClient.isInstalledOnSever) {
                        x.getSource().sendSuccess(new TextComponent("Use /cam-server list instead!"), false);
                        return 0;
                    }
                    String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
                    for (String key : CMDCamClient.savedPaths.keySet()) {
                        output += key + ", ";
                    }
                    x.getSource().sendSuccess(new TextComponent(output), false);
                    return 0;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("load")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("path", StringArgumentType.string()).executes((x) -> {
                            String pathArg = StringArgumentType.getString(x, "path");
                            if (CMDCamClient.isInstalledOnSever) {
                                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
                            } else {
                                CamPath path = CMDCamClient.savedPaths.get(pathArg);
                                if (path != null) {
                                    path.overwriteClientConfig();
                                    x.getSource().sendSuccess(new TextComponent("Loaded path '" + pathArg + "' successfully!"), false);
                                } else
                                    x.getSource().sendFailure(new TextComponent("Could not find path '" + pathArg + "'!"));
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("save")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("path", StringArgumentType.string()).executes((x) -> {
                            String pathArg = StringArgumentType.getString(x, "path");
                            try {
                                CamPath path = CMDCamClient.createPathFromCurrentConfiguration();
                                
                                if (CMDCamClient.isInstalledOnSever) {
                                    CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, path));
                                } else {
                                    CMDCamClient.savedPaths.put(pathArg, path);
                                    x.getSource().sendSuccess(new TextComponent("Saved path '" + pathArg + "' successfully!"), false);
                                }
                            } catch (PathParseException e) {
                                x.getSource().sendFailure(new TextComponent(e.getMessage()));
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
            currentPath.start(mc.level);
        } catch (PathParseException e) {
            currentPath = null;
            throw e;
        }
    }
    
    public static void stopPathServer() {
        currentPath.finish(mc.level);
        currentPath = null;
    }
    
    public static void stopPath() {
        if (currentPath.serverPath)
            return;
        currentPath.finish(mc.level);
        currentPath = null;
    }
    
    public static void tickPath(Level level, float renderTickTime) {
        currentPath.tick(level, renderTickTime);
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
