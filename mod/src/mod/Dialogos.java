package mod;

import static mod.Reflexao.campo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicLabelUI;

// Janelas abertas pelos menus Agenda, Tecnicos, Historia e Opcoes, e a Procura, no estilo
// "Noite de estreia": cartao escuro com filete dourado, titulo grande (nome do time em
// dourado), X redondo e ajustes de cada janela. As janelas feitas com GroupLayout passam
// para o layout absoluto do jogo (components.b), com as posicoes escolhidas aqui.
final class Dialogos {
    private Dialogos() {}

    static void aplica(Component c) {
        if (!(c instanceof JPanel)) return;
        JPanel p = (JPanel) c;
        String n = c.getClass().getName();
        if (n.equals("a.L")) calendario(p);
        else if (n.equals("a.gE")) empregos(p);
        else if (n.equals("a.gX")) ranking(p);
        else if (n.equals("a.R")) campeoes(p);
        else if (n.equals("a.o")) artilheiros(p);
        else if (n.equals("a.di")) galeria(p);
        else if (n.equals("a.fB")) opcoes(p);
        else if (n.equals("a.gx")) procura(p);
        else if (n.equals("a.jp")) sobre(p);
        else if (n.equals("a.C") || n.equals("a.gh")) batedores(p);
        else if (n.equals("a.cD")) Intervalo.aplica(p);
        else if (!Outras.aplica(p, n) && !Temporada.aplica(p, n)) Fichas.aplica(p, n);
    }

    // Penaltis: ordem dos batedores da disputa (a.C) e escolha do batedor na partida (a.gh).
    // O tema ja cuida das cores; aqui so os titulos em portugues certo.
    static void batedores(JPanel p) {
        Object t = campo(p, "vf");
        if (!(t instanceof javax.swing.JLabel)) return;
        javax.swing.JLabel l = (javax.swing.JLabel) t;
        String s = l.getText();
        if (s == null) return;
        if (s.startsWith("Disputa penalties")) l.setText("Disputa de pênaltis · ordem dos batedores");
        else if (s.startsWith("Penalty - escolha")) l.setText("Pênalti · escolha o batedor");
    }

    // ------------------------------------------------------------- pecas comuns

    static Object restricao(int x, int y, int w, int h) {
        try {
            return Class.forName("components.a").getConstructor(int.class, int.class, int.class, int.class)
                .newInstance(x, y, w, h);
        } catch (Throwable t) {
            return null;
        }
    }

    // Troca o layout do painel pelo absoluto do jogo; cada filho precisa de posicao depois.
    static boolean absoluto(Container c) {
        try {
            c.setLayout((LayoutManager) Class.forName("components.b").newInstance());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static void adiciona(Container pai, Component c, int x, int y, int w, int h) {
        Object k = restricao(x, y, w, h);
        if (k != null) pai.add(c, k, 0);
    }

    // Componente novo atras dos outros (pintado primeiro).
    static void fundo(Container pai, Component c, int x, int y, int w, int h) {
        Object k = restricao(x, y, w, h);
        if (k != null) pai.add(c, k);
    }

    static void pos(Object o, int x, int y, int w, int h) {
        if (o instanceof Component) Janelas.posicao((Component) o, x, y, w, h);
    }

    // Largura natural (-1) para um componente ja posicionado no layout absoluto.
    static void larguraLivre(Component c) {
        if (c.getParent() == null) return;
        Object l = c.getParent().getLayout();
        Object tabela = campo(l, "NA");
        if (!(tabela instanceof java.util.Map)) return;
        Object k = ((java.util.Map<?, ?>) tabela).get(c);
        if (k == null) return;
        try {
            Field f = k.getClass().getField("width");
            f.setInt(k, -1);
        } catch (Throwable t) {
            // fica como estava
        }
    }

    static void moveX(Component c, int x) {
        if (c.getParent() == null) return;
        Object tabela = campo(c.getParent().getLayout(), "NA");
        if (!(tabela instanceof java.util.Map)) return;
        Object k = ((java.util.Map<?, ?>) tabela).get(c);
        if (k == null) return;
        try {
            k.getClass().getField("x").setInt(k, x);
        } catch (Throwable t) {
            // fica como estava
        }
    }

    static boolean base(JPanel p) {
        if (p.getClientProperty(Janelas.FEITO) != null) return false;
        p.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        Janelas.embrulha(p);
        return true;
    }

    static void titulo(Object o, float px) {
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setFont(Estreia.titulo(px));
        l.setForeground(Estreia.BRANCO);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        l.setIconTextGap(10);
        l.setUI(new DuasCores());
    }

    static JLabel rotulo(String texto) {
        JLabel l = new JLabel(texto);
        rotulo(l);
        return l;
    }

    static void rotulo(Object o) {
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.18f));
        l.setForeground(Estreia.OURO);
        l.setOpaque(false);
        String t = l.getText();
        if (t != null) {
            t = t.trim();
            if (t.endsWith(":")) t = t.substring(0, t.length() - 1);
            l.setText(t.toUpperCase());
        }
    }

    static void fonte(Object o, java.awt.Font f, Color cor) {
        if (!(o instanceof JComponent)) return;
        ((JComponent) o).setFont(f);
        if (cor != null) ((JComponent) o).setForeground(cor);
    }

    static void cartao(Object o) {
        if (!(o instanceof JComponent)) return;
        ((JComponent) o).setOpaque(false);
        ((JComponent) o).setBorder(new Editor.Cartao(true));
    }

    static void rolagem(Object o) {
        if (o instanceof JScrollPane) ((JScrollPane) o).setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 30)));
    }

    // Lista vazia: mensagem discreta no meio, em vez de um quadro preto mudo.
    static void vazio(Object rolagem, String titulo, String texto) {
        if (!(rolagem instanceof JScrollPane)) return;
        Component v = ((JScrollPane) rolagem).getViewport().getView();
        if (!(v instanceof JTable)) return;
        JTable t = (JTable) v;
        t.setFillsViewportHeight(true);
        t.setBorder(new Vazio(titulo, texto));
    }

    static void interruptor(Object o) {
        if (!(o instanceof JCheckBox)) return;
        JCheckBox c = (JCheckBox) o;
        c.setUI(new ConfigEstreia.Interruptor());
        c.setOpaque(false);
        c.setFont(Estreia.texto(12.5f));
        c.setForeground(Estreia.BRANCO);
        c.setFocusPainted(false);
    }

    static void fechar(Object x, int largura) {
        Janelas.fechar(x);
        pos(x, largura - 56, 16, 36, 36);
    }

    // ------------------------------------------------------------- Calendario (Agenda)

    static void calendario(JPanel l) {
        if (!base(l) || !absoluto(l)) return;
        Object t = campo(l, "vf");
        titulo(t, 28f);
        pos(t, 30, 16, 470, 40);
        pos(campo(l, "va"), 540, 22, 200, 28);
        fechar(campo(l, "vm"), 820);
        Object s = campo(l, "ut");
        rolagem(s);
        pos(s, 30, 70, 760, 505);
        vazio(s, "Sem jogos marcados", "Os jogos do time aparecem aqui quando a temporada tiver datas.");
        final Object time = campo(l, "uk");
        final Object tab = campo(l, "vN");
        if (tab instanceof JTable) {
            final JTable t2 = (JTable) tab;
            Placar.instala(t2, time);
            t2.addPropertyChangeListener("model", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { Placar.instala(t2, time); } });
                }
            });
        }
        l.revalidate();
    }

    // Placar do Calendario: chip V/E/D (verde, cinza, vermelho) do ponto de vista do time
    // e o placar; nos jogos decididos nos penaltis, o placar dos penaltis embaixo.
    static class Placar extends JComponent implements javax.swing.table.TableCellRenderer {
        static final Color VITORIA = new Color(0x7CCB7A), EMPATE = new Color(0xB9B4A8), DERROTA = new Color(0xFF8A6E);
        private final javax.swing.table.TableCellRenderer orig;
        private final Object time;
        private String letra, placar, penaltis;
        private Color cor;

        private Placar(javax.swing.table.TableCellRenderer orig, Object time) {
            this.orig = orig;
            this.time = time;
        }

        static void instala(JTable t, Object time) {
            javax.swing.table.TableCellRenderer r = t.getDefaultRenderer(best.I.class);
            if (r == null || r instanceof Placar) return;
            t.setDefaultRenderer(best.I.class, new Placar(r, time));
            int[] larg = {84, 130, 96, 130, 178, 68, 74};
            for (int i = 0; i < larg.length && i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
        }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
            int m = t.convertColumnIndexToModel(col);
            if (m == 4 && c instanceof JLabel) {
                // competicoes alinhadas a esquerda, uma embaixo da outra
                ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                ((JLabel) c).setBorder(new EmptyBorder(0, 14, 0, 4));
            }
            if (m != 2 || !(v instanceof best.I)) return c;
            best.I j = (best.I) v;
            if (!j.e()) {
                letra = null;
                placar = "x";
                penaltis = null;
            } else {
                boolean casa = j.hc() == time;
                int pro = casa ? j.hu() : j.hw(), contra = casa ? j.hw() : j.hu();
                int[] pen = j.hQ();
                boolean temPen = pen != null && pen.length > 1 && pen[0] >= 0 && pen[1] >= 0;
                placar = j.hu() + " x " + j.hw();
                penaltis = temPen ? "pên " + pen[0] + "-" + pen[1] : null;
                if (pro == contra && temPen) {
                    // decidido nos penaltis: conta quem venceu a disputa
                    pro = casa ? pen[0] : pen[1];
                    contra = casa ? pen[1] : pen[0];
                }
                if (j.hc() != time && j.hd() != time) {
                    letra = null;
                } else if (pro > contra) {
                    letra = "V";
                    cor = VITORIA;
                } else if (pro < contra) {
                    letra = "D";
                    cor = DERROTA;
                } else {
                    letra = "E";
                    cor = EMPATE;
                }
            }
            setBackground(c.getBackground());
            setOpaque(c instanceof JComponent && ((JComponent) c).isOpaque());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = getWidth(), h = getHeight();
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, w, h);
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Font fp = Estreia.negrito(13f), fl = Estreia.negrito(11f), fs = Estreia.semi(9.5f);
            FontMetrics mp = g.getFontMetrics(fp), ml = g.getFontMetrics(fl), ms = g.getFontMetrics(fs);
            // chip sempre na mesma coluna; o placar centralizado no espaco depois dele
            int x = 10, xp0 = x + 20 + 6, area = w - xp0 - 6;
            int yPlacar = penaltis == null ? (h - mp.getHeight()) / 2 + mp.getAscent() : h / 2 + 1;
            if (letra != null) {
                RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, (h - 20) / 2f, 20, 20, 8, 8);
                g.setColor(Tema.alfa(cor, 46));
                g.fill(r);
                g.setColor(Tema.alfa(cor, 170));
                g.draw(r);
                g.setFont(fl);
                g.setColor(cor);
                g.drawString(letra, x + (20 - ml.stringWidth(letra)) / 2f + 0.5f, (h - ml.getHeight()) / 2f + ml.getAscent());
            }
            g.setFont(fp);
            g.setColor(letra == null ? Estreia.FRACO : Estreia.BRANCO);
            int xp = xp0 + (area - mp.stringWidth(placar)) / 2;
            g.drawString(placar, xp, yPlacar);
            if (penaltis != null) {
                g.setFont(fs);
                g.setColor(Estreia.FRACO);
                g.drawString(penaltis, xp + (mp.stringWidth(placar) - ms.stringWidth(penaltis)) / 2f, yPlacar + ms.getAscent() + 1);
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Central de Empregos

    static void empregos(JPanel ge) {
        if (!base(ge)) return;
        Object o = campo(ge, "vd");
        if (!(o instanceof JPanel) || !absoluto(ge)) return;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(new EmptyBorder(0, 0, 0, 0));
        pos(vd, 0, 0, 711, 670);
        if (!absoluto(vd)) return;

        Object t = campo(ge, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Central de empregos");
        titulo(t, 28f);
        pos(t, 28, 16, 520, 40);
        Object sub = campo(ge, "uu");
        if (sub instanceof JLabel) {
            ((JLabel) sub).setText("Times e seleções que procuram um técnico. Escolha um e assuma o cargo.");
            ((JLabel) sub).setHorizontalAlignment(SwingConstants.LEFT);
        }
        fonte(sub, Estreia.texto(12.5f), Estreia.FRACO);
        pos(sub, 28, 54, 600, 20);
        fechar(campo(ge, "Kj"), 711);

        // abas Times / Selecoes
        Object abas = campo(ge, "Iv");
        if (abas instanceof JToolBar) {
            JToolBar tb = (JToolBar) abas;
            tb.setOpaque(false);
            tb.setBorder(new EmptyBorder(0, 0, 0, 0));
            tb.setLayout(new GridLayout(1, 0, 8, 0));
            for (String n : new String[] {"Ib", "uZ"}) {
                Object b = campo(ge, n);
                if (b instanceof JButton) ((JButton) b).setUI(new AbaBotao());
            }
        }
        pos(abas, 28, 88, 228, 32);

        Object lista = campo(ge, "zf");
        rolagem(lista);
        pos(lista, 28, 134, 430, 508);
        vazio(lista, "Nenhuma vaga aberta", "Quando um time demitir o técnico, ele aparece aqui.");

        // cartao do time escolhido: nome grande, atalhos e o botao de assumir
        JPanel c = new JPanel(null);
        cartao(c);
        fundo(vd, c, 474, 134, 209, 508);
        adiciona(vd, rotulo("Time escolhido"), 490, 150, 180, 16);
        Object nome = campo(ge, "zP");
        fonte(nome, Estreia.titulo(22f), Estreia.BRANCO);
        if (nome instanceof JLabel) ((JLabel) nome).setIconTextGap(8);
        pos(nome, 490, 170, 180, 40);
        adiciona(vd, rotulo("Ver do time"), 490, 230, 180, 16);
        Object atalhos = campo(ge, "ve");
        if (atalhos instanceof JToolBar) {
            JToolBar tb = (JToolBar) atalhos;
            tb.setOpaque(false);
            tb.setFloatable(false);
            tb.setBorder(new EmptyBorder(0, 0, 0, 0));
            tb.setLayout(new GridLayout(0, 1, 0, 8));
            atalho(campo(ge, "HR"), "Calendário", "calendario");
            atalho(campo(ge, "HY"), "Retrospecto", "historico");
            atalho(campo(ge, "aeN"), "Tabelas", "todos");
        }
        pos(atalhos, 490, 252, 177, 118);
        Object assumir = campo(ge, "aeM");
        Editor.botao(assumir, null, "maleta", true);
        pos(assumir, 490, 592, 177, 36);
        pos(campo(ge, "zh"), 490, 560, 177, 0);
        ge.revalidate();
    }

    static void atalho(Object o, String texto, String icone) {
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        b.setText(texto);
        b.setToolTipText(null);
        b.putClientProperty(BotaoUI.PRIMARIO, Boolean.FALSE);
        b.setContentAreaFilled(true);
        b.setFont(Estreia.negrito(12.5f));
        b.setIcon(new Estreia.Icone(icone, 16, Estreia.OURO));
        b.setRolloverIcon(null);
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setVerticalTextPosition(SwingConstants.CENTER);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setIconTextGap(9);
        b.setMargin(new Insets(0, 14, 0, 8));
        b.setBorder(new EmptyBorder(0, 14, 0, 8));
        b.setMaximumSize(new Dimension(177, 34));
        b.setPreferredSize(new Dimension(177, 34));
    }

    // Aba-pilula para botoes: a escolhida e a que o jogo deixa com a fonte maior (14 x 12).
    static class AbaBotao extends BasicButtonUI {
        @Override
        public Dimension getPreferredSize(JComponent c) { return new Dimension(110, 30); }
        @Override
        public void paint(Graphics g0, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            boolean on = b.getFont().getSize2D() >= 13.5f;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, h - 3, h - 3);
            if (on) {
                g.setColor(Estreia.OURO);
                g.fill(r);
            } else {
                g.setColor(new Color(244, 241, 234, b.getModel().isRollover() ? 26 : 14));
                g.fill(r);
                g.setColor(new Color(244, 241, 234, 60));
                g.draw(r);
            }
            java.awt.Font f = Estreia.negrito(12.5f);
            FontMetrics fm = g.getFontMetrics(f);
            g.setFont(f);
            g.setColor(on ? Estreia.OURO_ESCURO : Estreia.BRANCO);
            String s = b.getText();
            g.drawString(s, (w - fm.stringWidth(s)) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Ranking de tecnicos

    static void ranking(JPanel gx) {
        if (!base(gx) || !absoluto(gx)) return;
        Object t = campo(gx, "ug");
        titulo(t, 28f);
        pos(t, 24, 16, 300, 40);
        pos(campo(gx, "ue"), 444, 22, 190, 28);
        pos(campo(gx, "KK"), 644, 22, 172, 28);
        fechar(campo(gx, "vm"), 880);
        Object lista = campo(gx, "ut");
        rolagem(lista);
        pos(lista, 24, 70, 404, 573);

        Object o = campo(gx, "vd");
        if (o instanceof JPanel && absoluto((JPanel) o)) {
            JPanel vd = (JPanel) o;
            cartao(vd);
            JLabel nome = (JLabel) campo(gx, "Fs"), numeros = (JLabel) campo(gx, "xI");
            if (nome != null && numeros != null) {
                pos(nome, 0, 0, 0, 0);
                pos(numeros, 0, 0, 0, 0);
                nome.setVisible(false);
                numeros.setVisible(false);
                fundo(vd, new Tecnico(nome, numeros), 0, 0, 412, 124);
            }
            Object trofeus = campo(gx, "Ie");
            if (trofeus instanceof JButton) {
                JButton b = (JButton) trofeus;
                b.setText("");
                b.setToolTipText("Títulos do técnico");
                b.setIcon(new Estreia.Icone("taca", 20, Estreia.OURO));
                b.putClientProperty(BotaoUI.PRIMARIO, Boolean.FALSE);
                b.putClientProperty(BotaoUI.REDONDO, Boolean.TRUE);
                b.setContentAreaFilled(true);
                b.setBorder(new EmptyBorder(0, 0, 0, 0));
            }
            pos(trofeus, 354, 14, 42, 42);
        }
        pos(o, 444, 70, 412, 124);
        Object hist = campo(gx, "wi");
        rolagem(hist);
        pos(hist, 444, 206, 412, 437);
        vazio(hist, "Sem histórico", "As temporadas do técnico aparecem aqui.");
        gx.revalidate();
    }

    // Cartao do tecnico: nome grande, reputacao em rotulo dourado e numeros em destaque.
    // Le os textos que o jogo poe nos rotulos (escondidos) Fs e xI.
    static class Tecnico extends JComponent {
        static final Pattern REPUTACAO = Pattern.compile("^(.*?)\\s*\\(reputa\\S*:\\s*(.*)\\)\\s*$");
        static final Pattern NUMEROS = Pattern.compile("(\\d+) J, (\\d+) V, (\\d+) D, (\\d+) Pontos, (\\d+) T");
        static final String[] NOMES = {"JOGOS", "VITÓRIAS", "DERROTAS", "PONTOS", "TÍTULOS"};
        final JLabel nome, numeros;
        Tecnico(JLabel nome, JLabel numeros) {
            this.nome = nome;
            this.numeros = numeros;
            java.beans.PropertyChangeListener l = new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { repaint(); }
            };
            nome.addPropertyChangeListener("text", l);
            numeros.addPropertyChangeListener("text", l);
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            String n = nome.getText() == null ? "" : nome.getText(), rep = null;
            Matcher m = REPUTACAO.matcher(n);
            if (m.matches()) {
                n = m.group(1).trim();
                rep = m.group(2).trim();
            }
            if ("Técnico".equals(n)) n = "Escolha um técnico";
            g.setFont(Estreia.titulo(26f));
            g.setColor(Estreia.BRANCO);
            g.drawString(n.toUpperCase(), 18, 40);
            if (rep != null) {
                g.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.18f));
                g.setColor(Estreia.OURO);
                g.drawString(("Reputação " + rep).toUpperCase(), 18, 58);
            }
            Matcher q = NUMEROS.matcher(numeros.getText() == null ? "" : numeros.getText());
            if (q.find()) {
                for (int i = 0; i < 5; i++) {
                    int x = 18 + i * 76;
                    g.setFont(Estreia.titulo(24f));
                    g.setColor(i == 4 ? Estreia.OURO : Estreia.BRANCO);
                    g.drawString(q.group(i + 1), x, 96);
                    g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.14f));
                    g.setColor(Estreia.FRACO);
                    g.drawString(NOMES[i], x, 112);
                }
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Campeoes e Artilheiros

    static void faixaDeAbas(Object vd, Object barra) {
        if (vd instanceof JComponent) {
            ((JComponent) vd).setOpaque(false);
            ((JComponent) vd).setBorder(new Telas.Filete());
        }
        if (barra instanceof JComponent) ((JComponent) barra).setOpaque(false);
    }

    static void campeoes(JPanel r) {
        if (!base(r) || !absoluto(r)) return;
        Object t = campo(r, "vf");
        titulo(t, 28f);
        pos(t, 29, 14, 500, 40);
        fechar(campo(r, "vm"), 848);
        Object vd = campo(r, "vd");
        faixaDeAbas(vd, campo(r, "ve"));
        pos(vd, 29, 62, 790, 44);
        final Object lista = campo(r, "ut");
        rolagem(lista);
        vazio(lista, "Nenhum campeão ainda", "Os campeões de cada temporada aparecem aqui.");
        final Object filtro = campo(r, "ue");
        Runnable ajusta = new Runnable() {
            public void run() {
                boolean f = filtro instanceof Component && ((Component) filtro).isVisible();
                pos(lista, 29, 116, 790, f ? 456 : 488);
                pos(filtro, 619, 582, 200, 26);
                ((Component) lista).revalidate();
            }
        };
        ajusta.run();
        reajusta(filtro, ajusta);
        r.revalidate();
    }

    static void reajusta(Object o, final Runnable r) {
        if (!(o instanceof Component)) return;
        ((Component) o).addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) { r.run(); }
            public void componentHidden(java.awt.event.ComponentEvent e) { r.run(); }
        });
    }

    static void artilheiros(JPanel a) {
        if (!base(a) || !absoluto(a)) return;
        Object t = campo(a, "vf");
        titulo(t, 28f);
        pos(t, 20, 14, 500, 40);
        fechar(campo(a, "vb"), 758);
        Object vd = campo(a, "vd");
        faixaDeAbas(vd, campo(a, "ve"));
        pos(vd, 20, 62, 718, 44);
        Object lista = campo(a, "ut");
        rolagem(lista);
        pos(lista, 20, 116, 718, 442);
        vazio(lista, "Nenhum artilheiro ainda", "Os goleadores aparecem aqui depois das primeiras rodadas.");
        Object sempre = campo(a, "vc");
        Editor.botao(sempre, "Melhores de todos os tempos", "taca", false);
        pos(sempre, 20, 568, 260, 34);
        Object todos = campo(a, "vg");
        fonte(todos, Estreia.semi(12f), Estreia.OURO);
        if (todos instanceof JLabel) ((JLabel) todos).setHorizontalAlignment(SwingConstants.RIGHT);
        pos(todos, 538, 575, 200, 20);
        a.revalidate();
    }

    // ------------------------------------------------------------- Sobre (Opcoes)

    // Titulo grande e os creditos em duas colunas. So muda o que aparece nesta janela:
    // as travas da versao nao registrada continuam as do jogo.
    static void sobre(JPanel jp) {
        if (!base(jp) || !absoluto(jp)) return;
        Object t = campo(jp, "ug");
        titulo(t, 30f);
        pos(t, 30, 18, 380, 40);
        fechar(campo(jp, "vm"), 496);
        for (String s : new String[] {"uh", "vx", "vy", "a_", "ur", "us"}) {
            Object o = campo(jp, s);
            if (o instanceof Component) ((Component) o).setVisible(false);
        }
        credito(jp, 30, "Jogo criado por", "Emmanuel Santos");
        credito(jp, 260, "Modificado por", "Leonardo Brombila");
        JLabel direitos = new JLabel("Todos os direitos reservados.");
        fonte(direitos, Estreia.texto(11.5f), Estreia.FRACO);
        adiciona(jp, direitos, 30, 140, 430, 18);
        jp.revalidate();
    }

    private static void credito(JPanel pai, int x, String rotulo, String nome) {
        adiciona(pai, rotulo(rotulo), x, 76, 210, 16);
        JLabel l = new JLabel(nome);
        fonte(l, Estreia.semi(16f), Estreia.BRANCO);
        adiciona(pai, l, x, 96, 210, 24);
    }

    // ------------------------------------------------------------- Galeria de trofeus

    static void galeria(JPanel di) {
        if (!base(di)) return;
        Object t = campo(di, "vf");
        titulo(t, 24f);
        pos(t, 20, 6, 700, 32);
        Object x = campo(di, "vm");
        Janelas.fechar(x);
        pos(x, 842, 5, 30, 30);
        Object ks = campo(di, "ER");
        int cheios = 0;
        if (ks instanceof Object[]) {
            for (Object k : (Object[]) ks) {
                if (!(k instanceof JPanel)) continue;
                Object taca = campo(k, "Ow");
                boolean cheio = taca instanceof Component && ((Component) taca).isVisible();
                if (cheio) {
                    cheios++;
                    trofeu((JPanel) k);
                } else {
                    ((JPanel) k).setVisible(false);
                }
            }
        }
        if (cheios == 0) adiciona(di, new SemTitulos(), 20, 42, 846, 162);
        di.revalidate();
    }

    static void trofeu(JPanel k) {
        cartao(k);
        Object nome = campo(k, "Ov");
        fonte(nome, Estreia.semi(12f), Estreia.OURO);
        if (nome instanceof JLabel) ((JLabel) nome).setOpaque(false);
        for (String s : new String[] {"BE", "BF"}) {
            Object sep = campo(k, s);
            if (sep instanceof JComponent) {
                ((JComponent) sep).setForeground(new Color(244, 241, 234, 36));
                ((JComponent) sep).setBackground(new Color(0, 0, 0, 0));
            }
        }
        Object anos = campo(k, "Ot");
        fonte(anos, Estreia.texto(11.5f), Estreia.FRACO);
        Object qtd = campo(k, "Ou");
        if (qtd instanceof JLabel) {
            JLabel q = (JLabel) qtd;
            q.setOpaque(false);
            q.setHorizontalAlignment(SwingConstants.RIGHT);
            q.setFont(Estreia.titulo(28f));
            q.setForeground(Estreia.OURO);
            // o jogo troca a fonte do numero depois; mantemos a nossa
            q.addPropertyChangeListener("font", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    JLabel l = (JLabel) e.getSource();
                    if (l.getFont().getSize() != 28) l.setFont(Estreia.titulo(28f));
                }
            });
            pos(q, 130, 126, 62, 34);
        }
        Object img = campo(k, "Ow");
        if (img instanceof JLabel) ((JLabel) img).setOpaque(false);
    }

    // Time sem titulos: taca apagada e uma frase, em vez de quatro quadros vazios.
    static class SemTitulos extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            new Editor.Cartao(false).paintBorder(this, g, 0, 0, w, h);
            java.awt.Font t = Estreia.titulo(26f), s = Estreia.texto(13f);
            String a = "AINDA SEM TÍTULOS", b = "As taças que o clube conquistar vão aparecer aqui.";
            int larg = Math.max(g.getFontMetrics(t).stringWidth(a), g.getFontMetrics(s).stringWidth(b));
            int x0 = (w - (64 + 22 + larg)) / 2;
            Estreia.icone(g, "taca", x0, (h - 64) / 2.0, 64, Tema.alfa(Estreia.OURO, 110));
            g.setFont(t);
            g.setColor(Estreia.BRANCO);
            g.drawString(a, x0 + 86, h / 2 - 4);
            g.setFont(s);
            g.setColor(Estreia.FRACO);
            g.drawString(b, x0 + 86, h / 2 + 18);
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Opcoes (Preferencias)

    static void opcoes(JPanel fb) {
        if (!base(fb)) return;
        Object t = campo(fb, "ug");
        titulo(t, 28f);
        pos(t, 28, 12, 300, 40);
        fechar(campo(fb, "vm"), 867);
        if (!(fb.getClientProperty("brombila.janela") instanceof JCheckBox)) {
            JCheckBox janela = Janela.chave();
            fb.putClientProperty("brombila.janela", janela);
            adiciona(fb, janela, 600, 20, 200, 26);
        }

        Object esq = campo(fb, "vd"), dir = campo(fb, "we");
        cartao(esq);
        cartao(dir);
        pos(esq, 20, 62, 384, 542);
        pos(dir, 416, 62, 431, 542);
        if (!(esq instanceof JPanel) || !(dir instanceof JPanel)) return;
        JPanel vd = (JPanel) esq, we = (JPanel) dir;

        // coluna da esquerda: jogo (menus) e preferencias (chaves)
        adiciona(vd, rotulo("Jogo"), 18, 16, 340, 16);
        String[][] linhas = {
            {"a_", "Jk", null}, {"ur", "Jp", null}, {"us", "Jq", "Velocidade sem humanos:"},
            {"vx", "Ji", "Pênaltis sem humanos:"}, {"vy", "Jo", null},
            {"vz", "Jm", "Substituição no intervalo:"}, {"vA", "IW", "Aviso de fim de contrato:"},
        };
        for (int i = 0; i < linhas.length; i++) {
            int y = 40 + i * 31;
            Object r = campo(fb, linhas[i][0]);
            if (r instanceof JLabel) {
                JLabel l = (JLabel) r;
                if (linhas[i][2] != null) l.setText(linhas[i][2]);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                fonte(l, Estreia.semi(12.5f), Estreia.FRACO);
            }
            pos(r, 18, y + 3, 196, 20);
            pos(campo(fb, linhas[i][1]), 214, y, 152, 26);
        }
        adiciona(vd, rotulo("Preferências"), 18, 268, 340, 16);
        String[] chaves = {"IL", "IN", "IO", "IV", "IU"};
        for (int i = 0; i < chaves.length; i++) {
            Object c = campo(fb, chaves[i]);
            interruptor(c);
            pos(c, 16, 290 + i * 30, chaves[i].equals("IO") ? 200 : 350, 26);
        }
        if (campo(fb, "IO") instanceof JCheckBox) ((JCheckBox) campo(fb, "IO")).setText("Cor nos placares:");
        String[] placar = {"Ef", "Eg", "Eh"};
        for (int i = 0; i < placar.length; i++) pos(campo(fb, placar[i]), 236 + i * 40, 348, -1, -1);
        Object tema = campo(fb, "JD");
        rotulo(tema);
        if (tema instanceof JLabel) ((JLabel) tema).setText("TEMA DE CORES DAS JANELAS");
        pos(tema, 18, 446, 340, 16);
        String[] cores = {"Jr", "Js", "Jt", "Ju", "Jv"};
        for (int i = 0; i < cores.length; i++) pos(campo(fb, cores[i]), 18 + i * 40, 468, -1, -1);
        Object lista = campo(fb, "IP");
        interruptor(lista);
        pos(lista, 16, 506, 240, 26);
        Object botaoCores = campo(fb, "IM");
        Editor.botao(botaoCores, "Cores", null, false);
        pos(botaoCores, 270, 504, 96, 30);

        // coluna da direita: ligas sem humanos
        Object ver = campo(fb, "Bj");
        rotulo(ver);
        if (ver instanceof JLabel) ((JLabel) ver).setText("VER PARTIDAS DAS LIGAS SEM HUMANOS");
        pos(ver, 18, 16, 400, 16);
        Object paises = campo(fb, "wi");
        rolagem(paises);
        pos(paises, 18, 40, 395, 140);
        String[] filtros = {"IT", "IR", "IS", "IQ"};
        for (int i = 0; i < filtros.length; i++) {
            Object c = campo(fb, filtros[i]);
            interruptor(c);
            pos(c, 16, 190 + i * 28, 400, 26);
        }
        Object comp = campo(fb, "afo");
        rotulo(comp);
        if (comp instanceof JLabel) ((JLabel) comp).setText("COMPETIÇÕES SEM HUMANOS");
        pos(comp, 18, 314, 400, 16);
        Object vp = campo(fb, "afp"), vr = campo(fb, "vw");
        fonte(vp, Estreia.semi(12f), Estreia.FRACO);
        fonte(vr, Estreia.semi(12f), Estreia.FRACO);
        if (vp instanceof JLabel) ((JLabel) vp).setHorizontalAlignment(SwingConstants.LEFT);
        if (vr instanceof JLabel) ((JLabel) vr).setHorizontalAlignment(SwingConstants.LEFT);
        pos(vp, 18, 336, 160, 18);
        pos(vr, 252, 336, 160, 18);
        Object a = campo(fb, "zf"), b = campo(fb, "ut");
        rolagem(a);
        rolagem(b);
        pos(a, 18, 358, 162, 166);
        pos(b, 252, 358, 161, 166);
        Object ida = campo(fb, "yV"), volta = campo(fb, "yZ");
        Editor.botao(ida, "»", null, false);
        Editor.botao(volta, "«", null, false);
        pos(ida, 190, 404, 52, 30);
        pos(volta, 190, 444, 52, 30);
        fb.revalidate();
    }

    // ------------------------------------------------------------- Procura

    static void procura(JPanel gx) {
        if (!base(gx)) return;
        Object filtros = campo(gx, "zi");
        cartao(filtros);
        if (filtros instanceof Container) {
            Container o = (Container) filtros;
            Janelas.fechar(campo(o, "vm"));
            pos(campo(o, "vm"), 756, 8, 34, 34);
            for (Component c : o.getComponents()) {
                if (c instanceof JCheckBox) {
                    interruptor(c);
                    larguraLivre(c);
                    // duas colunas de chaves no canto direito do cartao
                    String n = ((JCheckBox) c).getText();
                    if ("estrela".equals(n) || "À venda".equals(n)) moveX(c, 556);
                    else if ("Top Mundial".equals(n) || "Empréstimo".equals(n)) moveX(c, 664);
                } else if (c instanceof JTextField) {
                    Editor.texto((JTextField) c);
                } else if (c instanceof javax.swing.JSlider) {
                    ((JComponent) c).setOpaque(false);
                } else if (c instanceof JLabel) {
                    JLabel l = (JLabel) c;
                    String s = l.getText() == null ? "" : l.getText();
                    boolean valor = s.matches("\\d+\\s*-\\s*\\d+\\+?");
                    fonte(l, Estreia.semi(12f), valor ? Estreia.OURO : Estreia.FRACO);
                }
            }
        }
        Editor.botao(campo(gx, "Kf"), "Limpar filtros", "volta", false);
        Editor.botao(campo(gx, "yY"), null, "busca", true);
        fonte(campo(gx, "zh"), Estreia.semi(12f), Estreia.FRACO);
        Object lista = campo(gx, "ut");
        rolagem(lista);
        vazio(lista, "Nenhum jogador listado", "Escolha os filtros e clique em Procurar.");
        gx.revalidate();
    }

    // ------------------------------------------------------------- titulo em duas cores

    // "Calendário - ABC" vira CALENDÁRIO em branco e ABC em dourado.
    static class DuasCores extends BasicLabelUI {
        @Override
        protected void paintEnabledText(JLabel l, Graphics g0, String s, int x, int y) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String t = l.getText() == null ? "" : l.getText();
            String a = t, b = null;
            int i = t.indexOf(" - ");
            if (i > 0) {
                a = t.substring(0, i);
                b = t.substring(i + 3);
            } else if (t.endsWith(")") && t.lastIndexOf(" (") > 0) {
                // "Rodolfo Castro (G)": posicao em dourado
                i = t.lastIndexOf(" (");
                a = t.substring(0, i);
                b = t.substring(i + 2, t.length() - 1);
            }
            a = a.toUpperCase();
            g.setColor(Estreia.BRANCO);
            g.drawString(a, x, y);
            if (b != null) {
                g.setColor(Estreia.OURO);
                g.drawString(b.toUpperCase(), x + g.getFontMetrics().stringWidth(a + "  "), y);
            }
        }
        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            return new Dimension(d.width + 40, d.height);
        }
    }

    // Mensagem no meio da tabela vazia, desenhada pela borda da propria tabela.
    static class Vazio extends AbstractBorder {
        final String titulo, texto;
        int linhasFixas = 1; // paineis de linhas: so o cabecalho = vazio
        Vazio(String titulo, String texto) { this.titulo = titulo; this.texto = texto; }
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        @Override
        public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 0, 0); return i; }
        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            if (c instanceof JTable) {
                if (((JTable) c).getRowCount() > 0) return;
            } else if (!(c instanceof Container) || ((Container) c).getComponentCount() > linhasFixas) {
                return;
            }
            Rectangle v = ((JComponent) c).getVisibleRect();
            if (v.height < 60) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Font ft = Estreia.titulo(22f), fx = Estreia.texto(12.5f);
            FontMetrics mt = g.getFontMetrics(ft), mx = g.getFontMetrics(fx);
            int cx = v.x + v.width / 2, cy = v.y + v.height / 2;
            g.setFont(ft);
            g.setColor(new Color(244, 241, 234, 150));
            String a = titulo.toUpperCase();
            g.drawString(a, cx - mt.stringWidth(a) / 2, cy - 4);
            g.setFont(fx);
            g.setColor(Estreia.APAGADO);
            g.drawString(texto, cx - mx.stringWidth(texto) / 2, cy + 18);
            g.dispose();
        }
    }
}
