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
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Method;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

// Janelas Tabelas (a.ay) e Times (a.iA) no estilo "Noite de estreia".
final class Telas {
    private Telas() {}

    static final String NAO_DEFINIDO = "(ainda não definido)";

    private static void fonte(Object o, java.awt.Font f, Color cor) {
        if (!(o instanceof JComponent)) return;
        ((JComponent) o).setFont(f);
        if (cor != null) ((JComponent) o).setForeground(cor);
    }

    private static void cartao(Object o) {
        if (!(o instanceof JPanel)) return;
        ((JPanel) o).setOpaque(false);
        ((JPanel) o).setBorder(new Editor.Cartao(true));
    }

    // ------------------------------------------------------------- Tabelas

    static void tabelas(final JPanel ay) {
        if (ay.getClientProperty(Janelas.FEITO) != null) return;
        ay.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        ay.setOpaque(false);
        ay.setBorder(new EmptyBorder(0, 0, 0, 0));
        Janelas.embrulha(ay);

        // faixa de cima: abas (nacional, internacional...), ano e X redondo
        Object topo = campo(ay, "vd");
        if (topo instanceof JComponent) {
            ((JComponent) topo).setOpaque(false);
            ((JComponent) topo).setBorder(new Filete());
        }
        Object barra = campo(ay, "ve");
        if (barra instanceof JComponent) ((JComponent) barra).setOpaque(false);
        Object ano = campo(ay, "xN");
        if (ano instanceof JLabel) {
            fonte(ano, Estreia.titulo(26f), Estreia.OURO);
            Janelas.posicao((JLabel) ano, 640, 6, 100, 36);
        }
        Object x = campo(ay, "vm");
        Janelas.fechar(x);
        Janelas.posicao((Component) x, 762, 6, 36, 36);

        // artilheiros, notas e estatisticas: cartoes com titulo em rotulo dourado
        for (String p : new String[] {"yd", "yf", "yh"}) cartao(campo(ay, p));
        titulo(campo(ay, "us"), "ARTILHEIROS");
        titulo(campo(ay, "uh"), "MELHORES NOTAS");
        titulo(campo(ay, "ur"), "ESTATÍSTICAS E HISTÓRIA");
        for (String c : new String[] {"vy", "vx", "vz", "vA"}) {
            Object o = campo(ay, c);
            if (o instanceof JLabel) {
                ((JLabel) o).setOpaque(false);
                fonte(o, Estreia.semi(11f), Estreia.FRACO);
            }
        }
        lista(ay, new String[] {"xj", "xk", "xl", "xm", "xn"});
        lista(ay, new String[] {"xO", "xP", "xQ", "xR", "xS"});
        for (String v : new String[] {"xo", "xp", "xq", "xr", "xs", "xY", "xZ", "ya", "yb", "yc"}) {
            fonte(campo(ay, v), Estreia.negrito(12.5f), Estreia.BRANCO);
        }
        for (String v : new String[] {"xt", "xu", "xv", "xw", "xx", "xT", "xU", "xV", "xW", "xX"}) {
            fonte(campo(ay, v), Estreia.texto(12f), Estreia.FRACO);
        }
        for (String k : new String[] {"xK", "xM", "xF", "xL"}) fonte(campo(ay, k), Estreia.texto(12f), Estreia.FRACO);
        for (String v : new String[] {"xE", "xA", "xG", "xB", "xC"}) fonte(campo(ay, v), Estreia.semi(12.5f), Estreia.BRANCO);
        fonte(campo(ay, "xD"), Estreia.semi(12f), Estreia.OURO);

        // abas de baixo (Estatisticas / Rodadas): pilulas; a escolhida e a que esta aberta
        final Object estat = campo(ay, "ye"), rodadas = campo(ay, "yg");
        aba(campo(ay, "xI"), estat);
        aba(campo(ay, "xH"), rodadas);
        posicoes(campo(ay, "xb"));
        taca(ay);
        ay.revalidate();
        ay.repaint();
    }

    // Times empatados: o jogo deixa a posicao em branco; aqui cada linha mostra o numero dela
    // dentro do grupo (a contagem recomeca a cada titulo de grupo).
    private static void posicoes(Object o) {
        if (!(o instanceof JPanel)) return;
        final JPanel xb = (JPanel) o;
        final Runnable numera = new Runnable() {
            public void run() {
                int n = 0;
                for (Component c : xb.getComponents()) {
                    if (!(c instanceof components.au)) { n = 0; continue; }
                    n++;
                    Object sg = campo(c, "Sg");
                    if (sg instanceof JLabel && ((JLabel) sg).getText() != null && ((JLabel) sg).getText().trim().isEmpty())
                        ((JLabel) sg).setText(Integer.toString(n));
                }
            }
        };
        numera.run();
        xb.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent e) { numera.run(); }
        });
    }

    // Taca do "Maior vencedor": a do jogo e reduzida sem suavizacao (fica borrada); aqui entra a
    // nossa, do mesmo tipo da competicao.
    private static void taca(final JPanel ay) {
        Object o = campo(ay, "xJ");
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        final Runnable troca = new Runnable() {
            public void run() {
                if (l.getIcon() == null || l.getClientProperty("brombila.taca") == l.getIcon()) return;
                String nome = "";
                try {
                    Object at = campo(ay, "wU");
                    nome = ((String[]) at.getClass().getMethod("mA").invoke(at))[0];
                } catch (Throwable t) { /* modelo de liga */ }
                Icon i = Campeao.miniatura(nome, 55);
                if (i == null) return;
                l.putClientProperty("brombila.taca", i);
                l.setIcon(i);
            }
        };
        troca.run();
        l.addPropertyChangeListener("icon", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) { troca.run(); }
        });
    }

    private static void titulo(Object o, String texto) {
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setOpaque(false);
        l.setText(texto);
        l.setFont(Estreia.espacada(Estreia.negrito(11f), 0.18f));
        l.setForeground(Estreia.OURO);
        l.setBorder(new EmptyBorder(4, 12, 0, 0));
    }

    // Lista de 5 linhas: "(ainda nao definido)" aparece so uma vez, discreto.
    private static void lista(Object dono, String[] nomes) {
        for (int i = 0; i < nomes.length; i++) {
            Object o = campo(dono, nomes[i]);
            if (!(o instanceof JLabel)) continue;
            final JLabel l = (JLabel) o;
            final boolean primeira = i == 0;
            l.setFont(Estreia.semi(12.5f));
            final Runnable ajusta = new Runnable() {
                public void run() {
                    String t = l.getText();
                    if (NAO_DEFINIDO.equals(t)) {
                        if (primeira) l.setText("Sem dados ainda");
                        else l.setText("");
                    }
                    Color c = (primeira && "Sem dados ainda".equals(l.getText())) ? Estreia.APAGADO : Estreia.BRANCO;
                    if (!c.equals(l.getForeground())) l.setForeground(c);
                }
            };
            ajusta.run();
            l.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { ajusta.run(); }
            });
        }
    }

    private static void aba(Object o, final Object painel) {
        if (!(o instanceof JLabel) || !(painel instanceof Component)) return;
        JLabel l = (JLabel) o;
        l.setOpaque(false);
        l.setFont(Estreia.negrito(12.5f));
        l.setUI(new Aba((Component) painel));
        l.setHorizontalAlignment(JLabel.CENTER);
        l.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    }

    // Aba-pilula: dourada quando o painel dela esta aberto, contorno quando nao.
    static class Aba extends BasicLabelUI {
        final Component painel;
        Aba(Component painel) { this.painel = painel; }
        boolean escolhida() { return painel.isVisible(); }
        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            v = Animacao.valor(c, "brombila.aba", escolhida() ? 1f : 0f, 220, Float.NaN);
            pilula(g, 0, 0, c.getWidth(), c.getHeight(), v);
            g.dispose();
            super.paint(g0, c);
        }
        private float v;
        @Override
        protected void paintEnabledText(JLabel l, Graphics g, String s, int x, int y) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Tema.mistura(Estreia.BRANCO, Estreia.OURO_ESCURO, v));
            g.drawString(s, x, y);
        }
    }

    // Aba-pilula com v de 0 (contorno) a 1 (dourada). No meio da troca, o dourado cresce do
    // centro para as bordas enquanto o contorno some.
    static void pilula(Graphics2D g, float x, float y, float w, float h, float v) {
        RoundRectangle2D r = new RoundRectangle2D.Float(x + 1, y + 1, w - 3, h - 3, h - 3, h - 3);
        if (v < 1f) {
            g.setColor(new Color(244, 241, 234, Math.round(14 * (1f - v))));
            g.fill(r);
            g.setColor(new Color(244, 241, 234, Math.round(60 * (1f - v))));
            g.draw(r);
        }
        if (v > 0f) {
            float lw = (w - 3) * (0.55f + 0.45f * v);
            g.setColor(Tema.alfa(Estreia.OURO, Math.round(255 * Math.min(1f, v * 1.6f))));
            g.fill(new RoundRectangle2D.Float(x + 1 + (w - 3 - lw) / 2f, y + 1, lw, h - 3, h - 3, h - 3));
        }
    }

    // Linha fina embaixo da faixa de cima.
    static class Filete extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        @Override
        public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 0, 0); return i; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(new Color(244, 241, 234, 34));
            g.fillRect(x, y + h - 1, w, 1);
        }
    }

    // ------------------------------------------------------------- Times

    static void times(final JPanel iA) {
        if (iA.getClientProperty(Janelas.FEITO) != null) return;
        iA.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        iA.setOpaque(false);
        iA.setBorder(new EmptyBorder(0, 0, 0, 0));
        Janelas.embrulha(iA);

        for (String p : new String[] {"we", "wf", "wg", "wh"}) cartao(campo(iA, p));
        Object barra = campo(iA, "ve");
        if (barra instanceof JComponent) ((JComponent) barra).setOpaque(false);

        // cabecalho do time, como no Editor
        Editor.cor(campo(iA, "uu"), Estreia.titulo(26f), Estreia.BRANCO);
        Editor.compacto(campo(iA, "uu"), 30);
        Editor.cor(campo(iA, "MR"), Estreia.espacada(Estreia.semi(11f), 0.2f), Estreia.OURO);
        for (String l : new String[] {"MS", "MQ", "MM"}) Editor.cor(campo(iA, l), Estreia.semi(13f), Estreia.BRANCO);
        Editor.cor(campo(iA, "MT"), Estreia.texto(12.5f), Estreia.FRACO);

        // confianca
        Object dir = campo(iA, "a_"), tor = campo(iA, "ur");
        if (dir instanceof JLabel) ((JLabel) dir).setText("Diretoria");
        if (tor instanceof JLabel) ((JLabel) tor).setText("Torcida");
        fonte(dir, Estreia.semi(12.5f), Estreia.BRANCO);
        fonte(tor, Estreia.semi(12.5f), Estreia.BRANCO);

        // jogador escolhido: nome e abas Info / Carreira
        fonte(campo(iA, "MO"), Estreia.negrito(13.5f), Estreia.BRANCO);
        Object abas = campo(iA, "ML");
        if (abas instanceof JTabbedPane) {
            JTabbedPane t = (JTabbedPane) abas;
            t.setUI(new AbasUI());
            t.setOpaque(false);
            t.setFont(Estreia.negrito(12.5f));
        }
        Object info = campo(iA, "MN");
        if (info instanceof JLabel) {
            ((JLabel) info).setOpaque(false);
            ((JLabel) info).setForeground(Estreia.BRANCO);
            ((JLabel) info).setFont(Estreia.texto(13f));
            ((JLabel) info).setBorder(new EmptyBorder(10, 12, 10, 12));
        }
        for (String s : new String[] {"zf", "zg"}) {
            Object o = campo(iA, s);
            if (o instanceof JScrollPane) {
                ((JScrollPane) o).setOpaque(false);
                ((JScrollPane) o).getViewport().setOpaque(false);
                ((JScrollPane) o).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
        }

        // arvore de ligas
        Object arvore = campo(iA, "uw");
        if (arvore instanceof JTree) arvore((JTree) arvore);
        Object rolArvore = campo(iA, "wi");
        if (rolArvore instanceof JScrollPane) Editor.rolagem((JScrollPane) rolArvore);
        Object rolElenco = campo(iA, "ut");
        if (rolElenco instanceof JScrollPane) Editor.rolagem((JScrollPane) rolElenco);

        Editor.cor(campo(iA, "MP"), Estreia.negrito(14f), Estreia.OURO);
        Editor.botao(campo(iA, "MJ"), null, "volta", false);
        Editor.botao(campo(iA, "FN"), null, "mais", true);
        Editor.botao(campo(iA, "MK"), null, "etiqueta", false);
        Editor.botao(campo(iA, "vm"), null, "fechar", false);
        iA.revalidate();
        iA.repaint();
    }

    static void arvore(final JTree t) {
        t.setOpaque(true);
        t.setBackground(ConfigEstreia.CELULA);
        t.setRowHeight(24);
        t.setFont(Estreia.semi(12.5f));
        if (t.getUI() instanceof BasicTreeUI) {
            BasicTreeUI ui = (BasicTreeUI) t.getUI();
            ui.setExpandedIcon(new Chevron(true));
            ui.setCollapsedIcon(new Chevron(false));
            ui.setLeftChildIndent(8);
            ui.setRightChildIndent(14);
        }
        t.putClientProperty("JTree.lineStyle", "None");
        t.setCellRenderer(new Ramos());
    }

    // Ligas e divisoes em texto, times com o escudo do clube.
    static class Ramos extends DefaultTreeCellRenderer {
        private final java.util.Map<Object, Icon> escudos = new java.util.WeakHashMap<Object, Icon>();
        Ramos() {
            setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
            setBackgroundSelectionColor(ConfigEstreia.SELECAO);
            setBorderSelectionColor(new Color(0, 0, 0, 0));
            setTextNonSelectionColor(Estreia.BRANCO);
            setTextSelectionColor(Estreia.BRANCO);
            setOpaque(false);
        }
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object valor, boolean sel, boolean aberto,
                                                      boolean folha, int linha, boolean foco) {
            super.getTreeCellRendererComponent(tree, valor, sel, aberto, folha, linha, false);
            Object u = (valor instanceof DefaultMutableTreeNode) ? ((DefaultMutableTreeNode) valor).getUserObject() : null;
            int nivel = (valor instanceof DefaultMutableTreeNode) ? ((DefaultMutableTreeNode) valor).getLevel() : 0;
            setIconTextGap(7);
            setBorder(new EmptyBorder(0, 4, 0, 4));
            if (u != null && !(u instanceof String)) {
                setFont(Estreia.texto(12.5f));
                setForeground(Estreia.BRANCO);
                setIcon(escudo(u));
            } else if (nivel <= 1) {
                setFont(Estreia.espacada(Estreia.negrito(11.5f), 0.12f));
                setForeground(Estreia.OURO);
                setText(getText() == null ? "" : getText().toUpperCase());
                setIcon(null);
            } else {
                setFont(Estreia.semi(12.5f));
                setForeground(sel ? Estreia.BRANCO : new Color(0xE6E2DA));
                setIcon(new Estreia.Icone("todos", 14, Estreia.FRACO));
            }
            return this;
        }
        private Icon escudo(Object time) {
            Icon i = escudos.get(time);
            if (i != null) return i;
            try {
                Method m = time.getClass().getMethod("x", int.class, int.class);
                Object r = m.invoke(time, 18, 18);
                if (r instanceof Icon) i = (Icon) r;
            } catch (Throwable t) {
                // sem escudo
            }
            if (i == null) i = new ImageIcon(new java.awt.image.BufferedImage(18, 18, java.awt.image.BufferedImage.TYPE_INT_ARGB));
            escudos.put(time, i);
            return i;
        }
        @Override
        public void paintComponent(Graphics g0) {
            if (selected) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(ConfigEstreia.SELECAO);
                g.fill(new RoundRectangle2D.Float(0, 1, getWidth() - 1, getHeight() - 2, 8, 8));
                g.dispose();
            }
            boolean s = selected;
            selected = false; // o fundo ja foi pintado acima
            super.paintComponent(g0);
            selected = s;
        }
    }

    // Seta fina de abrir/fechar da arvore.
    static class Chevron implements Icon {
        final boolean aberto;
        Chevron(boolean aberto) { this.aberto = aberto; }
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 12; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Estreia.OURO);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            if (aberto) { p.moveTo(x + 2.5, y + 4.5); p.lineTo(x + 6, y + 8); p.lineTo(x + 9.5, y + 4.5); }
            else { p.moveTo(x + 4.5, y + 2.5); p.lineTo(x + 8, y + 6); p.lineTo(x + 4.5, y + 9.5); }
            g.draw(p);
            g.dispose();
        }
    }

    // Abas Info / Carreira como pilulas, sem a moldura grossa.
    static class AbasUI extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(5, 16, 5, 16);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(2, 2, 6, 2);
            contentBorderInsets = new Insets(1, 1, 1, 1);
        }
        @Override
        protected int calculateTabHeight(int pos, int i, int fontHeight) { return fontHeight + 12; }
        // Todas as abas em contorno; a pilula dourada e desenhada uma vez so e desliza ate a
        // aba escolhida (x e largura animados).
        @Override
        protected void paintTabArea(Graphics g0, int pos, int sel) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < tabPane.getTabCount() && i < rects.length; i++) {
                Rectangle r = rects[i];
                pilula(g, r.x + 1, r.y, r.width - 2, r.height + 1, 0f);
            }
            if (sel >= 0 && sel < rects.length) {
                Rectangle r = rects[sel];
                float x = Animacao.valor(tabPane, "brombila.abaX", r.x, 260, Float.NaN);
                float w = Animacao.valor(tabPane, "brombila.abaW", r.width, 260, Float.NaN);
                pilula(g, x + 1, r.y, w - 2, r.height + 1, 1f);
                dourada = new Rectangle(Math.round(x), r.y, Math.round(w), r.height);
            }
            g.dispose();
            super.paintTabArea(g0, pos, sel);
        }
        private Rectangle dourada;
        @Override
        protected void paintTabBackground(Graphics g0, int pos, int i, int x, int y, int w, int h, boolean sel) {}
        @Override
        protected void paintTabBorder(Graphics g, int pos, int i, int x, int y, int w, int h, boolean sel) {}
        @Override
        protected void paintFocusIndicator(Graphics g, int pos, Rectangle[] r, int i, Rectangle ir, Rectangle tr, boolean sel) {}
        @Override
        protected void paintText(Graphics g0, int pos, java.awt.Font f, java.awt.FontMetrics fm, int i, String s,
                                 Rectangle r, boolean sel) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(f);
            // texto escuro na parte que a pilula dourada cobre
            Rectangle aba = i < rects.length ? rects[i] : r;
            float cobre = 0f;
            if (dourada != null) {
                int a0 = Math.max(aba.x, dourada.x), a1 = Math.min(aba.x + aba.width, dourada.x + dourada.width);
                cobre = Math.max(0f, Math.min(1f, (a1 - a0) / (float) Math.max(1, aba.width)));
            }
            g.setColor(Tema.mistura(Estreia.BRANCO, Estreia.OURO_ESCURO, cobre));
            g.drawString(s, r.x, r.y + fm.getAscent());
        }
        @Override
        protected void paintContentBorder(Graphics g0, int pos, int i) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle b = tabPane.getBounds();
            int top = calculateTabAreaHeight(pos, runCount, maxTabHeight);
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, top + 0.5f, b.width - 1, b.height - top - 1, 12, 12);
            g.setColor(ConfigEstreia.CELULA);
            g.fill(r);
            g.setColor(new Color(244, 241, 234, 36));
            g.draw(r);
            g.dispose();
        }
    }
}
