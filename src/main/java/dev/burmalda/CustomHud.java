package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Replacement hotbar and crosshair (called from InGameHudMixin). */
public final class CustomHud {
    public static void hotbar(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) return;
        TextRenderer tr = mc.textRenderer;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int client = Colors.client();

        int w = 9 * 20 + 8, h = 26;
        int x = sw / 2 - w / 2, y = sh - h - 4;
        int bg = Colors.a(0x0E0B16, (int) (Modules.HOTBAR.f("Opacity") * 255));
        Draw.panel(c, x, y, w, h, 7, bg, Colors.a(client, 110), client, 1f);

        int sel = p.getInventory().selectedSlot;
        for (int i = 0; i < 9; i++) {
            int sx = x + 4 + i * 20, sy = y + 3;
            if (i == sel) {
                Draw.rrect(c, sx, sy, 20, 20, 5, Colors.a(client, 255));
                Draw.rrect(c, sx + 1, sy + 1, 18, 18, 4, 0xFF241E3A);
            }
            ItemStack st = p.getInventory().main.get(i);
            if (!st.isEmpty()) {
                c.drawItem(st, sx + 2, sy + 2);
                c.drawItemInSlot(tr, st, sx + 2, sy + 2);
            }
        }

        ItemStack off = p.getOffHandStack();
        if (!off.isEmpty()) {
            int ox = x - 30;
            Draw.panel(c, ox, y + 1, 24, 24, 7, bg, Colors.a(client, 110), client, 1f);
            c.drawItem(off, ox + 4, y + 5);
            c.drawItemInSlot(tr, off, ox + 4, y + 5);
        }
    }

    public static void crosshair(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.options.getPerspective().isFirstPerson()) return;
        Feature f = Modules.CROSSHAIR;
        int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
        int size = (int) f.f("Size"), gap = (int) f.f("Gap"), th = (int) f.f("Thickness");
        int col = Colors.a(f.b("Client color") ? Colors.client() : 0xFFFFFF, 255);

        switch (f.m("Style")) {
            case 0, 3 -> {
                int g = f.m("Style") == 3 ? 0 : gap;
                int y0 = cy - th / 2, x0 = cx - th / 2;
                c.fill(cx - g - size, y0, cx - g, y0 + th, col);
                c.fill(cx + g, y0, cx + g + size, y0 + th, col);
                c.fill(x0, cy - g - size, x0 + th, cy - g, col);
                c.fill(x0, cy + g, x0 + th, cy + g + size, col);
            }
            case 1 -> c.fill(cx - th, cy - th, cx + th, cy + th, col);
            default -> {
                int r = gap + size / 2 + 2;
                for (int i = 0; i < 48; i++) {
                    double a = i * Math.PI * 2 / 48;
                    int px = cx + (int) Math.round(Math.cos(a) * r);
                    int py = cy + (int) Math.round(Math.sin(a) * r);
                    c.fill(px, py, px + th, py + th, col);
                }
            }
        }
    }

    private CustomHud() { }
}
