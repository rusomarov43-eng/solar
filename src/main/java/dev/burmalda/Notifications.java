package dev.burmalda;

/** Last notification, shown in the Dynamic Island. */
public final class Notifications {
    private static String text = "";
    private static long born;

    public static void push(String t) { text = t; born = System.currentTimeMillis(); }
    public static String text() { return text; }
    public static long born() { return born; }
    private Notifications() { }
}
