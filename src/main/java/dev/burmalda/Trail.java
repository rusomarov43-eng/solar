package dev.burmalda;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Glowing ribbon on the ground behind you. */
public final class Trail {
    private static final List<Vec3d> PTS = new ArrayList<>();

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || !Modules.TRAIL.enabled) { PTS.clear(); return; }
        Vec3d p = mc.player.getPos();
        boolean moved = false;
        if (!PTS.isEmpty()) {
            double d = PTS.get(PTS.size() - 1).squaredDistanceTo(p);
            if (d > 100) PTS.clear();
        }
        if (PTS.isEmpty() || PTS.get(PTS.size() - 1).squaredDistanceTo(p) > 0.0025) { PTS.add(p); moved = true; }
        if (!moved && !PTS.isEmpty()) PTS.remove(0);
        int max = (int) Modules.TRAIL.f("Length");
        while (PTS.size() > max) PTS.remove(0);
    }

    public static void render(WorldRenderContext ctx) {
        Feature f = Modules.TRAIL;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!f.enabled || mc.player == null || PTS.isEmpty()) return;

        float td = ctx.tickDelta();
        List<Vec3d> pts = new ArrayList<>(PTS);
        pts.add(new Vec3d(
                MathHelper.lerp(td, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(td, mc.player.prevY, mc.player.getY()),
                MathHelper.lerp(td, mc.player.prevZ, mc.player.getZ())));
        int n = pts.size();
        if (n < 2) return;

        Vec3d cp = ctx.camera().getPos();
        Matrix4f mat = ctx.matrixStack().peek().getPositionMatrix();
        double w = f.f("Width") / 2.0;
        BufferBuilder bb = WorldDraw.begin();
        for (int i = 1; i < n; i++) {
            Vec3d a = pts.get(i - 1), b = pts.get(i);
            double dx = b.x - a.x, dz = b.z - a.z;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1e-4) continue;
            double px = -dz / len * w, pz = dx / len * w;
            float t0 = (i - 1) / (float) (n - 1), t1 = i / (float) (n - 1);
            int c0 = f.m("Color") == 0 ? Colors.client() : Colors.rainbow(0.2f, t0 * 0.6f);
            int c1 = f.m("Color") == 0 ? Colors.client() : Colors.rainbow(0.2f, t1 * 0.6f);
            int a0 = (int) (200 * t0), a1 = (int) (200 * t1);
            double y0 = a.y - cp.y + 0.03, y1 = b.y - cp.y + 0.03;
            WorldDraw.v(bb, mat, a.x - cp.x + px, y0, a.z - cp.z + pz, c0, a0);
            WorldDraw.v(bb, mat, a.x - cp.x - px, y0, a.z - cp.z - pz, c0, a0);
            WorldDraw.v(bb, mat, b.x - cp.x - px, y1, b.z - cp.z - pz, c1, a1);
            WorldDraw.v(bb, mat, b.x - cp.x + px, y1, b.z - cp.z + pz, c1, a1);
        }
        WorldDraw.end();
    }

    private Trail() { }
}
