package team.creative.cmdcam.client;

import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.NetworkConstants;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.builder.PointArgumentBuilder;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.SceneStartCommandBuilder;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.client.CreativeCoreClient;

public class CMDCamClient {
    
    public final static Minecraft mc = Minecraft.getInstance();
    public static final CamCommandProcessorClient PROCESSOR = new CamCommandProcessorClient();
    public static final HashMap<String, CamScene> SCENES = new HashMap<>();
    
    private static final CamScene scene = CamScene.createDefault();
    private static CamScene playing;
    private static boolean serverAvailable = false;
    private static boolean hideGuiCache;
    private static boolean hasTargetMarker;
    private static CamPoint targetMarker;
    
    public static void resetServerAvailability() {
        serverAvailable = false;
    }
    
    public static void setServerAvailability() {
        serverAvailable = true;
    }
    
    public static void init(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new CamEventHandlerClient());
        CreativeCoreClient.registerClientConfig(CMDCam.MODID);
        ModLoadingContext.get()
                .registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        CMDCamClient.init(event);
    }
    
    public static void load(IEventBus bus) {
        bus.addListener(CMDCamClient::init);
        bus.addListener(CMDCamClient::commands);
        bus.addListener(KeyHandler::registerKeys);
    }
    
    public static void commands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> cam = Commands.literal("cam");
        
        SceneStartCommandBuilder.start(cam, PROCESSOR);
        
        SceneCommandBuilder.scene(cam, PROCESSOR);
        
        event.getDispatcher().register(cam.then(Commands.literal("stop").executes(x -> {
            CMDCamClient.stop();
            return 0;
        })).then(Commands.literal("pause").executes(x -> {
            CMDCamClient.pause();
            return 0;
        })).then(Commands.literal("resume").executes(x -> {
            CMDCamClient.resume();
            return 0;
        })).then(Commands.literal("show").then(Commands.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = true;
                x.getSource().sendSuccess(Component.translatable("scene.interpolation.show", interpolation), false);
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = true;
                x.getSource().sendSuccess(Component.translatable("scene.interpolation.show_all"), false);
            }
            return 0;
        }))).then(Commands.literal("hide").then(Commands.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = false;
                x.getSource().sendSuccess(Component.translatable("scene.interpolation.hide", interpolation), false);
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = false;
                x.getSource().sendSuccess(Component.translatable("scene.interpolation.hide_all"), false);
            }
            return 0;
        }))).then(Commands.literal("list").executes((x) -> {
            if (CMDCamClient.serverAvailable) {
                x.getSource().sendFailure(Component.translatable("scenes.list_fail"));
                return 0;
            }
            x.getSource().sendSuccess(Component.translatable("scenes.list", SCENES.size(), String.join(", ", SCENES.keySet())), true);
            return 0;
        })).then(Commands.literal("load").then(Commands.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            if (CMDCamClient.serverAvailable)
                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
            else {
                CamScene scene = CMDCamClient.SCENES.get(pathArg);
                if (scene != null) {
                    set(scene);
                    x.getSource().sendSuccess(Component.translatable("scenes.load", pathArg), false);
                } else
                    x.getSource().sendFailure(Component.translatable("scenes.load_fail", pathArg));
            }
            return 0;
        }))).then(Commands.literal("save").then(Commands.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            try {
                CamScene scene = CMDCamClient.createScene();
                
                if (CMDCamClient.serverAvailable)
                    CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, scene));
                else {
                    CMDCamClient.SCENES.put(pathArg, scene);
                    x.getSource().sendSuccess(Component.translatable("scenes.save", pathArg), false);
                }
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            return 0;
        }))).then(new PointArgumentBuilder("follow_center", (x, y) -> targetMarker = y, PROCESSOR).executes(x -> {
            targetMarker = CamPoint.createLocal();
            return 0;
        })));
        
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
        checkTargetMarker();
    }
    
    public static void checkTargetMarker() {
        hasTargetMarker = scene.posTarget != null;
        if (hasTargetMarker && targetMarker == null)
            targetMarker = CamPoint.createLocal();
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
        mc.options.hideGui = hideGuiCache;
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
        mc.options.hideGui = hideGuiCache;
    }
    
    public static void stopServer() {
        if (playing == null)
            return;
        playing.finish(mc.level);
        playing = null;
        mc.options.hideGui = hideGuiCache;
    }
    
    public static void noTickPath(Level level, float renderTickTime) {
        hideGuiCache = mc.options.hideGui;
    }
    
    public static void mcTickPath(Level level) {
        playing.mcTick(level);
    }
    
    public static void tickPath(Level level, float renderTickTime) {
        playing.tick(level, renderTickTime);
        if (!playing.playing()) {
            mc.options.hideGui = hideGuiCache;
            playing = null;
        }
    }
    
    public static void resetTargetMarker() {
        targetMarker = null;
    }
    
    public static boolean hasTargetMarker() {
        return hasTargetMarker && targetMarker != null && scene.posTarget != null;
    }
    
    public static CamPoint getTargetMarker() {
        return targetMarker;
    }
    
    public static CamScene createScene() throws SceneException {
        if (scene.points.size() < 1)
            throw new SceneException("scene.create_fail");
        
        CamScene newScene = scene.copy();
        if (newScene.points.size() == 1)
            newScene.points.add(newScene.points.get(0));
        return newScene;
    }
    
    public static void teleportTo(CamPoint point) {
        Minecraft mc = Minecraft.getInstance();
        mc.player.getAbilities().flying = true;
        
        CamEventHandlerClient.roll = (float) point.roll;
        CamEventHandlerClient.currentFOV = (float) point.zoom;
        mc.player.absMoveTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
}
