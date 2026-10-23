package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

// Desenho da tela Escalar time (usado pela classe components.ao do jogo, que o mod substitui):
// camisas com sombra e escala suave, numero com contraste, e rotulo do jogador com nome em
// Manrope, etiqueta da posicao, forca e mini barra de energia.
public final class Escalacao {
    private Escalacao() {}

    static final int ROTULO_L = 150, ROTULO_A = 40;
    private static final Color SOMBRA = new Color(0, 0, 0, 170);
    private static final Color AMBAR = new Color(0xF2A23A);

    public static void prepara(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // Camisa com sombra no chao. w/h <= 0 usa o tamanho da imagem.
    public static void camisa(Graphics2D g, Image img, int x, int y, int w, int h) {
        if (img == null) return;
        if (w <= 0) w = img.getWidth(null);
        if (h <= 0) h = img.getHeight(null);
        float cx = x + w / 2f, cy = y + h * 0.93f, rx = w * 0.36f, ry = h * 0.09f;
        g.setPaint(new RadialGradientPaint(cx, cy, rx, new float[] {0f, 1f},
            new Color[] {new Color(0, 0, 0, 120), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2));
        g.drawImage(img, x, y, w, h, null);
    }

    // Cartao de vidro atras de cada jogador da lista "Jogadores disponiveis".
    public static void cartao(Graphics2D g, int x, int y, int w, int h) {
        RoundRectangle2D r = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1, h - 1, 12, 12);
        g.setColor(new Color(244, 241, 234, 9));
        g.fill(r);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(244, 241, 234, 22));
        g.draw(r);
    }

    // Numero da camisa: escuro em camisa clara, claro em camisa escura.
    public static void numero(Graphics2D g, Image camisa, int numero, int x, int y, int w, int h) {
        if (numero <= 0) return;
        String s = Integer.toString(numero);
        g.setFont(Estreia.negrito(h >= 48 ? 13f : 11f));
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(s)) / 2;
        int ty = y + Math.round(h * 0.47f) + fm.getAscent() / 2 - 1;
        // contorno em volta do numero: le bem em camisa lisa, listrada ou com estampa
        boolean clara = luminancia(camisa) > 0.62f;
        java.awt.Shape forma = g.getFont().createGlyphVector(g.getFontRenderContext(), s).getOutline(tx, ty);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(clara ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 190));
        g.draw(forma);
        g.setColor(clara ? new Color(0x15130E) : Color.WHITE);
        g.fill(forma);
    }

    private static final Map<Image, Float> LUZ = new WeakHashMap<Image, Float>();

    // Brilho medio do peito da camisa (cacheado por imagem).
    static synchronized float luminancia(Image img) {
        if (img == null) return 1f;
        Float v = LUZ.get(img);
        if (v != null) return v;
        int w = img.getWidth(null), h = img.getHeight(null);
        float res = 1f;
        if (w > 0 && h > 0) {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = b.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            double soma = 0, peso = 0;
            for (int yy = h * 3 / 10; yy < h * 7 / 10; yy++) {
                for (int xx = w * 3 / 10; xx < w * 7 / 10; xx++) {
                    int p = b.getRGB(xx, yy);
                    int a = p >>> 24;
                    if (a < 128) continue;
                    soma += (0.2126 * (p >> 16 & 255) + 0.7152 * (p >> 8 & 255) + 0.0722 * (p & 255)) / 255.0;
                    peso++;
                }
            }
            if (peso > 0) res = (float) (soma / peso);
        }
        LUZ.put(img, res);
        return res;
    }

    // Rotulo do jogador (150x40, centralizado):
    //   linha 1: nome;
    //   linha 2: etiqueta da posicao (ambar se improvisado), "F 13", mini barra de energia e o lado.
    // forca < 0 esconde a forca (modo habilidade individual); energia < 0 esconde a barra;
    // extra (se nao for nulo) substitui forca/energia por um texto.
    public static Image rotulo(String nome, String posicao, boolean improvisado, int forca, int energia,
                               String lado, String extra, boolean pilula) {
        return rotulo(nome, posicao, improvisado, forca, energia, lado, extra, pilula, ROTULO_L - 6);
    }

    // Nome que nao cabe: "Wellington Reis" vira "W. Reis" antes de recorrer as reticencias.
    static String curto(String nome, FontMetrics fm, int largura) {
        if (nome == null) return "";
        nome = nome.trim();
        if (fm.stringWidth(nome) <= largura) return nome;
        String[] p = nome.split("\\s+");
        if (p.length >= 2) {
            String ab = p[0].charAt(0) + ". " + p[p.length - 1];
            if (fm.stringWidth(ab) <= largura) return ab;
            return p[p.length - 1].length() > 3 && fm.stringWidth(p[p.length - 1]) <= largura ? p[p.length - 1] : ab;
        }
        return nome;
    }

    // pilula = fundo escuro atras do rotulo (no campo, onde rotulos vizinhos podem se encostar).
    public static Image rotulo(String nome, String posicao, boolean improvisado, int forca, int energia,
                               String lado, String extra, boolean pilula, int largura) {
        BufferedImage img = new BufferedImage(ROTULO_L, ROTULO_A, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        prepara(g);
        // nome
        Font fn = Estreia.semi(11.5f);
        g.setFont(fn);
        FontMetrics fm = g.getFontMetrics();
        while (fm.stringWidth(nome) > largura && fn.getSize2D() > 10.5f) {
            fn = fn.deriveFont(fn.getSize2D() - 0.5f);
            g.setFont(fn);
            fm = g.getFontMetrics();
        }
        String n = curto(nome, fm, largura);
        while (fm.stringWidth(n) > largura && n.length() > 3) n = n.substring(0, n.length() - 2) + "…";
        int nx = (ROTULO_L - fm.stringWidth(n)) / 2;
        int ny = fm.getAscent() + 1;

        // linha de informacoes
        Font fp = Estreia.negrito(9f);
        Font fi = Estreia.semi(10f);
        FontMetrics mp = g.getFontMetrics(fp), mi = g.getFontMetrics(fi);
        int chipW = posicao == null || posicao.isEmpty() ? 0 : mp.stringWidth(posicao) + 8;
        String txt = extra != null ? extra : (forca >= 0 ? "F " + forca : "");
        int txtW = txt.isEmpty() ? 0 : mi.stringWidth(txt);
        int barW = (extra == null && energia >= 0) ? 22 : 0;
        String ld = (extra == null && lado != null && !lado.isEmpty()) ? lado : "";
        int ldW = ld.isEmpty() ? 0 : mi.stringWidth(ld);
        int gap = 5;
        int total = chipW + (txtW > 0 ? gap + txtW : 0) + (barW > 0 ? gap + barW : 0) + (ldW > 0 ? gap + ldW : 0);
        if (chipW == 0 && total > 0) total -= gap;
        int x = (ROTULO_L - total) / 2;
        int base = ny + 15;
        if (pilula) {
            int pw = Math.min(ROTULO_L - 1, Math.max(fm.stringWidth(n), total) + 14);
            RoundRectangle2D fundo = new RoundRectangle2D.Float((ROTULO_L - pw) / 2f, 0.5f, pw, base + 5, 10, 10);
            g.setColor(new Color(6, 9, 8, 165));
            g.fill(fundo);
            g.setColor(new Color(244, 241, 234, 24));
            g.draw(fundo);
        }
        texto(g, n, nx, ny, Estreia.BRANCO);
        if (chipW > 0) {
            Color cc = improvisado ? AMBAR : Estreia.OURO;
            RoundRectangle2D chip = new RoundRectangle2D.Float(x, base - 9.5f, chipW, 12, 6, 6);
            g.setColor(new Color(0, 0, 0, 120));
            g.fill(new RoundRectangle2D.Float(x, base - 8.5f, chipW, 12, 6, 6));
            g.setColor(cc);
            g.fill(chip);
            g.setFont(fp);
            g.setColor(Estreia.OURO_ESCURO);
            g.drawString(posicao, x + 4, base);
            x += chipW + gap;
        }
        if (txtW > 0) {
            g.setFont(fi);
            texto(g, txt, x, base, new Color(244, 241, 234, 225));
            x += txtW + gap;
        }
        if (barW > 0) {
            float pct = Math.max(0, Math.min(100, energia)) / 100f;
            int by = base - 5;
            g.setColor(new Color(0, 0, 0, 150));
            g.fill(new RoundRectangle2D.Float(x - 0.5f, by - 0.5f, barW + 1, 5, 5, 5));
            g.setColor(new Color(244, 241, 234, 40));
            g.fill(new RoundRectangle2D.Float(x, by, barW, 4, 4, 4));
            g.setColor(pct > 0.7f ? Tema.BOM : pct > 0.4f ? Tema.OURO : Tema.VERMELHO);
            g.fill(new RoundRectangle2D.Float(x, by, Math.max(4, barW * pct), 4, 4, 4));
            x += barW + gap;
        }
        if (ldW > 0) {
            g.setFont(fi);
            texto(g, ld, x, base, new Color(244, 241, 234, 170));
        }
        g.dispose();
        return img;
    }

    private static void texto(Graphics2D g, String s, int x, int y, Color cor) {
        g.setColor(SOMBRA);
        g.drawString(s, x + 0.6f, y + 1.1f);
        g.setColor(new Color(0, 0, 0, 90));
        g.drawString(s, x - 0.5f, y + 0.5f);
        g.setColor(cor);
        g.drawString(s, x, y);
    }
}
