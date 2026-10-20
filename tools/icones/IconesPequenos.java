import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

// Icones pequenos do jogo (tabelas, painel do jogador, menus) no estilo "Linha neon".
// Usa os helpers de IconesNeon. Cada icone tem uma cor de acento que guarda o
// significado do original (estrela dourada, excluir vermelho...).
// Uso: java IconesPequenos <pasta aicons original> <pasta de saida> [folha.png]
public class IconesPequenos extends IconesNeon {
    static final Color OURO = new Color(0xFFD84D), VERM = new Color(0xFF5A4D);

    static class Ic {
        String arq, nome; Color acento; List<F> g;
        Ic(String arq, String nome, Color acento) { this.arq = arq; this.nome = nome; this.acento = acento; this.g = new ArrayList<F>(); }
        Ic add(F f) { g.add(f); return this; }
    }

    static Shape texto(String s, double cx, double cy, double alt) {
        Font f = new Font("Inter", Font.BOLD, 100);
        Shape sh = new TextLayout(s, f, new FontRenderContext(null, true, true)).getOutline(null);
        Rectangle2D b = sh.getBounds2D();
        double k = alt / b.getHeight();
        AffineTransform at = AffineTransform.getTranslateInstance(cx, cy);
        at.scale(k, k);
        at.translate(-b.getCenterX(), -b.getCenterY());
        return at.createTransformedShape(sh);
    }
    static Shape pessoa(double cx, double top, double esc) {
        Path2D p = new Path2D.Double();
        p.append(new Ellipse2D.Double(cx - 3.2 * esc, top, 6.4 * esc, 6.4 * esc), false);
        p.moveTo(cx - 6.5 * esc, top + 17 * esc);
        p.curveTo(cx - 6.5 * esc, top + 11 * esc, cx - 3 * esc, top + 9 * esc, cx, top + 9 * esc);
        p.curveTo(cx + 3 * esc, top + 9 * esc, cx + 6.5 * esc, top + 11 * esc, cx + 6.5 * esc, top + 17 * esc);
        return p;
    }

    static List<Ic> lista() {
        List<Ic> l = new ArrayList<Ic>();
        l.add(new Ic("estrela", "Destaque", OURO).add(cd(estrela(12, 12.8, 10))));
        l.add(new Ic("estrelared", "Craque", VERM).add(cd(estrela(12, 12.8, 10))));
        l.add(new Ic("captain", "Capitão", NEON).add(t(Lz(3, 6, 21, 4, 21, 18, 3, 20))).add(cd(texto("C", 12, 12, 9))));
        l.add(new Ic("iconsetup", "Preferências", NEON).add(t(engrenagem(12, 12, 7, 2.2, 8))).add(td(O(12, 12, 2.8))));
        l.add(new Ic("icon_search", "Procurar", NEON).add(t(O(10.5, 10.5, 6.5))).add(td(L(15.3, 15.3, 20.5, 20.5))));
        l.add(new Ic("icondel", "Excluir", VERM).add(t(O(12, 12, 9.5))).add(td(L(8.2, 8.2, 15.8, 15.8))).add(td(L(15.8, 8.2, 8.2, 15.8))));
        l.add(new Ic("iconadd", "Adicionar", NEON).add(t(O(12, 12, 9.5))).add(td(L(12, 7.5, 12, 16.5))).add(td(L(7.5, 12, 16.5, 12))));
        l.add(new Ic("home", "Casa", NEON).add(t(Lz(3, 11.5, 12, 3.5, 21, 11.5, 19, 11.5, 19, 20.5, 5, 20.5, 5, 11.5))).add(td(RR(10, 14, 4, 6.5, 1.5))));
        l.add(new Ic("uparray", "Subir", NEON).add(t(O(12, 12, 9.5))).add(td(L(7.5, 14, 12, 9, 16.5, 14))));
        l.add(new Ic("downarray", "Descer", NEON).add(t(O(12, 12, 9.5))).add(td(L(7.5, 10, 12, 15, 16.5, 10))));
        l.add(new Ic("iconmoney2", "Dinheiro", NEON).add(cd(texto("$", 12, 12, 19))));
        l.add(new Ic("n03", "Finanças", NEON).add(t(RR(1.5, 5.5, 21, 13, 3))).add(cd(texto("$", 12, 12, 9))).add(c(O(5.5, 12, 1.1))).add(c(O(18.5, 12, 1.1))));
        l.add(new Ic("n04", "Mensagens", NEON).add(t(Lz(3, 5, 21, 5, 21, 16, 10, 16, 6, 20, 6, 16, 3, 16))).add(cd(O(8, 10.5, 1.2))).add(cd(O(12, 10.5, 1.2))).add(cd(O(16, 10.5, 1.2))));
        l.add(new Ic("iconfire", "Pedir demissão", VERM).add(t(L(13, 4, 4, 4, 4, 20, 13, 20))).add(td(L(9, 12, 20.5, 12))).add(td(L(16.5, 8, 20.5, 12, 16.5, 16))));
        l.add(new Ic("retire", "Aposentar", OURO).add(td(new Arc2D.Double(7, 10, 10, 10, 0, 180, Arc2D.OPEN))).add(td(L(12, 3, 12, 6)))
            .add(td(L(4.5, 7.5, 6.5, 9.5))).add(td(L(19.5, 7.5, 17.5, 9.5))).add(t(L(2.5, 15, 21.5, 15))).add(t(L(6, 19, 18, 19))));
        l.add(new Ic("je", "Editar jogador", NEON).add(t(pessoa(9, 3, 1))).add(td(L(14, 20, 15, 16, 20, 11, 21.5, 12.5, 16.5, 17.5, 14, 20))));
        l.add(new Ic("ja", "Novo jogador", NEON).add(t(pessoa(9, 3, 1))).add(td(L(18.5, 12, 18.5, 20))).add(td(L(14.5, 16, 22.5, 16))));
        l.add(new Ic("save1", "Salvar", NEON).add(t(Lz(3.5, 3.5, 17, 3.5, 20.5, 7, 20.5, 20.5, 3.5, 20.5))).add(t(RR(7.5, 3.5, 8, 5.5, 1))).add(td(RR(7, 13, 10, 7.5, 1.5))));
        l.add(new Ic("exit", "Remover", VERM).add(td(L(5.5, 5.5, 18.5, 18.5))).add(td(L(18.5, 5.5, 5.5, 18.5))));
        l.add(new Ic("iconchecked", "Confirmado", NEON).add(t(O(12, 12, 9.5))).add(td(L(7.5, 12.5, 10.5, 15.5, 16.5, 8.5))));
        l.add(new Ic("transfericon", "Transferência", NEON).add(t(L(4, 8.5, 19, 8.5))).add(t(L(15, 4.5, 19, 8.5, 15, 12.5)))
            .add(td(L(20, 15.5, 5, 15.5))).add(td(L(9, 11.5, 5, 15.5, 9, 19.5))));
        l.add(new Ic("loadt", "Carregar", NEON).add(td(L(12, 3, 12, 14))).add(td(L(7.5, 9.5, 12, 14, 16.5, 9.5))).add(t(L(3.5, 15, 3.5, 20.5, 20.5, 20.5, 20.5, 15))));
        return l;
    }

    // Linha neon com acento colorido; o brilho e da cor do acento.
    static void desenha(Graphics2D g, Ic ic, int w, int h) {
        int tam = Math.min(w, h);
        AffineTransform at = escala(tam, tam * 0.04);
        at.preConcatenate(AffineTransform.getTranslateInstance((w - tam) / 2.0, (h - tam) / 2.0));
        float lw = tam / 12.5f;
        BufferedImage gl = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = prep(gl.createGraphics());
        pintaGlifo(gg, ic.g, at, lw * 1.7f, NEON, ic.acento);
        gg.dispose();
        Composite o = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.drawImage(desfoca(gl, Math.max(1, Math.min(tam / 12, 12))), 0, 0, null);
        g.setComposite(o);
        // formas cheias com acento: preenchimento translucido + contorno
        for (F f : ic.g) {
            Shape s = at.createTransformedShape(f.s);
            Color cor = f.destaque ? ic.acento : NEON;
            if (f.cheio && f.s instanceof Path2D && !(f.s instanceof Path2D.Float && false) && ic.arq.startsWith("estrela")) {
                g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 70));
                g.fill(s);
                g.setColor(cor);
                g.setStroke(traco(lw));
                g.draw(s);
            } else {
                g.setColor(cor);
                if (f.cheio) g.fill(s); else { g.setStroke(traco(lw)); g.draw(s); }
            }
        }
    }

    // Desenha k vezes maior e reduz com media de area: bordas lisas mesmo em 16px.
    static BufferedImage render(Ic ic, int w, int h, int k) {
        BufferedImage big = new BufferedImage(w * k, h * k, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = prep(big.createGraphics());
        desenha(g, ic, w * k, h * k);
        g.dispose();
        if (k == 1) return big;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.drawImage(big.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g2.dispose();
        return img;
    }

    public static void main(String[] a) throws Exception {
        String orig = a[0], out = a[1];
        new File(out).mkdirs();
        new File(out, "4x").mkdirs();
        List<Ic> l = lista();
        for (Ic ic : l) {
            BufferedImage o = ImageIO.read(new File(orig, ic.arq + ".png"));
            ImageIO.write(render(ic, o.getWidth(), o.getHeight(), 8), "png", new File(out, ic.arq + ".png"));
            ImageIO.write(render(ic, o.getWidth() * 4, o.getHeight() * 4, 2), "png", new File(out, "4x/" + ic.arq + ".png"));
        }
        if (a.length > 2) folha(orig, out, a[2], l);
    }

    // Folha de aprovacao: original x novo, em tamanho real e ampliado 3x.
    static void folha(String orig, String novo, String arq, List<Ic> l) throws Exception {
        int cols = 4, cw = 350, ch = 112, top = 96;
        int rows = (l.size() + cols - 1) / cols;
        int W = cols * cw + 40, H = top + rows * ch + 20;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prep(img.createGraphics());
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x0F2218), 0, H, new Color(0x050D08)));
        g.fillRect(0, 0, W, H);
        g.setFont(new Font("Inter", Font.BOLD, 24)); g.setColor(NEON); g.drawString("Ícones pequenos — Linha neon (para aprovar)", 20, 42);
        g.setFont(new Font("Inter", Font.PLAIN, 14)); g.setColor(new Color(0x8FAE9A));
        g.drawString("Em cada quadro: original e novo no tamanho real do jogo, depois o novo em alta resolução", 20, 68);
                for (int i = 0; i < l.size(); i++) {
            Ic ic = l.get(i);
            int x = 20 + (i % cols) * cw, y = top + (i / cols) * ch;
            g.setColor(new Color(0, 0, 0, 90)); g.fillRoundRect(x, y, cw - 14, ch - 12, 14, 14);
            g.setColor(new Color(255, 255, 255, 22)); g.drawRoundRect(x, y, cw - 14, ch - 12, 14, 14);
            BufferedImage o = ImageIO.read(new File(orig, ic.arq + ".png"));
            BufferedImage n = ImageIO.read(new File(novo, ic.arq + ".png"));
            int cy = y + 50;
            g.drawImage(o, x + 18, cy - o.getHeight() / 2, null);
            g.setColor(new Color(0x8FAE9A)); g.setFont(new Font("Inter", Font.PLAIN, 16)); g.drawString("→", x + 52, cy + 6);
            g.drawImage(n, x + 78, cy - n.getHeight() / 2, null);
            BufferedImage z = ImageIO.read(new File(novo, "4x/" + ic.arq + ".png"));
            double kz = 76.0 / Math.max(z.getWidth(), z.getHeight());
            int zw = (int) Math.round(z.getWidth() * kz), zh = (int) Math.round(z.getHeight() * kz);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(z, x + 158 - zw / 2, cy - zh / 2, zw, zh, null);
            g.setFont(new Font("Inter", Font.BOLD, 14)); g.setColor(Color.WHITE); g.drawString(ic.nome, x + 216, cy - 4);
            g.setFont(new Font("Inter", Font.PLAIN, 11)); g.setColor(new Color(0x8FAE9A)); g.drawString(ic.arq + ".png", x + 216, cy + 14);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(arq));
    }
}
