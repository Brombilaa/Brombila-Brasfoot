package mod;

import static mod.Reflexao.campo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicRadioButtonUI;

// Tela "Configuracoes iniciais" (a.hU) no estilo "Noite de estreia".
// Os componentes do jogo continuam os mesmos (mesmas acoes e o mesmo layout);
// aqui so trocamos fundo, fontes, cores, chaves, opcoes e botoes.
final class ConfigEstreia {
    private ConfigEstreia() {}

    static final Color LINHA_TABELA = new Color(0x1A1F1C);
    static final Color CELULA = new Color(0x0E1210);
    static final Color ZEBRA = new Color(0x121714);
    static final Color CABECALHO = new Color(0x0A0D0C);
    static final Color SELECAO = new Color(0x3A3120);

    static void aplica(final JPanel hU) {
        if (hU.getClientProperty(Estreia.MARCA) != null) return;
        hU.putClientProperty(Estreia.MARCA, Boolean.TRUE);
        hU.setOpaque(false);

        // Fundo: foto do estadio escurecida ocupando a janela toda.
        Container pai = hU.getParent();
        if (pai != null && !(pai instanceof Fundo) && !(pai instanceof javax.swing.JViewport)) {
            pai.remove(hU);
            pai.setLayout(new BorderLayout());
            Fundo f = new Fundo(true);
            f.centro(hU);
            pai.add(f, BorderLayout.CENTER);
            pai.revalidate();
            pai.repaint();
        }

        for (String n : new String[] {"vd", "we", "wf", "wg"}) {
            Object p = campo(hU, n);
            if (p instanceof JPanel) {
                JPanel pp = (JPanel) p;
                pp.setOpaque(false);
                pp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estreia.LINHA));
            }
        }

        JLabel kicker = (JLabel) campo(hU, "a_");
        if (kicker != null) {
            kicker.setText("NOVO JOGO");
            kicker.setFont(Estreia.espacada(Estreia.semi(12f), 0.3f));
            kicker.setForeground(Estreia.OURO);
            kicker.setHorizontalAlignment(SwingConstants.LEFT);
            kicker.setBorder(BorderFactory.createEmptyBorder(6, 17, 0, 0));
        }
        JLabel titulo = (JLabel) campo(hU, "vf");
        if (titulo != null) {
            titulo.setText("CONFIGURAÇÕES INICIAIS");
            titulo.setFont(Estreia.titulo(44f));
            titulo.setForeground(Estreia.BRANCO);
            titulo.setHorizontalAlignment(SwingConstants.LEFT);
            titulo.setBorder(BorderFactory.createEmptyBorder(0, 17, 4, 0));
        }

        secao(hU, "Mo", "LIGAS NACIONAIS");
        secao(hU, "Mt", "COMPETIÇÕES INTERNACIONAIS");
        secao(hU, "Mp", "ESTADUAIS");
        secao(hU, "Mq", "REGIONAIS");
        secao(hU, "Mr", "SISTEMA DE SALÁRIOS");
        secao(hU, "Ms", "FORÇA DOS JOGADORES");
        secao(hU, "afB", "TEMPORADA DE INÍCIO");

        for (String n : new String[] {"Mh", "afz", "Mi", "Mj", "Mk"}) {
            Object o = campo(hU, n);
            if (o instanceof JCheckBox) {
                JCheckBox c = (JCheckBox) o;
                c.setFont(Estreia.texto(14f));
                c.setForeground(Estreia.BRANCO);
                c.setUI(new Interruptor());
                c.setOpaque(false);
            }
        }
        for (String n : new String[] {"Mu", "Mv", "Mw", "Mx"}) {
            Object o = campo(hU, n);
            if (o instanceof JRadioButton) {
                JRadioButton r = (JRadioButton) o;
                r.setUI(new Pilula());
                r.setFont(Estreia.semi(13f));
                r.setRolloverEnabled(true);
                r.setOpaque(false);
            }
        }

        botao(hU, "Bw", "Iniciar jogo", "seta", true);
        botao(hU, "Mg", "Voltar ao início", "volta", false);
        botao(hU, "Md", "Configurar", "ajuste", false);
        botao(hU, "Mb", "Configurar", "ajuste", false);
        botao(hU, "Me", "Configurar", "ajuste", false);
        botao(hU, "Mc", "Preferências", "ajuste", false);
        botao(hU, "Mf", null, "todos", false);
        Object todos = campo(hU, "Mf");
        if (todos instanceof JButton) ((JButton) todos).setToolTipText("Marcar todas as ligas");

        for (String n : new String[] {"ut", "My"}) {
            Object o = campo(hU, n);
            if (o instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) o;
                s.setBorder(BorderFactory.createLineBorder(Estreia.LINHA));
                s.getViewport().setBackground(CELULA);
                s.setOpaque(false);
            }
        }
        for (String n : new String[] {"JG", "Mz"}) {
            Object o = campo(hU, n);
            if (o instanceof JTable) {
                JTable t = (JTable) o;
                t.setBackground(CELULA);
                t.setForeground(Estreia.BRANCO);
                t.setGridColor(LINHA_TABELA);
                t.setFont(Estreia.texto(13f));
                if (t.getTableHeader() != null) t.getTableHeader().setFont(Estreia.espacada(Estreia.semi(11f), 0.12f));
            }
        }

        Object combo = campo(hU, "afA");
        if (combo instanceof javax.swing.JComboBox) {
            @SuppressWarnings("unchecked")
            javax.swing.JComboBox<Object> cb = (javax.swing.JComboBox<Object>) combo;
            // so 2026 e 2027 (o jogo grava o ano pelo indice da escolha; ver mod.Ano)
            int escolha = Math.max(0, cb.getSelectedIndex());
            cb.setModel(new javax.swing.DefaultComboBoxModel<Object>(new Object[] {
                String.valueOf(Ano.PRIMEIRA), String.valueOf(Ano.SEGUNDA)}));
            cb.setSelectedIndex(Math.min(escolha, 1));
            cb.setFont(Estreia.semi(13f));
            cb.setBackground(CELULA);
            cb.setForeground(Estreia.BRANCO);
            cb.setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 46)));
        }
        // O titulo maior pede um pouco mais de altura do que os 728 px originais.
        hU.setPreferredSize(null);

        JLabel nota = (JLabel) campo(hU, "ug");
        if (nota != null) {
            nota.setFont(Estreia.texto(12f));
            nota.setForeground(Estreia.FRACO);
        }
        hU.revalidate();
        hU.repaint();
    }

    // Tela "Adicionar tecnico humano" (a.bV). Aqui o tema continua trocando as
    // cores, porque o jogo acende em amarelo a divisao escolhida (vira dourado).
    static void tecnico(final JPanel bV) {
        if (bV.getClientProperty(TECNICO) != null) return;
        bV.putClientProperty(TECNICO, Boolean.TRUE);
        bV.setOpaque(false);
        Container pai = bV.getParent();
        if (pai != null && !(pai instanceof Fundo) && !(pai instanceof javax.swing.JViewport)) {
            pai.remove(bV);
            pai.setLayout(new BorderLayout());
            Fundo f = new Fundo(false);
            f.centro(bV);
            pai.add(f, BorderLayout.CENTER);
            pai.revalidate();
            pai.repaint();
        }

        JLabel kicker = (JLabel) campo(bV, "a_");
        if (kicker != null) {
            kicker.setText("NOVO JOGO");
            kicker.setFont(Estreia.espacada(Estreia.semi(12f), 0.3f));
            kicker.setForeground(Estreia.OURO);
        }
        final JLabel titulo = (JLabel) campo(bV, "vf");
        if (titulo != null) {
            titulo.setFont(Estreia.titulo(44f));
            titulo.setForeground(Estreia.BRANCO);
            titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            maiusculas(titulo);
            // "Adicionar tecnico humano 2", 3...: o jogo troca o texto ao incluir outro
            titulo.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { maiusculas(titulo); }
            });
        }
        Object treinar = campo(bV, "BO");
        if (treinar instanceof JLabel) {
            ((JLabel) treinar).setFont(Estreia.semi(15f));
            ((JLabel) treinar).setForeground(Estreia.OURO);
        }
        for (String n : new String[] {"BH", "BI", "BJ", "BK", "uF", "BL", "BM"}) {
            Object o = campo(bV, n);
            if (o instanceof JLabel) ((JLabel) o).setFont(Estreia.texto(14f));
        }
        Object time = campo(bV, "BN");
        if (time instanceof JLabel) ((JLabel) time).setFont(Estreia.negrito(16f));
        for (String n : new String[] {"ue", "Bz", "BA", "BB", "BC", "BD"}) {
            Object o = campo(bV, n);
            if (o instanceof javax.swing.JComboBox) {
                javax.swing.JComboBox<?> cb = (javax.swing.JComboBox<?>) o;
                cb.setFont(Estreia.semi(13f));
                cb.setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 46)));
            }
        }
        Object nome = campo(bV, "uf");
        if (nome instanceof javax.swing.JTextField) {
            javax.swing.JTextField tf = (javax.swing.JTextField) nome;
            tf.setFont(Estreia.semi(14f));
            tf.setBackground(CELULA);
            tf.setForeground(Estreia.BRANCO);
            tf.setCaretColor(Estreia.OURO);
            tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(244, 241, 234, 60)), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        }
        Object aleatorio = campo(bV, "By");
        if (aleatorio instanceof JCheckBox) {
            JCheckBox c = (JCheckBox) aleatorio;
            c.setUI(new Interruptor());
            c.setFont(Estreia.texto(14f));
            c.setOpaque(false);
        }
        for (String n : new String[] {"BE", "BF", "BG"}) {
            Object o = campo(bV, n);
            if (o instanceof javax.swing.JSeparator) {
                ((javax.swing.JSeparator) o).setForeground(new Color(244, 241, 234, 40));
                ((javax.swing.JSeparator) o).setBackground(new Color(0, 0, 0, 0));
            }
        }
        botao(bV, "Bw", "Iniciar jogo", "seta", true);
        botao(bV, "Bx", "Adicionar e incluir outro", "mais", false);
        // Caixas mais largas (nomes de pais e de time inteiros, sem "...") e a de
        // nacionalidade da mesma altura do campo Nome.
        congela(bV);
        for (String n : new String[] {"ue", "Bz", "BA", "BB", "BC", "BD", "uf", "BE", "BF", "BG"}) {
            Object o = campo(bV, n);
            if (o instanceof Component) ajusta((Component) o, 0, 0, LARGURA_EXTRA, 0);
        }
        Object nac = campo(bV, "BD");
        if (nac instanceof Component && ((Component) nac).getHeight() < 28) ajusta((Component) nac, 0, -4, 0, 8);
        if (aleatorio instanceof Component) ajusta((Component) aleatorio, LARGURA_EXTRA, 0, 0, 0);
        // O formulario cresceu para a direita: o titulo acompanha para seguir centrado.
        for (String n : new String[] {"a_", "vf"}) {
            Object o = campo(bV, n);
            if (o instanceof Component) ajusta((Component) o, LARGURA_EXTRA / 2, 0, 0, 0);
        }

        JLabel nota = (JLabel) campo(bV, "ug");
        if (nota != null) {
            nota.setFont(Estreia.texto(12f));
            nota.setForeground(Estreia.FRACO);
        }
        bV.revalidate();
        bV.repaint();
    }

    static final String TECNICO = "brombila.tecnico";
    static final int LARGURA_EXTRA = 86;

    // Troca o GroupLayout da tela pelo layout absoluto do jogo, mantendo cada
    // componente onde ele estava, para poder alargar as caixas sem desmontar a tela.
    private static void congela(JPanel p) {
        p.setSize(p.getPreferredSize());
        p.doLayout();
        Component[] filhos = p.getComponents();
        java.awt.Rectangle[] r = new java.awt.Rectangle[filhos.length];
        int larg = 0, alt = 0;
        for (int i = 0; i < filhos.length; i++) {
            r[i] = filhos[i].getBounds();
            larg = Math.max(larg, r[i].x + r[i].width);
            alt = Math.max(alt, r[i].y + r[i].height);
        }
        if (!Dialogos.absoluto(p)) return;
        java.awt.LayoutManager2 l = (java.awt.LayoutManager2) p.getLayout();
        for (int i = 0; i < filhos.length; i++)
            l.addLayoutComponent(filhos[i], Dialogos.restricao(r[i].x, r[i].y, r[i].width, r[i].height));
        p.setPreferredSize(new Dimension(larg + LARGURA_EXTRA, alt));
    }

    // Mexe na posicao/tamanho guardados no layout absoluto do jogo (components.b).
    private static void ajusta(Component c, int dx, int dy, int dw, int dh) {
        if (c.getParent() == null) return;
        Object tabela = campo(c.getParent().getLayout(), "NA");
        if (!(tabela instanceof Map)) return;
        Object k = ((Map<?, ?>) tabela).get(c);
        if (k == null) return;
        try {
            Class<?> t = k.getClass();
            int w = t.getField("width").getInt(k), h = t.getField("height").getInt(k);
            if (w < 0) w = c.getPreferredSize().width;
            if (h < 0) h = c.getPreferredSize().height;
            t.getField("x").setInt(k, t.getField("x").getInt(k) + dx);
            t.getField("y").setInt(k, t.getField("y").getInt(k) + dy);
            t.getField("width").setInt(k, w + dw);
            t.getField("height").setInt(k, h + dh);
        } catch (Throwable e) {
            // fica como estava
        }
    }

    private static void maiusculas(JLabel l) {
        String s = l.getText();
        if (s != null && !s.equals(s.toUpperCase())) l.setText(s.toUpperCase());
    }

    private static void secao(JPanel hU, String nome, String texto) {
        Object o = campo(hU, nome);
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setText(texto);
        l.setOpaque(false);
        l.setFont(Estreia.espacada(Estreia.semi(11.5f), 0.2f));
        l.setForeground(Estreia.FRACO);
    }

    private static void botao(JPanel hU, String nome, String texto, String icone, boolean primario) {
        Object o = campo(hU, nome);
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        b.putClientProperty(BotaoUI.PRIMARIO, primario ? Boolean.TRUE : Boolean.FALSE);
        if (texto != null) b.setText(texto);
        b.setFont(Estreia.negrito(13f));
        b.setIcon(new Estreia.Icone(icone, 16, primario ? Estreia.OURO_ESCURO : Estreia.OURO));
        b.setDisabledIcon(new Estreia.Icone(icone, 16, Estreia.APAGADO));
        b.setIconTextGap(8);
        if (primario) b.setHorizontalTextPosition(SwingConstants.LEADING);
        b.setFocusPainted(false);
    }

    // ------------------------------------------------------------- fundo

    static class Fundo extends JPanel {
        private BufferedImage cache;

        // marca = true: a tela usa so as cores proprias (o tema nao mexe nela)
        Fundo(boolean marca) {
            super(new GridBagLayout());
            if (marca) putClientProperty(Estreia.MARCA, Boolean.TRUE);
            setOpaque(true);
        }

        // Tela do jogo no meio do fundo. Em telas baixas (1280x720, 1366x768 com a barra do
        // Windows) ela nao cabe: fica encostada em cima, do tamanho da area, e rola.
        void centro(final JComponent tela) {
            final javax.swing.JScrollPane rola = new javax.swing.JScrollPane(tela);
            rola.setBorder(BorderFactory.createEmptyBorder());
            rola.setViewportBorder(null);
            rola.setOpaque(false);
            rola.getViewport().setOpaque(false);
            rola.getVerticalScrollBar().setUnitIncrement(24);
            rola.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            setLayout(new java.awt.LayoutManager() {
                public void addLayoutComponent(String n, Component c) {}
                public void removeLayoutComponent(Component c) {}
                public Dimension preferredLayoutSize(Container p) { return tela.getPreferredSize(); }
                public Dimension minimumLayoutSize(Container p) { return new Dimension(0, 0); }
                public void layoutContainer(Container p) {
                    Dimension t = tela.getPreferredSize();
                    int w = p.getWidth(), h = p.getHeight();
                    int barra = t.height > h ? rola.getVerticalScrollBar().getPreferredSize().width : 0;
                    int rw = Math.min(t.width + barra, w), rh = Math.min(t.height, h);
                    rola.setBounds((w - rw) / 2, (h - rh) / 2, rw, rh);
                }
            });
            add(rola);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) cache = desenha(w, h);
            g.drawImage(cache, 0, 0, null);
        }

        static BufferedImage desenha(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            BufferedImage f = Estreia.foto();
            double esc = Math.max(w / (double) f.getWidth(), h / (double) f.getHeight()) * 1.06;
            int fw = (int) Math.round(f.getWidth() * esc), fh = (int) Math.round(f.getHeight() * esc);
            g.drawImage(f, (w - fw) / 2, (h - fh) / 2, fw, fh, null);
            g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[] {0f, 1f},
                new Color[] {new Color(6, 9, 8, 224), new Color(6, 9, 8, 247)}));
            g.fillRect(0, 0, w, h);
            float rx = 0.45f * w, ry = 0.35f * h;
            AffineTransform el = new AffineTransform();
            el.translate(0.72f * w, 0.02f * h);
            el.scale(1.0, ry / rx);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), rx, new Point2D.Float(0, 0), new float[] {0f, 1f},
                new Color[] {new Color(242, 193, 78, 30), new Color(242, 193, 78, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, el));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return img;
        }
    }

    // ------------------------------------------- chave liga/desliga (checkbox)

    private static final Map<AbstractButton, float[]> posicoes = new WeakHashMap<AbstractButton, float[]>();
    private static Timer animacao;

    static class Chave implements Icon {
        public int getIconWidth() { return 34; }
        public int getIconHeight() { return 18; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean ativo = m.isEnabled();
            float alvo = m.isSelected() ? 1f : 0f;
            float[] p = posicoes.get(b);
            if (p == null) { p = new float[] {alvo}; posicoes.put(b, p); }
            if (p[0] != alvo) anima();
            float t = Animacao.suave(p[0]);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D trilho = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, 33, 17, 17, 17);
            Color desl = new Color(244, 241, 234, ativo ? (m.isRollover() ? 60 : 40) : 18);
            Color lig = ativo ? Estreia.OURO : Tema.alfa(Estreia.OURO, 90);
            g2.setColor(Tema.mistura(desl, lig, t));
            g2.fill(trilho);
            float kx = x + 2.5f + 16f * t;
            Color bola = Tema.mistura(Estreia.BRANCO, Estreia.OURO_ESCURO, t);
            g2.setColor(ativo ? bola : Tema.alfa(bola, 120));
            g2.fill(new Ellipse2D.Float(kx, y + 2.5f, 13, 13));
            g2.dispose();
        }
    }

    private static void anima() {
        if (animacao == null) {
            animacao = new Timer(Animacao.QUADRO_MS, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean mexendo = false;
                    for (Iterator<Map.Entry<AbstractButton, float[]>> it = posicoes.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<AbstractButton, float[]> en = it.next();
                        AbstractButton b = en.getKey();
                        if (b == null) continue;
                        float alvo = b.isSelected() ? 1f : 0f;
                        float[] p = en.getValue();
                        if (p[0] == alvo) continue;
                        p[0] = p[0] < alvo ? Math.min(alvo, p[0] + 0.09f) : Math.max(alvo, p[0] - 0.09f);
                        b.repaint();
                        mexendo = true;
                    }
                    if (!mexendo) animacao.stop();
                }
            });
        }
        if (!animacao.isRunning()) animacao.start();
    }

    // Caixa de marcar como chave liga/desliga; desativada (jogo nao registrado) fica apagada.
    static class Interruptor extends javax.swing.plaf.basic.BasicCheckBoxUI {
        private final Chave chave = new Chave();

        @Override
        public Dimension getPreferredSize(JComponent c) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = b.getFontMetrics(b.getFont());
            return new Dimension(chave.getIconWidth() + 12 + largura(b) + 6, Math.max(24, fm.getHeight() + 6));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int h = c.getHeight();
            chave.paintIcon(c, g2, 0, (h - chave.getIconHeight()) / 2);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            g2.setFont(b.getFont());
            g2.setColor(!b.isEnabled() ? Estreia.APAGADO : b.getModel().isRollover() ? Color.WHITE : Estreia.BRANCO);
            g2.drawString(b.getText(), chave.getIconWidth() + 12, (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    // Largura do texto como ele e desenhado (com metricas fracionarias). O
    // stringWidth comum mede menos e a ultima letra saia cortada.
    static int largura(AbstractButton b) {
        String t = b.getText();
        if (t == null || t.isEmpty()) return 0;
        java.awt.font.FontRenderContext frc = new java.awt.font.FontRenderContext(null, true, true);
        return (int) Math.ceil(b.getFont().getStringBounds(t, frc).getWidth());
    }

    // -------------------------------------------- opcoes (radio) em pilula

    static class Pilula extends BasicRadioButtonUI {
        @Override
        public Dimension getPreferredSize(JComponent c) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = b.getFontMetrics(b.getFont());
            return new Dimension(largura(b) + 32, Math.max(30, fm.getHeight() + 14));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            String s = b.getText();
            int tw = fm.stringWidth(s);
            int h = Math.min(c.getHeight() - 2, 30), pw = tw + 30;
            int y = (c.getHeight() - h) / 2;
            RoundRectangle2D pil = new RoundRectangle2D.Float(0.5f, y + 0.5f, pw - 1, h - 1, h, h);
            Color fg;
            if (m.isSelected()) {
                g2.setColor(m.isEnabled() ? Estreia.BRANCO : Tema.alfa(Estreia.BRANCO, 90));
                g2.fill(pil);
                fg = new Color(0x121411);
            } else {
                g2.setColor(new Color(244, 241, 234, m.isRollover() && m.isEnabled() ? 110 : 46));
                g2.draw(pil);
                fg = !m.isEnabled() ? Estreia.APAGADO : m.isRollover() ? Estreia.BRANCO : Estreia.FRACO;
            }
            g2.setFont(b.getFont());
            g2.setColor(fg);
            g2.drawString(s, 15, y + (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }
}
