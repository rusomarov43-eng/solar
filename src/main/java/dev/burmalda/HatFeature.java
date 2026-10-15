package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Hat, cape and wings on YOUR player only (client-side, nobody else can see them). */
public class HatFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public HatFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e,
                       float limbAngle, float limbDistance, float tickDelta, float animProgress,
                       float headYaw, float headPitch) {
        if (e != MinecraftClient.getInstance().player || e.isInvisible()) return;
        float time = (System.currentTimeMillis() % 100000L) / 1000f;

        if (Modules.HAT.enabled) {
            Feature f = Modules.HAT;
            int rgb = Colors.pick(f.m("Color"), f.f("Hue"));
            float s = f.f("Scale");
            m.push();
            getContextModel().head.rotate(m);
            m.scale(s, s, s);
            Cosmetics.hat(f.m("Hat"), new MB(m, vcp, light, -1f), rgb, time);
            m.pop();
        }

        if (Modules.CAPE.enabled) {
            Feature f = Modules.CAPE;
            int rgb = Colors.pick(f.m("Color"), f.f("Hue"));

            double dx = MathHelper.lerp(tickDelta, e.prevCapeX, e.capeX) - MathHelper.lerp(tickDelta, e.prevX, e.getX());
            double dy = MathHelper.lerp(tickDelta, e.prevCapeY, e.capeY) - MathHelper.lerp(tickDelta, e.prevY, e.getY());
            double dz = MathHelper.lerp(tickDelta, e.prevCapeZ, e.capeZ) - MathHelper.lerp(tickDelta, e.prevZ, e.getZ());
            float yaw = e.prevBodyYaw + (e.bodyYaw - e.prevBodyYaw) * tickDelta;
            double sin = MathHelper.sin(yaw * 0.017453292f);
            double cos = -MathHelper.cos(yaw * 0.017453292f);
            float lift = MathHelper.clamp((float) dy * 10f, -6f, 32f);
            float back = MathHelper.clamp((float) (dx * sin + dz * cos) * 100f, 0f, 150f);
            float side = MathHelper.clamp((float) (dx * cos - dz * sin) * 100f, -20f, 20f);
            lift += MathHelper.sin(limbAngle * 1.3f) * 14f * limbDistance;
            if (e.isInSneakingPose()) lift += 25f;

            m.push();
            m.translate(0f, 0f, 0.125f);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6f + back / 2f + lift));
            m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side / 2f));
            m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-side / 2f));
            Cosmetics.cape(new MB(m, vcp, light, -1f), rgb, time);
            m.pop();
        }

        if (Modules.WINGS.enabled) {
            Feature f = Modules.WINGS;
            int rgb = Colors.pick(f.m("Color"), f.f("Hue"));
            float s = f.f("Scale");
            m.push();
            getContextModel().body.rotate(m);
            m.scale(s, s, s);
            Cosmetics.wings(new MB(m, vcp, light, -1f), rgb, time, f.f("Flap speed"));
            m.pop();
        }
    }
}
