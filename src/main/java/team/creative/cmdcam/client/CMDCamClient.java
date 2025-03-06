package team.creative.cmdcam.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.fabricators_of_create.porting_lib.event.client.FieldOfViewEvents;
import io.github.fabricators_of_create.porting_lib.event.client.RenderTickStartCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.client.mixin.MinecraftAccessor;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.builder.client.ClientPointArgumentBuilder;
import team.creative.cmdcam.common.command.builder.client.ClientSceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.client.ClientSceneStartCommandBuilder;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.fabric.ComputeCameraAnglesCallback;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.creativecore.client.CreativeCoreClient;

import java.util.HashMap;
import java.util.List;

public class CMDCamClient implements ClientModInitializer {
    
    public final static Minecraft mc = Minecraft.getInstance();
    public static final CamCommandProcessorClient PROCESSOR_CLIENT = new CamCommandProcessorClient();
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

    @Override
    public void onInitializeClient() {
        resetScenes();
        registerEvents();
    }

    private static void registerEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(CamEventHandlerClient::onClientTick);
        RenderTickStartCallback.EVENT.register(CamEventHandlerClient::onRenderTick);
        FieldOfViewEvents.COMPUTE.register(CamEventHandlerClient::fov);
        WorldRenderEvents.AFTER_ENTITIES.register(CamEventHandlerClient::worldRender);

        ComputeCameraAnglesCallback.EVENT.register(CamEventHandlerClient::cameraRoll);

        UseBlockCallback.EVENT.register(CamEventHandlerClient::onPlayerUseBlock);
        UseEntityCallback.EVENT.register(CamEventHandlerClient::onPlayerUseEntity);

        CreativeCoreClient.registerClientConfig(CMDCam.MODID);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> commands(dispatcher));

        KeyHandler.registerKeys();
    }
    
    public static void commands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var cam = ClientCommandManager.literal("cam");

        ClientSceneStartCommandBuilder.start(cam, PROCESSOR_CLIENT);
        ClientSceneCommandBuilder.scene(cam, PROCESSOR_CLIENT);
        
        dispatcher.register(cam.then(ClientCommandManager.literal("stop").executes(x -> {
            CMDCamClient.stop();
            return 0;
        })).then(ClientCommandManager.literal("pause").executes(x -> {
            CMDCamClient.pause();
            return 0;
        })).then(ClientCommandManager.literal("resume").executes(x -> {
            CMDCamClient.resume();
            return 0;
        })).then(ClientCommandManager.literal("show").executes(x -> {
            CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = true;
            x.getSource().sendFeedback(Component.translatable("scene.interpolation.show_active"));
            return 0;
        }).then(ClientCommandManager.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = true;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.show", interpolation));
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = true;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.show_all"));
            }
            return 0;
        }))).then(ClientCommandManager.literal("hide").executes(x -> {
            CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = false;
            x.getSource().sendFeedback(Component.translatable("scene.interpolation.hide_active"));
            return 0;
        }).then(ClientCommandManager.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = false;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.hide", interpolation));
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = false;
                x.getSource().sendFeedback(Component.translatable("scene.interpolation.hide_all"));
                CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = false;
            }
            return 0;
        }))).then(ClientCommandManager.literal("list").executes((x) -> {
            if (CMDCamClient.serverAvailable) {
                x.getSource().sendError(Component.translatable("scenes.list_fail"));
                return 0;
            }
            x.getSource().sendFeedback(Component.translatable("scenes.list", SCENES.size(), String.join(", ", SCENES.keySet())));
            return 0;
        })).then(ClientCommandManager.literal("load").then(ClientCommandManager.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            if (CMDCamClient.serverAvailable)
                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
            else {
                CamScene scene = CMDCamClient.SCENES.get(pathArg);
                if (scene != null) {
                    set(scene);
                    x.getSource().sendFeedback(Component.translatable("scenes.load", pathArg));
                } else
                    x.getSource().sendError(Component.translatable("scenes.load_fail", pathArg));
            }
            return 0;
        }))).then(ClientCommandManager.literal("save").then(ClientCommandManager.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            try {
                CamScene scene = CMDCamClient.createScene();
                
                if (CMDCamClient.serverAvailable)
                    CMDCam.NETWORK.sendToServer(new SetPathPacket(pathArg, scene));
                else {
                    CMDCamClient.SCENES.put(pathArg, scene);
                    x.getSource().sendFeedback(Component.translatable("scenes.save", pathArg));
                }
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            return 0;
        }))).then(new ClientPointArgumentBuilder("follow_center", (x, y) -> targetMarker = y, PROCESSOR_CLIENT).executes(x -> {
            targetMarker = CamPoint.createLocal();
            return 0;
        })));
        
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
    
    public static void gameTickPath(Level level) {
        playing.gameTick(level);
    }
    
    public static void renderTickPath(Level level, float renderTickTime) {
        playing.renderTick(level, renderTickTime);
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
        
        CamEventHandlerClient.roll((float) point.roll);
        var partialTick = mc.isPaused() ? ((MinecraftAccessor) mc).getPausePartialTick() : ((MinecraftAccessor) mc).getTimer().partialTick;
        CamEventHandlerClient.fov(point.zoom - CamEventHandlerClient.fovExactVanilla(partialTick));
        mc.player.absMoveTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.absMoveTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
}
