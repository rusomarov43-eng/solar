package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Card with head, name and HP of the entity you are aiming at / just hit. */
public final class TargetHud {
    private static float alpha, shownHp = 1f;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF8F8AA8;

    public static void render(DrawContext c, MinecraftClient mc, TextRenderer tr, int client, int sw, int sh) {
        Feature f = Modules.TARGET_HUD;
        LivingEntity t = TargetTracker.target;
        long now = System.currentTimeMillis();
        boolean show = f.enabled && t != null && !t.isRemoved() && now - TargetTracker.lastSeen < 3000;
        alpha += ((show ? 1f : 0f) - alpha) * 0.2f;
        if (t == null || alpha < 0.03f) return;

        float hp = Math.max(0f, t.getHealth());
        float max = Math.max(1f, t.getMaxHealth());
        shownHp += (Math.min(1f, hp / max) - shownHp) * 0.15f;

        int w = 150, h = 42;
        int x = sw / 2 + 20, y = sh / 2 + 20;
        Draw.panel(c, x, y, w, h, 7, Colors.a(0x0E0B16, (int) (235 * alpha)),
                Colors.a(client, (int) (130 * alpha)), client, alpha);
        if (alpha < 0.6f) return;

        String name = t.getName().getString();
        int hs = 30, hx = x + 6, hy = y + 6;
        Draw.rrect(c, hx - 1, hy - 1, hs + 2, hs + 2, 4, Colors.a(client, 200));
        if (f.b("Head") && t instanceof AbstractClientPlayerEntity p) {
            Identifier tex = p.getSkinTexture();
            c.drawTexture(tex, hx, hy, hs, hs, 8f, 8f, 8, 8, 64, 64);
            c.drawTexture(tex, hx, hy, hs, hs, 40f, 8f, 8, 8, 64, 64);
        } else {
            Draw.rrect(c, hx, hy, hs, hs, 3, 0xFF1C1730);
            String ch = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
            UI.draw(c, tr, ch, hx + hs / 2 - UI.w(tr, ch) / 2, hy + hs / 2 - 4, WHITE);
        }

        int tx = x + hs + 14;
        UI.draw(c, tr, UI.trim(tr, name, w - hs - 56), tx, y + 8, WHITE);
        if (f.b("Health text")) {
            String s = String.format(Locale.ROOT, "%.1f", hp);
            UI.draw(c, tr, s, x + w - 8 - UI.w(tr, s), y + 8, GRAY);
        }
        int bw = w - hs - 24, by = y + 25;
        Draw.rrect(c, tx, by, bw, 6, 3, 0xFF2A2540);
        Draw.rrect(c, tx, by, (int) (bw * Math.max(0f, Math.min(1f, shownHp))), 6, 3,
                Colors.a(Colors.lerp(0xFF4040, 0x40FF70, shownHp), 255));
    }

    private TargetHud() { }
}
