package dev.burmalda.mixin;

import dev.burmalda.Modules;
import dev.burmalda.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    private void burmalda$aspect(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        if (!Modules.ASPECT.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        float cur = (float) mc.getWindow().getFramebufferWidth() / (float) mc.getWindow().getFramebufferHeight();
        float target = Modules.ASPECT.f("Ratio");
        Matrix4f m = cir.getReturnValue();
        m.m00(m.m00() * cur / target);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void burmalda$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (!changingFov) return;
        cir.setReturnValue(Zoom.modify(cir.getReturnValue()));
    }
}
