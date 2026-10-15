package dev.burmalda;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Main-menu backgrounds: photos from your PC (drop them onto the game window, or put them into
 * config/nebula/backgrounds) or the built-in animated aurora.
 */
public final class Backgrounds {
    private static final List<Path> FILES = new ArrayList<>();
    private static int index = -1;
    private static boolean inited;

    private static Identifier texId;
    private static int texW, texH;
    private static String loaded = "";
    private static volatile NativeImage pending;
    private static volatile String pendingFor = "";
    private static volatile boolean loading, pendingReady;

    private static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve(Brand.ID).resolve("backgrounds"); }

    private static boolean isImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".bmp") || n.endsWith(".gif");
    }

    public static void init() {
        if (inited) return;
        inited = true;
        try { Files.createDirectories(dir()); } catch (IOException ignored) { }
        refresh();
        try {
            Path sel = dir().resolve("selected.txt");
            if (Files.exists(sel)) {
                String name = Files.readString(sel).trim();
                for (int i = 0; i < FILES.size(); i++) if (FILES.get(i).getFileName().toString().equals(name)) index = i;
            }
        } catch (IOException ignored) { }
    }

    public static void refresh() {
        FILES.clear();
        try (Stream<Path> s = Files.list(dir())) {
            s.filter(Files::isRegularFile).filter(Backgrounds::isImage).sorted().forEach(FILES::add);
        } catch (IOException ignored) { }
        if (index >= FILES.size()) index = FILES.size() - 1;
    }

    private static void saveSel() {
        try { Files.writeString(dir().resolve("selected.txt"), index < 0 ? "" : FILES.get(index).getFileName().toString()); }
        catch (IOException ignored) { }
    }

    public static void next() {
        init(); refresh();
        if (FILES.isEmpty()) { index = -1; return; }
        index = index + 1 >= FILES.size() ? -1 : index + 1;
        saveSel();
    }

    public static void prev() {
        init(); refresh();
        if (FILES.isEmpty()) { index = -1; return; }
        index = index - 1 < -1 ? FILES.size() - 1 : index - 1;
        saveSel();
    }

    public static void useDefault() { init(); index = -1; saveSel(); }

    public static String currentName() {
        init();
        return index < 0 || index >= FILES.size() ? "Default (aurora)" : FILES.get(index).getFileName().toString();
    }

    public static int count() { init(); return FILES.size(); }

    public static void openFolder() {
        init();
        try { Util.getOperatingSystem().open(dir().toFile()); } catch (Throwable ignored) { }
    }

    /** Called when files are dropped onto the window. */
    public static void addFiles(List<Path> paths) {
        init();
        int added = 0;
        String last = null;
        for (Path p : paths) {
            if (!isImage(p)) continue;
            try {
                Path target = dir().resolve(p.getFileName().toString());
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                added++;
                last = target.getFileName().toString();
            } catch (IOException ignored) { }
        }
        if (added == 0) { Notifications.push("Drop a PNG / JPG image"); return; }
        refresh();
        for (int i = 0; i < FILES.size(); i++) if (FILES.get(i).getFileName().toString().equals(last)) index = i;
        saveSel();
        Notifications.push(added == 1 ? "Background set" : added + " backgrounds added");
    }

    private static NativeImage read(Path p) {
        try (InputStream in = Files.newInputStream(p)) { return NativeImage.read(in); } catch (Throwable ignored) { }
        try { return ImgFallback.read(p); } catch (Throwable t) { return null; }
    }

    // ---------------------------------------------------------------- drawing
    public static void draw(DrawContext c, int w, int h, int client, float t) {
        init();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (pendingReady) {
            pendingReady = false;
            NativeImage img = pending;
            pending = null;
            release(mc);
            loaded = pendingFor;
            if (img != null) {
                texW = img.getWidth(); texH = img.getHeight();
                texId = mc.getTextureManager().registerDynamicTexture("nebula_bg", new NativeImageBackedTexture(img));
            }
        }
        String want = index >= 0 && index < FILES.size() ? FILES.get(index).toString() : "";
        if (!want.equals(loaded) && !loading) {
            if (want.isEmpty()) { release(mc); loaded = ""; }
            else {
                loading = true;
                final String f = want;
                Thread th = new Thread(() -> {
                    NativeImage img = read(Path.of(f));
                    pending = img; pendingFor = f; pendingReady = true; loading = false;
                }, "nebula-bg");
                th.setDaemon(true);
                th.start();
            }
        }

        if (texId != null && !loaded.isEmpty()) {
            float scale = Math.max(w / (float) texW, h / (float) texH);
            float rw = w / scale, rh = h / scale;
            c.drawTexture(texId, 0, 0, w, h, (texW - rw) / 2f, (texH - rh) / 2f, (int) rw, (int) rh, texW, texH);
            c.fillGradient(0, 0, w, h, Colors.a(0x000000, 70), Colors.a(0x000000, 150));
            c.fillGradient(0, 0, w, h, Colors.a(client, 18), Colors.a(client, 0));
        } else {
            aurora(c, w, h, client, t);
        }
    }

    private static void release(MinecraftClient mc) {
        if (texId != null) { mc.getTextureManager().destroyTexture(texId); texId = null; }
    }

    private static void aurora(DrawContext c, int w, int h, int client, float t) {
        c.fillGradient(0, 0, w, h, 0xFF05060D, Colors.a(Colors.lerp(0x05060D, client, 0.22f), 255));
        int[] cols = {client, Colors.lerp(client, 0x40C8FF, 0.6f), Colors.lerp(client, 0xFF60C0, 0.5f)};
        int stripes = 56;
        int sw = w / stripes + 1;
        for (int b = 0; b < 3; b++) {
            double center = w * (0.22 + 0.28 * b + 0.12 * Math.sin(t * 0.18 + b * 2.1));
            double width = w * (0.16 + 0.04 * Math.sin(t * 0.23 + b));
            double pulse = 0.65 + 0.35 * Math.sin(t * 0.4 + b * 1.7);
            for (int i = 0; i < stripes; i++) {
                int x = i * sw;
                double d = (x + sw / 2.0 - center) / width;
                int a = (int) (80 * Math.exp(-d * d) * pulse);
                if (a < 2) continue;
                c.fillGradient(x, (int) (h * 0.05), x + sw, (int) (h * 0.95), Colors.a(cols[b], a), Colors.a(cols[b], 0));
            }
        }
        for (int i = 0; i < 60; i++) {
            float px = ((i * 73) % 100) / 100f * w + (float) Math.sin(t * 0.25 + i) * 12f;
            float py = (((i * 37) % 100) / 100f * h + t * (4 + (i % 5) * 2)) % h;
            int s = i % 4 == 0 ? 2 : 1;
            c.fill((int) px, (int) py, (int) px + s, (int) py + s, Colors.a(0xFFFFFF, 25 + (i % 4) * 14));
        }
    }

    private Backgrounds() { }
}
