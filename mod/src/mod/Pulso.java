package mod;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicLabelUI;

// Balao de mensagens: quando o numero sobe (chegou mensagem), o balao pulsa uma vez,
// cresce um pouco e solta um anel dourado. O numero anterior fica guardado por chave
// porque o jogo cria a tela principal de novo a cada rodada.
final class Pulso extends BasicLabelUI {
    private static final Map<String, Integer> ultimo = new HashMap<String, Integer>();
    private static final int MS = 750;

    private long inicio;

    static void liga(Object o, final String chave) {
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        if (l.getUI() instanceof Pulso) return;
        final Pulso p = new Pulso();
        l.setUI(p);
        l.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) { p.confere(l, chave); }
        });
        p.confere(l, chave);
    }

    private void confere(JLabel l, String chave) {
        int n = numero(l.getText());
        Integer antes = ultimo.put(chave, n);
        if (antes != null && n > antes) Sons.toca("mensagem");
        if (antes != null && n > antes && Animacao.ligada()) {
            inicio = System.currentTimeMillis();
            Animacao.repinta(l);
        }
    }

    private static int numero(String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        // espera a tela aparecer para comecar a contar (a tela nova entra com fade)
        if (inicio > 0 && !c.isShowing()) inicio = System.currentTimeMillis();
        float t = inicio == 0 ? 1f : (System.currentTimeMillis() - inicio) / (float) MS;
        if (t >= 1f) {
            inicio = 0;
            super.paint(g0, c);
            return;
        }
        Animacao.repinta(c);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float cx = c.getWidth() / 2f, cy = c.getHeight() / 2f;
        // anel saindo do balao
        float k = Animacao.suave(t), rx = 6 + 14 * k, ry = Math.min(cy - 1, 5 + 8 * k);
        g.setColor(new Color(242, 193, 78, Math.round(170 * (1f - t))));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new Ellipse2D.Float(cx - rx, cy - ry, 2 * rx, 2 * ry));
        // o balao cresce e volta (dois tempos: sobe rapido, desce suave)
        double z = 1 + 0.22 * Math.sin(Math.PI * Math.min(1f, t * 1.6f));
        g.translate(cx, cy);
        g.scale(z, z);
        g.translate(-cx, -cy);
        super.paint(g, c);
        g.dispose();
    }
}
