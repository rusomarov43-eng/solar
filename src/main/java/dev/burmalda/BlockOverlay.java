package dev.burmalda;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

/** Colored animated outline (+ optional fill) on the block you look at. Purely visual. */
public final class BlockOverlay {
    public static boolean onOutline(WorldRenderContext ctx, WorldRenderContext.BlockOutlineContext oc) {
        Feature m = Modules.BLOCK_OVERLAY;
        if (!m.enabled || ctx.consumers() == null) return true; // true = vanilla outline

        float t = System.currentTimeMillis() / 1000f * m.f("Shader speed");
        int base = m.b("Client color") ? Colors.client() : 0xFFFFFF;
        int rgb = switch (m.m("Shader")) {
            case 0 -> Colors.lerp(base, 0xFFFFFF, (float) (Math.sin(t * 2.0) * 0.5 + 0.5));
            case 1 -> MathHelper.hsvToRgb((t * 0.2f) % 1f, 0.6f, 1f);
            default -> base;
        };
        float alpha = m.f("Shader opacity");
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;

        VoxelShape shape = oc.blockState().getOutlineShape(ctx.world(), oc.blockPos(), ShapeContext.of(oc.entity()));
        if (shape.isEmpty()) return true;
        double ox = oc.blockPos().getX() - oc.cameraX();
        double oy = oc.blockPos().getY() - oc.cameraY();
        double oz = oc.blockPos().getZ() - oc.cameraZ();

        if (m.b("Fill")) {
            VertexConsumer fv = ctx.consumers().getBuffer(RenderLayer.getDebugFilledBox());
            Box bx = shape.getBoundingBox().offset(ox, oy, oz).expand(0.002);
            WorldRenderer.renderFilledBox(ctx.matrixStack(), fv,
                    bx.minX, bx.minY, bx.minZ, bx.maxX, bx.maxY, bx.maxZ,
                    r, g, b, m.f("Fill opacity"));
        }

        VertexConsumer vc = ctx.consumers().getBuffer(RenderLayer.getLines());
        WorldRenderer.drawShapeOutline(ctx.matrixStack(), vc, shape, ox, oy, oz, r, g, b, alpha, false);
        return false;
    }

    private BlockOverlay() { }
}
