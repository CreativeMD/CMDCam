package team.creative.cmdcam.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.cmdcam.client.CMDCamClient;

@Mixin(Camera.class)
public class CameraMixin {
    
    @Inject(at = @At("HEAD"), method = "isDetached()Z", cancellable = true)
    public void isDetached(CallbackInfoReturnable<Boolean> info) {
        if (CMDCamClient.isPlaying() && CMDCamClient.getScene().mode.getCamera() != Minecraft.getInstance().player)
            info.setReturnValue(true);
    }
    
}
