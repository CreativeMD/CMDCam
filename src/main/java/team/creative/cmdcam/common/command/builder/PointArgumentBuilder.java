package team.creative.cmdcam.common.command.builder;

import java.util.function.BiConsumer;

import org.apache.logging.log4j.util.TriConsumer;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;

public class PointArgumentBuilder extends ArgumentBuilder<CommandSourceStack, PointArgumentBuilder> {
    
    private final String literal;
    private final TriConsumer<CommandContext<CommandSourceStack>, CamPoint, Integer> indexConsumer;
    private final BiConsumer<CommandContext<CommandSourceStack>, CamPoint> consumer;
    private final CamCommandProcessor processor;
    
    public PointArgumentBuilder(final String literal, TriConsumer<CommandContext<CommandSourceStack>, CamPoint, Integer> consumer, CamCommandProcessor processor) {
        this.literal = literal;
        this.indexConsumer = consumer;
        this.consumer = null;
        this.processor = processor;
    }
    
    public PointArgumentBuilder(final String literal, BiConsumer<CommandContext<CommandSourceStack>, CamPoint> consumer, CamCommandProcessor processor) {
        this.literal = literal;
        this.indexConsumer = null;
        this.consumer = consumer;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected PointArgumentBuilder getThis() {
        return this;
    }
    
    private void processPoint(CommandContext<CommandSourceStack> x, CamPoint point) {
        
        if (indexConsumer != null) {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                indexConsumer.accept(x, point, index);
            else
                x.getSource().sendFailure(Component.translatable("scene.index", index + 1));
        } else
            consumer.accept(x, point);
    }
    
    @Override
    public CommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literal);
        
        if (indexConsumer != null)
            builder.then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                if (processor.canCreatePoint(x)) {
                    CamPoint point = processor.createPoint(x);
                    CamScene scene = processor.getScene(x);
                    if (scene.posTarget != null)
                        try {
                            processor.makeRelative(processor.getScene(x), x.getSource().getUnsidedLevel(), point);
                        } catch (SceneException e) {
                            x.getSource().sendFailure(e.getComponent());
                        }
                    processPoint(x, point);
                }
                return 0;
            }));
        else
            builder.executes((x) -> {
                if (processor.canCreatePoint(x)) {
                    CamPoint point = processor.createPoint(x);
                    CamScene scene = processor.getScene(x);
                    if (scene.posTarget != null)
                        try {
                            processor.makeRelative(processor.getScene(x), x.getSource().getUnsidedLevel(), point);
                        } catch (SceneException e) {
                            x.getSource().sendFailure(e.getComponent());
                        }
                    processPoint(x, point);
                }
                return 0;
            });
        
        builder.then(Commands.argument("location", Vec3Argument.vec3()).executes((x) -> {
            Vec3 vec = Vec3Argument.getVec3(x, "location");
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, 0, 0, 0, 70);
            processPoint(x, point);
            return 0;
        }).then(Commands.argument("rotation", RotationArgument.rotation()).executes(x -> {
            Vec3 vec = Vec3Argument.getVec3(x, "location");
            Vec2 rotation = RotationArgument.getRotation(x, "rotation").getRotation(x.getSource());
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, 0, 70);
            processPoint(x, point);
            return 0;
        }).then(Commands.argument("roll", DoubleArgumentType.doubleArg()).executes(x -> {
            Vec3 vec = Vec3Argument.getVec3(x, "location");
            Vec2 rotation = RotationArgument.getRotation(x, "rotation").getRotation(x.getSource());
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, DoubleArgumentType.getDouble(x, "roll"), 70);
            processPoint(x, point);
            return 0;
        }).then(Commands.argument("fov", DoubleArgumentType.doubleArg()).executes(x -> {
            Vec3 vec = Vec3Argument.getVec3(x, "location");
            Vec2 rotation = RotationArgument.getRotation(x, "rotation").getRotation(x.getSource());
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, DoubleArgumentType.getDouble(x, "roll"), DoubleArgumentType.getDouble(x, "fov"));
            processPoint(x, point);
            return 0;
        })))));
        
        return builder.build();
    }
    
}
