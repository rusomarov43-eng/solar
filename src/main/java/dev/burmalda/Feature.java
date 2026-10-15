package dev.burmalda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One card in the menu (a "module"). */
public class Feature {
    public final String name, category;
    public boolean enabled, collapsed, bindDown;
    public int bind = -1;
    public float anim, hover, open = 1f;
    public final List<Setting> settings;

    public Feature(String name, String category, boolean enabled, Setting... s) {
        this.name = name; this.category = category; this.enabled = enabled;
        this.settings = new ArrayList<>(Arrays.asList(s));
    }

    public Setting get(String n) {
        for (Setting s : settings) if (s.name.equals(n)) return s;
        throw new IllegalArgumentException(n);
    }
    public boolean b(String n) { return get(n).on; }
    public float f(String n) { return get(n).num; }
    public int m(String n) { return get(n).mode; }

    public void toggle() {
        enabled = !enabled;
        Notifications.push(name + (enabled ? " enabled" : " disabled"));
    }

    public String info() {
        for (Setting s : settings) if (s.type == Setting.Type.MODE) return s.modes[s.mode];
        return "ON";
    }
}
