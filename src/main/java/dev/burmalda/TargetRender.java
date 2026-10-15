package dev.burmalda;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Glowing ring around your current target. Depth-tested: never visible through walls. */
public final class TargetRender {
    public static void render(WorldRenderContext ctx) {
        Feature f = Modules.TARGET_ESP;
        LivingEntity t = TargetTracker.target;
        if (!f.enabled || t == null || t.isRemoved()) return;

        long now = System.currentTimeMillis();
        float fade = MathHelper.clamp((3000 - (now - TargetTracker.lastSeen)) / 500f, 0f, 1f);
        if (fade <= 0f) return;

        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        float td = ctx.tickDelta();
        double ex = MathHelper.lerp(td, t.prevX, t.getX()) - cp.x;
        double ey = MathHelper.lerp(td, t.prevY, t.getY()) - cp.y;
        double ez = MathHelper.lerp(td, t.prevZ, t.getZ()) - cp.z;

        float time = now / 1000f * f.f("Speed");
        float size = f.f("Size");
        int rgb = f.b("Client color") ? Colors.client() : 0xFFFFFF;

        Matrix4f mat = ctx.matrixStack().peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (f.m("Style") == 0) {
            // billboard ring: 4 rotating arcs, pushed a bit towards the camera so it sits in front of the body
            double cx = ex, cy = ey + t.getHeight() * 0.5, cz = ez;
            double len = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (len > 0.001) { cx -= cx / len * 0.6; cy -= cy / len * 0.6; cz -= cz / len * 0.6; }
            Vector3f right = new Vector3f(1, 0, 0).rotate(cam.getRotation());
            Vector3f up = new Vector3f(0, 1, 0).rotate(cam.getRotation());
            float rad = Math.max(t.getWidth() * 1.2f, t.getHeight() * 0.5f) * size * (1f + 0.04f * (float) Math.sin(time * 3));
            float spin = time * 2f;
            float[] widths = {0.26f, 0.18f, 0.1f};
            int[] alphas = {20, 45, 230};
            for (int l = 0; l < 3; l++) {
                for (int k = 0; k < 4; k++) {
                    float a0 = spin + k * (float) (Math.PI / 2);
                    arc(bb, mat, cx, cy, cz, right, up, a0, a0 + 0.95f,
                            rad - widths[l] / 2, rad + widths[l] / 2, rgb, (int) (alphas[l] * fade));
                }
            }
        } else {
            // horizontal ring sliding up and down the body
            float ph = (float) (Math.sin(time * 1.5) * 0.5 + 0.5);
            double cy = ey + ph * t.getHeight();
            Vector3f a = new Vector3f(1, 0, 0), b = new Vector3f(0, 0, 1);
            float rad = Math.max(0.5f, t.getWidth() * 0.9f) * size;
            float[] widths = {0.14f, 0.09f, 0.05f};
            int[] alphas = {20, 50, 230};
            for (int l = 0; l < 3; l++) {
                arc(bb, mat, ex, cy, ez, a, b, 0f, (float) (Math.PI * 2),
                        rad - widths[l] / 2, rad + widths[l] / 2, rgb, (int) (alphas[l] * fade));
            }
        }

        tess.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void arc(BufferBuilder bb, Matrix4f m, double cx, double cy, double cz,
                            Vector3f a, Vector3f b, float a0, float a1, float r0, float r1, int rgb, int alpha) {
        int steps = Math.max(2, (int) ((a1 - a0) / 0.12f));
        float da = (a1 - a0) / steps;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255;
        for (int i = 0; i < steps; i++) {
            float s0 = a0 + i * da, s1 = s0 + da;
            float c0 = (float) Math.cos(s0), n0 = (float) Math.sin(s0);
            float c1 = (float) Math.cos(s1), n1 = (float) Math.sin(s1);
            vert(bb, m, cx, cy, cz, a, b, c0 * r0, n0 * r0, r, g, bl, alpha);
            vert(bb, m, cx, cy, cz, a, b, c0 * r1, n0 * r1, r, g, bl, alpha);
            vert(bb, m, cx, cy, cz, a, b, c1 * r1, n1 * r1, r, g, bl, alpha);
            vert(bb, m, cx, cy, cz, a, b, c1 * r0, n1 * r0, r, g, bl, alpha);
        }
    }

    private static void vert(BufferBuilder bb, Matrix4f m, double cx, double cy, double cz,
                             Vector3f a, Vector3f b, float u, float w, int r, int g, int bl, int alpha) {
        bb.vertex(m,
                (float) (cx + a.x * u + b.x * w),
                (float) (cy + a.y * u + b.y * w),
                (float) (cz + a.z * u + b.z * w)).color(r, g, bl, alpha).next();
    }

    private TargetRender() { }
}
