package team.creative.cmdcam.common.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.mc.TickUtils;

public interface CamCommandProcessor {
    
    public CamScene getScene(CommandContext<CommandSourceStack> context);
    
    public boolean canSelectTarget();
    
    public void selectTarget(CommandContext<CommandSourceStack> context, boolean look) throws SceneException;
    
    public default void setTarget(CommandContext<CommandSourceStack> context, CamTarget target, boolean look) throws SceneException {
        if (look)
            getScene(context).lookTarget = target;
        else {
            checkFollowTarget(context, target != null);
            getScene(context).posTarget = target;
        }
    }
    
    public default void checkFollowTarget(CommandContext<CommandSourceStack> context, boolean shouldFollow) throws SceneException {
        CamScene scene = getScene(context);
        if (scene.points.isEmpty())
            return;
        if (shouldFollow && scene.posTarget == null)
            throw new SceneException("scene.follow.absolute_fail");
        if (!shouldFollow && scene.posTarget != null)
            throw new SceneException("scene.follow.relative_fail");
    }
    
    public boolean canCreatePoint(CommandContext<CommandSourceStack> context);
    
    public CamPoint createPoint(CommandContext<CommandSourceStack> context);
    
    public default void makeRelative(CamScene scene, Level level, CamPoint point) throws SceneException {
        if (scene.posTarget != null) {
            Vec3d vec = scene.posTarget.position(level, TickUtils.getDeltaFrameTime(level));
            if (vec == null)
                throw new SceneException("scene.follow.not_found");
            point.sub(vec);
        }
    }
    
    public boolean requiresSceneName();
    
    public boolean requiresPlayer();
    
    public void start(CommandContext<CommandSourceStack> context) throws SceneException;
    
    public void teleport(CommandContext<CommandSourceStack> context, int index);
    
    public void markDirty(CommandContext<CommandSourceStack> context);
    
    public Player getPlayer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException;
    
    public Entity getEntity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException;
    
}
