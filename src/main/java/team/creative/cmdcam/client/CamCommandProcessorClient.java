package team.creative.cmdcam.client;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import team.creative.cmdcam.common.command.CamCommandProcessor;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;

public class CamCommandProcessorClient implements CamCommandProcessor {
    
    @Override
    public CamScene getScene(CommandContext<CommandSourceStack> context) {
        return CMDCamClient.getConfigScene();
    }
    
    @Override
    public boolean canSelectTarget() {
        return true;
    }
    
    @Override
    public void selectTarget(CommandContext<CommandSourceStack> context, boolean look) throws SceneException {
        if (look)
            checkFollowTarget(context, true);
        CamEventHandlerClient.startSelectionMode(x -> {
            try {
                setTarget(context, x, look);
            } catch (SceneException e) {}
        });
    }
    
    @Override
    public boolean canCreatePoint(CommandContext<CommandSourceStack> context) {
        return true;
    }
    
    @Override
    public CamPoint createPoint(CommandContext<CommandSourceStack> context) {
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
    public void start(CommandContext<CommandSourceStack> context) throws SceneException {
        CMDCamClient.start(CMDCamClient.createScene());
    }
    
    @Override
    public void teleport(CommandContext<CommandSourceStack> context, int index) {
        CMDCamClient.teleportTo(getScene(context).points.get(index));
    }
    
    @Override
    public void markDirty(CommandContext<CommandSourceStack> context) {
        CMDCamClient.checkTargetMarker();
    }
    
}
