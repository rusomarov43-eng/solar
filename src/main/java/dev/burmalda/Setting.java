package dev.burmalda;

import java.util.Locale;

public class Setting {
    public enum Type { BOOL, SLIDER, MODE }

    public final String name;
    public final Type type;
    public boolean on;
    public float num, min, max;
    public String[] modes = new String[0];
    public int mode;
    public float anim;

    private Setting(String name, Type type) { this.name = name; this.type = type; }

    public static Setting bool(String n, boolean v) {
        Setting s = new Setting(n, Type.BOOL); s.on = v; return s;
    }
    public static Setting slider(String n, float v, float min, float max) {
        Setting s = new Setting(n, Type.SLIDER); s.num = v; s.min = min; s.max = max; return s;
    }
    public static Setting mode(String n, int idx, String... modes) {
        Setting s = new Setting(n, Type.MODE); s.mode = idx; s.modes = modes; return s;
    }

    public String save() {
        return switch (type) {
            case BOOL -> String.valueOf(on);
            case SLIDER -> String.valueOf(num);
            case MODE -> String.valueOf(mode);
        };
    }

    public void load(String v) {
        try {
            switch (type) {
                case BOOL -> on = Boolean.parseBoolean(v);
                case SLIDER -> num = Math.max(min, Math.min(max, Float.parseFloat(v)));
                case MODE -> mode = Math.floorMod(Integer.parseInt(v), modes.length);
            }
        } catch (Exception ignored) { }
    }

    public String display() {
        return String.format(Locale.ROOT, (max - min) >= 2f ? "%.1f" : "%.2f", num);
    }
}
