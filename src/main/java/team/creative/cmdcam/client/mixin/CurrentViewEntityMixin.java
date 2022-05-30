package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;
import team.creative.cmdcam.client.CMDCamClient;

@Mixin(LocalPlayer.class)
public abstract class CurrentViewEntityMixin {
    
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z")
    public void isControlledCamera(CallbackInfoReturnable<Boolean> callback) {
        if (CMDCamClient.isPlaying())
            callback.setReturnValue(true);
    }
    
}
