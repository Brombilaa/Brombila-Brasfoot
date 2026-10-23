package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

// Fundo dos paineis escuros no estilo "Noite de estreia":
//  - degrade vertical + brilho de refletores nos cantos de cima + granulado,
//    tudo nas coordenadas da janela (paineis aninhados nao mostram emenda);
//  - painel com cor diferente do painel de fora vira "cartao de vidro":
//    mais claro, com contorno que brilha em cima.
public class PainelUI extends BasicPanelUI {
    private static final PainelUI UNICO = new PainelUI();
    private static TexturePaint granulado;

    public static ComponentUI createUI(JComponent c) {
        return UNICO;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            Color bg = c.getBackground();
            if (bg != null && escuro(bg)) pintaFundo((Graphics2D) g, c, bg);
            else {
                g.setColor(bg);
                g.fillRect(0, 0, c.getWidth(), c.getHeight());
            }
        }
        paint(g, c);
    }

    static boolean escuro(Color c) {
        return Tema.luminancia(c) < 0.35f && c.getAlpha() == 255;
    }

    // Cor do primeiro pai opaco (o que aparece "atras" deste painel).
    static Color corDeFora(Component c) {
        for (Container p = c.getParent(); p != null; p = p.getParent()) {
            if (p.isOpaque() || !(p instanceof JComponent)) return p.getBackground();
        }
        return null;
    }

    static void pintaFundo(Graphics2D g, JComponent c, Color bg) {
        Component raiz = SwingUtilities.getRoot(c);
        Point o = (raiz != null && raiz != c) ? SwingUtilities.convertPoint(c, 0, 0, raiz) : new Point(0, 0);
        int largRaiz = (raiz != null) ? Math.max(raiz.getWidth(), c.getWidth()) : c.getWidth();
        int altRaiz = (raiz != null) ? Math.max(raiz.getHeight(), c.getHeight()) : c.getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle clip = g2.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, c.getWidth(), c.getHeight());
            // degrade da janela
            g2.setPaint(new GradientPaint(0, -o.y, Tema.mistura(bg, Color.WHITE, 0.05f), 0, altRaiz - o.y,
                Tema.mistura(bg, Color.BLACK, 0.35f)));
            g2.fill(clip);
            // refletores
            float r = Math.max(largRaiz, altRaiz) * 0.45f;
            refletor(g2, clip, 0.10f * largRaiz - o.x, -0.05f * altRaiz - o.y, r, 46);
            refletor(g2, clip, 0.92f * largRaiz - o.x, -0.07f * altRaiz - o.y, r * 1.05f, 40);
            refletor(g2, clip, 0.5f * largRaiz - o.x, 1.12f * altRaiz - o.y, r * 1.1f, 30);
            // granulado
            TexturePaint t = granulado();
            java.awt.geom.Rectangle2D a = t.getAnchorRect();
            g2.setPaint(new TexturePaint(t.getImage(), new Rectangle(-o.x, -o.y, (int) a.getWidth(), (int) a.getHeight())));
            g2.fill(clip);
            // cartao de vidro
            Color fora = corDeFora(c);
            if (fora != null && !Tema.igual(fora, bg) && c.getWidth() > 40 && c.getHeight() > 24) {
                int w = c.getWidth(), h = c.getHeight();
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 16), 0, h, new Color(255, 255, 255, 4)));
                g2.fillRect(0, 0, w, h);
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 60), 0, Math.min(h, 140), new Color(255, 255, 255, 10)));
                g2.drawRect(0, 0, w - 1, h - 1);
                g2.setPaint(new GradientPaint(0, 0, Tema.alfa(Tema.DOURADO, 0), w / 2f, 0, Tema.alfa(Tema.DOURADO, 110), true));
                g2.fillRect(0, 0, w, 1);
            }
        } finally {
            g2.dispose();
        }
    }

    private static void refletor(Graphics2D g2, Rectangle clip, float cx, float cy, float raio, int alfa) {
        Rectangle area = new Rectangle(Math.round(cx - raio), Math.round(cy - raio), Math.round(raio * 2), Math.round(raio * 2));
        Rectangle i = area.intersection(clip);
        if (i.isEmpty()) return;
        g2.setPaint(new RadialGradientPaint(cx, cy, raio, new float[] {0f, 0.5f, 1f},
            new Color[] {Tema.alfa(new Color(0xE0A93A), alfa), Tema.alfa(new Color(0xE0A93A), alfa / 4), Tema.alfa(new Color(0xE0A93A), 0)}));
        g2.fill(i);
    }

    // Ladrilho 128x128 semitransparente gerado uma vez.
    static synchronized TexturePaint granulado() {
        if (granulado == null) {
            int n = 128;
            BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(22_23);
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    int a = r.nextInt(9);
                    img.setRGB(x, y, (a << 24) | (r.nextBoolean() ? 0xFFFFFF : 0x000000));
                }
            }
            granulado = new TexturePaint(img, new Rectangle(0, 0, n, n));
        }
        return granulado;
    }
}
