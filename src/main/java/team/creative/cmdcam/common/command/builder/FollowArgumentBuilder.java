package team.creative.cmdcam.common.command.builder;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

public class FollowArgumentBuilder extends ArgumentBuilder<CommandSourceStack, FollowArgumentBuilder> {
    
    private final CamAttribute attribute;
    private final CamCommandProcessor processor;
    
    public FollowArgumentBuilder(CamAttribute attribute, CamCommandProcessor processor) {
        this.attribute = attribute;
        this.processor = processor;
    }
    
    @Override
    protected FollowArgumentBuilder getThis() {
        return this;
    }
    
    @Override
    public CommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(attribute.name())
                .then(Commands.literal("step").then(Commands.argument("div", DoubleArgumentType.doubleArg(1)).executes(x -> {
                    double div = DoubleArgumentType.getDouble(x, "div");
                    processor.getScene(x).getConfig(attribute).div = div;
                    processor.markDirty(x);
                    x.getSource().sendSuccess(() -> Component.translatable("scene.follow.div", attribute.name(), div), false);
                    return 0;
                })));
        
        return builder.build();
    }
    
}
