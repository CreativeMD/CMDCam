package team.creative.cmdcam.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import team.creative.cmdcam.fabric.ComputeCameraAnglesCallback;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @SuppressWarnings("InvalidInjectorMethodSignature") // it is very incorrect
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void am$callCameraMove(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci, boolean bl, Camera camera, PoseStack poseStack2, double d, float f, float g, Matrix4f matrix4f) {
        var event = new ComputeCameraAnglesCallback((GameRenderer) (Object) this, camera, partialTicks, camera.getYRot(), camera.getXRot(), 0);
        ComputeCameraAnglesCallback.EVENT.invoker().onComputeCameraAngles(event);

        camera.setAnglesInternal(event.getYaw(), event.getPitch());

        matrixStack.mulPose(Axis.ZP.rotationDegrees(event.getRoll()));
    }
}
