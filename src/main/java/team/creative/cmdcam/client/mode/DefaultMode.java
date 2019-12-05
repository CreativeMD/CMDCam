package team.creative.cmdcam.client.mode;

import team.creative.cmdcam.common.utils.CamPath;
import team.creative.cmdcam.common.utils.CamPoint;
import team.creative.cmdcam.common.utils.CamTarget.SelfTarget;

public class DefaultMode extends CamMode {
	
	public DefaultMode(CamPath path) {
		super(path);
		if (path != null && path.target != null && mc != null && path.target instanceof SelfTarget)
			path.target = null;
	}
	
	@Override
	public CamMode createMode(CamPath path) {
		return new DefaultMode(path);
	}
	
	@Override
	public String getDescription() {
		return "the player acts as the camera";
	}
	
	@Override
	public void processPoint(CamPoint point) {
		super.processPoint(point);
		mc.mouseHelper.grabMouse();
		
		mc.player.abilities.isFlying = true;
		
		mc.player.setPositionAndRotation(point.x, point.y, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
		mc.player.prevRotationYaw = (float) point.rotationYaw;
		mc.player.prevRotationPitch = (float) point.rotationPitch;
		mc.player.setLocationAndAngles(point.x, point.y/*-mc.thePlayer.getEyeHeight()*/, point.z, (float) point.rotationYaw, (float) point.rotationPitch);
		
	}
	
}
