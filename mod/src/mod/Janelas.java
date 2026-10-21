package mod;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSpinnerUI;

// Janelas pequenas do jogo no estilo "Noite de estreia":
//  - Estadio (a.cl): fundo novo em aicons/estadio.jpg; aqui ficam fontes, campos de preco,
//    o X redondo e os botoes;
//  - Academia de Juniores (a.ds): cartao escuro com luz de refletor, cabecalho com titulo
//    grande e vagas em destaque, botoes-pilula com icones.
final class Janelas {
    private Janelas() {}

    static final String FEITO = "brombila.janela";

    private static Object campo(Object dono, String nome) {
        try {
            Field f = dono.getClass().getDeclaredField(nome);
            f.setAccessible(true);
            return f.get(dono);
        } catch (Throwable t) {
            return null;
        }
    }

    // Reposiciona um componente num painel com o AbsoluteLayout do jogo (components.b).
    private static void posicao(Component c, int x, int y, int w, int h) {
        if (c == null || c.getParent() == null) return;
        LayoutManager l = c.getParent().getLayout();
        if (!(l instanceof LayoutManager2)) return;
        try {
            Object k = Class.forName("components.a").getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(x, y, w, h);
            ((LayoutManager2) l).addLayoutComponent(c, k);
        } catch (Throwable t) {
            // fica onde o jogo colocou
        }
    }

    static void fechar(Object o) {
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        b.setText("");
        b.setToolTipText("Fechar");
        b.setIcon(new Estreia.Icone("fechar", 16, Estreia.BRANCO));
        b.putClientProperty(BotaoUI.PRIMARIO, Boolean.FALSE);
        b.putClientProperty(BotaoUI.REDONDO, Boolean.TRUE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    private static void botao(Object o, String texto, String icone, boolean primario, float fonte) {
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        if (texto != null) b.setText(texto);
        b.putClientProperty(BotaoUI.PRIMARIO, primario ? Boolean.TRUE : Boolean.FALSE);
        b.setFont(Estreia.negrito(fonte));
        Color cor = primario ? Estreia.OURO_ESCURO : Estreia.OURO;
        b.setIcon(new Estreia.Icone(icone, 15, cor));
        b.setDisabledIcon(new Estreia.Icone(icone, 15, Estreia.APAGADO));
        b.setIconTextGap(6);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 8, 6, 10));
    }

    // ------------------------------------------------------------- estadio

    static void estadio(JPanel cl) {
        if (cl.getClientProperty(FEITO) != null) return;
        cl.putClientProperty(FEITO, Boolean.TRUE);
        cl.putClientProperty(Estreia.MARCA, Boolean.TRUE); // cores proprias

        final JLabel titulo = (JLabel) campo(cl, "Cg");
        if (titulo != null) {
            titulo.setFont(Estreia.titulo(26f));
            titulo.setForeground(Estreia.BRANCO);
            posicao(titulo, 26, 14, 500, 36);
        }
        String[] valores = {"Cf", "Cc", "Cd", "Ce"};
        int[] ys = {122, 150, 177, 204};
        for (int i = 0; i < valores.length; i++) {
            Object o = campo(cl, valores[i]);
            if (o instanceof JLabel) {
                JLabel l = (JLabel) o;
                l.setFont(Estreia.negrito(15f));
                l.setForeground(Estreia.BRANCO);
                posicao(l, 165, ys[i] - 1, 110, 21);
            }
        }
        String[] precos = {"Cl", "Ci", "Cj", "Ck"};
        int[] yp = {122, 150, 180, 210};
        for (int i = 0; i < precos.length; i++) {
            Object o = campo(cl, precos[i]);
            if (o instanceof JSpinner) {
                seletor((JSpinner) o);
                posicao((JSpinner) o, 460, yp[i] - 3, 84, 25);
            }
        }
        Object sugerir = campo(cl, "Ca");
        botao(sugerir, "Sugeridos", "etiqueta", false, 12f);
        if (sugerir instanceof JButton) {
            ((JButton) sugerir).setToolTipText("Usar os preços sugeridos");
            posicao((JButton) sugerir, 440, 232, 118, 28);
        }
        Object sugestao = campo(cl, "Ch");
        if (sugestao instanceof JLabel) {
            ((JLabel) sugestao).setFont(Estreia.semi(12.5f));
            ((JLabel) sugestao).setForeground(Estreia.OURO);
            posicao((JLabel) sugestao, 450, 277, 140, 18);
        }
        Object sempre = campo(cl, "Cb");
        if (sempre instanceof JCheckBox) {
            JCheckBox c = (JCheckBox) sempre;
            c.setUI(new ConfigEstreia.Interruptor());
            c.setFont(Estreia.texto(12.5f));
            c.setForeground(Estreia.FRACO);
            c.setOpaque(false);
            posicao(c, 312, 296, 280, 26);
        }
        Object obra = campo(cl, "zh");
        if (obra instanceof JLabel) {
            ((JLabel) obra).setFont(Estreia.semi(12.5f));
            ((JLabel) obra).setForeground(Estreia.OURO);
            posicao((JLabel) obra, 30, 270, 270, 20);
        }
        Object expandir = campo(cl, "BY");
        botao(expandir, "Expandir estádio", "obra", true, 13f);
        posicao((Component) expandir, 26, 280, 180, 36);
        Object x = campo(cl, "BZ");
        fechar(x);
        posicao((Component) x, 552, 12, 36, 36);
        cl.revalidate();
        cl.repaint();
    }

    // Campo numerico: caixa escura arredondada, numero em Manrope e setas finas.
    static void seletor(JSpinner s) {
        s.setUI(new Seletor());
        s.setOpaque(false);
        s.setBorder(new EmptyBorder(0, 0, 0, 0));
        JComponent ed = s.getEditor();
        ed.setOpaque(false);
        if (ed instanceof JSpinner.DefaultEditor) {
            JTextField t = ((JSpinner.DefaultEditor) ed).getTextField();
            t.setFont(Estreia.negrito(13.5f));
            t.setForeground(Estreia.BRANCO);
            t.setDisabledTextColor(Estreia.APAGADO);
            t.setCaretColor(Estreia.OURO);
            t.setSelectionColor(ConfigEstreia.SELECAO);
            t.setOpaque(false);
            t.setHorizontalAlignment(SwingConstants.CENTER);
            t.setBorder(new EmptyBorder(0, 6, 0, 0));
        }
        // o jogo troca o modelo (e o editor) ao marcar "precos sugeridos"
        if (s.getClientProperty(FEITO) == null) {
            s.putClientProperty(FEITO, Boolean.TRUE);
            s.addPropertyChangeListener("editor", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    final JSpinner sp = (JSpinner) e.getSource();
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() { seletor(sp); sp.revalidate(); sp.repaint(); }
                    });
                }
            });
        }
    }

    static class Seletor extends BasicSpinnerUI {
        @Override
        protected Component createNextButton() {
            Component c = new Seta(true);
            c.setName("Spinner.nextButton");
            installNextButtonListeners(c);
            return c;
        }

        @Override
        protected Component createPreviousButton() {
            Component c = new Seta(false);
            c.setName("Spinner.previousButton");
            installPreviousButtonListeners(c);
            return c;
        }

        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10);
            g.setColor(c.isEnabled() ? new Color(6, 9, 8, 170) : new Color(6, 9, 8, 90));
            g.fill(r);
            g.setColor(c.isEnabled() ? new Color(244, 241, 234, 60) : new Color(244, 241, 234, 26));
            g.draw(r);
            // divisoria antes das setas
            g.setColor(new Color(244, 241, 234, 28));
            g.drawLine(w - 22, 5, w - 22, h - 6);
            g.dispose();
            super.paint(g0, c);
        }

        @Override
        protected java.awt.LayoutManager createLayout() {
            return new java.awt.LayoutManager() {
                public void addLayoutComponent(String n, Component c) {}
                public void removeLayoutComponent(Component c) {}
                public Dimension preferredLayoutSize(Container p) { return new Dimension(84, 25); }
                public Dimension minimumLayoutSize(Container p) { return preferredLayoutSize(p); }
                public void layoutContainer(Container p) {
                    int w = p.getWidth(), h = p.getHeight();
                    for (Component k : p.getComponents()) {
                        if ("Spinner.nextButton".equals(k.getName())) k.setBounds(w - 21, 1, 20, h / 2 - 1);
                        else if ("Spinner.previousButton".equals(k.getName())) k.setBounds(w - 21, h / 2, 20, h - h / 2 - 1);
                        else k.setBounds(1, 1, w - 24, h - 2);
                    }
                }
            };
        }
    }

    // Seta fina (chevron) que acende em dourado no hover.
    static class Seta extends JButton {
        final boolean cima;
        Seta(boolean cima) {
            this.cima = cima;
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float cx = getWidth() / 2f, cy = getHeight() / 2f + (cima ? 1f : -1f);
            Path2D p = new Path2D.Float();
            p.moveTo(cx - 3.5f, cy + (cima ? 1.8f : -1.8f));
            p.lineTo(cx, cy + (cima ? -1.8f : 1.8f));
            p.lineTo(cx + 3.5f, cy + (cima ? 1.8f : -1.8f));
            boolean on = isEnabled() && (getModel().isRollover() || getModel().isPressed());
            g.setColor(!isEnabled() ? Estreia.APAGADO : on ? Estreia.OURO : Estreia.FRACO);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
            g.dispose();
        }
    }

    // ------------------------------------------------------------- juniores

    static void juniores(final JPanel ds) {
        if (ds.getClientProperty(FEITO) != null) return;
        ds.putClientProperty(FEITO, Boolean.TRUE);
        ds.setOpaque(false);
        ds.setBorder(new EmptyBorder(0, 0, 0, 0));
        Container pai = ds.getParent();
        if (pai != null && !(pai instanceof Cartao)) {
            pai.remove(ds);
            pai.setLayout(new BorderLayout());
            Cartao c = new Cartao();
            c.add(ds);
            pai.add(c, BorderLayout.CENTER);
            pai.revalidate();
            pai.repaint();
        }
        Object cab = campo(ds, "vd");
        if (cab instanceof JPanel) {
            ((JPanel) cab).setOpaque(false);
            ((JPanel) cab).putClientProperty(Estreia.MARCA, Boolean.TRUE);
        }
        Object titulo = campo(ds, "ug");
        if (titulo instanceof JLabel) {
            JLabel l = (JLabel) titulo;
            l.setText("ACADEMIA DE JUNIORES");
            l.setFont(Estreia.titulo(25f));
            l.setForeground(Estreia.BRANCO);
            l.setIconTextGap(12);
        }
        Object vagas = campo(ds, "uh");
        if (vagas instanceof JLabel) {
            JLabel l = (JLabel) vagas;
            l.setFont(Estreia.negrito(13f));
            l.setForeground(Estreia.OURO);
        }
        fechar(campo(ds, "vb"));
        botao(campo(ds, "FQ"), null, "cima", true, 12f);
        botao(campo(ds, "FP"), null, "sair", false, 12f);
        botao(campo(ds, "FN"), null, "mais", true, 12f);
        botao(campo(ds, "FO"), null, "lapis", false, 12f);
        Object rolagem = campo(ds, "ut");
        if (rolagem instanceof JComponent) {
            ((JComponent) rolagem).setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 30)));
        }
        ds.revalidate();
        ds.repaint();
    }

    // Fundo da janela: cartao escuro, luz de refletor no alto, filete dourado e contorno fino.
    static class Cartao extends JPanel {
        Cartao() {
            super(new BorderLayout());
            setOpaque(true);
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x1A1E1B), 0, h, new Color(0x0C0F0D)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(w * 0.22f, -20, Math.max(w, h) * 0.7f, new float[] {0f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 28), Tema.alfa(Estreia.OURO, 0)}));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, 0, Tema.alfa(Estreia.OURO, 0), w / 2f, 0, Tema.alfa(Estreia.OURO, 210), true));
            g.fillRect(0, 0, w, 2);
            g.setColor(new Color(244, 241, 234, 34));
            g.drawRect(0, 0, w - 1, h - 1);
            g.dispose();
        }
    }
}
