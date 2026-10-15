package dev.burmalda.mixin;

import dev.burmalda.SwingAnim;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow
    protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    /** Swap the vanilla swing offset for our swing styles. */
    @Redirect(method = "renderFirstPersonItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"))
    private void nebula$swing(HeldItemRenderer self, MatrixStack matrices, Arm arm, float swing) {
        if (!SwingAnim.apply(matrices, arm, swing)) this.applySwingOffset(matrices, arm, swing);
    }

    /** ViewModel: move / scale the held item. */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void nebula$view(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                             float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingAnim.viewModel(matrices);
    }
}
