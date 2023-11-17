package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.util.TriConsumer;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.client.ClientCamCommandProcessor;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.fabric.PosArgHelper;

import java.util.function.BiConsumer;

public class ClientPointArgumentBuilder extends ArgumentBuilder<FabricClientCommandSource, ClientPointArgumentBuilder> {
    
    private final String literal;
    private final TriConsumer<CommandContext<FabricClientCommandSource>, CamPoint, Integer> indexConsumer;
    private final BiConsumer<CommandContext<FabricClientCommandSource>, CamPoint> consumer;
    private final ClientCamCommandProcessor processor;
    
    public ClientPointArgumentBuilder(final String literal, TriConsumer<CommandContext<FabricClientCommandSource>, CamPoint, Integer> consumer, ClientCamCommandProcessor processor) {
        this.literal = literal;
        this.indexConsumer = consumer;
        this.consumer = null;
        this.processor = processor;
    }
    
    public ClientPointArgumentBuilder(final String literal, BiConsumer<CommandContext<FabricClientCommandSource>, CamPoint> consumer, ClientCamCommandProcessor processor) {
        this.literal = literal;
        this.indexConsumer = null;
        this.consumer = consumer;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected ClientPointArgumentBuilder getThis() {
        return this;
    }
    
    private void processPoint(CommandContext<FabricClientCommandSource> x, CamPoint point) {
        
        if (indexConsumer != null) {
            int index = IntegerArgumentType.getInteger(x, "index") - 1;
            CamScene scene = processor.getScene(x);
            if (index >= 0 && index < scene.points.size())
                indexConsumer.accept(x, point, index);
            else
                x.getSource().sendError(Component.translatable("scene.index", index + 1));
        } else
            consumer.accept(x, point);
    }
    
    @Override
    public CommandNode<FabricClientCommandSource> build() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(literal);
        
        if (indexConsumer != null)
            builder.then(RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument("index", IntegerArgumentType.integer()).executes((x) -> {
                if (processor.canCreatePoint(x)) {
                    CamPoint point = processor.createPoint(x);
                    CamScene scene = processor.getScene(x);
                    if (scene.posTarget != null)
                        try {
                            processor.makeRelative(processor.getScene(x), x.getSource().getWorld(), point);
                        } catch (SceneException e) {
                            x.getSource().sendError(e.getComponent());
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
                            processor.makeRelative(processor.getScene(x), x.getSource().getWorld(), point);
                        } catch (SceneException e) {
                            x.getSource().sendError(e.getComponent());
                        }
                    processPoint(x, point);
                }
                return 0;
            });
        
        builder.then(ClientCommandManager.argument("location", Vec3Argument.vec3()).executes((x) -> {
            Vec3 vec = PosArgHelper.getVec3(x, "location");
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, 0, 0, 0, 70);
            processPoint(x, point);
            return 0;
        }).then(ClientCommandManager.argument("rotation", RotationArgument.rotation()).executes(x -> {
            Vec3 vec = PosArgHelper.getVec3(x, "location");
            Vec2 rotation = PosArgHelper.getRotation(x, "rotation");
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, 0, 70);
            processPoint(x, point);
            return 0;
        }).then(ClientCommandManager.argument("roll", DoubleArgumentType.doubleArg()).executes(x -> {
            Vec3 vec = PosArgHelper.getVec3(x, "location");
            Vec2 rotation = PosArgHelper.getRotation(x, "rotation");
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, DoubleArgumentType.getDouble(x, "roll"), 70);
            processPoint(x, point);
            return 0;
        }).then(ClientCommandManager.argument("fov", DoubleArgumentType.doubleArg()).executes(x -> {
            Vec3 vec = PosArgHelper.getVec3(x, "location");
            Vec2 rotation = PosArgHelper.getRotation(x, "rotation");
            CamPoint point = new CamPoint(vec.x, vec.y, vec.z, rotation.y, rotation.x, DoubleArgumentType.getDouble(x, "roll"), DoubleArgumentType.getDouble(x, "fov"));
            processPoint(x, point);
            return 0;
        })))));
        
        return builder.build();
    }


}
