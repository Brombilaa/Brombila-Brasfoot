package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

// Placar de cima da partida (a.dV): no lugar da faixa com textura e dos nomes em tarja da cor
// do time, um placar de transmissao. Escudos grandes, nomes em Saira, placar dourado no meio
// com o minuto e a barra do tempo, ultimo gol de cada lado, competicao e estadio a esquerda e
// as estatisticas em barras duplas a direita. Os rotulos do jogo continuam la (o jogo segue
// escrevendo neles), so ficam escondidos; o placar le o texto deles.
final class Placar {
    private Placar() {}

    static void instala(JPanel dv) {
        if (dv.getClientProperty("mod.placar") != null) return;
        dv.putClientProperty("mod.placar", Boolean.TRUE);
        Object hr = Dialogos.campo(dv, "Hr"), hq = Dialogos.campo(dv, "Hq");
        if (!(hr instanceof JPanel)) return;
        JPanel faixa = (JPanel) hr;
        final Painel p = new Painel(dv);
        // a barra de cima (titulo, tempo) entra no placar: some e o campo ganha a altura dela
        if (hq instanceof JComponent) ((JComponent) hq).setVisible(false);
        PropertyChangeListener l = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) { p.repaint(); }
        };
        for (Component c : faixa.getComponents()) {
            c.setVisible(false);
            c.addPropertyChangeListener("text", l);
            c.addPropertyChangeListener("icon", l);
        }
        for (String s : new String[] {"Hg", "uh", "Hf"}) {
            Object o = Dialogos.campo(dv, s);
            if (o instanceof JComponent) ((JComponent) o).addPropertyChangeListener(l);
        }
        Object hf = Dialogos.campo(dv, "Hf");
        if (hf instanceof JProgressBar) {
            ((JProgressBar) hf).addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) { p.repaint(); }
            });
        }
        faixa.setOpaque(false);
        Dialogos.adiciona(faixa, p, 0, 0, 1008, 130);
        faixa.revalidate();
        dv.revalidate();
    }

    static String texto(Object o) {
        if (!(o instanceof JLabel)) return "";
        String s = ((JLabel) o).getText();
        if (s == null) return "";
        if (s.regionMatches(true, 0, "<html>", 0, 6) && s.toLowerCase().indexOf("<td") < 0) {
            s = s.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]+>", "").replace("&nbsp;", " ");
        }
        return s.replaceAll("\\s+", " ").trim();
    }

    static Icon icone(Object o) {
        return o instanceof JLabel ? ((JLabel) o).getIcon() : null;
    }

    // Times do jogo mostrado no placar (dV.vK.get(GW).tR()), para cores e escudos nitidos.
    static best.ah[] times(JPanel dv) {
        try {
            Object lista = Dialogos.campo(dv, "vK");
            Object gw = Dialogos.campo(dv, "GW");
            if (!(lista instanceof List) || !(gw instanceof Integer)) return null;
            int i = (Integer) gw;
            if (i < 0 || i >= ((List<?>) lista).size()) return null;
            best.I jogo = ((components.v) ((List<?>) lista).get(i)).tR();
            return new best.ah[] {jogo.hc(), jogo.hd()};
        } catch (Throwable t) {
            return null;
        }
    }

    static Font caber(Graphics2D g, String s, float px, float min, int largura) {
        Font f = Estreia.titulo(px);
        while (px > min && g.getFontMetrics(f).stringWidth(s) > largura) {
            px -= 1f;
            f = Estreia.titulo(px);
        }
        return f;
    }

    static String corta(FontMetrics fm, String s, int largura) {
        if (fm.stringWidth(s) <= largura) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > largura) s = s.substring(0, s.length() - 1);
        return s.trim() + "…";
    }

    static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    // Fundo de cartao: vidro escuro, luz no alto, filete dourado e contorno fino.
    static void cartao(Graphics2D g, float x, float y, float w, float h, float raio, boolean luz) {
        RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, raio, raio);
        g.setPaint(new GradientPaint(0, y, new Color(0x1B201C), 0, y + h, new Color(0x0E110F)));
        g.fill(r);
        java.awt.Shape clip = g.getClip();
        g.clip(r);
        if (luz) {
            g.setPaint(new RadialGradientPaint(x + w / 2f, y - 10, w * 0.45f, new float[] {0f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 34), Tema.alfa(Estreia.OURO, 0)}));
            g.fill(r);
        }
        g.setPaint(new GradientPaint(x, 0, Tema.alfa(Estreia.OURO, 0), x + w / 2f, 0, Tema.alfa(Estreia.OURO, 190), true));
        g.fillRect((int) x + 18, (int) y, (int) w - 36, 2);
        g.setClip(clip);
        g.setColor(new Color(244, 241, 234, 32));
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
    }

    // ------------------------------------------------------------------ painel

    static class Painel extends JComponent {
        final JPanel dv;
        best.ah[] cache;
        Icon[] escudos = new Icon[2];

        Painel(JPanel dv) {
            this.dv = dv;
            setOpaque(false);
        }

        Object c(String n) { return Dialogos.campo(dv, n); }

        Icon escudo(int i, best.ah[] t, String rotulo) {
            if (t != null && t != cache) {
                cache = t;
                escudos = new Icon[2];
            }
            if (t != null && escudos[i] == null) {
                try { escudos[i] = t[i].x(54, 54); } catch (Throwable e) { /* escudo do rotulo */ }
            }
            return escudos[i] != null ? escudos[i] : icone(c(rotulo));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            int w = getWidth(), h = getHeight();
            cartao(g, 0, 0, w, h, 18, true);
            best.ah[] t = times(dv);
            int cx = 508;

            // esquerda: competicao, fase, estadio e publico
            String comp = texto(c("Hg")), fase = texto(c("Ht"));
            int x0 = 20;
            g.setFont(Estreia.espacada(Estreia.negrito(10f), 0.18f));
            g.setColor(Estreia.OURO);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(corta(fm, comp.toUpperCase(), 180), x0, 30);
            g.setFont(Estreia.texto(12f));
            g.setColor(Estreia.FRACO);
            g.drawString(corta(g.getFontMetrics(), fase, 180), x0, 48);
            String estadio = texto(c("Hl")), publico = texto(c("Hm"));
            g.setColor(Estreia.LINHA);
            g.fillRect(x0, 62, 26, 1);
            if (estadio.length() > 0) {
                Menus.desenha(g, "estadio", x0, 74, 14, Estreia.APAGADO);
                g.setFont(Estreia.semi(12f));
                g.setColor(Estreia.BRANCO);
                g.drawString(corta(g.getFontMetrics(), estadio, 162), x0 + 20, 86);
            }
            if (publico.length() > 0) {
                Menus.desenha(g, "torcida", x0, 96, 14, Estreia.APAGADO);
                g.setFont(Estreia.texto(12f));
                g.setColor(Estreia.FRACO);
                g.drawString(milhar(publico) + " torcedores", x0 + 20, 108);
            }

            // placar
            String[] gols = texto(c("Hn")).split("\\s*x\\s*");
            String a = gols.length > 0 ? gols[0].trim() : "0", b = gols.length > 1 ? gols[1].trim() : "0";
            int pw = 130, px = cx - pw / 2, py = 30, ph = 58;
            RoundRectangle2D caixa = new RoundRectangle2D.Float(px, py, pw, ph, 14, 14);
            g.setPaint(new GradientPaint(0, py, new Color(0x0A0C0B), 0, py + ph, new Color(0x15130D)));
            g.fill(caixa);
            g.setColor(Tema.alfa(Estreia.OURO, 120));
            g.draw(caixa);
            Font fp = Estreia.titulo(46f);
            g.setFont(fp);
            FontMetrics fpm = g.getFontMetrics();
            int base = py + 50;
            g.setPaint(new GradientPaint(0, py + 8, new Color(0xFFF1C8), 0, py + ph - 6, Estreia.OURO));
            g.drawString(a, cx - 16 - fpm.stringWidth(a), base);
            g.drawString(b, cx + 16, base);
            g.setColor(Tema.alfa(Estreia.OURO, 120));
            g.fillRect(cx - 5, py + 28, 10, 3);
            // tempo: chip com o minuto e barra fina
            String tempo = texto(c("uh")).replace(" - ", " · ").toUpperCase();
            g.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.14f));
            FontMetrics ft = g.getFontMetrics();
            int tw = ft.stringWidth(tempo) + 22;
            boolean intervalo = tempo.startsWith("INTERV") || tempo.startsWith("FIM");
            g.setColor(intervalo ? Tema.alfa(Estreia.OURO, 46) : new Color(0x0A0C0B));
            g.fill(new RoundRectangle2D.Float(cx - tw / 2f, 96, tw, 20, 20, 20));
            g.setColor(Tema.alfa(Estreia.OURO, intervalo ? 160 : 70));
            g.draw(new RoundRectangle2D.Float(cx - tw / 2f + 0.5f, 96.5f, tw - 1, 19, 19, 19));
            g.setColor(intervalo ? Estreia.OURO : Estreia.BRANCO);
            g.drawString(tempo, cx - ft.stringWidth(tempo) / 2f, 110);
            Object hf = c("Hf");
            if (hf instanceof JProgressBar) {
                JProgressBar bar = (JProgressBar) hf;
                double f = bar.getMaximum() > bar.getMinimum()
                    ? (bar.getValue() - bar.getMinimum()) / (double) (bar.getMaximum() - bar.getMinimum()) : 0;
                g.setColor(new Color(244, 241, 234, 40));
                g.fill(new RoundRectangle2D.Float(px + 14, py + ph - 6, pw - 28, 2.5f, 3, 3));
                g.setColor(Estreia.OURO);
                g.fill(new RoundRectangle2D.Float(px + 14, py + ph - 6, (float) ((pw - 28) * Math.max(0, Math.min(1, f))), 2.5f, 3, 3));
            }

            // times
            lado(g, t, 0, cx);
            lado(g, t, 1, cx);

            // estatisticas
            Estatisticas.pinta(g, texto(c("xI")), 806, 12, 184, 108);
            g.dispose();
        }

        void lado(Graphics2D g, best.ah[] t, int i, int cx) {
            boolean casa = i == 0;
            Icon e = escudo(i, t, casa ? "Hh" : "Hi");
            int ex = casa ? cx - 65 - 12 - 54 : cx + 65 + 12;
            if (e != null) e.paintIcon(this, g, ex + (54 - e.getIconWidth()) / 2, 32 + (54 - e.getIconHeight()) / 2);
            String nome = texto(c(casa ? "Hp" : "Ho"));
            if (t != null) try { nome = t[i].getNome(); } catch (Throwable x) { /* nome do rotulo */ }
            int larg = 146;
            int borda = casa ? ex - 12 : ex + 54 + 12;
            Font f = caber(g, nome, 27f, 17f, larg);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            String s = corta(fm, nome, larg);
            int sw = fm.stringWidth(s);
            int nx = casa ? borda - sw : borda;
            g.setColor(Estreia.BRANCO);
            g.drawString(s, nx, 62);
            // cores do time em dois tracos
            Color c1 = Estreia.OURO, c2 = Estreia.BRANCO;
            if (t != null) {
                try {
                    if (t[i].kB() != null) c1 = t[i].kB();
                    if (t[i].kC() != null) c2 = t[i].kC();
                } catch (Throwable x) { /* cores padrao */ }
            }
            int sx = casa ? borda - 34 : borda;
            g.setColor(c1);
            g.fill(new RoundRectangle2D.Float(sx, 70, 20, 3, 3, 3));
            g.setColor(c2);
            g.fill(new RoundRectangle2D.Float(sx + 22, 70, 12, 3, 3, 3));
            g.setColor(new Color(244, 241, 234, 40));
            g.draw(new RoundRectangle2D.Float(sx + 22 - 0.5f, 69.5f, 13, 4, 3, 3));
            // ultimo gol
            String gol = texto(c(casa ? "Hj" : "Hk"));
            if (gol.length() > 0) {
                g.setFont(Estreia.texto(12f));
                FontMetrics gm = g.getFontMetrics();
                String gs = corta(gm, gol, larg - 20);
                int gw = gm.stringWidth(gs) + 18;
                int gx = casa ? borda - gw : borda;
                Icon ic = icone(c(casa ? "Hj" : "Hk"));
                if (ic != null) {
                    Graphics2D gi = (Graphics2D) g.create();
                    double k = 14.0 / Math.max(ic.getIconWidth(), ic.getIconHeight());
                    gi.translate(gx, 85);
                    if (k < 1) gi.scale(k, k);
                    ic.paintIcon(this, gi, 0, 0);
                    gi.dispose();
                } else {
                    Menus.desenha(g, "bola", gx, 86, 13, Estreia.OURO);
                }
                g.setColor(Estreia.FRACO);
                g.drawString(gs, gx + 18, 97);
            }
        }
    }

    static String milhar(String s) {
        try {
            long n = Long.parseLong(s.replaceAll("[^0-9]", ""));
            return String.format(new java.util.Locale("pt", "BR"), "%,d", n);
        } catch (Throwable t) {
            return s;
        }
    }

    // ------------------------------------------------------------------ estatisticas

    // Le a tabela em HTML do jogo (best.I.ha()) e desenha cada linha como "casa · rotulo · fora"
    // com uma barra dupla embaixo (dourado para o time da casa, creme para o visitante).
    static final class Estatisticas {
        private Estatisticas() {}

        static final Pattern TD = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        static List<String[]> linhas(String html) {
            List<String> cel = new ArrayList<String>();
            Matcher m = TD.matcher(html == null ? "" : html);
            while (m.find()) cel.add(m.group(1).replaceAll("<[^>]+>", "").replace("&nbsp;", " ").trim());
            List<String[]> r = new ArrayList<String[]>();
            for (int i = 0; i + 2 < cel.size(); i += 3) r.add(new String[] {cel.get(i), cel.get(i + 1), cel.get(i + 2)});
            return r;
        }

        static double numero(String s) {
            Matcher m = Pattern.compile("\\d+").matcher(s);
            return m.find() ? Double.parseDouble(m.group()) : 0;
        }

        static void pinta(Graphics2D g, String html, int x, int y, int w, int h) {
            List<String[]> ls = linhas(html);
            if (ls.isEmpty()) return;
            float passo = h / (float) ls.size();
            for (int i = 0; i < ls.size(); i++) {
                String[] l = ls.get(i);
                float yy = y + i * passo;
                String a = l[0].replace(" ", ""), b = l[2].replace(" ", ""), rot = l[1].toUpperCase();
                g.setFont(Estreia.negrito(12f));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(Estreia.BRANCO);
                g.drawString(a, x, yy + 12);
                g.drawString(b, x + w - fm.stringWidth(b), yy + 12);
                g.setFont(Estreia.espacada(Estreia.negrito(8.5f), 0.12f));
                FontMetrics rm = g.getFontMetrics();
                g.setColor(Estreia.APAGADO);
                g.drawString(rot, x + (w - rm.stringWidth(rot)) / 2f, yy + 11);
                double va = numero(l[0]), vb = numero(l[2]);
                double f = va + vb > 0 ? va / (va + vb) : 0.5;
                float by = yy + 15f, bw = w;
                g.setColor(new Color(244, 241, 234, 18));
                g.fill(new RoundRectangle2D.Float(x, by, bw, 2.5f, 3, 3));
                float meio = (float) (bw * f);
                g.setColor(va >= vb ? Estreia.OURO : Tema.alfa(Estreia.OURO, 150));
                g.fill(new RoundRectangle2D.Float(x, by, Math.max(0, meio - 1.5f), 2.5f, 3, 3));
                g.setColor(vb > va ? Estreia.BRANCO : Tema.alfa(Estreia.BRANCO, 110));
                g.fill(new RoundRectangle2D.Float(x + meio + 1.5f, by, Math.max(0, bw - meio - 1.5f), 2.5f, 3, 3));
            }
        }
    }

    // Componente solto para usar as estatisticas fora do placar (intervalo).
    static class Barras extends JComponent {
        final JLabel fonte;
        Barras(JLabel fonte) {
            this.fonte = fonte;
            setOpaque(false);
            fonte.addPropertyChangeListener("text", new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) { repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            antialias(g);
            Estatisticas.pinta(g, fonte.getText(), 0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }
}
