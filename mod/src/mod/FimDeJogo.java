package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicPanelUI;

// Fim de jogo (a.hk): a tela que aparece depois da partida, com o placar, as notas dos dois
// times, a sumula (gols, cartoes, contusoes), as estatisticas e os outros jogos da rodada.
// As tabelas e rotulos do jogo ficam escondidos; o painel Sumula le a partida escolhida
// (campo KY) direto do jogo e desenha tudo, com uma entrada animada: escudos deslizam,
// o placar salta, as notas sobem linha a linha, os lances caem e as barras crescem.
// Clicar em outro jogo da lista a esquerda troca a partida e repete a entrada, mais rapida.
final class FimDeJogo {
    private FimDeJogo() {}

    static final int W = 1024, H = 748;
    static final int LX = 12, LW = 312;              // coluna da rodada
    static final int RX = 336, RW = 676;             // area da partida
    static final int HY = 14, HH = 150;              // placar
    static final int MY = 176, MH = 420;             // notas e sumula
    static final int SY = 608, SH = 56;              // estatisticas
    static final int CW = 240, CX = RX + CW + 12, CCW = RW - 2 * CW - 24;  // coluna do meio

    static void aplica(final JPanel hk) {
        if (hk.getClientProperty(Janelas.FEITO) != null) return;
        hk.putClientProperty(Janelas.FEITO, Boolean.TRUE);

        // pecas do jogo que a Sumula passa a desenhar
        String[] some = {"ut", "zf", "zg", "uh", "a_", "ur", "us", "Ln", "Lo", "Lp", "Lq", "Lh", "Li",
            "Le", "Lf", "Lg", "Lj", "Lk", "Lm", "Ll", "zO", "zP", "Cg", "Dz", "Dy", "Fq", "yu", "Ld"};
        for (String s : some) {
            Object c = Reflexao.campo(hk, s);
            if (c instanceof JComponent) ((JComponent) c).setVisible(false);
        }

        // rodada: seletor e lista dos jogos
        Dialogos.pos(Reflexao.campo(hk, "va"), LX + 14, 44, LW - 28, 30);
        Object wi = Reflexao.campo(hk, "wi");
        if (wi instanceof JScrollPane) {
            final JScrollPane sp = (JScrollPane) wi;
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            linhas(hk, sp);
            sp.getViewport().addChangeListener(new javax.swing.event.ChangeListener() {
                Component ultimo;
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    Component v = sp.getViewport().getView();
                    if (v != ultimo) { ultimo = v; linhas(hk, sp); }
                }
            });
        }
        Dialogos.pos(wi, LX + 8, 84, LW - 12, 576 - 84 + 76);

        // botoes de baixo
        String[] botoes = {"La", "HS", "Lc", "Lb"};
        for (int i = 0; i < botoes.length; i++) Dialogos.pos(Reflexao.campo(hk, botoes[i]), LX + 4 + i * 58, 680, 50, 40);
        Object zk = Reflexao.campo(hk, "zK");
        if (zk instanceof JButton) {
            JButton b = (JButton) zk;
            b.putClientProperty(BotaoUI.PRIMARIO, Boolean.TRUE);
            b.setFont(Estreia.negrito(14f));
            b.setText("Continuar");
            b.setIcon(null);
        }
        Dialogos.pos(zk, RX + RW - 200, 678, 200, 44);

        final Sumula s = new Sumula(hk);
        // a partida mostrada muda quando o jogo troca os rotulos (clique na lista)
        for (String n : new String[] {"zO", "Lm"}) {
            Object c = Reflexao.campo(hk, n);
            if (c instanceof JLabel) ((JLabel) c).addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { s.confere(); }
            });
        }
        Dialogos.fundo(hk, s, 0, 0, W, H);
        hk.revalidate();
        hk.repaint();
    }

    // ------------------------------------------------------------------ lista da rodada

    static void linhas(JPanel hk, JScrollPane sp) {
        if (hk == null) return;
        Component v = sp.getViewport().getView();
        if (!(v instanceof JPanel)) return;
        JPanel xb = (JPanel) v;
        xb.setOpaque(false);
        for (Component c : xb.getComponents()) {
            if (!(c instanceof JPanel) || !c.getClass().getName().equals("components.V")) continue;
            JPanel linha = (JPanel) c;
            if (linha.getUI() instanceof Linha) continue;
            linha.setUI(new Linha(hk));
            linha.setOpaque(false);
            boolean jogo = Reflexao.campo(linha, "zz") != null;
            for (Component k : linha.getComponents()) {
                if (!(k instanceof JLabel)) continue;
                JLabel l = (JLabel) k;
                l.setOpaque(false);
                if (!jogo) {
                    l.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.14f));
                    l.setForeground(Estreia.OURO);
                    l.setText(l.getText() == null ? "" : l.getText().toUpperCase());
                } else if (l == Reflexao.campo(linha, "Lm")) {
                    l.setFont(Estreia.titulo(16f));
                    l.setForeground(Estreia.BRANCO);
                    l.setText(l.getText() == null ? "" : l.getText().trim());
                } else {
                    l.setFont(Estreia.texto(11.5f));
                    l.setForeground(new Color(0xE6E2DA));
                }
            }
        }
        xb.repaint();
    }

    // Linha da lista: transparente; o jogo escolhido ganha fundo dourado e filete a esquerda.
    static final class Linha extends BasicPanelUI {
        final JPanel hk;
        Linha(JPanel hk) { this.hk = hk; }

        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            Placar.antialias(g);
            int w = c.getWidth(), h = c.getHeight();
            Object jogo = Reflexao.campo(c, "zz");
            if (jogo == null) {
                g.setColor(Estreia.LINHA);
                g.fillRect(8, h - 3, w - 16, 1);
                g.dispose();
                return;
            }
            boolean escolhido = jogo == Reflexao.campo(hk, "KY");
            if (escolhido) {
                RoundRectangle2D r = new RoundRectangle2D.Float(2, 1, w - 4, h - 2, 10, 10);
                g.setPaint(new GradientPaint(0, 0, Tema.alfa(Estreia.OURO, 56), w, 0, Tema.alfa(Estreia.OURO, 14)));
                g.fill(r);
                g.setColor(Estreia.OURO);
                g.fill(new RoundRectangle2D.Float(2, 5, 3, h - 10, 3, 3));
            } else {
                g.setColor(new Color(244, 241, 234, 10));
                g.fillRect(8, h - 1, w - 16, 1);
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ dados da partida

    static final class Linhas {
        final List<Object[]> titulares = new ArrayList<Object[]>();   // {F, nota, pos}
        final List<Object[]> trocas = new ArrayList<Object[]>();      // {entra F, sai F, minuto, nota}
    }

    static final class Lance {
        int tipo, lado, minuto, tempo, jeito;   // tipo 1 gol, 2 amarelo, 3/4 vermelho, 5 contusao
        String nome, extra;
    }

    static final class Dados {
        best.I jogo;
        String casa = "", fora = "", competicao = "", rodada = "", estadio = "", publico = "", renda = "";
        int gc, gf, pc = -1, pf = -1;
        Image[] escudos = new Image[2];
        Color[] cores = new Color[2];
        Linhas[] times = {new Linhas(), new Linhas()};
        List<Lance> lances = new ArrayList<Lance>();
        int[][] stats = new int[5][2];
        String[] statTexto = new String[10];
        Object craque;
        int humano = -1;   // 0 casa, 1 fora
        Map<Object, int[]> marcas = new IdentityHashMap<Object, int[]>();   // jogador -> {gols, amarelo, vermelho, contusao}
    }

    static Dados le(JPanel hk, best.I i) {
        Dados d = new Dados();
        d.jogo = i;
        best.ah c = i.hc(), f = i.hd();
        d.casa = c.getNome();
        d.fora = f.getNome();
        d.gc = i.hu();
        d.gf = i.hw();
        int[] p = i.hQ();
        if (p != null && p.length > 1 && p[0] >= 0 && p[1] >= 0) { d.pc = p[0]; d.pf = p[1]; }
        d.escudos[0] = escudo(c);
        d.escudos[1] = escudo(f);
        d.cores[0] = cor(c);
        d.cores[1] = cor(f);
        if (Boolean.TRUE.equals(c.jZ())) d.humano = 0;
        else if (Boolean.TRUE.equals(f.jZ())) d.humano = 1;
        try { d.competicao = i.hy().getNome(); } catch (Throwable t) {}
        try { d.rodada = i.ij() == null ? "" : i.ij(); } catch (Throwable t) {}
        d.estadio = Placar.texto(Reflexao.campo(hk, "Cg"));
        d.publico = Placar.texto(Reflexao.campo(hk, "Dy"));
        d.renda = Placar.texto(Reflexao.campo(hk, "Dz"));

        // lances (gols, cartoes, contusoes) e marcas por jogador
        ArrayList<?> ev = i.hE();
        for (int k = 0; ev != null && k < ev.size(); k++) {
            best.A a = (best.A) ev.get(k);
            int t = a.b();
            if (t < 1 || t > 5 || a.eo() == null) continue;
            Lance l = new Lance();
            l.tipo = t;
            l.lado = a.et();
            l.minuto = a.em();
            l.tempo = a.en();
            l.jeito = a.el();
            l.nome = a.eo().getNome();
            if (t == 1) {
                if (l.jeito == 2) l.extra = "contra";
                else if (l.jeito == 3) l.extra = "pênalti";
                else if (l.jeito == 4) l.extra = "falta";
                else if (l.jeito == 5) l.extra = "olímpico";
            }
            d.lances.add(l);
            int[] m = d.marcas.get(a.eo());
            if (m == null) d.marcas.put(a.eo(), m = new int[4]);
            if (t == 1 && l.jeito != 2) m[0]++;
            else if (t == 2) m[1]++;
            else if (t == 3 || t == 4) m[2]++;
            else if (t == 5) m[3]++;
        }
        java.util.Collections.sort(d.lances, new java.util.Comparator<Lance>() {
            public int compare(Lance a, Lance b) {
                if (a.tempo != b.tempo) return a.tempo - b.tempo;
                return a.minuto - b.minuto;
            }
        });

        // notas: titulares na ordem da posicao, depois as trocas
        double melhor = 0;
        for (int lado = 0; lado < 2; lado++) {
            ArrayList<?> lista = lado == 0 ? i.hl() : i.hm();
            best.ah time = lado == 0 ? c : f;
            Linhas ls = d.times[lado];
            for (int k = 0; lista != null && k < lista.size(); k++) {
                best.F j = (best.F) lista.get(k);
                if (j == null) continue;
                ls.titulares.add(new Object[] {j, j.gk(), posicao(j)});
                if (j.gk() > melhor) { melhor = j.gk(); d.craque = j; }
            }
            java.util.Collections.sort(ls.titulares, new java.util.Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) {
                    return ordem((String) a[2]) - ordem((String) b[2]);
                }
            });
            for (int k = 0; ev != null && k < ev.size(); k++) {
                best.A a = (best.A) ev.get(k);
                if (a.b() != 6 || a.cu() != time || a.ep() == null) continue;
                int abs = a.em() + (a.en() == 2 ? 45 : a.en() == 3 ? 90 : a.en() == 4 ? 105 : 0);
                String min = a.em() == 0 && a.en() == 2 ? "int." : abs + "'";
                ls.trocas.add(new Object[] {a.ep(), a.eo(), min, a.ep().gk()});
                if (a.ep().gk() > melhor) { melhor = a.ep().gk(); d.craque = a.ep(); }
            }
        }
        if (melhor < 2) d.craque = null;

        int[][] s = {i.hz(), i.hA(), i.hZ(), i.hB(), i.hC()};
        int[] fora = i.ia();
        for (int k = 0; k < 5; k++) {
            int a = s[k] != null && s[k].length > 0 ? s[k][0] : 0, b = s[k] != null && s[k].length > 1 ? s[k][1] : 0;
            d.stats[k][0] = a;
            d.stats[k][1] = b;
            String ta = Integer.toString(a), tb = Integer.toString(b);
            if (k == 0) { ta += "%"; tb += "%"; }
            if (k == 2 && fora != null && fora.length > 1) { ta += " (" + fora[0] + ")"; tb += " (" + fora[1] + ")"; }
            d.statTexto[2 * k] = ta;
            d.statTexto[2 * k + 1] = tb;
        }
        return d;
    }

    static int ordem(String p) {
        return "GLZMA".indexOf(p == null || p.isEmpty() ? "?" : p.substring(0, 1)) & 0xFF;
    }

    static String posicao(best.F j) {
        try {
            int vaga = j.fT();
            if (vaga >= 0 && vaga < best.aq.sE.length) return best.aq.rI[best.aq.sE[vaga][0]];
        } catch (Throwable t) {}
        try { return best.aq.rI[j.getPosicao()]; } catch (Throwable t) { return "?"; }
    }

    static Image escudo(best.ah t) {
        try {
            ImageIcon i = t.jY() != null ? t.kP() : t.K(true);
            return i == null ? null : i.getImage();
        } catch (Throwable e) {
            return null;
        }
    }

    // cor principal do clube, clareada se for escura demais para o fundo
    static Color cor(best.ah t) {
        Color c;
        try { c = t.kB(); } catch (Throwable e) { c = null; }
        if (c == null) return Estreia.OURO;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        if (hsb[2] < 0.55f) hsb[2] = 0.55f + hsb[2] * 0.3f;
        if (hsb[1] < 0.15f) return new Color(0xE6E2DA);
        return Color.getHSBColor(hsb[0], Math.min(hsb[1], 0.85f), hsb[2]);
    }

    static String nota(double n) {
        if (n < 2.0) return "sn";
        if (n >= 10.0) return "10";
        return String.format(java.util.Locale.ROOT, "%.1f", n).replace('.', ',');
    }

    // ------------------------------------------------------------------ desenho

    static final class Sumula extends JComponent implements ActionListener {
        final JPanel hk;
        Object mostrado;
        Dados d;
        long t0;
        float ritmo = 1f;      // 1 na primeira entrada, mais rapido ao trocar de jogo
        final Timer timer = new Timer(16, this);
        static final int DURA = 2200;

        Sumula(JPanel hk) {
            this.hk = hk;
            setOpaque(true);
        }

        @Override
        public boolean contains(int x, int y) { return false; }

        void confere() {
            Object ky = Reflexao.campo(hk, "KY");
            if (ky != mostrado) {
                mostrado = ky;
                d = null;
                ritmo = t0 == 0 ? 1f : 1.8f;
                t0 = 0;
                hk.repaint();
            }
        }

        public void actionPerformed(ActionEvent e) {
            // repinta pelo pai: a Sumula fica atras dos outros e nao pode ser pintada sozinha
            hk.repaint(RX, 0, RW, H);
            if (t() > DURA) timer.stop();
        }

        float t() { return t0 == 0 ? 0 : (System.nanoTime() - t0) / 1e6f * ritmo; }

        // 0..1 entre os instantes a e b (ms da entrada)
        float fase(float a, float b) {
            if (!Animacao.ligada()) return 1f;
            float t = t();
            return Math.max(0f, Math.min(1f, (t - a) / (b - a)));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Object ky = Reflexao.campo(hk, "KY");
            if (ky != mostrado || d == null) {
                if (ky != mostrado) { ritmo = mostrado == null ? 1f : 1.8f; t0 = 0; }
                mostrado = ky;
                try { d = ky instanceof best.I ? le(hk, (best.I) ky) : null; } catch (Throwable t) { t.printStackTrace(); d = null; }
            }
            if (t0 == 0 && isShowing()) {
                t0 = System.nanoTime();
                if (Animacao.ligada()) timer.restart();
            }
            Graphics2D g = (Graphics2D) g0.create();
            Placar.antialias(g);
            fundo(g);
            Placar.cartao(g, LX, HY, LW, 576 + 76 - HY + 8, 16, false);
            g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.2f));
            g.setColor(Estreia.OURO);
            g.drawString("RODADA", LX + 16, 34);
            if (d != null) {
                try {
                    placar(g);
                    notas(g, 0, RX, MY);
                    notas(g, 1, RX + RW - CW, MY);
                    sumula(g);
                    estatisticas(g);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            g.dispose();
        }

        void fundo(Graphics2D g) {
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x161A17), 0, h, new Color(0x0A0C0B)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(RX + RW / 2f, -60, RW * 0.8f, new float[] {0f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 30), Tema.alfa(Estreia.OURO, 0)}));
            g.fillRect(0, 0, w, h);
            g.setColor(new Color(244, 241, 234, 22));
            g.fillRect(14, 670, w - 28, 1);
            g.setColor(new Color(244, 241, 234, 40));
            g.drawRect(0, 0, w - 1, h - 1);
        }

        // ---------------------------------------------------------- placar

        void placar(Graphics2D g) {
            Placar.cartao(g, RX, HY, RW, HH, 18, true);
            // brilho das cores dos clubes atras dos escudos
            for (int lado = 0; lado < 2; lado++) {
                float cx = lado == 0 ? RX + 150 : RX + RW - 150;
                float a = fase(0, 600);
                g.setPaint(new RadialGradientPaint(cx, HY + 70, 120, new float[] {0f, 1f},
                    new Color[] {Tema.alfa(d.cores[lado], (int) (40 * a)), Tema.alfa(d.cores[lado], 0)}));
                java.awt.Shape clip = g.getClip();
                g.clip(new RoundRectangle2D.Float(RX, HY, RW, HH, 18, 18));
                g.fillRect(RX, HY, RW, HH);
                g.setClip(clip);
            }
            int cx = RX + RW / 2;

            // chip FIM DE JOGO + competicao
            float a0 = fase(0, 350);
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(a0));
            String chip = "FIM DE JOGO";
            g.setFont(Estreia.espacada(Estreia.negrito(9f), 0.18f));
            FontMetrics fm = g.getFontMetrics();
            int cw = fm.stringWidth(chip) + 22;
            g.setColor(Tema.alfa(Estreia.OURO, 40));
            g.fill(new RoundRectangle2D.Float(cx - cw / 2f, HY + 14, cw, 20, 20, 20));
            g.setColor(Tema.alfa(Estreia.OURO, 120));
            g.draw(new RoundRectangle2D.Float(cx - cw / 2f + 0.5f, HY + 14.5f, cw - 1, 19, 19, 19));
            g.setColor(Estreia.OURO);
            g.drawString(chip, cx - fm.stringWidth(chip) / 2f, HY + 28);
            String sub = d.rodada.length() > 0 ? d.rodada : d.competicao;
            g.setFont(Estreia.texto(11f));
            fm = g.getFontMetrics();
            sub = Placar.corta(fm, sub, 300);
            g.setColor(Estreia.FRACO);
            g.drawString(sub, cx - fm.stringWidth(sub) / 2f, HY + 50);
            g.setComposite(comp);

            // escudos e nomes deslizando dos lados
            for (int lado = 0; lado < 2; lado++) {
                float a = Animacao.suave(fase(80 + lado * 70, 620 + lado * 70));
                int ex = lado == 0 ? RX + 150 : RX + RW - 150;
                float dx = (1 - a) * (lado == 0 ? -50 : 50);
                g.setComposite(AlphaComposite.SrcOver.derive(a));
                Image img = d.escudos[lado];
                int lado2 = 64;
                if (img != null) Escala.desenha(g, img, (int) (ex - lado2 / 2 + dx), HY + 22, lado2, lado2, null);
                String nome = lado == 0 ? d.casa : d.fora;
                java.awt.Font f = Placar.caber(g, nome, 24f, 15f, 230);
                g.setFont(f);
                fm = g.getFontMetrics();
                nome = Placar.corta(fm, nome, 230);
                g.setColor(Estreia.BRANCO);
                g.drawString(nome, ex - fm.stringWidth(nome) / 2f + dx, HY + 116);
                // faixa nas cores do clube embaixo do nome
                g.setColor(d.cores[lado]);
                g.fill(new RoundRectangle2D.Float(ex - 14 + dx, HY + 124, 28 * a, 3, 3, 3));
                g.setComposite(comp);
            }

            // placar: os numeros saltam
            float p = fase(260, 760);
            float esc = 1f + 0.55f * (1f - sobe(p));
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, p * 2f)));
            AffineTransform at = g.getTransform();
            g.translate(cx, HY + 106);
            g.scale(esc, esc);
            g.setFont(Estreia.titulo(60f));
            fm = g.getFontMetrics();
            String a = Integer.toString(d.gc), b = Integer.toString(d.gf);
            boolean vc = d.gc > d.gf, vf = d.gf > d.gc;
            g.setColor(vc || d.gc == d.gf ? Estreia.OURO : Tema.alfa(Estreia.BRANCO, 150));
            g.drawString(a, -26 - fm.stringWidth(a), 0);
            g.setColor(vf || d.gc == d.gf ? Estreia.OURO : Tema.alfa(Estreia.BRANCO, 150));
            g.drawString(b, 26, 0);
            g.setColor(Tema.alfa(Estreia.BRANCO, 90));
            g.fill(new RoundRectangle2D.Float(-9, -22, 18, 4, 4, 4));
            g.setTransform(at);
            g.setComposite(comp);

            // linha de baixo do placar: penaltis ou resultado do time humano
            float r = fase(700, 1100);
            g.setComposite(AlphaComposite.SrcOver.derive(r));
            int y = HY + 122;
            String s = null;
            Color c = Estreia.OURO;
            if (d.humano >= 0) {
                int nos = d.humano == 0 ? d.gc : d.gf, eles = d.humano == 0 ? d.gf : d.gc;
                if (d.pc >= 0 && nos == eles) { nos = d.humano == 0 ? d.pc : d.pf; eles = d.humano == 0 ? d.pf : d.pc; }
                s = nos > eles ? "VITÓRIA" : nos < eles ? "DERROTA" : "EMPATE";
                c = nos > eles ? new Color(0x3FD47E) : nos < eles ? new Color(0xFF6B5E) : Estreia.OURO;
            }
            if (d.pc >= 0) s = (s == null ? "" : s + "  ·  ") + "PÊNALTIS " + d.pc + " x " + d.pf;
            if (s != null) {
                g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.2f));
                fm = g.getFontMetrics();
                int w = fm.stringWidth(s) + 24;
                g.setColor(Tema.alfa(c, 40));
                g.fill(new RoundRectangle2D.Float(cx - w / 2f, y - 4, w, 20, 20, 20));
                g.setColor(c);
                g.drawString(s, cx - fm.stringWidth(s) / 2f, y + 10);
            }
            g.setComposite(comp);

            // brilho que atravessa o placar
            float sw = fase(900, 1500);
            if (sw > 0 && sw < 1) {
                java.awt.Shape clip = g.getClip();
                g.clip(new RoundRectangle2D.Float(RX, HY, RW, HH, 18, 18));
                float x = RX - 160 + (RW + 320) * Animacao.suave(sw);
                g.setPaint(new GradientPaint(x - 80, 0, Tema.alfa(Estreia.OURO, 0), x, 0, Tema.alfa(Estreia.OURO, 46), true));
                g.fill(new java.awt.Polygon(new int[] {(int) x - 80, (int) x + 10, (int) x + 50, (int) x - 40},
                    new int[] {HY, HY, HY + HH, HY + HH}, 4));
                g.setClip(clip);
            }
        }

        // ease-out com uma sobra pequena no fim (pulo do placar)
        static float sobe(float t) {
            t = Math.max(0f, Math.min(1f, t));
            float c1 = 1.70158f, c3 = c1 + 1;
            return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }

        // ---------------------------------------------------------- notas

        void notas(Graphics2D g, int lado, int x, int y) {
            Placar.cartao(g, x, y, CW, MH, 16, false);
            Linhas ls = d.times[lado];
            String time = lado == 0 ? d.casa : d.fora;
            g.setColor(d.cores[lado]);
            g.fill(new RoundRectangle2D.Float(x + 16, y + 16, 4, 14, 4, 4));
            g.setFont(Estreia.titulo(17f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Estreia.BRANCO);
            g.drawString(Placar.corta(fm, time, CW - 80), x + 26, y + 29);
            g.setFont(Estreia.espacada(Estreia.negrito(8.5f), 0.16f));
            g.setColor(Tema.alfa(Estreia.OURO, 210));
            fm = g.getFontMetrics();
            g.drawString("NOTA", x + CW - 16 - fm.stringWidth("NOTA"), y + 28);
            int ry = y + 44;
            int linha = 19;
            int k = 0;
            float base = lado == 0 ? 300 : 360;
            for (Object[] o : ls.titulares) {
                float a = Animacao.suave(fase(base + k * 38, base + k * 38 + 260));
                jogador(g, x, ry, (best.F) o[0], (Double) o[1], (String) o[2], null, a, lado, k);
                ry += linha;
                k++;
            }
            if (ls.trocas.isEmpty()) {
                g.setComposite(AlphaComposite.SrcOver.derive(fase(base + k * 38, base + k * 38 + 260)));
                g.setFont(Estreia.texto(10.5f));
                g.setColor(Estreia.APAGADO);
                g.drawString("Sem substituições", x + 16, ry + 18);
                g.setComposite(AlphaComposite.SrcOver);
            } else {
                ry += 8;
                float a = fase(base + k * 38, base + k * 38 + 260);
                g.setComposite(AlphaComposite.SrcOver.derive(a));
                g.setFont(Estreia.espacada(Estreia.negrito(8.5f), 0.16f));
                g.setColor(Tema.alfa(Estreia.OURO, 210));
                g.drawString("SUBSTITUIÇÕES", x + 16, ry + 6);
                g.setColor(Estreia.LINHA);
                g.fillRect(x + 110, ry + 2, CW - 126, 1);
                g.setComposite(AlphaComposite.SrcOver);
                ry += 14;
                for (Object[] o : ls.trocas) {
                    if (ry + 28 > y + MH - 4) break;
                    float b = Animacao.suave(fase(base + k * 38, base + k * 38 + 260));
                    best.F entra = (best.F) o[0], sai = (best.F) o[1];
                    jogador(g, x, ry, entra, (Double) o[3], posicao(entra), (String) o[2], b, lado, k);
                    g.setComposite(AlphaComposite.SrcOver.derive(b));
                    g.setFont(Estreia.texto(10f));
                    g.setColor(Estreia.APAGADO);
                    seta(g, x + 50, ry + 22, false);
                    g.drawString(Placar.corta(g.getFontMetrics(), "sai " + (sai == null ? "" : sai.getNome()), CW - 80), x + 60, ry + 25);
                    g.setComposite(AlphaComposite.SrcOver);
                    ry += 30;
                    k++;
                }
            }
        }

        void jogador(Graphics2D g, int x, int y, best.F j, double n, String pos, String entrou, float a, int lado, int k) {
            if (a <= 0) return;
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(a));
            int dx = (int) ((1 - a) * (lado == 0 ? -12 : 12));
            x += dx;
            boolean craque = j == d.craque;
            if (craque) {
                float pulso = fase(1500, 2200);
                g.setPaint(new GradientPaint(x + 8, 0, Tema.alfa(Estreia.OURO, (int) (30 + 30 * (1 - pulso))), x + CW - 8, 0, Tema.alfa(Estreia.OURO, 6)));
                g.fill(new RoundRectangle2D.Float(x + 8, y - 1, CW - 16, 19, 8, 8));
            } else if (k % 2 == 1) {
                g.setColor(new Color(244, 241, 234, 6));
                g.fillRect(x + 10, y - 1, CW - 20, 19);
            }
            // chip da posicao
            int setor = Principal.setor(pos);
            Color pc = Principal.cor(setor);
            g.setColor(Tema.alfa(pc, 46));
            g.fill(new RoundRectangle2D.Float(x + 14, y + 2, 16, 14, 5, 5));
            g.setFont(Estreia.negrito(9f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(pc);
            String p = pos == null ? "" : pos;
            g.drawString(p, x + 22 - fm.stringWidth(p) / 2f, y + 13);
            // nome (seta verde quando entrou)
            int nx = x + 36;
            if (entrou != null) {
                seta(g, nx + 4, y + 9, true);
                nx += 12;
            }
            g.setFont(craque ? Estreia.negrito(11.5f) : Estreia.texto(11.5f));
            fm = g.getFontMetrics();
            int[] m = d.marcas.get(j);
            int icones = 0;
            if (m != null) icones = Math.min(m[0], 3) + (m[1] > 0 ? 1 : 0) + (m[2] > 0 ? 1 : 0) + (m[3] > 0 ? 1 : 0);
            int livre = CW - (nx - x) - 52 - icones * 12 - (entrou != null ? 28 : 0);
            String nome = Placar.corta(fm, j.getNome(), livre);
            g.setColor(craque ? Estreia.BRANCO : new Color(0xE6E2DA));
            g.drawString(nome, nx, y + 13);
            int ix = nx + fm.stringWidth(nome) + 6;
            if (entrou != null) {
                g.setFont(Estreia.texto(10f));
                g.setColor(Estreia.APAGADO);
                g.drawString(entrou, ix, y + 13);
                ix += g.getFontMetrics().stringWidth(entrou) + 6;
            }
            if (m != null) {
                for (int q = 0; q < Math.min(m[0], 3); q++) { bola(g, ix + 5, y + 9, 4.5f); ix += 12; }
                if (m[1] > 0) { cartao(g, ix, y + 3, new Color(0xF2C94C)); ix += 12; }
                if (m[2] > 0) { cartao(g, ix, y + 3, new Color(0xE5484D)); ix += 12; }
                if (m[3] > 0) { cruz(g, ix + 4, y + 9); ix += 12; }
            }
            if (craque) {
                g.setColor(Estreia.OURO);
                estrela(g, x + CW - 58, y + 9, 5.5f);
            }
            // nota: conta ate o valor
            float c = fase(600 + k * 38, 1300 + k * 38);
            double v = n < 2 ? n : n * Animacao.suave(c);
            String s = n < 2 ? "sn" : nota(Math.max(v, Math.min(n, 1.0)));
            Color nc = n < 2 ? Estreia.APAGADO : Notas.cor(n);
            g.setColor(Tema.alfa(nc, 34));
            g.fill(new RoundRectangle2D.Float(x + CW - 46, y + 1, 32, 16, 8, 8));
            g.setFont(Estreia.negrito(10.5f));
            fm = g.getFontMetrics();
            g.setColor(nc);
            g.drawString(s, x + CW - 30 - fm.stringWidth(s) / 2f, y + 13);
            g.setComposite(comp);
        }

        // ---------------------------------------------------------- sumula (coluna do meio)

        void sumula(Graphics2D g) {
            int x = CX, y = MY, w = CCW;
            Placar.cartao(g, x, y, w, MH, 16, false);
            float a = fase(200, 600);
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(a));
            g.setFont(Estreia.espacada(Estreia.negrito(8.5f), 0.16f));
            g.setColor(Tema.alfa(Estreia.OURO, 210));
            g.drawString("SÚMULA", x + 14, y + 26);
            int iy = y + 46;
            iy = info(g, "ESTÁDIO", d.estadio, x, iy, w);
            iy = info(g, "PÚBLICO", d.publico.isEmpty() || d.publico.equals("n/i") ? "" : Placar.milhar(d.publico) + " torcedores", x, iy, w);
            iy = info(g, "RENDA", d.renda.isEmpty() || d.renda.equals("n/i") ? "" : "$ " + d.renda, x, iy, w);
            g.setColor(Estreia.LINHA);
            g.fillRect(x + 14, iy + 2, w - 28, 1);
            g.setComposite(comp);
            iy += 16;
            if (d.lances.isEmpty()) {
                g.setComposite(AlphaComposite.SrcOver.derive(fase(600, 900)));
                g.setFont(Estreia.texto(11f));
                g.setColor(Estreia.APAGADO);
                String s = "Sem gols nem cartões.";
                g.drawString(s, x + w / 2f - g.getFontMetrics().stringWidth(s) / 2f, iy + 14);
                g.setComposite(comp);
                return;
            }
            int k = 0;
            for (Lance l : d.lances) {
                if (iy + 20 > y + MH - 8) break;
                float b = Animacao.suave(fase(500 + k * 110, 800 + k * 110));
                if (b > 0) lance(g, l, x, iy, w, b);
                iy += l.extra != null ? 30 : 22;
                k++;
            }
        }

        int info(Graphics2D g, String rotulo, String texto, int x, int y, int w) {
        if (texto == null || texto.trim().isEmpty()) return y;
        g.setFont(Estreia.espacada(Estreia.negrito(7.5f), 0.14f));
        g.setColor(Estreia.APAGADO);
        g.drawString(rotulo, x + 14, y + 4);
        g.setFont(Estreia.semi(11.5f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xE6E2DA));
        g.drawString(Placar.corta(fm, texto.trim(), w - 28), x + 14, y + 19);
        return y + 30;
    }

    void lance(Graphics2D g, Lance l, int x, int y, int w, float a) {
            Composite comp = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(a));
            int dy = (int) ((1 - a) * -8);
            y += dy;
            boolean casa = l.lado == 0;
            // minuto no meio, icone e nome para o lado do time
            // o jogo conta os minutos dentro de cada tempo (42' do 2o = 87')
            int abs = l.minuto + (l.tempo == 2 ? 45 : l.tempo == 3 ? 90 : l.tempo == 4 ? 105 : 0);
            String min = abs + "'";
            g.setFont(Estreia.negrito(10f));
            FontMetrics fm = g.getFontMetrics();
            int mx = casa ? x + 14 : x + w - 14 - 30;
            g.setColor(Tema.alfa(d.cores[l.lado], 40));
            g.fill(new RoundRectangle2D.Float(mx, y, 30, 16, 8, 8));
            g.setColor(Estreia.BRANCO);
            g.drawString(min, mx + 15 - fm.stringWidth(min) / 2f, y + 12);
            int ix = casa ? mx + 38 : mx - 14;
            if (l.tipo == 1) bola(g, ix + 4, y + 8, 5f);
            else if (l.tipo == 2) cartao(g, ix, y + 2, new Color(0xF2C94C));
            else if (l.tipo == 3 || l.tipo == 4) cartao(g, ix, y + 2, new Color(0xE5484D));
            else if (l.tipo == 5) cruz(g, ix + 4, y + 8);
            g.setFont(l.tipo == 1 ? Estreia.semi(11f) : Estreia.texto(11f));
            fm = g.getFontMetrics();
            int livre = w - 30 - 14 - 14 - 22;
            String nome = Placar.corta(fm, l.nome, livre);
            g.setColor(l.tipo == 1 ? Estreia.BRANCO : new Color(0xD9D4C9));
            if (casa) g.drawString(nome, ix + 14, y + 12);
            else g.drawString(nome, ix - 6 - fm.stringWidth(nome), y + 12);
            if (l.extra != null) {
                g.setFont(Estreia.texto(9.5f));
                fm = g.getFontMetrics();
                g.setColor(Estreia.APAGADO);
                if (casa) g.drawString(l.extra, ix + 14, y + 25);
                else g.drawString(l.extra, ix - 6 - fm.stringWidth(l.extra), y + 25);
            }
            g.setComposite(comp);
        }

        // ---------------------------------------------------------- estatisticas

        void estatisticas(Graphics2D g) {
            String[] nomes = {"POSSE DE BOLA", "FINALIZAÇÕES", "NO GOL (FORA)", "DESARMES", "ERROS DE PASSE"};
            int gap = 8;
            float tw = (RW - gap * 4) / 5f;
            for (int k = 0; k < 5; k++) {
                float x = RX + k * (tw + gap);
                Placar.cartao(g, x, SY, tw, SH, 14, false);
                float a = fase(700 + k * 70, 1000 + k * 70);
                Composite comp = g.getComposite();
                g.setComposite(AlphaComposite.SrcOver.derive(a));
                g.setFont(Estreia.espacada(Estreia.negrito(8f), 0.12f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Estreia.FRACO);
                g.drawString(nomes[k], x + tw / 2f - fm.stringWidth(nomes[k]) / 2f, SY + 18);
                g.setFont(Estreia.titulo(17f));
                fm = g.getFontMetrics();
                int va = d.stats[k][0], vb = d.stats[k][1];
                // nos erros de passe, menos e melhor
                boolean ma = k == 4 ? va < vb : va > vb, mb = k == 4 ? vb < va : vb > va;
                g.setColor(ma ? Estreia.OURO : Estreia.BRANCO);
                g.drawString(d.statTexto[2 * k], x + 12, SY + 44);
                g.setColor(mb ? Estreia.OURO : Estreia.BRANCO);
                g.drawString(d.statTexto[2 * k + 1], x + tw - 12 - fm.stringWidth(d.statTexto[2 * k + 1]), SY + 44);
                // barra dividida crescendo do meio
                float tot = va + vb;
                float frac = tot <= 0 ? 0.5f : va / tot;
                float cresce = Animacao.suave(fase(900 + k * 70, 1500 + k * 70));
                float bx = x + 12, bw = tw - 24, by = SY + 49;
                g.setColor(new Color(244, 241, 234, 20));
                g.fill(new RoundRectangle2D.Float(bx, by, bw, 3, 3, 3));
                float meio = bx + bw * frac;
                float wa = bw * frac * cresce, wb = bw * (1 - frac) * cresce;
                g.setColor(ma ? Estreia.OURO : Tema.alfa(d.cores[0], 200));
                g.fill(new RoundRectangle2D.Float(meio - wa, by, wa, 3, 3, 3));
                g.setColor(mb ? Estreia.OURO : Tema.alfa(d.cores[1], 200));
                g.fill(new RoundRectangle2D.Float(meio, by, wb, 3, 3, 3));
                g.setComposite(comp);
            }
        }

        // ---------------------------------------------------------- icones pequenos

        static void bola(Graphics2D g, float cx, float cy, float r) {
            g.setColor(Estreia.BRANCO);
            g.fill(new java.awt.geom.Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(new Color(0x1A1D1B));
            Path2D p = new Path2D.Float();
            for (int i = 0; i < 5; i++) {
                double an = -Math.PI / 2 + i * 2 * Math.PI / 5;
                float px = cx + (float) Math.cos(an) * r * 0.42f, py = cy + (float) Math.sin(an) * r * 0.42f;
                if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
            }
            p.closePath();
            g.fill(p);
        }

        static void cartao(Graphics2D g, float x, float y, Color c) {
            g.setColor(c);
            g.fill(new RoundRectangle2D.Float(x, y, 9, 12, 2, 2));
        }

        static void cruz(Graphics2D g, float cx, float cy) {
            g.setColor(new Color(0xFF6B5E));
            g.fill(new RoundRectangle2D.Float(cx - 1.5f, cy - 5, 3, 10, 1, 1));
            g.fill(new RoundRectangle2D.Float(cx - 5, cy - 1.5f, 10, 3, 1, 1));
        }

        static void seta(Graphics2D g, float cx, float cy, boolean sobe) {
            g.setColor(sobe ? new Color(0x3FD47E) : new Color(0xFF6B5E));
            Path2D p = new Path2D.Float();
            if (sobe) { p.moveTo(cx - 4, cy + 2); p.lineTo(cx, cy - 3); p.lineTo(cx + 4, cy + 2); }
            else { p.moveTo(cx - 4, cy - 2); p.lineTo(cx, cy + 3); p.lineTo(cx + 4, cy - 2); }
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(p);
        }

        static void estrela(Graphics2D g, float cx, float cy, float r) {
            Path2D p = new Path2D.Float();
            for (int i = 0; i < 10; i++) {
                double an = -Math.PI / 2 + i * Math.PI / 5;
                float rr = i % 2 == 0 ? r : r * 0.45f;
                float px = cx + (float) Math.cos(an) * rr, py = cy + (float) Math.sin(an) * rr;
                if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
            }
            p.closePath();
            g.fill(p);
        }
    }
}
