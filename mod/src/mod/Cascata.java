package mod;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Entrada em cascata de uma tela inteira: o fundo aparece primeiro, depois cada bloco (titulo,
// cartoes, botoes) sobe 28 px clareando, um depois do outro, e a lista de dentro do cartao se
// desenrola de cima para baixo. Tudo desenhado de dois retratos da tela (fundo e tela pronta),
// numa camada por cima que sai sozinha no fim; os componentes de verdade nao se mexem.
final class Cascata {
    private Cascata() {}

    static final class Bloco {
        final Rectangle area;
        final int atraso;
        final Rectangle lista; // parte que se desenrola (pode ser null)

        Bloco(int x, int y, int w, int h, int atraso, Rectangle lista) {
            this.area = new Rectangle(x, y, w, h);
            this.atraso = atraso;
            this.lista = lista;
        }
    }

    private static final int SUBIDA = 28, ENTRA = 420, DESENROLA = 620, ESPERA_LISTA = 220;

    // Liga a cascata para quando a tela aparecer.
    static void quandoAparecer(final JPanel tela, final Bloco... blocos) {
        if (!Animacao.ligada()) return;
        if (tela.isShowing()) {
            comeca(tela, blocos);
            return;
        }
        tela.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !tela.isShowing()) return;
                tela.removeHierarchyListener(this);
                comeca(tela, blocos);
            }
        });
    }

    private static void comeca(final JPanel tela, final Bloco[] blocos) {
        JRootPane rp = SwingUtilities.getRootPane(tela);
        if (rp == null) return;
        final JLayeredPane camadas = rp.getLayeredPane();
        int fim = 0;
        for (Bloco b : blocos) fim = Math.max(fim, b.atraso + ENTRA + (b.lista != null ? ESPERA_LISTA + DESENROLA - ENTRA : 0));
        final int total = fim + 40;
        final long[] inicio = {0};
        final BufferedImage[] fotos = new BufferedImage[2];
        final JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                int w = tela.getWidth(), h = tela.getHeight();
                if (w <= 0 || h <= 0) return;
                if (fotos[0] == null) {
                    // retratos tirados no primeiro quadro, com o tema ja aplicado e tudo no lugar
                    fotos[0] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D f = fotos[0].createGraphics();
                    tela.getUI().update(f, tela);
                    f.dispose();
                    fotos[1] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    f = fotos[1].createGraphics();
                    tela.paint(f);
                    f.dispose();
                    inicio[0] = System.currentTimeMillis();
                }
                Point o = SwingUtilities.convertPoint(tela, 0, 0, this);
                long dt = System.currentTimeMillis() - inicio[0];
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.translate(o.x, o.y);
                g.clipRect(0, 0, w, h);
                g.drawImage(fotos[0], 0, 0, null);
                for (Bloco b : blocos) desenha(g, fotos[1], b, dt);
                g.dispose();
            }
        };
        c.addMouseListener(new MouseAdapter() {}); // segura os cliques enquanto entra
        c.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        camadas.add(c, Integer.valueOf(JLayeredPane.DRAG_LAYER + 2));
        c.repaint();
        final Timer timer = new Timer(Animacao.QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (inicio[0] != 0 && System.currentTimeMillis() - inicio[0] > total || !tela.isShowing()) {
                    timer.stop();
                    camadas.remove(c);
                    camadas.repaint();
                    return;
                }
                c.repaint();
            }
        });
        timer.start();
    }

    private static void desenha(Graphics2D g0, BufferedImage foto, Bloco b, long dt) {
        float t = Math.min(1f, Math.max(0f, (dt - b.atraso) / (float) ENTRA));
        if (t <= 0f) return;
        float u = 1f - t;
        float e = 1f - u * u * u;
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(0, Math.round(SUBIDA * (1f - e)));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, t * 1.4f)));
        Rectangle a = b.area;
        if (b.lista == null) {
            g.setClip(a);
            g.drawImage(foto, 0, 0, null);
            g.dispose();
            return;
        }
        // cartao sem a lista, depois a lista ate a linha que ja desenrolou, com a borda esfumada
        Rectangle l = b.lista;
        g.setClip(a);
        g.clipRect(a.x, a.y, a.width, l.y - a.y);
        g.drawImage(foto, 0, 0, null);
        g.setClip(a);
        g.clipRect(a.x, l.y + l.height, a.width, a.y + a.height - l.y - l.height);
        g.drawImage(foto, 0, 0, null);
        float k = Math.min(1f, Math.max(0f, (dt - b.atraso - ESPERA_LISTA) / (float) DESENROLA));
        float v = 1f - k;
        int ate = Math.round(l.height * (1f - v * v));
        if (ate > 0) {
            g.setClip(a);
            g.clipRect(l.x, l.y, l.width, ate);
            g.drawImage(foto, 0, 0, null);
        }
        int borda = 36;
        for (int i = 0; i < 6 && ate < l.height; i++) {
            int y0 = l.y + ate + i * borda / 6;
            g.setClip(a);
            g.clipRect(l.x, y0, l.width, borda / 6);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, t * 1.4f) * (1f - (i + 1) / 7f)));
            g.drawImage(foto, 0, 0, null);
        }
        g.dispose();
    }
}
