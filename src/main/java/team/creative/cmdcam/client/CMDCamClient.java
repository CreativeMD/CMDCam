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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
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
        mc = Minecraft.getInstance();
        KeyHandler.initKeys();
        event.enqueueWork(() -> {
            ClientCommandRegistry.register(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("cam").executes((x) -> {
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam add [number] " + ChatFormatting.RED + "register a point at the current position"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam clear " + ChatFormatting.RED + "delete all registered points"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam start [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam stop " + ChatFormatting.RED + "stops the animation"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam goto <index> " + ChatFormatting.RED + "tp to the given point"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam set <index> " + ChatFormatting.RED + "updates point to current location"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam remove <index> " + ChatFormatting.RED + "removes the given point"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam target <none:self> " + ChatFormatting.RED + "set the camera target"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam mode <default:outside> " + ChatFormatting.RED + "set current mode"), Util.NIL_UUID);
                mc.player.sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam interpolation <" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "set the camera interpolation"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"), Util.NIL_UUID);
                mc.player.sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"), Util.NIL_UUID);
                mc.player.sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String
                        .join(":", CamInterpolation.getMovementNames()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"), Util.NIL_UUID);
                mc.player
                        .sendMessage(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"), Util.NIL_UUID);
                return 0;
            }).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("clear").executes((x) -> { // cam clear
                mc.player.sendMessage(new TextComponent("Cleared all registered points!"), Util.NIL_UUID);
                CMDCamClient.points.clear();
                return 0;
            })).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("add").executes((x) -> { // cam add
                CMDCamClient.points.add(new CamPoint());
                mc.player.sendMessage(new TextComponent("Registered " + CMDCamClient.points.size() + ". Point!"), Util.NIL_UUID);
                return 0;
            }).then(RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> { // cam add <index>
                Integer index = IntegerArgumentType.getInteger(x, "index");
                index--;
                if (index >= 0 && index < CMDCamClient.points.size()) {
                    CMDCamClient.points.add(index, new CamPoint());
                    mc.player.sendMessage(new TextComponent("Inserted " + index + ". Point!"), Util.NIL_UUID);
                } else
                    mc.player.sendMessage(new TextComponent("The given index '" + index + "' is too high/low!"), Util.NIL_UUID);
                return 0;
            }))).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("start").executes((x) -> { // cam start
                try {
                    CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                } catch (PathParseException e) {
                    mc.player.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
                }
                return 0;
            }).then(RequiredArgumentBuilder.<SharedSuggestionProvider, Long>argument("duration", DurationArgument.duration()).executes((x) -> {
                try {
                    long duration = DurationArgument.getDuration(x, "duration");
                    if (duration > 0)
                        CMDCamClient.lastDuration = duration;
                    CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                } catch (PathParseException e) {
                    mc.player.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
                }
                return 0;
            }).then(RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
                try {
                    long duration = DurationArgument.getDuration(x, "duration");
                    if (duration > 0)
                        CMDCamClient.lastDuration = duration;
                    
                    CMDCamClient.lastLoop = IntegerArgumentType.getInteger(x, "loop");
                    CMDCamClient.startPath(CMDCamClient.createPathFromCurrentConfiguration());
                } catch (PathParseException e) {
                    mc.player.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
                }
                return 0;
            })))).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("stop").executes((x) -> {
                CMDCamClient.stopPath();
                return 0;
            })).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("remove")
                    .then(RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                        Integer index = IntegerArgumentType.getInteger(x, "index");
                        index--;
                        if (index >= 0 && index < CMDCamClient.points.size()) {
                            CMDCamClient.points.remove((int) index);
                            mc.player.sendMessage(new TextComponent("Removed " + (index + 1) + ". point!"), Util.NIL_UUID);
                        } else
                            mc.player.sendMessage(new TextComponent("The given index '" + index + "' is too high/low!"), Util.NIL_UUID);
                        return 0;
                    }))).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("set")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                                Integer index = IntegerArgumentType.getInteger(x, "index");
                                index--;
                                if (index >= 0 && index < CMDCamClient.points.size()) {
                                    CMDCamClient.points.set(index, new CamPoint());
                                    mc.player.sendMessage(new TextComponent("Updated " + (index + 1) + ". point!"), Util.NIL_UUID);
                                } else
                                    mc.player.sendMessage(new TextComponent("The given index '" + index + "' is too high/low!"), Util.NIL_UUID);
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("goto")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
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
                                    mc.player.sendMessage(new TextComponent("The given index '" + (index + 1) + "' is too high/low!"), Util.NIL_UUID);
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("mode")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("mode", CamModeArgument.mode()).executes((x) -> {
                                String mode = StringArgumentType.getString(x, "mode");
                                CMDCamClient.lastMode = mode;
                                mc.player.sendMessage(new TextComponent("Changed to " + mode + " path!"), Util.NIL_UUID);
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("target").executes((x) -> {
                        CamEventHandlerClient.selectEntityMode = true;
                        mc.player.sendMessage(new TextComponent("Please select a target either an entity or a block!"), Util.NIL_UUID);
                        return 0;
                    })/*.then(RequiredArgumentBuilder.<ISuggestionProvider, EntitySelector>argument("entity", EntityArgument.entity()).executes((x) -> {
                      CommandContext<CommandSource> context = new CommandContext<>(mc.player.getCommandSource(), x.getInput(), argumentField.get(x), x.getCommand(), x.getRootNode(), x.getNodes(), x.getRange(), x.getChild(), x.getRedirectModifier(), x.isForked());
                      Entity entity = EntityArgument.getEntity(context, "entity");
                      return 0;
                      }))*/
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("target", TargetArgument.target()).executes((x) -> {
                                String target = StringArgumentType.getString(x, "target");
                                if (target.equalsIgnoreCase("self")) {
                                    CMDCamClient.target = new CamTarget.SelfTarget();
                                    mc.player.sendMessage(new TextComponent("The camera will point towards you!"), Util.NIL_UUID);
                                } else if (target.equals("none")) {
                                    CMDCamClient.target = null;
                                    mc.player.sendMessage(new TextComponent("Removed target!"), Util.NIL_UUID);
                                }
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("interpolation")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
                                String interpolation = StringArgumentType.getString(x, "interpolation");
                                CMDCamClient.lastInterpolation = interpolation;
                                mc.player.sendMessage(new TextComponent("Interpolation is set to '" + interpolation + "'!"), Util.NIL_UUID);
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("show")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                                String interpolation = StringArgumentType.getString(x, "interpolation");
                                CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
                                if (move != null) {
                                    move.isRenderingEnabled = true;
                                    mc.player.sendMessage(new TextComponent("Showing '" + interpolation + "' interpolation path!"), Util.NIL_UUID);
                                } else if (interpolation.equalsIgnoreCase("all")) {
                                    for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
                                        movement.isRenderingEnabled = true;
                                    mc.player.sendMessage(new TextComponent("Showing all interpolation paths!"), Util.NIL_UUID);
                                }
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("hide")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                                String interpolation = StringArgumentType.getString(x, "interpolation");
                                CamInterpolation move = CamInterpolation.getInterpolation(interpolation);
                                if (move != null) {
                                    move.isRenderingEnabled = false;
                                    mc.player.sendMessage(new TextComponent("Hiding '" + interpolation + "' interpolation path!"), Util.NIL_UUID);
                                } else if (interpolation.equalsIgnoreCase("all")) {
                                    for (CamInterpolation movement : CamInterpolation.interpolationTypes.values())
                                        movement.isRenderingEnabled = false;
                                    mc.player.sendMessage(new TextComponent("Hiding all interpolation paths!"), Util.NIL_UUID);
                                }
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("follow-speed")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, Double>argument("factor", DoubleArgumentType.doubleArg()).executes((x) -> {
                                double factor = DoubleArgumentType.getDouble(x, "factor");
                                CMDCamClient.cameraFollowSpeed = factor;
                                mc.player.sendMessage(new TextComponent("Camera follow speed is set to  '" + factor + "'. Default is 1.0!"), Util.NIL_UUID);
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("list").executes((x) -> {
                        if (CMDCamClient.isInstalledOnSever) {
                            mc.player.sendMessage(new TextComponent("Use /cam-server list instead!"), Util.NIL_UUID);
                            return 0;
                        }
                        String output = "There are " + CMDCamClient.savedPaths.size() + " path(s) in total. ";
                        for (String key : CMDCamClient.savedPaths.keySet()) {
                            output += key + ", ";
                        }
                        mc.player.sendMessage(new TextComponent(output), Util.NIL_UUID);
                        return 0;
                    })).then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("load")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("path", StringArgumentType.string()).executes((x) -> {
                                String pathArg = StringArgumentType.getString(x, "path");
                                if (CMDCamClient.isInstalledOnSever) {
                                    CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
                                } else {
                                    CamPath path = CMDCamClient.savedPaths.get(pathArg);
                                    if (path != null) {
                                        path.overwriteClientConfig();
                                        mc.player.sendMessage(new TextComponent("Loaded path '" + pathArg + "' successfully!"), Util.NIL_UUID);
                                    } else
                                        mc.player.sendMessage(new TextComponent("Could not find path '" + pathArg + "'!"), Util.NIL_UUID);
                                }
                                return 0;
                            })))
                    .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("save")
                            .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("path", StringArgumentType.string()).executes((x) -> {
                                String pathArg = StringArgumentType.getString(x, "path");
                                try {
                                    CamPath path = CMDCamClient.createPathFromCurrentConfiguration();
                                    
                                    if (CMDCamClient.isInstalledOnSever) {
                                        CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, path));
                                    } else {
                                        CMDCamClient.savedPaths.put(pathArg, path);
                                        mc.player.sendMessage(new TextComponent("Saved path '" + pathArg + "' successfully!"), Util.NIL_UUID);
                                    }
                                } catch (PathParseException e) {
                                    mc.player.sendMessage(new TextComponent(e.getMessage()), Util.NIL_UUID);
                                }
                                return 0;
                            }))));
        });
        
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
