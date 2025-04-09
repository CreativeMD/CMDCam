package team.creative.cmdcam.fabric;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import team.creative.cmdcam.client.mixin.LocalCoordinatesAccessor;
import team.creative.cmdcam.client.mixin.WorldCoordinatesAccessor;

public class PosArgHelper {
    public static BlockPos getLoadedBlockPos(CommandContext<FabricClientCommandSource> commandContext, String string) throws CommandSyntaxException {
        Level serverLevel = (commandContext.getSource()).getWorld();
        return getLoadedBlockPos(commandContext, serverLevel, string);
    }

    public static BlockPos getLoadedBlockPos(CommandContext<FabricClientCommandSource> commandContext, Level serverLevel, String string) throws CommandSyntaxException {
        BlockPos blockPos = getBlockPos(commandContext, string);
        if (!serverLevel.hasChunkAt(blockPos)) {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
        } else if (!serverLevel.isInWorldBounds(blockPos)) {
            throw BlockPosArgument.ERROR_OUT_OF_WORLD.create();
        } else {
            return blockPos;
        }
    }

    public static BlockPos getBlockPos(CommandContext<FabricClientCommandSource> commandContext, String string) {
        var coords = (commandContext.getArgument(string, Coordinates.class));
        var source = commandContext.getSource();

        Position pos;

        if (coords instanceof WorldCoordinates world) {
            var vec3 = source.getPosition();
            var ac = (WorldCoordinatesAccessor) world;
            pos = new Vec3(ac.getX().get(vec3.x), ac.getY().get(vec3.y), ac.getZ().get(vec3.z));
        } else if (coords instanceof LocalCoordinates local) {
            var ac = (LocalCoordinatesAccessor) local;
            Vec2 vec2 = source.getRotation();
            Vec3 vec3 = source.getPosition();
            float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float g = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float h = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
            float i = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
            float j = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            float k = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            Vec3 vec32 = new Vec3((f * h), i, (g * h));
            Vec3 vec33 = new Vec3((f * j), k, (g * j));
            Vec3 vec34 = vec32.cross(vec33).scale(-1.0);
            double d = vec32.x * ac.getForwards() + vec33.x * ac.getUp() + vec34.x * ac.getLeft();
            double e = vec32.y * ac.getForwards() + vec33.y * ac.getUp() + vec34.y * ac.getLeft();
            double l = vec32.z * ac.getForwards() + vec33.z * ac.getUp() + vec34.z * ac.getLeft();
            pos = new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
        } else throw new IllegalStateException("a");

        return BlockPos.containing(pos);
    }

    public static Vec3 getVec3(CommandContext<FabricClientCommandSource> commandContext, String string) {
        var coords = (commandContext.getArgument(string, Coordinates.class));
        var source = commandContext.getSource();

        Vec3 pos;

        if (coords instanceof WorldCoordinates world) {
            var vec3 = source.getPosition();
            var ac = (WorldCoordinatesAccessor) world;
            pos = new Vec3(ac.getX().get(vec3.x), ac.getY().get(vec3.y), ac.getZ().get(vec3.z));
        } else if (coords instanceof LocalCoordinates local) {
            var ac = (LocalCoordinatesAccessor) local;
            Vec2 vec2 = source.getRotation();
            Vec3 vec3 = source.getPosition();
            float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float g = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
            float h = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
            float i = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
            float j = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            float k = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
            Vec3 vec32 = new Vec3((f * h), i, (g * h));
            Vec3 vec33 = new Vec3((f * j), k, (g * j));
            Vec3 vec34 = vec32.cross(vec33).scale(-1.0);
            double d = vec32.x * ac.getForwards() + vec33.x * ac.getUp() + vec34.x * ac.getLeft();
            double e = vec32.y * ac.getForwards() + vec33.y * ac.getUp() + vec34.y * ac.getLeft();
            double l = vec32.z * ac.getForwards() + vec33.z * ac.getUp() + vec34.z * ac.getLeft();
            pos = new Vec3(vec3.x + d, vec3.y + e, vec3.z + l);
        } else throw new IllegalStateException("a");

        return pos;
    }

    public static Vec2 getRotation(CommandContext<FabricClientCommandSource> commandContext, String string) {
        var coords = commandContext.getArgument(string, Coordinates.class);

        if (coords instanceof WorldCoordinates world) {
            var rot = commandContext.getSource().getRotation();
            return new Vec2((float) ((WorldCoordinatesAccessor) world).getX().get(rot.x), (float) ((WorldCoordinatesAccessor) world).getY().get(rot.y));
        }

        return Vec2.ZERO;
    }
}
