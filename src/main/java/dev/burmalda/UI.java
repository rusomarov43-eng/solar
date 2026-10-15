package dev.burmalda;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Smooth (TTF) text for the whole UI. Theme -> Font = Vanilla switches back to the Minecraft font. */
public final class UI {
    private static final Style REG = Style.EMPTY.withFont(new Identifier(Brand.ID, "ui"));
    private static final Style BOLD = Style.EMPTY.withFont(new Identifier(Brand.ID, "ui_bold"));

    private static boolean smooth() { return Modules.THEME.m("Font") == 0; }

    public static Text t(String s) { return smooth() ? Text.literal(s).setStyle(REG) : Text.literal(s); }
    public static Text tb(String s) { return smooth() ? Text.literal(s).setStyle(BOLD) : Text.literal(s); }

    public static int w(TextRenderer tr, String s) { return tr.getWidth(t(s)); }
    public static int wb(TextRenderer tr, String s) { return tr.getWidth(tb(s)); }

    public static void draw(DrawContext c, TextRenderer tr, String s, int x, int y, int argb) {
        if ((argb >>> 24) < 4) return; // alpha < 4 would be drawn fully opaque by vanilla
        c.drawText(tr, t(s), x, y, argb, false);
    }

    public static void drawBold(DrawContext c, TextRenderer tr, String s, int x, int y, int argb) {
        if ((argb >>> 24) < 4) return;
        c.drawText(tr, tb(s), x, y, argb, false);
    }

    public static void centered(DrawContext c, TextRenderer tr, String s, int cx, int y, int argb) {
        draw(c, tr, s, cx - w(tr, s) / 2, y, argb);
    }

    public static String trim(TextRenderer tr, String s, int maxW) {
        if (w(tr, s) <= maxW) return s;
        int n = s.length();
        while (n > 0 && w(tr, s.substring(0, n) + "...") > maxW) n--;
        return n <= 0 ? "" : s.substring(0, n) + "...";
    }

    private UI() { }
}
