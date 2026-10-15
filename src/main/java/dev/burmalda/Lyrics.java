package dev.burmalda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Time-synced lyrics for the current track from the community LRCLIB service (https://lrclib.net). */
public final class Lyrics {
    public record Line(long ms, String text) { }

    private static volatile List<Line> lines = List.of();
    private static volatile String status = "";
    private static String forKey = "";
    private static int gen;
    private static final Pattern TAG = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");

    public static List<Line> lines() { return lines; }
    public static String status() { return status; }

    /** Call from the client tick. Starts a lookup whenever the track changes. */
    public static void update(boolean wanted) {
        if (!wanted || !MediaMonitor.hasTrack()) {
            if (!forKey.isEmpty()) { forKey = ""; lines = List.of(); status = ""; gen++; }
            return;
        }
        String key = MediaMonitor.artist() + "|" + MediaMonitor.title();
        if (key.equals(forKey)) return;
        forKey = key;
        lines = List.of();
        status = "Searching lyrics...";
        final int g = ++gen;
        final String ar = MediaMonitor.artist(), ti = MediaMonitor.title();
        final long dur = MediaMonitor.durMs();
        Thread t = new Thread(() -> fetch(g, ar, ti, dur), "nebula-lyrics");
        t.setDaemon(true);
        t.start();
    }

    /** Index of the line that is active at posMs, or -1 before the first line. */
    public static int index(long posMs) {
        List<Line> l = lines;
        int lo = 0, hi = l.size() - 1, ans = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (l.get(mid).ms() <= posMs) { ans = mid; lo = mid + 1; } else hi = mid - 1;
        }
        return ans;
    }

    // ------------------------------------------------------------------ lookup
    private static void fetch(int g, String artist, String title, long durMs) {
        List<String[]> cands = new ArrayList<>();
        String cleanTitle = clean(title), cleanArtist = cleanArtist(artist);
        int dash = cleanTitle.indexOf(" - ");
        if (dash > 0) {
            String left = cleanTitle.substring(0, dash).trim(), right = clean(cleanTitle.substring(dash + 3));
            cands.add(new String[]{left, right});
            cands.add(new String[]{cleanArtist, right});
        }
        cands.add(new String[]{cleanArtist, cleanTitle});

        List<Line> found = null;
        for (String[] c : cands) {
            found = byGet(c[0], c[1], durMs);
            if (found == null) found = bySearch("track_name=" + enc(c[1]) + "&artist_name=" + enc(c[0]));
            if (found != null) break;
        }
        if (found == null) found = bySearch("q=" + enc(cleanArtist + " " + cleanTitle));
        if (g != gen) return; // the track changed meanwhile
        if (found == null || found.isEmpty()) { lines = List.of(); status = "No synced lyrics found"; }
        else { lines = found; status = ""; }
    }

    private static List<Line> byGet(String artist, String track, long durMs) {
        String url = "https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(track)
                + (durMs > 0 ? "&duration=" + (durMs / 1000) : "");
        String body = http(url);
        if (body == null) return null;
        try { return fromObject(JsonParser.parseString(body).getAsJsonObject()); } catch (Exception e) { return null; }
    }

    private static List<Line> bySearch(String query) {
        String body = http("https://lrclib.net/api/search?" + query);
        if (body == null) return null;
        try {
            JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
            for (JsonElement el : arr) {
                List<Line> l = fromObject(el.getAsJsonObject());
                if (l != null && !l.isEmpty()) return l;
            }
        } catch (Exception ignored) { }
        return null;
    }

    private static List<Line> fromObject(JsonObject o) {
        if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return null;
        return parseLrc(o.get("syncedLyrics").getAsString());
    }

    static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher m = TAG.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find()) {
                long min = Long.parseLong(m.group(1)), sec = Long.parseLong(m.group(2));
                String f = m.group(3);
                long ms = 0;
                if (f != null) ms = Long.parseLong((f + "00").substring(0, 3));
                times.add(min * 60000 + sec * 1000 + ms);
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (long t : times) out.add(new Line(t, text));
        }
        out.sort((a, b) -> Long.compare(a.ms(), b.ms()));
        return out;
    }

    private static String http(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestProperty("User-Agent", "Nebula-Visuals/" + Brand.VERSION + " (Minecraft client mod)");
            c.setConnectTimeout(6000);
            c.setReadTimeout(9000);
            if (c.getResponseCode() != 200) return null;
            try (InputStream in = c.getInputStream()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        } catch (Exception e) {
            return null;
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static String clean(String s) {
        s = s.replaceAll("(?i)\\s*[(\\[][^)\\]]*(official|video|lyric|audio|clip|remaster|visuali[sz]er|hd|4k|m/v)[^)\\]]*[)\\]]", "");
        s = s.replaceAll("(?i)\\s*(ft\\.|feat\\.|featuring)\\s.*$", "");
        return s.trim();
    }

    private static String cleanArtist(String a) {
        a = a.replaceAll("(?i)\\s*-\\s*topic$", "");
        a = a.replaceAll("(?i)vevo$", "");
        return a.trim();
    }

    private Lyrics() { }
}
