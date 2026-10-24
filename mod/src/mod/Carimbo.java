package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Carimbo de negocio fechado: "CONTRATADO", "EMPRESTADO" ou "VENDIDO" cai de cima, grande e
// transparente, bate no papel (0,26 s) com um anel de poeira e fica torto por cima da janela.
public final class Carimbo {
    private Carimbo() {}

    static final Color VERMELHO = new Color(0xE0524A);
    static final String CHAVE = "brombila.carimbo";

    // Janela de aviso do jogo (JOptionPane) que acabou de abrir.
    public static void aviso(Window w) {
        if (!(w instanceof JDialog)) return;
        String t = ((JDialog) w).getTitle();
        if (t == null) return;
        String texto = null;
        Color cor = Tema.OURO;
        if (t.startsWith("Compra realizada")) texto = "CONTRATADO";
        else if (t.startsWith("Empréstimo realizado")) texto = "EMPRESTADO";
        if (texto == null) return;
        final JRootPane rp = ((JDialog) w).getRootPane();
        final String txt = texto;
        final Color c = cor;
        // espera a janela terminar de entrar
        Timer t0 = new Timer(Animacao.ligada() ? 220 : 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JLayeredPane camadas = rp.getLayeredPane();
                int w = camadas.getWidth(), h = camadas.getHeight();
                if (w <= 0) return;
                // no canto de cima, ao lado do titulo, sem cobrir a mensagem
                poe(camadas, txt, c, Math.round(w * 0.76f), Math.round(h * 0.25f), 0.62f);
            }
        });
        t0.setRepeats(false);
        t0.start();
    }

    // Carimbo sobre um componente (coordenadas do componente); devolve a camada para tirar depois.
    public static JComponent sobre(Component alvo, String texto, Color cor, int cx, int cy, float escala) {
        Window w = SwingUtilities.getWindowAncestor(alvo);
        if (!(w instanceof RootPaneContainer)) return null;
        JLayeredPane camadas = ((RootPaneContainer) w).getRootPane().getLayeredPane();
        Point p = SwingUtilities.convertPoint(alvo, cx, cy, camadas);
        final JComponent c = poe(camadas, texto, cor, p.x, p.y, escala);
        // a tela do carimbo saiu (ex.: leilao -> Continuar): o carimbo sai junto
        alvo.addHierarchyListener(new java.awt.event.HierarchyListener() {
            public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) == 0 || e.getChanged().isShowing()) return;
                if (!e.getComponent().isShowing()) {
                    e.getComponent().removeHierarchyListener(this);
                    tira(c);
                }
            }
        });
        return c;
    }

    public static void tira(JComponent c) {
        if (c == null || c.getParent() == null) return;
        java.awt.Container p = c.getParent();
        p.remove(c);
        p.repaint();
    }

    private static JComponent poe(final JLayeredPane camadas, final String texto, final Color cor, final int cx, final int cy,
            final float escala) {
        final long inicio = System.currentTimeMillis();
        final boolean anima = Animacao.ligada();
        final JComponent c = new JComponent() {
            @Override
            public boolean contains(int x, int y) {
                return false; // nao pega clique
            }

            @Override
            protected void paintComponent(Graphics g0) {
                long dt = System.currentTimeMillis() - inicio;
                float t = anima ? Math.min(1f, dt / 260f) : 1f;
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                // cai acelerando: escala 2,6 -> 1, transparente -> firme
                float e = t * t;
                float esc = escala * (2.6f - 1.6f * e);
                float alfa = 0.15f + 0.75f * e;
                // tremidinha no impacto
                float tremor = 0f;
                if (anima && dt > 260 && dt < 380) tremor = (float) Math.sin((dt - 260) / 120f * Math.PI * 3) * 2.5f * (1f - (dt - 260) / 120f);
                // anel de poeira saindo do carimbo
                if (anima && dt > 240 && dt < 640) {
                    float u = (dt - 240) / 400f;
                    float r = escala * (70 + 70 * (1f - (1f - u) * (1f - u)));
                    g.setColor(Tema.alfa(cor, Math.round(90 * (1f - u))));
                    g.setStroke(new BasicStroke(2.5f * (1f - u) + 0.5f));
                    g.draw(new Ellipse2D.Float(cx - r, cy - r * 0.55f, 2 * r, 2 * r * 0.55f));
                }
                g.translate(cx + tremor, cy);
                g.rotate(Math.toRadians(-11));
                g.scale(esc, esc);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
                Font f = Estreia.titulo(40f);
                g.setFont(f);
                // caixa pelo desenho real das letras (a Saira tem muito respiro em cima)
                java.awt.geom.Rectangle2D lb = f.createGlyphVector(g.getFontRenderContext(), texto).getVisualBounds();
                float tw = (float) lb.getWidth();
                float bw = tw + 34, bh = (float) lb.getHeight() + 22;
                RoundRectangle2D.Float fora = new RoundRectangle2D.Float(-bw / 2, -bh / 2, bw, bh, 12, 12);
                RoundRectangle2D.Float dentro = new RoundRectangle2D.Float(-bw / 2 + 5, -bh / 2 + 5, bw - 10, bh - 10, 8, 8);
                g.setColor(Tema.alfa(Estreia.NOITE, 120));
                g.fill(fora);
                g.setColor(cor);
                g.setStroke(new BasicStroke(3.2f));
                g.draw(fora);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(dentro);
                g.drawString(texto, (float) (-lb.getX() - tw / 2f), (float) (-lb.getY() - lb.getHeight() / 2f));
                g.dispose();
            }
        };
        c.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        camadas.add(c, JLayeredPane.DRAG_LAYER);
        if (anima) {
            final Timer timer = new Timer(Animacao.QUADRO_MS, null);
            timer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    c.repaint();
                    if (System.currentTimeMillis() - inicio > 700) timer.stop();
                }
            });
            timer.start();
        }
        c.repaint();
        return c;
    }
}
