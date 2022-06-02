package team.creative.cmdcam.common.command.builder;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.target.CamTarget;

public class TargetArgumentBuilder extends ArgumentBuilder<CommandSourceStack, TargetArgumentBuilder> {
    
    private final String literal;
    private final CamCommandProcessor processor;
    
    public TargetArgumentBuilder(final String literal, CamCommandProcessor processor) {
        this.literal = literal;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected TargetArgumentBuilder getThis() {
        return this;
    }
    
    @Override
    public CommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(literal).then(Commands.literal("none").executes(x -> {
            processor.getScene(x).lookTarget = null;
            x.getSource().sendSuccess(new TranslatableComponent("scene.look.target.remove"), false);
            return 0;
        })).then(Commands.literal("self").executes(x -> {
            processor.getScene(x).lookTarget = new CamTarget.SelfTarget();
            x.getSource().sendSuccess(new TranslatableComponent("scene.look.target.self"), false);
            return 0;
        })).then(Commands.literal("player").then(Commands.argument("player", EntityArgument.player()).executes(x -> {
            Player player = EntityArgument.getPlayer(x, "player");
            processor.getScene(x).lookTarget = new CamTarget.PlayerTarget(player);
            x.getSource().sendSuccess(new TranslatableComponent("scene.look.target.player", player.getScoreboardName()), false);
            return 0;
        }))).then(Commands.literal("entity").then(Commands.argument("entity", EntityArgument.entity()).executes(x -> {
            Entity entity = EntityArgument.getEntity(x, "entity");
            processor.getScene(x).lookTarget = new CamTarget.EntityTarget(entity);
            x.getSource().sendSuccess(new TranslatableComponent("scene.look.target.entity", entity.getStringUUID()), false);
            return 0;
        }))).then(Commands.literal("pos").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(x -> {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(x, "pos");
            processor.getScene(x).lookTarget = new CamTarget.BlockTarget(pos);
            x.getSource().sendSuccess(new TranslatableComponent("scene.look.target.pos", pos.toShortString()), false);
            return 0;
        })));
        
        if (processor.canSelectTarget())
            builder.then(Commands.literal("select").executes(x -> {
                processor.selectTarget();
                x.getSource().sendSuccess(new TextComponent("scene.look.target.select"), false);
                return 0;
            }));
        
        return builder.build();
    }
    
}
