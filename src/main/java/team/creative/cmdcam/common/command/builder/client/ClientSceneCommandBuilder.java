package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.command.argument.*;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

public class ClientSceneCommandBuilder {

    public static void scene(ArgumentBuilder<FabricClientCommandSource, ?> origin, CamCommandProcessor<FabricClientCommandSource> processor) {
        ArgumentBuilder<FabricClientCommandSource, ?> original = origin;

        if (processor.requiresSceneName())
            origin = ClientCommandManager.argument("name", StringArgumentType.string());

        origin.then(ClientCommandManager.literal("clear").executes((x) -> {
            processor.getScene(x).points.clear();
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.clear"));
            return 0;
        }));

        origin.then(new ClientPointArgumentBuilder("add", (x, point) -> {
            processor.getScene(x).points.add(point);
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.add", processor.getScene(x).points.size()));
        }, processor));

        origin.then(new ClientPointArgumentBuilder("insert", (x, point, index) -> {
            processor.getScene(x).points.add(index, point);
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.insert", index + 1));
        }, processor));

        origin.then(new ClientPointArgumentBuilder("set", (x, point, index) -> {
            processor.getScene(x).points.set(index, point);
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.set", index + 1));
        }, processor));

        origin.then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("index", IntegerArgumentType.integer()).executes((x) -> {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                scene.points.remove(index);
            else
                x.getSource().sendError(Component.translatable("scene.index", index + 1));
            processor.markDirty(x);
            return 0;
        })));

        origin.then(ClientCommandManager.literal("duration").then(ClientCommandManager.argument("duration", DurationArgument.duration()).executes(x -> {
            long duration = DurationArgument.getDuration(x, "duration");
            if (duration > 0)
                processor.getScene(x).duration = duration;
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.duration", duration));
            return 0;
        })));

        origin.then(ClientCommandManager.literal("loops").then(ClientCommandManager.argument("loop", IntegerArgumentType.integer(-1)).executes(x -> {
            int loop = IntegerArgumentType.getInteger(x, "loop");
            processor.getScene(x).loop = loop;
            processor.markDirty(x);
            if (loop == 0)
                x.getSource().sendFeedback(Component.translatable("scene.add", processor.getScene(x).points.size()));
            else if (loop < 0)
                x.getSource().sendFeedback(Component.translatable("scene.loops.endless"));
            else
                x.getSource().sendFeedback(Component.translatable("scene.loops", loop));
            return 0;
        })));

        ArgumentBuilder<FabricClientCommandSource, ?> tpO = ClientCommandManager.literal("goto");
        ArgumentBuilder<FabricClientCommandSource, ?> tp = tpO;
        if (processor.requiresPlayer())
            tp = ClientCommandManager.argument("players", EntityArgument.players());

        tp.then(ClientCommandManager.argument("index", IntegerArgumentType.integer(0)).executes(x -> {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                processor.teleport(x, index);
            else
                x.getSource().sendError(Component.translatable("scene.index", index + 1));
            return 0;
        }));
        if (processor.requiresPlayer())
            origin.then(tpO.then(tp));
        else
            origin.then(tpO);

        origin.then(ClientCommandManager.literal("mode").then(ClientCommandManager.argument("mode", CamModeArgument.mode()).executes(x -> {
            processor.getScene(x).setMode(StringArgumentType.getString(x, "mode"));
            return 0;
        })));

        origin.then(new ClientTargetArgumentBuilder("target", true, processor));
        origin.then(new ClientTargetArgumentBuilder("follow", false, processor));

        origin.then(new ClientFollowArgumentBuilder(CamAttribute.PITCH, processor)).then(new ClientFollowArgumentBuilder(CamAttribute.YAW, processor)).then(
            new ClientFollowArgumentBuilder(CamAttribute.POSITION, processor));

        origin.then(ClientCommandManager.literal("interpolation").then(ClientCommandManager.argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            processor.getScene(x).interpolation = CamInterpolation.REGISTRY.get(interpolation);
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.interpolation", interpolation));
            return 0;
        })));

        origin.then(ClientCommandManager.literal("spinning_fix").then(ClientCommandManager.argument("mode", CamPitchModeArgument.pitchMode()).executes((x) -> {
            CamPitchMode mode = CamPitchModeArgument.getMode(x, "mode");
            processor.getScene(x).pitchMode = mode;
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.pitch_mode", mode));
            return 0;
        })));

        origin.then(ClientCommandManager.literal("distance_timing").then(ClientCommandManager.argument("value", BoolArgumentType.bool()).executes((x) -> {
            boolean value = BoolArgumentType.getBool(x, "value");
            processor.getScene(x).distanceBasedTiming = value;
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable("scene.distance_timing", value));
            return 0;
        })));

        origin.then(ClientCommandManager.literal("smooth_start")
                .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                        .executes((context) -> setSmoothStart(BoolArgumentType.getBool(context, "value"), "default", processor, context))
                        .then(ClientCommandManager.argument("mode", new SceneSwitchArgument())
                                .executes((context) -> setSmoothStart(BoolArgumentType.getBool(context, "value"),
                                        StringArgumentType.getString(context, "mode"), processor, context))
                        )
                )
        );

        origin.then(ClientCommandManager.literal("reset_scenes").executes((x) -> {
            processor.markDirty(x);
            CMDCamClient.resetScenes();
            x.getSource().sendFeedback(Component.translatable("scene.reset"));
            return 0;
        }));

        origin.then(ClientCommandManager.literal("save_scenes")
                .executes((x) -> saveScenes("default", x))
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .executes((x) -> saveScenes(StringArgumentType.getString(x, "name"), x))));

        origin.then(ClientCommandManager.literal("load_scenes")
                .executes((x) -> loadScenes("default", x))
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .executes((x) -> loadScenes(StringArgumentType.getString(x, "name"), x))));

        if (processor.requiresSceneName())
            original.then(origin);

    }

    public static int saveScenes(String name, CommandContext<FabricClientCommandSource> context) {
        if (name == null || name.isBlank()) {
            name = "default";
        }

        if (!CMDCamClient.saveScenes(name)) {
            context.getSource().sendError(Component.translatable("scene.save.error"));
            return -1;
        }

        context.getSource().sendFeedback(Component.translatable("scene.save.success"));
        return 0;
    }

    public static int loadScenes(String name, CommandContext<FabricClientCommandSource> context) {
        if (name == null || name.isBlank()) {
            name = "default";
        }

        if (!CMDCamClient.loadScenes(name)) {
            context.getSource().sendError(Component.translatable("scene.load.error"));
            return -1;
        }

        context.getSource().sendFeedback(Component.translatable("scene.load.success"));
        return 0;
    }

    public static int setSmoothStart(boolean value, String mode, CamCommandProcessor<FabricClientCommandSource> processor, CommandContext<FabricClientCommandSource> context) {
        if (mode.equals("default")) {
            processor.getScene(context).smoothBeginning = value;
        } else if (mode.equals("all")) {
            CMDCamClient.setSmoothStart(value);
        } else {
            CMDCamClient.setSmoothStart(value, Integer.parseInt(mode));
        }

        processor.markDirty(context);
        context.getSource().sendFeedback(Component.translatable("scene.smooth_beginning", value));
        return 0;
    }
}
