package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Minimal glass main menu: big clock, greeting, three buttons, a small dock and a background picker. */
public class BrandTitleScreen extends Screen {
    private final long opened = System.currentTimeMillis();
    private final GBtn single, multi, options, client, bg, quit, prev, next, def, folder;
    private final GBtn[] all;
    private boolean bgOpen;

    public BrandTitleScreen() {
        super(Text.literal(Brand.NAME));
        MinecraftClient mc = MinecraftClient.getInstance();
        single = new GBtn(Text.translatable("menu.singleplayer").getString(), () -> mc.setScreen(new WorldSelectScreen(this))).icon("user");
        multi = new GBtn(Text.translatable("menu.multiplayer").getString(), () -> mc.setScreen(new ServerSelectScreen(this))).icon("ping");
        options = new GBtn(Text.translatable("menu.options").getString(), () -> mc.setScreen(new OptionsScreen(this, mc.options))).icon("cpu");
        client = new GBtn("", () -> mc.setScreen(new MenuScreen())).icon("logo");
        bg = new GBtn("", () -> bgOpen = !bgOpen).icon("fps");
        quit = new GBtn("", mc::scheduleStop).icon("bolt");
        prev = new GBtn("<", Backgrounds::prev);
        next = new GBtn(">", Backgrounds::next);
        def = new GBtn("Default", Backgrounds::useDefault);
        folder = new GBtn("Open folder", Backgrounds::openFolder);
        all = new GBtn[]{single, multi, options, client, bg, quit, prev, next, def, folder};
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void filesDragged(List<Path> paths) { Backgrounds.addFiles(paths); }

    private static String greeting() {
        int h = LocalTime.now().getHour();
        return h < 5 ? "Good night" : h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        float t = (now - opened) / 1000f;
        float fade = Math.min(1f, t / 0.7f);
        int accent = Colors.client();
        int cx = width / 2;

        Backgrounds.draw(c, width, height, accent, t);
        Notifications_tick();

        // brand
        Icons.draw(c, "logo", 14, 14, 2, Colors.a(accent, 255));
        UI.drawBold(c, textRenderer, Brand.NAME, 34, 13, 0xFFFFFFFF);
        UI.draw(c, textRenderer, "v" + Brand.VERSION, 34, 24, 0xAAFFFFFF);

        // clock
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        MatrixStack ms = c.getMatrices();
        int top = Math.max(24, height / 2 - 150);
        ms.push();
        ms.translate(cx, top, 0);
        ms.scale(4.2f, 4.2f, 1f);
        UI.drawBold(c, textRenderer, time, -UI.wb(textRenderer, time) / 2, 0, Colors.a(0xFFFFFF, (int) (255 * fade)));
        ms.pop();
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()));
        UI.centered(c, textRenderer, date, cx, top + 46, Colors.a(0xFFFFFF, (int) (200 * fade)));
        String hello = greeting() + ", " + MinecraftClient.getInstance().getSession().getUsername();
        UI.centered(c, textRenderer, hello, cx, top + 62, Colors.a(accent, (int) (255 * fade)));

        // buttons
        int bw = Math.min(260, width - 40), bh = 34, y = top + 90;
        single.at(cx - bw / 2, y, bw, bh).primary();
        multi.at(cx - bw / 2, y + 42, bw, bh);
        options.at(cx - bw / 2, y + 84, bw, bh);
        int dy = y + 132;
        client.at(cx - 57, dy, 34, 34);
        bg.at(cx - 17, dy, 34, 34);
        quit.at(cx + 23, dy, 34, 34);

        // background picker
        if (bgOpen) {
            int pw = 260, ph = 92, px = cx - pw / 2, py = dy + 44;
            Draw.glass(c, px, py, pw, ph, 12, accent, 1f);
            UI.drawBold(c, textRenderer, "Background", px + 12, py + 9, 0xFFFFFFFF);
            String name = UI.trim(textRenderer, Backgrounds.currentName(), pw - 24);
            UI.draw(c, textRenderer, name, px + 12, py + 22, 0xAAFFFFFF);
            prev.at(px + 12, py + 38, 30, 22);
            next.at(px + 46, py + 38, 30, 22);
            def.at(px + 82, py + 38, 70, 22);
            folder.at(px + 156, py + 38, pw - 168, 22);
            UI.draw(c, textRenderer, "Drop a photo into the window to use it", px + 12, py + 68, 0x99FFFFFF);
        } else {
            prev.at(-100, -100, 0, 0); next.at(-100, -100, 0, 0); def.at(-100, -100, 0, 0); folder.at(-100, -100, 0, 0);
        }

        for (GBtn b : all) if (b.w > 0) b.render(c, textRenderer, mx, my, accent);

        UI.centered(c, textRenderer, "Click the dock: client menu  |  backgrounds  |  quit", cx, height - 16, 0x66FFFFFF);

        MouseTrail.update(mx, my);
        MouseTrail.render(c, accent, fade);
    }

    private void Notifications_tick() { /* the island lives in the HUD; nothing to draw here */ }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        MouseTrail.click(mx, my);
        if (button == 0) {
            for (GBtn b : all) if (b.w > 0 && b.click(mx, my)) return true;
            if (bgOpen) bgOpen = false;
        }
        return super.mouseClicked(mx, my, button);
    }
}
