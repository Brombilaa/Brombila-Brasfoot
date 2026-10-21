package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

// Botoes do tema "Noite de estreia":
//  - primario: pilula dourada, texto escuro, sobe e brilha no hover;
//  - secundario (Voltar, Cancelar, Sair...): contorno claro, fundo translucido;
//  - desativado: vidro apagado;
//  - so de icone (barra de cima): halo de refletor e sublinhado dourado no hover.
// Hover e clique sao animados.
public class BotaoUI extends BasicButtonUI {
    private static final int RAIO = 12;
    private static final int MARGEM = 2; // espaco para o brilho do hover
    // Botao principal na tela "Noite de estreia" (dourado); os outros ficam so com contorno.
    static final String PRIMARIO = "brombila.primario";
    // Botao redondo (o X de fechar das janelas).
    static final String REDONDO = "brombila.redondo";

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
        hover = aproxima(hover, alvoH, 0.09f);   // ~170 ms
        aperto = aproxima(aperto, alvoA, 0.25f); // ~60 ms
        botao.repaint();
        if (hover == alvoH && aperto == alvoA) timer.stop();
    }

    private static float aproxima(float v, float alvo, float passo) {
        if (v < alvo) return Math.min(alvo, v + passo);
        if (v > alvo) return Math.max(alvo, v - passo);
        return v;
    }

    // Botoes de "voltar/cancelar" ficam so com contorno.
    static boolean secundario(AbstractButton b) {
        String t = b.getText();
        // botao so de icone: corpo de vidro, para o icone dourado nao sumir no fundo dourado
        if ((t == null || t.trim().isEmpty()) && b.getIcon() != null) return true;
        if (t == null) return false;
        t = t.trim().toLowerCase();
        return t.startsWith("<<") || t.startsWith("voltar") || t.startsWith("cancelar") || t.startsWith("sair")
            || t.startsWith("fechar") || t.equals("x") || t.equals("no") || t.equals("não") || t.equals("nao");
    }

    @Override
    public void update(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            float hv = Animacao.suave(hover);
            int dy = Math.round(aperto);
            if (b.isContentAreaFilled()) {
                estreia(g2, b, w, h, hv, dy);
            } else if (hv > 0.01f && b.isEnabled()) {
                // botao so de icone: halo de refletor + sublinhado dourado
                g2.setPaint(new RadialGradientPaint(w / 2f, h, Math.max(w, h) * 0.75f, new float[] {0f, 1f},
                    new Color[] {Tema.alfa(Tema.NEON, Math.round(70 * hv)), Tema.alfa(Tema.NEON, 0)}));
                g2.fillRect(0, 0, w, h);
                g2.setColor(Tema.alfa(Tema.NEON, Math.round(255 * hv)));
                int lw = Math.round((w - 16) * hv);
                g2.fillRect((w - lw) / 2, h - 2, lw, 2);
            }
        } finally {
            g2.dispose();
        }
        paint(g, c);
    }

    // Pilula dourada (principal) ou contorno claro, como no modelo "Noite de estreia".
    private static void estreia(Graphics2D g2, AbstractButton b, int w, int h, float hv, int dy) {
        float sobe = primario(b) ? 1.5f * hv : 0f;
        // pilula; botoes quase quadrados (so de icone) ficam com cantos arredondados
        float raio = (w < 2 * h && b.getClientProperty(REDONDO) == null) ? RAIO : h - 2 * MARGEM;
        Shape corpo = new RoundRectangle2D.Float(MARGEM, MARGEM + dy - sobe, w - 2 * MARGEM - 1, h - 2 * MARGEM - 1, raio, raio);
        if (!b.isEnabled()) {
            g2.setColor(new Color(244, 241, 234, 30));
            g2.draw(corpo);
        } else if (primario(b)) {
            for (int i = 3; i > 0 && hv > 0; i--) {
                g2.setColor(Tema.alfa(Estreia.OURO, Math.round(22 * hv)));
                g2.setStroke(new BasicStroke(i * 2f));
                g2.draw(corpo);
            }
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Tema.mistura(Estreia.OURO, new Color(0xFFD978), hv));
            g2.fill(corpo);
        } else {
            g2.setColor(new Color(244, 241, 234, Math.round(8 + 14 * hv)));
            g2.fill(corpo);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(Tema.mistura(new Color(244, 241, 234, 56), Estreia.BRANCO, hv));
            g2.draw(corpo);
        }
    }

    // Principal: marcado pela tela (PRIMARIO) ou, nas telas do jogo, todo botao
    // que nao e de voltar/cancelar.
    static boolean primario(AbstractButton b) {
        Object p = b.getClientProperty(PRIMARIO);
        return (p instanceof Boolean) ? (Boolean) p : !secundario(b);
    }

    @Override
    protected void paintText(Graphics g, JComponent c, Rectangle r, String text) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color fg;
        boolean pilula = b.isContentAreaFilled();
        if (!pilula) {
            fg = b.isEnabled() ? Tema.texto(b.getForeground()) : Tema.TEXTO_FRACO;
            if (hover > 0 && b.isEnabled()) fg = Tema.mistura(fg, Tema.NEON, Animacao.suave(hover));
        } else if (!b.isEnabled()) {
            fg = Estreia.APAGADO;
        } else if (primario(b)) {
            fg = Estreia.OURO_ESCURO;
        } else {
            fg = Tema.mistura(Estreia.FRACO, Estreia.BRANCO, Animacao.suave(hover));
        }
        int dy = Math.round(aperto);
        if (pilula && primario(b)) dy -= Math.round(1.5f * Animacao.suave(hover));
        java.awt.FontMetrics fm = g2.getFontMetrics(b.getFont());
        g2.setFont(b.getFont());
        g2.setColor(fg);
        BasicGraphicsUtils.drawStringUnderlineCharAt(g2, text, b.getDisplayedMnemonicIndex(),
            r.x + getTextShiftOffset(), r.y + fm.getAscent() + getTextShiftOffset() + dy);
    }

    @Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
        if (!b.isContentAreaFilled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 120));
        g2.drawRoundRect(MARGEM + 2, MARGEM + 2, b.getWidth() - 2 * MARGEM - 5, b.getHeight() - 2 * MARGEM - 5, RAIO - 4, RAIO - 4);
        g2.dispose();
    }

    @Override
    protected void paintButtonPressed(Graphics g, AbstractButton b) {
        // o "afundar" ja e desenhado em update()
    }
}
