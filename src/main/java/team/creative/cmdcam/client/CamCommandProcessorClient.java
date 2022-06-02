package team.creative.cmdcam.client;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import team.creative.cmdcam.common.command.CamCommandProcessor;
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
    public void selectTarget() {
        CamEventHandlerClient.startSelectionMode();
    }
    
}
