package team.creative.cmdcam.common.command.builder;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.scene.CamScene;

public class SceneStartCommandBuilder {
    
    public static void start(ArgumentBuilder<CommandSourceStack, ?> origin, CamCommandProcessor processor) {
        ArgumentBuilder<CommandSourceStack, ?> startO = Commands.literal("start");
        ArgumentBuilder<CommandSourceStack, ?> start = startO;
        
        if (processor.requiresPlayer())
            start = Commands.argument("players", EntityArgument.players());
        else if (processor.requiresSceneName())
            start = Commands.argument("name", StringArgumentType.string());
        
        start.executes((x) -> {
            try {
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            return 0;
        }).then(Commands.argument("duration", DurationArgument.duration()).executes((x) -> {
            try {
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    processor.getScene(x).duration = duration;
                processor.markDirty(x);
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            return 0;
        }).then(Commands.argument("loop", IntegerArgumentType.integer(-1)).executes((x) -> {
            try {
                CamScene scene = processor.getScene(x);
                long duration = DurationArgument.getDuration(x, "duration");
                if (duration > 0)
                    scene.duration = duration;
                scene.loop = IntegerArgumentType.getInteger(x, "loop");
                processor.markDirty(x);
                processor.start(x);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            return 0;
        })));
        
        if (processor.requiresSceneName())
            origin.then(startO.then(Commands.argument("name", StringArgumentType.string()).then(start)));
        else {
            if (processor.requiresPlayer())
                origin.then(startO.then(start));
            else
                origin.then(startO);
        }
    }
    
}
