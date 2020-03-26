package team.creative.cmdcam.client.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import team.creative.cmdcam.common.util.CamPath;
import team.creative.cmdcam.common.util.CamPoint;

public class OutsideMode extends CamMode {
	
	public Entity camPlayer;
	
	public OutsideMode(CamPath path) {
		super(path);
		if (path != null)
			this.camPlayer = new ItemEntity(mc.world, 0, 0, 0);
	}
	
	@Override
	public CamMode createMode(CamPath path) {
		return new OutsideMode(path);
	}
	
	@Override
	public String getDescription() {
		return "the player isn't the camera, but you are still in control";
	}
	
	@Override
	public void onPathFinish() {
		super.onPathFinish();
		mc.renderViewEntity = mc.player;
	}
	
	@Override
	public void processPoint(CamPoint point) {
		super.processPoint(point);
		
		mc.renderViewEntity = camPlayer;
		if (camPlayer instanceof PlayerEntity)
			((PlayerEntity) camPlayer).abilities.isFlying = true;
		
		camPlayer.setPositionAndRotation(point.x, point.y - camPlayer.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
		camPlayer.prevRotationYaw = (float) point.rotationYaw;
		camPlayer.prevRotationPitch = (float) point.rotationPitch;
		camPlayer.setLocationAndAngles(point.x, point.y - camPlayer.getEyeHeight(), point.z, (float) point.rotationYaw, (float) point.rotationPitch);
	}
	
}
