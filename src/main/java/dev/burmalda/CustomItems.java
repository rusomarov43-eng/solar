package dev.burmalda;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

/** Procedural 3D knife / katana drawn instead of the held sword. Only for your own player, only on your screen. */
public final class CustomItems {
    private static final float[] STEEL = MB.rgb(0xC9D2DB);

    public static boolean wants(ItemStack stack) {
        Feature f = Modules.CUSTOM_ITEMS;
        if (!f.enabled) return false;
        Item it = stack.getItem();
        return switch (f.m("Replace")) {
            case 0 -> it instanceof SwordItem;
            case 1 -> it == Items.IRON_SWORD;
            case 2 -> it == Items.DIAMOND_SWORD;
            case 3 -> it == Items.NETHERITE_SWORD;
            default -> it == Items.STICK;
        };
    }

    /** Matrices are already in item model space ([0,1] cube, y up). */
    public static void render(MatrixStack m, VertexConsumerProvider vcp, int light) {
        Feature f = Modules.CUSTOM_ITEMS;
        MB b = new MB(m, vcp, light, 1f);
        int accent = Colors.client();
        m.push();
        m.translate(0.5f, 0.5f, 0.5f);
        b.rotZ(-45f);
        b.scale(1.18f);
        if (f.m("Model") == 0) {
            m.translate(0f, -0.065f, 0f);
            knife(b, accent, f.b("Glow inlay"));
        } else {
            m.translate(0f, -0.21f, 0f);
            katana(b, accent, f.b("Glow inlay"));
        }
        m.pop();
    }

    private static void knife(MB b, int accent, boolean glow) {
        b.solid().color(0x2B1B13);
        b.lathe(new float[][]{MB.P(0f, -0.285f, 1), MB.P(0.027f, -0.285f, 1), MB.P(0.032f, -0.265f), MB.P(0.035f, -0.2f),
                MB.P(0.033f, -0.13f), MB.P(0.028f, -0.064f, 1), MB.P(0f, -0.064f, 1)}, 20);
        b.color(0x8E8E96);
        b.lathe(new float[][]{MB.P(0.0365f, -0.218f, 1), MB.P(0.0365f, -0.206f, 1)}, 20);
        b.lathe(new float[][]{MB.P(0.0345f, -0.148f, 1), MB.P(0.0345f, -0.136f, 1)}, 20);
        b.color(0x9AA3AD).ellipsoid(0f, -0.3f, 0f, 0.038f, 0.03f, 0.038f, 14);
        b.color(0xB8C0CA).box(-0.06f, -0.064f, -0.017f, 0.06f, -0.034f, 0.017f);
        b.color(accent).glow(glow);
        b.box(-0.045f, -0.058f, 0.0165f, 0.045f, -0.04f, 0.0185f);
        b.box(-0.045f, -0.058f, -0.0185f, 0.045f, -0.04f, -0.0165f);
        b.glow(false);
        blade(b, -0.034f, 0.46f, 16, 0.034f, 0.0095f, 0.02f, 0.5f);
    }

    private static void katana(MB b, int accent, boolean glow) {
        b.solid().color(0x17171B);
        b.lathe(new float[][]{MB.P(0f, -0.28f, 1), MB.P(0.024f, -0.28f, 1), MB.P(0.025f, -0.03f, 1), MB.P(0f, -0.03f, 1)}, 16);
        for (int k = 0; k < 8; k++) {
            float h0 = -0.27f + k * 0.03f;
            b.color(k % 2 == 0 ? accent : 0x23232B).glow(glow && k % 2 == 0);
            b.lathe(new float[][]{MB.P(0.0262f, h0, 1), MB.P(0.0262f, h0 + 0.0295f, 1)}, 16);
        }
        b.glow(false);
        b.color(0xB08A3A).ellipsoid(0f, -0.285f, 0f, 0.03f, 0.014f, 0.03f, 12);
        b.color(0xC09A44).lathe(new float[][]{MB.P(0f, -0.034f, 1), MB.P(0.058f, -0.034f, 1), MB.P(0.058f, -0.024f, 1), MB.P(0f, -0.024f, 1)}, 28);
        blade(b, -0.024f, 0.70f, 22, 0.022f, 0.0075f, 0.045f, 0.9f);
    }

    /**
     * Single-edged blade strip along +h. Cross-section: edge (+x), two bevels, flat back (-x).
     * bend = sideways tip sweep, taper = how fast the width falls off near the tip.
     */
    private static void blade(MB b, float h0, float h1, int n, float halfW, float thick, float bend, float taper) {
        b.solid().color(0xFFFFFF);
        float[][][] st = new float[n + 1][5][];
        float[] center;
        float[][] cols = {MB.mix(STEEL, new float[]{1, 1, 1}, 0.55f), MB.mix(STEEL, new float[]{1, 1, 1}, 0.15f),
                MB.mix(STEEL, new float[]{0, 0, 0}, 0.30f), MB.mix(STEEL, new float[]{0, 0, 0}, 0.30f),
                MB.mix(STEEL, new float[]{1, 1, 1}, 0.15f)};
        float[][] ctr = new float[n + 1][];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n;
            float h = h0 + (h1 - h0) * t;
            float w = halfW * (float) Math.pow(Math.max(0.0, 1.0 - Math.pow(t, 2.0 + 3.0 * taper)), 0.55);
            float th = thick * (1f - 0.55f * t);
            float xo = bend * t * t * t;
            ctr[i] = new float[]{xo, h, 0f};
            st[i][0] = new float[]{xo + w, h, 0f};
            st[i][1] = new float[]{xo + w * 0.35f, h, th};
            st[i][2] = new float[]{xo - w, h, th * 0.9f};
            st[i][3] = new float[]{xo - w, h, -th * 0.9f};
            st[i][4] = new float[]{xo + w * 0.35f, h, -th};
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < 5; k++) {
                int k2 = (k + 1) % 5;
                float[] a = st[i][k], bb = st[i][k2], c = st[i + 1][k2], d = st[i + 1][k];
                float[] cen = {(ctr[i][0] + ctr[i + 1][0]) / 2, (ctr[i][1] + ctr[i + 1][1]) / 2, 0f};
                emit(b, a, bb, c, d, cen, cols[k], cols[k2], cols[k2], cols[k]);
            }
        }
        // tip cap and base cap
        for (int k = 0; k < 5; k++) {
            int k2 = (k + 1) % 5;
            float[] a = st[0][k], bb = st[0][k2];
            float[] c0 = ctr[0];
            emit(b, a, bb, c0, c0, new float[]{c0[0], c0[1] + 1f, 0f}, cols[k], cols[k2], cols[k2], cols[k]);
        }
    }

    /** Emit a quad, flipping the winding so the normal points away from 'center'. */
    private static void emit(MB b, float[] a, float[] bb, float[] c, float[] d, float[] center,
                             float[] ca, float[] cb, float[] cc, float[] cd) {
        float ux = bb[0] - a[0], uy = bb[1] - a[1], uz = bb[2] - a[2];
        float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float mx = (a[0] + bb[0] + c[0] + d[0]) / 4 - center[0];
        float my = (a[1] + bb[1] + c[1] + d[1]) / 4 - center[1];
        float mz = (a[2] + bb[2] + c[2] + d[2]) / 4 - center[2];
        if (nx * mx + ny * my + nz * mz >= 0) {
            b.quadC(a[0], a[1], a[2], bb[0], bb[1], bb[2], c[0], c[1], c[2], d[0], d[1], d[2], ca, cb, cc, cd);
        } else {
            b.quadC(a[0], a[1], a[2], d[0], d[1], d[2], c[0], c[1], c[2], bb[0], bb[1], bb[2], ca, cd, cc, cb);
        }
    }

    private CustomItems() { }
}
