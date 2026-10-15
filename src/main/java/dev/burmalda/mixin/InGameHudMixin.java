package dev.burmalda.mixin;

import dev.burmalda.CustomHud;
import dev.burmalda.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void burmalda$crosshair(DrawContext context, CallbackInfo ci) {
        if (Modules.CROSSHAIR.enabled) {
            CustomHud.crosshair(context);
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void burmalda$hotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        if (Modules.HOTBAR.enabled) {
            CustomHud.hotbar(context);
            ci.cancel();
        }
    }
}
