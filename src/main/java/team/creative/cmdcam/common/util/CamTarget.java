package team.creative.cmdcam.common.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class CamTarget {
    
    private static HashMap<String, Class<? extends CamTarget>> targetTypes = new HashMap<>();
    private static HashMap<Class<? extends CamTarget>, String> targetTypesInverted = new HashMap<>();
    
    public static void registerTargetType(Class<? extends CamTarget> targetClass, String id) {
        targetTypes.put(id, targetClass);
        targetTypesInverted.put(targetClass, id);
    }
    
    public static Class<? extends CamTarget> getClassByID(String id) {
        return targetTypes.get(id);
    }
    
    public static CamTarget readFromNBT(CompoundTag nbt) {
        Class<? extends CamTarget> targetClass = getClassByID(nbt.getString("id"));
        if (targetClass != null) {
            try {
                return targetClass.getConstructor().newInstance();
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                System.out.println("Invalid target class " + nbt.getString("id") + "," + targetClass.getName());
            }
        }
        return null;
    }
    
    static {
        registerTargetType(BlockTarget.class, "block");
        registerTargetType(EntityTarget.class, "entity");
        registerTargetType(SelfTarget.class, "self");
    }
    
    public abstract Vec3 getTargetVec(Level world, float partialTicks);
    
    protected abstract void write(CompoundTag nbt);
    
    protected abstract void read(CompoundTag nbt);
    
    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putString("id", targetTypesInverted.get(this.getClass()));
        write(nbt);
        return nbt;
    }
    
    public void start(Level world) {}
    
    public void finish() {}
    
    public static class BlockTarget extends CamTarget {
        
        public BlockTarget() {
            
        }
        
        public BlockTarget(BlockPos pos) {
            this.pos = pos;
        }
        
        public BlockPos pos;
        
        @Override
        public Vec3 getTargetVec(Level level, float partialTicks) {
            return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        
        @Override
        protected void write(CompoundTag nbt) {
            nbt.putIntArray("data", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
        
        @Override
        protected void read(CompoundTag nbt) {
            int[] array = nbt.getIntArray("data");
            if (array == null || array.length != 3)
                throw new IllegalArgumentException("Invalid block target data=" + array);
            pos = new BlockPos(array[0], array[1], array[2]);
        }
        
    }
    
    public static class EntityTarget extends CamTarget {
        
        public Entity cachedEntity;
        public String uuid;
        
        public EntityTarget() {
            
        }
        
        public EntityTarget(Entity entity) {
            this.cachedEntity = entity;
            this.uuid = entity.getStringUUID();
        }
        
        @Override
        public void start(Level world) {
            for (Entity entity : world
                    .getEntitiesOfClass(Entity.class, new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY))) {
                if (entity.getStringUUID().equals(uuid)) {
                    cachedEntity = entity;
                    break;
                }
            }
        }
        
        @Override
        public void finish() {
            cachedEntity = null;
        }
        
        @Override
        public Vec3 getTargetVec(Level level, float partialTicks) {
            if (cachedEntity != null && !cachedEntity.isAlive())
                cachedEntity = null;
            
            if (cachedEntity instanceof LivingEntity)
                return ((LivingEntity) cachedEntity).getEyePosition(partialTicks);
            else if (cachedEntity != null)
                return cachedEntity.getEyePosition(partialTicks);
            
            return null;
        }
        
        @Override
        protected void write(CompoundTag nbt) {
            nbt.putString("uuid", uuid);
        }
        
        @Override
        protected void read(CompoundTag nbt) {
            uuid = nbt.getString("uuid");
        }
        
    }
    
    public static class SelfTarget extends CamTarget {
        
        public SelfTarget() {
            
        }
        
        @Override
        protected void write(CompoundTag nbt) {
            
        }
        
        @Override
        protected void read(CompoundTag nbt) {
            
        }
        
        @Override
        public Vec3 getTargetVec(Level level, float partialTicks) {
            
            Entity cachedEntity = Minecraft.getInstance().player;
            
            if (cachedEntity instanceof LivingEntity)
                return ((LivingEntity) cachedEntity).getEyePosition(partialTicks);
            else if (cachedEntity != null)
                return cachedEntity.getEyePosition(partialTicks);
            
            return null;
        }
        
    }
    
}
