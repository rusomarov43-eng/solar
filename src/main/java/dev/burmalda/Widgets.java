package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Keystrokes + CPS, armor, effects and the stats panel. */
public final class Widgets {
    public static final long START = System.currentTimeMillis();
    private static final int BG = 0xE60E0B16, WHITE = 0xFFFFFFFF, GRAY = 0xFF8F8AA8;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final float[] press = new float[7];
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    private static boolean lWas, rWas;
    private static ItemStack[] icons;

    // ------------------------------------------------------------ keystrokes
    public static void keystrokes(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw, int sh) {
        Feature f = Modules.KEYSTROKES;
        long now = System.currentTimeMillis();
        long win = mc.getWindow().getHandle();
        boolean l = mc.currentScreen == null && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = mc.currentScreen == null && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (l && !lWas) LEFT.addLast(now);
        if (r && !rWas) RIGHT.addLast(now);
        lWas = l; rWas = r;
        while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.pollFirst();
        while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.pollFirst();

        boolean[] down = {
                mc.options.forwardKey.isPressed(), mc.options.leftKey.isPressed(),
                mc.options.backKey.isPressed(), mc.options.rightKey.isPressed(),
                l, r, mc.options.jumpKey.isPressed()};
        for (int i = 0; i < 7; i++) press[i] += ((down[i] ? 1f : 0f) - press[i]) * 0.35f;

        int k = 22, g = 2, total = 3 * k + 2 * g;
        int x = f.m("Side") == 0 ? sw - total - 6 : 6;
        int y = sh / 2 - 30;

        key(c, tr, x + k + g, y, k, k, "W", press[0], client, null);
        key(c, tr, x, y + k + g, k, k, "A", press[1], client, null);
        key(c, tr, x + k + g, y + k + g, k, k, "S", press[2], client, null);
        key(c, tr, x + 2 * (k + g), y + k + g, k, k, "D", press[3], client, null);
        int ny = y + 2 * (k + g);
        if (f.b("Mouse")) {
            int bw = (total - g) / 2;
            key(c, tr, x, ny, bw, k, "LMB", press[4], client, f.b("CPS") ? String.valueOf(LEFT.size()) : null);
            key(c, tr, x + bw + g, ny, total - bw - g, k, "RMB", press[5], client, f.b("CPS") ? String.valueOf(RIGHT.size()) : null);
            ny += k + g;
        }
        if (f.b("Space")) {
            key(c, tr, x, ny, total, 12, "", press[6], client, null);
            c.fill(x + 20, ny + 5, x + total - 20, ny + 7, Colors.a(0xFFFFFF, 150 + (int) (100 * press[6])));
        }
    }

    private static void key(DrawContext c, TextRenderer tr, int x, int y, int w, int h, String label,
                            float a, int client, String sub) {
        Draw.rrect(c, x - 1, y - 1, w + 2, h + 2, 2, Colors.a(Colors.lerp(0x2E2942, client, a), 255));
        Draw.rrect(c, x, y, w, h, 2, Colors.a(Colors.lerp(0x14101F, client, a * 0.7f), 235));
        if (label.isEmpty()) return;
        int col = a > 0.5f ? WHITE : 0xFFD8D4EA;
        if (sub == null) {
            UI.draw(c, tr, label, x + (w - UI.w(tr, label)) / 2, y + (h - 7) / 2, col);
        } else {
            UI.draw(c, tr, label, x + (w - UI.w(tr, label)) / 2, y + 3, col);
            UI.draw(c, tr, sub, x + (w - UI.w(tr, sub)) / 2, y + 12, a > 0.5f ? WHITE : Colors.a(client, 255));
        }
    }

    // ------------------------------------------------------------ armor
    public static void armor(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw, int sh) {
        var armor = mc.player.getInventory().armor;
        int n = 0;
        for (ItemStack s : armor) if (!s.isEmpty()) n++;
        if (n == 0) return;
        int w = 54, h = n * 20 + 6, x = sw - w - 6, y = sh - 22 - 8 - h;
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        int ry = y + 3;
        for (int i = 3; i >= 0; i--) {
            ItemStack s = armor.get(i);
            if (s.isEmpty()) continue;
            c.drawItem(s, x + 4, ry + 2);
            if (s.isDamageable()) {
                float pct = (s.getMaxDamage() - s.getDamage()) / (float) s.getMaxDamage();
                UI.draw(c, tr, Math.round(pct * 100) + "%", x + 24, ry + 6, Colors.a(Colors.lerp(0xFF4040, 0x40FF70, pct), 255));
            }
            ry += 20;
        }
    }

    // ------------------------------------------------------------ effects
    public static void effects(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw, int y) {
        List<StatusEffectInstance> rows = new ArrayList<>(mc.player.getStatusEffects());
        if (rows.isEmpty()) return;
        int w = 124, h = rows.size() * 12 + 18, x = sw - w - 6;
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        Icons.draw(c, "potion", x + 7, y + 5, 1, Colors.a(client, 255));
        UI.draw(c, tr, "Effects", x + 19, y + 5, WHITE);
        for (int i = 0; i < rows.size(); i++) {
            StatusEffectInstance e = rows.get(i);
            int ry = y + 17 + i * 12;
            Draw.rrect(c, x + 7, ry + 1, 4, 6, 1, Colors.a(e.getEffectType().getColor(), 255));
            String name = e.getEffectType().getName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            UI.draw(c, tr, UI.trim(tr, name, 66), x + 16, ry, WHITE);
            String time;
            if (e.getDuration() < 0) time = "**:**";
            else time = String.format(Locale.ROOT, "%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            UI.draw(c, tr, time, x + w - 7 - UI.w(tr, time), ry, GRAY);
        }
    }

    // ------------------------------------------------------------ stats (clock, speed, playtime, item counter)
    public static void stats(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int y) {
        Feature f = Modules.STATS;
        List<String> ic = new ArrayList<>(), tx = new ArrayList<>();
        if (f.b("Clock")) { ic.add("clock"); tx.add(LocalTime.now().format(TIME)); }
        if (f.b("Speed")) {
            double dx = mc.player.getX() - mc.player.prevX, dz = mc.player.getZ() - mc.player.prevZ;
            ic.add("bolt"); tx.add(String.format(Locale.ROOT, "%.1f b/s", Math.sqrt(dx * dx + dz * dz) * 20));
        }
        if (f.b("Playtime")) {
            long s = (System.currentTimeMillis() - START) / 1000;
            ic.add("play"); tx.add(String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60));
        }
        if (f.b("Day") && mc.world != null) { ic.add("clock"); tx.add("Day " + (mc.world.getTimeOfDay() / 24000L + 1)); }
        if (f.b("Biome") && mc.world != null) {
            String bn = mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(k -> k.getValue().getPath().replace('_', ' ')).orElse("unknown");
            ic.add("pin"); tx.add(bn);
        }
        if (f.b("Kills")) { ic.add("heart"); tx.add(HitEffects.kills + " K  " + HitEffects.deaths + " D"); }
        boolean items = f.b("Items");
        if (ic.isEmpty() && !items) return;

        int x = 6, w = items ? 136 : 92;
        int h = 6 + ic.size() * 12 + (items ? 22 : 0);
        Draw.panel(c, x, y, w, h, 6, BG, Colors.a(client, 90), client, 1f);
        int ry = y + 4;
        for (int i = 0; i < ic.size(); i++) {
            Icons.draw(c, ic.get(i), x + 7, ry + 1, 1, Colors.a(client, 255));
            UI.draw(c, tr, tx.get(i), x + 19, ry, WHITE);
            ry += 12;
        }
        if (items) {
            if (icons == null) icons = new ItemStack[]{
                    new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.GOLDEN_APPLE),
                    new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.EXPERIENCE_BOTTLE)};
            Item[] it = {Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENDER_PEARL, Items.EXPERIENCE_BOTTLE};
            var inv = mc.player.getInventory();
            for (int i = 0; i < 4; i++) {
                int cx = x + 5 + i * 32;
                int n = inv.count(it[i]) + (i == 1 ? inv.count(Items.ENCHANTED_GOLDEN_APPLE) : 0);
                c.drawItem(icons[i], cx, ry + 1);
                UI.draw(c, tr, String.valueOf(n), cx + 17, ry + 5, n > 0 ? WHITE : GRAY);
            }
        }
    }

    private Widgets() { }
}
