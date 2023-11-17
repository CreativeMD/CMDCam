package team.creative.cmdcam;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.creative.cmdcam.client.mixin.ArgumentTypeInfosAccessor;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.CamPitchModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument.AllInterpolationArgument;
import team.creative.cmdcam.common.command.builder.SceneCommandBuilder;
import team.creative.cmdcam.common.command.builder.SceneStartCommandBuilder;
import team.creative.cmdcam.common.packet.*;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.server.CMDCamServer;
import team.creative.cmdcam.server.CamEventHandler;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.network.CreativePacket;

import java.util.Collection;
import java.util.function.Supplier;

@Mod(value = CMDCam.MODID)
public class CMDCam implements ModInitializer {
    
    public static final String MODID = "cmdcam";
    
    private static final Logger LOGGER = LogManager.getLogger(CMDCam.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork(1, LOGGER, new ResourceLocation(CMDCam.MODID, "main"));
    public static final CMDCamConfig CONFIG = new CMDCamConfig();
    
    private void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> camServer = Commands.literal("cam-server");
        
        SceneStartCommandBuilder.start(camServer, CMDCamServer.PROCESSOR);
        
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal("get");
        SceneCommandBuilder.scene(get, CMDCamServer.PROCESSOR);
        camServer.then(get);
        
        dispatcher.register(camServer.then(Commands.literal("stop").then(Commands.argument("players", EntityArgument.players()).executes(x -> {
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
            x.getSource().sendSystemMessage(Component.translatable("scenes.list", names.size(), String.join(", ", names)));
            return 0;
        })).then(Commands.literal("clear").executes((x) -> {
            CMDCamServer.clearPaths(x.getSource().getLevel());
            x.getSource().sendSuccess(() -> Component.translatable("scenes.clear"), true);
            return 0;
        })).then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.removePath(x.getSource().getLevel(), name))
                x.getSource().sendSuccess(() -> Component.translatable("scene.remove", name), true);
            else
                x.getSource().sendFailure(Component.translatable("scene.remove_fail", name));
            return 0;
        }))).then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.string()).executes((x) -> {
            String name = StringArgumentType.getString(x, "name");
            if (CMDCamServer.get(x.getSource().getLevel(), name) != null)
                x.getSource().sendSuccess(() -> Component.translatable("scene.exists", name), true);
            else {
                CMDCamServer.set(x.getSource().getLevel(), name, CamScene.createDefault());
                x.getSource().sendSuccess(() -> Component.translatable("scene.create", name), true);
            }
            return 0;
        }))));
    }

    public static synchronized <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerByClass(Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeInfosAccessor.getByClass().put(infoClass, argumentTypeInfo);
        return argumentTypeInfo;
    }

    private <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> void register(String id, Supplier<I> supplier) {
        Registry.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, new ResourceLocation(MODID, id), supplier.get());
    }

    @Override
    public void onInitialize() {
        //DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CMDCamClient.load(FMLJavaModLoadingContext.get().getModEventBus()));

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            commands(dispatcher);
        }));

        register("duration", () -> registerByClass(DurationArgument.class, SingletonArgumentInfo.<DurationArgument>contextFree(
                DurationArgument::duration)));
        register("cam_mode", () -> registerByClass(CamModeArgument.class, SingletonArgumentInfo.<CamModeArgument>contextFree(
                CamModeArgument::mode)));
        register("interpolation", () -> registerByClass(InterpolationArgument.class, SingletonArgumentInfo
                .<InterpolationArgument>contextFree(InterpolationArgument::interpolation)));
        register("all_interpolation", () -> registerByClass(AllInterpolationArgument.class, SingletonArgumentInfo
                .<AllInterpolationArgument>contextFree(InterpolationArgument::interpolationAll)));
        register("pitch_mode", () -> registerByClass(CamPitchModeArgument.class, SingletonArgumentInfo.<CamPitchModeArgument>contextFree(
                CamPitchModeArgument::pitchMode)));

        NETWORK.registerType(ConnectPacket.class, ConnectPacket::new);
        NETWORK.registerType(GetPathPacket.class, GetPathPacket::new);
        NETWORK.registerType(SetPathPacket.class, SetPathPacket::new);
        NETWORK.registerType(StartPathPacket.class, StartPathPacket::new);
        NETWORK.registerType(StopPathPacket.class, StopPathPacket::new);
        NETWORK.registerType(TeleportPathPacket.class, TeleportPathPacket::new);
        NETWORK.registerType(PausePathPacket.class, PausePathPacket::new);
        NETWORK.registerType(ResumePathPacket.class, ResumePathPacket::new);

        new CamEventHandler();

        CreativeConfigRegistry.ROOT.registerValue(MODID, CONFIG);
    }
}
