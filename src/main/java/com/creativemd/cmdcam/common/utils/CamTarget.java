package com.creativemd.cmdcam.common.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
    
    public static CamTarget readFromNBT(NBTTagCompound nbt) {
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
        registerTargetType(VecTarget.class, "vec");
        registerTargetType(EntityTarget.class, "entity");
        registerTargetType(SelfTarget.class, "self");
    }
    
    public abstract Vec3d getTargetVec(World world, float partialTicks);
    
    protected abstract void write(NBTTagCompound nbt);
    
    protected abstract void read(NBTTagCompound nbt);
    
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setString("id", targetTypesInverted.get(this.getClass()));
        write(nbt);
        return nbt;
    }
    
    public static class VecTarget extends CamTarget {
        
        public VecTarget() {
            
        }
        
        public VecTarget(Vec3d vec) {
            this.vec = vec;
        }
        
        public Vec3d vec;
        
        @Override
        public Vec3d getTargetVec(World world, float partialTicks) {
            return vec;
        }
        
        @Override
        protected void write(NBTTagCompound nbt) {
            nbt.setDouble("x", vec.x);
            nbt.setDouble("y", vec.y);
            nbt.setDouble("z", vec.z);
        }
        
        @Override
        protected void read(NBTTagCompound nbt) {
            this.vec = new Vec3d(nbt.getDouble("x"), nbt.getDouble("y"), nbt.getDouble("z"));
        }
        
    }
    
    public static class BlockTarget extends CamTarget {
        
        public BlockTarget() {
            
        }
        
        public BlockTarget(BlockPos pos) {
            this.pos = pos;
        }
        
        public BlockPos pos;
        
        @Override
        public Vec3d getTargetVec(World world, float partialTicks) {
            return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
        
        @Override
        protected void write(NBTTagCompound nbt) {
            nbt.setIntArray("data", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
        
        @Override
        protected void read(NBTTagCompound nbt) {
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
        
        public EntityTarget(String uuid) {
            this.cachedEntity = null;
            this.uuid = uuid;
        }
        
        public EntityTarget(Entity entity) {
            this.cachedEntity = entity;
            this.uuid = entity.getCachedUniqueIdString();
        }
        
        @Override
        public Vec3d getTargetVec(World world, float partialTicks) {
            
            if (cachedEntity == null) {
                for (Entity entity : world.getLoadedEntityList()) {
                    if (entity.getCachedUniqueIdString().equals(uuid)) {
                        cachedEntity = entity;
                        break;
                    }
                }
            }
            
            if (cachedEntity instanceof EntityLivingBase)
                return ((EntityLivingBase) cachedEntity).getPositionEyes(partialTicks).subtract(new Vec3d(0, ((EntityLivingBase) cachedEntity).getEyeHeight(), 0));
            else if (cachedEntity != null)
                return cachedEntity.getPositionEyes(partialTicks);
            
            return null;
        }
        
        @Override
        protected void write(NBTTagCompound nbt) {
            nbt.setString("uuid", uuid);
        }
        
        @Override
        protected void read(NBTTagCompound nbt) {
            uuid = nbt.getString("uuid");
        }
        
    }
    
    public static class SelfTarget extends CamTarget {
        
        public SelfTarget() {
            
        }
        
        @Override
        protected void write(NBTTagCompound nbt) {
            
        }
        
        @Override
        protected void read(NBTTagCompound nbt) {
            
        }
        
        @Override
        public Vec3d getTargetVec(World world, float partialTicks) {
            
            Entity cachedEntity = Minecraft.getMinecraft().player;
            
            if (cachedEntity instanceof EntityLivingBase)
                return ((EntityLivingBase) cachedEntity).getPositionEyes(partialTicks).subtract(new Vec3d(0, ((EntityLivingBase) cachedEntity).getEyeHeight(), 0));
            else if (cachedEntity != null)
                return cachedEntity.getPositionEyes(partialTicks);
            
            return null;
        }
        
    }
    
}
