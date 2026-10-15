package dev.burmalda;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Procedural mesh builder: smooth surfaces of revolution, tori, ellipsoids, boxes and ribbons with real normals.
 * Shapes are described with "h" = height UP. ys = -1 for the player model space (y points down), +1 for items.
 * Call solid() / glass() before emitting shapes of that kind (switching layers flushes the previous one).
 */
public final class MB {
    private static final Identifier TEX = new Identifier(Brand.ID, "textures/hat.png");
    private final MatrixStack m;
    private final VertexConsumerProvider vcp;
    private final int light;
    public final float ys;
    private VertexConsumer vc;
    private float cr = 1, cg = 1, cb = 1, ca = 1;
    private boolean emissive;

    public MB(MatrixStack m, VertexConsumerProvider vcp, int light, float ys) {
        this.m = m; this.vcp = vcp; this.light = light; this.ys = ys;
        solid();
    }

    public MB solid() { vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX)); ca = 1f; return this; }
    public MB glass() { vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEX)); return this; }
    public MB color(int rgb) { return color(rgb, 1f); }
    public MB color(int rgb, float a) {
        cr = (rgb >> 16 & 255) / 255f; cg = (rgb >> 8 & 255) / 255f; cb = (rgb & 255) / 255f; ca = a; return this;
    }
    public MB glow(boolean on) { emissive = on; return this; }

    public void push() { m.push(); }
    public void pop() { m.pop(); }
    public void translate(float x, float h, float z) { m.translate(x, ys * h, z); }
    public void rotX(float deg) { m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(deg)); }
    public void rotY(float deg) { m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(deg)); }
    public void rotZ(float deg) { m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(deg)); }
    public void scale(float s) { m.scale(s, s, s); }

    // ---------------------------------------------------------------- vertex
    private void vert(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b) {
        MatrixStack.Entry e = m.peek();
        vc.vertex(e.getPositionMatrix(), x, y, z)
                .color(Math.min(1f, r), Math.min(1f, g), Math.min(1f, b), ca)
                .texture(0.5f, 0.5f).overlay(OverlayTexture.DEFAULT_UV)
                .light(emissive ? 0xF000F0 : light)
                .normal(e.getNormalMatrix(), nx, ny, nz).next();
    }

    private void pv(float x, float h, float z, float nx, float nh, float nz, float k) {
        vert(x, ys * h, z, nx, ys * nh, nz, cr * k, cg * k, cb * k);
    }

    private static float shade(float nh) { return 0.82f + 0.18f * nh; }

    // ---------------------------------------------------------------- profile helpers
    public static float[] P(float r, float h) { return new float[]{r, h, 0, 0, 0}; }
    public static float[] P(float r, float h, float sharp) { return new float[]{r, h, sharp, 0, 0}; }
    public static float[] P(float r, float h, float sharp, float ox, float oz) { return new float[]{r, h, sharp, ox, oz}; }

    /** Surface of revolution around the vertical axis. Rows: {r, h, sharp, ox, oz}, bottom to top, outside = +r. */
    public void lathe(float[][] p, int seg) {
        int n = p.length;
        if (n < 2) return;
        float[] snr = new float[n - 1], snh = new float[n - 1];
        for (int i = 0; i < n - 1; i++) {
            float dr = p[i + 1][0] - p[i][0], dh = p[i + 1][1] - p[i][1];
            float len = (float) Math.sqrt(dr * dr + dh * dh);
            if (len < 1e-6f) len = 1f;
            snr[i] = dh / len; snh[i] = -dr / len;
        }
        for (int i = 0; i < n - 1; i++) {
            float[] a = vn(p, snr, snh, i, i), b = vn(p, snr, snh, i + 1, i);
            for (int j = 0; j < seg; j++) {
                double t0 = 2 * Math.PI * j / seg, t1 = 2 * Math.PI * (j + 1) / seg;
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                ring(p[i], a, c0, s0); ring(p[i + 1], b, c0, s0);
                ring(p[i + 1], b, c1, s1); ring(p[i], a, c1, s1);
            }
        }
    }

    private void ring(float[] q, float[] nrm, float c, float s) {
        pv(q[3] + q[0] * c, q[1], q[4] + q[0] * s, nrm[0] * c, nrm[1], nrm[0] * s, shade(nrm[1]));
    }

    private static float[] vn(float[][] p, float[] snr, float[] snh, int k, int seg) {
        float nr = snr[seg], nh = snh[seg];
        if (p[k][2] < 0.5f) {
            int other = (k == seg) ? seg - 1 : seg + 1;
            if (other >= 0 && other < snr.length) { nr += snr[other]; nh += snh[other]; }
        }
        float l = (float) Math.sqrt(nr * nr + nh * nh);
        if (l < 1e-6f) l = 1f;
        return new float[]{nr / l, nh / l};
    }

    /** Horizontal torus around the vertical axis through (cx, cz). */
    public void torus(float cx, float h, float cz, float R, float r, int segU, int segV) {
        for (int i = 0; i < segU; i++) {
            double u0 = 2 * Math.PI * i / segU, u1 = 2 * Math.PI * (i + 1) / segU;
            for (int j = 0; j < segV; j++) {
                double v0 = 2 * Math.PI * j / segV, v1 = 2 * Math.PI * (j + 1) / segV;
                tp(cx, h, cz, R, r, u0, v0); tp(cx, h, cz, R, r, u1, v0);
                tp(cx, h, cz, R, r, u1, v1); tp(cx, h, cz, R, r, u0, v1);
            }
        }
    }

    private void tp(float cx, float h, float cz, float R, float r, double u, double v) {
        float cu = (float) Math.cos(u), su = (float) Math.sin(u), cv = (float) Math.cos(v), sv = (float) Math.sin(v);
        pv(cx + (R + r * cv) * cu, h + r * sv, cz + (R + r * cv) * su, cv * cu, sv, cv * su, shade(sv));
    }

    /** Vertical torus arc (in the XY plane, like a headphone band). Angles in radians, 0 = +x, pi/2 = up. */
    public void arcBand(float cx, float ch, float cz, float R, float r, int segU, int segV, double a0, double a1) {
        for (int i = 0; i < segU; i++) {
            double u0 = a0 + (a1 - a0) * i / segU, u1 = a0 + (a1 - a0) * (i + 1) / segU;
            for (int j = 0; j < segV; j++) {
                double v0 = 2 * Math.PI * j / segV, v1 = 2 * Math.PI * (j + 1) / segV;
                ap(cx, ch, cz, R, r, u0, v0); ap(cx, ch, cz, R, r, u1, v0);
                ap(cx, ch, cz, R, r, u1, v1); ap(cx, ch, cz, R, r, u0, v1);
            }
        }
    }

    private void ap(float cx, float ch, float cz, float R, float r, double u, double v) {
        float nx = (float) Math.cos(u), nh = (float) Math.sin(u);
        float cv = (float) Math.cos(v), sv = (float) Math.sin(v);
        float ex = nx * cv, eh = nh * cv, ez = sv;
        pv(cx + R * nx + r * ex, ch + R * nh + r * eh, cz + r * ez, ex, eh, ez, shade(eh));
    }

    public void ellipsoid(float cx, float h, float cz, float rx, float rh, float rz, int seg) {
        int rings = Math.max(4, seg / 2);
        for (int i = 0; i < rings; i++) {
            double t0 = Math.PI * i / rings, t1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < seg; j++) {
                double p0 = 2 * Math.PI * j / seg, p1 = 2 * Math.PI * (j + 1) / seg;
                ep(cx, h, cz, rx, rh, rz, t0, p0); ep(cx, h, cz, rx, rh, rz, t1, p0);
                ep(cx, h, cz, rx, rh, rz, t1, p1); ep(cx, h, cz, rx, rh, rz, t0, p1);
            }
        }
    }

    private void ep(float cx, float h, float cz, float rx, float rh, float rz, double t, double p) {
        float st = (float) Math.sin(t), ct = (float) Math.cos(t);
        float x = st * (float) Math.cos(p), z = st * (float) Math.sin(p), up = ct;
        float nx = x / rx, nh = up / rh, nz = z / rz;
        float l = (float) Math.sqrt(nx * nx + nh * nh + nz * nz);
        if (l < 1e-6f) l = 1f;
        pv(cx + rx * x, h + rh * up, cz + rz * z, nx / l, nh / l, nz / l, shade(nh / l));
    }

    // ---------------------------------------------------------------- flat quads
    public void quad(float[] a, float[] b, float[] c, float[] d, float[] ca_, float[] cb_, float[] cc_, float[] cd_) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = d[0] - a[0], vy = d[1] - a[1], vz = d[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return;
        nx /= l; ny /= l; nz /= l;
        float k = 0.86f + 0.14f * (-ny * ys);
        vert(a[0], a[1], a[2], nx, ny, nz, ca_[0] * k, ca_[1] * k, ca_[2] * k);
        vert(b[0], b[1], b[2], nx, ny, nz, cb_[0] * k, cb_[1] * k, cb_[2] * k);
        vert(c[0], c[1], c[2], nx, ny, nz, cc_[0] * k, cc_[1] * k, cc_[2] * k);
        vert(d[0], d[1], d[2], nx, ny, nz, cd_[0] * k, cd_[1] * k, cd_[2] * k);
    }

    /** Quad in (x, h, z) coordinates with the current color. */
    public void quadH(float ax, float ah, float az, float bx, float bh, float bz,
                      float cx, float ch, float cz, float dx, float dh, float dz) {
        float[] col = {cr, cg, cb};
        quad(new float[]{ax, ys * ah, az}, new float[]{bx, ys * bh, bz}, new float[]{cx, ys * ch, cz}, new float[]{dx, ys * dh, dz},
                col, col, col, col);
    }

    /** Quad with per-vertex rgb colors (each {r,g,b} 0..1). */
    public void quadC(float ax, float ah, float az, float bx, float bh, float bz,
                      float cx, float ch, float cz, float dx, float dh, float dz,
                      float[] c0, float[] c1, float[] c2, float[] c3) {
        quad(new float[]{ax, ys * ah, az}, new float[]{bx, ys * bh, bz}, new float[]{cx, ys * ch, cz}, new float[]{dx, ys * dh, dz},
                c0, c1, c2, c3);
    }

    public void box(float x0, float h0, float z0, float x1, float h1, float z1) {
        quadH(x0, h1, z0, x1, h1, z0, x1, h1, z1, x0, h1, z1); // top
        quadH(x0, h0, z1, x1, h0, z1, x1, h0, z0, x0, h0, z0); // bottom
        quadH(x0, h0, z0, x1, h0, z0, x1, h1, z0, x0, h1, z0); // front
        quadH(x1, h0, z1, x0, h0, z1, x0, h1, z1, x1, h1, z1); // back
        quadH(x0, h0, z1, x0, h0, z0, x0, h1, z0, x0, h1, z1); // left
        quadH(x1, h0, z0, x1, h0, z1, x1, h1, z1, x1, h1, z0); // right
    }

    public static float[] rgb(int c) { return new float[]{(c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f}; }
    public static float[] mix(float[] a, float[] b, float t) {
        return new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }
}
