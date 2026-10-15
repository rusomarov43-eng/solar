package dev.burmalda;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    private static Path path(String name) { return FabricLoader.getInstance().getConfigDir().resolve(name); }

    public static String profile(int n) { return Brand.ID + "_profile" + n + ".properties"; }

    public static void load() { load(Brand.ID + ".properties"); }
    public static void save() { save(Brand.ID + ".properties"); }

    public static void load(String file) {
        Path f = path(file);
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (var in = Files.newInputStream(f)) { p.load(in); } catch (IOException e) { return; }
        for (Feature m : Modules.ALL) {
            String en = p.getProperty(m.name + ".enabled");
            if (en != null) m.enabled = Boolean.parseBoolean(en);
            String bd = p.getProperty(m.name + ".bind");
            if (bd != null) { try { m.bind = Integer.parseInt(bd); } catch (NumberFormatException ignored) { } }
            for (Setting s : m.settings) {
                String v = p.getProperty(m.name + "." + s.name);
                if (v != null) s.load(v);
            }
        }
    }

    public static void save(String file) {
        Properties p = new Properties();
        for (Feature m : Modules.ALL) {
            p.setProperty(m.name + ".enabled", String.valueOf(m.enabled));
            p.setProperty(m.name + ".bind", String.valueOf(m.bind));
            for (Setting s : m.settings) p.setProperty(m.name + "." + s.name, s.save());
        }
        try (var out = Files.newOutputStream(path(file))) { p.store(out, Brand.NAME); } catch (IOException ignored) { }
    }

    private Config() { }
}
