package mod;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

// Janelas que faltavam no estilo "Noite de estreia": Tecnicos humanos, Ultimas trocas e
// Transferencias, Galeria de trofeus do tecnico, Ranking de clubes, Lideres, Time da
// Rodada/Ano, Bola de Ouro, Mensagens, Financas e Salvar jogo.
final class Outras {
    private Outras() {}

    static boolean aplica(JPanel p, String n) {
        if (n.equals("a.ip")) tecnicos(p);
        else if (n.equals("a.hC")) trocas(p);
        else if (n.equals("a.dg")) trofeusTecnico(p);
        else if (n.equals("a.hc")) rankingClubes(p);
        else if (n.equals("a.y")) lideres(p);
        else if (n.equals("a.iu")) timeDaRodada(p);
        else if (n.equals("a.H")) bolaDeOuro(p);
        else if (n.equals("a.bn")) mensagens(p);
        else if (n.equals("a.cW")) financas(p);
        else if (n.equals("a.fw")) salvar(p);
        else if (n.equals("a.bz")) escalar(p);
        else if (n.equals("a.bl")) demissoes(p);
        else if (n.equals("a.gO")) convites(p);
        else return false;
        return true;
    }

    private static Object campo(Object dono, String nome) { return Dialogos.campo(dono, nome); }
    private static void pos(Object o, int x, int y, int w, int h) { Dialogos.pos(o, x, y, w, h); }

    private static String texto(Object o) {
        return o instanceof JLabel && ((JLabel) o).getText() != null ? ((JLabel) o).getText() : "";
    }

    // ------------------------------------------------------------- Tecnicos humanos

    static void tecnicos(JPanel ip) {
        if (!Dialogos.base(ip) || !Dialogos.absoluto(ip)) return;
        Object t = campo(ip, "ug");
        Dialogos.titulo(t, 28f);
        pos(t, 30, 16, 360, 40);
        Dialogos.fechar(campo(ip, "vm"), 487);
        Object lista = campo(ip, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 30, 70, 428, 290);
        Object tab = campo(ip, "MD");
        if (tab instanceof JTable && ((JTable) tab).getColumnCount() > 0
            && "Tecnico".equals(((JTable) tab).getColumnModel().getColumn(0).getHeaderValue()))
            ((JTable) tab).getColumnModel().getColumn(0).setHeaderValue("Técnico");
        Editor.botao(campo(ip, "yZ"), "Remover selecionado", "lixeira", false);
        pos(campo(ip, "yZ"), 30, 372, 190, 36);
        Editor.botao(campo(ip, "MC"), "Adicionar novo", "mais", true);
        pos(campo(ip, "MC"), 228, 372, 170, 36);
        Object qtd = campo(ip, "uh");
        if (qtd instanceof JLabel) {
            JLabel q = (JLabel) qtd;
            q.setUI(new Fichas.Chip());
            q.setIcon(null);
            q.setFont(Estreia.negrito(12f));
            q.setForeground(Estreia.OURO);
            q.setHorizontalAlignment(SwingConstants.RIGHT);
            q.setToolTipText("Técnicos humanos no jogo");
        }
        pos(qtd, 406, 379, 52, 22);
        ip.revalidate();
    }

    // ------------------------------------------------------------- Ultimas trocas / Transferencias

    static void trocas(JPanel hc) {
        if (!Dialogos.base(hc) || !Dialogos.absoluto(hc)) return;
        Object t = campo(hc, "vf");
        Dialogos.titulo(t, 28f);
        pos(t, 20, 12, 410, 40);
        pos(campo(hc, "GD"), 440, 20, 180, 28);
        Dialogos.fechar(campo(hc, "vm"), 695);
        Object lista = campo(hc, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 20, 66, 655, 414);
        if (texto(t).toLowerCase().contains("transfer"))
            Dialogos.vazio(lista, "Nenhuma transferência ainda", "As compras e vendas de jogadores aparecem aqui.");
        else
            Dialogos.vazio(lista, "Nenhuma troca de técnico ainda", "Quando um clube trocar de técnico, aparece aqui.");
        hc.revalidate();
    }

    // ------------------------------------------------------------- Galeria de trofeus do tecnico

    static void trofeusTecnico(JPanel dg) {
        if (!Dialogos.base(dg)) return;
        Object t = campo(dg, "EC");
        if (t instanceof JLabel) ((JLabel) t).setText("Galeria de troféus");
        Dialogos.titulo(t, 28f);
        pos(t, 30, 16, 210, 40);
        Object nome = campo(dg, "vf");
        if (nome instanceof JLabel) {
            JLabel l = (JLabel) nome;
            l.setText("Técnico · " + l.getText());
            Dialogos.rotulo(l);
        }
        pos(nome, 30, 60, 300, 18);
        Dialogos.fechar(campo(dg, "vm"), 971);
        String[][] colunas = {
            {"EA", "Nenhum título internacional"},
            {"EB", "Nenhum título nacional"},
            {"Ez", "Nenhum título estadual"},
        };
        for (String[] c : colunas) coluna(campo(dg, c[0]), c[1]);
        dg.revalidate();
    }

    // components.l: titulo, separador e lista de titulos.
    private static void coluna(Object o, String vazio) {
        if (!(o instanceof JPanel)) return;
        JPanel l = (JPanel) o;
        Dialogos.cartao(l);
        Object titulo = campo(l, "Ov");
        Dialogos.rotulo(titulo);
        pos(titulo, 16, 14, 280, 16);
        Object sep = campo(l, "BE");
        if (sep instanceof Component) ((Component) sep).setVisible(false);
        Object lista = campo(l, "ut");
        if (lista instanceof JScrollPane) {
            JScrollPane s = (JScrollPane) lista;
            s.setOpaque(false);
            s.getViewport().setOpaque(false);
            Component v = s.getViewport().getView();
            if (v instanceof JComponent) ((JComponent) v).setOpaque(false);
        }
        pos(lista, 8, 40, 294, 470);
        Dialogos.vazio(lista, vazio, "As taças que o técnico ganhar aparecem aqui.");
    }

    // ------------------------------------------------------------- Ranking de clubes

    static void rankingClubes(JPanel hc) {
        if (!Dialogos.base(hc) || !Dialogos.absoluto(hc)) return;
        Object t = campo(hc, "vf");
        Dialogos.titulo(t, 28f);
        pos(t, 23, 12, 236, 40);
        Dialogos.fechar(campo(hc, "vm"), 825);
        Object lista = campo(hc, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 23, 64, 408, 525);
        Object vd = campo(hc, "vd");
        Dialogos.cartao(vd);
        if (vd instanceof Container) Dialogos.absoluto((Container) vd);
        pos(vd, 449, 64, 359, 66);
        Object clube = campo(hc, "uu");
        Dialogos.fonte(clube, Estreia.titulo(24f), Estreia.BRANCO);
        pos(clube, 16, 8, 330, 28);
        Object pontos = campo(hc, "xI");
        Dialogos.fonte(pontos, Estreia.semi(12.5f), Estreia.OURO);
        pos(pontos, 16, 38, 330, 20);
        Object comp = campo(hc, "wi");
        Dialogos.rolagem(comp);
        pos(comp, 449, 142, 359, 402);
        Dialogos.vazio(comp, "Sem pontos ainda", "As competições com pontos aparecem aqui.");
        Editor.botao(campo(hc, "Ie"), "Troféus e títulos", "taca", true);
        pos(campo(hc, "Ie"), 449, 554, 359, 36);
        hc.revalidate();
    }

    // ------------------------------------------------------------- Lideres (assistencias etc.)

    static void lideres(JPanel y) {
        if (!Dialogos.base(y) || !Dialogos.absoluto(y)) return;
        Object t = campo(y, "vf");
        Dialogos.titulo(t, 28f);
        pos(t, 20, 14, 320, 40);
        pos(campo(y, "ue"), 340, 22, 180, 28);
        Dialogos.fechar(campo(y, "vm"), 596);
        Object lista = campo(y, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 20, 66, 556, 526);
        Dialogos.vazio(lista, "Nenhum líder ainda", "Os líderes aparecem aqui depois das primeiras rodadas.");
        y.revalidate();
    }

    // ------------------------------------------------------------- Time da Rodada / Time do Ano

    // O gramado novo vem das imagens aiconsj/campo_diminuido*.jpg (tools/escalar/diminuidos.py).
    static void timeDaRodada(JPanel iu) {
        if (!Dialogos.base(iu)) return;
        Object t = campo(iu, "vf");
        Dialogos.titulo(t, 26f);
        pos(t, 16, 8, 280, 36);
        Dialogos.fechar(campo(iu, "vm"), 372);
        pos(campo(iu, "ue"), 15, 46, 118, 28);
        pos(campo(iu, "vv"), 139, 46, 104, 28);
        pos(campo(iu, "MG"), 249, 46, 108, 28);
        iu.revalidate();
    }

    // ------------------------------------------------------------- Bola de Ouro

    static void bolaDeOuro(JPanel h) {
        if (!Dialogos.base(h)) return;
        // rotulo, lista, coluna, linha (grade 3x3; o canto de cima a direita e da bola)
        String[][] quadros = {
            {"ug", "vB", "0", "0"}, {"vx", "vG", "1", "0"}, {"uh", "vC", "0", "1"}, {"us", "vF", "1", "1"},
            {"vz", "vI", "2", "1"}, {"a_", "vD", "0", "2"}, {"ur", "vE", "1", "2"}, {"vy", "vH", "2", "2"},
        };
        int[] xs = {10, 345, 680}, ys = {12, 238, 457};
        for (String[] q : quadros) {
            Object rotulo = campo(h, q[0]);
            Object lista = campo(h, q[1]);
            if (!(rotulo instanceof JLabel) || !(lista instanceof JScrollPane)) continue;
            JLabel l = (JLabel) rotulo;
            int x = xs[Integer.parseInt(q[2])], y = ys[Integer.parseInt(q[3])];
            boolean ouro = "vy".equals(q[0]);
            if (ouro) {
                l.setUI(new Fichas.Chip());
                l.setOpaque(false);
                l.setFont(Estreia.negrito(11.5f));
                l.setForeground(Estreia.OURO);
                l.setText("Bola de Ouro");
            } else {
                Dialogos.rotulo(l);
            }
            l.setHorizontalAlignment(SwingConstants.LEFT);
            JScrollPane s = (JScrollPane) lista;
            s.setBorder(BorderFactory.createLineBorder(ouro ? Tema.alfa(Estreia.OURO, 110) : new Color(244, 241, 234, 30)));
            Dialogos.vazio(s, ouro ? "Ainda sem vencedor" : "Sem indicados ainda", "Sai no fim da temporada.");
            pos(l, x + 2, y + 2, ouro ? 120 : 320, 22);
            pos(s, x, y + 28, 325, 180);
        }
        Dialogos.fechar(campo(h, "vm"), 1020);
        pos(campo(h, "ue"), 760, 158, 180, 28);
        pos(campo(h, "vv"), 760, 192, 180, 28);
        h.revalidate();
    }

    // ------------------------------------------------------------- Central de Mensagens

    static void mensagens(JPanel bn) {
        if (!Dialogos.base(bn) || !Dialogos.absoluto(bn)) return;
        Object t = campo(bn, "ug");
        if (t instanceof JLabel) ((JLabel) t).setIcon(null);
        Dialogos.titulo(t, 28f);
        pos(t, 23, 12, 500, 40);
        Dialogos.fechar(campo(bn, "vm"), 894);
        Object lista = campo(bn, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 23, 66, 401, 300);
        Dialogos.vazio(lista, "Nenhuma mensagem", "As mensagens da diretoria aparecem aqui.");
        Object vd = campo(bn, "vd");
        Dialogos.cartao(vd);
        pos(vd, 440, 66, 431, 300);
        Object texto = campo(bn, "Ac");
        Dialogos.fonte(texto, Estreia.texto(13.5f), Estreia.BRANCO);
        if (texto instanceof JLabel) ((JLabel) texto).setVerticalAlignment(SwingConstants.TOP);
        Dialogos.fonte(campo(bn, "Ab"), Estreia.semi(12.5f), Estreia.OURO);
        Editor.botao(campo(bn, "Aa"), "Multar", null, true);
        Editor.botao(campo(bn, "zZ"), "Não multar", null, false);
        bn.revalidate();
    }

    // ------------------------------------------------------------- Financas

    static void financas(JPanel cw) {
        if (!Dialogos.base(cw) || !Dialogos.absoluto(cw)) return;
        Object t = campo(cw, "ug");
        Dialogos.titulo(t, 28f);
        pos(t, 21, 12, 290, 40);
        Dialogos.fechar(campo(cw, "vm"), 398);
        Object lista = campo(cw, "ut");
        Dialogos.cartao(lista);
        if (lista instanceof JScrollPane) {
            JScrollPane s = (JScrollPane) lista;
            s.getViewport().setOpaque(false);
            Component v = s.getViewport().getView();
            if (v instanceof JTable) {
                JTable tab = (JTable) v;
                tab.setOpaque(false);
                tab.setRowHeight(21);
                if (tab.getColumnCount() == 2) {
                    tab.getColumnModel().getColumn(0).setPreferredWidth(230);
                    tab.getColumnModel().getColumn(1).setPreferredWidth(110);
                }
                tab.setDefaultRenderer(components.j.class, new Extrato(tab.getDefaultRenderer(components.j.class)));
            }
        }
        pos(lista, 21, 62, 358, 350);
        for (String s : new String[] {"Ec", "DZ"}) {
            Object o = campo(cw, s);
            if (o instanceof JLabel) ((JLabel) o).setUI(new Valor());
            Dialogos.fonte(o, Estreia.semi(13f), Estreia.BRANCO);
        }
        pos(campo(cw, "Ec"), 25, 420, 350, 20);
        pos(campo(cw, "DZ"), 25, 442, 350, 20);
        Object vd = campo(cw, "vd");
        Dialogos.cartao(vd);
        if (vd instanceof Container && Dialogos.absoluto((Container) vd)) {
            Object kicker = campo(cw, "uh");
            Dialogos.rotulo(kicker);
            pos(kicker, 16, 14, 326, 16);
            for (String s : new String[] {"Ea", "Eb"}) {
                Object o = campo(cw, s);
                if (o instanceof JLabel) ((JLabel) o).setUI(new Valor());
                Dialogos.fonte(o, Estreia.semi(12.5f), Estreia.BRANCO);
            }
            pos(campo(cw, "Ea"), 16, 40, 326, 20);
            pos(campo(cw, "Eb"), 16, 64, 326, 20);
            Editor.botao(campo(cw, "DY"), null, "emprestimo", true);
            Editor.botao(campo(cw, "DX"), null, "contrato", false);
            pos(campo(cw, "DY"), 14, 100, 160, 36);
            pos(campo(cw, "DX"), 184, 100, 160, 36);
        }
        pos(vd, 21, 472, 358, 150);
        cw.revalidate();
    }

    // "Rotulo: valor" com o rotulo apagado a esquerda e o valor em destaque a direita.
    static class Valor extends javax.swing.plaf.basic.BasicLabelUI {
        @Override
        public void paint(java.awt.Graphics g0, JComponent c) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            JLabel l = (JLabel) c;
            String s = l.getText() == null ? "" : l.getText();
            int i = s.indexOf(':');
            String a = i < 0 ? s : s.substring(0, i), b = i < 0 ? "" : s.substring(i + 1).trim();
            java.awt.FontMetrics fm = g.getFontMetrics(l.getFont());
            int y = (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setFont(Estreia.texto(l.getFont().getSize2D()));
            g.setColor(Estreia.FRACO);
            g.drawString(a, 0, y);
            g.setFont(l.getFont());
            g.setColor(Estreia.BRANCO);
            g.drawString(b, c.getWidth() - fm.stringWidth(b), y);
            g.dispose();
        }
    }

    // Extrato: secoes (Receitas/Despesas) em rotulo dourado, linhas com o nome apagado a
    // esquerda e o valor a direita, totais em negrito e o saldo em verde ou vermelho.
    static class Extrato implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer orig;
        Extrato(javax.swing.table.TableCellRenderer orig) { this.orig = orig; }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, false, false, row, col);
            if (!(c instanceof JLabel) || !(v instanceof components.j)) return c;
            JLabel l = (JLabel) c;
            components.j j = (components.j) v;
            l.setOpaque(false);
            l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            String nome = j.sT() == null ? "" : j.sT().trim();
            if (j.tt()) {
                // titulo de secao: so na primeira coluna
                l.setText(col == 0 ? nome.toUpperCase() : "");
                l.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.18f));
                l.setForeground(Estreia.OURO);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
            boolean saldo = nome.toLowerCase().startsWith("saldo");
            if (col == 0) {
                if (nome.endsWith(":")) nome = nome.substring(0, nome.length() - 1);
                l.setText(nome);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setFont(j.isBold() ? Estreia.negrito(12.5f) : Estreia.texto(12.5f));
                l.setForeground(j.isBold() ? Estreia.BRANCO : Estreia.FRACO);
            } else {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(j.isBold() ? Estreia.negrito(12.5f) : Estreia.semi(12.5f));
                l.setForeground(saldo ? (j.ts() < 0 ? new Color(0xFF8A6E) : new Color(0x7CCB7A)) : Estreia.BRANCO);
            }
            return l;
        }
    }

    // ------------------------------------------------------------- Salvar jogo

    static void salvar(JPanel fw) {
        if (!Dialogos.base(fw) || !Dialogos.absoluto(fw)) return;
        Object vd = campo(fw, "vd");
        if (!(vd instanceof JPanel) || !Dialogos.absoluto((Container) vd)) return;
        ((JPanel) vd).setOpaque(false);
        ((JPanel) vd).setBorder(null);
        pos(vd, 0, 0, 494, 192);
        JLabel titulo = new JLabel("Salvar jogo");
        Dialogos.titulo(titulo, 28f);
        Dialogos.adiciona(fw, titulo, 24, 18, 440, 40);
        Object pede = campo(fw, "ug");
        if (pede instanceof JLabel) ((JLabel) pede).setText("Nome do arquivo");
        Dialogos.rotulo(pede);
        if (pede instanceof JLabel) ((JLabel) pede).setHorizontalAlignment(SwingConstants.LEFT);
        pos(pede, 26, 76, 300, 16);
        Object nome = campo(fw, "IA");
        if (nome instanceof javax.swing.JTextField) Editor.texto((javax.swing.JTextField) nome);
        pos(nome, 24, 98, 300, 36);
        Editor.botao(campo(fw, "zn"), "Salvar", "disquete", true);
        pos(campo(fw, "zn"), 336, 97, 134, 38);
        fw.revalidate();
    }

    // ------------------------------------------------------------- Escalar time (rodape)

    // O gramado e as camisas ja sao novos (Escalacao); aqui ficam os rotulos, o cartao do
    // adversario e a condicao do gramado como chip colorido.
    static void escalar(JPanel bz) {
        if (bz.getClientProperty(Janelas.FEITO) != null) return;
        bz.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        for (String s : new String[] {"vA", "vy"}) {
            Object o = campo(bz, s);
            if (o instanceof JLabel) ((JLabel) o).setOpaque(false);
            Dialogos.rotulo(o);
        }
        pos(campo(bz, "vA"), 447, 2, 400, 18);
        pos(campo(bz, "vy"), 447, 187, 400, 18);
        Dialogos.interruptor(campo(bz, "Be"));
        pos(campo(bz, "Be"), 270, 628, 165, 24);
        // rotulos curtos: os originais nao cabem em caixa alta sobre os seletores
        String[][] curtos = {{"ug", "Automática"}, {"uh", "Estilo de jogo"}, {"a_", "Marcação"}, {"ur", "Ataques"}};
        for (String[] r : curtos) {
            Object o = campo(bz, r[0]);
            if (o instanceof JLabel) ((JLabel) o).setText(r[1]);
            Dialogos.rotulo(o);
        }
        Object we = campo(bz, "we");
        Dialogos.cartao(we);
        pos(we, 10, 658, 430, 76);
        Object zc = campo(bz, "zc");
        if (zc instanceof JLabel) ((JLabel) zc).setText("Gramado");
        for (String s : new String[] {"vw", "zc"}) {
            Dialogos.rotulo(campo(bz, s));
            Object o = campo(bz, s);
            if (o instanceof JLabel) ((JLabel) o).setHorizontalAlignment(SwingConstants.LEFT);
        }
        pos(campo(bz, "Bj"), 10, 14, 40, 48);
        pos(campo(bz, "vw"), 56, 18, 120, 16);
        Object rival = campo(bz, "Bk");
        Dialogos.fonte(rival, Estreia.semi(12.5f), Estreia.BRANCO);
        if (rival instanceof JLabel) ((JLabel) rival).setHorizontalAlignment(SwingConstants.LEFT);
        pos(rival, 56, 38, 142, 22);
        pos(campo(bz, "zc"), 202, 18, 100, 16);
        Object gramado = campo(bz, "Bm");
        if (gramado instanceof JLabel) {
            JLabel g = (JLabel) gramado;
            g.setUI(new Gramado());
            g.setFont(Estreia.negrito(11.5f));
            g.setHorizontalAlignment(SwingConstants.LEFT);
        }
        pos(gramado, 202, 37, 100, 22);
        Titulares.instala(bz);
    }

    // Condicao do gramado: chip verde (boa), amarelo (ruim) ou vermelho (precaria/pessima).
    static class Gramado extends javax.swing.plaf.basic.BasicLabelUI {
        @Override
        public void paint(java.awt.Graphics g0, JComponent c) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            JLabel l = (JLabel) c;
            String s = l.getText() == null ? "" : l.getText().trim();
            String k = s.toLowerCase();
            Color cor = k.startsWith("prec") || k.startsWith("p\u00e9s") || k.startsWith("pes") ? new Color(0xFF8A6E)
                : k.startsWith("ruim") || k.startsWith("regular") ? new Color(0xF2C14E) : new Color(0x7CCB7A);
            java.awt.FontMetrics fm = g.getFontMetrics(l.getFont());
            int h = c.getHeight(), tw = fm.stringWidth(s), w = Math.min(c.getWidth(), tw + 24);
            java.awt.geom.RoundRectangle2D r = new java.awt.geom.RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, h - 1, h - 1);
            g.setColor(Tema.alfa(cor, 34));
            g.fill(r);
            g.setColor(Tema.alfa(cor, 140));
            g.draw(r);
            g.setFont(l.getFont());
            g.setColor(cor);
            g.drawString(s, (w - tw) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Demissoes na rodada

    static void demissoes(JPanel bl) {
        if (!Dialogos.base(bl) || !Dialogos.absoluto(bl)) return;
        Object o = campo(bl, "vd");
        if (!(o instanceof JPanel)) return;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(null);
        pos(vd, 0, 0, 608, 304);
        if (!Dialogos.absoluto(vd)) return;
        Object t = campo(bl, "ug");
        if (t instanceof JLabel) ((JLabel) t).setText("Demissões na rodada");
        Dialogos.titulo(t, 28f);
        pos(t, 24, 16, 400, 40);
        Object seguir = campo(bl, "zK");
        Editor.botao(seguir, "Continuar", "seta", true);
        if (seguir instanceof javax.swing.AbstractButton) ((javax.swing.AbstractButton) seguir).setHorizontalTextPosition(SwingConstants.LEFT);
        pos(seguir, 452, 18, 132, 36);
        Object lista = campo(bl, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 24, 68, 560, 214);
        Object tab = campo(bl, "zV");
        if (tab instanceof JTable) {
            JTable tb = (JTable) tab;
            tb.setRowHeight(24);
            int[] larg = {128, 140, 150, 140};
            for (int i = 0; i < larg.length && i < tb.getColumnCount(); i++) tb.getColumnModel().getColumn(i).setPreferredWidth(larg[i]);
            tb.setDefaultRenderer(components.h.class, new Demissao(tb.getDefaultRenderer(components.h.class)));
        }
        bl.revalidate();
    }

    // Demitido apagado, novo tecnico com seta e o motivo como chip colorido.
    static class Demissao implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer orig;
        private final MotivoChip chip = new MotivoChip();
        Demissao(javax.swing.table.TableCellRenderer orig) { this.orig = orig; }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!(c instanceof JLabel)) return c;
            JLabel l = (JLabel) c;
            int m = t.convertColumnIndexToModel(col);
            if (m == 1) {
                l.setForeground(Estreia.FRACO);
                l.setFont(Estreia.texto(12.5f));
            } else if (m == 2) {
                l.setForeground(Estreia.BRANCO);
                l.setFont(Estreia.semi(12.5f));
                l.setIcon(new Estreia.Icone("seta", 12, Estreia.OURO));
                l.setIconTextGap(6);
            } else if (m == 3) {
                String s = l.getText() == null ? "" : l.getText().trim();
                chip.texto = s.isEmpty() ? "Pediu demissão" : s.equals("Pressão torcida") ? "Pressão da torcida" : s;
                chip.cor = s.startsWith("Crise") ? new Color(0xFF8A6E) : s.startsWith("Press") ? new Color(0xF2C14E)
                    : s.isEmpty() ? new Color(0x6CB8E6) : new Color(0xE0A050);
                chip.setBackground(l.getBackground());
                chip.setOpaque(l.isOpaque());
                return chip;
            }
            return l;
        }
    }

    static class MotivoChip extends JComponent {
        String texto = "";
        Color cor = Estreia.OURO;
        @Override
        protected void paintComponent(java.awt.Graphics g0) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) g0.create();
            int w = getWidth(), h = getHeight();
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, w, h);
            }
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            java.awt.Font f = Estreia.negrito(11f);
            java.awt.FontMetrics fm = g.getFontMetrics(f);
            int tw = fm.stringWidth(texto), cw = Math.min(w - 8, tw + 18), ch = Math.min(18, h - 4);
            java.awt.geom.RoundRectangle2D r = new java.awt.geom.RoundRectangle2D.Float(4.5f, (h - ch) / 2f, cw, ch, ch, ch);
            g.setColor(Tema.alfa(cor, 34));
            g.fill(r);
            g.setColor(Tema.alfa(cor, 140));
            g.draw(r);
            g.setFont(f);
            g.setColor(cor);
            g.drawString(texto, 4.5f + (cw - tw) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // ------------------------------------------------------------- Convites para ser tecnico

    static void convites(JPanel go) {
        if (!Dialogos.base(go) || !Dialogos.absoluto(go)) return;
        Object o = campo(go, "vd");
        if (!(o instanceof JPanel)) return;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(null);
        pos(vd, 0, 0, 774, 462);
        if (!Dialogos.absoluto(vd)) return;
        Object t = campo(go, "ug");
        Dialogos.titulo(t, 28f);
        if (t instanceof JLabel) ((JLabel) t).setHorizontalAlignment(SwingConstants.LEFT);
        pos(t, 28, 16, 700, 40);
        Object nome = campo(go, "Fs");
        if (nome instanceof JLabel) {
            ((JLabel) nome).setText("Técnico · " + ((JLabel) nome).getText());
            Dialogos.rotulo(nome);
            ((JLabel) nome).setHorizontalAlignment(SwingConstants.LEFT);
        }
        pos(nome, 28, 58, 400, 16);
        Object recebidos = campo(go, "ur");
        if (recebidos instanceof JLabel) ((JLabel) recebidos).setText("Convites recebidos");
        Dialogos.rotulo(recebidos);
        pos(recebidos, 28, 96, 300, 16);
        Object lista = campo(go, "wi");
        Dialogos.rolagem(lista);
        pos(lista, 28, 118, 430, 330);
        Object tab = campo(go, "vn");
        if (tab instanceof JTable) ((JTable) tab).setRowHeight(26);

        JPanel c = new JPanel(null);
        Dialogos.cartao(c);
        Dialogos.fundo(vd, c, 474, 118, 272, 330);
        Dialogos.adiciona(vd, Dialogos.rotulo("Time escolhido"), 490, 134, 240, 16);
        Object time = campo(go, "uu");
        Dialogos.fonte(time, Estreia.titulo(22f), Estreia.BRANCO);
        if (time instanceof JLabel) {
            ((JLabel) time).setIconTextGap(8);
            ((JLabel) time).setHorizontalAlignment(SwingConstants.LEFT);
        }
        pos(time, 490, 154, 240, 40);
        Dialogos.adiciona(vd, Dialogos.rotulo("Ver do time"), 490, 206, 240, 16);
        Object atalhos = campo(go, "ve");
        if (atalhos instanceof javax.swing.JToolBar) {
            javax.swing.JToolBar tb = (javax.swing.JToolBar) atalhos;
            tb.setOpaque(false);
            tb.setFloatable(false);
            tb.setBorder(BorderFactory.createEmptyBorder());
            tb.setLayout(new java.awt.GridLayout(0, 1, 0, 8));
            Dialogos.atalho(campo(go, "HR"), "Calendário", "calendario");
            Dialogos.atalho(campo(go, "aeN"), "Tabelas", "todos");
            Dialogos.atalho(campo(go, "HY"), "Retrospecto", "historico");
        }
        pos(atalhos, 490, 228, 177, 118);
        Editor.botao(campo(go, "Ki"), null, "maleta", true);
        pos(campo(go, "Ki"), 488, 358, 244, 36);
        Editor.botao(campo(go, "Kj"), null, "fechar", false);
        pos(campo(go, "Kj"), 488, 400, 244, 36);

        // "Equipe atual" vira "Elenco do <time escolhido>"
        final Object elenco = campo(go, "uh");
        Dialogos.rotulo(elenco);
        if (elenco instanceof JLabel && time instanceof JLabel) {
            final JLabel e = (JLabel) elenco;
            final JLabel tl = (JLabel) time;
            Runnable r = new Runnable() {
                public void run() {
                    String n = tl.getText() == null ? "" : tl.getText().trim();
                    e.setText(n.isEmpty() ? "ELENCO" : ("ELENCO · " + n).toUpperCase());
                }
            };
            r.run();
            final Runnable rr = r;
            tl.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent ev) { rr.run(); }
            });
        }
        pos(elenco, 28, 476, 600, 16);
        Object ut = campo(go, "ut");
        Dialogos.rolagem(ut);
        pos(ut, 28, 500, 718, 206);
        Object zj = campo(go, "zj");
        if (zj instanceof JTable) ((JTable) zj).putClientProperty(Principal.POSICAO, 0);
        go.revalidate();
    }
}
