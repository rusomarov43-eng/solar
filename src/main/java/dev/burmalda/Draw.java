package dev.burmalda;

import net.minecraft.client.gui.DrawContext;

/** Glass drawing primitives: anti-aliased rounded rectangles, soft shadows, frosted panels. */
public final class Draw {
    public static float glassLevel() { return Modules.THEME.f("Glass"); }

    /** Anti-aliased filled rounded rectangle. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        int a0 = color >>> 24;
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double off = r - Math.sqrt(r * (double) r - dy * dy);
            int inset = (int) Math.ceil(off);
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
            if (inset > 0) {
                int ac = (int) (a0 * (inset - off) * 0.9);
                if (ac > 0) {
                    int cc = (ac << 24) | (color & 0xFFFFFF);
                    c.fill(x + inset - 1, y + i, x + inset, y + i + 1, cc);
                    c.fill(x + w - inset, y + i, x + w - inset + 1, y + i + 1, cc);
                    c.fill(x + inset - 1, y + h - 1 - i, x + inset, y + h - i, cc);
                    c.fill(x + w - inset, y + h - 1 - i, x + w - inset + 1, y + h - i, cc);
                }
            }
        }
    }

    /** 1px rounded outline. */
    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        r = Math.max(1, Math.min(r, Math.min(w, h) / 2));
        int[] ins = new int[r];
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            ins[i] = (int) Math.round(r - Math.sqrt(r * (double) r - dy * dy));
        }
        c.fill(x + ins[0], y, x + w - ins[0], y + 1, color);
        c.fill(x + ins[0], y + h - 1, x + w - ins[0], y + h, color);
        for (int i = 1; i < r; i++) {
            int a = ins[i], b = Math.max(ins[i - 1], a + 1);
            c.fill(x + a, y + i, x + b, y + i + 1, color);
            c.fill(x + w - b, y + i, x + w - a, y + i + 1, color);
            c.fill(x + a, y + h - 1 - i, x + b, y + h - i, color);
            c.fill(x + w - b, y + h - 1 - i, x + w - a, y + h - i, color);
        }
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    /** Frosted glass panel. am = overall alpha multiplier (fade animations). */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, int accentRgb, float am) {
        r = Math.max(r, 6);
        for (int g = 3; g >= 1; g--)
            rrect(c, x - g, y - g + 2, w + 2 * g, h + 2 * g, r + g, Colors.a(0x000000, (int) (15 * am / g)));
        int fill = (int) (255 * glassLevel() * am);
        rrect(c, x, y, w, h, r, Colors.a(Colors.lerp(0x0B0D17, accentRgb, 0.05f), fill));
        rrect(c, x, y, w, h, r, Colors.a(0xFFFFFF, (int) (9 * am)));
        int sheen = Math.min(h / 2, 22);
        if (sheen > 3 && w > r * 2)
            c.fillGradient(x + r / 2, y + 1, x + w - r / 2, y + sheen, Colors.a(0xFFFFFF, (int) (24 * am)), Colors.a(0xFFFFFF, 0));
        outline(c, x, y, w, h, r, Colors.a(0xFFFFFF, (int) (40 * am)));
    }

    /** Legacy entry point used by the HUD widgets: same glass look for every panel. */
    public static void panel(DrawContext c, int x, int y, int w, int h, int r,
                             int fill, int border, int accentRgb, float anim) {
        glass(c, x, y, w, h, r + 2, accentRgb, anim);
    }

    private Draw() { }
}
