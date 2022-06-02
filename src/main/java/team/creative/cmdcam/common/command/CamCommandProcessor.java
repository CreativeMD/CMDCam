package team.creative.cmdcam.common.command;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import team.creative.cmdcam.common.scene.CamScene;

public interface CamCommandProcessor {
    
    public CamScene getScene(CommandContext<CommandSourceStack> context);
    
    public boolean canSelectTarget();
    
    public void selectTarget();
    
}
