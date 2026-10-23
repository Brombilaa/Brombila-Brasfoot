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

    private static BufferedImage branco;

    static BufferedImage branco() {
        if (branco == null) {
            try {
                branco = ImageIO.read(Logo.class.getResource("/mod/logo/logo-branco.png"));
            } catch (Throwable t) {
                branco = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        }
        return branco;
    }

    // Sombra suave (preta, desfocada) com margem r em volta.
    static BufferedImage sombra(BufferedImage img, int r) {
        int w = img.getWidth() + 2 * r, h = img.getHeight() + 2 * r;
        int[] a = new int[w * h];
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) a[(y + r) * w + x + r] = img.getRGB(x, y) >>> 24;
        int[] t = new int[w * h];
        for (int passo = 0; passo < 2; passo++) { // dois borroes de caixa ~ gaussiano
            for (int y = 0; y < h; y++) {
                int s = 0;
                for (int x = -r; x < w; x++) {
                    if (x + r < w) s += a[y * w + x + r];
                    if (x - r - 1 >= 0) s -= a[y * w + x - r - 1];
                    if (x >= 0) t[y * w + x] = s / (2 * r + 1);
                }
            }
            for (int x = 0; x < w; x++) {
                int s = 0;
                for (int y = -r; y < h; y++) {
                    if (y + r < h) s += t[(y + r) * w + x];
                    if (y - r - 1 >= 0) s -= t[(y - r - 1) * w + x];
                    if (y >= 0) a[y * w + x] = s / (2 * r + 1);
                }
            }
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < a.length; i++) out.setRGB(i % w, i / w, Math.min(255, a[i] * 3 / 2) << 24);
        return out;
    }

    // Faixa de luz dourada-clara que passa sobre o logo (so onde ha desenho); p de 0 a 1.
    // A imagem de saida e reaproveitada entre quadros (o menu chama isso a cada quadro do brilho).
    private static BufferedImage brilhoBuf;

    static BufferedImage brilho(BufferedImage img, float p) {
        int w = img.getWidth(), h = img.getHeight();
        if (brilhoBuf == null || brilhoBuf.getWidth() != w || brilhoBuf.getHeight() != h) {
            brilhoBuf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        BufferedImage out = brilhoBuf;
        java.awt.Graphics2D g = out.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.drawImage(img, 0, 0, null);
        g.setComposite(java.awt.AlphaComposite.SrcIn);
        float lb = 0.22f * w, x = -lb + (w + 2 * lb) * p, inc = 0.35f * h;
        g.setPaint(new java.awt.LinearGradientPaint(x - inc, 0, x + inc + 0.01f, h, new float[] {0f, 0.5f, 1f},
            new java.awt.Color[] {new java.awt.Color(255, 236, 190, 0), new java.awt.Color(255, 236, 190, 150), new java.awt.Color(255, 236, 190, 0)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return out;
    }
    private static List<Image> icones;

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
