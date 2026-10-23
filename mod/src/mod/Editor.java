package mod;

import static mod.Reflexao.campo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;

// Editor de times (d.n) e as janelas dele (Editar Time d.ac, Editar Jogador d.f,
// Transferir Jogador d.ai) no estilo "Noite de estreia": foto do estadio ao fundo,
// cartoes de vidro no lugar dos paineis verdes, tabelas neutras e botoes-pilula com icones.
final class Editor {
    private Editor() {}

    // ------------------------------------------------------------- janela principal

    static void aplica(final JFrame n) {
        final JComponent conteudo = (JComponent) n.getContentPane();
        if (conteudo.getClientProperty(Janelas.FEITO) != null) return;
        conteudo.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        conteudo.putClientProperty(Estreia.MARCA, Boolean.TRUE); // cores proprias
        conteudo.setOpaque(false);

        // foto do estadio atras de tudo
        final JLayeredPane camadas = n.getLayeredPane();
        final ConfigEstreia.Fundo fundo = new ConfigEstreia.Fundo(false);
        camadas.add(fundo, Integer.valueOf(JLayeredPane.FRAME_CONTENT_LAYER - 1));
        fundo.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        camadas.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { fundo.setBounds(0, 0, camadas.getWidth(), camadas.getHeight()); }
        });

        for (String p : new String[] {"vd", "wf", "we"}) cartao(campo(n, p));
        Object busca = campo(n, "wf");
        if (busca instanceof JComponent) ((JComponent) busca).setBorder(new Cartao(false));

        // lista de times e elenco
        for (String t : new String[] {"Vg", "UQ"}) {
            Object o = campo(n, t);
            if (o instanceof JTable) tabela((JTable) o);
        }
        for (String s : new String[] {"ut", "wi"}) {
            Object o = campo(n, s);
            if (o instanceof JScrollPane) rolagem((JScrollPane) o);
        }

        // procurar
        for (String l : new String[] {"Ve", "Vd"}) rotulo(campo(n, l), Estreia.semi(12.5f), Estreia.FRACO);
        Object mostrar = campo(n, "Va");
        if (mostrar instanceof JLabel) {
            ((JLabel) mostrar).setText("MOSTRAR TIMES");
            rotulo(mostrar, Estreia.espacada(Estreia.semi(11f), 0.2f), Estreia.OURO);
        }
        for (String t : new String[] {"Ej", "Ek"}) {
            Object o = campo(n, t);
            if (o instanceof JTextField) texto((JTextField) o);
        }
        for (String b : new String[] {"UJ", "UK"}) {
            Object o = campo(n, b);
            if (o instanceof JButton) {
                JButton bt = (JButton) o;
                bt.setIcon(new Estreia.Icone("busca", 16, Estreia.OURO));
                bt.putClientProperty(BotaoUI.PRIMARIO, Boolean.FALSE);
                bt.setToolTipText("Procurar");
            }
        }
        botao(campo(n, "UM"), "Sem escudo", null, false);
        botao(campo(n, "UL"), "Sem camisas", null, false);
        botao(campo(n, "UE"), "Jogadores duplicados", null, false);
        botao(campo(n, "UC"), "Adicionar", "mais", true);
        botao(campo(n, "UF"), "Editar", "lapis", false);
        botao(campo(n, "UD"), "Excluir", "fechar", false);
        botao(campo(n, "Kc"), "Início", "volta", false);
        botao(campo(n, "BZ"), "Sair do jogo", "sair", false);
        botao(campo(n, "UO"), null, null, true);
        botao(campo(n, "UN"), null, null, false);
        Object aleatorio = campo(n, "afR");
        if (aleatorio instanceof JButton) {
            botao(aleatorio, null, "mais", true);
        }

        // cartao do time: nome grande, reputacao em rotulo dourado, nivel em destaque
        cor(campo(n, "Gk"), Estreia.titulo(24f), Estreia.BRANCO);
        cor(campo(n, "MR"), Estreia.espacada(Estreia.semi(11.5f), 0.2f), Estreia.OURO);
        for (String l : new String[] {"MQ", "MM", "UV", "MS"}) cor(campo(n, l), Estreia.semi(13.5f), Estreia.BRANCO);
        cor(campo(n, "UW"), Estreia.texto(12.5f), Estreia.FRACO);
        cor(campo(n, "Vb"), Estreia.titulo(26f), Estreia.OURO);
        for (String l : new String[] {"UZ", "UY"}) cor(campo(n, l), Estreia.negrito(13f), Estreia.BRANCO);
        cor(campo(n, "afS"), Estreia.texto(12.5f), Estreia.FRACO);
        Object nivel = campo(n, "Vb");
        if (nivel instanceof JLabel) ((JLabel) nivel).setToolTipText("Nível");
        // a Saira tem entrelinha grande: altura justa para o cartao nao crescer
        compacto(campo(n, "Gk"), 28);
        compacto(nivel, 30);
        n.repaint();
    }

    // Cor fixa: o editor pinta os textos com a cor do time a cada time escolhido
    // (texto escuro sumiria no cartao), entao a cor volta sempre para a nossa.
    static void cor(Object o, java.awt.Font fonte, final Color cor) {
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        l.setFont(fonte);
        l.setForeground(cor);
        l.addPropertyChangeListener("foreground", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                if (!cor.equals(l.getForeground())) l.setForeground(cor);
            }
        });
    }

    static void compacto(Object o, final int altura) {
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        final Runnable mede = new Runnable() {
            public void run() {
                String t = l.getText();
                int w = (t == null) ? 0 : l.getFontMetrics(l.getFont()).stringWidth(t) + 4;
                l.setPreferredSize(new java.awt.Dimension(w, altura));
                l.setMinimumSize(new java.awt.Dimension(Math.min(w, 40), altura));
                l.revalidate();
            }
        };
        mede.run();
        l.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) { mede.run(); }
        });
    }

    private static void rotulo(Object o, java.awt.Font fonte, Color cor) {
        if (!(o instanceof JLabel)) return;
        ((JLabel) o).setFont(fonte);
        ((JLabel) o).setForeground(cor);
    }

    private static void cartao(Object o) {
        if (!(o instanceof JPanel)) return;
        JPanel p = (JPanel) o;
        p.setOpaque(false);
        p.setBorder(new Cartao(true));
    }

    static void tabela(JTable t) {
        t.setBackground(ConfigEstreia.CELULA);
        t.setRowHeight(Math.max(t.getRowHeight(), 22));
        t.setIntercellSpacing(new java.awt.Dimension(0, 0));
        t.setSelectionBackground(ConfigEstreia.SELECAO);
        t.setSelectionForeground(Estreia.BRANCO);
        t.setFillsViewportHeight(true);
    }

    static void rolagem(JScrollPane s) {
        s.setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 30)));
        s.getViewport().setBackground(ConfigEstreia.CELULA);
        if (s.getColumnHeader() != null) s.getColumnHeader().setBackground(ConfigEstreia.CABECALHO);
        java.awt.Component canto = s.getCorner(JScrollPane.UPPER_RIGHT_CORNER);
        if (canto == null) {
            JPanel c = new JPanel();
            c.setBackground(ConfigEstreia.CABECALHO);
            s.setCorner(JScrollPane.UPPER_RIGHT_CORNER, c);
        }
    }

    static void texto(JTextField tf) {
        tf.setFont(Estreia.semi(12.5f));
        tf.setBackground(ConfigEstreia.CELULA);
        tf.setForeground(Estreia.BRANCO);
        tf.setDisabledTextColor(Estreia.APAGADO);
        tf.setCaretColor(Estreia.OURO);
        tf.setSelectionColor(ConfigEstreia.SELECAO);
        tf.setSelectedTextColor(Estreia.BRANCO);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new Cartao.Linha(), BorderFactory.createEmptyBorder(1, 7, 1, 7)));
    }

    static void botao(Object o, String texto, String icone, boolean primario) {
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        if (texto != null) b.setText(texto);
        b.putClientProperty(BotaoUI.PRIMARIO, primario ? Boolean.TRUE : Boolean.FALSE);
        b.setFont(Estreia.negrito(12.5f));
        if (icone != null) {
            b.setIcon(new Estreia.Icone(icone, 15, primario ? Estreia.OURO_ESCURO : Estreia.OURO));
            b.setDisabledIcon(new Estreia.Icone(icone, 15, Estreia.APAGADO));
            b.setIconTextGap(6);
        }
        b.setMargin(new Insets(2, 8, 2, 8));
        b.setFocusPainted(false);
    }

    // ------------------------------------------------------------- janelas do editor

    // Editar/Adicionar Time (d.ac), Editar/Adicionar Jogador (d.f), Transferir Jogador (d.ai)
    static void janela(JPanel raiz, String interno, String titulo) {
        if (raiz.getClientProperty(Janelas.FEITO) != null) return;
        raiz.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        raiz.setOpaque(false);
        Janelas.embrulha(raiz);
        if (interno != null) {
            Object o = campo(raiz, interno);
            if (o instanceof JPanel) ((JPanel) o).setOpaque(false);
        }
        Object t = campo(raiz, titulo);
        if (t instanceof JLabel) {
            final JLabel l = (JLabel) t;
            l.setFont(Estreia.titulo(26f));
            l.setForeground(Estreia.BRANCO);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            maiusculas(l);
            l.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { maiusculas(l); }
            });
            compacto(l, 24);
        }
        // rotulos com largura fixa no layout do jogo
        Object corTexto = campo(raiz, "zv"), corFundo = campo(raiz, "zw");
        if (corTexto instanceof JLabel) ((JLabel) corTexto).setText("Cor do texto:");
        if (corFundo instanceof JLabel) ((JLabel) corFundo).setText("Cor do fundo:");
        estiliza(raiz);
        raiz.revalidate();
        raiz.repaint();
    }

    private static void maiusculas(JLabel l) {
        String s = l.getText();
        if (s != null && !s.equals(s.toUpperCase())) l.setText(s.toUpperCase());
    }

    private static void estiliza(java.awt.Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) k;
                cb.setOpaque(false);
                cb.setUI(new ConfigEstreia.Interruptor());
                cb.setFont(Estreia.texto(13f));
                cb.setForeground(Estreia.BRANCO);
            } else if (k instanceof JSlider) {
                JSlider s = (JSlider) k;
                s.setUI(new Trilho(s));
                s.setOpaque(false);
            } else if (k instanceof JTextField) {
                texto((JTextField) k);
            } else if (k instanceof JTable) {
                tabela((JTable) k);
            } else if (k instanceof JScrollPane) {
                rolagem((JScrollPane) k);
            } else if (k instanceof JLabel && ((JLabel) k).getIcon() == null && !((JLabel) k).isOpaque()
                       && ((JLabel) k).getFont() != null && ((JLabel) k).getFont().getSize() < 14) {
                k.setFont(Estreia.semi(12f));
            } else if (k instanceof JButton) {
                JButton b = (JButton) k;
                String t = b.getText();
                if ("OK".equals(t) || "Confirmar".equals(t)) botao(b, null, "seta", true);
                else if ("Cancelar".equals(t)) botao(b, null, null, false);
            }
            if (k instanceof java.awt.Container && !(k instanceof JScrollPane)) estiliza((java.awt.Container) k);
        }
    }

    // ------------------------------------------------------------- pecas

    // Fundo de cartao desenhado pela borda (os paineis do jogo nao podem ser trocados).
    static class Cartao extends AbstractBorder {
        final boolean filete;
        Cartao(boolean filete) { this.filete = filete; }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        @Override
        public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 0, 0); return i; }
        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 16, 16);
            g.setColor(new Color(16, 20, 18, 214));
            g.fill(r);
            if (filete) {
                g.setClip(r);
                g.setPaint(new java.awt.GradientPaint(x, 0, Tema.alfa(Estreia.OURO, 0), x + w / 2f, 0, Tema.alfa(Estreia.OURO, 170), true));
                g.fillRect(x + 16, y, w - 32, 2);
                g.setClip(null);
            }
            g.setColor(new Color(244, 241, 234, 30));
            g.setStroke(new BasicStroke(1f));
            g.draw(r);
            g.dispose();
        }

        // Contorno fino dos campos de texto, que acende em dourado com o foco.
        static class Linha extends AbstractBorder {
            @Override
            public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
            @Override
            public Insets getBorderInsets(Component c, Insets i) { i.set(1, 1, 1, 1); return i; }
            @Override
            public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(c.hasFocus() ? Tema.alfa(Estreia.OURO, 170) : new Color(244, 241, 234, 56));
                g.drawRect(x, y, w - 1, h - 1);
                g.dispose();
            }
        }
    }

    // Slider: trilho fino com a parte preenchida em dourado e bolinha com brilho.
    static class Trilho extends BasicSliderUI {
        Trilho(JSlider s) { super(s); }
        @Override
        protected java.awt.Dimension getThumbSize() { return new java.awt.Dimension(16, 16); }
        @Override
        public void paintTrack(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle t = trackRect;
            float cy = t.y + t.height / 2f;
            g.setColor(new Color(244, 241, 234, 40));
            g.fill(new RoundRectangle2D.Float(t.x, cy - 2, t.width, 4, 4, 4));
            float fim = thumbRect.x + thumbRect.width / 2f;
            g.setColor(Estreia.OURO);
            g.fill(new RoundRectangle2D.Float(t.x, cy - 2, Math.max(0, fim - t.x), 4, 4, 4));
            g.dispose();
        }
        @Override
        public void paintThumb(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r = thumbRect;
            float cx = r.x + r.width / 2f, cy = r.y + r.height / 2f;
            g.setColor(Tema.alfa(Estreia.OURO, 50));
            g.fill(new Ellipse2D.Float(cx - 9, cy - 9, 18, 18));
            g.setColor(Estreia.OURO);
            g.fill(new Ellipse2D.Float(cx - 6.5f, cy - 6.5f, 13, 13));
            g.setColor(Estreia.OURO_ESCURO);
            g.fill(new Ellipse2D.Float(cx - 2, cy - 2, 4, 4));
            g.dispose();
        }
        @Override
        public void paintFocus(Graphics g) {}
    }
}
