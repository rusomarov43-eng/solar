package dev.burmalda;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

import java.util.Locale;

/** Dynamic Island: notifications + YouTube player (cover, progress, rewind / pause / forward). */
public final class Island {
    private static final int CARD_W = 236, CARD_H = 80;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF9A98A8;

    private static float curW, curH;

    // interactive card geometry (menu open)
    private static boolean active;
    private static int ix, iy, iw, ih, barX, barW, barY;

    /** HUD version: animated pill under the watermark. */
    public static void renderHud(DrawContext c, TextRenderer tr, int client, int sw, boolean menuOpen) {
        Feature f = Modules.ISLAND;
        long now = System.currentTimeMillis();
        boolean musicOn = f.b("Music") && MediaMonitor.visible();
        if (menuOpen && f.b("Music") && MediaMonitor.hasTrack()) return; // the menu draws the interactive card

        String note = Notifications.text();
        boolean showNote = !note.isEmpty() && now - Notifications.born() < f.f("Duration") * 1000f;
        boolean music = !showNote && musicOn;
        boolean big = music && MediaMonitor.isPlaying() && now - MediaMonitor.changedAt() < 4000;

        int tw = 0, th = 0;
        if (showNote) { tw = UI.w(tr, note) + 34; th = 18; }
        else if (music) { tw = big ? CARD_W : 170; th = big ? CARD_H : 20; }

        curW += (tw - curW) * 0.22f;
        curH += (th - curH) * 0.22f;
        if (curW < 8 || curH < 6) return;

        int w = (int) curW, h = (int) curH, x = (sw - w) / 2, y = 30;
        int bg = Colors.a(Colors.lerp(0x07050C, client, music ? 0.12f : 0.04f), 255);
        Draw.panel(c, x, y, w, h, Math.min(10, h / 2), bg, Colors.a(client, 110), client, 1f);
        if (Math.abs(curW - tw) > 6 || Math.abs(curH - th) > 4) return;

        if (showNote) {
            Draw.rrect(c, x + 9, y + 7, 4, 4, 2, Colors.a(client, 255));
            UI.draw(c, tr, note, x + 20, y + 5, WHITE);
        } else if (music) {
            if (big) card(c, tr, client, x, y, w, -1000, -1000, false);
            else compact(c, tr, client, x, y, w, h);
        }
    }

    /** Menu version: full card at the top, clickable. */
    public static void renderInteractive(DrawContext c, TextRenderer tr, int client, int sw, int mx, int my) {
        active = false;
        Feature f = Modules.ISLAND;
        if (!f.enabled || !f.b("Music") || !MediaMonitor.hasTrack()) return;
        int x = (sw - CARD_W) / 2, y = 6;
        int bg = Colors.a(Colors.lerp(0x07050C, client, 0.12f), 255);
        Draw.panel(c, x, y, CARD_W, CARD_H, 10, bg, Colors.a(client, 160), client, 1f);
        card(c, tr, client, x, y, CARD_W, mx, my, true);
        active = true; ix = x; iy = y; iw = CARD_W; ih = CARD_H;
    }

    public static boolean click(double mx, double my) {
        if (!active || mx < ix || mx >= ix + iw || my < iy || my >= iy + ih) return false;
        for (int i = 0; i < 3; i++) {
            int bx = btnX(ix, iw, i), by = iy + CARD_H - 20;
            if (mx >= bx && mx < bx + 22 && my >= by && my < by + 18) {
                MediaMonitor.send(i == 0 ? "rew" : i == 1 ? "toggle" : "fwd");
                return true;
            }
        }
        if (MediaMonitor.durMs() > 0 && mx >= barX && mx < barX + barW && my >= barY - 4 && my < barY + 9) {
            long ms = (long) ((mx - barX) / barW * MediaMonitor.durMs());
            MediaMonitor.send("seek:" + ms);
        }
        return true;
    }

    private static int btnX(int x, int w, int i) { return x + w / 2 - 11 + (i - 1) * 34; }

    private static void compact(DrawContext c, TextRenderer tr, int client, int x, int y, int w, int h) {
        cover(c, x + 4, y + 3, 14);
        String t = MediaMonitor.title() + (MediaMonitor.artist().isEmpty() ? "" : " - " + MediaMonitor.artist());
        UI.draw(c, tr, UI.trim(tr, t, w - 62), x + 24, y + 6, WHITE);
        double time = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 3; i++) {
            int bh = MediaMonitor.isPlaying() ? 2 + (int) (6 * Math.abs(Math.sin(time * 3 + i * 1.3))) : 2;
            c.fill(x + w - 20 + i * 5, y + h - 5 - bh, x + w - 17 + i * 5, y + h - 5, Colors.a(client, 255));
        }
    }

    private static void card(DrawContext c, TextRenderer tr, int client, int x, int y, int w, int mx, int my, boolean interactive) {
        cover(c, x + 8, y + 8, 36);
        UI.draw(c, tr, UI.trim(tr, MediaMonitor.title(), w - 62), x + 52, y + 11, WHITE);
        UI.draw(c, tr, UI.trim(tr, MediaMonitor.artist(), w - 62), x + 52, y + 23, GRAY);

        long dur = MediaMonitor.durMs(), pos = MediaMonitor.posNow();
        String l = fmt(pos), r = fmt(dur);
        int ty = y + 50;
        UI.draw(c, tr, l, x + 8, ty, GRAY);
        UI.draw(c, tr, r, x + w - 8 - UI.w(tr, r), ty, GRAY);
        int bx = x + 8 + UI.w(tr, l) + 6, bw = w - 16 - UI.w(tr, l) - UI.w(tr, r) - 12;
        int by = ty + 3;
        Draw.rrect(c, bx, by, bw, 3, 1, 0xFF2A2F33);
        int fw = dur > 0 ? (int) (bw * Math.min(1.0, pos / (double) dur)) : 0;
        Draw.rrect(c, bx, by, fw, 3, 1, Colors.a(client, 255));
        Draw.rrect(c, bx + fw - 2, by - 2, 5, 7, 2, WHITE);
        if (interactive) { barX = bx; barW = bw; barY = by; }

        String[] icons = {"rew", MediaMonitor.isPlaying() ? "pause" : "play", "fwd"};
        for (int i = 0; i < 3; i++) {
            int px = btnX(x, w, i), py = y + CARD_H - 20;
            boolean hov = interactive && mx >= px && mx < px + 22 && my >= py && my < py + 18;
            if (hov) Draw.rrect(c, px, py, 22, 18, 3, Colors.a(client, 70));
            Icons.draw(c, icons[i], px + 4, py + 2, 2, WHITE);
        }
    }

    private static void cover(DrawContext c, int x, int y, int size) {
        Draw.rrect(c, x - 1, y - 1, size + 2, size + 2, 3, 0xFF2A2F33);
        var id = MediaMonitor.coverId();
        if (id != null) {
            int cw = MediaMonitor.coverW(), ch = MediaMonitor.coverH();
            int side = Math.min(cw, ch);
            c.drawTexture(id, x, y, size, size, (cw - side) / 2f, (ch - side) / 2f, side, side, cw, ch);
        } else {
            Draw.rrect(c, x, y, size, size, 3, 0xFF14101F);
            int sc = size >= 30 ? 3 : 1;
            Icons.draw(c, "music", x + (size - 7 * sc) / 2, y + (size - 7 * sc) / 2, sc, 0xFF6E6890);
        }
    }

    private static String fmt(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    private Island() { }
}
