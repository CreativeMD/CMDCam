package team.creative.cmdcam.client.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.cmdcam.common.utils.CamPath;
import team.creative.cmdcam.common.utils.CamPoint;

public class OutsideMode extends CamMode {
	
	public LivingEntity camPlayer;
	
	public OutsideMode(CamPath path) {
		super(path);
		if (path != null)
			this.camPlayer = new ZombieEntity(mc.world);
	}
	
	@Override
	public CamMode createMode(CamPath path) {
		return new OutsideMode(path);
	}
	
	@Override
	public String getDescription() {
		return "the player isn't the camera, you can control him at every time";
	}
	
	@Override
	public void onPathFinish() {
		super.onPathFinish();
		mc.setRenderViewEntity(mc.player);
	}
	
	@Override
	public LivingEntity getCamera() {
		return camPlayer;
	}
	
	@Override
	public void processPoint(CamPoint point) {
		super.processPoint(point);
		
		mc.setRenderViewEntity(camPlayer);
		if (camPlayer instanceof PlayerEntity)
			((PlayerEntity) camPlayer).abilities.isFlying = true;
		camPlayer.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
		camPlayer.setLocationAndAngles(point.x, point.y - camPlayer.getEyeHeight() + mc.player.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
		camPlayer.setRotationYawHead(0);
	}
	
}
