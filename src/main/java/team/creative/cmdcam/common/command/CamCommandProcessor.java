package team.creative.cmdcam.common.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import team.creative.cmdcam.client.SceneException;
import team.creative.cmdcam.common.math.point.CamPoint;
import team.creative.cmdcam.common.scene.CamScene;
import team.creative.cmdcam.common.target.CamTarget;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.mc.TickUtils;

public interface CamCommandProcessor<T extends SharedSuggestionProvider> {

    CamScene getScene(CommandContext<T> context);

    boolean canSelectTarget();

    void selectTarget(CommandContext<T> context, boolean look) throws SceneException;

    default void setTarget(CommandContext<T> context, CamTarget target, boolean look) throws SceneException {
        if (look)
            getScene(context).lookTarget = target;
        else {
            checkFollowTarget(context, target != null);
            getScene(context).posTarget = target;
        }
    }

    default void checkFollowTarget(CommandContext<T> context, boolean shouldFollow) throws SceneException {
        CamScene scene = getScene(context);
        if (scene.points.isEmpty())
            return;
        if (shouldFollow && scene.posTarget == null)
            throw new SceneException("scene.follow.absolute_fail");
        if (!shouldFollow && scene.posTarget != null)
            throw new SceneException("scene.follow.relative_fail");
    }

    boolean canCreatePoint(CommandContext<T> context);

    CamPoint createPoint(CommandContext<T> context);

    default void makeRelative(CamScene scene, Level level, CamPoint point) throws SceneException {
        if (scene.posTarget != null) {
            Vec3d vec = scene.posTarget.position(level, TickUtils.getFrameTime(level));
            if (vec == null)
                throw new SceneException("scene.follow.not_found");
            point.sub(vec);
        }
    }

    boolean requiresSceneName();

    boolean requiresPlayer();

    void start(CommandContext<T> context) throws SceneException;

    void teleport(CommandContext<T> context, int index);

    void markDirty(CommandContext<T> context);

    Player getPlayer(CommandContext<T> context, String name) throws CommandSyntaxException;

    Entity getEntity(CommandContext<T> context, String name) throws CommandSyntaxException;

}
