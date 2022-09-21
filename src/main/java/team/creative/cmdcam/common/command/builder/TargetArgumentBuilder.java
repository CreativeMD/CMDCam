package team.creative.cmdcam.common.command.builder;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.target.CamTarget;

public class TargetArgumentBuilder extends ArgumentBuilder<CommandSourceStack, TargetArgumentBuilder> {
    
    private final String literal;
    private final boolean look;
    private final CamCommandProcessor processor;
    
    public TargetArgumentBuilder(final String literal, boolean look, CamCommandProcessor processor) {
        this.literal = literal;
        this.look = look;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected TargetArgumentBuilder getThis() {
        return this;
    }
    
    private String translatePrefix() {
        if (look)
            return "scene.look.target.";
        return "scene.follow.target.";
    }
    
    @Override
    public CommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literal).then(Commands.literal("none").executes(x -> {
            try {
                processor.setTarget(x, null, look);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendSuccess(Component.translatable(translatePrefix() + "remove"), false);
            return 0;
        })).then(Commands.literal("self").executes(x -> {
            try {
                processor.setTarget(x, new CamTarget.SelfTarget(), look);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendSuccess(Component.translatable(translatePrefix() + "self"), false);
            return 0;
        })).then(Commands.literal("player").then(Commands.argument("player", EntityArgument.player()).executes(x -> {
            Player player = processor.getPlayer(x, "player");
            try {
                processor.setTarget(x, new CamTarget.PlayerTarget(player), look);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendSuccess(Component.translatable(translatePrefix() + "player", player.getScoreboardName()), false);
            return 0;
        }))).then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes(x -> {
            Entity entity = processor.getEntity(x, "entity");
            try {
                processor.setTarget(x, new CamTarget.EntityTarget(entity), look);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendSuccess(Component.translatable(translatePrefix() + "entity", entity.getStringUUID()), false);
            return 0;
        }))).then(Commands.literal("pos").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(x -> {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(x, "pos");
            try {
                processor.setTarget(x, new CamTarget.BlockTarget(pos), look);
            } catch (SceneException e) {
                x.getSource().sendFailure(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendSuccess(Component.translatable(translatePrefix() + "pos", pos.toShortString()), false);
            return 0;
        })));
        
        if (processor.canSelectTarget())
            builder.then(Commands.literal("select").executes(x -> {
                try {
                    processor.selectTarget(x, look);
                } catch (SceneException e) {
                    x.getSource().sendFailure(Component.translatable(e.getMessage()));
                }
                x.getSource().sendSuccess(Component.translatable(translatePrefix() + "select"), false);
                return 0;
            }));
        
        return builder.build();
    }
    
}
