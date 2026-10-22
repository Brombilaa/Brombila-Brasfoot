package mod;

import c.Ordens;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

// Cartao "Ordens" aberto por cima do campo da Partida ao vivo (botao Dar ordens ou tecla O).
// O jogo fica parado enquanto o cartao esta aberto; Aplicar entrega o plano ao motor (c.Ordens),
// que passa a usar a partir do proximo minuto. Pintado a mao, no mesmo estilo da Partida.
final class PainelOrdens {

    static final String[] GRUPOS = {"MENTALIDADE", "ATACAR POR", "PRESSÃO", "PASSE"};
    static final String[][] OPCOES = {Ordens.MENTALIDADES, Ordens.ATAQUES, Ordens.PRESSOES, Ordens.PASSES};

    final Ordens.Plano plano;
    final String[] dicas;
    final int sugestaoLado;     // Ordens.MEIO / MISTO / LATERAIS segundo o auxiliar
    final String minuto;

    // areas clicaveis da ultima pintura
    final ArrayList<double[]> alvos = new ArrayList<double[]>();   // x, y, w, h, grupo, valor
    Rectangle2D.Double aplicar = new Rectangle2D.Double(), cancelar = new Rectangle2D.Double(), cartao = new Rectangle2D.Double();
    Point mouse = new Point(-1, -1);

    PainelOrdens(Ordens.Plano atual, String minuto, int sugestaoLado, String[] dicas) {
        this.plano = atual.copia();
        this.minuto = minuto;
        this.sugestaoLado = sugestaoLado;
        this.dicas = dicas;
    }

    int valor(int grupo) {
        switch (grupo) {
            case 0: return plano.mentalidade - Ordens.RETRANCA;
            case 1: return plano.ataque;
            case 2: return plano.pressao;
            default: return plano.passe;
        }
    }

    void escolhe(int grupo, int i) {
        switch (grupo) {
            case 0: plano.mentalidade = i + Ordens.RETRANCA; break;
            case 1: plano.ataque = i; break;
            case 2: plano.pressao = i; break;
            default: plano.passe = i;
        }
    }

    // 0 nada, 1 mudou algo, 2 aplicar, 3 cancelar
    int clique(Point p) {
        if (aplicar.contains(p)) return 2;
        if (cancelar.contains(p)) return 3;
        for (double[] a : alvos) {
            if (p.x >= a[0] && p.x <= a[0] + a[2] && p.y >= a[1] && p.y <= a[1] + a[3]) {
                escolhe((int) a[4], (int) a[5]);
                return 1;
            }
        }
        return cartao.contains(p) ? 0 : 3;
    }

    static String resumo(Ordens.Plano p) {
        return Ordens.MENTALIDADES[p.mentalidade - Ordens.RETRANCA] + " · " + Ordens.ATAQUES[p.ataque].toLowerCase()
            + " · pressão " + Ordens.PRESSOES[p.pressao].toLowerCase() + " · passe " + Ordens.PASSES[p.passe].toLowerCase();
    }

    // ------------------------------------------------------------------ pintura

    void pinta(Graphics2D g, double ax, double ay, double aw, double ah) {
        alvos.clear();
        double w = Math.min(600, aw - 32);
        // dicas do auxiliar quebradas em linhas para caber no cartao
        g.setFont(Estreia.texto(12.5f));
        FontMetrics ft = g.getFontMetrics();
        ArrayList<String> linhas = new ArrayList<String>();
        ArrayList<Boolean> inicio = new ArrayList<Boolean>();
        for (String d : dicas) {
            boolean primeira = true;
            for (String l : quebra(d, ft, w - 48 - 58)) { linhas.add(l); inicio.add(primeira); primeira = false; }
        }
        double dh = 40 + 19 * linhas.size();
        double h = 82 + 4 * 44 + 6 + 24 + 12 + 66 + dh + 16 + 56;
        double x = ax + (aw - w) / 2, y = ay + Math.max(8, (ah - h) / 2);
        cartao.setRect(x, y, w, h);
        // escurece o campo atras
        g.setColor(new Color(4, 6, 5, 150));
        g.fill(new Rectangle2D.Double(ax, ay, aw, ah));
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h, 20, 20);
        g.setPaint(new GradientPaint(0, (float) y, new Color(0x171C19), 0, (float) (y + h), new Color(0x0E1110)));
        g.fill(r);
        g.setColor(new Color(242, 193, 78, 90));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(r);

        double px = x + 24, pw = w - 48;
        g.setFont(Estreia.titulo(30f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Partida.OURO);
        g.drawString("Ordens", (float) px, (float) (y + 40));
        g.setFont(Estreia.semi(12f));
        fm = g.getFontMetrics();
        String parado = minuto + " · jogo parado";
        g.setColor(Tema.TEXTO_FRACO);
        g.drawString(parado, (float) (px + pw - fm.stringWidth(parado)), (float) (y + 36));
        g.setFont(Estreia.texto(12.5f));
        g.setColor(new Color(0xD9D5CB));
        g.drawString("Valem a partir do próximo lance e duram até o fim do jogo ou até você mudar.", (float) px, (float) (y + 62));

        double ly = y + 82;
        double rotuloW = 112;
        for (int gI = 0; gI < 4; gI++) {
            g.setFont(Estreia.espacada(Estreia.semi(10f), 0.14f));
            fm = g.getFontMetrics();
            g.setColor(Partida.OURO);
            g.drawString(GRUPOS[gI], (float) px, (float) (ly + 22));
            segmentos(g, gI, px + rotuloW, ly, pw - rotuloW, 33);
            ly += 44;
        }

        // o que muda
        ly += 6;
        g.setColor(Tema.BORDA);
        g.draw(new Line2D.Double(px, ly, px + pw, ly));
        ly += 24;
        g.setFont(Estreia.espacada(Estreia.semi(10f), 0.14f));
        g.setColor(Partida.OURO);
        g.drawString("O QUE MUDA", (float) px, (float) ly);
        ly += 12;
        double ataque = Ordens.ataque(plano, 0) - 1, defesa = Ordens.defesa(plano, 0) - 1, meio = Ordens.meio(plano, 0) - 1;
        double folego = Ordens.cansacoPorMinuto(plano) * 45;
        String[] nomes = {"Ataque", "Defesa", "Meio-campo", "Fôlego"};
        double[] vals = {ataque, defesa, meio, -folego * 8};
        double colW = (pw - 24) / 2;
        for (int i = 0; i < 4; i++) {
            double bx = px + (i % 2) * (colW + 24), by = ly + (i / 2) * 30;
            barra(g, nomes[i], vals[i], i == 3 ? textoFolego(folego) : null, bx, by, colW);
        }
        ly += 66;

        // auxiliar
        RoundRectangle2D.Double caixa = new RoundRectangle2D.Double(px, ly, pw, dh, 12, 12);
        g.setColor(new Color(0x1A1F1C));
        g.fill(caixa);
        g.setColor(Tema.BORDA);
        g.draw(caixa);
        g.setColor(Partida.OURO);
        g.fill(new Ellipse2D.Double(px + 14, ly + 12, 16, 16));
        g.setColor(new Color(0x14110A));
        g.setFont(Estreia.negrito(11f));
        g.drawString("i", (float) (px + 20.2), (float) (ly + 24.5));
        g.setFont(Estreia.espacada(Estreia.semi(10f), 0.14f));
        g.setColor(Partida.OURO);
        g.drawString("AUXILIAR TÉCNICO", (float) (px + 40), (float) (ly + 24));
        g.setFont(Estreia.texto(12.5f));
        double tx = px + 58;
        for (int i = 0; i < linhas.size(); i++) {
            double ty = ly + 46 + i * 19;
            if (inicio.get(i)) {
                g.setColor(Partida.OURO);
                g.fill(new Ellipse2D.Double(tx - 13, ty - 7, 5, 5));
            }
            g.setColor(new Color(0xD9D5CB));
            g.drawString(linhas.get(i), (float) tx, (float) ty);
        }

        // botoes
        double by = y + h - 56;
        g.setFont(Estreia.semi(13.5f));
        fm = g.getFontMetrics();
        double aw2 = fm.stringWidth("Aplicar ordens") + 48, cw = fm.stringWidth("Cancelar") + 40;
        aplicar.setRect(px + pw - aw2, by, aw2, 38);
        cancelar.setRect(aplicar.x - 10 - cw, by, cw, 38);
        boolean hA = aplicar.contains(mouse), hC = cancelar.contains(mouse);
        RoundRectangle2D.Double pa = new RoundRectangle2D.Double(aplicar.x, by, aw2, 38, 38, 38);
        g.setPaint(new GradientPaint(0, (float) by, hA ? new Color(0xFFDD85) : new Color(0xF6CE6A), 0, (float) (by + 38), new Color(0xD9A53B)));
        g.fill(pa);
        g.setColor(new Color(0x1A1408));
        g.drawString("Aplicar ordens", (float) (aplicar.x + 24), (float) (by + 19 + fm.getAscent() / 2.0 - 2));
        RoundRectangle2D.Double pc = new RoundRectangle2D.Double(cancelar.x, by, cw, 38, 38, 38);
        g.setColor(hC ? new Color(0x222824) : new Color(0x151917));
        g.fill(pc);
        g.setColor(Tema.BORDA);
        g.draw(pc);
        g.setColor(Tema.TEXTO);
        g.drawString("Cancelar", (float) (cancelar.x + 20), (float) (by + 19 + fm.getAscent() / 2.0 - 2));
        g.setFont(Estreia.texto(11.5f));
        g.setColor(Tema.TEXTO_FRACO);
        g.drawString("Esc cancela · O abre e fecha", (float) px, (float) (by + 24));
    }

    static ArrayList<String> quebra(String t, FontMetrics fm, double max) {
        ArrayList<String> r = new ArrayList<String>();
        StringBuilder l = new StringBuilder();
        for (String p : t.split(" ")) {
            String teste = l.length() == 0 ? p : l + " " + p;
            if (fm.stringWidth(teste) > max && l.length() > 0) { r.add(l.toString()); l = new StringBuilder(p); }
            else { l.setLength(0); l.append(teste); }
        }
        if (l.length() > 0) r.add(l.toString());
        return r;
    }

    static String textoFolego(double f) {
        if (Math.abs(f) < 0.001) return "normal";
        int pct = (int) Math.round(Math.abs(f) * 100);
        if (pct == 0) pct = 1;
        return f > 0 ? "−" + pct + "% por tempo" : "+" + pct + "% por tempo";
    }

    void segmentos(Graphics2D g, int grupo, double x, double y, double w, double h) {
        String[] op = OPCOES[grupo];
        RoundRectangle2D.Double fundo = new RoundRectangle2D.Double(x, y, w, h, h, h);
        g.setColor(new Color(0x0B0E0D));
        g.fill(fundo);
        g.setColor(Tema.BORDA);
        g.setStroke(new BasicStroke(1f));
        g.draw(fundo);
        double sw = (w - 6) / op.length;
        int sel = valor(grupo);
        g.setFont(Estreia.semi(op.length > 3 ? 12f : 12.5f));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < op.length; i++) {
            double sx = x + 3 + i * sw;
            alvos.add(new double[]{sx, y, sw, h, grupo, i});
            RoundRectangle2D.Double s = new RoundRectangle2D.Double(sx, y + 3, sw, h - 6, h - 6, h - 6);
            boolean ativo = i == sel, sobre = mouse.x >= sx && mouse.x <= sx + sw && mouse.y >= y && mouse.y <= y + h;
            if (ativo) {
                Color topo = grupo == 0 ? corMentalidade(i) : new Color(0xF6CE6A);
                g.setPaint(new GradientPaint(0, (float) y, topo, 0, (float) (y + h), escurece(topo)));
                g.fill(s);
            } else if (sobre) {
                g.setColor(new Color(255, 255, 255, 18));
                g.fill(s);
            }
            String t = op[i];
            g.setColor(ativo ? new Color(0x1A1408) : Tema.TEXTO);
            g.drawString(t, (float) (sx + (sw - fm.stringWidth(t)) / 2), (float) (y + h / 2 + fm.getAscent() / 2.0 - 2));
            // sugestao do auxiliar no lado do ataque
            if (grupo == 1 && i == sugestaoLado && sugestaoLado != Ordens.MISTO) {
                g.setColor(ativo ? new Color(0x1A1408) : Partida.OURO);
                g.fill(new Ellipse2D.Double(sx + sw - 16, y + 9, 6, 6));
            }
        }
    }

    // retranca azul-acinzentado ... tudo ou nada vermelho: da para ler de longe o quanto o time se expoe
    static Color corMentalidade(int i) {
        switch (i) {
            case 0: return new Color(0x8DB3E2);
            case 1: return new Color(0xB5CFEA);
            case 2: return new Color(0xF6CE6A);
            case 3: return new Color(0xF4A261);
            default: return new Color(0xFF7A66);
        }
    }

    static Color escurece(Color c) {
        return new Color((int) (c.getRed() * 0.82), (int) (c.getGreen() * 0.78), (int) (c.getBlue() * 0.7));
    }

    void barra(Graphics2D g, String nome, double v, String texto, double x, double y, double w) {
        g.setFont(Estreia.semi(12f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        g.drawString(nome, (float) x, (float) (y + 14));
        double bx = x + 92, bw = w - 92 - 88, cy = y + 9;
        g.setColor(new Color(255, 255, 255, 26));
        g.fill(new RoundRectangle2D.Double(bx, cy - 3, bw, 6, 6, 6));
        double meio = bx + bw / 2;
        double k = Math.max(-1, Math.min(1, v / 0.75));
        Color cor = v > 0.004 ? Tema.BOM : v < -0.004 ? new Color(0xFF6B5E) : Tema.TEXTO_FRACO;
        if (Math.abs(k) > 0.004) {
            double len = Math.abs(k) * bw / 2;
            g.setColor(cor);
            g.fill(new RoundRectangle2D.Double(k > 0 ? meio : meio - len, cy - 3, len, 6, 6, 6));
        }
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new Rectangle2D.Double(meio - 0.75, cy - 6, 1.5, 12));
        String t = texto != null ? texto : Math.abs(v) < 0.005 ? "igual" : (v > 0 ? "+" : "−") + Math.round(Math.abs(v) * 100) + "%";
        g.setFont(Estreia.semi(11.5f));
        fm = g.getFontMetrics();
        g.setColor(cor);
        if (texto != null) {
            g.setFont(Estreia.texto(11f));
            fm = g.getFontMetrics();
        }
        g.drawString(t, (float) (x + w - fm.stringWidth(t)), (float) (y + 14));
    }
}
