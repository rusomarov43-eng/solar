package dev.burmalda;

import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Glowing trail behind the cursor + click ripples (menu and main menu). */
public final class MouseTrail {
    private static final List<double[]> PTS = new ArrayList<>();   // x, y, time
    private static final List<double[]> RIP = new ArrayList<>();   // x, y, time
    private static double lx = -10000, ly = -10000;

    public static void update(double mx, double my) {
        long now = System.currentTimeMillis();
        if (lx < -5000) { lx = mx; ly = my; }
        double dx = mx - lx, dy = my - ly, d = Math.sqrt(dx * dx + dy * dy);
        if (d >= 1) {
            int n = (int) Math.min(40, Math.max(1, d / 2));
            for (int i = 1; i <= n; i++) {
                double t = i / (double) n;
                PTS.add(new double[]{lx + dx * t, ly + dy * t, now});
            }
            lx = mx; ly = my;
        }
        PTS.removeIf(p -> now - p[2] > 450);
        RIP.removeIf(p -> now - p[2] > 550);
        while (PTS.size() > 220) PTS.remove(0);
    }

    public static void click(double mx, double my) {
        RIP.add(new double[]{mx, my, System.currentTimeMillis()});
    }

    public static void render(DrawContext c, int client, float alphaMul) {
        long now = System.currentTimeMillis();
        for (double[] p : PTS) {
            float a = 1f - (float) ((now - p[2]) / 450.0);
            int size = 1 + (int) (4 * a);
            int x = (int) p[0] - size / 2, y = (int) p[1] - size / 2;
            int rgb = Colors.lerp(client, 0xFFFFFF, (1f - a) * 0.5f);
            c.fill(x - 2, y - 2, x + size + 2, y + size + 2, Colors.a(client, (int) (22 * a * alphaMul)));
            c.fill(x, y, x + size, y + size, Colors.a(rgb, (int) (210 * a * alphaMul)));
        }
        for (double[] r : RIP) {
            float t = (float) ((now - r[2]) / 550.0);
            double rad = 4 + t * 30;
            int a = (int) (200 * (1f - t) * alphaMul);
            for (int i = 0; i < 28; i++) {
                double ang = i * Math.PI * 2 / 28;
                int px = (int) (r[0] + Math.cos(ang) * rad), py = (int) (r[1] + Math.sin(ang) * rad);
                c.fill(px, py, px + 2, py + 2, Colors.a(client, a));
            }
        }
    }

    private MouseTrail() { }
}
