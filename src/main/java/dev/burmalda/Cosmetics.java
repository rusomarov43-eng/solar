package dev.burmalda;

import static dev.burmalda.MB.P;

/** Smooth procedural cosmetics. Model space: head pivot at the neck, head top at h = 0.5, back is +z, front is -z. */
public final class Cosmetics {
    private static final float PI = (float) Math.PI;

    public static void hat(int id, MB b, int main, float time) {
        switch (id) {
            case 0 -> topHat(b, main);
            case 1 -> crown(b, main);
            case 2 -> halo(b, time);
            case 3 -> wizard(b, main);
            case 4 -> horns(b);
            case 5 -> catEars(b, main);
            case 6 -> bunny(b);
            case 7 -> headphones(b, main);
            default -> shades(b);
        }
    }

    private static void topHat(MB b, int main) {
        b.solid().color(0x16161C);
        b.lathe(new float[][]{
                P(0f, 0.485f, 1), P(0.42f, 0.485f, 1), P(0.42f, 0.515f, 1), P(0.275f, 0.522f, 1),
                P(0.272f, 0.53f, 0), P(0.265f, 0.75f, 0), P(0.26f, 0.98f, 1), P(0f, 0.98f, 1)}, 36);
        b.color(main);
        b.lathe(new float[][]{P(0.272f, 0.53f, 1), P(0.279f, 0.53f, 1), P(0.277f, 0.61f, 1), P(0.268f, 0.61f, 1)}, 36);
        b.color(0xE7C25A);
        b.box(-0.045f, 0.535f, -0.288f, 0.045f, 0.605f, -0.272f);
    }

    private static void crown(MB b, int main) {
        b.solid().color(0xE0B040);
        b.lathe(new float[][]{
                P(0.262f, 0.49f, 1), P(0.298f, 0.49f, 1), P(0.302f, 0.56f, 0), P(0.298f, 0.62f, 1),
                P(0.262f, 0.62f, 1), P(0.262f, 0.49f, 1)}, 40);
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * k / 8;
            float x = (float) Math.cos(a) * 0.282f, z = (float) Math.sin(a) * 0.282f;
            b.color(0xE9BD4A);
            b.lathe(new float[][]{P(0.045f, 0.62f, 1, x, z), P(0.022f, 0.70f, 0, x, z), P(0f, 0.75f, 1, x, z)}, 12);
        }
        b.color(main).glow(true);
        for (int k = 0; k < 8; k++) {
            double a = 2 * Math.PI * k / 8;
            b.ellipsoid((float) Math.cos(a) * 0.282f, 0.757f, (float) Math.sin(a) * 0.282f, 0.017f, 0.017f, 0.017f, 8);
        }
        b.ellipsoid(0f, 0.555f, -0.304f, 0.032f, 0.036f, 0.014f, 12);
        b.ellipsoid(-0.2f, 0.555f, -0.226f, 0.022f, 0.026f, 0.012f, 10);
        b.ellipsoid(0.2f, 0.555f, -0.226f, 0.022f, 0.026f, 0.012f, 10);
        b.glow(false);
    }

    private static void halo(MB b, float time) {
        float bob = (float) Math.sin(time * 2.0) * 0.012f;
        b.solid().color(0xFFF1B5).glow(true);
        b.torus(0f, 0.82f + bob, 0f, 0.30f, 0.017f, 44, 10);
        b.glass().color(0xFFE08A, 0.26f);
        b.torus(0f, 0.82f + bob, 0f, 0.30f, 0.04f, 44, 10);
        b.glow(false);
    }

    private static void wizard(MB b, int main) {
        b.solid().color(main);
        float[][] prof = new float[17][];
        prof[0] = P(0f, 0.485f, 1);
        prof[1] = P(0.46f, 0.485f, 1);
        prof[2] = P(0.46f, 0.505f, 1);
        prof[3] = P(0.31f, 0.52f, 1);
        for (int k = 0; k <= 12; k++) {
            float t = k / 12f;
            float r = 0.29f * (float) Math.pow(1f - t, 0.85);
            prof[4 + k] = P(r, 0.52f + 0.66f * t, k == 12 ? 1 : 0, 0f, 0.17f * t * t);
        }
        b.lathe(prof, 36);
        b.color(0x1B1530);
        b.lathe(new float[][]{P(0.292f, 0.52f, 1), P(0.3f, 0.52f, 1), P(0.292f, 0.585f, 1), P(0.284f, 0.585f, 1)}, 36);
        b.color(0xE7C25A);
        b.box(-0.05f, 0.525f, -0.302f, 0.05f, 0.58f, -0.286f);
    }

    private static void horns(MB b) {
        for (int s = -1; s <= 1; s += 2) {
            float[][] lo = new float[6][], hi = new float[6][];
            for (int k = 0; k < 11; k++) {
                float t = k / 10f;
                float r = 0.056f * (float) Math.pow(1f - t, 0.8) + 0.002f;
                float[] row = P(r, 0.49f + 0.30f * t - 0.05f * t * t, k == 10 ? 1 : 0, s * (0.17f + 0.14f * t * t + 0.03f * t), -0.03f * t);
                if (k <= 5) lo[k] = row;
                if (k >= 5) hi[k - 5] = row;
            }
            b.solid().color(0x40301F).lathe(lo, 14);
            b.color(0xEADFC6).lathe(hi, 14);
        }
    }

    private static void catEars(MB b, int main) {
        for (int s = -1; s <= 1; s += 2) {
            b.push();
            b.translate(s * 0.16f, 0.49f, -0.02f);
            b.rotZ(s * -16f);
            b.solid().color(main);
            b.lathe(new float[][]{P(0.085f, 0f, 1), P(0.05f, 0.09f, 0), P(0f, 0.19f, 1)}, 3 * 4);
            b.color(0xFF9FB8);
            b.lathe(new float[][]{P(0.05f, 0.005f, 1, 0f, -0.026f), P(0.03f, 0.08f, 0, 0f, -0.026f), P(0f, 0.15f, 1, 0f, -0.026f)}, 12);
            b.pop();
        }
    }

    private static void bunny(MB b) {
        for (int s = -1; s <= 1; s += 2) {
            b.push();
            b.translate(s * 0.115f, 0.49f, 0.02f);
            b.rotZ(s * -9f);
            b.solid().color(0xF3F3F7);
            b.ellipsoid(0f, 0.2f, 0f, 0.052f, 0.21f, 0.026f, 16);
            b.color(0xFFB3C6);
            b.ellipsoid(0f, 0.2f, -0.013f, 0.03f, 0.17f, 0.014f, 14);
            b.pop();
        }
    }

    private static void headphones(MB b, int main) {
        b.solid().color(0x1C1D24);
        b.arcBand(0f, 0.25f, 0f, 0.31f, 0.017f, 28, 10, 0.0, Math.PI);
        for (int s = -1; s <= 1; s += 2) {
            b.color(0x23242C).ellipsoid(s * 0.318f, 0.25f, 0f, 0.05f, 0.095f, 0.085f, 16);
            b.color(0x111217).ellipsoid(s * 0.292f, 0.25f, 0f, 0.022f, 0.075f, 0.068f, 14);
            b.color(main).glow(true).ellipsoid(s * 0.364f, 0.25f, 0f, 0.008f, 0.062f, 0.062f, 14).glow(false);
        }
    }

    private static void shades(MB b) {
        b.solid().color(0x0F0F14);
        b.box(-0.215f, 0.285f, -0.298f, 0.215f, 0.302f, -0.282f);
        b.box(-0.03f, 0.255f, -0.298f, 0.03f, 0.285f, -0.282f);
        for (int s = -1; s <= 1; s += 2) {
            b.box(s * 0.285f - 0.008f, 0.272f, -0.298f, s * 0.285f + 0.008f, 0.29f, 0.2f);
            b.box(s * 0.205f - 0.01f, 0.17f, -0.298f, s * 0.205f + 0.01f, 0.29f, -0.282f);
            b.box(s * 0.025f - 0.01f, 0.17f, -0.298f, s * 0.025f + 0.01f, 0.29f, -0.282f);
        }
        b.glass().color(0x14202E, 0.78f);
        for (int s = -1; s <= 1; s += 2) b.ellipsoid(s * 0.115f, 0.232f, -0.292f, 0.092f, 0.062f, 0.012f, 16);
    }

    // ------------------------------------------------------------------ wings
    public static void wings(MB b, int main, float time, float flapSpeed) {
        float flap = (float) Math.sin(time * 3.0 * flapSpeed) * 16f;
        for (int s = -1; s <= 1; s += 2) {
            b.push();
            b.translate(s * 0.1f, 0.34f, 0.14f);
            b.rotX(-24f);
            b.rotY(s * (-26f + flap));
            wing(b, s, main);
            b.pop();
        }
    }

    private static void wing(MB b, int s, int main) {
        float[] light = MB.mix(MB.rgb(main), new float[]{1f, 1f, 1f}, 0.78f);
        float[] tip = MB.rgb(main);
        // arm bone
        b.solid().color(0xCBBFA9);
        float[][] arm = new float[7][];
        for (int k = 0; k < 7; k++) {
            float t = k / 6f;
            arm[k] = P(0.026f * (1f - 0.55f * t), 0f, 0);
        }
        // feathers: three rows, fanned along an upward-out arm
        float[][] pts = {{0f, 0f}, {0.2f, 0.12f}, {0.42f, 0.24f}, {0.66f, 0.3f}};
        for (int row = 0; row < 3; row++) {
            int n = row == 0 ? 7 : row == 1 ? 6 : 5;
            float len0 = row == 0 ? 0.92f : row == 1 ? 0.66f : 0.42f;
            for (int i = 0; i < n; i++) {
                float t = n == 1 ? 0 : i / (float) (n - 1);
                float ax = 0.04f + 0.64f * t;
                float ay = 0.30f * (float) Math.sin(Math.min(1f, t * 1.15f) * 1.1f) * 0.9f;
                float beta = (8f + 60f * t - 10f * row) * PI / 180f;
                float dx = s * (float) Math.sin(beta), dh = -(float) Math.cos(beta);
                float len = len0 * (1.0f - 0.38f * t) * (row == 0 ? 1f : 0.95f);
                feather(b, s * ax, ay, 0.004f * row, dx, dh, len, 0.075f - 0.012f * row,
                        MB.mix(light, tip, row == 0 ? 0.55f : 0.25f + 0.2f * t), tip);
            }
        }
        // arm as a couple of fat segments
        b.solid().color(0xD8CCB6);
        for (int k = 0; k < 3; k++) {
            float x0 = k * 0.22f, x1 = (k + 1) * 0.22f;
            b.box(s > 0 ? x0 : -x1, 0.0f + 0.1f * k, -0.012f, s > 0 ? x1 : -x0, 0.035f + 0.1f * (k + 1) * 0.9f, 0.014f);
        }
    }

    /** One feather: leaf-shaped ribbon in the wing plane, slightly curved out of it. */
    private static void feather(MB b, float x, float h, float z, float dx, float dh, float len, float w,
                                float[] cBase, float[] cTip) {
        int n = 7;
        float px = -dh, ph = dx; // in-plane perpendicular
        float pl = (float) Math.sqrt(px * px + ph * ph);
        px /= pl; ph /= pl;
        float[] last = null;
        float lx0 = 0, lh0 = 0, lz0 = 0, lx1 = 0, lh1 = 0, lz1 = 0;
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n;
            float ww = w * (float) Math.pow(Math.sin(Math.PI * (0.18 + 0.82 * t) * 0.97), 0.75) * (1f - 0.25f * t);
            float cx = x + dx * len * t, ch = h + dh * len * t, cz = z + 0.07f * t * t;
            float x0 = cx + px * ww, h0 = ch + ph * ww, x1 = cx - px * ww, h1 = ch - ph * ww;
            float[] col = MB.mix(cBase, cTip, t);
            if (last != null) {
                b.quadC(lx0, lh0, lz0, x0, h0, cz, x1, h1, cz, lx1, lh1, lz1, last, col, col, last);
            }
            lx0 = x0; lh0 = h0; lz0 = cz; lx1 = x1; lh1 = h1; lz1 = cz; last = col;
        }
    }

    // ------------------------------------------------------------------ cape (cloth)
    public static void cape(MB b, int main, float time) {
        int rows = 14, cols = 10;
        float width = 0.27f, length = 0.98f;
        float[] top = MB.rgb(main), bottom = MB.mix(MB.rgb(main), new float[]{0.04f, 0.04f, 0.08f}, 0.65f);
        float[][][] g = new float[rows + 1][cols + 1][3];
        for (int i = 0; i <= rows; i++) {
            float v = i / (float) rows;
            for (int j = 0; j <= cols; j++) {
                float u = j / (float) cols * 2f - 1f;
                float x = u * width * (1f + 0.22f * v);
                float y = v * length;
                float z = 0.012f + 0.045f * (1f - u * u) * v
                        + (float) Math.sin(v * 7.0 + time * 2.2 + u * 2.2) * 0.014f * v;
                g[i][j] = new float[]{x, y, z};
            }
        }
        b.solid();
        for (int i = 0; i < rows; i++) {
            float[] c0 = MB.mix(top, bottom, i / (float) rows), c1 = MB.mix(top, bottom, (i + 1) / (float) rows);
            for (int j = 0; j < cols; j++) {
                b.quad(g[i][j], g[i][j + 1], g[i + 1][j + 1], g[i + 1][j], c0, c0, c1, c1);
            }
        }
        // gold trim along the bottom edge and shoulders
        b.color(0xE0B040);
        for (int j = 0; j < cols; j++) {
            float[] a = g[rows][j], c = g[rows][j + 1];
            b.quad(new float[]{a[0], a[1] - 0.03f, a[2]}, new float[]{c[0], c[1] - 0.03f, c[2]},
                    new float[]{c[0], c[1] + 0.008f, c[2] + 0.004f}, new float[]{a[0], a[1] + 0.008f, a[2] + 0.004f},
                    new float[]{0.88f, 0.69f, 0.25f}, new float[]{0.88f, 0.69f, 0.25f},
                    new float[]{0.88f, 0.69f, 0.25f}, new float[]{0.88f, 0.69f, 0.25f});
        }
        b.color(0xE0B040).ellipsoid(-width, -0.005f, 0.0f, 0.03f, 0.03f, 0.03f, 10);
        b.ellipsoid(width, -0.005f, 0.0f, 0.03f, 0.03f, 0.03f, 10);
    }

    private Cosmetics() { }
}
