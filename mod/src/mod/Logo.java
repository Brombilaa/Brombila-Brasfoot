package mod;

import java.awt.Image;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// Logo do menu inicial e icone das janelas (artes do Leo, em mod/logo).
final class Logo {
    private Logo() {}

    private static BufferedImage logo;
    private static List<Image> icones;

    static BufferedImage logo() {
        if (logo == null) {
            try {
                logo = ImageIO.read(Logo.class.getResource("/mod/logo/logo.png"));
            } catch (Throwable t) {
                logo = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        }
        return logo;
    }

    static List<Image> icones() {
        if (icones == null) {
            icones = new ArrayList<Image>();
            for (int n : new int[] {16, 24, 32, 48, 64, 128, 256}) {
                try {
                    icones.add(ImageIO.read(Logo.class.getResource("/mod/logo/icone-" + n + ".png")));
                } catch (Throwable t) {
                    // sem esse tamanho
                }
            }
        }
        return icones;
    }

    // Reducao em passos de metade (bicubica), para o logo ficar nitido.
    static BufferedImage reduz(BufferedImage img, int w, int h) {
        BufferedImage atual = img;
        int cw = img.getWidth(), ch = img.getHeight();
        do {
            cw = Math.max(w, cw / 2);
            ch = Math.max(h, ch / 2);
            BufferedImage prox = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = prox.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(atual, 0, 0, cw, ch, null);
            g.dispose();
            atual = prox;
        } while (cw != w || ch != h);
        return atual;
    }

    // O jogo poe a bola antiga (aicons/ball.png) nas janelas; troca pelo icone novo.
    static void icone(Window w) {
        if (icones().isEmpty()) return;
        try {
            w.setIconImages(icones);
        } catch (Throwable t) {
            // fica o do jogo
        }
    }
}
