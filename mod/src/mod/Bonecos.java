package mod;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.InflaterInputStream;

// Bonecos 3D da partida. Os quadros vem prontos de tools/tv/sprites.py (4 cortes de cabelo x
// 8 direcoes x 9 poses) e guardam, por pixel, a luz, o brilho, o material e a coordenada na
// imagem da camisa do time. Aqui cada quadro e pintado na hora com o uniforme e a pele/cabelo
// do jogador, entao qualquer time do jogo aparece com a propria camisa.
final class Bonecos {

    static final int PELE = 0, CABELO = 1, CAMISA = 2, CALCAO = 3, MEIA = 4, CHUTEIRA = 5, SOLA = 6, OLHO = 7, BOCA = 8, FAIXA = 9;
    static final int COSTAS = 16, VAZIO = 255, TEX = 50;

    final int cortes, dirs, poses, largura, altura;
    final float escala, peY;          // px por metro no tamanho original; linha dos pes
    final short[] x0, y0, w, h;
    final byte[][] dados;             // planos L, A, M, U, V (w*h cada, em sequencia)

    private static final Map<String, Bonecos> carregados = new HashMap<String, Bonecos>();
    private static final int[] GAMA = new int[1024];
    static {
        for (int i = 0; i < GAMA.length; i++) GAMA[i] = (int) Math.round(255 * Math.pow(Math.min(1.0, i / 1023.0), 1 / 1.1));
    }

    static synchronized Bonecos carrega(String nome) {
        if (carregados.containsKey(nome)) return carregados.get(nome);
        Bonecos b = null;
        try {
            InputStream in = Bonecos.class.getResourceAsStream("/mod/bonecos/" + nome + ".bin");
            if (in != null) b = new Bonecos(in);
        } catch (Throwable t) { t.printStackTrace(); }
        carregados.put(nome, b);
        return b;
    }

    private Bonecos(InputStream bruto) throws Exception {
        DataInputStream in = new DataInputStream(bruto);
        byte[] magia = new byte[4];
        in.readFully(magia);
        if (magia[0] != 'B' || magia[3] != '1') throw new IllegalStateException("bonecos: arquivo invalido");
        cortes = in.readInt(); dirs = in.readInt(); poses = in.readInt();
        largura = in.readInt(); altura = in.readInt();
        escala = in.readFloat(); peY = in.readFloat();
        DataInputStream z = new DataInputStream(new java.io.BufferedInputStream(new InflaterInputStream(in), 1 << 16));
        int n = cortes * dirs * poses;
        x0 = new short[n]; y0 = new short[n]; w = new short[n]; h = new short[n];
        dados = new byte[n][];
        for (int i = 0; i < n; i++) {
            x0[i] = z.readShort(); y0[i] = z.readShort(); w[i] = z.readShort(); h[i] = z.readShort();
            dados[i] = new byte[w[i] * h[i] * 5];
            z.readFully(dados[i]);
        }
        z.close();
    }

    int quadro(int corte, int dir, int pose) {
        return (Math.floorMod(corte, cortes) * dirs + Math.floorMod(dir, dirs)) * poses + Math.max(0, Math.min(poses - 1, pose));
    }

    // ------------------------------------------------------------------ uniforme e corpo

    static final class Kit {
        final int[] camisa = new int[TEX * TEX];
        int calcao, meia, faixa;
        boolean clara;                // camisa clara: numero escuro
        Color media = Color.GRAY;      // cor media do peito
        final Map<Integer, byte[]> numeros = new HashMap<Integer, byte[]>();

        // camisa do jogo (imagem 50x50 com fundo transparente) vira textura; o calcao e o
        // meiao saem das cores do clube
        static Kit de(Image img, Color c1, Color c2) {
            Kit k = new Kit();
            BufferedImage t = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
            if (img != null) {
                Graphics2D g = t.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, TEX, TEX, null);
                g.dispose();
            } else {
                Graphics2D g = t.createGraphics();
                g.setColor(c1);
                g.fillRect(0, 0, TEX, TEX);
                g.setColor(c2);
                g.fillRect(0, 0, TEX, 5);
                g.fillRect(0, 18, 14, 3);
                g.fillRect(36, 18, 14, 3);
                g.dispose();
            }
            t.getRGB(0, 0, TEX, TEX, k.camisa, 0, TEX);
            preenche(k.camisa);
            // cor media do peito decide calcao e meiao
            long r = 0, gg = 0, b = 0;
            int n = 0;
            for (int y = 14; y < 40; y++) for (int x = 17; x < 33; x++) {
                int p = k.camisa[y * TEX + x];
                r += (p >> 16) & 255; gg += (p >> 8) & 255; b += p & 255; n++;
            }
            Color media = new Color((int) (r / n), (int) (gg / n), (int) (b / n));
            k.media = media;
            k.clara = Partida.brilho(media) > 0.6;
            Color outra = Partida.distancia(media, c1) <= Partida.distancia(media, c2) ? c2 : c1;
            Color mesma = outra == c2 ? c1 : c2;
            if (Partida.distancia(outra, media) < 50) outra = k.clara ? new Color(0x1A1A1A) : new Color(0xF2F2EE);
            k.calcao = outra.getRGB();
            k.meia = outra.getRGB();
            k.faixa = mesma.getRGB();
            return k;
        }

        // pixels transparentes da camisa (bordas, axilas) pegam a cor do vizinho
        static void preenche(int[] px) {
            for (int volta = 0; volta < 12; volta++) {
                boolean falta = false;
                int[] novo = px.clone();
                for (int y = 0; y < TEX; y++) for (int x = 0; x < TEX; x++) {
                    int i = y * TEX + x;
                    if ((px[i] >>> 24) > 100) continue;
                    long r = 0, g = 0, b = 0;
                    int n = 0;
                    for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx, yy = y + dy;
                        if (xx < 0 || yy < 0 || xx >= TEX || yy >= TEX) continue;
                        int p = px[yy * TEX + xx];
                        if ((p >>> 24) <= 100) continue;
                        r += (p >> 16) & 255; g += (p >> 8) & 255; b += p & 255; n++;
                    }
                    if (n > 0) novo[i] = 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
                    else falta = true;
                }
                System.arraycopy(novo, 0, px, 0, px.length);
                if (!falta) break;
            }
            for (int i = 0; i < px.length; i++) if ((px[i] >>> 24) <= 100) px[i] = 0xFF808080;
        }

        // mascara 32x32 do numero das costas
        byte[] numero(int n) {
            byte[] m = numeros.get(n);
            if (m != null) return m;
            BufferedImage im = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font f = Estreia.titulo(34f);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            String s = String.valueOf(n);
            g.setColor(Color.WHITE);
            g.drawString(s, (32 - fm.stringWidth(s)) / 2f, 16 + fm.getAscent() * 0.36f);
            g.dispose();
            m = new byte[32 * 32];
            for (int i = 0; i < m.length; i++) m[i] = (byte) ((im.getRGB(i % 32, i / 32) >>> 24));
            numeros.put(n, m);
            return m;
        }
    }

    static final class Corpo {
        int pele, cabelo, corte;
        static final int[] PELES = {0xF1C7A1, 0xE0AC84, 0xC98E64, 0xB97A52, 0x8D5A3B, 0x5B3A26};
        static final int[] CABELOS = {0x1C1410, 0x0E0B09, 0x3B2A1C, 0x6B4A2A, 0xA07A4A};

        // sempre o mesmo visual para o mesmo jogador (sai do nome)
        static Corpo de(String nome) {
            int h = nome == null ? 0 : nome.hashCode();
            h ^= h >>> 13; h *= 0x5bd1e995; h ^= h >>> 15;
            Corpo c = new Corpo();
            int ip = Math.floorMod(h, PELES.length);
            c.pele = PELES[ip];
            int ic = Math.floorMod(h >>> 5, 10);
            c.cabelo = ip >= 4 ? CABELOS[ic % 2] : CABELOS[ic % CABELOS.length];
            int k = Math.floorMod(h >>> 11, 10);
            // 0 curto, 1 black, 2 raspado, 3 topete
            if (ip >= 4) c.corte = k < 4 ? 1 : k < 8 ? 2 : 0;
            else c.corte = k < 5 ? 0 : k < 7 ? 2 : k < 9 ? 3 : 1;
            return c;
        }
    }

    // ------------------------------------------------------------------ pintura

    // pinta o quadro q em img (reaproveitada se couber); devolve a imagem usada
    BufferedImage pinta(int q, Kit k, Corpo c, int numero, BufferedImage img) {
        int W = Math.max(1, w[q]), H = Math.max(1, h[q]);
        if (img == null || img.getWidth() < W || img.getHeight() < H)
            img = new BufferedImage(Math.max(W, img == null ? 0 : img.getWidth()), Math.max(H, img == null ? 0 : img.getHeight()), BufferedImage.TYPE_INT_ARGB);
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int iw = img.getWidth();
        byte[] d = dados[q];
        int n = w[q] * h[q];
        byte[] num = numero > 0 ? k.numero(numero) : null;
        int corNum = k.clara ? 0x15130E : 0xF7F7F2;
        for (int y = 0; y < img.getHeight(); y++) java.util.Arrays.fill(px, y * iw, y * iw + iw, 0);
        for (int i = 0; i < n; i++) {
            int m = d[2 * n + i] & 255;
            if (m == VAZIO) continue;
            int mat = m & 15, base;
            switch (mat) {
                case PELE: base = c.pele; break;
                case CABELO: base = c.cabelo; break;
                case CALCAO: base = k.calcao; break;
                case MEIA: base = k.meia; break;
                case FAIXA: base = k.faixa; break;
                case CHUTEIRA: base = 0x171717; break;
                case SOLA: base = 0x2A2A2A; break;
                case OLHO: base = 0x0F0C0C; break;
                case BOCA: base = escurece(c.pele); break;
                case CAMISA: {
                    int u = d[3 * n + i] & 255, v = d[4 * n + i] & 255;
                    int tx = Math.min(TEX - 1, u * TEX / 256), ty = Math.min(TEX - 1, v * TEX / 256);
                    base = k.camisa[ty * TEX + tx];
                    if ((m & COSTAS) != 0 && num != null) {
                        // numero: caixa no meio das costas (x -0.12..0.12 m, y 0.25..0.5 m do tronco)
                        double x = (0.5 - u / 255.0) / 0.23 * 0.22, yy = 0.58 - (v / 255.0 - 0.08) / 0.86 * 0.625;
                        double nu = (0.12 - x) / 0.24, nv = (0.5 - yy) / 0.25;
                        if (nu >= 0 && nu < 1 && nv >= 0 && nv < 1) {
                            int a = num[(int) (nv * 32) * 32 + (int) (nu * 32)] & 255;
                            if (a > 0) base = mistura(base, corNum, a);
                        }
                    }
                    break;
                }
                default: base = 0x808080;
            }
            int L = d[i] & 255, A = d[n + i] & 255;
            int r = (((base >> 16) & 255) * L) / 100 + A, g = (((base >> 8) & 255) * L) / 100 + (A * 95) / 100, b = ((base & 255) * L) / 100 + (A * 85) / 100;
            r = GAMA[Math.min(1023, r * 4)]; g = GAMA[Math.min(1023, g * 4)]; b = GAMA[Math.min(1023, b * 4)];
            px[(i / w[q]) * iw + (i % w[q])] = 0xFF000000 | r << 16 | g << 8 | b;
        }
        return img;
    }

    static int escurece(int c) {
        return (((c >> 16) & 255) * 55 / 100) << 16 | (((c >> 8) & 255) * 55 / 100) << 8 | ((c & 255) * 55 / 100);
    }

    static int mistura(int a, int b, int alfa) {
        int r = ((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * alfa) / 255;
        int g = ((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * alfa) / 255;
        int bl = (a & 255) + (((b & 255) - (a & 255)) * alfa) / 255;
        return r << 16 | g << 8 | bl;
    }
}
