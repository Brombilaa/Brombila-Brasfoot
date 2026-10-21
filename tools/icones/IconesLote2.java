import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

// Segundo lote de icones "Linha neon": painel do jogador (vender, contrato, chuteiras...)
// e eventos da partida (cartoes, gol, substituicao, setas, trofeus).
// Uso: java IconesLote2 <raiz do jar original> <pasta de saida> [folha.png]
public class IconesLote2 extends IconesPequenos {
    static final Color PRATA = new Color(0xD5E2DA), AMARELO = new Color(0xFFD23F), CINZA = new Color(0x8FAE9A);

    static Ic tinta(Ic ic, Color c) { ic.cor = c; ic.acento = c; return ic; }

    // Bola: pentagono cheio no centro, gomos cheios cortados na borda e costuras finas.
    static Shape pentagono(double cx, double cy, double r, double giro) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + giro + i * Math.PI * 2 / 5;
            double x = cx + Math.cos(a) * r, y = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }
    // Bola classica: pentagono preto no centro, costuras dos hexagonos e pentagonos cortados na borda.
    // Bola classica: projecao de um icosaedro truncado (12 pentagonos pretos + 20 hexagonos),
    // olhando de frente para um pentagono. Pentagonos visiveis cheios, costuras visiveis tracadas.
    static Ic bola(Ic ic, double cx, double cy, double r) {
        double f = (1 + Math.sqrt(5)) / 2;
        List<double[]> v = new ArrayList<double[]>();
        double[][] base = {{0, 1, 3 * f}, {1, 2 + f, 2 * f}, {f, 2, f * f * f}};
        for (double[] b0 : base)
            for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) for (int sz = -1; sz <= 1; sz += 2) {
                double[] q = {b0[0] * sx, b0[1] * sy, b0[2] * sz};
                for (int k = 0; k < 3; k++) {
                    double[] w = {q[k % 3], q[(k + 1) % 3], q[(k + 2) % 3]};
                    boolean novo = true;
                    for (double[] u : v) if (Math.abs(u[0] - w[0]) + Math.abs(u[1] - w[1]) + Math.abs(u[2] - w[2]) < 1e-6) novo = false;
                    if (novo) v.add(w);
                }
            }
        // gira para que o pentagono na direcao (0, 1, f) fique de frente (+z), com um pequeno giro extra
        double[] eixo = norm(new double[] {0, 1, f});
        double[][] m = rotPara(eixo);
        double raio = Math.sqrt(9 * f * f + 1);
        List<double[]> p = new ArrayList<double[]>();
        for (double[] w : v) p.add(mul(m, new double[] {w[0] / raio, w[1] / raio, w[2] / raio}));
        Area borda = new Area(O(cx, cy, r));
        Color couro = mistura(ic.cor, Color.WHITE, 0.6f), tinta = new Color(0x0B1811);
        ic.add(c(O(cx, cy, r)).cor(couro));
        // pentagonos: direcoes dos 12 vertices do icosaedro
        List<double[]> dirs = new ArrayList<double[]>();
        for (int sa = -1; sa <= 1; sa += 2) for (int sb = -1; sb <= 1; sb += 2) {
            double[] q = {0, sa, sb * f};
            for (int k = 0; k < 3; k++) dirs.add(mul(m, norm(new double[] {q[k % 3], q[(k + 1) % 3], q[(k + 2) % 3]})));
        }
        for (final double[] d : dirs) {
            if (d[2] < 0.2) continue;
            List<double[]> cinco = new ArrayList<double[]>(p);
            java.util.Collections.sort(cinco, new java.util.Comparator<double[]>() {
                public int compare(double[] x, double[] y) { return Double.compare(dot(y, d), dot(x, d)); }
            });
            final List<double[]> pt = cinco.subList(0, 5);
            final double[] c = {0, 0, 0};
            for (double[] w : pt) { c[0] += w[0] / 5; c[1] += w[1] / 5; }
            List<double[]> ord = new ArrayList<double[]>(pt);
            java.util.Collections.sort(ord, new java.util.Comparator<double[]>() {
                public int compare(double[] x, double[] y) { return Double.compare(Math.atan2(x[1] - c[1], x[0] - c[0]), Math.atan2(y[1] - c[1], y[0] - c[0])); }
            });
            Path2D pg = new Path2D.Double();
            for (int i = 0; i < 5; i++) {
                double[] w = ord.get(i);
                double x = cx + w[0] * r, y = cy - w[1] * r;
                if (i == 0) pg.moveTo(x, y); else pg.lineTo(x, y);
            }
            pg.closePath();
            Area a0 = new Area(pg); a0.intersect(borda);
            if (!a0.isEmpty()) ic.add(c(a0).cor(tinta));
        }
        // costuras: arestas (distancia 2 no tamanho original) com o meio na frente
        double lado = 2 / raio;
        for (int i = 0; i < p.size(); i++) for (int j = i + 1; j < p.size(); j++) {
            double[] a1 = p.get(i), b1 = p.get(j);
            double dx = a1[0] - b1[0], dy = a1[1] - b1[1], dz = a1[2] - b1[2];
            if (Math.abs(Math.sqrt(dx * dx + dy * dy + dz * dz) - lado) > 1e-3) continue;
            if ((a1[2] + b1[2]) / 2 < 0.3) continue;
            ic.add(t(L(cx + a1[0] * r, cy - a1[1] * r, cx + b1[0] * r, cy - b1[1] * r)).cor(tinta));
        }
        ic.traco = 0.32f;
        ic.add(t(O(cx, cy, r)));
        return ic;
    }
    static Color mistura(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
    static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    static double[] norm(double[] a) { double n = Math.sqrt(dot(a, a)); return new double[] {a[0] / n, a[1] / n, a[2] / n}; }
    static double[] mul(double[][] m, double[] a) {
        return new double[] {dot(m[0], a), dot(m[1], a), dot(m[2], a)};
    }
    // Rotacao que leva o vetor unitario e para +z (Rodrigues).
    static double[][] rotPara(double[] e) {
        double[] z = {0, 0, 1};
        double[] k = {e[1] * z[2] - e[2] * z[1], e[2] * z[0] - e[0] * z[2], e[0] * z[1] - e[1] * z[0]};
        double s = Math.sqrt(dot(k, k)), c = dot(e, z);
        if (s < 1e-9) return new double[][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        k = new double[] {k[0] / s, k[1] / s, k[2] / s};
        double[][] K = {{0, -k[2], k[1]}, {k[2], 0, -k[0]}, {-k[1], k[0], 0}};
        double[][] R = new double[3][3];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
            double kk = 0;
            for (int l = 0; l < 3; l++) kk += K[i][l] * K[l][j];
            R[i][j] = (i == j ? 1 : 0) + s * K[i][j] + (1 - c) * kk;
        }
        return R;
    }

    // Caminho SVG simples (M/L/H/V/A/C e minusculas) para portar desenhos de linha.
    static Shape svg(String d) {
        Path2D p = new Path2D.Double();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[MmLlHhVvAaCcZz]|-?\\d*\\.?\\d+(?:e-?\\d+)?").matcher(d);
        List<String> tk = new ArrayList<String>();
        while (m.find()) tk.add(m.group());
        double x = 0, y = 0, sx = 0, sy = 0; char cmd = 'M'; int i = 0;
        while (i < tk.size()) {
            String s0 = tk.get(i);
            if (Character.isLetter(s0.charAt(0))) { cmd = s0.charAt(0); i++; if (cmd == 'z' || cmd == 'Z') { p.closePath(); x = sx; y = sy; continue; } }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M': { double nx = num(tk, i++), ny = num(tk, i++); if (rel) { nx += x; ny += y; } p.moveTo(nx, ny); x = sx = nx; y = sy = ny; cmd = rel ? 'l' : 'L'; break; }
                case 'L': { double nx = num(tk, i++), ny = num(tk, i++); if (rel) { nx += x; ny += y; } p.lineTo(nx, ny); x = nx; y = ny; break; }
                case 'H': { double nx = num(tk, i++); if (rel) nx += x; p.lineTo(nx, y); x = nx; break; }
                case 'V': { double ny = num(tk, i++); if (rel) ny += y; p.lineTo(x, ny); y = ny; break; }
                case 'C': { double[] v = new double[6]; for (int k = 0; k < 6; k++) v[k] = num(tk, i++) + (rel ? (k % 2 == 0 ? x : y) : 0);
                    p.curveTo(v[0], v[1], v[2], v[3], v[4], v[5]); x = v[4]; y = v[5]; break; }
                case 'A': { double rx = num(tk, i++), ry = num(tk, i++), rot = num(tk, i++), la = num(tk, i++), sw = num(tk, i++), nx = num(tk, i++), ny = num(tk, i++);
                    if (rel) { nx += x; ny += y; }
                    arco(p, x, y, rx, ry, rot, la != 0, sw != 0, nx, ny); x = nx; y = ny; break; }
                default: i++;
            }
        }
        return p;
    }
    static double num(List<String> tk, int i) { return Double.parseDouble(tk.get(i)); }
    // Arco SVG (forma de ponto final) -> centro, como na especificacao SVG F.6.5.
    static void arco(Path2D p, double x1, double y1, double rx, double ry, double rotGraus, boolean grande, boolean horario, double x2, double y2) {
        double phi = Math.toRadians(rotGraus), c = Math.cos(phi), s = Math.sin(phi);
        double dx = (x1 - x2) / 2, dy = (y1 - y2) / 2;
        double x1p = c * dx + s * dy, y1p = -s * dx + c * dy;
        rx = Math.abs(rx); ry = Math.abs(ry);
        double lam = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry);
        if (lam > 1) { rx *= Math.sqrt(lam); ry *= Math.sqrt(lam); }
        double num = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double k = Math.sqrt(Math.max(0, num / (rx * rx * y1p * y1p + ry * ry * x1p * x1p))) * (grande == horario ? -1 : 1);
        double cxp = k * rx * y1p / ry, cyp = -k * ry * x1p / rx;
        double cx = c * cxp - s * cyp + (x1 + x2) / 2, cy = s * cxp + c * cyp + (y1 + y2) / 2;
        double t1 = Math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx), t2 = Math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);
        double dt = t2 - t1;
        if (horario && dt < 0) dt += 2 * Math.PI;
        if (!horario && dt > 0) dt -= 2 * Math.PI;
        int n = 16;
        for (int j = 1; j <= n; j++) {
            double t = t1 + dt * j / n;
            double ex = rx * Math.cos(t), ey = ry * Math.sin(t);
            p.lineTo(c * ex - s * ey + cx, s * ex + c * ey + cy);
        }
    }

    // Aperto de maos (desenho de linha portado do Lucide, licenca ISC).
    static Ic aperto(Ic ic) {
        ic.add(td(svg("m11 17 2 2a1 1 0 1 0 3-3")));
        ic.add(td(svg("m14 14 2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4")));
        ic.add(t(svg("m21 3 1 11h-2")));
        ic.add(t(svg("M3 3 2 14l6.5 6.5a1 1 0 1 0 3-3")));
        return ic.add(t(svg("M3 4h8")));
    }

    static Shape chuteira(AffineTransform at) {
        Path2D p = new Path2D.Double();
        p.moveTo(3, 7); p.lineTo(9, 7);
        p.curveTo(9.5, 10, 11, 11, 14, 11.5);
        p.curveTo(18, 12, 21.5, 13, 21.5, 15.5);
        p.curveTo(21.5, 16.5, 21, 17, 20, 17);
        p.lineTo(3, 17); p.closePath();
        return at.createTransformedShape(p);
    }
    static Ic bota(Ic ic, AffineTransform at) {
        ic.add(t(chuteira(at)));
        for (double x : new double[] {5.5, 10, 15, 19}) ic.add(t(at.createTransformedShape(L(x, 17, x, 19))));
        ic.add(td(at.createTransformedShape(L(10, 9.5, 11.5, 8))));
        return ic;
    }

    static Ic trofeu(Ic ic, String n) {
        Path2D copa = new Path2D.Double();
        copa.moveTo(6, 3.5); copa.lineTo(18, 3.5);
        copa.curveTo(18, 10, 15, 13.5, 12, 13.5);
        copa.curveTo(9, 13.5, 6, 10, 6, 3.5);
        copa.closePath();
        ic.add(t(copa));
        ic.add(t(new CubicCurve2D.Double(6.2, 5.5, 2, 5.5, 2.5, 11, 7.6, 11)));
        ic.add(t(new CubicCurve2D.Double(17.8, 5.5, 22, 5.5, 21.5, 11, 16.4, 11)));
        ic.add(t(L(12, 13.5, 12, 17)));
        ic.add(t(RR(7.5, 17, 9, 3.5, 1.5)));
        return ic.add(cd(texto(n, 12, 8.2, 5.5)));
    }

    static Shape cartao(double cx, double cy, double graus) {
        AffineTransform r = AffineTransform.getRotateInstance(Math.toRadians(graus), cx, cy);
        return r.createTransformedShape(RR(cx - 5, cy - 7, 10, 14, 2.5));
    }

    static Shape tag() { return Lz(3, 12, 11, 4, 20, 4, 20, 13, 12, 21); }

    static Ic seta(Ic ic, double x1, double y1, double x2, double y2) {
        double a = Math.atan2(y2 - y1, x2 - x1), k = 6;
        ic.add(td(L(x1, y1, x2, y2)));
        return ic.add(td(L(x2 - k * Math.cos(a - 0.8), y2 - k * Math.sin(a - 0.8), x2, y2, x2 - k * Math.cos(a + 0.8), y2 - k * Math.sin(a + 0.8))));
    }

    static Ic cruz(Ic ic) {
        ic.add(td(RR(3, 3, 18, 18, 5)));
        return ic.add(cd(Lz(10, 6.5, 14, 6.5, 14, 10, 17.5, 10, 17.5, 14, 14, 14, 14, 17.5, 10, 17.5, 10, 14, 6.5, 14, 6.5, 10, 10, 10)));
    }

    static List<Ic> lista2() {
        List<Ic> l = new ArrayList<Ic>();
        // painel do jogador
        Ic vender = new Ic("sell1", "Vender", NEON).em("aicons").add(t(tag())).add(c(O(16, 8, 1.4))).add(cd(texto("$", 11.5, 13.5, 7.5)));
        l.add(vender);
        Ic venderD = new Ic("sell1d", "Vender (off)", NEON).em("aicons").add(t(tag())).add(c(O(16, 8, 1.4))).add(cd(texto("$", 11.5, 13.5, 7.5))).apagado();
        l.add(venderD);
        l.add(new Ic("sell2", "Retirar da venda", VERM).em("aicons").add(t(tag())).add(c(O(16, 8, 1.4))).add(td(L(3.5, 20.5, 20.5, 3.5))));
        l.add(new Ic("sell2d", "Retirar (off)", VERM).em("aicons").add(t(tag())).add(c(O(16, 8, 1.4))).add(td(L(3.5, 20.5, 20.5, 3.5))).apagado());
        l.add(aperto(new Ic("contract", "Contrato", NEON).em("aicons")));
        l.add(aperto(new Ic("contractd", "Contrato (off)", NEON).em("aicons")).apagado());
        Ic apos = new Ic("retiredisabled", "Aposentar (off)", OURO).em("aicons");
        for (Ic x : lista()) if (x.arq.equals("retire")) apos.g.addAll(x.g);
        l.add(apos.apagado());
        l.add(new Ic("icon22", "Estatísticas", NEON).em("aicons").add(t(L(3, 20.5, 21, 20.5))).add(t(RR(5, 12, 3.5, 8.5, 1))).add(td(RR(10.25, 5, 3.5, 15.5, 1))).add(t(RR(15.5, 9, 3.5, 11.5, 1))));
        l.add(new Ic("icon22small", "Estatísticas P", NEON).em("aicons").add(t(L(3, 20.5, 21, 20.5))).add(t(RR(5, 12, 3.5, 8.5, 1))).add(td(RR(10.25, 5, 3.5, 15.5, 1))).add(t(RR(15.5, 9, 3.5, 11.5, 1))));
        l.add(cruz(new Ic("icontusao", "Lesão", VERM).em("aicons")));
        l.add(new Ic("jd", "Dispensar jogador", VERM).em("aicons").add(t(pessoa(9, 3, 1))).add(td(L(15, 14, 21, 20))).add(td(L(21, 14, 15, 20))));
        l.add(seta(new Ic("iconemprestados", "Emprestados", NEON).em("aicons").add(t(pessoa(9, 3, 1))), 14, 16, 22, 16));
        l.add(new Ic("greencheck", "Confirmar", NEON).em("aicons").add(t(RR(3, 3, 18, 18, 5))).add(td(L(7.5, 12.5, 10.5, 15.5, 16.5, 8.5))));
        l.add(bola(new Ic("kicker", "Cobrador", NEON).em("aicons"), 13, 12, 8));
        l.add(tinta(new Ic("falsonove", "Falso 9", OURO).em("aicons"), OURO).add(t(O(12, 12, 9))).add(cd(texto("9", 12, 12, 11))));
        l.add(bola(new Ic("ic_bola_verde", "Bola (verde)", NEON).em("aicons"), 12, 12, 9));
        l.add(bola(tinta(new Ic("ic_bola_vermelha", "Bola (vermelha)", VERM).em("aicons"), VERM), 12, 12, 9));
        AffineTransform semNum = AffineTransform.getTranslateInstance(0, 0);
        l.add(bota(new Ic("ic_chuteira1", "Chuteira", NEON).em("aicons"), semNum));
        for (int n = 2; n <= 7; n++) {
            AffineTransform at = new AffineTransform();
            at.translate(7.5, 2); at.scale(0.7, 0.85);
            l.add(bota(new Ic("ic_chuteira" + n, "Chuteira " + n, NEON).em("aicons"), at).add(cd(texto("" + n, 4, 12, 11))));
        }
        // partida
        l.add(new Ic("eca", "Cartão amarelo", AMARELO).em("aeicons").vidro().add(cd(cartao(12, 12, 8))));
        l.add(new Ic("ecv", "Cartão vermelho", VERM).em("aeicons").vidro().add(cd(cartao(12, 12, 8))));
        l.add(new Ic("icv", "Vermelho (lista)", VERM).em("aicons").vidro().add(cd(cartao(12, 12, 8))));
        l.add(tinta(new Ic("ecacv", "Segundo amarelo", AMARELO).em("aeicons").vidro(), AMARELO).add(c(cartao(9, 11.5, -12))).add(new F(cartao(15, 12.5, 12), true, true)));
        l.get(l.size() - 1).acento = VERM;
        l.add(bola(new Ic("egol", "Gol", NEON).em("aeicons"), 12, 12, 9.5));
        l.add(bola(tinta(new Ic("egolc", "Gol contra", VERM).em("aeicons"), VERM), 12, 12, 9.5));
        l.add(new Ic("esubs", "Substituição", VERM).em("aeicons").add(t(L(4, 8.5, 19, 8.5))).add(t(L(15, 4.5, 19, 8.5, 15, 12.5)))
            .add(td(L(20, 15.5, 5, 15.5))).add(td(L(9, 11.5, 5, 15.5, 9, 19.5))));
        l.add(cruz(new Ic("ect", "Lesão na partida", VERM).em("aeicons")));
        l.add(seta(new Ic("arrowlfet", "Saiu", VERM).em("aeicons"), 20, 12, 4, 12));
        l.add(seta(new Ic("arrowright", "Entrou", NEON).em("aeicons"), 4, 12, 20, 12));
        l.add(seta(new Ic("r_desce", "Rebaixado", VERM).em("aeicons"), 12, 3.5, 12, 20.5));
        l.add(seta(new Ic("r_sobe", "Promovido", NEON).em("aeicons"), 12, 20.5, 12, 3.5));
        l.add(new Ic("r_eli", "Eliminado", VERM).em("aeicons").add(td(L(5.5, 5.5, 18.5, 18.5))).add(td(L(18.5, 5.5, 5.5, 18.5))));
        l.add(tinta(new Ic("r_nulo", "Sem resultado", CINZA).em("aeicons"), CINZA).add(t(O(12, 12, 8.5))).add(t(L(6, 18, 18, 6))));
        l.add(trofeu(tinta(new Ic("r_ch", "Campeão", OURO).em("aeicons"), OURO), "1"));
        l.add(trofeu(tinta(new Ic("r_vice", "Vice", PRATA).em("aeicons"), PRATA), "2"));
        l.add(trofeu(tinta(new Ic("ic_champion", "Campeão G", OURO).em("aeicons"), OURO), "1"));
        l.add(trofeu(tinta(new Ic("ic_vice", "Vice G", PRATA).em("aeicons"), PRATA), "2"));
        l.add(bota(tinta(new Ic("ic_art", "Artilheiro", OURO).em("aeicons").vidro(), OURO), semNum));
        return l;
    }

    public static void main(String[] a) throws Exception {
        gera(a, lista2(), "Painel do jogador e partida");
    }
}
