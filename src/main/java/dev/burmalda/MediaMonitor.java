package dev.burmalda;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads "now playing" from Windows (browser media sessions) through a small hidden PowerShell helper.
 * With "YouTube only" on, a session only counts if a browser window title contains "YouTube" and the
 * track title. Windows only; on other systems it simply stays empty.
 */
public final class MediaMonitor {
    private static Process proc;
    private static Path dir;
    private static boolean hook, failed;
    private static long startedAt;
    private static Integer lastSrc;

    private static volatile boolean has, playing;
    private static volatile String title = "", artist = "", coverPath = "";
    private static volatile long pos, dur, stamp, changed, lastPlayingAt;
    private static volatile int serial;

    private static int loadedSerial;
    private static Identifier coverId;
    private static int coverW, coverH;

    // ---- public state
    public static boolean hasTrack() { return has && !title.isEmpty(); }
    public static boolean visible() {
        return hasTrack() && (playing || System.currentTimeMillis() - lastPlayingAt < 6000);
    }
    public static boolean isPlaying() { return playing; }
    public static String title() { return title; }
    public static String artist() { return artist; }
    public static long durMs() { return dur; }
    public static long changedAt() { return changed; }
    public static Identifier coverId() { return coverId; }
    public static int coverW() { return coverW; }
    public static int coverH() { return coverH; }

    public static long posNow() {
        long p = pos + (playing ? System.currentTimeMillis() - stamp : 0);
        if (dur > 0) p = Math.min(p, dur);
        return Math.max(0, p);
    }

    public static void send(String cmd) {
        if (dir == null) return;
        try { Files.writeString(dir.resolve("cmd.txt"), cmd); } catch (IOException ignored) { }
    }

    // ---- lifecycle (called every client tick)
    public static void update(boolean run, int source) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return;
        if (run && proc == null && !failed) start();
        if (!run && proc != null) stop();

        if (proc != null) {
            if (!proc.isAlive()) {
                if (System.currentTimeMillis() - startedAt < 5000) failed = true;
                proc = null; has = false; playing = false;
                return;
            }
            if (lastSrc == null || lastSrc != source) {
                try { Files.writeString(dir.resolve("src.txt"), String.valueOf(source)); } catch (IOException ignored) { }
                lastSrc = source;
            }
        }
        if (serial != loadedSerial) loadCover();
    }

    private static void start() {
        try {
            dir = Files.createTempDirectory("burmalda-media");
            Path script = dir.resolve("media.ps1");
            try (InputStream in = MediaMonitor.class.getResourceAsStream("/assets/" + Brand.ID + "/media.ps1")) {
                if (in == null) { failed = true; return; }
                Files.copy(in, script);
            }
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                    dir.toString(), String.valueOf(ProcessHandle.current().pid()));
            proc = pb.start();
            startedAt = System.currentTimeMillis();
            lastSrc = null;
            final Process p = proc;
            Thread t = new Thread(() -> read(p), "burmalda-media");
            t.setDaemon(true);
            t.start();
            Thread e = new Thread(() -> {
                try (InputStream es = p.getErrorStream()) { es.transferTo(OutputStream.nullOutputStream()); }
                catch (IOException ignored) { }
            }, "burmalda-media-err");
            e.setDaemon(true);
            e.start();
            if (!hook) { Runtime.getRuntime().addShutdownHook(new Thread(MediaMonitor::stop)); hook = true; }
        } catch (Exception ex) {
            failed = true;
            proc = null;
        }
    }

    public static void stop() {
        Process p = proc;
        proc = null;
        has = false; playing = false;
        if (p != null) p.destroyForcibly();
    }

    private static void read(Process p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') line = line.substring(1);
                if (line.startsWith("S\t")) {
                    String[] a = line.split("\t", -1);
                    if (a.length < 8) continue;
                    long now = System.currentTimeMillis();
                    boolean pl = a[1].equals("1");
                    if (!a[2].equals(title) || !a[3].equals(artist)) changed = now;
                    title = a[2]; artist = a[3];
                    playing = pl;
                    pos = num(a[4]); dur = num(a[5]); stamp = now;
                    if (pl) lastPlayingAt = now;
                    coverPath = a[7];
                    serial = (int) num(a[6]);
                    has = true;
                } else if (line.startsWith("N")) {
                    has = false; playing = false;
                }
            }
        } catch (IOException ignored) { }
    }

    private static long num(String s) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static void loadCover() {
        loadedSerial = serial;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (coverId != null) { mc.getTextureManager().destroyTexture(coverId); coverId = null; }
        String path = coverPath;
        if (path == null || path.isEmpty()) return;
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            NativeImage img = NativeImage.read(in);
            coverW = img.getWidth();
            coverH = img.getHeight();
            coverId = mc.getTextureManager().registerDynamicTexture("burmalda_cover", new NativeImageBackedTexture(img));
        } catch (Exception e) {
            coverId = null;
        }
    }

    private MediaMonitor() { }
}
