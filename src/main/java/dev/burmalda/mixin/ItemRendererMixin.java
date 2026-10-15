package dev.burmalda.mixin;

import dev.burmalda.CustomItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow
    public abstract BakedModel getModel(ItemStack stack, World world, LivingEntity entity, int seed);

    /** Held items of the local player only (first and third person). */
    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"), cancellable = true)
    private void nebula$item(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, boolean leftHanded,
                             MatrixStack matrices, VertexConsumerProvider vcp, World world, int light, int overlay,
                             int seed, CallbackInfo ci) {
        if (entity == null || entity != MinecraftClient.getInstance().player) return;
        if (!CustomItems.wants(stack)) return;
        BakedModel model = this.getModel(stack, world, entity, seed);
        matrices.push();
        model.getTransformation().getTransformation(mode).apply(leftHanded, matrices);
        matrices.translate(-0.5f, -0.5f, -0.5f);
        CustomItems.render(matrices, vcp, light);
        matrices.pop();
        ci.cancel();
    }
}
