package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.awt.font.TextAttribute;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicOptionPaneUI;

// Caixas de pergunta e aviso do jogo (JOptionPane) no estilo "Noite de estreia":
// janela sem a barra do Windows, cartao escuro com cantos arredondados, sombra e filete dourado,
// o titulo da janela vira um rotulo dourado em caixa alta, icone grande com halo,
// texto em Manrope e botoes-pilula grandes (Sim dourado, Nao com contorno).
public class Pergunta extends BasicOptionPaneUI {
    static final int SOMBRA = 18;
    private static final int RAIO = 20;

    public static ComponentUI createUI(JComponent c) { return new Pergunta(); }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        optionPane.setOpaque(false);
        optionPane.setBorder(new EmptyBorder(SOMBRA + 50, SOMBRA + 30, SOMBRA + 24, SOMBRA + 34));
    }

    @Override
    protected void installComponents() {
        super.installComponents();
        estiliza(optionPane, 0);
    }

    // Repassa depois que o jogo troca mensagem/botoes.
    @Override
    protected java.beans.PropertyChangeListener createPropertyChangeListener() {
        final java.beans.PropertyChangeListener orig = super.createPropertyChangeListener();
        return new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                orig.propertyChange(e);
                if (optionPane != null) estiliza(optionPane, 0);
            }
        };
    }

    private static int estiliza(Container c, int botoes) {
        for (Component k : c.getComponents()) {
            if (k instanceof JPanel) {
                ((JPanel) k).setOpaque(false);
                if (k.getName() != null && k.getName().equals("OptionPane.buttonArea")) ((JPanel) k).setBorder(new EmptyBorder(26, 0, 0, 0));
            }
            if (k instanceof JButton) {
                JButton b = (JButton) k;
                b.setFont(Estreia.negrito(14f));
                b.setMargin(new Insets(10, 26, 10, 26));
                b.setBorder(new EmptyBorder(10, 26, 10, 26));
                b.setMnemonic(0);
                b.setFocusPainted(false);
                b.setDisplayedMnemonicIndex(-1);
                java.awt.Dimension pd = b.getPreferredSize();
                if (pd.width < 120) { b.setPreferredSize(new java.awt.Dimension(120, pd.height)); }
                b.putClientProperty(BotaoUI.PRIMARIO, botoes == 0 ? Boolean.TRUE : Boolean.FALSE);
                botoes++;
            } else if (k instanceof JLabel && ((JLabel) k).getIcon() == null) {
                k.setFont(Estreia.semi(16f));
                k.setForeground(Estreia.BRANCO);
            }
            if (k instanceof Container) botoes = estiliza((Container) k, botoes);
        }
        return botoes;
    }

    @Override
    public void paint(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        int w = c.getWidth(), h = c.getHeight();
        boolean transparente = transparente(c);
        // sem transparencia (sistema sem suporte): cartao ocupa a janela toda
        float x = transparente ? SOMBRA : 0, y = transparente ? SOMBRA : 0, cw = w - 2 * x, ch = h - 2 * y;
        float ox = SOMBRA - x, oy = SOMBRA - y; // o texto continua no mesmo lugar
        if (transparente) {
            // sombra suave em volta do cartao
            for (int i = SOMBRA; i > 0; i -= 2) {
                float a = (1f - i / (float) SOMBRA);
                g.setColor(new Color(0, 0, 0, Math.round(70 * a * a)));
                g.fill(new RoundRectangle2D.Float(x - i, y - i + 4, cw + 2 * i, ch + 2 * i, RAIO + i, RAIO + i));
            }
        }
        float raio = transparente ? RAIO : 0;
        RoundRectangle2D cartao = new RoundRectangle2D.Float(x, y, cw, ch, raio, raio);
        g.setPaint(new GradientPaint(0, y, new Color(0x1A1E1B), 0, y + ch, new Color(0x0E110F)));
        g.fill(cartao);
        // luz de refletor vinda do canto de cima
        g.setClip(cartao);
        g.setPaint(new RadialGradientPaint(x + cw * 0.18f, y - 10, Math.max(cw, ch) * 0.75f, new float[] {0f, 1f},
            new Color[] {Tema.alfa(Estreia.OURO, 30), Tema.alfa(Estreia.OURO, 0)}));
        g.fill(cartao);
        // filete dourado no topo
        g.setPaint(new GradientPaint(x, 0, Tema.alfa(Estreia.OURO, 0), x + cw / 2, 0, Tema.alfa(Estreia.OURO, 210), true));
        g.fillRect((int) x, (int) y, (int) cw, 2);
        g.setClip(null);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(244, 241, 234, 30));
        g.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, ch - 1, raio, raio));
        // titulo da janela como rotulo
        String titulo = tituloDe(c);
        if (titulo != null && !titulo.trim().isEmpty()) {
            Map<TextAttribute, Object> at = new HashMap<TextAttribute, Object>();
            at.put(TextAttribute.TRACKING, 0.2f);
            g.setFont(Estreia.negrito(11f).deriveFont(at));
            g.setColor(Estreia.OURO);
            g.drawString(titulo.trim().toUpperCase(new java.util.Locale("pt", "BR")), x + ox + 30, y + oy + 32);
        }
        g.dispose();
    }

    private static String tituloDe(Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        if (w instanceof JDialog) return ((JDialog) w).getTitle();
        if (w instanceof java.awt.Frame) return ((java.awt.Frame) w).getTitle();
        return null;
    }

    static boolean forcaTransparente; // so para gerar previas fora do Windows

    private static boolean transparente(Component c) {
        if (forcaTransparente) return true;
        Window w = SwingUtilities.getWindowAncestor(c);
        return w != null && w.getBackground() != null && w.getBackground().getAlpha() == 0;
    }

    // Chamado quando o JOptionPane entra na janela, antes de ela aparecer:
    // tira a barra do Windows e deixa o fundo transparente (cantos redondos + sombra).
    static void prepara(JOptionPane p) {
        Window w = SwingUtilities.getWindowAncestor(p);
        if (!(w instanceof JDialog) || w.isDisplayable()) return;
        JDialog d = (JDialog) w;
        try {
            d.setUndecorated(true);
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                d.setBackground(new Color(0, 0, 0, 0));
                d.getRootPane().setOpaque(false);
                ((JComponent) d.getContentPane()).setOpaque(false);
            } else {
                d.setBackground(Estreia.NOITE);
            }
        } catch (Throwable t) {
            // se o sistema nao deixar, fica a janela normal
        }
    }

    // Icone grande com halo: "?", "i", "!" ou "×".
    static class Sinal implements Icon {
        final String sinal;
        final Color cor;
        Sinal(String sinal, Color cor) { this.sinal = sinal; this.cor = cor; }
        public int getIconWidth() { return 56; }
        public int getIconHeight() { return 56; }
        // 0 a 1 durante o pulso (comeca 0,25 s depois de aparecer e dura 0,8 s); -1 fora dele
        private static float pulso(Component c) {
            if (!(c instanceof javax.swing.JComponent) || !Animacao.ligada()) return -1f;
            javax.swing.JComponent j = (javax.swing.JComponent) c;
            Object o = j.getClientProperty("brombila.pulso");
            long agora = System.currentTimeMillis();
            if (!(o instanceof Long)) {
                j.putClientProperty("brombila.pulso", agora);
                o = agora;
            }
            float t = (agora - (Long) o - 320) / 800f;
            if (t < 0f) {
                // durante o zoom de entrada nao repinta (o veu da entrada esta por cima)
                if (j.getClientProperty("brombila.pulsoEspera") == null) {
                    j.putClientProperty("brombila.pulsoEspera", Boolean.TRUE);
                    final Component alvo = c;
                    javax.swing.Timer espera = new javax.swing.Timer((int) (-t * 800) + 10, new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent e) { alvo.repaint(); }
                    });
                    espera.setRepeats(false);
                    espera.start();
                }
                return 0f;
            }
            if (t < 1f) Animacao.repinta(c);
            return t;
        }

        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            float cx = x + 28, cy = y + 28;
            // depois do zoom de entrada, o halo pulsa uma vez: um anel sai do circulo e some
            float p = pulso(c);
            if (p > 0f && p < 1f) {
                float r = 19 + 9 * Animacao.suave(p);
                g.setColor(Tema.alfa(cor, Math.round(150 * (1f - p))));
                g.setStroke(new BasicStroke(2.5f * (1f - p) + 0.5f));
                g.draw(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
            }
            g.setPaint(new RadialGradientPaint(cx, cy, 28, new float[] {0.55f, 1f},
                new Color[] {Tema.alfa(cor, 60), Tema.alfa(cor, 0)}));
            g.fill(new Ellipse2D.Float(x, y, 56, 56));
            g.setColor(Tema.alfa(cor, 40));
            g.fill(new Ellipse2D.Float(cx - 19, cy - 19, 38, 38));
            g.setColor(cor);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Float(cx - 19, cy - 19, 38, 38));
            g.setFont(Estreia.negrito(21f));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(sinal, cx - fm.stringWidth(sinal) / 2f, cy - fm.getHeight() / 2f + fm.getAscent());
            g.dispose();
        }
    }
}
