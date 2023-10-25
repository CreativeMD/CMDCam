package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    
    @Invoker
    public double callGetFov(Camera cam, float partialTickTime, boolean renderLevel);
    
}
