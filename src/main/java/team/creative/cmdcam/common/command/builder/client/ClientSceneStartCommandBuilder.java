package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.client.ClientCamCommandProcessor;
import team.creative.cmdcam.common.scene.CamScene;

public class ClientSceneStartCommandBuilder {

    public static void start(ArgumentBuilder<FabricClientCommandSource, ?> origin, ClientCamCommandProcessor processor) {
        ArgumentBuilder<FabricClientCommandSource, ?> startO = ClientCommandManager.literal("start");
        ArgumentBuilder<FabricClientCommandSource, ?> start = startO;

        if (processor.requiresPlayer())
            start = ClientCommandManager.argument("players", EntityArgument.players());
        else if (processor.requiresSceneName())
            start = ClientCommandManager.argument("name", StringArgumentType.string());

        start.executes((x) -> {
            try {
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            return 0;
        }).then(ClientCommandManager.argument("duration", DurationArgument.duration()).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    processor.getScene(x).duration = duration;
                processor.markDirty(x);
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            return 0;
        }).then(ClientCommandManager.argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
            try {
                CamScene scene = processor.getScene(x);
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    scene.duration = duration;
                scene.loop = IntegerArgumentType.getInteger(x, "loop");
                processor.markDirty(x);
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            return 0;
        })));

        if (processor.requiresSceneName())
            origin.then(startO.then(ClientCommandManager.argument("name", StringArgumentType.string()).then(start)));
        else {
            if (processor.requiresPlayer())
                origin.then(startO.then(start));
            else
                origin.then(startO);
        }
    }
}
