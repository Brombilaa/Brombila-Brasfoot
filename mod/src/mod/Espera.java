package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicPanelUI;

// Janela do proprio jogo "Carregando..." / "Salvando jogo..." (a.hz), que aparece ao abrir
// um jogo salvo ou um jogo novo: o jogo trabalha em outra thread, entao aqui da para animar
// de verdade. Cartao escuro com a bola do logo girando e uma barra com brilho correndo.
final class Espera extends BasicPanelUI {
    private static BufferedImage bola;
    private final JPanel hz;
    private final long inicio = System.currentTimeMillis();

    private Espera(JPanel hz) { this.hz = hz; }

    static void aplica(final JPanel hz) {
        if (hz.getUI() instanceof Espera) return;
        for (Component c : hz.getComponents()) c.setVisible(false);
        hz.setBorder(BorderFactory.createEmptyBorder());
        hz.setUI(new Espera(hz));
        final Timer t = new Timer(Animacao.QUADRO_MS, null);
        t.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!hz.isDisplayable()) {
                    t.stop();
                    return;
                }
                hz.repaint();
            }
        });
        t.start();
    }

    private String texto() {
        Object o = Reflexao.campo(hz, "ug");
        String s = o instanceof JLabel ? ((JLabel) o).getText() : null;
        if (s == null || s.isEmpty()) return "CARREGANDO";
        s = s.toLowerCase();
        return s.startsWith("salvando") ? "SALVANDO O JOGO" : "CARREGANDO";
    }

    private static synchronized BufferedImage bola() {
        if (bola == null) {
            try {
                bola = Logo.separa(Logo.branco())[1];
            } catch (Throwable t) {
                bola = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            }
        }
        return bola;
    }

    @Override
    public void update(Graphics g, JComponent c) { paint(g, c); }

    @Override
    public void paint(Graphics g0, JComponent c) {
        int w = c.getWidth(), h = c.getHeight();
        float t = (System.currentTimeMillis() - inicio) / 1000f;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Estreia.NOITE);
        g.fillRect(0, 0, w, h);
        g.setPaint(new java.awt.RadialGradientPaint(w * 0.72f, -h * 0.2f, w * 0.7f, new float[] {0f, 1f},
            new Color[] {Tema.alfa(Estreia.OURO, 46), Tema.alfa(Estreia.OURO, 0)}));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 30));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 4, 4));

        // bola do logo girando, com um halo que respira
        float tam = Math.min(64, h - 36), cx = 24 + tam / 2, cy = h / 2f;
        float halo = 0.5f + 0.5f * (float) Math.sin(t * 3.0);
        g.setPaint(new java.awt.RadialGradientPaint(cx, cy, tam * 0.85f, new float[] {0.45f, 1f},
            new Color[] {Tema.alfa(Estreia.OURO, Math.round(30 + 40 * halo)), Tema.alfa(Estreia.OURO, 0)}));
        g.fillRect(0, 0, w, h);
        Logo.bola(g, bola(), cx, cy, tam, t * 2.6);

        // texto e barra com brilho correndo
        float tx = cx + tam / 2 + 22, tw = w - tx - 26;
        g.setFont(Estreia.titulo(24f));
        FontMetrics m = g.getFontMetrics();
        String s = texto();
        int pontos = (int) (t * 2.5) % 4;
        g.setColor(Estreia.BRANCO);
        g.drawString(s + "...".substring(0, pontos), tx, cy - 4);
        float by = cy + 10;
        g.setColor(new Color(255, 255, 255, 26));
        g.fill(new RoundRectangle2D.Float(tx, by, tw, 4, 4, 4));
        float v = (t * 0.75f) % 1.3f - 0.3f, cl = tw * 0.36f, bx = tx + v * tw;
        java.awt.Shape antes = g.getClip();
        g.clip(new RoundRectangle2D.Float(tx, by, tw, 4, 4, 4));
        g.setPaint(new LinearGradientPaint(bx, 0, bx + cl, 0, new float[] {0f, 0.7f, 1f},
            new Color[] {Tema.alfa(Estreia.OURO, 0), Estreia.OURO, new Color(255, 240, 200)}));
        g.fill(new java.awt.geom.Rectangle2D.Float(bx, by, cl, 4));
        g.setClip(antes);
        g.setFont(Estreia.texto(11f));
        g.setColor(Estreia.FRACO);
        g.drawString("isso pode levar alguns segundos", tx, by + 20);
        g.dispose();
    }
}
