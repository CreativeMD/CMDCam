package team.creative.cmdcam.common.command;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import team.creative.cmdcam.client.PathParseException;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;

public interface CamCommandProcessor {
    
    public CamScene getScene(CommandContext<CommandSourceStack> context);
    
    public boolean canSelectTarget();
    
    public void selectTarget();
    
    public boolean canCreatePoint(CommandContext<CommandSourceStack> context);
    
    public CamPoint createPoint(CommandContext<CommandSourceStack> context);
    
    public boolean requiresSceneName();
    
    public boolean requiresPlayer();
    
    public void start(CommandContext<CommandSourceStack> context) throws PathParseException;
    
    public void teleport(CommandContext<CommandSourceStack> context, int index);
    
    public void stop(CommandContext<CommandSourceStack> context);
    
}
