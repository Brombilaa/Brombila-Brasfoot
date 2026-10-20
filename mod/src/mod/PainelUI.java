package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

// Paineis de fundo escuro ganham um degrade vertical suave e uma textura leve
// (trama diagonal + granulado), em vez da cor chapada. O degrade e a textura
// usam as coordenadas da janela, entao paineis dentro de paineis da mesma cor
// continuam sem emenda.
public class PainelUI extends BasicPanelUI {
    private static final PainelUI UNICO = new PainelUI();
    private static TexturePaint textura;

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
        return Tema.luminancia(c) < 0.42f && c.getAlpha() == 255;
    }

    static void pintaFundo(Graphics2D g, JComponent c, Color bg) {
        Component raiz = SwingUtilities.getRoot(c);
        Point o = (raiz != null && raiz != c) ? SwingUtilities.convertPoint(c, 0, 0, raiz) : new Point(0, 0);
        int alturaRaiz = (raiz != null) ? Math.max(raiz.getHeight(), c.getHeight()) : c.getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Rectangle clip = g2.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, c.getWidth(), c.getHeight());
            Color topo = Tema.mistura(bg, Color.WHITE, 0.07f);
            Color base = Tema.mistura(bg, Color.BLACK, 0.22f);
            g2.setPaint(new GradientPaint(0, -o.y, topo, 0, alturaRaiz - o.y, base));
            g2.fill(clip);
            TexturePaint t = textura();
            java.awt.geom.Rectangle2D a = t.getAnchorRect();
            g2.setPaint(new TexturePaint(t.getImage(), new Rectangle(-o.x, -o.y, (int) a.getWidth(), (int) a.getHeight())));
            g2.fill(clip);
        } finally {
            g2.dispose();
        }
    }

    // Ladrilho 96x96 semitransparente gerado uma vez.
    static synchronized TexturePaint textura() {
        if (textura == null) {
            int n = 96;
            BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
            Random r = new Random(22_23);
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    int a = 0;
                    // trama diagonal fina, a cada 6 px
                    if (((x + y) % 6) == 0) a += 9;
                    // granulado
                    int ruido = r.nextInt(9);
                    int rgb;
                    if (r.nextBoolean()) { rgb = 0xFFFFFF; a += ruido; } else { rgb = 0x000000; a += ruido + 4; }
                    img.setRGB(x, y, (Math.min(255, a) << 24) | rgb);
                }
            }
            textura = new TexturePaint(img, new Rectangle(0, 0, n, n));
        }
        return textura;
    }
}
