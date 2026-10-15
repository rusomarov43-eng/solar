package dev.burmalda;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

/** Floating red "-1.00" numbers above entities you hit. */
public final class DamageParticles {
    public static void render(WorldRenderContext ctx) {
        Feature f = Modules.DAMAGE;
        if (!f.enabled || TargetTracker.DMG.isEmpty() || ctx.consumers() == null) return;

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        long now = System.currentTimeMillis();
        float life = f.f("Time") * 1000f;
        TargetTracker.DMG.removeIf(d -> now - d.born > life);

        MatrixStack m = ctx.matrixStack();
        for (TargetTracker.Dmg d : TargetTracker.DMG) {
            float t = (now - d.born) / life;
            int alpha = (int) (255 * (1f - t * t));
            if (alpha < 8) continue;

            m.push();
            m.translate(d.pos.x - cp.x, d.pos.y + t * 0.8 - cp.y, d.pos.z - cp.z);
            m.multiply(cam.getRotation());
            float s = 0.03f * f.f("Size");
            m.scale(-s, -s, s);

            int rgb = f.b("Client color") ? Colors.client() : 0xFF3030;
            tr.draw(d.text, -tr.getWidth(d.text) / 2f, 0, Colors.a(rgb, alpha), false,
                    m.peek().getPositionMatrix(), ctx.consumers(),
                    TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            m.pop();
        }
    }

    private DamageParticles() { }
}
