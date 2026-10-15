package dev.burmalda;

/** Hold the zoom key (default C) to zoom in. */
public final class Zoom {
    private static float t;

    public static double modify(double fov) {
        Feature z = Modules.ZOOM;
        boolean on = z.enabled && BurmaldaClient.zoomKey != null && BurmaldaClient.zoomKey.isPressed();
        t += ((on ? 1f : 0f) - t) * (z.b("Smooth") ? 0.2f : 1f);
        if (t < 0.002f) { t = 0f; return fov; }
        return fov / (1.0 + (z.f("Factor") - 1.0) * t);
    }

    private Zoom() { }
}
