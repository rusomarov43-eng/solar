package dev.burmalda;

import net.minecraft.client.gui.DrawContext;

import java.util.HashMap;
import java.util.Map;

/** 7x7 pixel icons, drawn with fill() so they stay crisp and cost nothing. */
public final class Icons {
    private static final Map<String, String[]> MAP = new HashMap<>();

    static {
        put("logo", "...X...", "..XXX..", ".XXXXX.", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...");
        put("cpu", ".X.X.X.", "XXXXXXX", ".X...X.", "XX...XX", ".X...X.", "XXXXXXX", ".X.X.X.");
        put("ram", ".......", "XXXXXXX", "X.X.X.X", "X.X.X.X", "XXXXXXX", ".X.X.X.", ".......");
        put("fps", "XXXXXXX", "X.....X", "X.X.X.X", "X.....X", "XXXXXXX", "..XXX..", ".XXXXX.");
        put("ping", "......X", "......X", "....X.X", "....X.X", "..X.X.X", "..X.X.X", "X.X.X.X");
        put("user", "..XXX..", ".XXXXX.", ".XXXXX.", "..XXX..", ".XXXXX.", "XXXXXXX", "XXXXXXX");
        put("clock", ".XXXXX.", "X..X..X", "X..X..X", "X..XXXX", "X.....X", "X.....X", ".XXXXX.");
        put("pin", "..XXX..", ".XXXXX.", ".XX.XX.", ".XXXXX.", "..XXX..", "...X...", "...X...");
        put("music", "...XXXX", "...X..X", "...X..X", "...X..X", ".XXX.XX", "XXXX.XX", ".XX..X.");
        put("play", "X......", "XXX....", "XXXXX..", "XXXXXXX", "XXXXX..", "XXX....", "X......");
        put("pause", ".XX.XX.", ".XX.XX.", ".XX.XX.", ".XX.XX.", ".XX.XX.", ".XX.XX.", ".XX.XX.");
        put("rew", "..X...X", ".XX..XX", "XXX.XXX", "XXXXXXX", "XXX.XXX", ".XX..XX", "..X...X");
        put("fwd", "X...X..", "XX..XX.", "XXX.XXX", "XXXXXXX", "XXX.XXX", "XX..XX.", "X...X..");
        put("shield", "XXXXXXX", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...");
        put("mouse", ".XXXXX.", "XX.X.XX", "XX.X.XX", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.");
        put("heart", ".XX.XX.", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X...");
        put("potion", "..XXX..", "...X...", "...X...", "..XXX..", ".XXXXX.", "XXXXXXX", ".XXXXX.");
        put("bolt", "...XXX.", "..XXX..", ".XXX...", "XXXXXXX", "...XXX.", "..XXX..", ".XX....");
    }

    private static void put(String id, String... rows) { MAP.put(id, rows); }

    public static void draw(DrawContext c, String id, int x, int y, int scale, int argb) {
        String[] rows = MAP.get(id);
        if (rows == null) return;
        for (int j = 0; j < rows.length; j++) {
            String r = rows[j];
            int i = 0;
            while (i < r.length()) {
                if (r.charAt(i) != 'X') { i++; continue; }
                int s = i;
                while (i < r.length() && r.charAt(i) == 'X') i++;
                c.fill(x + s * scale, y + j * scale, x + i * scale, y + (j + 1) * scale, argb);
            }
        }
    }

    private Icons() { }
}
