package team.creative.cmdcam.transform;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import team.creative.cmdcam.client.CMDCamClient;
import team.creative.cmdcam.client.mode.OutsideMode;

public class CamMouseOverHandler {
	
	public static Minecraft mc = Minecraft.getInstance();
	
	public static Entity camera = null;
	
	public static void setupMouseHandlerBefore() {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			camera = mc.getRenderViewEntity();
			mc.setRenderViewEntity(mc.player);
		}
	}
	
	public static void setupMouseHandlerAfter() {
		if (CMDCamClient.getCurrentPath() != null && CMDCamClient.getCurrentPath().cachedMode instanceof OutsideMode) {
			mc.setRenderViewEntity(camera);
		}
	}
	
}
