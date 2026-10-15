package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Nebula menu (glass). Right Shift opens / closes it with a smooth animation.
 * Sidebar = categories + profiles, cards = modules. LMB on a card = toggle, RMB = collapse,
 * click the key label = bind a key. Profiles: LMB loads, RMB saves. Type "admin" in the search box for the admin panel.
 */
public class MenuScreen extends Screen {
    private static final int W = 600, H = 360, SIDE = 140, CARD_W = 213, HDR = 26, ROW = 22;
    private static final String[] CAT_ICONS = {"fps", "shield", "cpu", "heart", "bolt", "logo"};
    private static final IntConsumer NOOP = b -> { };
    private static final int TXT = 0xFFFFFFFF, TXT2 = 0xB3FFFFFF, TXT3 = 0x73FFFFFF;

    private record Hit(int x, int y, int w, int h, IntConsumer click, DoubleConsumer drag, boolean clip) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging;
    private String tab = Modules.CATEGORIES[0];
    private float scroll, scrollTarget, maxScroll;
    private long tabAt = System.currentTimeMillis();
    private final long opened = System.currentTimeMillis();
    private boolean closing;
    private long closeAt;
    private long lastNano = System.nanoTime();
    private float selY = -1f;

    private float alpha = 1f, cardAlpha = 1f;
    private float curS = 1f, ox, oy;
    private int vy0, vy1;

    private String query = "";
    private boolean searchFocus;
    private Feature listening, prevListening;

    private boolean passFocus;
    private String pass = "", passMsg = "";

    public MenuScreen() {
        super(Text.literal(Brand.NAME));
        for (Feature f : Modules.ALL) {
            f.anim = f.enabled ? 1f : 0f;
            f.open = f.collapsed ? 0f : 1f;
            f.hover = 0f;
            for (Setting s : f.settings) s.anim = s.type == Setting.Type.BOOL ? (s.on ? 1f : 0f) : s.num;
        }
    }

    public static String keyName(int key) {
        try {
            String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
            return s.length() > 9 ? s.substring(0, 9) : s;
        } catch (Exception e) {
            return "KEY" + key;
        }
    }

    @Override public boolean shouldPause() { return false; }

    // ------------------------------------------------------------------ helpers
    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
    private static float ease(float t) { t = clamp01(t); return 1f - (1f - t) * (1f - t) * (1f - t); }

    private int A(int argb) {
        int a = (int) ((argb >>> 24) * alpha * cardAlpha);
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }

    private void text(DrawContext c, String s, int x, int y, int argb) { UI.draw(c, textRenderer, s, x, y, A(argb)); }
    private void bold(DrawContext c, String s, int x, int y, int argb) { UI.drawBold(c, textRenderer, s, x, y, A(argb)); }
    private int tw(String s) { return UI.w(textRenderer, s); }
    private void rr(DrawContext c, int x, int y, int w, int h, int r, int argb) { Draw.rrect(c, x, y, w, h, r, A(argb)); }
    private void ol(DrawContext c, int x, int y, int w, int h, int r, int argb) { Draw.outline(c, x, y, w, h, r, A(argb)); }
    private void icon(DrawContext c, String id, int x, int y, int scale, int argb) {
        int col = A(argb);
        if ((col >>> 24) >= 4) Icons.draw(c, id, x, y, scale, col);
    }

    private void scissor(DrawContext c, int x0, int y0, int x1, int y1) {
        c.enableScissor(Math.round(ox + x0 * curS), Math.round(oy + y0 * curS), Math.round(ox + x1 * curS), Math.round(oy + y1 * curS));
    }

    private List<String> categories() {
        List<String> cats = new ArrayList<>(List.of(Modules.CATEGORIES));
        if (Admin.unlocked()) cats.add("Admin");
        return cats;
    }

    private boolean adminView() { return query.equalsIgnoreCase("admin") || (tab.equals("Admin") && query.isEmpty() && Admin.unlocked()); }

    // ------------------------------------------------------------------ render
    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long nowN = System.nanoTime();
        float dt = Math.min(0.05f, (nowN - lastNano) / 1e9f);
        lastNano = nowN;
        float k = 1f - (float) Math.exp(-dt * 14f);
        long now = System.currentTimeMillis();

        float p;
        if (closing) {
            p = 1f - ease((now - closeAt) / 170f);
            if (p <= 0.001f) { MinecraftClient.getInstance().setScreen(null); return; }
        } else {
            p = ease((now - opened) / 240f);
        }
        alpha = p;
        int client = Colors.client();
        hits.clear();

        c.fillGradient(0, 0, width, height, Colors.a(0x000000, (int) (80 * p)),
                Colors.a(Colors.lerp(0x05030A, client, 0.22f), (int) (150 * p)));

        float fit = Math.max(0.5f, Math.min(1f, Math.min((width - 16f) / W, (height - 16f) / H)));
        curS = fit * (0.95f + 0.05f * p);
        ox = (width - W * curS) / 2f;
        oy = (height - H * curS) / 2f + (1f - p) * 14f;
        int ux = (int) ((mx - ox) / curS), uy = (int) ((my - oy) / curS);

        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(ox, oy, 0);
        ms.scale(curS, curS, 1f);
        drawUi(c, k, now, client, ux, uy);
        ms.pop();

        if (!closing && p > 0.9f) Island.renderInteractive(c, textRenderer, client, width, mx, my);
        MouseTrail.update(mx, my);
        MouseTrail.render(c, client, p);
    }

    private void drawUi(DrawContext c, float k, long now, int client, int ux, int uy) {
        cardAlpha = 1f;
        Draw.glass(c, 0, 0, W, H, 18, client, alpha);

        // ---------------- sidebar
        rr(c, 8, 8, SIDE - 10, H - 16, 14, Colors.a(0xFFFFFF, 12));
        icon(c, "logo", 18, 18, 2, Colors.a(client, 255));
        bold(c, Brand.NAME, 38, 15, TXT);
        text(c, "Visuals " + Brand.VERSION, 38, 27, TXT3);
        c.fill(16, 46, SIDE - 12, 47, A(0x22FFFFFF));

        List<String> cats = categories();
        int selIdx = 0;
        for (int i = 0; i < cats.size(); i++) if (cats.get(i).equals(tab)) selIdx = i;
        float targetY = 54 + selIdx * 28;
        if (selY < 0) selY = targetY;
        selY += (targetY - selY) * k;
        if (query.isEmpty() || adminView()) {
            rr(c, 14, (int) selY, SIDE - 22, 26, 9, Colors.a(client, 60));
            rr(c, 14, (int) selY + 6, 3, 14, 1, Colors.a(client, 255));
        }
        for (int i = 0; i < cats.size(); i++) {
            String cat = cats.get(i);
            int y = 54 + i * 28;
            boolean sel = query.isEmpty() && cat.equals(tab);
            int on = 0, total = 0;
            for (Feature f : Modules.ALL) if (f.category.equals(cat)) { total++; if (f.enabled) on++; }
            icon(c, CAT_ICONS[Math.min(i, CAT_ICONS.length - 1)], 25, y + 9, 1, sel ? Colors.a(client, 255) : TXT3);
            text(c, cat, 40, y + 8, sel ? TXT : TXT2);
            if (total > 0) {
                String cnt = on + "/" + total;
                text(c, cnt, SIDE - 20 - tw(cnt), y + 8, TXT3);
            }
            final String cc = cat;
            hits.add(new Hit(14, y, SIDE - 22, 26, b -> {
                tab = cc; query = ""; scroll = 0; scrollTarget = 0; tabAt = System.currentTimeMillis();
            }, null, false));
        }

        text(c, "Profiles", 20, H - 64, TXT3);
        for (int i = 0; i < 3; i++) {
            int bx = 14 + i * 37, by = H - 50, n = i + 1;
            boolean hov = ux >= bx && ux < bx + 33 && uy >= by && uy < by + 24;
            rr(c, bx, by, 33, 24, 9, Colors.a(hov ? client : 0xFFFFFF, hov ? 80 : 14));
            text(c, "P" + n, bx + 16 - tw("P" + n) / 2, by + 7, TXT);
            hits.add(new Hit(bx, by, 33, 24, b -> {
                if (b == 0) { Config.load(Config.profile(n)); Notifications.push("Profile " + n + " loaded"); }
                else if (b == 1) { Config.save(Config.profile(n)); Notifications.push("Profile " + n + " saved"); }
            }, null, false));
        }
        text(c, "LMB load   RMB save", 14, H - 21, TXT3);

        // ---------------- header
        String title = adminView() ? "Admin" : (query.isEmpty() ? tab : "Search");
        MatrixStack ms = c.getMatrices();
        ms.push();
        ms.translate(SIDE + 16, 13, 0);
        ms.scale(1.5f, 1.5f, 1f);
        bold(c, title, 0, 0, TXT);
        ms.pop();

        int sbw = 140, sbx = W - 14 - sbw, sby = 12;
        rr(c, sbx, sby, sbw, 24, 12, Colors.a(0xFFFFFF, 16));
        ol(c, sbx, sby, sbw, 24, 12, Colors.a(searchFocus ? client : 0xFFFFFF, searchFocus ? 200 : 40));
        boolean placeholder = query.isEmpty() && !searchFocus;
        String shown = placeholder ? "Search..." : query + (searchFocus && (now / 500) % 2 == 0 ? "_" : "");
        text(c, UI.trim(textRenderer, shown, sbw - 24), sbx + 12, sby + 8, placeholder ? TXT3 : TXT);
        hits.add(new Hit(sbx, sby, sbw, 24, b -> searchFocus = true, null, false));

        vy0 = 48;
        vy1 = H - 28;
        if (adminView()) {
            drawAdmin(c, now, client, ux, uy);
        } else {
            drawCards(c, k, now, client, ux, uy);
        }

        text(c, "LMB toggle   RMB collapse   key label = bind   RSHIFT close", SIDE + 16, H - 17, TXT3);
    }

    private void drawCards(DrawContext c, float k, long now, int client, int ux, int uy) {
        int cx0 = SIDE + 12, cx1 = W - 14;
        scroll += (scrollTarget - scroll) * k;
        int[] colY = new int[2];
        String q = query.toLowerCase(Locale.ROOT);
        scissor(c, cx0 - 6, vy0, cx1 + 6, vy1);
        int idx = 0;
        for (Feature m : Modules.ALL) {
            boolean match = q.isEmpty() ? m.category.equals(tab) : m.name.toLowerCase(Locale.ROOT).contains(q);
            if (!match) continue;
            m.open += ((m.collapsed ? 0f : 1f) - m.open) * k;
            int h = HDR + (m.settings.isEmpty() ? 0 : (int) ((m.settings.size() * ROW + 6) * m.open));
            int col = colY[0] <= colY[1] ? 0 : 1;
            float appear = ease((now - tabAt - idx * 45L) / 260f);
            int x = cx0 + col * (CARD_W + 10);
            int y = vy0 + colY[col] - (int) scroll + (int) ((1f - appear) * 12f);
            cardAlpha = appear;
            drawCard(c, m, x, y, CARD_W, h, client, k, ux, uy);
            colY[col] += h + 9;
            idx++;
        }
        cardAlpha = 1f;
        c.disableScissor();
        maxScroll = Math.max(0, Math.max(colY[0], colY[1]) - 9 - (vy1 - vy0));
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
    }

    private void drawCard(DrawContext c, Feature m, int x, int y, int w, int h, int client, float k, int ux, int uy) {
        boolean hov = ux >= x && ux < x + w && uy >= y && uy < y + HDR && uy >= vy0 && uy < vy1;
        m.hover += ((hov ? 1f : 0f) - m.hover) * k;
        m.anim += ((m.enabled ? 1f : 0f) - m.anim) * k;

        rr(c, x, y, w, h, 12, Colors.a(0xFFFFFF, 11 + (int) (8 * m.hover)));
        ol(c, x, y, w, h, 12, Colors.a(Colors.lerp(0xFFFFFF, client, m.anim * 0.85f), 26 + (int) (120 * m.anim)));

        text(c, m.name, x + 14, y + 8, TXT);

        String bt = listening == m ? "..." : (m.bind < 0 ? "n/a" : keyName(m.bind));
        int bw = tw(bt);
        int bx = x + w - 48 - bw;
        text(c, bt, bx, y + 8, listening == m ? TXT : (m.bind < 0 ? TXT3 : Colors.a(client, 255)));

        int sx = x + w - 38, sy = y + 6;
        rr(c, sx, sy, 26, 14, 7, Colors.a(Colors.lerp(0x3A3B4A, client, m.anim), 255));
        rr(c, sx + 2 + (int) (m.anim * 12), sy + 2, 10, 10, 5, 0xFFFFFFFF);

        hits.add(new Hit(x, y, w, HDR, b -> {
            if (b == 0) m.toggle();
            else if (b == 1 && !m.settings.isEmpty()) m.collapsed = !m.collapsed;
        }, null, true));
        hits.add(new Hit(bx - 3, y + 4, bw + 6, 18, b -> {
            if (b == 0) listening = (prevListening == m) ? null : m;
            else if (b == 1) m.bind = -1;
        }, null, true));

        if (m.settings.isEmpty() || m.open < 0.02f) return;
        scissor(c, x, y + HDR, x + w, y + h - 1);
        int ry = y + HDR + 2;
        boolean interactive = m.open > 0.95f;
        for (Setting s : m.settings) {
            drawRow(c, s, x, ry, w, client, k, interactive);
            ry += ROW;
        }
        c.disableScissor();
    }

    private void drawRow(DrawContext c, Setting s, int x, int ry, int w, int client, float k, boolean interactive) {
        text(c, s.name, x + 14, ry + 7, TXT2);
        switch (s.type) {
            case BOOL -> {
                s.anim += ((s.on ? 1f : 0f) - s.anim) * k;
                int sx = x + w - 14 - 26, sy = ry + 4;
                rr(c, sx, sy, 26, 14, 7, Colors.a(Colors.lerp(0x3A3B4A, client, s.anim), 255));
                rr(c, sx + 2 + (int) (s.anim * 12), sy + 2, 10, 10, 5, 0xFFFFFFFF);
                if (interactive) hits.add(new Hit(x, ry, w, ROW, b -> s.on = !s.on, null, true));
            }
            case SLIDER -> {
                s.anim += (s.num - s.anim) * k;
                final int tw = 62;
                final int tx = x + w - 14 - tw;
                float frac = clamp01((s.anim - s.min) / (s.max - s.min));
                int fw = (int) (tw * frac);
                rr(c, tx, ry + 10, tw, 3, 1, 0xFF3A3B4A);
                rr(c, tx, ry + 10, fw, 3, 1, Colors.a(client, 255));
                rr(c, tx + fw - 4, ry + 7, 9, 9, 4, 0xFFFFFFFF);
                String val = s.display();
                text(c, val, tx - 8 - tw(val), ry + 7, TXT);
                if (interactive) {
                    hits.add(new Hit(tx - 4, ry, tw + 8, ROW, NOOP, mxd -> {
                        float t = (float) Math.max(0, Math.min(1, (mxd - tx) / tw));
                        s.num = Math.round((s.min + t * (s.max - s.min)) * 100f) / 100f;
                    }, true));
                }
            }
            case MODE -> {
                int bx = x + w - 14 - 82;
                rr(c, bx, ry + 3, 82, 16, 8, Colors.a(0xFFFFFF, 18));
                text(c, s.modes[s.mode], bx + 9, ry + 7, TXT);
                int ax = bx + 82 - 14;
                c.fill(ax, ry + 9, ax + 7, ry + 10, A(0xCCFFFFFF));
                c.fill(ax + 1, ry + 10, ax + 6, ry + 11, A(0xCCFFFFFF));
                c.fill(ax + 2, ry + 11, ax + 5, ry + 12, A(0xCCFFFFFF));
                c.fill(ax + 3, ry + 12, ax + 4, ry + 13, A(0xCCFFFFFF));
                if (interactive) {
                    hits.add(new Hit(bx, ry + 3, 82, 16, b -> s.mode = Math.floorMod(s.mode + (b == 1 ? -1 : 1), s.modes.length), null, true));
                }
            }
        }
    }

    // ------------------------------------------------------------------ admin panel
    private void abtn(DrawContext c, int x, int y, int w, String label, String cmd, int client, int ux, int uy, boolean danger) {
        boolean hov = ux >= x && ux < x + w && uy >= y && uy < y + 22;
        int base = danger ? 0xFF5A5A : client;
        rr(c, x, y, w, 22, 11, Colors.a(hov ? base : 0xFFFFFF, hov ? 90 : 16));
        ol(c, x, y, w, 22, 11, Colors.a(hov ? base : 0xFFFFFF, hov ? 200 : 40));
        text(c, label, x + (w - tw(label)) / 2, y + 7, TXT);
        hits.add(new Hit(x, y, w, 22, b -> { if (b == 0) Admin.cmd(cmd); }, null, false));
    }

    private void arow(DrawContext c, String label, int y, String[] names, String[] cmds, int client, int ux, int uy) {
        int x0 = SIDE + 16;
        text(c, label, x0, y + 7, TXT2);
        for (int i = 0; i < names.length; i++) abtn(c, x0 + 96 + i * 84, y, 78, names[i], cmds[i], client, ux, uy, false);
    }

    private void drawAdmin(DrawContext c, long now, int client, int ux, int uy) {
        int x0 = SIDE + 16, y0 = 54;
        if (!Admin.unlocked()) {
            bold(c, "Admin panel is locked", x0, y0, TXT);
            text(c, Admin.hasKey() ? "Enter your passphrase to unlock." : "First time: choose a passphrase (4+ characters).", x0, y0 + 16, TXT2);
            int fw = 230;
            rr(c, x0, y0 + 40, fw, 28, 14, Colors.a(0xFFFFFF, 16));
            ol(c, x0, y0 + 40, fw, 28, 14, Colors.a(passFocus ? client : 0xFFFFFF, passFocus ? 200 : 40));
            String masked = "*".repeat(pass.length()) + (passFocus && (now / 500) % 2 == 0 ? "_" : "");
            text(c, masked.isEmpty() ? "passphrase" : masked, x0 + 14, y0 + 49, masked.isEmpty() ? TXT3 : TXT);
            hits.add(new Hit(x0, y0 + 40, fw, 28, b -> passFocus = true, null, false));
            String lbl = Admin.hasKey() ? "Unlock" : "Create";
            int bx = x0 + fw + 10;
            rr(c, bx, y0 + 40, 90, 28, 14, Colors.a(client, 110));
            ol(c, bx, y0 + 40, 90, 28, 14, Colors.a(client, 220));
            text(c, lbl, bx + (90 - tw(lbl)) / 2, y0 + 49, TXT);
            hits.add(new Hit(bx, y0 + 40, 90, 28, b -> submitPass(), null, false));
            if (!passMsg.isEmpty()) text(c, passMsg, x0, y0 + 80, 0xFFFF8080);
            text(c, "Every button here only sends a normal chat command, so the world / server", x0, y0 + 112, TXT3);
            text(c, "still decides what you are allowed to do (op / cheats needed).", x0, y0 + 124, TXT3);
            return;
        }

        String[] none = {};
        arow(c, "Game mode", y0, new String[]{"Survival", "Creative", "Adventure", "Spectator"},
                new String[]{"gamemode survival", "gamemode creative", "gamemode adventure", "gamemode spectator"}, client, ux, uy);
        arow(c, "Time", y0 + 28, new String[]{"Day", "Noon", "Night", "Midnight"},
                new String[]{"time set day", "time set noon", "time set night", "time set midnight"}, client, ux, uy);
        arow(c, "Weather", y0 + 56, new String[]{"Clear", "Rain", "Thunder"},
                new String[]{"weather clear", "weather rain", "weather thunder"}, client, ux, uy);
        arow(c, "Difficulty", y0 + 84, new String[]{"Peaceful", "Easy", "Normal", "Hard"},
                new String[]{"difficulty peaceful", "difficulty easy", "difficulty normal", "difficulty hard"}, client, ux, uy);
        arow(c, "Keep inventory", y0 + 112, new String[]{"On", "Off"},
                new String[]{"gamerule keepInventory true", "gamerule keepInventory false"}, client, ux, uy);
        arow(c, "Day cycle", y0 + 140, new String[]{"On", "Off"},
                new String[]{"gamerule doDaylightCycle true", "gamerule doDaylightCycle false"}, client, ux, uy);
        arow(c, "Me", y0 + 168, new String[]{"Heal", "Feed", "Clear fx", "Clear drops"},
                new String[]{"effect give @s minecraft:instant_health 1 10 true", "effect give @s minecraft:saturation 1 10 true",
                        "effect clear @s", "kill @e[type=item]"}, client, ux, uy);

        text(c, "Players", x0, y0 + 200 + 5, TXT2);
        MinecraftClient mc = MinecraftClient.getInstance();
        int row = 0;
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                String name = e.getProfile().getName();
                if (mc.player != null && name.equals(mc.player.getGameProfile().getName())) continue;
                if (row >= 3) break;
                int y = y0 + 198 + row * 25;
                text(c, UI.trim(textRenderer, name, 150), x0 + 96, y + 7, TXT);
                abtn(c, x0 + 258, y, 48, "TP", "tp @s " + name, client, ux, uy, false);
                abtn(c, x0 + 312, y, 54, "Kick", "kick " + name, client, ux, uy, true);
                row++;
            }
        }
        if (row == 0) text(c, "nobody else is online", x0 + 96, y0 + 203, TXT3);

        int lx = W - 14 - 70, ly = 12 + 30;
        rr(c, lx, ly - 4, 70, 22, 11, Colors.a(0xFFFFFF, 16));
        ol(c, lx, ly - 4, 70, 22, 11, Colors.a(0xFFFFFF, 40));
        text(c, "Lock", lx + (70 - tw("Lock")) / 2, ly + 3, TXT);
        hits.add(new Hit(lx, ly - 4, 70, 22, b -> { Admin.lock(); tab = Modules.CATEGORIES[0]; query = ""; }, null, false));
    }

    private void submitPass() {
        if (Admin.submit(pass)) { passMsg = ""; pass = ""; passFocus = false; Notifications.push("Admin unlocked"); }
        else passMsg = Admin.hasKey() ? "Wrong passphrase" : "Use at least 4 characters";
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing) return true;
        MouseTrail.click(mx, my);
        double ux = (mx - ox) / curS, uy = (my - oy) / curS;
        prevListening = listening;
        listening = null;
        searchFocus = false;
        passFocus = false;
        if (button == 0 && Island.click(mx, my)) return true;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(ux, uy)) continue;
            if (h.clip() && (uy < vy0 || uy >= vy1)) continue;
            h.click().accept(button);
            if (h.drag() != null && button == 0) { dragging = h; h.drag().accept(ux); }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { dragging.drag().accept((mx - ox) / curS); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) amount * 30f));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (passFocus && pass.length() < 32 && chr >= 32) { pass += chr; return true; }
        if (searchFocus && query.length() < 16 && chr >= 32) {
            query += chr;
            scroll = 0; scrollTarget = 0; tabAt = System.currentTimeMillis();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) listening.bind = -1;
            else listening.bind = key;
            listening = null;
            return true;
        }
        if (passFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!pass.isEmpty()) pass = pass.substring(0, pass.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_ENTER) { submitPass(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { passFocus = false; return true; }
            return true;
        }
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
                tabAt = System.currentTimeMillis();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) { searchFocus = false; return true; }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT && System.currentTimeMillis() - opened > 250) {
            close();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Smooth exit: start the animation, the real close happens in render(). */
    @Override
    public void close() {
        if (!closing) { closing = true; closeAt = System.currentTimeMillis(); }
    }

    @Override
    public void removed() { Config.save(); }
}
