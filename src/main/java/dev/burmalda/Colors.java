package dev.burmalda;

import net.minecraft.util.math.MathHelper;

public final class Colors {
    private static final float[] HUES = {0.74f, 0.58f, 0.5f, 0.35f, 0.0f, 0.88f, 0.07f};

    public static int client() {
        Feature t = Modules.THEME;
        if (t.b("Rainbow")) return rainbow(0.2f, 0f);
        int p = t.m("Preset");
        float hue = p < HUES.length ? HUES[p] : t.f("Hue");
        return MathHelper.hsvToRgb(hue, 0.65f, 1f) & 0xFFFFFF;
    }

    /** mode: 0 = client color, 1 = rainbow, 2 = custom hue */
    public static int pick(int mode, float hue) {
        return switch (mode) {
            case 0 -> client();
            case 1 -> rainbow(0.25f, 0f);
            default -> MathHelper.hsvToRgb(hue, 0.8f, 1f) & 0xFFFFFF;
        };
    }

    public static int rainbow(float speed, float offset) {
        float h = (((System.currentTimeMillis() % 100000L) / 1000f) * speed + offset) % 1f;
        return MathHelper.hsvToRgb(h, 0.6f, 1f) & 0xFFFFFF;
    }

    public static int a(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        int r = (int) ((c1 >> 16 & 255) * (1 - t) + (c2 >> 16 & 255) * t);
        int g = (int) ((c1 >> 8 & 255) * (1 - t) + (c2 >> 8 & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (r << 16) | (g << 8) | b;
    }
}
