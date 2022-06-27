package team.creative.cmdcam;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.registries.DeferredRegister;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.CamPitchModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.SceneStartCommandBuilder;
import team.creative.cmdcam.common.packet.ConnectPacket;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.PausePathPacket;
import team.creative.cmdcam.common.packet.ResumePathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.packet.StartPathPacket;
import team.creative.cmdcam.common.packet.StopPathPacket;
import team.creative.cmdcam.common.packet.TeleportPathPacket;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.cmdcam.server.CamEventHandler;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;

@Mod(value = CMDCam.MODID)
public class CMDCam {
    
    public static final String MODID = "cmdcam";
    
    private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork("1.0", LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
    public static final CMDCamConfig CONFIG = new CMDCamConfig();
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registry.COMMAND_ARGUMENT_TYPE_REGISTRY, MODID);
    
    public CMDCam() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(this::client));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.addListener(CMDCamClient::commands));
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
        
        COMMAND_ARGUMENT_TYPES.register(FMLJavaModLoadingContext.get().getModEventBus());
        COMMAND_ARGUMENT_TYPES.register("duration", () -> ArgumentTypeInfos
                .registerByClass(DurationArgument.class, SingletonArgumentInfo.<DurationArgument>contextFree(() -> DurationArgument.duration())));
        COMMAND_ARGUMENT_TYPES.register("cam_mode", () -> ArgumentTypeInfos
                .registerByClass(CamModeArgument.class, SingletonArgumentInfo.<CamModeArgument>contextFree(() -> CamModeArgument.mode())));
        COMMAND_ARGUMENT_TYPES.register("interpolation", () -> ArgumentTypeInfos
                .registerByClass(InterpolationArgument.class, SingletonArgumentInfo.<InterpolationArgument>contextFree(() -> InterpolationArgument.interpolation())));
        COMMAND_ARGUMENT_TYPES.register("all_interpolation", () -> ArgumentTypeInfos
                .registerByClass(AllInterpolationArgument.class, SingletonArgumentInfo.<AllInterpolationArgument>contextFree(() -> InterpolationArgument.interpolationAll())));
        COMMAND_ARGUMENT_TYPES.register("pitch_mode", () -> ArgumentTypeInfos
                .registerByClass(CamPitchModeArgument.class, SingletonArgumentInfo.<CamPitchModeArgument>contextFree(() -> CamPitchModeArgument.pitchMode())));
    }
    
    @OnlyIn(value = Dist.CLIENT)
    private void client(final FMLClientSetupEvent event) {
        ModLoadingContext.get()
                .registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        CMDCamClient.init(event);
    }
    
    private void init(final FMLCommonSetupEvent event) {
        NETWORK.registerType(ConnectPacket.class, ConnectPacket::new);
        NETWORK.registerType(GetPathPacket.class, GetPathPacket::new);
        NETWORK.registerType(SetPathPacket.class, SetPathPacket::new);
        NETWORK.registerType(StartPathPacket.class, StartPathPacket::new);
        NETWORK.registerType(StopPathPacket.class, StopPathPacket::new);
        NETWORK.registerType(TeleportPathPacket.class, TeleportPathPacket::new);
        NETWORK.registerType(PausePathPacket.class, PausePathPacket::new);
        NETWORK.registerType(ResumePathPacket.class, ResumePathPacket::new);
        
        MinecraftForge.EVENT_BUS.register(new CamEventHandler());
        
        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG);
    }
    
    private void serverStarting(final ServerStartingEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> camServer = Commands.literal("cam-server");
        
        SceneStartCommandBuilder.start(camServer, CMDCamServer.PROCESSOR);
        
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get");
        SceneCommandBuilder.scene(get, CMDCamServer.PROCESSOR);
        camServer.then(get);
        
        event.getServer().getCommands().getDispatcher().register(camServer.then(Commands.literal("stop").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new StopPathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("pause").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new PausePathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("resume").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
            CreativePacket packet = new ResumePathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("list").executes((x) -> {
            Collection<String> names = CMDCamServer.getSavedPaths(x.getSource().getLevel());
            x.getSource().sendSuccess(Component.translatable("scenes.list", names.size(), String.join(", ", names)), true);
            return 0;
        })).then(Commands.literal("clear").executes((x) -> {
            CMDCamServer.clearPaths(x.getSource().getLevel());
            x.getSource().sendSuccess(Component.translatable("scenes.clear"), true);
            return 0;
        })).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.removePath(x.getSource().getLevel(), name))
                x.getSource().sendSuccess(Component.translatable("scene.remove", name), true);
            else
                x.getSource().sendFailure(Component.translatable("scene.remove_fail", name));
            return 0;
        }))).then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.get(x.getSource().getLevel(), name) != null)
                x.getSource().sendSuccess(Component.translatable("scene.exists", name), true);
            else {
                CMDCamServer.set(x.getSource().getLevel(), name, CamScene.createDefault());
                x.getSource().sendSuccess(Component.translatable("scene.create", name), true);
            }
            return 0;
        }))));
    }
}
