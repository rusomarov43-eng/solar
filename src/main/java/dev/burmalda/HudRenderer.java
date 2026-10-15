package dev.burmalda;

import com.sun.management.OperatingSystemMXBean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class HudRenderer {
    private static final OperatingSystemMXBean OS = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static float cpu;
    private static long lastCpu;
    private static int leftY, rightY;

    private static final int BG = 0xE60E0B16;
    private static final int GRAY = 0xFF8F8AA8;
    private static final int WHITE = 0xFFFFFFFF;

    public static void render(DrawContext c, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        int client = Colors.client();
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        leftY = 6;
        rightY = 6;

        if (Modules.WATERMARK.enabled) watermark(c, mc, tr, client, sw);
        if (Modules.INFO.enabled) { info(c, mc, tr, client); leftY = 34; }
        if (Modules.ARRAYLIST.enabled) arrayList(c, tr, client);
        if (Modules.STATS.enabled) Widgets.stats(c, mc, tr, client, leftY);
        if (Modules.KEYBINDS.enabled) keybinds(c, tr, client, sw);
        if (Modules.EFFECTS.enabled) Widgets.effects(c, mc, tr, client, sw, rightY);
        if (Modules.KEYSTROKES.enabled) Widgets.keystrokes(c, mc, tr, client, sw, sh);
        if (Modules.ARMOR.enabled) Widgets.armor(c, mc, tr, client, sw, sh);
        if (Modules.COMPASS.enabled) ExtraHud.compass(c, mc, tr, client, sw);
        if (Modules.FPS_GRAPH.enabled) ExtraHud.fpsGraph(c, mc, tr, client, 6, sh - 170);
        if (Modules.LYRICS.enabled) ExtraHud.lyrics(c, tr, client, sw, sh);
        if (Modules.HIT_INFO.enabled) ExtraHud.hitInfo(c, tr, client, sw, sh);
        if (Modules.COORDS.enabled) coords(c, mc, tr, client, sw, sh);
        if (Modules.ISLAND.enabled) Island.renderHud(c, tr, client, sw, mc.currentScreen instanceof MenuScreen);
        TargetHud.render(c, mc, tr, client, sw, sh);
    }

    private static int ping(MinecraftClient mc) {
        var h = mc.getNetworkHandler();
        if (h == null) return 0;
        PlayerListEntry e = h.getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    private static void watermark(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw) {
        Feature f = Modules.WATERMARK;
        List<String> ic = new ArrayList<>(), tx = new ArrayList<>();
        ic.add("logo"); tx.add(Brand.NAME);
        if (f.b("Username")) { ic.add("user"); tx.add(mc.getSession().getUsername()); }
        if (f.b("FPS")) { ic.add("fps"); tx.add(String.valueOf(mc.getCurrentFps())); }
        if (f.b("Ping")) { ic.add("ping"); tx.add(ping(mc) + "ms"); }

        int gap = 12, total = 16;
        for (String t : tx) total += 11 + UI.w(tr, t);
        total += gap * (tx.size() - 1);

        int x = (sw - total) / 2, y = 4, h = 20;
        Draw.panel(c, x, y, total, h, 6, BG, Colors.a(client, 120), client, 1f);
        int px = x + 8;
        for (int i = 0; i < tx.size(); i++) {
            Icons.draw(c, ic.get(i), px, y + 7, 1, Colors.a(client, 255));
            UI.draw(c, tr, tx.get(i), px + 11, y + 6, i == 0 ? Colors.a(client, 255) : WHITE);
            px += 11 + UI.w(tr, tx.get(i));
            if (i < tx.size() - 1) {
                c.fill(px + gap / 2, y + 5, px + gap / 2 + 1, y + h - 5, 0x40FFFFFF);
                px += gap;
            }
        }
    }

    private static void info(DrawContext c, MinecraftClient mc, TextRenderer tr, int client) {
        long now = System.currentTimeMillis();
        if (now - lastCpu > 500) { cpu = (float) Math.max(0, OS.getCpuLoad()); lastCpu = now; }
        Runtime rt = Runtime.getRuntime();
        float ram = (rt.totalMemory() - rt.freeMemory()) / (float) rt.maxMemory();

        Feature f = Modules.INFO;
        List<String> ic = new ArrayList<>(), vals = new ArrayList<>();
        List<Float> fr = new ArrayList<>();
        if (f.b("CPU")) { ic.add("cpu"); vals.add(Math.round(cpu * 100) + "%"); fr.add(cpu); }
        if (f.b("RAM")) { ic.add("ram"); vals.add(Math.round(ram * 100) + "%"); fr.add(ram); }
        if (f.b("FPS")) {
            int fps = mc.getCurrentFps();
            ic.add("fps"); vals.add(String.valueOf(fps)); fr.add(Math.min(1f, fps / 240f));
        }
        if (ic.isEmpty()) return;

        int cell = 58, x = 6, y = 6, w = 10 + cell * ic.size(), h = 24;
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        for (int i = 0; i < ic.size(); i++) {
            int cx = x + 8 + i * cell;
            Icons.draw(c, ic.get(i), cx, y + 4, 1, Colors.a(client, 255));
            UI.draw(c, tr, vals.get(i), cx + 11, y + 4, WHITE);
            Draw.rrect(c, cx, y + 16, cell - 10, 3, 1, Colors.a(0xFFFFFF, 40));
            Draw.rrect(c, cx, y + 16, (int) ((cell - 10) * Math.max(0f, Math.min(1f, fr.get(i)))), 3, 1, Colors.a(client, 255));
        }
    }

    /** List of enabled non-HUD modules, left side under the Info panel. */
    private static void arrayList(DrawContext c, TextRenderer tr, int client) {
        Feature f = Modules.ARRAYLIST;
        List<Feature> on = new ArrayList<>();
        for (Feature m : Modules.ALL) if (m.enabled && !m.category.equals("HUD")) on.add(m);
        if (on.isEmpty()) return;
        on.sort((a, b) -> Integer.compare(UI.w(tr, b.name + b.info()), UI.w(tr, a.name + a.info())));

        int y = leftY;
        for (Feature m : on) {
            String inf = m.info();
            boolean hasInfo = !inf.equals("ON");
            int w = UI.w(tr, m.name) + (hasInfo ? UI.w(tr, inf) + 5 : 0) + 12;
            if (f.b("Background")) c.fill(6, y, 6 + w, y + 12, 0xB00E0B16);
            if (f.b("Bar")) c.fill(6, y, 8, y + 12, Colors.a(client, 255));
            UI.draw(c, tr, m.name, 12, y + 2, WHITE);
            if (hasInfo) UI.draw(c, tr, inf, 12 + UI.w(tr, m.name) + 5, y + 2, GRAY);
            y += 12;
        }
        leftY = y + 6;
    }

    /** Modules that have a key bound. */
    private static void keybinds(DrawContext c, TextRenderer tr, int client, int sw) {
        List<Feature> rows = new ArrayList<>();
        for (Feature f : Modules.ALL) if (f.bind >= 0) rows.add(f);
        if (rows.isEmpty()) return;

        int w = 110;
        for (Feature f : rows) w = Math.max(w, UI.w(tr, f.name) + UI.w(tr, MenuScreen.keyName(f.bind)) + 34);
        int h = 20 + rows.size() * 11 + 3;
        int x = sw - w - 6, y = rightY;
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        Icons.draw(c, "logo", x + 8, y + 6, 1, Colors.a(client, 255));
        UI.draw(c, tr, "Keybinds", x + 20, y + 6, WHITE);
        for (int i = 0; i < rows.size(); i++) {
            Feature f = rows.get(i);
            int ry = y + 20 + i * 11;
            UI.draw(c, tr, f.name, x + 8, ry, f.enabled ? WHITE : GRAY);
            String k = MenuScreen.keyName(f.bind);
            UI.draw(c, tr, k, x + w - 8 - UI.w(tr, k), ry, f.enabled ? Colors.a(client, 255) : GRAY);
        }
        rightY = y + h + 6;
    }

    private static void coords(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw, int sh) {
        String s = String.format("x %d   y %d   z %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        if (Modules.COORDS.b("Direction")) s += "   " + mc.player.getHorizontalFacing().asString().substring(0, 1).toUpperCase();
        int w = UI.w(tr, s) + 28, h = 16, x = sw - w - 6, y = sh - h - 6;
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        Icons.draw(c, "pin", x + 8, y + 4, 1, Colors.a(client, 255));
        UI.draw(c, tr, s, x + 19, y + 4, WHITE);
    }

    private HudRenderer() { }
}
