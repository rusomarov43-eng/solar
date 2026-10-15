package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Glass world picker. Play / create use the vanilla loaders, so every world type keeps working. */
public class WorldSelectScreen extends Screen {
    private static final int ROW = 58;
    private final Screen parent;
    private volatile List<LevelSummary> worlds = new ArrayList<>();
    private volatile boolean loading = true;
    private volatile String error = "";
    private int selected = -1, lastClickIdx = -1;
    private long lastClick;
    private float scroll, target;
    private boolean confirmDelete;
    private final Map<String, Identifier> icons = new HashMap<>();
    private final GBtn play, create, delete, classic, back;
    private final GBtn[] btns;
    private final long opened = System.currentTimeMillis();

    public WorldSelectScreen(Screen parent) {
        super(Text.literal(Brand.NAME));
        this.parent = parent;
        play = new GBtn("Play", this::playSelected).primary().icon("play");
        create = new GBtn("Create new", () -> CreateWorldScreen.create(MinecraftClient.getInstance(), this));
        delete = new GBtn("Delete", this::deleteSelected);
        classic = new GBtn("Classic list", () -> MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this)));
        back = new GBtn("Back", this::close);
        btns = new GBtn[]{play, create, delete, classic, back};
    }

    @Override protected void init() { load(); }

    private void load() {
        MinecraftClient mc = MinecraftClient.getInstance();
        loading = true; error = ""; selected = -1; confirmDelete = false;
        try {
            LevelStorage.LevelList list = mc.getLevelStorage().getLevelList();
            mc.getLevelStorage().loadSummaries(list).thenAcceptAsync(l -> {
                List<LevelSummary> copy = new ArrayList<>(l);
                Collections.sort(copy);
                worlds = copy;
                selected = copy.isEmpty() ? -1 : 0;
                loading = false;
            }, mc).exceptionally(t -> { error = "Could not load your worlds"; loading = false; return null; });
        } catch (Exception e) {
            error = "Could not load your worlds"; loading = false;
        }
    }

    private LevelSummary sel() { return selected >= 0 && selected < worlds.size() ? worlds.get(selected) : null; }

    private void playSelected() {
        LevelSummary s = sel();
        if (s == null) return;
        if (s.isDisabled()) { Notifications.push("This world can't be opened"); return; }
        MinecraftClient.getInstance().createIntegratedServerLoader().start(this, s.getName());
    }

    private void deleteSelected() {
        LevelSummary s = sel();
        if (s == null) return;
        if (!confirmDelete) { confirmDelete = true; return; }
        confirmDelete = false;
        try (LevelStorage.Session session = MinecraftClient.getInstance().getLevelStorage().createSession(s.getName())) {
            session.deleteSessionLock();
        } catch (Exception e) {
            Notifications.push("Could not delete the world");
        }
        load();
    }

    private Identifier icon(LevelSummary s) {
        Identifier id = icons.get(s.getName());
        if (id != null) return id;
        if (icons.containsKey(s.getName())) return null;
        icons.put(s.getName(), null);
        try {
            Path p = s.getIconPath();
            if (p != null && Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    NativeImage img = NativeImage.read(in);
                    id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("nebula_world", new NativeImageBackedTexture(img));
                    icons.put(s.getName(), id);
                }
            }
        } catch (Throwable ignored) { }
        return icons.get(s.getName());
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        float t = (System.currentTimeMillis() - opened) / 1000f;
        int accent = Colors.client();
        Backgrounds.draw(c, width, height, accent, t);
        c.fillGradient(0, 0, width, height, Colors.a(0x000000, 40), Colors.a(0x000000, 110));

        UI.drawBold(c, textRenderer, "Singleplayer", 24, 20, 0xFFFFFFFF);
        UI.draw(c, textRenderer, worlds.size() + " worlds", 24, 33, 0x99FFFFFF);

        int pw = Math.min(520, width - 40), px = (width - pw) / 2, py = 56, ph = height - 56 - 70;
        Draw.glass(c, px - 8, py - 8, pw + 16, ph + 16, 14, accent, 1f);
        target = Math.max(0, Math.min(Math.max(0, worlds.size() * ROW - ph), target));
        scroll += (target - scroll) * 0.25f;

        if (loading) UI.centered(c, textRenderer, "Loading...", width / 2, py + 30, 0xAAFFFFFF);
        else if (!error.isEmpty()) UI.centered(c, textRenderer, error, width / 2, py + 30, 0xFFFF7777);
        else if (worlds.isEmpty()) UI.centered(c, textRenderer, "No worlds yet - press \"Create new\"", width / 2, py + 30, 0xAAFFFFFF);

        c.enableScissor(px, py, px + pw, py + ph);
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        for (int i = 0; i < worlds.size(); i++) {
            int y = py + i * ROW - (int) scroll;
            if (y + ROW < py || y > py + ph) continue;
            LevelSummary s = worlds.get(i);
            boolean hov = mx >= px && mx < px + pw && my >= y && my < y + ROW - 4 && my >= py && my < py + ph;
            boolean on = i == selected;
            Draw.rrect(c, px, y, pw, ROW - 6, 10,
                    on ? Colors.a(Colors.lerp(0x10121E, accent, 0.30f), 210) : Colors.a(0xFFFFFF, hov ? 22 : 10));
            if (on) Draw.outline(c, px, y, pw, ROW - 6, 10, Colors.a(accent, 200));

            Draw.rrect(c, px + 8, y + 6, 40, 40, 8, 0xFF14101F);
            Identifier ic = icon(s);
            if (ic != null) c.drawTexture(ic, px + 8, y + 6, 40, 40, 0f, 0f, 64, 64, 64, 64);
            else Icons.draw(c, "pin", px + 22, y + 20, 2, 0xFF6E6890);

            UI.drawBold(c, textRenderer, UI.trim(textRenderer, s.getDisplayName(), pw - 80), px + 58, y + 8, 0xFFFFFFFF);
            String mode = s.getGameMode().getName();
            mode = mode.isEmpty() ? mode : Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
            String info = fmt.format(new Date(s.getLastPlayed())) + "  |  " + mode
                    + (s.isHardcore() ? "  |  Hardcore" : "") + (s.hasCheats() ? "  |  Cheats" : "");
            UI.draw(c, textRenderer, UI.trim(textRenderer, info, pw - 80), px + 58, y + 22, 0xAAFFFFFF);
            UI.draw(c, textRenderer, UI.trim(textRenderer, s.getVersion().getString(), pw - 80), px + 58, y + 34, 0x77FFFFFF);
        }
        c.disableScissor();

        delete.label = confirmDelete ? "Sure?" : "Delete";
        delete.danger = confirmDelete;
        play.enabled = sel() != null && !sel().isDisabled();
        delete.enabled = sel() != null;
        int bw = 118, gap = 8, total = btns.length * bw + (btns.length - 1) * gap;
        int bx = (width - total) / 2, by = height - 52;
        for (int i = 0; i < btns.length; i++) btns[i].at(bx + i * (bw + gap), by, bw, 30);
        for (GBtn b : btns) b.render(c, textRenderer, mx, my, accent);

        MouseTrail.update(mx, my);
        MouseTrail.render(c, accent, 1f);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        MouseTrail.click(mx, my);
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (GBtn b : btns) if (b.click(mx, my)) return true;
        int pw = Math.min(520, width - 40), px = (width - pw) / 2, py = 56, ph = height - 56 - 70;
        if (mx >= px && mx < px + pw && my >= py && my < py + ph) {
            int i = (int) ((my - py + scroll) / ROW);
            if (i >= 0 && i < worlds.size()) {
                long now = System.currentTimeMillis();
                if (i == lastClickIdx && now - lastClick < 400) playSelected();
                selected = i; lastClickIdx = i; lastClick = now; confirmDelete = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        target -= (float) amount * 40f;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER) { playSelected(); return true; }
        if (key == GLFW.GLFW_KEY_DOWN && selected < worlds.size() - 1) { selected++; return true; }
        if (key == GLFW.GLFW_KEY_UP && selected > 0) { selected--; return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void close() { MinecraftClient.getInstance().setScreen(parent); }
}
