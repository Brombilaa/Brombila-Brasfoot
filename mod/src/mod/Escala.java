package mod;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

// Reducao de imagem com qualidade, chamada pelo best.ah remendado (tools/escudos/remenda_ah.py)
// no lugar de Graphics2D.drawImage(img, x, y, w, h, obs). Reduz pela metade em passos com
// interpolacao bicubica ate chegar no tamanho, como um programa de imagem faria.
public final class Escala {
    private Escala() {}

    public static boolean desenha(Graphics2D g, Image img, int x, int y, int w, int h, ImageObserver obs) {
        // escudos pequenos (18 px das tabelas e listas, ate 32 px): brilho e sombra
        if (x == 0 && y == 0 && w == h && w >= 14 && w <= 32 && !"0".equals(ESTILO)) {
            try {
                Image r = reduz(img, w - 2, h - 2);
                g.drawImage(efeito(r, w, ESTILO), 0, 0, null);
                return true;
            } catch (Throwable t) {
                // cai no desenho normal
            }
        }
        try {
            int iw = img.getWidth(null), ih = img.getHeight(null);
            if (iw > 0 && ih > 0 && w > 0 && h > 0 && (iw != w || ih != h)) {
                Image atual = img;
                int cw = iw, ch = ih;
                while (cw / 2 >= w && ch / 2 >= h) {
                    cw = Math.max(w, cw / 2);
                    ch = Math.max(h, ch / 2);
                    atual = passo(atual, cw, ch);
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                return g.drawImage(atual, x, y, w, h, obs);
            }
        } catch (Throwable t) {
            // cai no desenho original
        }
        return g.drawImage(img, x, y, w, h, obs);
    }

    // "0" desliga; A sutil, B com aura dourada, C vidro (so para a previa)
    static String ESTILO = System.getProperty("brombila.escudo", "A");

    static Image reduz(Image img, int w, int h) {
        Image atual = img;
        int cw = img.getWidth(null), ch = img.getHeight(null);
        while (cw / 2 >= w && ch / 2 >= h) {
            cw = Math.max(w, cw / 2);
            ch = Math.max(h, ch / 2);
            atual = passo(atual, cw, ch);
        }
        return passo(atual, w, h);
    }

    // Escudo ja reduzido (lado - 2) centrado num quadrado de "lado" px: sombra macia embaixo,
    // brilho na metade de cima (so onde o escudo tem tinta) e um filete claro na borda de cima.
    static BufferedImage efeito(Image r, int lado, String estilo) {
        int ew = r.getWidth(null), eh = r.getHeight(null);
        BufferedImage esc = new BufferedImage(ew, eh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ge = esc.createGraphics();
        ge.drawImage(r, 0, 0, null);
        ge.dispose();
        int ox = (lado - ew) / 2, oy = 0;
        BufferedImage out = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_ARGB);
        // sombra: alfa do escudo borrado, deslocado 1 px para baixo
        float[] somb = sombra(esc, lado, ox, oy + 1);
        boolean aura = "B".equals(estilo);
        float[] halo = aura ? sombra(esc, lado, ox, oy) : null;
        if (aura) halo = borra(halo, lado);
        int[] px = out.getRGB(0, 0, lado, lado, null, 0, lado);
        for (int i = 0; i < px.length; i++) {
            float a = 0, rr = 0, gg = 0, bb = 0;
            if (aura) {
                float h = Math.min(1f, halo[i] * 0.55f);
                // aura dourada (#F2C14E)
                rr = 0xF2 * h; gg = 0xC1 * h; bb = 0x4E * h; a = h;
            }
            float s = Math.min(1f, somb[i] * ("C".equals(estilo) ? 0.8f : 0.65f));
            // sombra preta por cima da aura
            rr *= 1 - s; gg *= 1 - s; bb *= 1 - s; a = a + s * (1 - a);
            px[i] = cor(rr, gg, bb, a);
        }
        out.setRGB(0, 0, lado, lado, px, 0, lado);
        // escudo + brilho
        int[] e = esc.getRGB(0, 0, ew, eh, null, 0, ew);
        float forca = "C".equals(estilo) ? 0.42f : 0.26f;
        for (int y = 0; y < eh; y++) {
            for (int x = 0; x < ew; x++) {
                int c = e[y * ew + x];
                float ea = (c >>> 24) / 255f;
                if (ea <= 0) continue;
                float er = (c >> 16 & 255), eg = (c >> 8 & 255), eb = (c & 255);
                float t = y / (float) (eh - 1);
                float luz;
                if ("C".equals(estilo)) {
                    // faixa de vidro na diagonal + metade de cima mais clara
                    float d = (x + y * 0.8f) / (ew + eh * 0.8f);
                    luz = (t < 0.48f ? forca * (1 - t / 0.48f) : 0) + (Math.abs(d - 0.42f) < 0.07f ? 0.18f : 0);
                } else {
                    luz = t < 0.55f ? forca * (1 - t / 0.55f) : -0.10f * (t - 0.55f) / 0.45f;
                }
                // filete claro onde a borda de cima do escudo comeca
                if (y > 0 && (e[(y - 1) * ew + x] >>> 24) < 60 && ea > 0.5f) luz += 0.22f;
                else if (y == 0) luz += 0.22f;
                if (luz > 0) { er += (255 - er) * luz; eg += (255 - eg) * luz; eb += (255 - eb) * luz; }
                else { er *= 1 + luz; eg *= 1 + luz; eb *= 1 + luz; }
                int i = (y + oy) * lado + x + ox;
                int b = px[i];
                float ba = (b >>> 24) / 255f;
                float na = ea + ba * (1 - ea);
                float nr = (er * ea + (b >> 16 & 255) * (1 - ea)), ng = (eg * ea + (b >> 8 & 255) * (1 - ea)), nb = (eb * ea + (b & 255) * (1 - ea));
                px[i] = cor(nr * na, ng * na, nb * na, na);
            }
        }
        out.setRGB(0, 0, lado, lado, px, 0, lado);
        return out;
    }

    // cor premultiplicada -> ARGB
    private static int cor(float r, float g, float b, float a) {
        if (a <= 0.001f) return 0;
        int ia = Math.round(Math.min(1f, a) * 255);
        int ir = Math.min(255, Math.round(r / a)), ig = Math.min(255, Math.round(g / a)), ib = Math.min(255, Math.round(b / a));
        return ia << 24 | ir << 16 | ig << 8 | ib;
    }

    private static float[] sombra(BufferedImage esc, int lado, int ox, int oy) {
        float[] m = new float[lado * lado];
        for (int y = 0; y < esc.getHeight(); y++)
            for (int x = 0; x < esc.getWidth(); x++) {
                int yy = y + oy, xx = x + ox;
                if (yy < 0 || yy >= lado || xx < 0 || xx >= lado) continue;
                m[yy * lado + xx] = (esc.getRGB(x, y) >>> 24) / 255f;
            }
        return borra(m, lado);
    }

    private static float[] borra(float[] m, int lado) {
        float[] o = new float[m.length];
        float[] k = {0.25f, 0.5f, 0.25f};
        float[] t = new float[m.length];
        for (int y = 0; y < lado; y++)
            for (int x = 0; x < lado; x++) {
                float s = 0;
                for (int d = -1; d <= 1; d++) { int xx = x + d; if (xx >= 0 && xx < lado) s += m[y * lado + xx] * k[d + 1]; }
                t[y * lado + x] = s;
            }
        for (int y = 0; y < lado; y++)
            for (int x = 0; x < lado; x++) {
                float s = 0;
                for (int d = -1; d <= 1; d++) { int yy = y + d; if (yy >= 0 && yy < lado) s += t[yy * lado + x] * k[d + 1]; }
                o[y * lado + x] = s;
            }
        return o;
    }

    private static Image passo(Image img, int w, int h) {
        BufferedImage d = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = d.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return d;
    }
}
