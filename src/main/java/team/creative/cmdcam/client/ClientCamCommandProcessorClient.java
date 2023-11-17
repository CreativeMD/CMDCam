package team.creative.cmdcam.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import team.creative.cmdcam.common.command.client.ClientCamCommandProcessor;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;

public class ClientCamCommandProcessorClient implements ClientCamCommandProcessor {

    @Override
    public CamScene getScene(CommandContext<FabricClientCommandSource> context) {
        return CMDCamClient.getConfigScene();
    }

    @Override
    public boolean canSelectTarget() {
        return true;
    }

    @Override
    public void selectTarget(CommandContext<FabricClientCommandSource> context, boolean look) throws SceneException {
        if (!look)
            checkFollowTarget(context, true);
        CamEventHandlerClient.startSelectionMode(x -> {
            try {
                setTarget(context, x, look);
            } catch (SceneException e) {}
        });
    }

    @Override
    public boolean canCreatePoint(CommandContext<FabricClientCommandSource> context) {
        return true;
    }

    @Override
    public CamPoint createPoint(CommandContext<FabricClientCommandSource> context) {
        return CamPoint.createLocal();
    }

    @Override
    public boolean requiresSceneName() {
        return false;
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public void start(CommandContext<FabricClientCommandSource> context) throws SceneException {
        CMDCamClient.start(CMDCamClient.createScene());
    }

    @Override
    public void teleport(CommandContext<FabricClientCommandSource> context, int index) {
        CMDCamClient.teleportTo(getScene(context).points.get(index));
    }

    @Override
    public void markDirty(CommandContext<FabricClientCommandSource> context) {
        CMDCamClient.checkTargetMarker();
    }

    @Override
    public Player getPlayer(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        EntitySelectorClient selector = (EntitySelectorClient) context.getArgument(name, EntitySelector.class);
        return selector.findSinglePlayerClient(context.getSource());
    }

    @Override
    public Entity getEntity(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        EntitySelectorClient selector = (EntitySelectorClient) context.getArgument(name, EntitySelector.class);
        return selector.findSingleEntityClient(context.getSource());
    }

}
