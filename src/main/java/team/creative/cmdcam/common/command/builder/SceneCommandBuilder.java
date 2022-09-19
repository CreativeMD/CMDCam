package team.creative.cmdcam.common.command.builder;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TranslatableComponent;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.command.argument.CamModeArgument;
import team.creative.cmdcam.common.command.argument.CamPitchModeArgument;
import team.creative.cmdcam.common.command.argument.DurationArgument;
import team.creative.cmdcam.common.command.argument.InterpolationArgument;
import team.creative.cmdcam.common.math.interpolation.CamInterpolation;
import team.creative.cmdcam.common.math.interpolation.CamPitchMode;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

public class SceneCommandBuilder {
    
    public static void scene(ArgumentBuilder<CommandSourceStack, ?> origin, CamCommandProcessor processor) {
        ArgumentBuilder<CommandSourceStack, ?> original = origin;
        
        if (processor.requiresSceneName())
            origin = Commands.argument("name", StringArgumentType.string());
        
        origin.then(Commands.literal("clear").executes((x) -> {
            processor.getScene(x).points.clear();
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.clear"), false);
            return 0;
        }));
        
        origin.then(new PointArgumentBuilder("add", (x, point) -> {
            processor.getScene(x).points.add(point);
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.add", processor.getScene(x).points.size()), false);
        }, processor));
        
        origin.then(new PointArgumentBuilder("insert", (x, point, index) -> {
            processor.getScene(x).points.add(index, point);
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.insert", index), false);
        }, processor));
        
        origin.then(new PointArgumentBuilder("set", (x, point, index) -> {
            processor.getScene(x).points.set(index, point);
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.set", index), false);
        }, processor));
        
        origin.then(Commands.literal("remove").then(Commands.argument("index", IntegerArgumentType.integer()).executes((x) -> {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                scene.points.remove(index);
            else
                x.getSource().sendFailure(new TranslatableComponent("scene.index", index + 1));
            processor.markDirty(x);
            return 0;
        })));
        
        origin.then(Commands.literal("duration").then(Commands.argument("duration", DurationArgument.duration()).executes(x -> {
            long duration = DurationArgument.getDuration(x, "duration");
            if (duration > 0)
                processor.getScene(x).duration = duration;
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.duration", duration), false);
            return 0;
        })));
        
        origin.then(Commands.literal("loops").then(Commands.argument("loop", IntegerArgumentType.integer(-1)).executes(x -> {
            int loop = IntegerArgumentType.getInteger(x, "loop");
            processor.getScene(x).loop = loop;
            processor.markDirty(x);
            if (loop == 0)
                x.getSource().sendSuccess(new TranslatableComponent("scene.add", processor.getScene(x).points.size()), false);
            else if (loop < 0)
                x.getSource().sendSuccess(new TranslatableComponent("scene.loops.endless"), false);
            else
                x.getSource().sendSuccess(new TranslatableComponent("scene.loops", loop), false);
            return 0;
        })));
        
        ArgumentBuilder<CommandSourceStack, ?> tpO = Commands.literal("goto");
        ArgumentBuilder<CommandSourceStack, ?> tp = tpO;
        if (processor.requiresPlayer())
            tp = Commands.argument("players", EntityArgument.players());
        
        tp.then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(x -> {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                processor.teleport(x, index);
            else
                x.getSource().sendFailure(new TranslatableComponent("scene.index", index + 1));
            return 0;
        }));
        if (processor.requiresPlayer())
            origin.then(tpO.then(tp));
        else
            origin.then(tpO);
        
        origin.then(Commands.literal("mode").then(Commands.argument("mode", CamModeArgument.mode()).executes(x -> {
            processor.getScene(x).setMode(StringArgumentType.getString(x, "mode"));
            return 0;
        })));
        
        origin.then(new TargetArgumentBuilder("target", true, processor));
        origin.then(new TargetArgumentBuilder("follow", false, processor));
        
        origin.then(new FollowArgumentBuilder(CamAttribute.PITCH, processor)).then(new FollowArgumentBuilder(CamAttribute.YAW, processor))
                .then(new FollowArgumentBuilder(CamAttribute.POSITION, processor));
        
        origin.then(Commands.literal("interpolation").then(Commands.argument("interpolation", InterpolationArgument.interpolation()).executes((x) -> {
            String interpolation = StringArgumentType.getString(x, "interpolation");
            processor.getScene(x).interpolation = CamInterpolation.REGISTRY.get(interpolation);
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.interpolation", interpolation), false);
            return 0;
        })));
        
        origin.then(Commands.literal("smooth_start").then(Commands.argument("value", BoolArgumentType.bool()).executes((x) -> {
            boolean value = BoolArgumentType.getBool(x, "value");
            processor.getScene(x).smoothBeginning = value;
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.smooth_beginning", value), false);
            return 0;
        })));
        
        origin.then(Commands.literal("spinning_fix").then(Commands.argument("mode", CamPitchModeArgument.pitchMode()).executes((x) -> {
            CamPitchMode mode = CamPitchModeArgument.getMode(x, "mode");
            processor.getScene(x).pitchMode = mode;
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.pitch_mode", mode), false);
            return 0;
        })));
        
        origin.then(Commands.literal("distance_timing").then(Commands.argument("value", BoolArgumentType.bool()).executes((x) -> {
            boolean value = BoolArgumentType.getBool(x, "value");
            processor.getScene(x).distanceBasedTiming = value;
            processor.markDirty(x);
            x.getSource().sendSuccess(new TranslatableComponent("scene.distance_timing", value), false);
            return 0;
        })));
        
        if (processor.requiresSceneName())
            original.then(origin);
        
    }
    
}
