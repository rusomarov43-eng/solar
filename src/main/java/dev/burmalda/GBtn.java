package dev.burmalda;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Glass button used by the custom screens. */
public final class GBtn {
    public String label, icon;
    public int x, y, w, h;
    public float hover;
    public boolean primary, danger;
    public boolean enabled = true;
    public final Runnable action;

    public GBtn(String label, Runnable action) { this.label = label; this.action = action; }

    public GBtn at(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; return this; }
    public GBtn icon(String id) { this.icon = id; return this; }
    public GBtn primary() { this.primary = true; return this; }

    public boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    public void render(DrawContext c, TextRenderer tr, int mx, int my, int client) {
        boolean hov = enabled && in(mx, my);
        hover += ((hov ? 1f : 0f) - hover) * 0.22f;
        int base = danger ? 0xFF5A5A : client;
        Draw.rrect(c, x - 1, y - 1 + (hov ? 0 : 1), w + 2, h + 2, h / 2 + 1, Colors.a(0x000000, 34));
        int fill = primary ? Colors.a(Colors.lerp(0x0B0D17, base, 0.32f + 0.12f * hover), 210)
                : Colors.a(Colors.lerp(0x10121E, 0xFFFFFF, 0.04f + 0.06f * hover), 190);
        Draw.rrect(c, x, y, w, h, h / 2, fill);
        c.fillGradient(x + h / 3, y + 1, x + w - h / 3, y + h / 2, Colors.a(0xFFFFFF, 22), Colors.a(0xFFFFFF, 0));
        Draw.outline(c, x, y, w, h, h / 2, Colors.a(Colors.lerp(0xFFFFFF, base, hover), (int) (40 + 90 * hover)));
        int col = enabled ? 0xFFFFFFFF : 0xFF77738A;
        int tw = UI.w(tr, label);
        int ix = icon == null ? 0 : 11;
        int tx = x + (w - tw - ix) / 2;
        if (icon != null) Icons.draw(c, icon, tx, y + (h - 7) / 2, 1, Colors.a(primary ? 0xFFFFFF : base, 255));
        UI.draw(c, tr, label, tx + ix, y + (h - 9) / 2 + 1, col);
    }

    public boolean click(double mx, double my) {
        if (enabled && in(mx, my)) { action.run(); return true; }
        return false;
    }
}
