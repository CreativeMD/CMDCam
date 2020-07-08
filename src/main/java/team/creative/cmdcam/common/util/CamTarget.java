package team.creative.cmdcam.common.util;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
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

	public static CamTarget readFromNBT(CompoundNBT nbt) {
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

	public abstract Vector3d getTargetVec(World world, float partialTicks);

	protected abstract void write(CompoundNBT nbt);

	protected abstract void read(CompoundNBT nbt);

	public CompoundNBT writeToNBT(CompoundNBT nbt) {
		nbt.putString("id", targetTypesInverted.get(this.getClass()));
		write(nbt);
		return nbt;
	}

	public static class BlockTarget extends CamTarget {

		public BlockTarget() {

		}

		public BlockTarget(BlockPos pos) {
			this.pos = pos;
		}

		public BlockPos pos;

		@Override
		public Vector3d getTargetVec(World world, float partialTicks) {
			return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		}

		@Override
		protected void write(CompoundNBT nbt) {
			nbt.putIntArray("data", new int[] { pos.getX(), pos.getY(), pos.getZ() });
		}

		@Override
		protected void read(CompoundNBT nbt) {
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
			this.uuid = entity.getCachedUniqueIdString();
		}

		@Override
		public Vector3d getTargetVec(World world, float partialTicks) {

			if (cachedEntity == null) {
				for (Entity entity : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY))) {
					if (entity.getCachedUniqueIdString().equals(uuid)) {
						cachedEntity = entity;
						break;
					}
				}
			}

			if (cachedEntity instanceof LivingEntity)
				return ((LivingEntity) cachedEntity).getEyePosition(partialTicks);
			else if (cachedEntity != null)
				return cachedEntity.getEyePosition(partialTicks);

			return null;
		}

		@Override
		protected void write(CompoundNBT nbt) {
			nbt.putString("uuid", uuid);
		}

		@Override
		protected void read(CompoundNBT nbt) {
			uuid = nbt.getString("uuid");
		}

	}

	public static class SelfTarget extends CamTarget {

		public SelfTarget() {

		}

		@Override
		protected void write(CompoundNBT nbt) {

		}

		@Override
		protected void read(CompoundNBT nbt) {

		}

		@Override
		public Vector3d getTargetVec(World world, float partialTicks) {

			Entity cachedEntity = Minecraft.getInstance().player;

			if (cachedEntity instanceof LivingEntity)
				return ((LivingEntity) cachedEntity).getEyePosition(partialTicks);
			else if (cachedEntity != null)
				return cachedEntity.getEyePosition(partialTicks);

			return null;
		}

	}

}
