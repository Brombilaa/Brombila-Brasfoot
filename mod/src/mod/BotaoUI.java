package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
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
    // onda de luz do clique (ponto e hora) e posicao do mouse para o brilho
    private long clique;
    private float ox, oy;
    private int mx = -1;
    private AbstractButton botao;
    // pulinho do icone quando o mouse chega (botoes so de icone, barra de cima)
    private long pulo;
    private boolean dentro;
    private static final int PULO_MS = 420;

    public static ComponentUI createUI(JComponent c) {
        return new BotaoUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        botao = (AbstractButton) c;
        botao.setRolloverEnabled(true);
        ouvinte = new ChangeListener() {
            public void stateChanged(ChangeEvent e) { anima(); }
        };
        botao.getModel().addChangeListener(ouvinte);
        botao.addMouseListener(mouse);
        botao.addMouseMotionListener(mouse);
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
        if (botao != null) {
            botao.removeMouseListener(mouse);
            botao.removeMouseMotionListener(mouse);
        }
        botao = null;
        super.uninstallUI(c);
    }

    private final java.awt.event.MouseAdapter mouse = new java.awt.event.MouseAdapter() {
        public void mousePressed(java.awt.event.MouseEvent e) {
            if (botao == null || !botao.isEnabled() || !Animacao.ligada()) return;
            clique = System.currentTimeMillis();
            ox = e.getX();
            oy = e.getY();
            anima();
        }
        public void mouseMoved(java.awt.event.MouseEvent e) {
            mx = e.getX();
            if (hover > 0f && botao != null) botao.repaint();
        }
        public void mouseExited(java.awt.event.MouseEvent e) { mx = -1; }
    };

    private static final int ONDA_MS = 480;

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
        if (alvoH == 1f && !dentro && !botao.isContentAreaFilled() && Animacao.ligada()) pulo = System.currentTimeMillis();
        dentro = alvoH == 1f;
        botao.repaint();
        boolean onda = System.currentTimeMillis() - clique < ONDA_MS || System.currentTimeMillis() - pulo < PULO_MS;
        if (hover == alvoH && aperto == alvoA && !onda) timer.stop();
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
        if (c instanceof javax.swing.plaf.metal.MetalComboBoxButton && !(((javax.swing.plaf.metal.MetalComboBoxButton) c).getComboIcon() instanceof SetaVazia)) {
            // o Metal poe a setinha dele depois do UI; troca por um icone invisivel do mesmo tamanho
            javax.swing.plaf.metal.MetalComboBoxButton cbb = (javax.swing.plaf.metal.MetalComboBoxButton) c;
            cbb.setComboIcon(new SetaVazia(cbb.getComboIcon()));
            cbb.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            cbb.setMargin(new java.awt.Insets(0, 0, 0, 0));
        }
        if (c instanceof javax.swing.plaf.metal.MetalComboBoxButton) { combo(g, c); return; }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            float hv = Animacao.suave(hover);
            int dy = Math.round(aperto);
            if (b.isContentAreaFilled()) {
                Shape corpo = estreia(g2, b, w, h, hv, dy);
                if (b.isEnabled()) luz(g2, b, corpo, hv);
            } else if (hv > 0.01f && b.isEnabled() && w > 0 && h > 0) {
                // botao so de icone: halo de refletor + sublinhado dourado (num quadro proprio)
                java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D k = im.createGraphics();
                k.setPaint(new RadialGradientPaint(w / 2f, h, Math.max(w, h) * 0.75f, new float[] {0f, 1f},
                    new Color[] {Tema.alfa(Tema.DOURADO, Math.round(70 * hv)), Tema.alfa(Tema.DOURADO, 0)}));
                k.fillRect(0, 0, w, h);
                k.setColor(Tema.alfa(Tema.DOURADO, Math.round(255 * hv)));
                int lw = Math.round((w - 16) * hv);
                k.fillRect((w - lw) / 2, h - 2, lw, 2);
                k.dispose();
                g2.drawImage(im, 0, 0, null);
            }
        } finally {
            g2.dispose();
        }
        paint(g, c);
    }

    @Override
    protected void paintIcon(Graphics g, JComponent c, java.awt.Rectangle r) {
        float t = (System.currentTimeMillis() - pulo) / (float) PULO_MS;
        if (pulo == 0 || t >= 1f) {
            super.paintIcon(g, c, r);
            return;
        }
        // sobe rapido, desce e da um quiquezinho
        double dy = t < 0.62f ? -4 * Math.sin(Math.PI * t / 0.62f) : -1.2 * Math.sin(Math.PI * (t - 0.62f) / 0.38f);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, dy);
        super.paintIcon(g2, c, r);
        g2.dispose();
    }

    static class SetaVazia implements javax.swing.Icon {
        final int w, h;
        SetaVazia(javax.swing.Icon o) { w = 28; h = o == null ? 8 : o.getIconHeight(); }
        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component k, Graphics g, int x, int y) { }
    }

    // Botao do combo: bolinha dourada proporcional (diametro = lado menor menos folga), sem moldura
    private void combo(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        int w = c.getWidth(), h = c.getHeight();
        Color fundo = new Color(18, 22, 20);
        if (c.getParent() instanceof javax.swing.JComboBox) {
            // mesma cor do campo ao lado, para nao sobrar um quadrado em volta da bolinha
            @SuppressWarnings("unchecked")
            javax.swing.JComboBox<Object> cb = (javax.swing.JComboBox<Object>) c.getParent();
            fundo = cb.getBackground();
            try {
                Component r = cb.getRenderer().getListCellRendererComponent(new javax.swing.JList<Object>(), cb.getSelectedItem(), -1, false, false);
                if (r != null && r.getBackground() != null && r.getBackground().getAlpha() == 255) fundo = r.getBackground();
            } catch (Throwable t) { }
        }
        g.setColor(fundo);
        g.fillRect(0, 0, w, h);
        // a linha que o Metal risca um pixel antes do botao fica fora dele: alarga o clip para apagar
        g.setClip(-2, 0, w + 2, h);
        g.fillRect(-2, 1, 2, h - 2);
        float hv = Animacao.suave(hover);
        float d = Math.max(14, Math.min(w, h) - 8);
        float x = (w - d) / 2f, y = (h - d) / 2f;
        Color base = c.isEnabled() ? Estreia.OURO : Tema.alfa(Estreia.OURO, 90);
        g.setColor(base);
        g.fill(new java.awt.geom.Ellipse2D.Float(x, y, d, d));
        if (hv > 0.01f) {
            g.setColor(Tema.alfa(Color.WHITE, Math.round(70 * hv)));
            g.fill(new java.awt.geom.Ellipse2D.Float(x, y, d, d));
        }
        float cx = w / 2f, cy = h / 2f + Math.round(aperto);
        float k = d / 10f;
        g.setColor(c.isEnabled() ? Estreia.OURO_ESCURO : Tema.alfa(Estreia.OURO_ESCURO, 120));
        g.setStroke(new java.awt.BasicStroke(Math.max(1.6f, d / 11f), java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
        p.moveTo(cx - 1.8f * k, cy - 0.7f * k); p.lineTo(cx, cy + 1.1f * k); p.lineTo(cx + 1.8f * k, cy - 0.7f * k);
        g.draw(p);
        g.dispose();
    }

    // Setinha do combo: V fino dourado, desenhado do zero (a de Metal e um triangulo minusculo)
    private void seta(Graphics g0, JComponent c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        boolean hv = ((AbstractButton) c).getModel().isRollover() || ((AbstractButton) c).getModel().isArmed();
        float cx = c.getWidth() / 2f, cy = c.getHeight() / 2f + (hv ? 1f : 0f);
        g.setColor(c.isEnabled() ? Estreia.OURO_ESCURO : Tema.alfa(Estreia.OURO_ESCURO, 90));
        g.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
        p.moveTo(cx - 4, cy - 2); p.lineTo(cx, cy + 2); p.lineTo(cx + 4, cy - 2);
        g.draw(p);
        g.dispose();
    }

    // Pilula dourada (principal) ou contorno claro, como no modelo "Noite de estreia".
    // Brilho suave que acompanha o mouse em cima do botao e onda de luz que se abre do ponto
    // do clique (0,48 s), presos ao formato do botao.
    private void luz(Graphics2D g, AbstractButton b, Shape corpo, float hv) {
        if (!Animacao.ligada()) return;
        boolean prim = primario(b);
        java.awt.Rectangle c = corpo.getBounds();
        float t = (System.currentTimeMillis() - clique) / (float) ONDA_MS;
        boolean brilho = hv > 0.01f && mx >= 0;
        boolean onda = clique > 0 && t >= 0f && t < 1f;
        if (!brilho && !onda) return;
        // o brilho e a onda sao desenhados num quadro proprio e recortados pelo formato do botao
        // na memoria; assim nao dependem do recorte nem do gradiente da placa de video
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(c.width, c.height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(-c.x, -c.y);
        if (brilho) {
            float r = Math.max(c.height * 1.6f, 30f);
            Color cor = prim ? new Color(255, 248, 226) : Estreia.OURO;
            g2.setPaint(new RadialGradientPaint(mx, c.y + c.height / 2f, r, new float[] {0f, 1f},
                new Color[] {Tema.alfa(cor, Math.round((prim ? 110 : 40) * hv)), Tema.alfa(cor, 0)}));
            g2.fill(c);
        }
        if (onda) {
            float rmax = (float) Math.hypot(Math.max(ox - c.x, c.x + c.width - ox), Math.max(oy - c.y, c.y + c.height - oy));
            float r = 4f + (rmax - 4f) * Animacao.suave(t);
            Color cor = prim ? Color.WHITE : Estreia.OURO;
            g2.setColor(Tema.alfa(cor, Math.round((prim ? 110 : 70) * (1f - t))));
            g2.fill(new java.awt.geom.Ellipse2D.Float(ox - r, oy - r, 2 * r, 2 * r));
        }
        g2.setComposite(java.awt.AlphaComposite.DstIn);
        g2.setColor(Color.WHITE);
        g2.fill(corpo);
        g2.dispose();
        g.drawImage(img, c.x, c.y, null);
    }

    private static Shape estreia(Graphics2D g2, AbstractButton b, int w, int h, float hv, int dy) {
        float sobe = primario(b) ? 1.5f * hv : 0f;
        // pilula; botoes quase quadrados (so de icone) ficam com cantos arredondados
        float raio = (w < 2 * h && b.getClientProperty(REDONDO) == null) ? RAIO : h - 2 * MARGEM;
        Shape corpo = new RoundRectangle2D.Float(MARGEM, MARGEM + dy - sobe, w - 2 * MARGEM - 1, h - 2 * MARGEM - 1, raio, raio);
        if (b.getClientProperty(REDONDO) != null) {
            // circulo no meio do espaco que o layout do jogo deu ao botao
            float d = Math.min(w, h) - 2 * MARGEM - 1;
            corpo = new java.awt.geom.Ellipse2D.Float((w - d) / 2f, (h - d) / 2f + dy - sobe, d, d);
        }
        if (!b.isEnabled()) {
            g2.setColor(new Color(244, 241, 234, 52));
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
        return corpo;
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
            if (hover > 0 && b.isEnabled()) fg = Tema.mistura(fg, Tema.DOURADO, Animacao.suave(hover));
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
