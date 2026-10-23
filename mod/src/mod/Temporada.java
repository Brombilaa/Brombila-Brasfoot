package mod;

import static mod.Reflexao.campo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

// Janelas que aparecem durante a temporada, no estilo "Noite de estreia": Fim da temporada,
// E campeao!, Demitido, Treino, Taticas salvas, Proposta pelo jogador, Venda em leilao,
// Proposta de emprestimo, Aposentar jogador, Numeracao do elenco, Apelido do junior, Copa
// (chaves e fases), Torneio amistoso, Ranking de selecoes e Expansao do estadio.
final class Temporada {
    private Temporada() {}

    static boolean aplica(JPanel p, String n) {
        if (n.equals("a.cR")) fimDaTemporada(p);
        else if (n.equals("a.dJ")) Campeao.aplica(p);
        else if (n.equals("a.dm")) demitido(p);
        else if (n.equals("a.iQ")) treino(p);
        else if (n.equals("a.il")) taticas(p);
        else if (n.equals("a.cz")) proposta(p);
        else if (n.equals("a.iX")) leilao(p);
        else if (n.equals("a.dz")) leiloes(p);
        else if (n.equals("a.jm")) emprestimo(p);
        else if (n.equals("a.ji")) aposentar(p);
        else if (n.equals("a.ce")) numeracao(p);
        else if (n.equals("a.l")) apelido(p);
        else if (n.equals("a.al")) copa(p);
        else if (n.equals("a.jH")) torneio(p);
        else if (n.equals("a.jv")) selecoes(p);
        else if (n.equals("a.cq")) expansao(p);
        else return false;
        return true;
    }

    private static void pos(Object o, int x, int y, int w, int h) { Dialogos.pos(o, x, y, w, h); }

    private static String texto(Object o) {
        return o instanceof JLabel && ((JLabel) o).getText() != null ? ((JLabel) o).getText().trim() : "";
    }

    private static void esquerda(Object o) {
        if (o instanceof JLabel) ((JLabel) o).setHorizontalAlignment(SwingConstants.LEFT);
    }

    private static void centro(Object o) {
        if (o instanceof JLabel) ((JLabel) o).setHorizontalAlignment(SwingConstants.CENTER);
    }

    // Painel interno (vd) do tamanho da janela, transparente e com layout absoluto.
    private static JPanel interno(JPanel dono, String nome, int w, int h) {
        Object o = campo(dono, nome);
        if (!(o instanceof JPanel)) return null;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(null);
        pos(vd, 0, 0, w, h);
        return Dialogos.absoluto(vd) ? vd : null;
    }

    private static JPanel cartao(Container pai, int x, int y, int w, int h) {
        JPanel c = new JPanel(null);
        Dialogos.cartao(c);
        Dialogos.fundo(pai, c, x, y, w, h);
        return c;
    }

    private static JLabel rotulo(Container pai, String s, int x, int y, int w) {
        JLabel l = Dialogos.rotulo(s);
        Dialogos.adiciona(pai, l, x, y, w, 16);
        return l;
    }

    private static void combo(Object o) {
        if (!(o instanceof JComboBox)) return;
        JComboBox<?> c = (JComboBox<?>) o;
        c.setFont(Estreia.semi(12.5f));
        c.setBackground(ConfigEstreia.CELULA);
        c.setForeground(Estreia.BRANCO);
        c.setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 46)));
        if (!(c.getRenderer() instanceof Nosso)) c.setRenderer(new Exibe(c.getRenderer()));
    }

    // Caixa fechada com foco: o Metal pinta a cor de selecao (marrom); aqui fica a do campo.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class Exibe implements javax.swing.ListCellRenderer, Nosso {
        private final javax.swing.ListCellRenderer orig;
        Exibe(javax.swing.ListCellRenderer orig) { this.orig = orig; }
        public Component getListCellRendererComponent(javax.swing.JList l, Object v, int i, boolean sel, boolean foc) {
            Component k = orig.getListCellRendererComponent(l, v, i, i < 0 ? false : sel, i < 0 ? false : foc);
            if (i < 0) {
                k.setBackground(ConfigEstreia.CELULA);
                k.setForeground(Estreia.BRANCO);
            }
            return k;
        }
    }

    private static void campoTexto(Object o) {
        if (o instanceof JTextField) Editor.texto((JTextField) o);
    }

    // Botao com a seta depois do texto ("Continuar ›").
    private static void avancar(Object o, String s) {
        Editor.botao(o, s, "seta", true);
        if (o instanceof AbstractButton) ((AbstractButton) o).setHorizontalTextPosition(SwingConstants.LEFT);
    }

    // Reescreve o texto do jogo agora e sempre que ele mudar.
    interface Troca { String troca(String s); }

    private static void reescreve(final JLabel l, final Troca t) {
        final boolean[] dentro = {false};
        final Runnable r = new Runnable() {
            public void run() {
                if (dentro[0]) return;
                String s = l.getText();
                if (s == null) return;
                String n = t.troca(s);
                if (n == null || n.equals(s)) return;
                dentro[0] = true;
                try { l.setText(n); } finally { dentro[0] = false; }
            }
        };
        r.run();
        l.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) { r.run(); }
        });
    }

    private static String semTags(String s) {
        return s.replaceAll("<[^>]*>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    // Instala um renderizador por cima do que o jogo usa e o reinstala quando o jogo
    // troca o modelo (o jogo troca o renderizador logo depois do modelo).
    interface Envolve { TableCellRenderer envolve(TableCellRenderer orig); }

    private static void renderizador(final JTable t, final Class<?> tipo, final Envolve e, final Runnable depois) {
        final Runnable instala = new Runnable() {
            public void run() {
                TableCellRenderer r = t.getDefaultRenderer(tipo);
                if (r != null && !(r instanceof Nosso)) t.setDefaultRenderer(tipo, e.envolve(r));
                if (depois != null) depois.run();
                t.repaint();
            }
        };
        instala.run();
        t.addPropertyChangeListener("model", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent ev) { SwingUtilities.invokeLater(instala); }
        });
    }

    interface Nosso {}

    // ------------------------------------------------------------- telas inteiras

    // Fim da temporada e Torneio amistoso ficam dentro da janela principal: o fundo e o
    // mesmo cartao das janelas, pintado no proprio painel.
    static class FundoUI extends BasicPanelUI {
        private final Janelas.Cartao cartao = new Janelas.Cartao();
        @Override
        public void update(Graphics g, JComponent c) { paint(g, c); }
        @Override
        public void paint(Graphics g, JComponent c) {
            cartao.setSize(c.getWidth(), c.getHeight());
            cartao.paintComponent(g);
        }
    }

    private static boolean tela(JPanel p) {
        if (p.getClientProperty(Janelas.FEITO) != null) return false;
        p.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        p.setUI(new FundoUI());
        p.setOpaque(true);
        p.setBorder(new EmptyBorder(0, 0, 0, 0));
        return Dialogos.absoluto(p);
    }

    // ------------------------------------------------------------- Fim da temporada

    static void fimDaTemporada(JPanel cr) {
        if (!tela(cr)) return;
        Object t = campo(cr, "vf");
        Dialogos.titulo(t, 34f);
        pos(t, 29, 14, 600, 46);
        String[][] colunas = {{"BL", "ut", "DU", "29", "315"}, {"DR", "wi", "DT", "362", "315"}, {"DQ", "zf", "DS", "695", "307"}};
        for (String[] c : colunas) {
            int x = Integer.parseInt(c[3]), w = Integer.parseInt(c[4]);
            cartao(cr, x, 70, w, 572);
            Object r = campo(cr, c[0]);
            if (r instanceof JLabel && texto(r).startsWith("Regionais")) ((JLabel) r).setText("Regionais e estaduais");
            Dialogos.rotulo(r);
            esquerda(r);
            pos(r, x + 16, 86, 180, 16);
            Object s = campo(cr, c[1]);
            if (s instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) s;
                sp.setBorder(BorderFactory.createEmptyBorder());
                sp.setOpaque(false);
                sp.getViewport().setOpaque(false);
            }
            pos(s, x + 8, 116, w - 16, 518);
            Object tab = campo(cr, c[2]);
            if (tab instanceof JTable) {
                final JTable tb = (JTable) tab;
                tb.setOpaque(false);
                renderizador(tb, components.aB.class, new Envolve() {
                    public TableCellRenderer envolve(TableCellRenderer o) { return new Campeoes(o); }
                }, new Runnable() {
                    public void run() {
                        tb.setRowHeight(26);
                        if (tb.getColumnCount() >= 3) {
                            tb.getColumnModel().getColumn(0).setMaxWidth(32);
                            tb.getColumnModel().getColumn(0).setPreferredWidth(32);
                        }
                    }
                });
            }
        }
        combo(campo(cr, "ue"));
        pos(campo(cr, "ue"), 29 + 315 - 16 - 150, 78, 150, 30);
        Editor.botao(campo(cr, "DP"), "Time do Ano", "estrela", false);
        pos(campo(cr, "DP"), 29, 658, 170, 38);
        Editor.botao(campo(cr, "DN"), "Marcar amistosos", "amistoso", false);
        pos(campo(cr, "DN"), 209, 658, 190, 38);
        avancar(campo(cr, "DO"), "Iniciar novo ano");
        pos(campo(cr, "DO"), 1002 - 210, 658, 210, 38);
        cr.revalidate();
        cr.repaint();
    }

    // Campeonato em rotulo dourado; campeao com taca, vice com medalha e artilheiro com bola.
    static class Campeoes implements TableCellRenderer, Nosso {
        private final TableCellRenderer orig;
        Campeoes(TableCellRenderer orig) { this.orig = orig; }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, false, false, row, col);
            if (!(c instanceof JLabel) || !(v instanceof components.aB)) return c;
            JLabel l = (JLabel) c;
            components.aB b = (components.aB) v;
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(0, col == 0 ? 6 : 4, 0, 4));
            l.setForeground(Estreia.BRANCO);
            l.setFont(Estreia.semi(12.5f));
            if (b.getEstado() < 0 && b.to()) {
                // nome da competicao
                l.setIcon(null);
                l.setText(col == 1 ? texto(l).toUpperCase() : "");
                l.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.14f));
                l.setForeground(Estreia.OURO);
                l.setVerticalAlignment(SwingConstants.BOTTOM);
                l.setBorder(new EmptyBorder(0, 4, 4, 4));
                return l;
            }
            l.setVerticalAlignment(SwingConstants.CENTER);
            if (col == 0 && b.getEstado() < 0) {
                int e = b.et();
                l.setText(e >= 1 && e <= 3 ? "" : l.getText());
                l.setIcon(e == 1 ? new Estreia.Icone("taca", 16, Estreia.OURO)
                    : e == 2 ? new Estreia.Icone("medalha", 16, new Color(0xC9C4B8))
                    : e == 3 ? new Estreia.Icone("bola", 16, Estreia.BRANCO) : l.getIcon());
                l.setHorizontalAlignment(SwingConstants.CENTER);
            } else if (col == 2) {
                l.setFont(Estreia.texto(12f));
                l.setForeground(Estreia.FRACO);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (col == 1 && b.et() == 1) {
                l.setFont(Estreia.negrito(13f));
            }
            return l;
        }
    }

    // ------------------------------------------------------------- Demitido

    static void demitido(JPanel dm) {
        if (!Dialogos.base(dm) || !Dialogos.absoluto(dm)) return;
        JPanel vd = interno(dm, "vd", 475, 282);
        if (vd == null) return;
        Object t = campo(dm, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Demitido");
        Dialogos.titulo(t, 32f);
        pos(t, 24, 16, 300, 42);
        Object tec = campo(dm, "Fs");
        if (tec instanceof JLabel) ((JLabel) tec).setText("Técnico · " + texto(tec));
        Dialogos.rotulo(tec);
        esquerda(tec);
        pos(tec, 25, 58, 300, 16);
        pos(campo(dm, "Fq"), 380, 10, 75, 68);
        Object aviso = campo(dm, "a_");
        if (aviso instanceof Component) ((Component) aviso).setVisible(false);

        cartao(vd, 24, 88, 427, 76);
        rotulo(vd, "Motivo", 40, 100, 300);
        Object razao = campo(dm, "yA");
        if (razao instanceof JLabel) {
            JLabel l = (JLabel) razao;
            reescreve(l, new Troca() {
                public String troca(String s) {
                    if (s.startsWith("<html>")) return s;
                    String r = s.replaceFirst("^A razão foi:\\s*", "").trim();
                    if (!r.isEmpty()) r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
                    return "<html>" + r + "</html>";
                }
            });
            l.setVerticalAlignment(SwingConstants.TOP);
        }
        Dialogos.fonte(razao, Estreia.texto(13f), Estreia.BRANCO);
        pos(razao, 40, 120, 395, 38);

        rotulo(vd, "Contratado no seu lugar", 24, 180, 300);
        Object novo = campo(dm, "yB");
        if (novo instanceof JLabel) {
            reescreve((JLabel) novo, new Troca() {
                public String troca(String s) {
                    int i = s.indexOf(':');
                    return i >= 0 ? s.substring(i + 1).trim() : s;
                }
            });
        }
        Dialogos.fonte(novo, Estreia.semi(15f), Estreia.BRANCO);
        pos(novo, 24, 198, 280, 22);
        Object resto = campo(dm, "Fr");
        Dialogos.fonte(resto, Estreia.texto(12f), Estreia.FRACO);
        pos(resto, 24, 232, 290, 18);
        avancar(campo(dm, "zK"), "Continuar");
        pos(campo(dm, "zK"), 323, 222, 128, 36);
        dm.revalidate();
    }

    // ------------------------------------------------------------- Treino

    static void treino(JPanel iq) {
        if (!Dialogos.base(iq) || !Dialogos.absoluto(iq)) return;
        Object t = campo(iq, "ug");
        Dialogos.titulo(t, 30f);
        pos(t, 28, 16, 260, 40);
        Dialogos.fechar(campo(iq, "vm"), 409);
        Object foco = campo(iq, "a_");
        if (foco instanceof JLabel) ((JLabel) foco).setText("Foco do treino da semana");
        Dialogos.rotulo(foco);
        pos(foco, 29, 70, 300, 16);
        String[][] opcoes = {{"MW", "ur", "28", "Principais"}, {"MX", "us", "208", "Secundárias"}};
        for (String[] o : opcoes) {
            Object r = campo(iq, o[0]);
            Object sub = campo(iq, o[1]);
            if (r instanceof JRadioButton) {
                JRadioButton b = (JRadioButton) r;
                b.setUI(new Foco());
                b.setOpaque(false);
                b.setFocusPainted(false);
                b.setToolTipText(b.getText().trim());
                b.setText(o[3]);
                b.setFont(Estreia.negrito(14f));
                String s = texto(sub).replace("(", "").replace(")", "").replace("/", " · ");
                b.putClientProperty(Foco.SUB, s);
            }
            if (sub instanceof Component) ((Component) sub).setVisible(false);
            pos(r, Integer.parseInt(o[2]), 92, 172, 62);
        }
        Object auto = campo(iq, "MY");
        Dialogos.interruptor(auto);
        pos(auto, 28, 176, 352, 26);
        iq.revalidate();
    }

    // Opcao grande: titulo, linha de apoio e bolinha; a escolhida fica com contorno dourado.
    static class Foco extends BasicRadioButtonUI {
        static final String SUB = "brombila.sub";
        @Override
        public void paint(Graphics g0, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            RoundRectangle2D r = new RoundRectangle2D.Float(0.75f, 0.75f, w - 1.5f, h - 1.5f, 14, 14);
            boolean on = m.isSelected();
            g.setColor(on ? Tema.alfa(Estreia.OURO, 30) : new Color(244, 241, 234, m.isRollover() ? 18 : 8));
            g.fill(r);
            g.setStroke(new BasicStroke(on ? 1.5f : 1f));
            g.setColor(on ? Estreia.OURO : new Color(244, 241, 234, m.isRollover() ? 90 : 46));
            g.draw(r);
            Ellipse2D anel = new Ellipse2D.Float(14, 15, 14, 14);
            g.setStroke(new BasicStroke(1.4f));
            g.setColor(on ? Estreia.OURO : Estreia.FRACO);
            g.draw(anel);
            if (on) g.fill(new Ellipse2D.Float(17.5f, 18.5f, 7, 7));
            g.setFont(b.getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(b.isEnabled() ? Estreia.BRANCO : Estreia.APAGADO);
            g.drawString(b.getText(), 38, 15 + 7 - fm.getHeight() / 2f + fm.getAscent());
            Object sub = b.getClientProperty(SUB);
            if (sub != null) {
                java.awt.Font f = Estreia.texto(11.5f);
                FontMetrics fs = g.getFontMetrics(f);
                g.setFont(f);
                g.setColor(on ? Tema.alfa(Estreia.OURO, 220) : Estreia.FRACO);
                g.drawString(sub.toString(), 38, 42 + fs.getAscent() - fs.getHeight() / 2f);
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Taticas salvas

    static void taticas(JPanel il) {
        if (!Dialogos.base(il) || !Dialogos.absoluto(il)) return;
        Object t = campo(il, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Táticas salvas");
        Dialogos.titulo(t, 30f);
        pos(t, 24, 14, 277, 40);
        Object lista = campo(il, "Bf");
        combo(lista);
        // com foco, o Metal pinta a caixa fechada vazia com a cor de selecao
        if (lista instanceof JComboBox) ((JComboBox<?>) lista).setFocusable(false);
        pos(lista, 24, 62, 277, 32);
        Editor.botao(campo(il, "vb"), "Carregar", "pasta", true);
        Editor.botao(campo(il, "vc"), "Apagar", "lixeira", false);
        pos(campo(il, "vb"), 24, 104, 134, 36);
        pos(campo(il, "vc"), 167, 104, 134, 36);
        cartao(il, 24, 156, 277, 98);
        Object salvar = campo(il, "uh");
        if (salvar instanceof JLabel) ((JLabel) salvar).setText("Salvar a tática atual");
        Dialogos.rotulo(salvar);
        pos(salvar, 38, 170, 250, 16);
        Object nome = campo(il, "a_");
        if (nome instanceof Component) ((Component) nome).setVisible(false);
        Object campoNome = campo(il, "Ej");
        campoTexto(campoNome);
        if (campoNome instanceof JTextField) ((JTextField) campoNome).setToolTipText("Nome da tática");
        pos(campoNome, 38, 196, 150, 36);
        Editor.botao(campo(il, "GP"), "Salvar", "disquete", true);
        pos(campo(il, "GP"), 196, 196, 92, 36);
        il.revalidate();
    }

    // ------------------------------------------------------------- Proposta pelo jogador

    static void proposta(JPanel cz) {
        if (!Dialogos.base(cz) || !Dialogos.absoluto(cz)) return;
        JPanel vd = interno(cz, "vd", 373, 300);
        if (vd == null) return;
        Object k = campo(cz, "ug");
        Dialogos.rotulo(k);
        esquerda(k);
        pos(k, 25, 20, 320, 16);
        Object nome = campo(cz, "uF");
        Dialogos.titulo(nome, 28f);
        pos(nome, 24, 36, 325, 40);
        cartao(vd, 24, 84, 325, 44);
        Object passe = campo(cz, "uG");
        if (passe instanceof JLabel) ((JLabel) passe).setUI(new Outras.Valor());
        Dialogos.fonte(passe, Estreia.negrito(13.5f), Estreia.BRANCO);
        pos(passe, 40, 96, 293, 20);
        Object pede = campo(cz, "uI");
        if (pede instanceof JLabel) ((JLabel) pede).setText("Sua proposta (em milhares de $)");
        Dialogos.rotulo(pede);
        pos(pede, 25, 144, 320, 16);
        Object valor = campo(cz, "uE");
        campoTexto(valor);
        pos(valor, 24, 164, 325, 34);
        Object preco = campo(cz, "uH");
        if (preco instanceof JLabel) {
            reescreve((JLabel) preco, new Troca() {
                public String troca(String v) {
                    if (v.startsWith("Pre\u00e7o")) return " ";
                    if (v.startsWith("Sua oferta") || v.trim().isEmpty()) return v;
                    if (v.startsWith("valor") || v.startsWith("Valor")) return Character.toUpperCase(v.charAt(0)) + v.substring(1);
                    return "Sua oferta: " + v;
                }
            });
        }
        Dialogos.fonte(preco, Estreia.negrito(13f), Estreia.OURO);
        esquerda(preco);
        pos(preco, 25, 204, 320, 20);
        Editor.botao(campo(cz, "uC"), "Cancelar", null, false);
        Editor.botao(campo(cz, "uD"), "Enviar", "seta", true);
        if (campo(cz, "uD") instanceof AbstractButton) ((AbstractButton) campo(cz, "uD")).setHorizontalTextPosition(SwingConstants.LEFT);
        pos(campo(cz, "uC"), 24, 240, 150, 36);
        pos(campo(cz, "uD"), 199, 240, 150, 36);
        cz.revalidate();
    }

    // ------------------------------------------------------------- Apelido do junior

    static void apelido(JPanel l) {
        if (!Dialogos.base(l) || !Dialogos.absoluto(l)) return;
        Object k = campo(l, "ug");
        Dialogos.rotulo(k);
        esquerda(k);
        pos(k, 25, 20, 320, 16);
        Object nome = campo(l, "uF");
        Dialogos.titulo(nome, 28f);
        pos(nome, 24, 36, 325, 40);
        Object a = campo(l, "uG"), b = campo(l, "uI");
        if (a instanceof JLabel) ((JLabel) a).setText("O apelido aparece no lugar do nome do júnior");
        if (b instanceof JLabel) ((JLabel) b).setText("em todas as telas do jogo.");
        for (Object o : new Object[] {a, b}) {
            Dialogos.fonte(o, Estreia.texto(12.5f), Estreia.FRACO);
            esquerda(o);
        }
        pos(a, 25, 94, 320, 18);
        pos(b, 25, 112, 320, 18);
        rotulo(l, "Apelido", 25, 148, 200);
        Object campoNome = campo(l, "uE");
        campoTexto(campoNome);
        pos(campoNome, 24, 168, 325, 34);
        Object erro = campo(l, "uH");
        Dialogos.fonte(erro, Estreia.negrito(12.5f), new Color(0xFF8A6E));
        esquerda(erro);
        pos(erro, 25, 208, 320, 20);
        Editor.botao(campo(l, "uC"), "Cancelar", null, false);
        Editor.botao(campo(l, "uD"), "Definir", "lapis", true);
        pos(campo(l, "uC"), 24, 240, 150, 36);
        pos(campo(l, "uD"), 199, 240, 150, 36);
        l.revalidate();
    }

    // ------------------------------------------------------------- cartao do jogador

    // Substitui o texto em HTML do jogo (MN/MO) por um cartao desenhado com os dados do
    // proprio jogador: bandeira, nome, posicao, numeros principais e caracteristicas.
    private static void fichaDoJogador(JPanel dono) {
        Object painel = campo(dono, "wf"), jogador = campo(dono, "uz");
        if (!(painel instanceof JPanel) || !(jogador instanceof best.F)) return;
        JPanel wf = (JPanel) painel;
        for (String s : new String[] {"MN", "MO"}) {
            Object o = campo(dono, s);
            if (o instanceof Component) ((Component) o).setVisible(false);
        }
        Dialogos.cartao(wf);
        wf.setLayout(new java.awt.BorderLayout());
        wf.add(new Jogador((best.F) jogador), java.awt.BorderLayout.CENTER);
    }

    static class Jogador extends JComponent {
        best.F f;
        Jogador(best.F f) { this.f = f; setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g0) {
            if (f == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int x = 18, y = 18;
            try {
                java.net.URL u = getClass().getResource("/aflags/" + f.getPais() + ".png");
                if (u != null) {
                    Icon b = new ImageIcon(u);
                    b.paintIcon(this, g, x, y + 3);
                    x += b.getIconWidth() + 10;
                }
            } catch (Throwable t) {
                // sem bandeira
            }
            java.awt.Font fn = Estreia.titulo(24f);
            FontMetrics mn = g.getFontMetrics(fn);
            g.setFont(fn);
            g.setColor(Estreia.BRANCO);
            String nome = f.getNome().toUpperCase();
            g.drawString(nome, x, y + 2 + mn.getAscent() - 4);
            int px = x + mn.stringWidth(nome) + 10;
            String p = "";
            try { p = best.aq.rI[f.getPosicao()]; } catch (Throwable t) { /* sem posicao */ }
            if (!p.isEmpty()) {
                Color cp = Principal.cor(Principal.setor(p));
                RoundRectangle2D chip = new RoundRectangle2D.Float(px, y + 4, 24, 18, 6, 6);
                g.setColor(Tema.alfa(cp, 44));
                g.fill(chip);
                g.setColor(Tema.alfa(cp, 150));
                g.draw(chip);
                java.awt.Font fp = Estreia.negrito(11f);
                FontMetrics mp = g.getFontMetrics(fp);
                g.setFont(fp);
                g.setColor(cp);
                g.drawString(p, px + (24 - mp.stringWidth(p)) / 2f, y + 4 + (18 - mp.getHeight()) / 2f + mp.getAscent());
                px += 32;
            }
            if (f.fP()) {
                Color cr = new Color(0xFF8A6E);
                java.awt.Font fc = Estreia.negrito(10.5f);
                FontMetrics mc = g.getFontMetrics(fc);
                int cw = mc.stringWidth("Contundido") + 16;
                RoundRectangle2D chip = new RoundRectangle2D.Float(px, y + 4, cw, 18, 18, 18);
                g.setColor(Tema.alfa(cr, 34));
                g.fill(chip);
                g.setColor(Tema.alfa(cr, 140));
                g.draw(chip);
                g.setFont(fc);
                g.setColor(cr);
                g.drawString("Contundido", px + 8, y + 4 + (18 - mc.getHeight()) / 2f + mc.getAscent());
            }

            // numeros: 3 colunas x 2 linhas
            int[] jg = f.gw();
            boolean individual = false;
            try { individual = c.a.SR != null && c.a.SR.isHabilidadeIndividual(); } catch (Throwable t) { /* jogo antigo */ }
            String[][] dados = {
                {"Força", Integer.toString(f.fi())},
                {"Idade", Integer.toString(f.getIdade())},
                {"Jogos · gols", (jg != null && jg.length > 1 ? jg[0] + " · " + jg[1] : "-")},
                {"Valor", best.C.c(f.fk())},
                {"Salário", best.C.c(f.fj())},
            };
            int col = (w - 36) / 3, y0 = 66;
            for (int i = 0; i < 5; i++) {
                int cx = i < 3 ? 18 + i * col : 18 + (i - 3) * (col * 3 / 2), cy = y0 + (i / 3) * 50;
                rotuloEm(g, dados[i][0], cx, cy);
                g.setFont(i == 0 ? Estreia.titulo(24f) : Estreia.semi(15f));
                g.setColor(i == 0 ? Estreia.OURO : Estreia.BRANCO);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(dados[i][1], cx, cy + 14 + fm.getAscent() - (i == 0 ? 5 : 0));
            }
            int yc = y0 + 104;
            if (individual) {
                String[] nomes = {"Gol", "Des", "Arm", "Fin", "Vel", "Téc", "Pas"};
                int[] v = {f.gK(), f.gN(), f.gO(), f.gP(), f.gJ(), f.gL(), f.gM()};
                int cw = (w - 36) / 7;
                for (int i = 0; i < 7; i++) {
                    rotuloEm(g, nomes[i], 18 + i * cw, yc);
                    g.setFont(Estreia.semi(13.5f));
                    g.setColor(Estreia.BRANCO);
                    g.drawString(Integer.toString(v[i]), 18 + i * cw, yc + 14 + g.getFontMetrics().getAscent());
                }
                yc += 46;
            }
            String car = "";
            try { car = best.aq.qM[f.getCr1()] + " · " + best.aq.qM[f.getCr2()]; } catch (Throwable t) { /* sem caracteristicas */ }
            if (!car.isEmpty()) {
                g.setColor(new Color(244, 241, 234, 30));
                g.drawLine(18, yc - 8, w - 18, yc - 8);
                rotuloEm(g, "Características", 18, yc);
                g.setFont(Estreia.semi(13f));
                g.setColor(Estreia.BRANCO);
                g.drawString(car.replace("/", " / "), 18, yc + 14 + g.getFontMetrics().getAscent());
            }
            g.dispose();
        }

        private static void rotuloEm(Graphics2D g, String s, int x, int y) {
            g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.16f));
            g.setColor(Estreia.FRACO);
            g.drawString(s.toUpperCase(), x, y + g.getFontMetrics().getAscent());
        }
    }

    // ------------------------------------------------------------- Venda em leilao

    static void leilao(final JPanel ix) {
        if (!Dialogos.base(ix) || !Dialogos.absoluto(ix)) return;
        JPanel vd = interno(ix, "vd", 604, 550);
        if (vd == null) return;
        Object t = campo(ix, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Venda em leilão");
        Dialogos.titulo(t, 30f);
        pos(t, 28, 16, 400, 42);
        avancar(campo(ix, "zK"), "Continuar");
        pos(campo(ix, "zK"), 452, 18, 124, 36);

        Object wf = campo(ix, "wf");
        fichaDoJogador(ix);
        pos(wf, 28, 70, 340, 250);

        cartao(vd, 384, 70, 192, 250);
        rotulo(vd, "Vendedor", 400, 86, 160);
        Object vendedor = campo(ix, "zO");
        Dialogos.fonte(vendedor, Estreia.semi(14f), Estreia.BRANCO);
        if (vendedor instanceof JLabel) ((JLabel) vendedor).setIconTextGap(8);
        esquerda(vendedor);
        pos(vendedor, 400, 106, 166, 26);
        rotulo(vd, "Lance mínimo", 400, 150, 160);
        Object minimo = campo(ix, "Nh");
        Dialogos.fonte(minimo, Estreia.titulo(26f), Estreia.OURO);
        esquerda(minimo);
        pos(minimo, 400, 168, 166, 34);

        // resultado (quando o leilao ja aconteceu)
        Object res = campo(ix, "Ni");
        if (res instanceof JLabel) {
            JLabel l = (JLabel) res;
            reescreve(l, new Troca() {
                public String troca(String s) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("Comprador:\\s*(.*?)\\s+vendido por\\s+(.*)").matcher(semTags(s));
                    if (!m.find()) return s.startsWith("<html>") ? s : "<html>" + s + "</html>";
                    return "<html><font color='#F4F1EA'><b>" + m.group(1) + "</b></font> comprou por <font color='#F2C14E'><b>"
                        + m.group(2) + "</b></font></html>";
                }
            });
            l.setIconTextGap(12);
            l.setBorder(new EmptyBorder(0, 14, 0, 14));
            cartao(vd, 28, 336, 548, 58);
        }
        Dialogos.fonte(res, Estreia.texto(14f), Estreia.FRACO);
        esquerda(res);
        pos(res, 28, 336, 548, 58);

        // lances (jogo com mais de um tecnico humano)
        Object caixa = campo(ix, "zP");
        Dialogos.fonte(caixa, Estreia.semi(13f), Estreia.BRANCO);
        esquerda(caixa);
        pos(caixa, 28, 408, 548, 24);
        campoTexto(campo(ix, "uE"));
        pos(campo(ix, "uE"), 28, 440, 220, 36);
        Editor.botao(campo(ix, "Nf"), "Ofertar", "comprar", true);
        Editor.botao(campo(ix, "Ng"), "Pular", null, false);
        pos(campo(ix, "Nf"), 258, 440, 130, 36);
        pos(campo(ix, "Ng"), 396, 440, 100, 36);
        Object aviso = campo(ix, "uH");
        Dialogos.fonte(aviso, Estreia.negrito(12.5f), Estreia.OURO);
        pos(aviso, 28, 484, 548, 20);
        Object we = campo(ix, "we");
        if (we instanceof JComponent) {
            ((JComponent) we).setOpaque(false);
            ((JComponent) we).setBorder(null);
        }
        pos(we, 0, 0, 0, 0);

        // sem lances para dar: a janela encolhe e o espaco vazio de baixo some
        final Object campoLance = campo(ix, "uE");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (campoLance instanceof Component && ((Component) campoLance).isVisible()) return;
                java.awt.Window w = SwingUtilities.getWindowAncestor(ix);
                if (w == null || w.getHeight() < 500) return;
                java.awt.Rectangle r = w.getBounds();
                w.setBounds(r.x, r.y + (r.height - 418) / 2, r.width, 418);
                w.validate();
            }
        });
        ix.revalidate();
    }

    // ------------------------------------------------------------- Leilao entre tecnicos (a.dz)
    // Varios tecnicos humanos dao lances, um depois do outro. Os botoes do jogo sao JLabels com
    // mouse listener; aqui viram pilulas. A ficha do jogador e a mesma da Venda em leilao.

    static final int LW = 640, LH = 496;

    static void leiloes(final JPanel dz) {
        if (!Dialogos.base(dz) || !Dialogos.absoluto(dz)) return;
        Object o = campo(dz, "we");
        if (!(o instanceof JPanel)) return;
        final JPanel we = (JPanel) o;
        we.setOpaque(false);
        we.setBorder(null);
        pos(we, 0, 0, LW, LH);
        if (!Dialogos.absoluto(we)) return;
        Object t = campo(dz, "ug");
        if (t instanceof JLabel) {
            ((JLabel) t).setText("Venda em leilão");
            ((JLabel) t).setOpaque(false);
        }
        Dialogos.titulo(t, 30f);
        if (t instanceof Component) {
            dz.remove((Component) t);
            Dialogos.adiciona(we, (Component) t, 28, 16, 400, 42);
        }
        // tudo que estava no quadro de baixo (wf) passa para o painel principal
        Object wfo = campo(dz, "wf");
        if (wfo instanceof JPanel) {
            JPanel wf = (JPanel) wfo;
            for (Component c : wf.getComponents()) {
                wf.remove(c);
                Dialogos.adiciona(we, c, 0, 0, 10, 10);
            }
            wf.setVisible(false);
        }
        for (String n : new String[] {"vd", "Gh", "Gj", "Gm", "Fq", "ur"}) {
            Object h = campo(dz, n);
            if (h instanceof Component) ((Component) h).setVisible(false);
        }
        botaoRotulo(campo(dz, "Gb"), "Continuar", true);
        pos(campo(dz, "Gb"), 488, 20, 124, 36);

        // ficha do jogador
        final Jogador ficha = new Jogador(null);
        JPanel cf = cartao(we, 28, 70, 380, 236);
        cf.setLayout(new java.awt.BorderLayout());
        cf.add(ficha, java.awt.BorderLayout.CENTER);

        // vendedor, lance minimo e proximo leilao
        cartao(we, 424, 70, 188, 236);
        rotulo(we, "Vendedor", 440, 86, 160);
        final Object vendedor = campo(dz, "Gk");
        Dialogos.fonte(vendedor, Estreia.semi(14f), Estreia.BRANCO);
        esquerda(vendedor);
        if (vendedor instanceof JLabel) ((JLabel) vendedor).setIconTextGap(8);
        pos(vendedor, 440, 104, 164, 32);
        rotulo(we, "Lance mínimo", 440, 150, 160);
        final Object minimo = campo(dz, "Gi");
        Dialogos.fonte(minimo, Estreia.titulo(26f), Estreia.OURO);
        esquerda(minimo);
        pos(minimo, 440, 168, 164, 34);
        final JLabel proxRot = rotulo(we, "Próximo leilão", 440, 228, 160);
        final Object proximo = campo(dz, "uh");
        Dialogos.fonte(proximo, Estreia.semi(12.5f), Estreia.BRANCO);
        esquerda(proximo);
        pos(proximo, 440, 246, 164, 40);

        // lance do tecnico da vez
        cartao(we, 28, 322, 584, 150);
        rotulo(we, "Seu lance", 44, 338, 300);
        Object quem = campo(dz, "Gg");
        Dialogos.fonte(quem, Estreia.semi(14f), Estreia.BRANCO);
        esquerda(quem);
        if (quem instanceof JLabel) {
            ((JLabel) quem).setIconTextGap(8);
            reescreve((JLabel) quem, new Troca() {
                public String troca(String s) {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("^(.*?) \\((.*)\\) dinheiro em caixa: (.*)$").matcher(s);
                    if (!m.find()) return s;
                    return "<html><font color='#F4F1EA'><b>" + m.group(1) + "</b></font> <font color='#F4F1EA9E'>· " + m.group(2)
                        + " · em caixa</font> <font color='#F2C14E'><b>" + m.group(3) + "</b></font></html>";
                }
            });
        }
        pos(quem, 44, 358, 552, 28);
        final Object campoLance = campo(dz, "Gf");
        campoTexto(campoLance);
        pos(campoLance, 44, 400, 170, 38);
        final JLabel dica = new JLabel("em milhares de $");
        Dialogos.fonte(dica, Estreia.texto(11.5f), Estreia.APAGADO);
        Dialogos.adiciona(we, dica, 44, 442, 170, 16);
        final Object eco = campo(dz, "Gl");
        Dialogos.fonte(eco, Estreia.negrito(13f), Estreia.OURO);
        esquerda(eco);
        pos(eco, 228, 442, 368, 18);
        botaoRotulo(campo(dz, "Gc"), "Ofertar", true);
        botaoRotulo(campo(dz, "Ge"), "Não ofertar", false);
        botaoRotulo(campo(dz, "Gd"), "Ver próximo leilão", true);
        pos(campo(dz, "Gc"), 228, 400, 130, 38);
        pos(campo(dz, "Ge"), 366, 400, 130, 38);
        pos(campo(dz, "Gd"), 412, 418, 184, 38);
        final Object res = campo(dz, "yA");
        Dialogos.fonte(res, Estreia.titulo(24f), Estreia.OURO);
        esquerda(res);
        pos(res, 44, 420, 360, 36);
        // depois do lance o jogo esconde o campo: o resultado ocupa o lugar dele
        final Object ecoF = eco;
        if (campoLance instanceof Component) {
            final Component cl = (Component) campoLance;
            final Runnable troca = new Runnable() {
                public void run() {
                    boolean aberto = cl.isVisible();
                    dica.setVisible(aberto);
                    if (aberto) {
                        Dialogos.fonte(ecoF, Estreia.negrito(13f), Estreia.OURO);
                        pos(ecoF, 228, 442, 368, 18);
                    } else {
                        if (ecoF instanceof JLabel && texto(ecoF).toLowerCase().startsWith("vendido")) ((JLabel) ecoF).setText("Vendido para");
                        Dialogos.rotulo(ecoF);
                        pos(ecoF, 44, 402, 360, 16);
                    }
                    we.revalidate();
                    we.repaint();
                }
            };
            troca.run();
            cl.addComponentListener(new java.awt.event.ComponentAdapter() {
                public void componentShown(java.awt.event.ComponentEvent e) { troca.run(); }
                public void componentHidden(java.awt.event.ComponentEvent e) { troca.run(); }
            });
        }

        // o jogador muda a cada leilao: a ficha e o escudo do vendedor acompanham
        final Object gj = campo(dz, "Gj");
        final Runnable atualiza = new Runnable() {
            public void run() {
                best.F f = null;
                try {
                    Object i = campo(dz, "index");
                    f = ((best.N) a.dz.FY.get((Integer) i)).iA();
                } catch (Throwable e) { /* sem jogador */ }
                ficha.f = f;
                ficha.repaint();
                if (f != null && vendedor instanceof JLabel) {
                    try {
                        ((JLabel) vendedor).setIcon(f.fg().x(30, 30));
                        ((JLabel) vendedor).setText(f.fg().getNome());
                    } catch (Throwable e) { /* fica o texto do jogo */ }
                }
                if (minimo instanceof JLabel) {
                    String m = texto(minimo);
                    if (m.startsWith("Lance m")) ((JLabel) minimo).setText(m.substring(m.indexOf(':') + 1).trim());
                }
                if (proximo instanceof JLabel) {
                    JLabel l = (JLabel) proximo;
                    String p = texto(l);
                    if (p.startsWith("Próximo leilão:")) l.setText("<html>" + p.substring(15).trim() + "</html>");
                    proxRot.setVisible(l.isVisible());
                }
            }
        };
        atualiza.run();
        if (gj instanceof JLabel) {
            ((JLabel) gj).addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { SwingUtilities.invokeLater(atualiza); }
            });
        }

        // a janela do jogo tem 941x718; aqui fica do tamanho do conteudo
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                java.awt.Window w = SwingUtilities.getWindowAncestor(dz);
                if (w == null || w.getWidth() < LW + 40) return;
                java.awt.Rectangle r = w.getBounds();
                w.setBounds(r.x + (r.width - LW) / 2, r.y + (r.height - LH) / 2, LW, LH);
                w.validate();
            }
        });
        dz.revalidate();
    }

    // Botao do jogo feito de JLabel (o clique e um MouseListener): vira pilula.
    static void botaoRotulo(Object o, String texto, boolean primario) {
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        if (texto != null) l.setText(texto);
        l.setOpaque(false);
        l.setBorder(null);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setFont(Estreia.negrito(12.5f));
        l.setForeground(primario ? Estreia.OURO_ESCURO : Estreia.BRANCO);
        l.putClientProperty(BotaoRotuloUI.PRIMARIO, primario);
        l.setUI(new BotaoRotuloUI());
        l.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        l.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { l.putClientProperty(BotaoRotuloUI.SOBRE, true); l.repaint(); }
            public void mouseExited(java.awt.event.MouseEvent e) { l.putClientProperty(BotaoRotuloUI.SOBRE, null); l.repaint(); }
        });
    }

    static class BotaoRotuloUI extends BasicLabelUI {
        static final String PRIMARIO = "brombila.primario", SOBRE = "brombila.sobre";
        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth(), h = c.getHeight();
            boolean primario = Boolean.TRUE.equals(c.getClientProperty(PRIMARIO));
            boolean sobre = c.getClientProperty(SOBRE) != null;
            RoundRectangle2D r = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, h - 3, h - 3);
            if (primario) {
                g.setColor(sobre ? Estreia.OURO.brighter() : Estreia.OURO);
                g.fill(r);
            } else {
                g.setColor(new Color(244, 241, 234, sobre ? 30 : 14));
                g.fill(r);
                g.setColor(new Color(244, 241, 234, 70));
                g.draw(r);
            }
            g.dispose();
            super.paint(g0, c);
        }
        @Override
        protected void paintEnabledText(JLabel l, Graphics g, String s, int x, int y) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Boolean.TRUE.equals(l.getClientProperty(PRIMARIO)) ? Estreia.OURO_ESCURO : Estreia.BRANCO);
            g.drawString(s, x, y);
        }
    }

    // ------------------------------------------------------------- Proposta de emprestimo

    static void emprestimo(JPanel jm) {
        if (!Dialogos.base(jm) || !Dialogos.absoluto(jm)) return;
        JPanel vd = interno(jm, "vd", 403, 440);
        if (vd == null) return;
        Object t = campo(jm, "ug");
        Dialogos.titulo(t, 28f);
        pos(t, 24, 16, 355, 40);
        Object clube = campo(jm, "uu");
        if (clube instanceof JLabel) {
            JLabel l = (JLabel) clube;
            String s = texto(l);
            if (s.endsWith(":")) l.setText(s.substring(0, s.length() - 1));
            Object time = campo(jm, "Nu");
            if (time instanceof best.ah) {
                try { l.setIcon(((best.ah) time).x(30, 30)); } catch (Throwable e) { /* fica o escudo pequeno */ }
            }
            l.setIconTextGap(10);
        }
        Dialogos.fonte(clube, Estreia.semi(14f), Estreia.BRANCO);
        esquerda(clube);
        pos(clube, 24, 58, 355, 34);
        Object wf = campo(jm, "wf");
        fichaDoJogador(jm);
        pos(wf, 24, 102, 355, 262);
        Object sim = campo(jm, "vb"), nao = campo(jm, "vc");
        Editor.botao(sim, null, "emprestimo", true);
        Editor.botao(nao, "Recusar", "fechar", false);
        pos(sim, 24, 382, 170, 36);
        pos(nao, 209, 382, 170, 36);
        jm.revalidate();
    }

    // ------------------------------------------------------------- Aposentar jogador

    static void aposentar(JPanel ji) {
        if (!Dialogos.base(ji) || !Dialogos.absoluto(ji)) return;
        Object k = campo(ji, "ug");
        Dialogos.rotulo(k);
        esquerda(k);
        pos(k, 27, 20, 280, 16);
        Object nome = campo(ji, "uF");
        Dialogos.fonte(nome, Estreia.titulo(28f), Estreia.OURO);
        if (nome instanceof JLabel) ((JLabel) nome).setText(texto(nome).toUpperCase());
        esquerda(nome);
        pos(nome, 26, 36, 300, 40);
        Dialogos.fechar(campo(ji, "BZ"), 402);
        Object a = campo(ji, "ur"), b = campo(ji, "a_");
        for (Object o : new Object[] {a, b}) {
            Dialogos.fonte(o, Estreia.texto(12.5f), Estreia.FRACO);
            if (o instanceof JLabel) ((JLabel) o).setVerticalAlignment(SwingConstants.TOP);
        }
        pos(a, 27, 84, 350, 72);
        pos(b, 27, 156, 350, 36);
        Object rn = campo(ji, "us");
        if (rn instanceof JLabel) ((JLabel) rn).setText("Nome do novo júnior");
        Dialogos.rotulo(rn);
        pos(rn, 27, 202, 230, 16);
        campoTexto(campo(ji, "Nr"));
        pos(campo(ji, "Nr"), 26, 222, 236, 34);
        Object rp = campo(ji, "vy");
        Dialogos.rotulo(rp);
        pos(rp, 273, 202, 100, 16);
        combo(campo(ji, "Nq"));
        pos(campo(ji, "Nq"), 272, 222, 104, 34);
        Object dica = campo(ji, "vx");
        if (dica instanceof JLabel) ((JLabel) dica).setText("Deixe em branco para o jogo escolher um nome.");
        Dialogos.fonte(dica, Estreia.texto(11.5f), Estreia.APAGADO);
        pos(dica, 27, 262, 350, 18);
        Editor.botao(campo(ji, "Np"), "Aposentar jogador", "aposentar", true);
        pos(campo(ji, "Np"), 26, 300, 350, 38);
        ji.revalidate();
    }

    // ------------------------------------------------------------- Numeracao do elenco

    static void numeracao(JPanel ce) {
        if (!Dialogos.base(ce) || !Dialogos.absoluto(ce)) return;
        JPanel vd = interno(ce, "vd", 570, 433);
        if (vd == null) return;
        Object t = campo(ce, "ug");
        Dialogos.titulo(t, 28f);
        pos(t, 24, 16, 420, 40);
        Dialogos.fechar(campo(ce, "ud"), 570);
        Object r = campo(ce, "uh");
        if (r instanceof JLabel) ((JLabel) r).setText("Elenco");
        Dialogos.rotulo(r);
        pos(r, 25, 70, 200, 16);
        Object s = campo(ce, "ut");
        Dialogos.rolagem(s);
        pos(s, 24, 92, 290, 270);
        Object arv = campo(ce, "BV");
        if (arv instanceof JTree) {
            JTree tr = (JTree) arv;
            Telas.arvore(tr);
            tr.setRowHeight(28);
            tr.setCellRenderer(new Numeros());
        }
        Editor.botao(campo(ce, "yW"), "Numerar todos", "camisa", false);
        Editor.botao(campo(ce, "BR"), "Limpar todos", "lixeira", false);
        pos(campo(ce, "yW"), 24, 374, 150, 36);
        pos(campo(ce, "BR"), 182, 374, 132, 36);

        cartao(vd, 330, 92, 216, 270);
        Object pede = campo(ce, "a_");
        if (pede instanceof JLabel) ((JLabel) pede).setText("Número da camisa");
        Dialogos.rotulo(pede);
        pos(pede, 346, 108, 184, 16);
        JLabel faixa = new JLabel("De 1 a 99, sem repetir no elenco.");
        Dialogos.fonte(faixa, Estreia.texto(11.5f), Estreia.FRACO);
        Dialogos.adiciona(vd, faixa, 346, 126, 184, 16);
        Object num = campo(ce, "BU");
        if (num instanceof JSpinner) {
            Janelas.seletor((JSpinner) num);
            JComponent ed = ((JSpinner) num).getEditor();
            if (ed instanceof JSpinner.DefaultEditor) ((JSpinner.DefaultEditor) ed).getTextField().setFont(Estreia.titulo(30f));
        }
        pos(num, 346, 152, 184, 50);
        Editor.botao(campo(ce, "BT"), "Escolher", "camisa", true);
        pos(campo(ce, "BT"), 346, 212, 184, 36);
        Object msg = campo(ce, "zh");
        Dialogos.fonte(msg, Estreia.negrito(12f), Estreia.OURO);
        centro(msg);
        pos(msg, 346, 254, 184, 22);
        Editor.botao(campo(ce, "BS"), "Tirar número", "fechar", false);
        pos(campo(ce, "BS"), 346, 312, 184, 36);
        ce.revalidate();
    }

    // Jogador da arvore: numero da camisa num chip e o nome; pastas sem icone.
    static class Numeros extends DefaultTreeCellRenderer {
        private best.F f;
        Numeros() {
            setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
            setBackgroundSelectionColor(ConfigEstreia.SELECAO);
            setBorderSelectionColor(new Color(0, 0, 0, 0));
            setTextNonSelectionColor(Estreia.BRANCO);
            setTextSelectionColor(Color.WHITE);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object valor, boolean sel, boolean aberto,
                boolean folha, int linha, boolean foco) {
            super.getTreeCellRendererComponent(tree, valor, sel, aberto, folha, linha, false);
            Object u = valor instanceof DefaultMutableTreeNode ? ((DefaultMutableTreeNode) valor).getUserObject() : null;
            f = u instanceof best.F ? (best.F) u : null;
            if (f != null) {
                setText(f.getNome());
                setFont(Estreia.semi(12.5f));
                setIcon(new Vazio(34, 20));
                setIconTextGap(8);
            } else {
                setText(String.valueOf(u).toUpperCase());
                setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.16f));
                setForeground(Estreia.OURO);
                setIcon(null);
            }
            setBorder(new EmptyBorder(0, 2, 0, 6));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (f == null) return;
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int n = f.fn(), h = getHeight();
            String s = n > 0 ? Integer.toString(n) : "–";
            RoundRectangle2D chip = new RoundRectangle2D.Float(2.5f, (h - 20) / 2f, 32, 20, 8, 8);
            Color c = n > 0 ? Estreia.OURO : Estreia.APAGADO;
            g.setColor(Tema.alfa(c, n > 0 ? 34 : 14));
            g.fill(chip);
            g.setColor(Tema.alfa(c, n > 0 ? 150 : 70));
            g.draw(chip);
            java.awt.Font fn = Estreia.negrito(12f);
            FontMetrics fm = g.getFontMetrics(fn);
            g.setFont(fn);
            g.setColor(c);
            g.drawString(s, 2.5f + (32 - fm.stringWidth(s)) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // Icone vazio que so reserva espaco (o chip e desenhado por cima).
    static class Vazio implements Icon {
        final int w, h;
        Vazio(int w, int h) { this.w = w; this.h = h; }
        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component c, Graphics g, int x, int y) {}
    }

    // Icone de linha virado (seta para baixo a partir de "cima" etc.).
    static class Girado implements Icon {
        final String nome;
        final int lado;
        final Color cor;
        final double angulo;
        Girado(String nome, int lado, Color cor, double graus) {
            this.nome = nome;
            this.lado = lado;
            this.cor = cor;
            this.angulo = Math.toRadians(graus);
        }
        public int getIconWidth() { return lado; }
        public int getIconHeight() { return lado; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.rotate(angulo, x + lado / 2.0, y + lado / 2.0);
            Estreia.icone(g, nome, x, y, lado, cor);
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Copa: chaves e fases

    static void copa(JPanel al) {
        if (!Dialogos.base(al) || !Dialogos.absoluto(al)) return;
        Object t = campo(al, "vf");
        Dialogos.titulo(t, 30f);
        pos(t, 24, 14, 600, 42);
        Dialogos.fechar(campo(al, "vm"), 883);
        Object vd = campo(al, "vd");
        if (vd instanceof JComponent) {
            ((JComponent) vd).setOpaque(false);
            ((JComponent) vd).setBorder(new Telas.Filete());
        }
        pos(vd, 13, 62, 857, 40);
        Object abas = campo(al, "ve");
        if (abas instanceof JComponent) ((JComponent) abas).setOpaque(false);
        combo(campo(al, "va"));

        Object fases = campo(al, "wP");
        if (fases instanceof JComponent) {
            ((JComponent) fases).setOpaque(false);
            ((JComponent) fases).setBorder(null);
        }
        if (fases instanceof Container && Dialogos.absoluto((Container) fases)) {
            Object ant = campo(al, "wN"), prox = campo(al, "wO"), nome = campo(al, "wm");
            Editor.botao(ant, "Fase anterior", null, false);
            Editor.botao(prox, "Próxima fase", null, false);
            for (Object b : new Object[] {ant, prox}) {
                if (!(b instanceof JButton)) continue;
                JButton jb = (JButton) b;
                boolean volta = jb == ant;
                jb.setIcon(new Estreia.Icone(volta ? "volta" : "seta", 14, Estreia.OURO));
                jb.setDisabledIcon(new Estreia.Icone(volta ? "volta" : "seta", 14, Estreia.APAGADO));
                jb.setIconTextGap(6);
                jb.setHorizontalTextPosition(volta ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            pos(ant, 0, 2, 150, 34);
            pos(prox, 707, 2, 150, 34);
            Dialogos.fonte(nome, Estreia.titulo(22f), Estreia.OURO);
            centro(nome);
            pos(nome, 160, 2, 537, 34);
        }
        pos(fases, 13, 112, 857, 38);

        Object s = campo(al, "ut");
        if (s instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) s;
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
        }
        pos(s, 13, 198, 857, 588);
        if (s instanceof JScrollPane) Chaves.instala(al, (JScrollPane) s);
        Object tab = campo(al, "wp");
        if (tab instanceof JTable) {
            final JTable tb = (JTable) tab;
            tb.setOpaque(false);
            renderizador(tb, components.d.class, new Envolve() {
                public TableCellRenderer envolve(TableCellRenderer o) { return new Confronto(o); }
            }, new Runnable() {
                public void run() { alturas(tb); }
            });
        }
        al.revalidate();
    }

    // Cada confronto ocupa 3 linhas (espaco, escudos e nomes): o espaco fica fino.
    private static void alturas(JTable t) {
        if (t.getRowCount() == 0 || t.getColumnCount() < 7 || !(t.getValueAt(0, 0) instanceof components.d)) return;
        for (int r = 0; r < t.getRowCount(); r++) {
            Object v = t.getValueAt(r, 0);
            if (!(v instanceof components.d)) continue;
            components.d d = (components.d) v;
            String txt = d.sT();
            int h = d.sS() ? (txt == null || txt.trim().isEmpty() ? 12 : 28) : d.sR() ? 38 : 26;
            if (t.getRowHeight(r) != h) t.setRowHeight(r, h);
        }
        boolean volta = false;
        for (int r = 0; r < t.getRowCount() && !volta; r++) {
            components.d d = (components.d) t.getValueAt(r, 0);
            volta = d.sZ() ? d.sX() != null : d.sQ() != null;
        }
        int[] larg = volta ? new int[] {150, 64, 150, 110, 150, 64, 150} : new int[] {300, 80, 300, 150, 0, 0, 0};
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < 7; i++) {
            javax.swing.table.TableColumn c = t.getColumnModel().getColumn(i);
            c.setMinWidth(0);
            c.setMaxWidth(larg[i] == 0 ? 0 : 1000);
            c.setPreferredWidth(larg[i]);
            c.setWidth(larg[i]);
        }
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
    }

    // Confronto como cartao: escudos maiores, placar dourado no meio e nomes embaixo.
    static class Confronto implements TableCellRenderer, Nosso {
        private final TableCellRenderer orig;
        private final CartaoUI ui = new CartaoUI();
        Confronto(TableCellRenderer orig) { this.orig = orig; }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, false, false, row, col);
            if (!(c instanceof JLabel) || !(v instanceof components.d)) return c;
            JLabel l = (JLabel) c;
            if (l.getUI() != ui) l.setUI(ui);
            components.d d = (components.d) v;
            int parte = d.sS() ? 0 : d.sR() ? 1 : 2;
            boolean volta = col >= 4 && !(d.sZ() ? d.sX() != null : d.sQ() != null);
            l.putClientProperty(CartaoUI.PARTE, parte);
            l.putClientProperty(CartaoUI.COLUNA, col);
            l.putClientProperty(CartaoUI.ULTIMA, volta ? 3 : t.getColumnCount() - 1);
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(0, 8, 0, 8));
            String s = l.getText() == null ? "" : l.getText().trim();
            if (parte == 0) {
                l.setText(s.toUpperCase());
                l.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.16f));
                l.setForeground(Estreia.OURO);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setVerticalAlignment(SwingConstants.BOTTOM);
                return l;
            }
            l.setVerticalAlignment(SwingConstants.CENTER);
            if (col == 3) {
                l.setText(s.toUpperCase());
                l.setFont(Estreia.espacada(Estreia.negrito(10f), 0.16f));
                l.setForeground(Estreia.APAGADO);
                l.setHorizontalAlignment(SwingConstants.CENTER);
            } else if (col == 1 || col == 5) {
                l.setHorizontalAlignment(SwingConstants.CENTER);
                if (parte == 1) {
                    boolean x = s.equals("x");
                    l.setText(x ? "×" : s.replace("x", " × "));
                    l.setFont(x ? Estreia.texto(15f) : Estreia.titulo(22f));
                    l.setForeground(x ? Estreia.APAGADO : Estreia.OURO);
                } else {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("P:(\\d+) x (\\d+)").matcher(s);
                    l.setText(m.find() ? "pên. " + m.group(1) + "–" + m.group(2) : s);
                    l.setFont(Estreia.texto(11f));
                    l.setForeground(Estreia.FRACO);
                }
            } else {
                l.setFont(Estreia.semi(12.5f));
                l.setForeground(Estreia.BRANCO);
            }
            return l;
        }
    }

    // Pinta o pedaco do cartao que cabe na celula (cantos redondos so nas pontas).
    static class CartaoUI extends BasicLabelUI {
        static final String PARTE = "brombila.parte", COLUNA = "brombila.coluna", ULTIMA = "brombila.ultima";
        @Override
        public void paint(Graphics g0, JComponent c) {
            Object p = c.getClientProperty(PARTE), k = c.getClientProperty(COLUNA), u = c.getClientProperty(ULTIMA);
            if (p instanceof Integer && k instanceof Integer && u instanceof Integer && (Integer) p > 0 && (Integer) k <= (Integer) u) {
                int parte = (Integer) p, col = (Integer) k, ultima = (Integer) u;
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = c.getWidth(), h = c.getHeight(), raio = 14, folga = 30;
                float x0 = col == 0 ? 0.5f : -folga, x1 = col == ultima ? w - 0.5f : w + folga;
                float y0 = parte == 1 ? 0.5f : -folga, y1 = parte == 2 ? h - 3.5f : h + folga;
                Shape s = new RoundRectangle2D.Float(x0, y0, x1 - x0, y1 - y0, raio, raio);
                g.setColor(new Color(244, 241, 234, 10));
                g.fill(s);
                g.setColor(new Color(244, 241, 234, 34));
                g.draw(s);
                g.dispose();
            }
            super.paint(g0, c);
        }
    }

    // ------------------------------------------------------------- Torneio amistoso

    static void torneio(JPanel jh) {
        if (!tela(jh)) return;
        Object t = campo(jh, "uu");
        Dialogos.titulo(t, 32f);
        pos(t, 29, 14, 500, 44);
        Object sub = campo(jh, "ug");
        Dialogos.fonte(sub, Estreia.texto(12.5f), Estreia.FRACO);
        pos(sub, 29, 56, 680, 18);
        Object o = campo(jh, "uv");
        if (!(o instanceof JPanel)) return;
        JPanel uv = (JPanel) o;
        uv.setOpaque(false);
        uv.setBorder(null);
        pos(uv, 0, 0, 742, 577);
        if (!Dialogos.absoluto(uv)) return;

        String[][] campos = {{"vx", "Nr", "29", "Nome do torneio"}, {"us", "afG", "261", "Fórmula"}, {"vy", "afH", "493", "Sede (cidade)"}};
        for (String[] c : campos) {
            int x = Integer.parseInt(c[2]);
            Object r = campo(jh, c[0]);
            if (r instanceof JLabel) ((JLabel) r).setText(c[3]);
            Dialogos.rotulo(r);
            esquerda(r);
            pos(r, x + 1, 90, 220, 16);
            Object f = campo(jh, c[1]);
            campoTexto(f);
            combo(f);
            pos(f, x, 110, 220, 34);
        }
        Object a = campo(jh, "a_");
        if (a instanceof JLabel) ((JLabel) a).setText("Escolha os times");
        Dialogos.rotulo(a);
        esquerda(a);
        pos(a, 30, 164, 280, 16);
        Object arvS = campo(jh, "ut");
        Dialogos.rolagem(arvS);
        pos(arvS, 29, 184, 300, 296);
        Object arv = campo(jh, "uw");
        if (arv instanceof JTree) Telas.arvore((JTree) arv);

        String[][] setas = {{"yV", "seta", "0", "Adicionar"}, {"yZ", "volta", "0", "Remover"}, {"afF", "cima", "0", "Subir"}, {"afE", "cima", "180", "Descer"}};
        int y = 236;
        for (String[] s : setas) {
            Object b = campo(jh, s[0]);
            if (b instanceof JButton) {
                JButton jb = (JButton) b;
                Editor.botao(jb, "", null, false);
                jb.setIcon(new Girado(s[1], 16, Estreia.OURO, Double.parseDouble(s[2])));
                jb.setToolTipText(s[3]);
            }
            pos(b, 345, y, 52, 34);
            y += s[0].equals("yZ") ? 74 : 42;
        }

        Object part = campo(jh, "vz");
        Dialogos.rotulo(part);
        esquerda(part);
        if (part instanceof JLabel) {
            reescreve((JLabel) part, new Troca() {
                public String troca(String s) { return s.replaceFirst("(?i)participantes:\\s*", "PARTICIPANTES · "); }
            });
        }
        pos(part, 414, 164, 280, 16);
        Object lista = campo(jh, "wi");
        Dialogos.rolagem(lista);
        pos(lista, 413, 184, 300, 296);
        Dialogos.vazio(lista, "Nenhum time ainda", "Escolha os times à esquerda.");
        Object sortear = campo(jh, "CS");
        Dialogos.interruptor(sortear);
        pos(sortear, 413, 490, 200, 26);
        Object msg = campo(jh, "zh");
        Dialogos.fonte(msg, Estreia.negrito(12f), Estreia.OURO);
        esquerda(msg);
        pos(msg, 29, 492, 370, 22);
        Editor.botao(campo(jh, "uC"), "Cancelar torneio", "fechar", false);
        avancar(campo(jh, "Bw"), "Iniciar torneio");
        pos(campo(jh, "uC"), 29, 527, 190, 38);
        pos(campo(jh, "Bw"), 523, 527, 190, 38);
        jh.revalidate();
        jh.repaint();
    }

    // ------------------------------------------------------------- Ranking de selecoes

    static void selecoes(JPanel jv) {
        if (!Dialogos.base(jv) || !Dialogos.absoluto(jv)) return;
        Object t = campo(jv, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Seleções");
        Dialogos.titulo(t, 30f);
        pos(t, 20, 14, 200, 40);
        Object filtro = campo(jv, "ue");
        combo(filtro);
        pos(filtro, 200, 21, 160, 30);
        Dialogos.fechar(campo(jv, "vm"), 432);
        Object s = campo(jv, "ut");
        Dialogos.rolagem(s);
        pos(s, 20, 64, 392, 532);
        Object tab = campo(jv, "vn");
        if (tab instanceof JTable) {
            final JTable tb = (JTable) tab;
            final Class<?> tipo = tipoDaLinha(tb);
            renderizador(tb, tipo, new Envolve() {
                public TableCellRenderer envolve(TableCellRenderer o) { return new Podio(o); }
            }, new Runnable() {
                public void run() {
                    // o jogo troca o modelo ao mudar o continente; o tipo da linha e o mesmo
                    Class<?> k = tipoDaLinha(tb);
                    if (k != tipo && !(tb.getDefaultRenderer(k) instanceof Nosso)) tb.setDefaultRenderer(k, new Podio(tb.getDefaultRenderer(k)));
                    tb.setRowHeight(24);
                }
            });
        }
        Object barra = campo(jv, "ve");
        if (barra instanceof JToolBar) {
            JToolBar tb = (JToolBar) barra;
            tb.setOpaque(false);
            tb.setFloatable(false);
            tb.setBorder(BorderFactory.createEmptyBorder());
            tb.setLayout(new java.awt.GridLayout(1, 0, 8, 0));
            Dialogos.atalho(campo(jv, "HR"), "Calendário", "calendario");
            Dialogos.atalho(campo(jv, "Ie"), "Títulos", "taca");
            Dialogos.atalho(campo(jv, "HY"), "Retrospecto", "historico");
            for (String b : new String[] {"HR", "Ie", "HY"}) {
                Object o = campo(jv, b);
                if (!(o instanceof JButton)) continue;
                ((JButton) o).setBorder(new EmptyBorder(0, 11, 0, 6));
                ((JButton) o).setIconTextGap(7);
                ((JButton) o).setFont(Estreia.negrito(12f));
            }
        }
        pos(barra, 20, 610, 392, 36);
        jv.revalidate();
    }

    private static Class<?> tipoDaLinha(JTable t) {
        return t.getColumnCount() > 0 ? t.getColumnClass(0) : Object.class;
    }

    // Tres primeiros colocados com a posicao em dourado.
    static class Podio implements TableCellRenderer, Nosso {
        private final TableCellRenderer orig;
        Podio(TableCellRenderer orig) { this.orig = orig; }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!(c instanceof JLabel)) return c;
            JLabel l = (JLabel) c;
            if (t.convertColumnIndexToModel(col) == 0) {
                boolean podio = row < 3;
                l.setFont(podio ? Estreia.negrito(13f) : Estreia.texto(12.5f));
                l.setForeground(podio ? Estreia.OURO : Estreia.FRACO);
                l.setHorizontalAlignment(SwingConstants.CENTER);
            } else if (t.convertColumnIndexToModel(col) == 1) {
                l.setFont(row < 3 ? Estreia.negrito(12.5f) : Estreia.semi(12.5f));
            }
            return l;
        }
    }

    // ------------------------------------------------------------- Expansao do estadio

    static void expansao(JPanel cq) {
        if (!Dialogos.base(cq) || !Dialogos.absoluto(cq)) return;
        JPanel vd = interno(cq, "vd", 438, 360);
        if (vd == null) return;
        Object t = campo(cq, "ug");
        Dialogos.titulo(t, 30f);
        pos(t, 24, 14, 340, 40);
        Dialogos.fechar(campo(cq, "ud"), 438);
        Object pede = campo(cq, "vx");
        if (pede instanceof JLabel) ((JLabel) pede).setText("Quantos lugares construir em cada setor?");
        Dialogos.fonte(pede, Estreia.texto(12.5f), Estreia.FRACO);
        esquerda(pede);
        pos(pede, 25, 54, 390, 18);
        // setor, seletor, maximo
        String[][] setores = {{"uh", "CA", "Cw"}, {"a_", "CC", "Cy"}, {"ur", "CB", "Cx"}, {"us", "CD", "Cz"}};
        for (int i = 0; i < setores.length; i++) {
            int x = 24 + (i % 2) * 201, y = 82 + (i / 2) * 104;
            cartao(vd, x, y, 189, 96);
            Object nome = campo(cq, setores[i][0]);
            String s = texto(nome);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)\\s*\\(atual:\\s*(\\d+)\\)").matcher(s);
            String atual = "";
            if (m.find() && nome instanceof JLabel) {
                ((JLabel) nome).setText(m.group(1));
                atual = "Hoje " + milhar(m.group(2)) + " · ";
            }
            Dialogos.rotulo(nome);
            esquerda(nome);
            pos(nome, x + 14, y + 12, 161, 16);
            final String hoje = atual;
            Object sel = campo(cq, setores[i][1]);
            if (sel instanceof JSpinner) Janelas.seletor((JSpinner) sel);
            pos(sel, x + 14, y + 34, 161, 32);
            Object max = campo(cq, setores[i][2]);
            if (max instanceof JLabel) {
                reescreve((JLabel) max, new Troca() {
                    public String troca(String v) {
                        java.util.regex.Matcher k = java.util.regex.Pattern.compile("(\\d+)").matcher(v);
                        return v.startsWith("Hoje") || !v.startsWith("M") || !k.find() ? v : hoje + "máx. +" + milhar(k.group(1));
                    }
                });
            }
            Dialogos.fonte(max, Estreia.texto(11.5f), Estreia.APAGADO);
            esquerda(max);
            pos(max, x + 14, y + 70, 161, 18);
        }
        Object custo = campo(cq, "zh");
        Dialogos.fonte(custo, Estreia.semi(12f), Estreia.OURO);
        esquerda(custo);
        pos(custo, 25, 290, 390, 30);
        Editor.botao(campo(cq, "Cv"), "Iniciar construção", "obra", true);
        Editor.botao(campo(cq, "Cu"), "Pedir orçamento", "busca", true);
        pos(campo(cq, "Cv"), 24, 316, 186, 36);
        pos(campo(cq, "Cu"), 228, 316, 186, 36);
        cq.revalidate();
    }

    private static String milhar(String n) {
        try {
            return String.format(new java.util.Locale("pt", "BR"), "%,d", Long.parseLong(n));
        } catch (Throwable t) {
            return n;
        }
    }
}
