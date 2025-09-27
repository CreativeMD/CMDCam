package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import team.creative.cmdcam.client.CMDCamClient;

@Mixin(Camera.class)
public class CameraMixin {
    
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/Camera;isDetached()Z", cancellable = true)
    public void isDetached(CallbackInfoReturnable<Boolean> info) {
        if (CMDCamClient.isPlaying()) {
            var scene = CMDCamClient.getScene();
            if (scene.run != null && scene.mode.getCamera(scene.run) != Minecraft.getInstance().player)
                info.setReturnValue(true);
        }
        
    }
    
}
