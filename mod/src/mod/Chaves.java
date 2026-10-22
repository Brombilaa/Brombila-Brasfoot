package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

// Copa (a.al): abas "Visao geral" (a lista de confrontos do jogo) e "Por chave". Na aba Por
// chave, a esquerda ficam as chaves da fase aberta e a direita o caminho da chave escolhida
// ate a final: cada fase num cartao, com ida, volta, penaltis e quem passou em dourado.
// As fases seguintes sao lidas andando pelas fases do proprio jogo (o mesmo que clicar em
// "Proxima fase") e voltando para a fase em que o jogador estava.
final class Chaves {
    private Chaves() {}

    static void instala(final JPanel al, final JScrollPane tabela) {
        final JPanel por = new JPanel(null);
        Dialogos.absoluto(por);
        por.setOpaque(false);
        por.setVisible(false);
        Dialogos.adiciona(al, por, 13, 198, 857, 588);
        final Lista lista = new Lista();
        final Caminho caminho = new Caminho();
        final JScrollPane sl = new JScrollPane(lista);
        final JScrollPane sc = new JScrollPane(caminho);
        for (JScrollPane s : new JScrollPane[] {sl, sc}) {
            s.setOpaque(false);
            s.getViewport().setOpaque(false);
            s.setBorder(BorderFactory.createEmptyBorder());
            s.getVerticalScrollBar().setUnitIncrement(16);
            s.getHorizontalScrollBar().setUnitIncrement(16);
            RolagemSuave.instala(s);
        }
        sl.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Dialogos.adiciona(por, sl, 0, 0, 236, 588);
        Dialogos.adiciona(por, sc, 248, 0, 609, 588);
        lista.aoEscolher = new Runnable() {
            public void run() {
                caminho.mostra(lista.fases, lista.escolhida);
                SwingUtilities.invokeLater(() -> sc.getHorizontalScrollBar().setValue(0));
            }
        };

        final JButton geral = new JButton(), porChave = new JButton();
        final Runnable[] atualiza = new Runnable[1];
        final boolean[] aberta = {false};
        Runnable estilo = () -> {
            Editor.botao(geral, "Visão geral", "todos", !aberta[0]);
            Editor.botao(porChave, "Por chave", "confrontos", aberta[0]);
            tabela.setVisible(!aberta[0]);
            por.setVisible(aberta[0]);
            al.revalidate();
            al.repaint();
        };
        geral.addActionListener(ev -> { aberta[0] = false; estilo.run(); });
        porChave.addActionListener(ev -> { aberta[0] = true; estilo.run(); atualiza[0].run(); });
        estilo.run();
        Dialogos.adiciona(al, geral, 13, 158, 150, 32);
        Dialogos.adiciona(al, porChave, 171, 158, 150, 32);

        final boolean[] lendo = {false};
        atualiza[0] = () -> {
            if (!aberta[0] || lendo[0]) return;
            lendo[0] = true;
            try {
                List<Fase> fases = le(al);
                lista.mostra(fases);
                lista.aoEscolher.run();
            } finally {
                lendo[0] = false;
            }
        };
        // o jogador mudou de fase ou de competicao: refaz
        Object wp = Dialogos.campo(al, "wp");
        if (wp instanceof JComponent) {
            ((JComponent) wp).addPropertyChangeListener("model", ev -> {
                if (!lendo[0]) SwingUtilities.invokeLater(atualiza[0]);
            });
        }
        // so ha chaves em mata-mata: fora dele as abas somem
        Runnable visivel = () -> {
            boolean mata = tipo(al) == components.d.class;
            geral.setVisible(mata);
            porChave.setVisible(mata);
            if (!mata && aberta[0]) { aberta[0] = false; estilo.run(); }
        };
        visivel.run();
        if (wp instanceof JComponent) {
            ((JComponent) wp).addPropertyChangeListener("model", ev -> SwingUtilities.invokeLater(visivel));
        }
    }

    private static Class<?> tipo(JPanel al) {
        Object wp = Dialogos.campo(al, "wp");
        if (!(wp instanceof javax.swing.JTable)) return null;
        javax.swing.JTable t = (javax.swing.JTable) wp;
        return t.getRowCount() > 0 && t.getValueAt(0, 0) != null ? t.getValueAt(0, 0).getClass() : null;
    }

    // ------------------------------------------------------------------ dados

    static class Confronto {
        String rotulo = "", nomeA = "", nomeB = "";
        best.I ida, volta;
        best.ah a, b;
        // gols somados (do ponto de vista de a e b), penaltis; -1 = ainda nao
        int ga = -1, gb = -1, pa = -1, pb = -1;
        boolean fim;

        best.ah vencedor() {
            if (!fim || a == null || b == null || ga < 0) return null;
            if (ga != gb) return ga > gb ? a : b;
            if (pa >= 0 && pa != pb) return pa > pb ? a : b;
            return null;
        }

        boolean tem(best.ah t) { return t != null && (t == a || t == b); }
    }

    static class Fase {
        String nome = "";
        final List<Confronto> chaves = new ArrayList<Confronto>();
    }

    static Confronto confronto(components.d d) {
        Confronto c = new Confronto();
        c.rotulo = d.sU() == null ? "" : d.sU().trim();
        if (d.sZ()) {
            c.nomeA = d.sV() == null ? "" : d.sV();
            c.nomeB = d.sW() == null ? "" : d.sW();
            return c;
        }
        c.ida = d.sP();
        c.volta = d.sQ();
        if (c.ida == null) return c;
        c.a = c.ida.hc();
        c.b = c.ida.hd();
        c.nomeA = c.a == null ? "" : c.a.getNome();
        c.nomeB = c.b == null ? "" : c.b.getNome();
        boolean fim = c.ida.e() && (c.volta == null || c.volta.e());
        c.fim = fim;
        if (c.ida.e()) {
            c.ga = c.ida.hu();
            c.gb = c.ida.hw();
            if (c.volta != null && c.volta.e()) {
                // na volta o mandante e o outro time
                boolean inverte = c.volta.hc() == c.b;
                c.ga += inverte ? c.volta.hw() : c.volta.hu();
                c.gb += inverte ? c.volta.hu() : c.volta.hw();
            }
        }
        if (fim) {
            best.I ult = c.volta != null ? c.volta : c.ida;
            int[] p = ult.hQ();
            if (p != null && p.length > 1 && p[0] >= 0 && p[1] >= 0 && (p[0] > 0 || p[1] > 0)) {
                boolean inverte = ult.hc() == c.b;
                c.pa = inverte ? p[1] : p[0];
                c.pb = inverte ? p[0] : p[1];
            }
        }
        return c;
    }

    @SuppressWarnings("unchecked")
    private static Fase fase(JPanel al) {
        Fase f = new Fase();
        Object wm = Dialogos.campo(al, "wm");
        f.nome = wm instanceof JLabel && ((JLabel) wm).getText() != null ? ((JLabel) wm).getText().trim() : "";
        Object vt = Dialogos.campo(al, "vT");
        if (vt instanceof List) {
            for (Object o : new ArrayList<Object>((List<Object>) vt)) {
                if (o instanceof components.d && ((components.d) o).sR() && !((components.d) o).sS()) f.chaves.add(confronto((components.d) o));
            }
        }
        return f;
    }

    // Le a fase aberta e as seguintes, e volta para onde o jogador estava.
    static List<Fase> le(JPanel al) {
        List<Fase> fases = new ArrayList<Fase>();
        Fase atual = fase(al);
        fases.add(atual);
        Object wo = Dialogos.campo(al, "wO");
        Method anda;
        try {
            anda = al.getClass().getDeclaredMethod("cH", int.class);
            anda.setAccessible(true);
        } catch (Throwable t) {
            return fases;
        }
        int passos = 0;
        try {
            while (passos < 12 && wo instanceof JButton && ((JButton) wo).isEnabled() && ((JButton) wo).isVisible()) {
                anda.invoke(al, 1);
                passos++;
                if (tipo(al) != components.d.class) break;
                Fase f = fase(al);
                if (f.chaves.isEmpty() || f.nome.equals(fases.get(fases.size() - 1).nome)) break;
                fases.add(f);
            }
        } catch (Throwable t) {
            // fica com o que deu para ler
        } finally {
            try {
                for (int i = 0; i < passos; i++) anda.invoke(al, -1);
                // confere se voltou para a mesma fase
                Object wm = Dialogos.campo(al, "wm");
                for (int i = 0; i < 4 && wm instanceof JLabel && !atual.nome.equals(((JLabel) wm).getText().trim()); i++) anda.invoke(al, -1);
            } catch (Throwable t) {
                // sem volta
            }
        }
        return fases;
    }

    // Caminho da chave k (0 = primeira) da primeira fase ate a final.
    static List<Confronto> caminho(List<Fase> fases, int k) {
        List<Confronto> r = new ArrayList<Confronto>();
        if (fases.isEmpty() || k < 0 || k >= fases.get(0).chaves.size()) return r;
        Confronto c = fases.get(0).chaves.get(k);
        r.add(c);
        int idx = k;
        for (int p = 1; p < fases.size(); p++) {
            List<Confronto> prox = fases.get(p).chaves;
            if (prox.isEmpty()) break;
            int achou = -1;
            best.ah v = c.vencedor();
            for (int i = 0; i < prox.size() && achou < 0; i++) if (prox.get(i).tem(v)) achou = i;
            for (int i = 0; i < prox.size() && achou < 0 && v == null; i++) if (prox.get(i).tem(c.a) || prox.get(i).tem(c.b)) achou = i;
            if (achou < 0) achou = Math.min(prox.size() - 1, idx / 2);
            List<Confronto> ant = fases.get(p - 1).chaves;
            idx = achou;
            c = prox.get(idx);
            if (c.ida == null && 2 * idx + 1 < ant.size()) {
                // ainda sem times: diz de quais chaves vem cada vaga
                c.nomeA = "Vencedor chave " + numeroChave(ant, 2 * idx);
                c.nomeB = "Vencedor chave " + numeroChave(ant, 2 * idx + 1);
            }
            r.add(c);
        }
        return r;
    }

    private static String numeroChave(List<Confronto> l, int i) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*$").matcher(l.get(i).rotulo);
        return m.find() ? m.group(1) : Integer.toString(i + 1);
    }

    static Icon escudo(best.ah t, int lado) {
        if (t == null) return null;
        try { return t.x(lado, lado); } catch (Throwable e) { return null; }
    }

    // ------------------------------------------------------------------ lista de chaves

    static class Lista extends JComponent {
        static final int ALT = 54;
        List<Fase> fases = new ArrayList<Fase>();
        int escolhida = 0, sobre = -1;
        Runnable aoEscolher;

        Lista() {
            setOpaque(false);
            java.awt.event.MouseAdapter m = new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    int i = e.getY() / ALT;
                    if (i >= 0 && i < total()) {
                        escolhida = i;
                        repaint();
                        if (aoEscolher != null) aoEscolher.run();
                    }
                }
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    int i = e.getY() / ALT;
                    if (i != sobre) { sobre = i; repaint(); }
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) { sobre = -1; repaint(); }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        }

        int total() { return fases.isEmpty() ? 0 : fases.get(0).chaves.size(); }

        void mostra(List<Fase> f) {
            boolean mesma = !fases.isEmpty() && !f.isEmpty() && fases.get(0).nome.equals(f.get(0).nome) && total() == f.get(0).chaves.size();
            fases = f;
            if (!mesma) escolhida = sugerida();
            if (escolhida >= total()) escolhida = 0;
            revalidate();
            repaint();
            if (!mesma && escolhida > 0) {
                final int y = escolhida * ALT;
                SwingUtilities.invokeLater(() -> scrollRectToVisible(new java.awt.Rectangle(0, Math.max(0, y - 120), 10, 300)));
            }
        }

        // Comeca pela chave do time do jogador, se ele estiver nessa fase.
        int sugerida() {
            try {
                List<?> humanos = c.a.SR.aN();
                List<Confronto> ch = fases.get(0).chaves;
                for (int i = 0; i < ch.size(); i++) {
                    for (Object h : humanos) if (ch.get(i).tem((best.ah) h)) return i;
                }
            } catch (Throwable t) {
                // sem time humano
            }
            return 0;
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(224, Math.max(1, total()) * ALT); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            if (total() == 0) {
                g.setFont(Estreia.texto(12.5f));
                g.setColor(Estreia.APAGADO);
                g.drawString("Nenhuma chave nesta fase.", 6, 24);
            }
            for (int i = 0; i < total(); i++) {
                Confronto c = fases.get(0).chaves.get(i);
                int y = i * ALT;
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, y + 2.5f, w - 9, ALT - 6, 12, 12);
                boolean on = i == escolhida;
                g.setColor(on ? Tema.alfa(Estreia.OURO, 30) : new Color(244, 241, 234, i == sobre ? 16 : 7));
                g.fill(r);
                g.setColor(on ? Estreia.OURO : new Color(244, 241, 234, i == sobre ? 60 : 30));
                g.draw(r);
                g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.16f));
                g.setColor(on ? Estreia.OURO : Estreia.FRACO);
                String rot = c.rotulo.isEmpty() ? "CHAVE " + (i + 1) : c.rotulo.toUpperCase();
                g.drawString(rot, 12, y + 19);
                linha(g, c, y, w);
            }
            g.dispose();
        }

        // "Escudo Time A × Time B" numa linha, cortando nomes longos.
        private void linha(Graphics2D g, Confronto c, int y, int w) {
            Font f = Estreia.semi(12f);
            FontMetrics fm = g.getFontMetrics(f);
            int x = 12, base = y + 40;
            best.ah v = c.vencedor();
            Icon ea = escudo(c.a, 16), eb = escudo(c.b, 16);
            int metade = (w - 30 - 18) / 2;
            x = nome(g, ea, c.nomeA, x, base, metade, v != null && v == c.a, v != null && v != c.a, fm, f);
            g.setFont(Estreia.texto(11.5f));
            g.setColor(Estreia.APAGADO);
            g.drawString("×", x + 4, base);
            nome(g, eb, c.nomeB, x + 16, base, metade, v != null && v == c.b, v != null && v != c.b, fm, f);
        }

        private int nome(Graphics2D g, Icon e, String s, int x, int base, int max, boolean venceu, boolean caiu, FontMetrics fm, Font f) {
            int x0 = x;
            if (e != null) {
                e.paintIcon(this, g, x, base - 13);
                x += 20;
            }
            g.setFont(f);
            g.setColor(venceu ? Estreia.OURO : caiu ? Estreia.APAGADO : Estreia.BRANCO);
            String t = corta(s, fm, max - (x - x0));
            g.drawString(t, x, base);
            return x + fm.stringWidth(t);
        }
    }

    static String corta(String s, FontMetrics fm, int max) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    // ------------------------------------------------------------------ caminho ate a final

    static class Caminho extends JComponent {
        static final int CW = 212, CH = 132, GAP = 40, TOPO = 46;
        List<Confronto> caminho = new ArrayList<Confronto>();
        List<String> nomes = new ArrayList<String>();
        boolean chegaFinal = false;

        Caminho() { setOpaque(false); }

        void mostra(List<Fase> fases, int k) {
            caminho = Chaves.caminho(fases, k);
            nomes = new ArrayList<String>();
            for (int i = 0; i < caminho.size(); i++) nomes.add(fases.get(i).nome);
            Fase ult = fases.isEmpty() ? null : fases.get(fases.size() - 1);
            chegaFinal = ult != null && ult.chaves.size() == 1 && caminho.size() == fases.size();
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int n = caminho.size() + (chegaFinal ? 1 : 0);
            return new Dimension(Math.max(1, n) * (CW + GAP) + 10, 560);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (caminho.isEmpty()) {
                g.setFont(Estreia.texto(12.5f));
                g.setColor(Estreia.APAGADO);
                g.drawString("Escolha uma chave à esquerda.", 8, 30);
                g.dispose();
                return;
            }
            // descida em escada: cada fase um pouco abaixo, como num chaveamento
            int n = caminho.size();
            int passo = n > 1 ? Math.min(70, (getHeight() - TOPO - CH - 30) / Math.max(1, n - 1)) : 0;
            int[] ys = new int[n + 1];
            for (int i = 0; i <= n; i++) ys[i] = TOPO + 20 + Math.min(i, n - 1) * passo / 2;
            for (int i = 0; i < n; i++) {
                int x = i * (CW + GAP), y = TOPO + 30 + i * passo / 3;
                ys[i] = y;
                g.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.16f));
                g.setColor(Estreia.OURO);
                g.drawString(corta(nomes.get(i).toUpperCase(), g.getFontMetrics(), CW), x + 2, y - 12);
                cartao(g, caminho.get(i), x, y, i == 0);
                if (i + 1 < n || chegaFinal) {
                    int yn = i + 1 < n ? TOPO + 30 + (i + 1) * passo / 3 : y;
                    liga(g, x + CW, y + CH / 2, x + CW + GAP, yn + CH / 2, caminho.get(i).vencedor() != null);
                }
            }
            if (chegaFinal) {
                int x = n * (CW + GAP), y = ys[n - 1];
                taca(g, caminho.get(n - 1).vencedor(), x, y);
            }
            g.dispose();
        }

        void liga(Graphics2D g, int x0, int y0, int x1, int y1, boolean definido) {
            Path2D p = new Path2D.Float();
            int mx = (x0 + x1) / 2;
            p.moveTo(x0, y0);
            p.lineTo(mx - 6, y0);
            p.quadTo(mx, y0, mx, y0 + Integer.signum(y1 - y0) * 6);
            p.lineTo(mx, y1 - Integer.signum(y1 - y0) * 6);
            p.quadTo(mx, y1, mx + 6, y1);
            p.lineTo(x1, y1);
            g.setStroke(definido ? new BasicStroke(1.6f) : new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {4f, 4f}, 0f));
            g.setColor(definido ? Tema.alfa(Estreia.OURO, 200) : new Color(244, 241, 234, 70));
            g.draw(p);
        }

        void cartao(Graphics2D g, Confronto c, int x, int y, boolean primeiro) {
            RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, CW - 1, CH - 1, 14, 14);
            g.setColor(new Color(16, 19, 17, 235));
            g.fill(r);
            g.setColor(new Color(244, 241, 234, 10));
            g.fill(r);
            g.setStroke(new BasicStroke(primeiro ? 1.5f : 1f));
            g.setColor(primeiro ? Estreia.OURO : new Color(244, 241, 234, 50));
            g.draw(r);
            best.ah v = c.vencedor();
            time(g, c.a, c.nomeA, c.ga, c.pa, x, y + 14, v, c.ga >= 0);
            time(g, c.b, c.nomeB, c.gb, c.pb, x, y + 50, v, c.ga >= 0);
            g.setColor(new Color(244, 241, 234, 26));
            g.drawLine(x + 12, y + 92, x + CW - 12, y + 92);
            g.setFont(Estreia.texto(11f));
            g.setColor(Estreia.FRACO);
            String rodape = rodape(c);
            g.drawString(corta(rodape, g.getFontMetrics(), CW - 24), x + 12, y + 112);
        }

        void time(Graphics2D g, best.ah t, String nome, int gols, int pen, int x, int y, best.ah v, boolean jogou) {
            Icon e = escudo(t, 26);
            int tx = x + 12;
            if (e != null) {
                e.paintIcon(this, g, tx, y + 2);
                tx += 34;
            }
            boolean venceu = v != null && v == t, caiu = v != null && v != t;
            Font f = venceu ? Estreia.negrito(13f) : Estreia.semi(13f);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            g.setColor(venceu ? Estreia.OURO : caiu ? Estreia.APAGADO : Estreia.BRANCO);
            String s = nome == null || nome.isEmpty() ? "A definir" : limpa(nome);
            g.drawString(corta(s, fm, CW - (tx - x) - 46), tx, y + 20);
            if (jogou && gols >= 0) {
                Font fg = Estreia.titulo(22f);
                g.setFont(fg);
                FontMetrics mg = g.getFontMetrics();
                String sg = Integer.toString(gols);
                g.setColor(venceu ? Estreia.OURO : caiu ? Estreia.APAGADO : Estreia.BRANCO);
                g.drawString(sg, x + CW - 14 - mg.stringWidth(sg), y + 23);
                if (pen >= 0) {
                    g.setFont(Estreia.texto(10.5f));
                    String sp = "(" + pen + ")";
                    g.drawString(sp, x + CW - 20 - mg.stringWidth(sg) - g.getFontMetrics().stringWidth(sp), y + 21);
                }
            }
        }

        // Os jogos da chave: "Ida 2×1 · Volta 0×0 · Pên. 4–3" ou "A jogar".
        String rodape(Confronto c) {
            if (c.ida == null) return "Ainda sem times";
            StringBuilder s = new StringBuilder();
            if (c.ida.e()) s.append(c.volta != null ? "Ida " : "Jogo ").append(c.ida.hu()).append("×").append(c.ida.hw());
            else s.append(c.volta != null ? "Ida a jogar" : "A jogar");
            if (c.volta != null) {
                s.append(" · ");
                if (c.volta.e()) s.append("Volta ").append(c.volta.hu()).append("×").append(c.volta.hw());
                else s.append("Volta a jogar");
            }
            if (c.pa >= 0) s.append(" · Pên. ").append(c.pa).append("–").append(c.pb);
            return s.toString();
        }

        void taca(Graphics2D g, best.ah v, int x, int y) {
            RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, CW - 1, CH - 1, 14, 14);
            g.setColor(Tema.alfa(Estreia.OURO, v != null ? 34 : 12));
            g.fill(r);
            g.setColor(Tema.alfa(Estreia.OURO, v != null ? 220 : 90));
            g.draw(r);
            Estreia.icone(g, "taca", x + CW / 2 - 16, y + 16, 32, Estreia.OURO);
            g.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.18f));
            FontMetrics fm = g.getFontMetrics();
            String s = "CAMPEÃO";
            g.drawString(s, x + (CW - fm.stringWidth(s)) / 2, y + 66);
            g.setFont(v != null ? Estreia.titulo(22f) : Estreia.texto(12f));
            fm = g.getFontMetrics();
            String n = v != null ? v.getNome().toUpperCase() : "A definir";
            g.setColor(v != null ? Estreia.BRANCO : Estreia.APAGADO);
            n = corta(n, fm, CW - 20);
            g.drawString(n, x + (CW - fm.stringWidth(n)) / 2, y + 96);
        }

        // "Vencedor Preliminar 13" -> "Vencedor da chave 13" (o jogo cola o nome da fase).
        static String limpa(String s) {
            return s.startsWith("Vencedor") && !s.startsWith("Vencedor chave ") ? s.replaceFirst("^Vencedor\\s+.*?(\\d+)$", "Vencedor $1") : s;
        }
    }
}
