package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;

// Botao verde com cantos arredondados, degrade e hover/clique animados.
// Botoes so de icone (sem fundo) ganham um brilho suave ao passar o mouse.
public class BotaoUI extends BasicButtonUI {
    private static final int RAIO = 10;
    private static final Color TOPO = new Color(0x2FA562);
    private static final Color BASE = new Color(0x17693A);
    private static final Color DESLIGADO = new Color(0x8FA79A);

    private float hover;   // 0..1, animado
    private float aperto;  // 0..1, animado
    private Timer timer;
    private ChangeListener ouvinte;
    private AbstractButton botao;

    public static ComponentUI createUI(JComponent c) {
        return new BotaoUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        botao = (AbstractButton) c;
        botao.setRolloverEnabled(true);
        LookAndFeel.installProperty(botao, "opaque", Boolean.FALSE);
        ouvinte = new ChangeListener() {
            public void stateChanged(ChangeEvent e) { anima(); }
        };
        botao.getModel().addChangeListener(ouvinte);
        botao.addPropertyChangeListener("model", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                if (e.getOldValue() instanceof ButtonModel) ((ButtonModel) e.getOldValue()).removeChangeListener(ouvinte);
                if (e.getNewValue() instanceof ButtonModel) ((ButtonModel) e.getNewValue()).addChangeListener(ouvinte);
            }
        });
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (timer != null) timer.stop();
        if (botao != null && ouvinte != null) botao.getModel().removeChangeListener(ouvinte);
        botao = null;
        super.uninstallUI(c);
    }

    private void anima() {
        if (timer == null) {
            timer = new Timer(Animacao.QUADRO_MS, new ActionListener() {
                public void actionPerformed(ActionEvent e) { passo(); }
            });
        }
        if (!timer.isRunning()) timer.start();
    }

    private void passo() {
        if (botao == null) { timer.stop(); return; }
        ButtonModel m = botao.getModel();
        float alvoH = (m.isRollover() && botao.isEnabled()) ? 1f : 0f;
        float alvoA = (m.isArmed() && m.isPressed()) ? 1f : 0f;
        hover = aproxima(hover, alvoH, 0.11f);   // ~140 ms
        aperto = aproxima(aperto, alvoA, 0.25f); // ~60 ms
        botao.repaint();
        if (hover == alvoH && aperto == alvoA) timer.stop();
    }

    private static float aproxima(float v, float alvo, float passo) {
        if (v < alvo) return Math.min(alvo, v + passo);
        if (v > alvo) return Math.max(alvo, v - passo);
        return v;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            float hv = Animacao.suave(hover), ap = aperto;
            if (b.isContentAreaFilled()) {
                Color[] cores = cores(b);
                Color topo = cores[0], base = cores[1];
                if (b.isEnabled()) {
                    topo = Tema.mistura(topo, Color.WHITE, 0.16f * hv - 0.10f * ap);
                    base = Tema.mistura(base, Color.WHITE, 0.10f * hv);
                    if (ap > 0) base = Tema.mistura(base, Color.BLACK, 0.18f * ap);
                }
                // sombra
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(1, 2, w - 2, h - 2, RAIO, RAIO);
                // corpo
                int dy = Math.round(ap);
                g2.setPaint(new GradientPaint(0, 0, topo, 0, h, base));
                g2.fillRoundRect(0, dy, w - 1, h - 2, RAIO, RAIO);
                // reflexo na metade de cima
                g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 60), 0, h / 2f, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(1, dy + 1, w - 3, h / 2, RAIO, RAIO);
                // contorno, que brilha com o hover
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Tema.mistura(new Color(0, 0, 0, 70), new Color(0xB8F5CF), hv));
                g2.drawRoundRect(0, dy, w - 1, h - 2, RAIO, RAIO);
            } else if (hv > 0.01f && b.isEnabled()) {
                // botao so de icone: brilho suave atras
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f * hv));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, RAIO + 4, RAIO + 4);
            }
        } finally {
            g2.dispose();
        }
        paint(g, c);
    }

    // Cor do botao: verde padrao, ou a cor que o jogo escolheu (ja esverdeada).
    private static Color[] cores(AbstractButton b) {
        if (!b.isEnabled()) return new Color[] {DESLIGADO.brighter(), DESLIGADO};
        Color bg = b.getBackground();
        if (bg != null && !(bg instanceof UIResource)) {
            bg = Tema.fundo(bg);
            float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
            // So respeita cores fortes; cinzas/brancos do jogo viram o verde padrao.
            if (hsb[1] > 0.25f && hsb[2] > 0.15f) {
                return new Color[] {Tema.mistura(bg, Color.WHITE, 0.18f), Tema.mistura(bg, Color.BLACK, 0.18f)};
            }
        }
        return new Color[] {TOPO, BASE};
    }

    @Override
    protected void paintText(Graphics g, JComponent c, Rectangle r, String text) {
        AbstractButton b = (AbstractButton) c;
        if (!b.isContentAreaFilled()) {
            super.paintText(g, c, r, text);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color fg = b.getForeground();
        Color fundo = cores(b)[1];
        // Texto escuro em botao verde nao se le: troca por branco.
        if (fg == null || Math.abs(Tema.luminancia(fg) - Tema.luminancia(fundo)) < 0.45f) fg = Color.WHITE;
        if (!b.isEnabled()) fg = new Color(0xF0F4F1);
        int dy = Math.round(aperto);
        java.awt.FontMetrics fm = g2.getFontMetrics(b.getFont());
        int mn = b.getDisplayedMnemonicIndex();
        g2.setFont(b.getFont());
        g2.setColor(new Color(0, 0, 0, 70));
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g2, text, -1,
            r.x + getTextShiftOffset(), r.y + fm.getAscent() + getTextShiftOffset() + 1 + dy);
        g2.setColor(fg);
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g2, text, mn,
            r.x + getTextShiftOffset(), r.y + fm.getAscent() + getTextShiftOffset() + dy);
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        if (!b.isContentAreaFilled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 212, 92, 150));
        g2.drawRoundRect(2, 2, b.getWidth() - 5, b.getHeight() - 6, RAIO - 2, RAIO - 2);
        g2.dispose();
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // o "afundar" ja e desenhado em update()
    }
}
