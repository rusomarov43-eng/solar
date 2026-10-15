package dev.burmalda;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Expanding ground rings: jump circles and hit waves. */
public final class Rings {
    private static final class Ring {
        double x, y, z; long born, life; float size; int mode;
    }

    private static final List<Ring> LIST = new ArrayList<>();
    private static boolean wasOnGround = true;

    public static void add(double x, double y, double z, int colorMode, float size, long lifeMs) {
        Ring r = new Ring();
        r.x = x; r.y = y; r.z = z; r.mode = colorMode; r.size = size; r.life = lifeMs;
        r.born = System.currentTimeMillis();
        LIST.add(r);
        if (LIST.size() > 40) LIST.remove(0);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) { wasOnGround = true; LIST.clear(); return; }
        boolean on = mc.player.isOnGround();
        Feature j = Modules.JUMP;
        if (j.enabled && wasOnGround && !on && mc.player.getVelocity().y > 0.1) {
            add(mc.player.getX(), mc.player.getY(), mc.player.getZ(), j.m("Color"), j.f("Size"), (long) (j.f("Time") * 1000));
        }
        wasOnGround = on;
    }

    public static void render(WorldRenderContext ctx) {
        if (LIST.isEmpty()) return;
        long now = System.currentTimeMillis();
        LIST.removeIf(r -> now - r.born > r.life);
        if (LIST.isEmpty()) return;

        Vec3d cp = ctx.camera().getPos();
        Matrix4f mat = ctx.matrixStack().peek().getPositionMatrix();
        BufferBuilder bb = WorldDraw.begin();
        for (Ring r : LIST) {
            float t = (now - r.born) / (float) r.life;
            float ease = 1f - (1f - t) * (1f - t);
            float rad = 0.25f + ease * r.size * 1.8f;
            float w = 0.1f + 0.3f * (1f - t);
            int rgb = r.mode == 0 ? Colors.client() : Colors.rainbow(0.3f, (r.born % 1000) / 1000f);
            int a = (int) (230 * (1f - t));
            WorldDraw.flatRing(bb, mat, r.x - cp.x, r.y - cp.y + 0.02, r.z - cp.z, Math.max(0f, rad - w), rad, rgb, 0, a);
        }
        WorldDraw.end();
    }

    private Rings() { }
}
