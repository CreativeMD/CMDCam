package team.creative.cmdcam.client.mixin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import team.creative.cmdcam.client.EntitySelectorClient;

@Mixin(EntitySelector.class)
public abstract class EntitySelectorMixin implements EntitySelectorClient {
    
    @Shadow
    @Final
    private int maxResults;
    @Shadow
    @Final
    private boolean includesEntities;
    @Shadow
    @Final
    private boolean worldLimited;
    @Shadow
    @Final
    private Predicate<Entity> predicate;
    @Shadow
    @Final
    private MinMaxBounds.Doubles range;
    @Shadow
    @Final
    private Function<Vec3, Vec3> position;
    @Shadow
    @Final
    @Nullable
    private AABB aabb;
    @Shadow
    @Final
    private BiConsumer<Vec3, List<? extends Entity>> order;
    @Shadow
    @Final
    private boolean currentEntity;
    @Shadow
    @Final
    @Nullable
    private String playerName;
    @Shadow
    @Final
    @Nullable
    private UUID entityUUID;
    @Shadow
    @Final
    private EntityTypeTest<Entity, ?> type;
    @Shadow
    @Final
    private boolean usesSelector;
    
    @Shadow
    private void checkPermissions(CommandSourceStack source) throws CommandSyntaxException {}
    
    @Shadow
    private Predicate<Entity> getPredicate(Vec3 vec) {
        return null;
    }
    
    @Shadow
    public abstract boolean isWorldLimited();
    
    @Shadow
    private <T extends Entity> List<T> sortAndLimit(Vec3 vec, List<T> list) {
        return null;
    }
    
    @Override
    public Entity findSingleEntityClient(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        List<? extends Entity> list = this.findEntitiesClient(source);
        if (list.isEmpty())
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        if (list.size() > 1)
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        return list.get(0);
    }
    
    @Override
    public List<? extends Entity> findEntitiesClient(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        if (!this.includesEntities)
            return this.findPlayersClient(source);
        else if (this.playerName != null) {
            for (Player player : source.getUnsidedLevel().players())
                if (player.getGameProfile().getName().equalsIgnoreCase(playerName))
                    return Lists.newArrayList(player);
            return Collections.emptyList();
        } else if (this.entityUUID != null) {
            ClientLevel level = (ClientLevel) source.getUnsidedLevel();
            for (Entity entity : level.entitiesForRendering())
                if (entity.getUUID().equals(entityUUID))
                    return Lists.newArrayList(entity);
            return Collections.emptyList();
        }
        
        Vec3 vec3 = this.position.apply(source.getPosition());
        Predicate<Entity> predicate = this.getPredicate(vec3);
        if (this.currentEntity)
            return (List<? extends Entity>) (source.getEntity() != null && predicate.test(source.getEntity()) ? Lists.newArrayList(source.getEntity()) : Collections.emptyList());
        List<Entity> list = Lists.newArrayList();
        
        ClientLevel level = (ClientLevel) source.getUnsidedLevel();
        
        if (this.aabb != null)
            list.addAll(level.getEntities(this.type, this.aabb.move(vec3), predicate));
        else {
            for (Entity entity : level.entitiesForRendering()) {
                if (predicate.test(entity))
                    list.add(entity);
                
                for (PartEntity<?> p : level.getPartEntities()) {
                    Entity t = type.tryCast(p);
                    if (t != null && predicate.test(t))
                        list.add(t);
                }
            }
        }
        return this.sortAndLimit(vec3, list);
    }
    
    @Override
    public Player findSinglePlayerClient(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        List<Player> list = this.findPlayersClient(source);
        if (list.size() != 1)
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        return list.get(0);
    }
    
    @Override
    public List<Player> findPlayersClient(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermissions(source);
        if (this.playerName != null) {
            for (Player player : source.getUnsidedLevel().players())
                if (player.getGameProfile().getName().equalsIgnoreCase(playerName))
                    return Lists.newArrayList(player);
            return Collections.emptyList();
        } else if (this.entityUUID != null) {
            Player player = source.getUnsidedLevel().getPlayerByUUID(entityUUID);
            return player == null ? Collections.emptyList() : Lists.newArrayList(player);
        }
        
        Vec3 vec3 = this.position.apply(source.getPosition());
        Predicate<Entity> predicate = this.getPredicate(vec3);
        if (this.currentEntity) {
            if (source.getEntity() instanceof Player player && predicate.test(player))
                return Lists.newArrayList(player);
            return Collections.emptyList();
        }
        
        List<Player> list = Lists.newArrayList();
        for (Player player : source.getUnsidedLevel().players())
            if (predicate.test(player))
                list.add(player);
            
        return this.sortAndLimit(vec3, list);
    }
    
}
