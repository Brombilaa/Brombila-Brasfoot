package mod;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

// Bola de futebol de verdade (gomos pretos e brancos, costuras, luz e brilho), desenhada uma
// vez em varios angulos de giro e depois so escalada na tela. O giro acompanha o quanto a
// bola andou, entao ela rola e gira no chute.
final class Bola {

    static final int LADO = 72, GIROS = 32;
    private static BufferedImage[] quadros;

    private static synchronized BufferedImage[] quadros() {
        if (quadros == null) quadros = gera();
        return quadros;
    }

    // centros dos gomos: 12 pentagonos (vertices do icosaedro) e 20 hexagonos (faces)
    private static double[][] centros() {
        double f = (1 + Math.sqrt(5)) / 2;
        double[][] c = new double[32][];
        int n = 0;
        for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
            c[n++] = new double[]{0, a, b * f};
            c[n++] = new double[]{a, b * f, 0};
            c[n++] = new double[]{b * f, 0, a};
        }
        for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) for (int d = -1; d <= 1; d += 2) c[n++] = new double[]{a, b, d};
        double g = 1 / f;
        for (int a = -1; a <= 1; a += 2) for (int b = -1; b <= 1; b += 2) {
            c[n++] = new double[]{0, a * g, b * f};
            c[n++] = new double[]{a * g, b * f, 0};
            c[n++] = new double[]{b * f, 0, a * g};
        }
        for (double[] v : c) {
            double l = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            v[0] /= l; v[1] /= l; v[2] /= l;
        }
        return c;
    }

    // gira todos os centros para levar o vetor p ate a (Rodrigues)
    private static void alinha(double[][] c, double[] p0, double[] a) {
        double[] p = p0.clone();
        double vx = p[1] * a[2] - p[2] * a[1], vy = p[2] * a[0] - p[0] * a[2], vz = p[0] * a[1] - p[1] * a[0];
        double co = p[0] * a[0] + p[1] * a[1] + p[2] * a[2], s2 = vx * vx + vy * vy + vz * vz;
        if (s2 < 1e-12) return;
        double k = (1 - co) / s2;
        double[][] R = {
            {1 - k * (vy * vy + vz * vz), -vz + k * vx * vy, vy + k * vx * vz},
            {vz + k * vx * vy, 1 - k * (vx * vx + vz * vz), -vx + k * vy * vz},
            {-vy + k * vx * vz, vx + k * vy * vz, 1 - k * (vx * vx + vy * vy)}};
        for (double[] v : c) {
            double x = R[0][0] * v[0] + R[0][1] * v[1] + R[0][2] * v[2];
            double y = R[1][0] * v[0] + R[1][1] * v[1] + R[1][2] * v[2];
            double z = R[2][0] * v[0] + R[2][1] * v[1] + R[2][2] * v[2];
            v[0] = x; v[1] = y; v[2] = z;
        }
    }

    private static BufferedImage[] gera() {
        double[][] c = centros();
        BufferedImage[] out = new BufferedImage[GIROS];
        int S = LADO * 2;                       // supersample 2x
        double lx = -0.45, ly = -0.62, lz = 0.64;
        double ln = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ln; ly /= ln; lz /= ln;
        // eixo do giro quase na horizontal (a bola rola para a frente); um pentagono fica no
        // eixo, entao 72 graus de giro repetem o desenho e o ciclo de quadros fecha sem pulo
        double ax = 0.93, ay = 0.25, az = 0.27, an = Math.sqrt(ax * ax + ay * ay + az * az);
        ax /= an; ay /= an; az /= an;
        alinha(c, c[0], new double[]{ax, ay, az});
        for (int q = 0; q < GIROS; q++) {
            double ang = 2 * Math.PI / 5 * q / GIROS;
            double cs = Math.cos(ang), sn = Math.sin(ang), t = 1 - cs;
            double[][] R = {
                {t * ax * ax + cs, t * ax * ay - sn * az, t * ax * az + sn * ay},
                {t * ax * ay + sn * az, t * ay * ay + cs, t * ay * az - sn * ax},
                {t * ax * az - sn * ay, t * ay * az + sn * ax, t * az * az + cs}};
            int[] px = new int[LADO * LADO];
            for (int y = 0; y < LADO; y++) for (int x = 0; x < LADO; x++) {
                int r = 0, g = 0, b = 0, a = 0;
                for (int sy = 0; sy < 2; sy++) for (int sx = 0; sx < 2; sx++) {
                    double u = ((x * 2 + sx + 0.5) / S) * 2 - 1, v = ((y * 2 + sy + 0.5) / S) * 2 - 1;
                    double d2 = u * u + v * v;
                    if (d2 > 1) continue;
                    double nz = Math.sqrt(1 - d2);
                    // normal na bola girada
                    double bx = R[0][0] * u + R[0][1] * v + R[0][2] * nz;
                    double by = R[1][0] * u + R[1][1] * v + R[1][2] * nz;
                    double bz = R[2][0] * u + R[2][1] * v + R[2][2] * nz;
                    double m1 = -2, m2 = -2;
                    int i1 = 0;
                    for (int k = 0; k < c.length; k++) {
                        // face do icosaedro truncado: os pentagonos ficam mais longe do centro (menores)
                        double d = (bx * c[k][0] + by * c[k][1] + bz * c[k][2]) * (k < 12 ? 0.9741 : 1);
                        if (d > m1) { m2 = m1; m1 = d; i1 = k; } else if (d > m2) m2 = d;
                    }
                    double base = i1 < 12 ? 0.11 : 0.95;           // pentagono preto, hexagono branco
                    if (m1 - m2 < 0.0055) base = i1 < 12 ? 0.3 : 0.7;  // costura
                    double dif = Math.max(0, u * lx + v * ly + nz * lz);
                    double luz = 0.42 + 0.64 * dif;
                    double borda = 0.75 + 0.25 * nz;               // escurece na borda
                    double hx = lx, hy = ly, hz = lz + 1, hn = Math.sqrt(hx * hx + hy * hy + hz * hz);
                    double spec = Math.pow(Math.max(0, (u * hx + v * hy + nz * hz) / hn), 38) * 0.55;
                    double cr = base * luz * borda + spec, cg = cr, cb = base * luz * borda * 0.97 + spec;
                    r += (int) Math.min(255, cr * 255); g += (int) Math.min(255, cg * 255); b += (int) Math.min(255, cb * 255);
                    a += 255;
                }
                if (a == 0) continue;
                int n = a / 255;
                px[y * LADO + x] = (a / 4) << 24 | (r / n) << 16 | (g / n) << 8 | (b / n);
            }
            BufferedImage im = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
            im.setRGB(0, 0, LADO, LADO, px, 0, LADO);
            out[q] = im;
        }
        return out;
    }

    // bola com centro (cx, cy), raio r em pixels e giro em radianos (quanto ela ja rolou)
    static void desenha(Graphics2D g, double cx, double cy, double r, double giro) {
        if (r < 3.2) {
            Ellipse2D.Double e = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
            g.setColor(new Color(0xF1EFEA));
            g.fill(e);
            g.setColor(new Color(0x3A3A3A));
            g.draw(e);
            return;
        }
        BufferedImage[] q = quadros();
        int i = Math.floorMod((int) Math.floor(giro / (2 * Math.PI / 5) * GIROS), GIROS);
        Object antes = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int x0 = (int) Math.round(cx - r), y0 = (int) Math.round(cy - r), d = (int) Math.round(r * 2);
        g.drawImage(q[i], x0, y0, d, d, null);
        if (antes != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
    }
}
