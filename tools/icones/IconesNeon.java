import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// Gera os icones da barra de cima no estilo "Linha neon" (vetor, grade 24x24) para
// mod/assets/aicons. Uso: java IconesNeon <pasta aicons original> <pasta de saida> [folha]
// Com o terceiro argumento, monta tambem a folha de comparacao dos 3 estilos.
public class IconesNeon {
    static final Color NEON = new Color(0x3DFF8F), BASE = new Color(0x07120C), BRANCO = new Color(0xE3F1E8);

    // Uma forma do icone: tracada ou preenchida; "destaque" pinta com a cor de acento.
    static class F {
        Shape s; boolean cheio, destaque;
        F(Shape s, boolean cheio, boolean destaque) { this.s = s; this.cheio = cheio; this.destaque = destaque; }
    }
    static F t(Shape s) { return new F(s, false, false); }
    static F td(Shape s) { return new F(s, false, true); }
    static F c(Shape s) { return new F(s, true, false); }
    static F cd(Shape s) { return new F(s, true, true); }
    static Shape L(double... p) { Path2D a = new Path2D.Double(); a.moveTo(p[0], p[1]); for (int i = 2; i < p.length; i += 2) a.lineTo(p[i], p[i + 1]); return a; }
    static Shape Lz(double... p) { Path2D a = (Path2D) L(p); a.closePath(); return a; }
    static Shape RR(double x, double y, double w, double h, double r) { return new RoundRectangle2D.Double(x, y, w, h, r, r); }
    static Shape O(double cx, double cy, double r) { return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r); }
    static Shape E(double x, double y, double w, double h) { return new Ellipse2D.Double(x, y, w, h); }

    static Shape estrela(double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }
    static Shape engrenagem(double cx, double cy, double r, double dente, int n) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < n * 4; i++) {
            double a = i * Math.PI * 2 / (n * 4) - Math.PI / 2;
            double rr = (i % 4 == 0 || i % 4 == 1) ? r + dente : r;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    static String[] NOMES = {"Agenda", "Tabelas", "Estádio", "Juniores", "Times", "Procura", "Técnicos", "História", "Opções", "Escalar"};
    static String[] ARQ = {"icon02", "icon04", "icon05", "icon09", "icon06", "icon07", "icon08", "iconhistoria", "icon01", "tactics"};

    static List<F> glifo(int i) {
        List<F> g = new ArrayList<F>();
        switch (i) {
            case 0: // agenda
                g.add(t(RR(3, 5, 18, 16, 4))); g.add(t(L(3, 10, 21, 10))); g.add(t(L(8, 3, 8, 7))); g.add(t(L(16, 3, 16, 7)));
                g.add(cd(RR(13, 13, 5, 5, 1.5))); g.add(c(RR(6.5, 13, 3, 3, 1))); g.add(c(RR(6.5, 17, 3, 2.5, 1)));
                break;
            case 1: // tabelas (classificacao)
                g.add(t(RR(3, 3.5, 18, 17, 4)));
                g.add(cd(O(7.5, 8.5, 1.7))); g.add(td(L(11, 8.5, 18, 8.5)));
                g.add(c(O(7.5, 12.5, 1.5))); g.add(t(L(11, 12.5, 18, 12.5)));
                g.add(c(O(7.5, 16.5, 1.5))); g.add(t(L(11, 16.5, 16, 16.5)));
                break;
            case 2: // estadio
                g.add(t(E(2, 6, 20, 13))); g.add(t(RR(6.5, 9.5, 11, 6, 2))); g.add(td(L(12, 9.5, 12, 15.5))); g.add(td(O(12, 12.5, 1.6)));
                g.add(t(L(4, 3, 4, 6.5))); g.add(t(L(20, 3, 20, 6.5)));
                break;
            case 3: { // juniores (jogador com estrela)
                g.add(t(O(11, 7.5, 3.5)));
                Path2D b = new Path2D.Double(); b.moveTo(4, 21); b.curveTo(4, 15, 7.5, 13.5, 11, 13.5); b.curveTo(14.5, 13.5, 18, 15, 18, 21); g.add(t(b));
                g.add(cd(estrela(19, 5.5, 3.6)));
                break;
            }
            case 4: { // times (camisa)
                Path2D s = new Path2D.Double();
                s.moveTo(8.5, 3.5); s.lineTo(4, 5.5); s.lineTo(1.8, 10.5); s.lineTo(5.2, 12); s.lineTo(6.5, 9.8); s.lineTo(6.5, 20.5);
                s.lineTo(17.5, 20.5); s.lineTo(17.5, 9.8); s.lineTo(18.8, 12); s.lineTo(22.2, 10.5); s.lineTo(20, 5.5); s.lineTo(15.5, 3.5);
                s.curveTo(14.5, 6, 9.5, 6, 8.5, 3.5); s.closePath();
                g.add(t(s)); g.add(td(L(12, 8, 12, 20.5)));
                break;
            }
            case 5: // procura
                g.add(t(O(10.5, 10.5, 6.5))); g.add(td(L(15.3, 15.3, 20.5, 20.5)));
                g.add(c(O(10.5, 10.5, 2.2)));
                break;
            case 6: { // tecnicos (prancheta)
                g.add(t(RR(5, 4.5, 14, 17, 3))); g.add(c(RR(9, 2.5, 6, 3.6, 1.5)));
                g.add(td(L(8, 10, 11, 13))); g.add(td(L(11, 10, 8, 13)));
                g.add(t(O(15, 11.5, 1.8)));
                Path2D a = new Path2D.Double(); a.moveTo(8.5, 18); a.quadTo(12, 14, 15.5, 16); g.add(t(a)); g.add(t(L(14, 15, 15.6, 16.1, 14.5, 17.5)));
                break;
            }
            case 7: { // historia (relogio com seta voltando)
                Arc2D a = new Arc2D.Double(4, 4, 16, 16, 150, -300, Arc2D.OPEN); g.add(t(a));
                g.add(t(L(3.2, 6.5, 4.6, 10.2, 8.2, 9))); g.add(td(L(12, 8, 12, 12, 15, 14)));
                break;
            }
            case 8: // opcoes (engrenagem)
                g.add(t(engrenagem(12, 12, 7, 2.2, 8))); g.add(td(O(12, 12, 2.8)));
                break;
            case 10: // treino (cone)
                g.add(t(Lz(12, 3.5, 18, 19, 6, 19))); g.add(t(L(3.5, 20.5, 20.5, 20.5))); g.add(td(L(8.2, 13.5, 15.8, 13.5)));
                break;
            case 9: // escalar (prancheta tatica)
                g.add(t(RR(2.5, 4, 19, 16, 3))); g.add(t(L(12, 4, 12, 20))); g.add(t(O(12, 12, 2.8)));
                g.add(cd(O(6.5, 9, 1.4))); g.add(cd(O(6.5, 15, 1.4))); g.add(cd(O(16.5, 12, 1.4)));
                break;
        }
        return g;
    }

    interface Estilo { void desenha(Graphics2D g, List<F> fs, int tam, boolean hover); String nome(); }

    static Graphics2D prep(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
    static AffineTransform escala(int tam, double margem) {
        double k = (tam - 2 * margem) / 24.0;
        AffineTransform at = AffineTransform.getTranslateInstance(margem, margem); at.scale(k, k); return at;
    }
    static Stroke traco(float w) { return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }

    static void pintaGlifo(Graphics2D g, List<F> fs, AffineTransform at, float largura, Color cor, Color acento) {
        for (F f : fs) {
            Shape s = at.createTransformedShape(f.s);
            g.setColor(f.destaque ? acento : cor);
            if (f.cheio) g.fill(s); else { g.setStroke(traco(largura)); g.draw(s); }
        }
    }
    static BufferedImage desfoca(BufferedImage src, int raio) {
        int n = raio * 2 + 1; float[] k = new float[n * n]; float soma = 0;
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) { double d = Math.hypot(x - raio, y - raio); float v = (float) Math.exp(-d * d / (raio * raio / 1.5)); k[y * n + x] = v; soma += v; }
        for (int i = 0; i < k.length; i++) k[i] /= soma;
        BufferedImage pad = new BufferedImage(src.getWidth() + 2 * n, src.getHeight() + 2 * n, BufferedImage.TYPE_INT_ARGB);
        pad.createGraphics().drawImage(src, n, n, null);
        BufferedImage out = new ConvolveOp(new Kernel(n, n, k), ConvolveOp.EDGE_NO_OP, null).filter(pad, null);
        return out.getSubimage(n, n, src.getWidth(), src.getHeight());
    }

    // 1. Linha neon com brilho
    static class Linha implements Estilo {
        public String nome() { return "1. Linha neon"; }
        public void desenha(Graphics2D g, List<F> fs, int tam, boolean hover) {
            AffineTransform at = escala(tam, tam * 0.05);
            float w = Math.max(1.6f, tam / 16f);
            BufferedImage gl = new BufferedImage(tam, tam, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gg = prep(gl.createGraphics()); pintaGlifo(gg, fs, at, w * 1.6f, NEON, NEON); gg.dispose();
            BufferedImage bl = desfoca(gl, Math.max(2, tam / 14));
            Composite o = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hover ? 0.95f : 0.55f));
            g.drawImage(bl, 0, 0, null); g.setComposite(o);
            pintaGlifo(g, fs, at, w, hover ? new Color(0xB8FFD6) : NEON, hover ? Color.WHITE : new Color(0xB8FFD6));
        }
    }
    // 2. Solido em ladrilho de vidro
    static class Ladrilho implements Estilo {
        public String nome() { return "2. Ladrilho de vidro"; }
        public void desenha(Graphics2D g, List<F> fs, int tam, boolean hover) {
            double m = tam * 0.04;
            Shape tile = new RoundRectangle2D.Double(m, m, tam - 2 * m, tam - 2 * m, tam * 0.32, tam * 0.32);
            g.setPaint(new GradientPaint(0, 0, hover ? new Color(0x1F6B44) : new Color(0x1A3326), 0, tam, hover ? new Color(0x0E3A23) : new Color(0x0B1811)));
            g.fill(tile);
            g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 70), 0, tam * 0.6f, new Color(255, 255, 255, 0)));
            g.fill(new RoundRectangle2D.Double(m + 1, m + 1, tam - 2 * m - 2, (tam - 2 * m) * 0.5, tam * 0.3, tam * 0.3));
            g.setStroke(new BasicStroke(1f));
            g.setColor(hover ? NEON : new Color(61, 255, 143, 110)); g.draw(tile);
            AffineTransform at = escala(tam, tam * 0.22);
            pintaGlifo(g, fs, at, Math.max(1.5f, tam / 17f), BRANCO, NEON);
        }
    }
    // 3. Duotone (branco + neon, partes cheias translucidas)
    static class Duotone implements Estilo {
        public String nome() { return "3. Duotone"; }
        public void desenha(Graphics2D g, List<F> fs, int tam, boolean hover) {
            AffineTransform at = escala(tam, tam * 0.05);
            float w = Math.max(1.6f, tam / 15f);
            // preenchimento translucido das formas fechadas
            for (F f : fs) {
                if (f.cheio || f.destaque) continue;
                Shape s = at.createTransformedShape(f.s);
                if (s instanceof Path2D || f.s instanceof RectangularShape) { g.setColor(new Color(61, 255, 143, hover ? 70 : 38)); g.fill(s); }
            }
            pintaGlifo(g, fs, at, w, hover ? Color.WHITE : BRANCO, NEON);
        }
    }

    // Glifo escuro sem brilho, para icone que fica dentro de botao neon (tactics.png).
    static void escuro(Graphics2D g, List<F> fs, int tam) {
        pintaGlifo(g, fs, escala(tam, tam * 0.04), Math.max(1.8f, tam / 12f), new Color(0x04150B), new Color(0x04150B));
    }

    public static void main(String[] a) throws Exception {
        String orig = a[0], out = a[1];
        new File(out).mkdirs();
        String[] arqs = {"icon02", "icon04", "icon05", "icon09", "icon06", "icon07", "icon08", "iconhistoria", "icon01", "tactics", "icon03"};
        for (int i = 0; i < arqs.length; i++) {
            BufferedImage o = ImageIO.read(new File(orig, arqs[i] + ".png"));
            int w = o.getWidth(), h = o.getHeight(), tam = Math.min(w, h);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = prep(img.createGraphics());
            g.translate((w - tam) / 2, (h - tam) / 2);
            if (arqs[i].equals("tactics")) escuro(g, glifo(i), tam); else new Linha().desenha(g, glifo(i), tam, false);
            g.dispose();
            ImageIO.write(img, "png", new File(out, arqs[i] + ".png"));
        }
        if (a.length > 2) folha(orig, a[2]);
    }

    static void folha(String orig, String out) throws Exception {
        Estilo[] es = {new Linha(), new Ladrilho(), new Duotone()};
        new File(out).mkdirs();
        // PNGs 35x35 de cada estilo (o tamanho que o jogo usa)
        for (Estilo e : es) {
            File d = new File(out, "png-" + e.nome().substring(3).replace(' ', '-').toLowerCase());
            d.mkdirs();
            for (int i = 0; i < NOMES.length; i++) {
                BufferedImage img = new BufferedImage(35, 35, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = prep(img.createGraphics()); e.desenha(g, glifo(i), 35, false); g.dispose();
                ImageIO.write(img, "png", new File(d, ARQ[i] + ".png"));
            }
        }
        // folha de comparacao: cada linha = barra de cima do jogo, em 2x
        int Z = 2, col = 92, larg = 40 + col * NOMES.length, alt = 150;
        int W = larg * Z, H = (90 + alt * 4) * Z;
        BufferedImage folha = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prep(folha.createGraphics());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(Z, Z);
        g.setPaint(new GradientPaint(0, 0, new Color(0x0F2218), 0, H / Z, new Color(0x050D08))); g.fillRect(0, 0, W, H);
        g.setFont(new Font("Inter", Font.BOLD, 22)); g.setColor(NEON); g.drawString("Ícones da barra de cima — opções", 20, 42);
        g.setFont(new Font("Inter", Font.PLAIN, 13)); g.setColor(new Color(0x8FAE9A));
        g.drawString("tamanho real do jogo (35 px), ampliado 2x · o último de cada linha está com o mouse em cima", 20, 62);
        for (int r = 0; r < 4; r++) {
            int y0 = 80 + r * alt;
            g.setColor(new Color(0, 0, 0, 90)); g.fillRoundRect(14, y0, larg - 28, alt - 16, 16, 16);
            g.setColor(new Color(255, 255, 255, 25)); g.drawRoundRect(14, y0, larg - 28, alt - 16, 16, 16);
            g.setFont(new Font("Inter", Font.BOLD, 15)); g.setColor(r == 0 ? new Color(0xC9A0A0) : Color.WHITE);
            g.drawString(r == 0 ? "Original (atual)" : es[r - 1].nome(), 30, y0 + 26);
            for (int i = 0; i < NOMES.length; i++) {
                int x = 30 + i * col + (col - 35) / 2, y = y0 + 42;
                boolean hov = i == NOMES.length - 1;
                if (r == 0) {
                    BufferedImage o = ImageIO.read(new File(orig, ARQ[i] + ".png"));
                    g.drawImage(o, x + (35 - o.getWidth()) / 2, y + (35 - o.getHeight()) / 2, null);
                } else {
                    BufferedImage img = new BufferedImage(35, 35, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gi = prep(img.createGraphics()); es[r - 1].desenha(gi, glifo(i), 35, hov); gi.dispose();
                    if (hov) {
                        g.setPaint(new RadialGradientPaint(x + 17.5f, y + 60, 50, new float[] {0f, 1f}, new Color[] {new Color(61, 255, 143, 70), new Color(61, 255, 143, 0)}));
                        g.fillRect(x - 30, y - 10, 95, 85);
                    }
                    g.drawImage(img, x, y, null);
                }
                g.setFont(new Font("Inter", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                g.setColor(hov && r > 0 ? NEON : Color.WHITE);
                g.drawString(NOMES[i], 30 + i * col + (col - fm.stringWidth(NOMES[i])) / 2, y + 56);
                if (hov && r > 0) { g.setColor(NEON); g.fillRect(30 + i * col + 18, y + 64, col - 36, 2); }
            }
        }
        g.dispose();
        ImageIO.write(folha, "png", new File(out, "comparacao-icones.png"));
    }
}
