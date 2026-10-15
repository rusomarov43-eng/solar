package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compass strip and combo / reach pill. */
public final class ExtraHud {
    private static final int BG = 0xE60E0B16, WHITE = 0xFFFFFFFF, GRAY = 0xFF8F8AA8;

    public static void compass(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw) {
        int w = 190, h = 16, x = (sw - w) / 2, y = 56;
        Draw.panel(c, x, y, w, h, 5, BG, Colors.a(client, 90), client, 1f);
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        c.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        for (int ang = 0; ang < 360; ang += 15) {
            float rel = MathHelper.wrapDegrees(ang - yaw);
            int px = x + w / 2 + Math.round(rel * 2f);
            if (px < x || px > x + w) continue;
            if (ang % 45 == 0) {
                String n = names[ang / 45];
                boolean card = ang % 90 == 0;
                UI.draw(c, tr, n, px - UI.w(tr, n) / 2, y + 4, card ? WHITE : GRAY);
            } else {
                c.fill(px, y + 9, px + 1, y + 13, 0x60FFFFFF);
            }
        }
        c.disableScissor();
        c.fill(x + w / 2, y + 1, x + w / 2 + 1, y + 4, Colors.a(client, 255));
    }

    public static void hitInfo(DrawContext c, TextRenderer tr, int client, int sw, int sh) {
        long age = System.currentTimeMillis() - HitEffects.lastHit;
        if (HitEffects.lastHit == 0 || age > 3000) return;
        Feature f = Modules.HIT_INFO;
        List<String> parts = new ArrayList<>();
        if (f.b("Combo") && HitEffects.combo > 0) parts.add("Combo x" + HitEffects.combo);
        if (f.b("Reach")) parts.add(String.format(Locale.ROOT, "%.2f blocks", HitEffects.reach));
        if (parts.isEmpty()) return;
        String s = String.join("   ", parts);
        int w = UI.w(tr, s) + 28, h = 16, x = (sw - w) / 2, y = sh / 2 - 50;
        Draw.panel(c, x, y, w, h, 5, BG, Colors.a(client, 100), client, 1f);
        Icons.draw(c, "mouse", x + 8, y + 4, 1, Colors.a(client, 255));
        UI.draw(c, tr, s, x + 19, y + 4, WHITE);
    }

    // ------------------------------------------------------------------ lyrics (synced, fetched at runtime)
    private static int lastIdx = -2, prevIdx = -2;
    private static long changeAt;

    public static void lyrics(DrawContext c, TextRenderer tr, int client, int sw, int sh) {
        Feature f = Modules.LYRICS;
        if (!MediaMonitor.visible()) return;
        List<Lyrics.Line> ls = Lyrics.lines();
        if (ls.isEmpty()) return;
        long now = System.currentTimeMillis();
        int idx = Lyrics.index(MediaMonitor.posNow() + (long) f.f("Offset ms"));
        if (idx != lastIdx) { prevIdx = lastIdx; lastIdx = idx; changeAt = now; }
        if (idx < 0) return;

        String cur = ls.get(idx).text();
        String next = f.b("Next line") && idx + 1 < ls.size() ? ls.get(idx + 1).text() : "";
        String prev = prevIdx >= 0 && prevIdx < ls.size() ? ls.get(prevIdx).text() : "";
        if (cur.isBlank() && next.isBlank()) return;

        float a = Math.min(1f, (now - changeAt) / 300f);
        a = 1f - (1f - a) * (1f - a) * (1f - a);
        float scale = 1.25f;
        int baseY = switch (f.m("Position")) { case 0 -> sh - 112; case 1 -> sh / 2 + 64; default -> 78; };

        String curT = UI.trim(tr, cur, (int) ((sw - 90) / scale));
        String nextT = UI.trim(tr, next, sw - 90);
        int tw = (int) (UI.wb(tr, curT) * scale), nw = UI.w(tr, nextT);
        int w = Math.max(Math.max(tw, nw), 120) + 48, h = nextT.isBlank() ? 34 : 56;
        int x = (sw - w) / 2, y = baseY;
        Draw.glass(c, x, y, w, h, 14, client, 1f);

        if (!prev.isBlank() && a < 1f)
            UI.centered(c, tr, UI.trim(tr, prev, sw - 90), sw / 2, y + 10 - (int) (a * 8), Colors.a(0xFFFFFF, (int) (150 * (1f - a))));
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(sw / 2f, y + 10 + (1f - a) * 7f, 0);
        ms.scale(scale, scale, 1f);
        UI.drawBold(c, tr, curT, -UI.wb(tr, curT) / 2, 0, Colors.a(0xFFFFFF, (int) (255 * a)));
        ms.pop();
        if (!nextT.isBlank()) UI.centered(c, tr, nextT, sw / 2, y + h - 19, 0x88FFFFFF);
    }

    // ------------------------------------------------------------------ fps graph
    private static final int[] HIST = new int[80];
    private static int hp;

    public static void fpsGraph(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int x, int y) {
        HIST[hp++ % 80] = mc.getCurrentFps();
        int w = 100, h = 40;
        Draw.glass(c, x, y, w, h, 9, client, 1f);
        int max = 1;
        for (int v : HIST) max = Math.max(max, v);
        for (int i = 0; i < 80; i++) {
            int v = HIST[(hp + i) % 80];
            int bh = (int) ((h - 17) * (v / (float) max));
            c.fill(x + 10 + i, y + h - 5 - bh, x + 11 + i, y + h - 5, Colors.a(client, 200));
        }
        UI.draw(c, tr, "FPS " + mc.getCurrentFps(), x + 9, y + 4, 0xFFFFFFFF);
    }

    private ExtraHud() { }
}
