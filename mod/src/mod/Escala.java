package mod;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

// Reducao de imagem com qualidade, chamada pelo best.ah remendado (tools/escudos/remenda_ah.py)
// no lugar de Graphics2D.drawImage(img, x, y, w, h, obs). Reduz pela metade em passos com
// interpolacao bicubica ate chegar no tamanho, como um programa de imagem faria.
public final class Escala {
    private Escala() {}

    public static boolean desenha(Graphics2D g, Image img, int x, int y, int w, int h, ImageObserver obs) {
        try {
            int iw = img.getWidth(null), ih = img.getHeight(null);
            if (iw > 0 && ih > 0 && w > 0 && h > 0 && (iw != w || ih != h)) {
                Image atual = img;
                int cw = iw, ch = ih;
                while (cw / 2 >= w && ch / 2 >= h) {
                    cw = Math.max(w, cw / 2);
                    ch = Math.max(h, ch / 2);
                    atual = passo(atual, cw, ch);
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                return g.drawImage(atual, x, y, w, h, obs);
            }
        } catch (Throwable t) {
            // cai no desenho original
        }
        return g.drawImage(img, x, y, w, h, obs);
    }

    private static Image passo(Image img, int w, int h) {
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = d.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return d;
    }
}
