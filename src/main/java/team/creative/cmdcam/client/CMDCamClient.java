package team.creative.cmdcam.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.TargetArgument;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.mode.CamMode;
import team.creative.cmdcam.common.scene.mode.DefaultMode;
import team.creative.cmdcam.common.target.CamTarget;

public class CMDCamClient {
    
    public final static Minecraft mc = Minecraft.getInstance();
    
    public static HashMap<String, CamScene> savedPaths = new HashMap<>();
    
    private static final CamScene scene = new CamScene(10000, 0, "default", new ArrayList<>(), CamInterpolation.HERMITE);
    private static CamScene playing;
    private static boolean serverAvailable = false;
    
    public static void resetServerAvailability() {
        serverAvailable = false;
    }
    
    public static void setServerAvailability() {
        serverAvailable = true;
    }
    
    public static void init(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new CamEventHandlerClient());
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
                    .join(":", CamInterpolation.REGISTRY.keys()) + "> " + ChatFormatting.RED + "set the camera interpolation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam follow-speed <number> " + ChatFormatting.RED + "default is 1.0"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam show <all:" + String
                    .join(":", CamInterpolation.REGISTRY.keys()) + "> " + ChatFormatting.RED + "shows the path using the given interpolation"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam hide <all:" + String
                    .join(":", CamInterpolation.REGISTRY.keys()) + "> " + ChatFormatting.RED + "hides the path using the given interpolation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam save <name> " + ChatFormatting.RED + "saves the current path (including settings) with the given name"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam load <name> " + ChatFormatting.RED + "tries to load the saved path with the given name"), false);
            x.getSource().sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam list " + ChatFormatting.RED + "lists all saved paths"), false);
            return 0;
        }).then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear").executes((x) -> { // cam clear
            x.getSource().sendSuccess(new TextComponent("Cleared all registered points!"), false);
            CMDCamClient.getPoints().clear();
            return 0;
        })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("add").executes((x) -> { // cam add
            CMDCamClient.getPoints().add(CamPoint.createLocal());
            x.getSource().sendSuccess(new TextComponent("Registered " + CMDCamClient.getPoints().size() + ". Point!"), false);
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> { // cam add <index>
            Integer index = IntegerArgumentType.getInteger(x, "index");
            index--;
            if (index >= 0 && index < CMDCamClient.getPoints().size()) {
                CMDCamClient.getPoints().add(index, CamPoint.createLocal());
                x.getSource().sendSuccess(new TextComponent("Inserted " + index + ". Point!"), false);
            } else
                x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
            return 0;
        }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("start").executes((x) -> { // cam start
            try {
                CMDCamClient.start(CMDCamClient.createScene());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("duration", DurationArgument.duration()).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    CMDCamClient.getConfigScene().duration = duration;
                CMDCamClient.start(CMDCamClient.createScene());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        }).then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    CMDCamClient.getConfigScene().duration = duration;
                
                CMDCamClient.getConfigScene().loop = IntegerArgumentType.getInteger(x, "loop");
                CMDCamClient.start(CMDCamClient.createScene());
            } catch (PathParseException e) {
                x.getSource().sendFailure(new TextComponent(e.getMessage()));
            }
            return 0;
        })))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("stop").executes((x) -> {
            CMDCamClient.stop();
            return 0;
        })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("remove")
                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                    Integer index = IntegerArgumentType.getInteger(x, "index");
                    index--;
                    if (index >= 0 && index < CMDCamClient.getPoints().size()) {
                        CMDCamClient.getPoints().remove((int) index);
                        x.getSource().sendSuccess(new TextComponent("Removed " + (index + 1) + ". point!"), false);
                    } else
                        x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
                    return 0;
                }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                            Integer index = IntegerArgumentType.getInteger(x, "index");
                            index--;
                            if (index >= 0 && index < CMDCamClient.getPoints().size()) {
                                CMDCamClient.getPoints().set(index, CamPoint.createLocal());
                                x.getSource().sendSuccess(new TextComponent("Updated " + (index + 1) + ". point!"), false);
                            } else
                                x.getSource().sendFailure(new TextComponent("The given index '" + index + "' is too high/low!"));
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("goto")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                            Integer index = IntegerArgumentType.getInteger(x, "index");
                            index--;
                            if (index >= 0 && index < CMDCamClient.getPoints().size()) {
                                CamPoint point = CMDCamClient.getPoints().get(index);
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
                            CMDCamClient.getConfigScene().mode = CamMode.REGISTRY.createSafe(DefaultMode.class, mode, CMDCamClient.getConfigScene());
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
                                CMDCamClient.getConfigScene().lookTarget = new CamTarget.SelfTarget();
                                x.getSource().sendSuccess(new TextComponent("The camera will point towards you!"), false);
                            } else if (target.equals("none")) {
                                CMDCamClient.getConfigScene().lookTarget = null;
                                x.getSource().sendSuccess(new TextComponent("Removed target!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("interpolation")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CMDCamClient.getConfigScene().interpolation = CamInterpolation.REGISTRY.get(interpolation);
                            x.getSource().sendSuccess(new TextComponent("Interpolation is set to '" + interpolation + "'!"), false);
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("show")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CamInterpolation move = CamInterpolation.REGISTRY.get(interpolation);
                            if (move != null) {
                                move.isRenderingEnabled = true;
                                x.getSource().sendSuccess(new TextComponent("Showing '" + interpolation + "' interpolation path!"), false);
                            } else if (interpolation.equalsIgnoreCase("all")) {
                                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                                    movement.isRenderingEnabled = true;
                                x.getSource().sendSuccess(new TextComponent("Showing all interpolation paths!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("hide")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CamInterpolation move = CamInterpolation.REGISTRY.get(interpolation);
                            if (move != null) {
                                move.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TextComponent("Hiding '" + interpolation + "' interpolation path!"), false);
                            } else if (interpolation.equalsIgnoreCase("all")) {
                                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                                    movement.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TextComponent("Hiding all interpolation paths!"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("list").executes((x) -> {
                    if (CMDCamClient.serverAvailable) {
                        x.getSource().sendSuccess(new TextComponent("Use /cam-server list instead!"), false);
                        return 0;
                    }
                    x.getSource().sendSuccess(new TextComponent("There are " + CMDCamClient.savedPaths.size() + " path(s) in total. " + String
                            .join(", ", CMDCamClient.savedPaths.keySet())), false);
                    return 0;
                })).then(LiteralArgumentBuilder.<CommandSourceStack>literal("load")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("path", StringArgumentType.string()).executes((x) -> {
                            String pathArg = StringArgumentType.getString(x, "path");
                            if (CMDCamClient.serverAvailable)
                                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
                            else {
                                CamScene scene = CMDCamClient.savedPaths.get(pathArg);
                                if (scene != null) {
                                    set(scene);
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
                                CamScene scene = CMDCamClient.createScene();
                                
                                if (CMDCamClient.serverAvailable)
                                    CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, scene));
                                else {
                                    CMDCamClient.savedPaths.put(pathArg, scene);
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
    
    public static CamScene getScene() {
        if (isPlaying())
            return playing;
        return scene;
    }
    
    public static CamScene getConfigScene() {
        return scene;
    }
    
    public static boolean isPlaying() {
        return playing != null;
    }
    
    public static List<CamPoint> getPoints() {
        return scene.points;
    }
    
    public static void set(CamScene scene) {
        CMDCamClient.scene.set(scene);
    }
    
    public static void start(CamScene scene) {
        playing = scene;
        playing.play();
    }
    
    public static void pause() {
        if (playing != null)
            playing.pause();
    }
    
    public static void resume() {
        if (playing != null)
            playing.resume();
    }
    
    public static void stop() {
        if (playing == null)
            return;
        if (playing.serverSynced())
            return;
        playing.finish(mc.level);
        playing = null;
    }
    
    public static void stopServer() {
        if (playing == null)
            return;
        playing.finish(mc.level);
        playing = null;
    }
    
    public static void tickPath(Level level, float renderTickTime) {
        playing.tick(level, renderTickTime);
        if (!playing.playing())
            playing = null;
    }
    
    public static CamScene createScene() throws PathParseException {
        if (scene.points.size() < 1)
            throw new PathParseException("You have to register at least 1 point!");
        
        CamScene newScene = scene.copy();
        if (newScene.points.size() == 1)
            newScene.points.add(newScene.points.get(0));
        return newScene;
    }
    
}
