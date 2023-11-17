package team.creative.cmdcam.common.command.builder.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import team.creative.cmdcam.common.command.client.ClientCamCommandProcessor;
import team.creative.cmdcam.common.scene.attribute.CamAttribute;

public class ClientFollowArgumentBuilder extends ArgumentBuilder<FabricClientCommandSource, ClientFollowArgumentBuilder> {
    
    private final CamAttribute attribute;
    private final ClientCamCommandProcessor processor;
    
    public ClientFollowArgumentBuilder(CamAttribute attribute, ClientCamCommandProcessor processor) {
        this.attribute = attribute;
        this.processor = processor;
    }
    
    @Override
    protected ClientFollowArgumentBuilder getThis() {
        return this;
    }
    
    @Override
    public CommandNode<FabricClientCommandSource> build() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(attribute.name())
                .then(ClientCommandManager.literal("step").then(ClientCommandManager.argument("div", DoubleArgumentType.doubleArg(1)).executes(x -> {
                    double div = DoubleArgumentType.getDouble(x, "div");
                    processor.getScene(x).getConfig(attribute).div = div;
                    processor.markDirty(x);
                    x.getSource().sendFeedback(Component.translatable("scene.follow.div", attribute.name(), div));
                    return 0;
                })));
        
        return builder.build();
    }
    
}
