package team.creative.cmdcam.client;

import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.scene.CamScene;

public class CMDCamClient {
    
    public final static Minecraft mc = Minecraft.getInstance();
    private static final CamCommandProcessorClient processor = new CamCommandProcessorClient();
    
    public static HashMap<String, CamScene> savedPaths = new HashMap<>();
    
    private static final CamScene scene = CamScene.createDefault();
    private static CamScene playing;
    private static boolean serverAvailable = false;
    private static boolean hideGuiCache;
    
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
        LiteralArgumentBuilder<CommandSourceStack> cam = Commands.literal("cam");
        
        SceneCommandBuilder.scene(cam, processor);
        
        event.getDispatcher().register(cam.then(LiteralArgumentBuilder.<CommandSourceStack>literal("show")
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                    String interpolation = StringArgumentType.getString(x, "interpolation");
                    CamInterpolation move = CamInterpolation.REGISTRY.get(interpolation);
                    if (move != null) {
                        move.isRenderingEnabled = true;
                        x.getSource().sendSuccess(new TranslatableComponent("scene.interpolation.show", interpolation), false);
                    } else if (interpolation.equalsIgnoreCase("all")) {
                        for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                            movement.isRenderingEnabled = true;
                        x.getSource().sendSuccess(new TranslatableComponent("scene.interpolation.show_all"), false);
                    }
                    return 0;
                }))).then(LiteralArgumentBuilder.<CommandSourceStack>literal("hide")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
                            String interpolation = StringArgumentType.getString(x, "interpolation");
                            CamInterpolation move = CamInterpolation.REGISTRY.get(interpolation);
                            if (move != null) {
                                move.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TranslatableComponent("scene.interpolation.hide", interpolation), false);
                            } else if (interpolation.equalsIgnoreCase("all")) {
                                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                                    movement.isRenderingEnabled = false;
                                x.getSource().sendSuccess(new TranslatableComponent("scene.interpolation.hide_all"), false);
                            }
                            return 0;
                        })))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("list").executes((x) -> {
                    if (CMDCamClient.serverAvailable) {
                        x.getSource().sendFailure(new TranslatableComponent("scenes.list_fail"));
                        return 0;
                    }
                    x.getSource().sendSuccess(new TranslatableComponent("scenes.list", savedPaths.size(), String.join(", ", savedPaths.keySet())), true);
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
                                    x.getSource().sendSuccess(new TranslatableComponent("scenes.load", pathArg), false);
                                } else
                                    x.getSource().sendFailure(new TranslatableComponent("scenes.load_fail", pathArg));
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
                                    x.getSource().sendSuccess(new TranslatableComponent("scenes.save", pathArg), false);
                                }
                            } catch (PathParseException e) {
                                x.getSource().sendFailure(new TranslatableComponent(e.getMessage()));
                            }
                            return 0;
                        }))));
    }
    
    public static void renderBefore(RenderPlayerEvent.Pre event) {}
    
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
        if (scene.points.isEmpty())
            return;
        if (scene.points.size() == 1)
            scene.points.add(scene.points.get(0));
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
    
    public static void noTickPath(Level level, float renderTickTime) {
        hideGuiCache = mc.options.hideGui;
    }
    
    public static void tickPath(Level level, float renderTickTime) {
        playing.tick(level, renderTickTime);
        if (!playing.playing()) {
            mc.options.hideGui = hideGuiCache;
            playing = null;
        }
    }
    
    public static CamScene createScene() throws PathParseException {
        if (scene.points.size() < 1)
            throw new PathParseException("scene.create_fail");
        
        CamScene newScene = scene.copy();
        if (newScene.points.size() == 1)
            newScene.points.add(newScene.points.get(0));
        return newScene;
    }
    
    public static void teleportTo(CamPoint point) {
        Minecraft mc = Minecraft.getInstance();
        mc.player.getAbilities().flying = true;
        
        CamEventHandlerClient.roll = (float) point.roll;
        mc.options.fov = (float) point.zoom;
        mc.player.absMoveTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
}
