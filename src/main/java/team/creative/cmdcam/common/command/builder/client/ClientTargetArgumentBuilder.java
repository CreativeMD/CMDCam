package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.command.client.ClientCamCommandProcessor;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.cmdcam.fabric.PosArgHelper;

public class ClientTargetArgumentBuilder extends ArgumentBuilder<FabricClientCommandSource, ClientTargetArgumentBuilder> {
    
    private final String literal;
    private final boolean look;
    private final ClientCamCommandProcessor processor;
    
    public ClientTargetArgumentBuilder(final String literal, boolean look, ClientCamCommandProcessor processor) {
        this.literal = literal;
        this.look = look;
        this.processor = processor;
    }
    
    public String getLiteral() {
        return literal;
    }
    
    @Override
    protected ClientTargetArgumentBuilder getThis() {
        return this;
    }
    
    private String translatePrefix() {
        if (look)
            return "scene.look.target.";
        return "scene.follow.target.";
    }
    
    @Override
    public CommandNode<FabricClientCommandSource> build() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(literal).then(ClientCommandManager.literal("none").executes(x -> {
            try {
                processor.setTarget(x, null, look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "remove"));
            return 0;
        })).then(ClientCommandManager.literal("self").executes(x -> {
            try {
                processor.setTarget(x, new CamTarget.SelfTarget(), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "self"));
            return 0;
        })).then(ClientCommandManager.literal("player").then(ClientCommandManager.argument("player", EntityArgument.player()).executes(x -> {
            Player player = processor.getPlayer(x, "player");
            try {
                processor.setTarget(x, new CamTarget.PlayerTarget(player), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "player", player.getScoreboardName()));
            return 0;
        }))).then(ClientCommandManager.literal("entity").then(ClientCommandManager.argument("entity", EntityArgument.entity()).executes(x -> {
            Entity entity = processor.getEntity(x, "entity");
            try {
                processor.setTarget(x, new CamTarget.EntityTarget(entity), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "entity", entity.getStringUUID()));
            return 0;
        }))).then(ClientCommandManager.literal("pos").then(ClientCommandManager.argument("pos", BlockPosArgument.blockPos()).executes(x -> {
            BlockPos pos = PosArgHelper.getLoadedBlockPos(x, "pos");
            try {
                processor.setTarget(x, new CamTarget.BlockTarget(pos), look);
            } catch (SceneException e) {
                x.getSource().sendError(Component.translatable(e.getMessage()));
            }
            processor.markDirty(x);
            x.getSource().sendFeedback(Component.translatable(translatePrefix() + "pos", pos.toShortString()));
            return 0;
        })));
        
        if (processor.canSelectTarget())
            builder.then(ClientCommandManager.literal("select").executes(x -> {
                try {
                    processor.selectTarget(x, look);
                } catch (SceneException e) {
                    x.getSource().sendError(Component.translatable(e.getMessage()));
                }
                x.getSource().sendFeedback(Component.translatable(translatePrefix() + "select"));
                return 0;
            }));
        
        return builder.build();
    }
    
}
