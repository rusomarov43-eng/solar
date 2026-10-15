package dev.burmalda;

import net.minecraft.client.texture.NativeImage;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** Only touched through try/catch(Throwable): uses ImageIO for formats NativeImage can't read. */
final class ImgFallback {
    static NativeImage read(Path p) throws Exception {
        BufferedImage bi = javax.imageio.ImageIO.read(p.toFile());
        if (bi == null) return null;
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage ni = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
                ni.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return ni;
    }
    private ImgFallback() { }
}
