package team.creative.cmdcam.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.cmdcam.client.CamEventHandlerClient;

public class CamPoint {

	public double x;
	public double y;
	public double z;

	public double rotationYaw;
	public double rotationPitch;

	public double roll;
	public double zoom;

	public CamPoint(double x, double y, double z, double rotationYaw, double rotationPitch, double roll, double zoom) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.rotationYaw = rotationYaw;
		this.rotationPitch = rotationPitch;
		this.roll = roll;
		this.zoom = zoom;
	}

	public CamPoint(CompoundNBT nbt) {
		this.x = nbt.getDouble("x");
		this.y = nbt.getDouble("y");
		this.z = nbt.getDouble("z");
		this.rotationYaw = nbt.getDouble("rotationYaw");
		this.rotationPitch = nbt.getDouble("rotationPitch");
		this.roll = nbt.getDouble("roll");
		this.zoom = nbt.getDouble("zoom");
	}

	@OnlyIn(Dist.CLIENT)
	public CamPoint() {
		Minecraft mc = Minecraft.getInstance();
		this.x = mc.player.getPosX();
		this.y = mc.player.getPosYEye();
		this.z = mc.player.getPosZ();

		this.rotationYaw = mc.player.rotationYawHead;
		this.rotationPitch = mc.player.rotationPitch;

		this.roll = CamEventHandlerClient.roll;
		this.zoom = CamEventHandlerClient.fov;
	}

	public CamPoint getPointBetween(CamPoint point, double percent) {
		return new CamPoint(this.x + (point.x - this.x) * percent, this.y + (point.y - this.y) * percent, this.z + (point.z - this.z) * percent, this.rotationYaw + (point.rotationYaw - this.rotationYaw) * percent, this.rotationPitch + (point.rotationPitch - this.rotationPitch) * percent, this.roll + (point.roll - this.roll) * percent, this.zoom + (point.zoom - this.zoom) * percent);
	}

	public void faceEntity(Vector3d pos, float minYaw, float minPitch, double ticks) {
		double d0 = pos.x - this.x;
		double d2 = pos.z - this.z;
		double d1 = pos.y - this.y;

		double d3 = Math.sqrt(d0 * d0 + d2 * d2);
		double f2 = (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0D;
		double f3 = (-(Math.atan2(d1, d3) * 180.0D / Math.PI));

		this.rotationPitch = updateRotation(this.rotationPitch, f3, minPitch, ticks);
		this.rotationYaw = updateRotation(this.rotationYaw, f2, minYaw, ticks);
	}

	/** Arguments: current rotation, intended rotation, max increment. */
	private double updateRotation(double rotation, double intended, double min, double ticks) {
		double f3 = MathHelper.wrapDegrees(intended - rotation);

		if (f3 > 0)
			f3 = Math.min(Math.abs(f3 * ticks), f3);
		else
			f3 = Math.max(-Math.abs(f3 * ticks), f3);

		return rotation + f3;
	}

	public CamPoint copy() {
		return new CamPoint(x, y, z, rotationYaw, rotationPitch, roll, zoom);
	}

	@Override
	public String toString() {
		return "x:" + x + ",y:" + y + ",z:" + z + ",yaw:" + rotationYaw + ",pitch:" + rotationPitch + ",roll:" + roll + ",zoom:" + zoom;
	}

	public CompoundNBT writeToNBT(CompoundNBT nbt) {
		nbt.putDouble("x", x);
		nbt.putDouble("y", y);
		nbt.putDouble("z", z);
		nbt.putDouble("rotationYaw", rotationYaw);
		nbt.putDouble("rotationPitch", rotationPitch);
		nbt.putDouble("roll", roll);
		nbt.putDouble("zoom", zoom);
		return nbt;
	}

}
