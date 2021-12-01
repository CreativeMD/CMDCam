package team.creative.cmdcam;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.network.chat.TextComponent;
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
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.argument.TargetArgument;
import team.creative.cmdcam.common.packet.ConnectPacket;
import team.creative.cmdcam.common.packet.GetPathPacket;
import team.creative.cmdcam.common.packet.SetPathPacket;
import team.creative.cmdcam.common.packet.StartPathPacket;
import team.creative.cmdcam.common.packet.StopPathPacket;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.cmdcam.server.CamEventHandler;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;

@Mod(value = CMDCam.MODID)
public class CMDCam {
    
    public static final String MODID = "cmdcam";
    
    private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork("1.0", LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
    
    public CMDCam() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FMLJavaModLoadingContext.get().getModEventBus().addListener(this::client));
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
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
        
        ArgumentTypes.register("duration", DurationArgument.class, new EmptyArgumentSerializer<>(() -> DurationArgument.duration()));
        ArgumentTypes.register("cameramode", CamModeArgument.class, new EmptyArgumentSerializer<>(() -> CamModeArgument.mode()));
        ArgumentTypes.register("cameratarget", TargetArgument.class, new EmptyArgumentSerializer<>(() -> TargetArgument.target()));
        ArgumentTypes.register("interpolation", InterpolationArgument.class, new EmptyArgumentSerializer<>(() -> InterpolationArgument.interpolation()));
        ArgumentTypes.register("allinterpolation", AllInterpolationArgument.class, new EmptyArgumentSerializer<>(() -> InterpolationArgument.interpolationAll()));
        
        MinecraftForge.EVENT_BUS.register(new CamEventHandler());
    }
    
    private void serverStarting(final ServerStartingEvent event) {
        event.getServer().getCommands().getDispatcher().register(Commands.literal("cam-server").executes((x) -> {
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server start <player> <path> [time|ms|s|m|h|d] [loops (-1 -> endless)] " + ChatFormatting.RED + "starts the animation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server stop <player> " + ChatFormatting.RED + "stops the animation"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server list " + ChatFormatting.RED + "lists all saved paths"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server remove <name> " + ChatFormatting.RED + "removes the given path"), false);
            x.getSource()
                    .sendSuccess(new TextComponent("" + ChatFormatting.BOLD + ChatFormatting.YELLOW + "/cam-server clear " + ChatFormatting.RED + "clears all saved paths"), false);
            return 0;
        }).then(Commands.literal("start").then(Commands.argument("players", EntityArgument.players()).then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(x, "players");
            if (players.isEmpty())
                return 0;
            String pathName = StringArgumentType.getString(x, "name");
            CamPath path = CMDCamServer.getPath(x.getSource().getLevel(), pathName);
            if (path != null) {
                CreativePacket packet = new StartPathPacket(path);
                for (ServerPlayer player : players)
                    CMDCam.NETWORK.sendToClient(packet, player);
            } else
                x.getSource().sendSuccess(new TextComponent("Path '" + pathName + "' could not be found!"), true);
            return 0;
        }).then(Commands.argument("duration", DurationArgument.duration()).executes((x) -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(x, "players");
            if (players.isEmpty())
                return 0;
            String pathName = StringArgumentType.getString(x, "name");
            CamPath path = CMDCamServer.getPath(x.getSource().getLevel(), pathName);
            if (path != null) {
                path = path.copy();
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    path.duration = duration;
                CreativePacket packet = new StartPathPacket(path);
                for (ServerPlayer player : players)
                    CMDCam.NETWORK.sendToClient(packet, player);
            } else
                x.getSource().sendSuccess(new TextComponent("Path '" + pathName + "' could not be found!"), true);
            return 0;
        }).then(Commands.argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(x, "players");
            if (players.isEmpty())
                return 0;
            String pathName = StringArgumentType.getString(x, "name");
            CamPath path = CMDCamServer.getPath(x.getSource().getLevel(), pathName);
            if (path != null) {
                path = path.copy();
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    path.duration = duration;
                
                path.loop = IntegerArgumentType.getInteger(x, "loop");
                CreativePacket packet = new StartPathPacket(path);
                for (ServerPlayer player : players)
                    CMDCam.NETWORK.sendToClient(packet, player);
            } else
                x.getSource().sendSuccess(new TextComponent("Path '" + pathName + "' could not be found!"), true);
            return 0;
        })))))).then(Commands.literal("stop").then(Commands.argument("players", EntityArgument.players()).executes((x) -> {
            CreativePacket packet = new StopPathPacket();
            for (ServerPlayer player : EntityArgument.getPlayers(x, "players"))
                CMDCam.NETWORK.sendToClient(packet, player);
            return 0;
        }))).then(Commands.literal("list").executes((x) -> {
            Collection<String> names = CMDCamServer.getSavedPaths(x.getSource().getLevel());
            String output = "There are " + names.size() + " path(s) in total. ";
            for (String key : names) {
                output += key + ", ";
            }
            x.getSource().sendSuccess(new TextComponent(output), true);
            return 0;
        })).then(Commands.literal("clear").executes((x) -> {
            CMDCamServer.clearPaths(x.getSource().getLevel());
            x.getSource().sendSuccess(new TextComponent("Removed all existing paths (in this world)!"), true);
            return 0;
        })).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String path = StringArgumentType.getString(x, "name");
            if (CMDCamServer.removePath(x.getSource().getLevel(), path))
                x.getSource().sendSuccess(new TextComponent("Path '" + path + "' has been removed!"), true);
            else
                x.getSource().sendSuccess(new TextComponent("Path '" + path + "' could not be found!"), true);
            return 0;
        }))));
    }
}
