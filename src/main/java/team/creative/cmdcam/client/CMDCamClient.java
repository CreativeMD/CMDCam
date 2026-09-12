package team.creative.cmdcam.client;

import java.util.HashMap;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import team.creative.cmdcam.CMDCam;
import team.creative.cmdcam.client.mixin.HudAccessor;
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
import team.creative.creativecore.common.util.mc.TickUtils;

public class CMDCamClient {
    
    public static final CamCommandProcessorClient PROCESSOR = new CamCommandProcessorClient();
    public static final HashMap<String, CamScene> SCENES = new HashMap<>();
    
    private static CamScene scene = CamScene.createDefault();
    private static CamScene playing;
    private static boolean serverAvailable = false;
    private static boolean hideGuiCache;
    private static CamPoint targetMarker;
    
    public static CamPoint createLocalPoint() {
        Minecraft mc = Minecraft.getInstance();
        float partialTicks = TickUtils.getFrameTime(mc.level);
        Vec3 vec = mc.player.getEyePosition(partialTicks);
        return new CamPoint(vec.x, vec.y, vec.z, mc.player.getViewYRot(partialTicks), mc.player.getViewXRot(partialTicks), CamEventHandlerClient.roll(), CamEventHandlerClient
                .fovExact(partialTicks));
    }
    
    public static void resetServerAvailability() {
        serverAvailable = false;
    }
    
    public static void setServerAvailability() {
        serverAvailable = true;
    }
    
    public static void init(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(new CamEventHandlerClient());
        CreativeCoreClient.registerClientConfig(CMDCam.MODID);
    }
    
    public static void load(IEventBus bus) {
        bus.addListener(CMDCamClient::init);
        NeoForge.EVENT_BUS.addListener(CMDCamClient::commands);
        NeoForge.EVENT_BUS.addListener(CMDCamClient::login);
        bus.addListener(KeyHandler::registerKeys);
        bus.addListener(CMDCamClient::layers);
    }
    
    private static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.TITLE, Identifier.fromNamespaceAndPath(CMDCam.MODID, VanillaGuiLayers.TITLE.getNamespace()), (graphics, tracker) -> {
            if (CMDCamClient.isPlaying())
                ((HudAccessor) Minecraft.getInstance().gui.hud).callExtractTitle(graphics, tracker);
        });
    }
    
    private static void commands(RegisterClientCommandsEvent event) {
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
        })).then(Commands.literal("show").executes(x -> {
            CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = true;
            x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.show_active"), false);
            return 0;
        }).then(Commands.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = true;
                x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.show", interpolation), false);
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = true;
                x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.show_all"), false);
            }
            return 0;
        }))).then(Commands.literal("hide").executes(x -> {
            CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = false;
            x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.hide_active"), false);
            return 0;
        }).then(Commands.argument("interpolation", InterpolationArgument.interpolationAll()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            if (!interpolation.equalsIgnoreCase("all")) {
                CamInterpolation.REGISTRY.get(interpolation).isRenderingEnabled = false;
                x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.hide", interpolation), false);
            } else {
                for (CamInterpolation movement : CamInterpolation.REGISTRY.values())
                    movement.isRenderingEnabled = false;
                x.getSource().sendSuccess(() -> Component.translatable("scene.interpolation.hide_all"), false);
                CamEventHandlerClient.SHOW_ACTIVE_INTERPOLATION = false;
            }
            return 0;
        }))).then(Commands.literal("list").executes((x) -> {
            if (CMDCamClient.serverAvailable) {
                x.getSource().sendFailure(Component.translatable("scenes.list_fail"));
                return 0;
            }
            x.getSource().sendSuccess(() -> Component.translatable("scenes.list", SCENES.size(), String.join(", ", SCENES.keySet())), true);
            return 0;
        })).then(Commands.literal("load").then(Commands.argument("path", StringArgumentType.string()).executes((x) -> {
            String pathArg = StringArgumentType.getString(x, "path");
            if (CMDCamClient.serverAvailable)
                CMDCam.NETWORK.sendToServer(new GetPathPacket(pathArg));
            else {
                CamScene scene = CMDCamClient.SCENES.get(pathArg);
                if (scene != null) {
                    set(scene);
                    x.getSource().sendSuccess(() -> Component.translatable("scenes.load", pathArg), false);
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
                    x.getSource().sendSuccess(() -> Component.translatable("scenes.save", pathArg), false);
                }
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            return 0;
        }))).then(new PointArgumentBuilder("follow_center", (x, y) -> targetMarker = y, PROCESSOR, true).executes(x -> {
            targetMarker = createLocalPoint();
            return 0;
        })));
        
    }
    
    private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        scene = CamScene.createDefault();
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
        if (scene.posTarget != null && targetMarker == null)
            targetMarker = createLocalPoint();
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
        var hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hideGuiCache)
            hud.toggle();
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
        playing.finish(Minecraft.getInstance().level);
        playing = null;
        var hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hideGuiCache)
            hud.toggle();
    }
    
    public static void stopServer() {
        if (playing == null)
            return;
        playing.finish(Minecraft.getInstance().level);
        playing = null;
        var hud = Minecraft.getInstance().gui.hud;
        if (hud.isHidden() != hideGuiCache)
            hud.toggle();
    }
    
    public static void noTickPath(Level level, float renderTickTime) {
        hideGuiCache = Minecraft.getInstance().gui.hud.isHidden();
    }
    
    public static void gameTickPath(Level level) {
        playing.gameTick(level);
    }
    
    public static void renderTickPath(Level level, float renderTickTime) {
        playing.renderTick(level, renderTickTime);
        if (!playing.playing()) {
            var hud = Minecraft.getInstance().gui.hud;
            if (hud.isHidden() != hideGuiCache)
                hud.toggle();
            playing = null;
        }
    }
    
    public static void resetTargetMarker() {
        targetMarker = null;
    }
    
    public static boolean hasTargetMarker() {
        return targetMarker != null && scene.posTarget != null;
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
        CamEventHandlerClient.fov(point.zoom - CamEventHandlerClient.fovExactVanilla(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
        mc.player.absSnapTo(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
        mc.player.absSnapTo(point.x, point.y - mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
    }
    
}
