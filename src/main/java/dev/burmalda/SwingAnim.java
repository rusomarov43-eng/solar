package dev.burmalda;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** First-person swing styles. Replaces the vanilla swing offset when a style other than Vanilla is chosen. */
public final class SwingAnim {
    private static final float PI = (float) Math.PI;

    /** @return true if the swing was handled (vanilla offset must be skipped). */
    public static boolean apply(MatrixStack m, Arm arm, float t) {
        Feature f = Modules.SWING;
        if (!f.enabled) return false;
        int style = f.m("Style");
        if (style == 0) return false;
        float k = f.f("Intensity");
        float sg = arm == Arm.RIGHT ? 1f : -1f;
        float s = MathHelper.sin(t * PI);
        float s2 = MathHelper.sin(MathHelper.sqrt(t) * PI);

        switch (style) {
            case 1 -> { // Slash: diagonal sweep
                m.translate(sg * -0.2f * s2 * k, 0.1f * s * k, -0.18f * s * k);
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sg * 34f * s2 * k));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sg * -66f * s * k));
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-34f * s2 * k));
            }
            case 2 -> { // Stab: forward thrust
                m.translate(0f, -0.04f * s * k, -0.62f * s * k);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-12f * s * k));
            }
            case 3 -> { // Smooth: soft dip
                m.translate(0f, -0.2f * s * k, -0.14f * s * k);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-42f * s * k));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sg * 10f * s * k));
            }
            case 4 -> { // Spin: full roll around the item axis
                float e = 1f - (1f - t) * (1f - t);
                m.translate(0f, 0f, -0.25f * s * k);
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-360f * e * Math.min(1f, k)));
            }
            default -> { // Whip: big sideways arc
                m.translate(sg * -0.36f * s2 * k, 0.04f * s * k, -0.1f * s * k);
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sg * -95f * s2 * k));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sg * 22f * s * k));
            }
        }
        return true;
    }

    public static void viewModel(MatrixStack m) {
        Feature f = Modules.VIEWMODEL;
        if (!f.enabled) return;
        m.translate(f.f("X"), f.f("Y"), f.f("Z"));
        float s = f.f("Scale");
        m.scale(s, s, s);
    }

    private SwingAnim() { }
}
