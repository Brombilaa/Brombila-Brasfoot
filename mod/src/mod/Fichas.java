package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicLabelUI;

// Janelas do jogador e do time no estilo "Noite de estreia": ficha do jogador (historico),
// Retrospecto, Emprestimos, Vender jogador, Novo contrato, Confronto com o adversario,
// Marcar amistoso e Cores do time. Usa as pecas de Dialogos.
final class Fichas {
    private Fichas() {}

    static void aplica(JPanel p, String n) {
        if (n.equals("a.dk")) ficha(p);
        else if (n.equals("a.hw")) retrospecto(p);
        else if (n.equals("a.bs")) emprestimos(p);
        else if (n.equals("a.jc")) vender(p);
        else if (n.equals("a.fy")) contrato(p);
        else if (n.equals("a.aL")) confronto(p);
        else if (n.equals("a.h")) amistoso(p);
        else if (n.equals("a.bb")) cores(p);
        else if (n.equals("a.eg")) Principal.aplica(p);
    }

    private static Object campo(Object dono, String nome) { return Dialogos.campo(dono, nome); }
    private static void pos(Object o, int x, int y, int w, int h) { Dialogos.pos(o, x, y, w, h); }

    private static void texto(Object o, float px, Color cor) { Dialogos.fonte(o, Estreia.texto(px), cor); }
    private static void semi(Object o, float px, Color cor) { Dialogos.fonte(o, Estreia.semi(px), cor); }

    private static void esquerda(Object o) {
        if (o instanceof JLabel) ((JLabel) o).setHorizontalAlignment(SwingConstants.LEFT);
    }

    private static void centro(Object o) {
        if (o instanceof JLabel) ((JLabel) o).setHorizontalAlignment(SwingConstants.CENTER);
    }

    // Titulo grande de janela pequena (sem o filete de duas cores).
    private static void tituloSimples(Object o, float px) {
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setOpaque(false);
        l.setFont(Estreia.titulo(px));
        l.setForeground(Estreia.BRANCO);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        String t = l.getText();
        if (t != null) l.setText(t.toUpperCase());
    }

    // Lista feita de paineis (uma linha por componente): mensagem quando so ha o cabecalho.
    private static void vazioPainel(Object rolagem, String titulo, String texto, int fixas) {
        if (!(rolagem instanceof JScrollPane)) return;
        Component v = ((JScrollPane) rolagem).getViewport().getView();
        if (!(v instanceof JComponent)) return;
        Dialogos.Vazio b = new Dialogos.Vazio(titulo, texto);
        b.linhasFixas = fixas;
        ((JComponent) v).setBorder(b);
    }

    // Cabecalho das listas feitas de paineis: rotulos discretos, como os das tabelas.
    private static void cabecalho(Object rolagem) {
        if (!(rolagem instanceof JScrollPane)) return;
        Component v = ((JScrollPane) rolagem).getViewport().getView();
        if (!(v instanceof Container) || ((Container) v).getComponentCount() == 0) return;
        rotulos(((Container) v).getComponent(0));
    }

    private static void rotulos(Component c) {
        if (c instanceof JLabel) {
            ((JLabel) c).setFont(Estreia.semi(12f));
            ((JLabel) c).setForeground(Estreia.FRACO);
        } else if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) rotulos(k);
        }
    }

    // ------------------------------------------------------------- ficha do jogador

    static void ficha(JPanel dk) {
        if (!Dialogos.base(dk) || !Dialogos.absoluto(dk)) return;
        Object topo = campo(dk, "vd");
        if (topo instanceof JPanel && Dialogos.absoluto((JPanel) topo)) {
            JPanel vd = (JPanel) topo;
            vd.setOpaque(false);
            vd.setBorder(new EmptyBorder(0, 0, 0, 0));
            Object nome = campo(dk, "ug");
            if (nome instanceof JLabel) ((JLabel) nome).setOpaque(false);
            Dialogos.titulo(nome, 28f);
            pos(nome, 24, 14, 650, 40);
            Object x = campo(dk, "vb");
            Janelas.fechar(x);
            pos(x, 726, 16, 36, 36);
        }
        pos(topo, 0, 0, 786, 64);
        Object a = campo(dk, "a_"), b = campo(dk, "uh");
        for (Object o : new Object[] {a, b}) {
            if (o instanceof JLabel) ((JLabel) o).setOpaque(false);
            Dialogos.rotulo(o);
            esquerda(o);
        }
        pos(a, 24, 70, 370, 16);
        pos(b, 412, 70, 350, 16);
        Object hist = campo(dk, "wi"), jogos = campo(dk, "zf");
        Dialogos.rolagem(hist);
        Dialogos.rolagem(jogos);
        pos(hist, 24, 92, 372, 484);
        pos(jogos, 412, 92, 350, 484);
        Dialogos.vazio(jogos, "Sem jogos ainda", "Os jogos da temporada aparecem aqui.");
        dk.revalidate();
    }

    // ------------------------------------------------------------- retrospecto do time

    static void retrospecto(JPanel hw) {
        if (!Dialogos.base(hw)) return;
        Object t = campo(hw, "ug");
        Dialogos.titulo(t, 28f);
        pos(t, 24, 14, 600, 40);
        Dialogos.fechar(campo(hw, "vm"), 740);
        Object lista = campo(hw, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 24, 66, 692, 594);
        cabecalho(lista);
        vazioPainel(lista, "Sem temporadas ainda", "O desempenho de cada temporada aparece aqui.", 1);
        hw.revalidate();
    }

    // ------------------------------------------------------------- emprestimos

    static void emprestimos(JPanel bs) {
        if (!Dialogos.base(bs)) return;
        Object a = campo(bs, "a_"), b = campo(bs, "uh");
        tituloSimples(a, 22f);
        tituloSimples(b, 22f);
        pos(a, 24, 18, 440, 30);
        pos(b, 24, 268, 440, 30);
        for (String s : new String[] {"An", "Ao"}) {
            Object o = campo(bs, s);
            if (o instanceof JLabel) {
                JLabel l = (JLabel) o;
                l.setUI(new Chip());
                l.setFont(Estreia.negrito(11.5f));
                l.setForeground(Estreia.OURO);
                l.setHorizontalAlignment(SwingConstants.CENTER);
            }
        }
        pos(campo(bs, "An"), 474, 20, 190, 26);
        pos(campo(bs, "Ao"), 524, 270, 190, 26);
        Dialogos.fechar(campo(bs, "Am"), 737);
        Object aqui = campo(bs, "ut"), fora = campo(bs, "wi");
        Dialogos.rolagem(aqui);
        Dialogos.rolagem(fora);
        pos(aqui, 24, 58, 690, 186);
        pos(fora, 24, 308, 690, 268);
        cabecalho(aqui);
        cabecalho(fora);
        vazioPainel(aqui, "Nenhum jogador emprestado", "Jogadores de outros times emprestados ao seu aparecem aqui.", 1);
        vazioPainel(fora, "Nenhum jogador seu fora", "Seus jogadores emprestados a outros times aparecem aqui.", 1);
        bs.revalidate();
    }

    // Etiqueta-chip: texto dourado dentro de uma pilula com contorno.
    static class Chip extends BasicLabelUI {
        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            JLabel l = (JLabel) c;
            String s = l.getText() == null ? "" : l.getText().trim();
            FontMetrics fm = g.getFontMetrics(l.getFont());
            int h = c.getHeight(), tw = fm.stringWidth(s), w = Math.min(c.getWidth(), tw + 24);
            int x0 = l.getHorizontalAlignment() == SwingConstants.RIGHT ? c.getWidth() - w
                : l.getHorizontalAlignment() == SwingConstants.LEFT ? 0 : (c.getWidth() - w) / 2;
            RoundRectangle2D r = new RoundRectangle2D.Float(x0 + 0.5f, 0.5f, w - 1, h - 1, h - 1, h - 1);
            g.setColor(Tema.alfa(Estreia.OURO, 22));
            g.fill(r);
            g.setColor(Tema.alfa(Estreia.OURO, 120));
            g.draw(r);
            g.setFont(l.getFont());
            g.setColor(l.getForeground());
            g.drawString(s, x0 + (w - tw) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // ------------------------------------------------------------- vender jogador

    static void vender(JPanel jc) {
        if (!Dialogos.base(jc) || !Dialogos.absoluto(jc)) return;
        Object o = campo(jc, "vd");
        if (!(o instanceof JPanel)) return;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(new EmptyBorder(0, 0, 0, 0));
        pos(vd, 0, 0, 395, 420);
        if (!Dialogos.absoluto(vd)) return;
        Object t = campo(jc, "ug");
        tituloSimples(t, 26f);
        pos(t, 24, 16, 340, 34);
        Object nome = campo(jc, "uh");
        semi(nome, 13f, Estreia.OURO);
        esquerda(nome);
        pos(nome, 24, 50, 347, 20);
        String[] radios = {"Km", "Kn"};
        for (int i = 0; i < radios.length; i++) {
            Object r = campo(jc, radios[i]);
            if (r instanceof JRadioButton) {
                JRadioButton b = (JRadioButton) r;
                b.setUI(new ConfigEstreia.Pilula());
                b.setOpaque(false);
                b.setFocusPainted(false);
                b.setText(b.getText().trim());
                b.setFont(Estreia.semi(12.5f));
            }
            pos(r, 24, 84 + i * 38, 347, 32);
        }
        Object rotulo = campo(jc, "uI");
        semi(rotulo, 12f, Estreia.FRACO);
        esquerda(rotulo);
        pos(rotulo, 24, 170, 347, 18);
        Object campoPreco = campo(jc, "uE");
        if (campoPreco instanceof JTextField) Editor.texto((JTextField) campoPreco);
        pos(campoPreco, 24, 192, 347, 30);
        Object valor = campo(jc, "uH");
        Dialogos.fonte(valor, Estreia.negrito(13f), Estreia.OURO);
        esquerda(valor);
        pos(valor, 24, 228, 347, 20);
        Object ok = campo(jc, "vb"), cancela = campo(jc, "uC");
        Editor.botao(ok, null, "vender", true);
        Editor.botao(cancela, null, null, false);
        if (ok instanceof JButton && "Iniciar leilao".equals(((JButton) ok).getText())) ((JButton) ok).setText("Iniciar leilão");
        pos(ok, 24, 264, 186, 36);
        pos(cancela, 220, 264, 151, 36);
        Object aviso = campo(jc, "us"), multa = campo(jc, "Nn");
        texto(aviso, 11.5f, Estreia.FRACO);
        semi(multa, 11.5f, Estreia.BRANCO);
        esquerda(aviso);
        esquerda(multa);
        pos(aviso, 24, 322, 347, 18);
        pos(multa, 24, 342, 347, 18);
        jc.revalidate();
    }

    // ------------------------------------------------------------- novo contrato

    static void contrato(JPanel fy) {
        if (!Dialogos.base(fy) || !Dialogos.absoluto(fy)) return;
        Object o = campo(fy, "vd");
        if (!(o instanceof JPanel)) return;
        JPanel vd = (JPanel) o;
        vd.setOpaque(false);
        vd.setBorder(new EmptyBorder(0, 0, 0, 0));
        pos(vd, 0, 0, 357, 330);
        if (!Dialogos.absoluto(vd)) return;
        Object t = campo(fy, "ug");
        tituloSimples(t, 26f);
        pos(t, 24, 16, 310, 34);
        Object nome = campo(fy, "uF");
        semi(nome, 13f, Estreia.OURO);
        esquerda(nome);
        pos(nome, 24, 50, 310, 20);
        Object ate = campo(fy, "uG"), atual = campo(fy, "Ec");
        texto(ate, 12.5f, Estreia.BRANCO);
        texto(atual, 12.5f, Estreia.BRANCO);
        esquerda(ate);
        esquerda(atual);
        pos(ate, 24, 82, 310, 20);
        pos(atual, 24, 104, 310, 20);
        Object rot = campo(fy, "uI");
        semi(rot, 12f, Estreia.FRACO);
        esquerda(rot);
        pos(rot, 24, 138, 310, 18);
        Object sal = campo(fy, "IE");
        if (sal instanceof JSpinner) Janelas.seletor((JSpinner) sal);
        pos(sal, 24, 160, 180, 30);
        Object dur = campo(fy, "uH");
        semi(dur, 12f, Estreia.FRACO);
        esquerda(dur);
        pos(dur, 24, 210, 80, 20);
        pos(campo(fy, "Bf"), 100, 206, 150, 28);
        Object cancela = campo(fy, "uC"), ok = campo(fy, "ID");
        Editor.botao(cancela, null, null, false);
        Editor.botao(ok, null, "contrato", true);
        pos(cancela, 24, 262, 130, 36);
        pos(ok, 164, 262, 169, 36);
        fy.revalidate();
    }

    // ------------------------------------------------------------- confronto com o adversario

    static void confronto(JPanel al) {
        if (!Dialogos.base(al)) return;
        Dialogos.fechar(campo(al, "vm"), 617);
        Object casa = campo(al, "yA"), fora = campo(al, "yB");
        for (Object o : new Object[] {casa, fora}) {
            Dialogos.fonte(o, Estreia.titulo(22f), Estreia.BRANCO);
            centro(o);
        }
        pos(casa, 70, 12, 220, 28);
        pos(fora, 300, 12, 220, 28);
        pos(campo(al, "yv"), 150, 44, 60, 60);
        pos(campo(al, "yu"), 380, 44, 60, 60);
        Object jogos = campo(al, "yG");
        Dialogos.fonte(jogos, Estreia.titulo(26f), Estreia.OURO);
        centro(jogos);
        pos(jogos, 230, 52, 130, 32);
        Object resumo = campo(al, "yE");
        semi(resumo, 12f, Estreia.FRACO);
        centro(resumo);
        pos(resumo, 220, 86, 150, 18);
        for (String s : new String[] {"yC", "yw", "yD", "yx"}) {
            Object o = campo(al, s);
            semi(o, 12f, Estreia.FRACO);
            centro(o);
        }
        pos(campo(al, "yC"), 80, 108, 200, 18);
        pos(campo(al, "yw"), 80, 126, 200, 18);
        pos(campo(al, "yD"), 310, 108, 200, 18);
        pos(campo(al, "yx"), 310, 126, 200, 18);
        Object mv1 = campo(al, "yF"), mv2 = campo(al, "yI");
        Dialogos.rotulo(mv1);
        Dialogos.rotulo(mv2);
        centro(mv1);
        centro(mv2);
        pos(mv1, 80, 154, 200, 16);
        pos(mv2, 310, 154, 200, 16);
        Object p1 = campo(al, "yy"), p2 = campo(al, "yz");
        Dialogos.fonte(p1, Estreia.negrito(13f), Estreia.BRANCO);
        Dialogos.fonte(p2, Estreia.negrito(13f), Estreia.BRANCO);
        centro(p1);
        centro(p2);
        pos(p1, 60, 174, 240, 20);
        pos(p2, 290, 174, 240, 20);
        Object rot = campo(al, "yH");
        Dialogos.rotulo(rot);
        esquerda(rot);
        pos(rot, 20, 208, 400, 16);
        Object lista = campo(al, "ut");
        Dialogos.rolagem(lista);
        pos(lista, 20, 230, 577, 150);
        Dialogos.vazio(lista, "Nenhum jogo entre os dois", "Escolha outros times embaixo para comparar.");
        pos(campo(al, "yr"), 20, 400, 200, 28);
        Object x = campo(al, "ug");
        semi(x, 12f, Estreia.FRACO);
        centro(x);
        pos(x, 222, 404, 24, 20);
        pos(campo(al, "ys"), 248, 400, 200, 28);
        Object mostrar = campo(al, "yq");
        Editor.botao(mostrar, "Comparar", "confrontos", true);
        pos(mostrar, 462, 396, 135, 36);
        al.revalidate();
    }

    // ------------------------------------------------------------- marcar amistoso

    static void amistoso(JPanel h) {
        if (!Dialogos.base(h) || !Dialogos.absoluto(h)) return;
        Object t = campo(h, "uu");
        if (t instanceof JLabel) {
            String s = ((JLabel) t).getText();
            int i = s == null ? -1 : s.indexOf(" - ");
            if (i > 0) ((JLabel) t).setText(s.substring(i + 3) + " - " + s.substring(0, i));
        }
        Dialogos.titulo(t, 28f);
        pos(t, 24, 14, 470, 40);
        Dialogos.fechar(campo(h, "ud"), 565);
        Object o = campo(h, "uv");
        Dialogos.cartao(o);
        pos(o, 24, 66, 517, 486);
        if (!(o instanceof JPanel) || !Dialogos.absoluto((JPanel) o)) return;
        String[][] rotulos = {{"uh", "Data:"}, {"a_", "Adversário:"}, {"us", "Local do jogo:"}};
        int[] ys = {26, 66, 358};
        for (int i = 0; i < rotulos.length; i++) {
            Object r = campo(h, rotulos[i][0]);
            if (r instanceof JLabel) ((JLabel) r).setText(rotulos[i][1]);
            semi(r, 12.5f, Estreia.FRACO);
            esquerda(r);
            pos(r, 20, ys[i], 150, 20);
        }
        pos(campo(h, "up"), 170, 22, 170, 28);
        Object arvore = campo(h, "ut");
        Dialogos.rolagem(arvore);
        pos(arvore, 170, 62, 327, 248);
        Object tree = campo(h, "uw");
        if (tree instanceof JTree) Telas.arvore((JTree) tree);
        Object sel = campo(h, "ur");
        semi(sel, 12.5f, Estreia.OURO);
        esquerda(sel);
        pos(sel, 170, 318, 327, 20);
        pos(campo(h, "uq"), 170, 354, 170, 28);
        Object propor = campo(h, "uo");
        Editor.botao(propor, null, "amistoso", true);
        pos(propor, 170, 418, 210, 38);
        h.revalidate();
    }

    // ------------------------------------------------------------- cores do time

    static void cores(JPanel bb) {
        if (!Dialogos.base(bb)) return;
        Object t = campo(bb, "ug");
        tituloSimples(t, 24f);
        pos(t, 30, 16, 250, 32);
        for (String s : new String[] {"vx", "uh"}) {
            Object o = campo(bb, s);
            semi(o, 12f, Estreia.FRACO);
            if (o instanceof JLabel) {
                String x = ((JLabel) o).getText();
                if (x != null && x.endsWith(":")) ((JLabel) o).setText(x.substring(0, x.length() - 1));
            }
        }
        pos(campo(bb, "vx"), 70, 58, 90, 18);
        pos(campo(bb, "uh"), 170, 58, 90, 18);
        // os quadrinhos guardam a cor no fundo (o jogo grava getBackground): so o desenho muda
        for (String s : new String[] {"zw", "zv", "zx"}) {
            Object o = campo(bb, s);
            if (o instanceof JLabel) {
                ((JLabel) o).setUI(new Amostra());
                ((JLabel) o).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
        }
        Dialogos.fonte(campo(bb, "zx"), Estreia.titulo(28f), null);
        Object salvar = campo(bb, "zn"), cancelar = campo(bb, "ud");
        Editor.botao(salvar, "Salvar", "disquete", true);
        Editor.botao(cancelar, "Cancelar", null, false);
        pos(salvar, 30, 250, 118, 36);
        pos(cancelar, 160, 250, 118, 36);
        bb.revalidate();
    }

    // Quadrinho de cor: a cor do fundo do rotulo em retangulo arredondado com contorno fino.
    static class Amostra extends BasicLabelUI {
        @Override
        public void update(Graphics g, JComponent c) { paint(g, c); }
        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
            g.setColor(c.getBackground());
            g.fill(r);
            g.setColor(new Color(244, 241, 234, 70));
            g.draw(r);
            g.dispose();
            super.paint(g0, c);
        }
    }
}
