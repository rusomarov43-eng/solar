package dev.burmalda;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/** Tiny helper for translucent colored quads in the world (depth-tested, no depth write). */
public final class WorldDraw {
    public static BufferBuilder begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        return bb;
    }

    public static void end() {
        Tessellator.getInstance().draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    public static void v(BufferBuilder bb, Matrix4f m, double x, double y, double z, int rgb, int alpha) {
        bb.vertex(m, (float) x, (float) y, (float) z)
                .color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, Math.max(0, Math.min(255, alpha))).next();
    }

    /** Flat ring in the XZ plane. Coordinates are relative to the camera. */
    public static void flatRing(BufferBuilder bb, Matrix4f m, double cx, double cy, double cz,
                                float r0, float r1, int rgb, int aInner, int aOuter) {
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a0 = i * 2 * Math.PI / seg, a1 = (i + 1) * 2 * Math.PI / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            v(bb, m, cx + c0 * r0, cy, cz + s0 * r0, rgb, aInner);
            v(bb, m, cx + c0 * r1, cy, cz + s0 * r1, rgb, aOuter);
            v(bb, m, cx + c1 * r1, cy, cz + s1 * r1, rgb, aOuter);
            v(bb, m, cx + c1 * r0, cy, cz + s1 * r0, rgb, aInner);
        }
    }

    private WorldDraw() { }
}
