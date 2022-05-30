package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;
import team.creative.cmdcam.client.CamEventHandlerClient;

@Mixin(GameRenderer.class)
public abstract class MouseOverMixin {
    
    @Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V")
    public void pickBefore(CallbackInfo info) {
        CamEventHandlerClient.setupMouseHandlerBefore();
    }
    
    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V")
    public void pickAfter(CallbackInfo info) {
        CamEventHandlerClient.setupMouseHandlerAfter();
    }
    
}
