package team.creative.cmdcam.common.target;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.common.util.math.vec.Vec3d;
import team.creative.creativecore.common.util.registry.NamedTypeRegistry;
import team.creative.creativecore.common.util.registry.exception.RegistryException;

public abstract class CamTarget {
    
    public static final NamedTypeRegistry<CamTarget> REGISTRY = new NamedTypeRegistry<CamTarget>().addConstructorPattern();
    
    public static CamTarget load(CompoundTag nbt) {
        try {
            CamTarget target = REGISTRY.create(nbt.getString("id"));
            target.loadExtra(nbt);
            return target;
        } catch (RegistryException e) {
            return null;
        }
    }
    
    static {
        REGISTRY.register("pos", BlockTarget.class);
        REGISTRY.register("entity", EntityTarget.class);
        REGISTRY.register("self", SelfTarget.class);
        REGISTRY.register("player", PlayerTarget.class);
    }
    
    public abstract Vec3d position(Level world, float partialTicks);
    
    protected abstract void saveExtra(CompoundTag nbt);
    
    protected abstract void loadExtra(CompoundTag nbt);
    
    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("id", REGISTRY.getId(this));
        saveExtra(nbt);
        return nbt;
    }
    
    public void start(Level level) {}
    
    public void finish() {}
    
    public static class BlockTarget extends CamTarget {
        
        public BlockPos pos;
        
        public BlockTarget() {}
        
        public BlockTarget(BlockPos pos) {
            this.pos = pos;
        }
        
        @Override
        public Vec3d position(Level level, float partialTicks) {
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        
        @Override
        protected void saveExtra(CompoundTag nbt) {
            nbt.putIntArray("data", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
        
        @Override
        protected void loadExtra(CompoundTag nbt) {
            int[] array = nbt.getIntArray("data");
            if (array == null || array.length != 3)
                throw new IllegalArgumentException("Invalid block target data=" + array);
            pos = new BlockPos(array[0], array[1], array[2]);
        }
        
    }
    
    public static class EntityTarget extends CamTarget {
        
        public Entity cachedEntity;
        public UUID uuid;
        
        public EntityTarget() {}
        
        public EntityTarget(Entity entity) {
            this.uuid = entity.getUUID();
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public void start(Level level) {
            if (level instanceof ServerLevel)
                cachedEntity = ((ServerLevel) level).getEntities().get(uuid);
            else
                for (Entity entity : ((ClientLevel) level).entitiesForRendering())
                    if (entity.getUUID().equals(uuid)) {
                        cachedEntity = entity;
                        break;
                    }
        }
        
        @Override
        public void finish() {
            cachedEntity = null;
        }
        
        @Override
        public Vec3d position(Level level, float partialTicks) {
            if (cachedEntity != null && !cachedEntity.isAlive())
                cachedEntity = null;
            
            if (cachedEntity != null)
                return new Vec3d(cachedEntity.getEyePosition(partialTicks));
            
            return null;
        }
        
        @Override
        protected void saveExtra(CompoundTag nbt) {
            nbt.putString("uuid", uuid.toString());
        }
        
        @Override
        protected void loadExtra(CompoundTag nbt) {
            uuid = UUID.fromString(nbt.getString("uuid"));
        }
        
    }
    
    public static class SelfTarget extends CamTarget {
        
        public SelfTarget() {}
        
        @Override
        protected void saveExtra(CompoundTag nbt) {}
        
        @Override
        protected void loadExtra(CompoundTag nbt) {}
        
        @Override
        @OnlyIn(Dist.CLIENT)
        public Vec3d position(Level level, float partialTicks) {
            return new Vec3d(Minecraft.getInstance().player.getEyePosition(partialTicks));
        }
        
    }
    
    public static class PlayerTarget extends CamTarget {
        
        public Player cachedPlayer;
        public UUID uuid;
        
        public PlayerTarget() {}
        
        public PlayerTarget(Player player) {
            this.cachedPlayer = player;
            this.uuid = player.getUUID();
        }
        
        @Override
        public void start(Level level) {
            cachedPlayer = level.getPlayerByUUID(uuid);
        }
        
        @Override
        public void finish() {
            cachedPlayer = null;
        }
        
        @Override
        public Vec3d position(Level level, float partialTicks) {
            if (cachedPlayer == null || !cachedPlayer.isAlive())
                return null;
            
            return new Vec3d(cachedPlayer.getEyePosition(partialTicks));
        }
        
        @Override
        protected void saveExtra(CompoundTag nbt) {
            nbt.putString("uuid", uuid.toString());
        }
        
        @Override
        protected void loadExtra(CompoundTag nbt) {
            uuid = UUID.fromString(nbt.getString("uuid"));
        }
        
    }
    
}
