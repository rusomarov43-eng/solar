package dev.burmalda;

import java.util.ArrayList;
import java.util.List;

public final class Modules {
    public static final List<Feature> ALL = new ArrayList<>();
    public static final String[] CATEGORIES = {"Render", "Combat", "HUD", "Cosmetics", "Misc"};

    // ---- Render
    public static final Feature SWING = reg(new Feature("SwingAnimation", "Render", true,
            Setting.mode("Style", 1, "Vanilla", "Slash", "Stab", "Smooth", "Spin", "Whip"),
            Setting.slider("Intensity", 1f, 0.3f, 2f)));
    public static final Feature VIEWMODEL = reg(new Feature("ViewModel", "Render", false,
            Setting.slider("X", 0f, -0.6f, 0.6f),
            Setting.slider("Y", 0f, -0.6f, 0.6f),
            Setting.slider("Z", 0f, -0.6f, 0.6f),
            Setting.slider("Scale", 1f, 0.5f, 1.5f)));
    public static final Feature BLOCK_OVERLAY = reg(new Feature("BlockOverlay", "Render", true,
            Setting.mode("Shader", 0, "Holy", "Rainbow", "Static"),
            Setting.slider("Shader speed", 1f, 0.1f, 5f),
            Setting.slider("Shader opacity", 1f, 0.1f, 1f),
            Setting.bool("Client color", true),
            Setting.bool("Fill", true),
            Setting.slider("Fill opacity", 0.25f, 0.05f, 0.8f)));
    public static final Feature JUMP = reg(new Feature("JumpCircles", "Render", true,
            Setting.mode("Color", 0, "Client", "Rainbow"),
            Setting.slider("Size", 1f, 0.5f, 2.5f),
            Setting.slider("Time", 1f, 0.4f, 2.5f)));
    public static final Feature TRAIL = reg(new Feature("Trail", "Render", false,
            Setting.mode("Color", 0, "Client", "Rainbow"),
            Setting.slider("Length", 30f, 10f, 80f),
            Setting.slider("Width", 0.4f, 0.1f, 1f)));
    public static final Feature AURA = reg(new Feature("Aura", "Render", false,
            Setting.mode("Type", 0, "Stars", "Snow", "Hearts", "Magic", "Flame"),
            Setting.slider("Amount", 2f, 1f, 8f)));
    public static final Feature CROSSHAIR = reg(new Feature("Crosshair", "Render", true,
            Setting.mode("Style", 0, "Cross", "Dot", "Circle", "Plus"),
            Setting.slider("Size", 4f, 1f, 12f),
            Setting.slider("Gap", 3f, 0f, 10f),
            Setting.slider("Thickness", 1f, 1f, 3f),
            Setting.bool("Client color", true)));
    public static final Feature ASPECT = reg(new Feature("AspectRatio", "Render", false,
            Setting.slider("Ratio", 1.33f, 0.8f, 2.4f)));

    // ---- Combat (visual only)
    public static final Feature TARGET_ESP = reg(new Feature("TargetESP", "Combat", true,
            Setting.mode("Style", 0, "Ring", "Circle"),
            Setting.slider("Size", 1f, 0.5f, 2f),
            Setting.slider("Speed", 1f, 0.2f, 3f),
            Setting.bool("Client color", true)));
    public static final Feature TARGET_HUD = reg(new Feature("TargetHUD", "Combat", true,
            Setting.bool("Head", true), Setting.bool("Health text", true)));
    public static final Feature DAMAGE = reg(new Feature("DamageParticles", "Combat", true,
            Setting.slider("Size", 1f, 0.5f, 2f),
            Setting.slider("Time", 1f, 0.5f, 3f),
            Setting.bool("Client color", false)));
    public static final Feature HIT_PARTICLES = reg(new Feature("HitParticles", "Combat", true,
            Setting.mode("Type", 0, "Crit", "Magic", "Flame", "Heart", "Totem", "Soul"),
            Setting.slider("Amount", 8f, 1f, 30f)));
    public static final Feature HIT_WAVE = reg(new Feature("HitWave", "Combat", true,
            Setting.mode("Color", 0, "Client", "Rainbow"),
            Setting.slider("Size", 1f, 0.5f, 2.5f)));
    public static final Feature KILL = reg(new Feature("KillEffect", "Combat", true,
            Setting.mode("Effect", 0, "Totem", "Soul", "Hearts")));
    public static final Feature HIT_SOUND = reg(new Feature("HitSound", "Combat", false,
            Setting.mode("Sound", 0, "Orb", "Ding", "Anvil", "Crit"),
            Setting.slider("Volume", 0.6f, 0.1f, 1f)));
    public static final Feature HIT_INFO = reg(new Feature("HitInfo", "Combat", true,
            Setting.bool("Combo", true), Setting.bool("Reach", true)));

    // ---- HUD
    public static final Feature WATERMARK = reg(new Feature("Watermark", "HUD", true,
            Setting.bool("Username", true), Setting.bool("FPS", true), Setting.bool("Ping", true)));
    public static final Feature LYRICS = reg(new Feature("Lyrics", "HUD", true,
            Setting.mode("Position", 0, "Bottom", "Center", "Top"),
            Setting.slider("Offset ms", 0f, -2000f, 2000f),
            Setting.bool("Next line", true)));
    public static final Feature ISLAND = reg(new Feature("DynamicIsland", "HUD", true,
            Setting.slider("Duration", 2.5f, 1f, 6f),
            Setting.bool("Music", true),
            Setting.mode("Source", 0, "YouTube", "Spotify", "Any")));
    public static final Feature INFO = reg(new Feature("Info", "HUD", true,
            Setting.bool("CPU", true), Setting.bool("RAM", true), Setting.bool("FPS", true)));
    public static final Feature KEYSTROKES = reg(new Feature("Keystrokes", "HUD", true,
            Setting.mode("Side", 0, "Right", "Left"),
            Setting.bool("Mouse", true), Setting.bool("CPS", true), Setting.bool("Space", true)));
    public static final Feature ARMOR = reg(new Feature("ArmorHUD", "HUD", true));
    public static final Feature EFFECTS = reg(new Feature("Effects", "HUD", true));
    public static final Feature STATS = reg(new Feature("Stats", "HUD", true,
            Setting.bool("Clock", true), Setting.bool("Speed", true),
            Setting.bool("Playtime", true), Setting.bool("Items", true),
            Setting.bool("Biome", false), Setting.bool("Day", false), Setting.bool("Kills", false)));
    public static final Feature FPS_GRAPH = reg(new Feature("FpsGraph", "HUD", false));
    public static final Feature COMPASS = reg(new Feature("Compass", "HUD", false));
    public static final Feature ARRAYLIST = reg(new Feature("ArrayList", "HUD", true,
            Setting.bool("Background", true), Setting.bool("Bar", true)));
    public static final Feature KEYBINDS = reg(new Feature("Keybinds", "HUD", true));
    public static final Feature COORDS = reg(new Feature("Coordinates", "HUD", true,
            Setting.bool("Direction", true)));
    public static final Feature HOTBAR = reg(new Feature("Hotbar", "HUD", true,
            Setting.slider("Opacity", 0.9f, 0.3f, 1f)));
    public static final Feature THEME = reg(new Feature("Theme", "HUD", true,
            Setting.mode("Preset", 0, "Purple", "Blue", "Cyan", "Green", "Red", "Pink", "Orange", "Custom"),
            Setting.slider("Hue", 0.74f, 0f, 1f),
            Setting.bool("Rainbow", false),
            Setting.slider("Glass", 0.62f, 0.25f, 1f),
            Setting.mode("Font", 0, "Smooth", "Vanilla")));

    // ---- Cosmetics (only you see them)
    public static final Feature HAT = reg(new Feature("Hat", "Cosmetics", true,
            Setting.mode("Hat", 0, "Top hat", "Crown", "Halo", "Wizard", "Horns", "Cat ears", "Bunny", "Headphones", "Shades"),
            Setting.mode("Color", 0, "Client", "Rainbow", "Custom"),
            Setting.slider("Hue", 0.12f, 0f, 1f),
            Setting.slider("Scale", 1f, 0.7f, 1.5f)));
    public static final Feature CAPE = reg(new Feature("Cape", "Cosmetics", false,
            Setting.mode("Color", 0, "Client", "Rainbow", "Custom"),
            Setting.slider("Hue", 0.6f, 0f, 1f)));
    public static final Feature WINGS = reg(new Feature("Wings", "Cosmetics", false,
            Setting.mode("Color", 0, "Client", "Rainbow", "Custom"),
            Setting.slider("Hue", 0.55f, 0f, 1f),
            Setting.slider("Flap speed", 1f, 0.2f, 3f),
            Setting.slider("Scale", 1f, 0.6f, 1.6f)));
    public static final Feature CUSTOM_ITEMS = reg(new Feature("CustomItems", "Cosmetics", true,
            Setting.mode("Model", 0, "Knife", "Katana"),
            Setting.mode("Replace", 0, "All swords", "Iron sword", "Diamond sword", "Netherite sword", "Stick"),
            Setting.bool("Glow inlay", true)));

    // ---- Misc
    public static final Feature ZOOM = reg(new Feature("Zoom", "Misc", true,
            Setting.slider("Factor", 3f, 1.5f, 8f),
            Setting.bool("Smooth", true)));
    public static final Feature TIME = reg(new Feature("TimeChanger", "Misc", false,
            Setting.slider("Hour", 18f, 0f, 24f)));
    public static final Feature MAINMENU = reg(new Feature("MainMenu", "Misc", true,
            Setting.bool("Particles", true)));

    private static Feature reg(Feature f) { ALL.add(f); return f; }
    private Modules() { }
}
