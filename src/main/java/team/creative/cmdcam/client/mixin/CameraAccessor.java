package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;

@Mixin(Camera.class)
public interface CameraAccessor {
    
    @Invoker
    public float callCalculateFov(float partialTicks);
    
}
