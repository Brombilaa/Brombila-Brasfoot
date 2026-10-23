package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;
import javax.swing.ImageIcon;

// Partida ao vivo em 3D: camera isometrica acompanhando a bola, com os bonecos de
// mod.Bonecos, estadio (gramado, linhas, gols, placas e torcida) e, depois de cada gol,
// o replay do lance na camera de TV, em camera lenta. Mesma animacao de mod.Partida
// (posicoes, bola, lances); aqui so muda o desenho.
final class Transmissao {

    static boolean ligado = true;          // V alterna entre 3D e a prancheta (preferencia da sessao)

    static final double CX = 105, CZ = 68;               // campo em metros
    static final double AZ_ISO = Math.toRadians(-38), EL_ISO = Math.toRadians(36);
    static final double EL_TV = 0.2, DIST_TV = 46;
    static final long JANELA_ANTES = 3600, JANELA_DEPOIS = 1100;   // trecho do replay em volta do gol
    static final double LENTO = 0.62;
    static final long REPLAY_MS = (long) ((JANELA_ANTES + JANELA_DEPOIS) / LENTO);

    static final Color GRAMA_A = new Color(0x22702F), GRAMA_B = new Color(0x1D6229), GRAMA_FORA = new Color(0x184F23);
    static final Color FUNDO = new Color(0x0B0D0C), LINHA = new Color(240, 242, 236, 225);

    final Partida p;
    Bonecos iso, tv;
    volatile boolean carregou;
    BufferedImage tmp, meia;
    final Bonecos.Kit[] kits = new Bonecos.Kit[2], goleiros = new Bonecos.Kit[2];
    final Map<Object, Bonecos.Corpo> corpos = new IdentityHashMap<Object, Bonecos.Corpo>();
    BufferedImage torcida;
    final BufferedImage[] placas = new BufferedImage[4];
    double alvoX = CX / 2, alvoZ = CZ / 2;
    long ultimo;

    // replay
    final ArrayDeque<Foto> fotos = new ArrayDeque<Foto>();
    ArrayList<Foto> replay;
    long replayDe, replayAte, golEm;
    int replayLado;
    double rAlvoX, rAlvoZ;
    boolean replayPronto;

    static final class Foto {
        long t;
        Partida.Ator[] a;
        double[] x, y, vx, vy, passo;
        double bx, by, bh;
        boolean bv;
        Partida.Ator dono, receptor;
    }

    Transmissao(Partida p) {
        this.p = p;
        Thread t = new Thread(new Runnable() {
            public void run() {
                iso = Bonecos.carrega("iso");
                tv = Bonecos.carrega("tv");
                carregou = true;
            }
        }, "mod.bonecos");
        t.setDaemon(true);
        t.start();
    }

    // so o estadio, sem partida (cena dos penaltis): cores da torcida dos dois times
    Color[] coresTorcida;

    Transmissao(Color torcida0, Color torcida1) {
        this.p = null;
        coresTorcida = new Color[]{torcida0, torcida1};
    }

    boolean pronto() { return ligado && carregou && iso != null; }

    void novoJogo() {
        kits[0] = kits[1] = goleiros[0] = goleiros[1] = null;
        corpos.clear();
        torcida = null;
        fotos.clear();
        replay = null;
        replayAte = 0;
        alvoX = CX / 2; alvoZ = CZ / 2;
    }

    // ------------------------------------------------------------------ uniformes

    void kits() {
        if (kits[0] != null) return;
        for (int lado = 0; lado < 2; lado++) {
            Object time = lado == 0 ? p.casa : p.fora;
            Image img = null;
            Object ic = Partida.chama(time, "kS");
            if (ic instanceof ImageIcon) img = ((ImageIcon) ic).getImage();
            Color c1 = Partida.cor(time, "kB", lado == 0 ? Color.WHITE : Color.BLACK), c2 = Partida.cor(time, "kC", lado == 0 ? Color.BLACK : Color.WHITE);
            kits[lado] = Bonecos.Kit.de(img, c1, c2);
            if (lado == 1 && parecidos(kits[0], kits[1])) {
                // camisas parecidas: o visitante usa o outro uniforme (ou as cores que a prancheta usa)
                Bonecos.Kit melhor = null;
                for (int u = 1; u <= 3; u++) {
                    Object o = Partida.chamaInt(time, "bU", u);
                    if (!(o instanceof ImageIcon)) continue;
                    Bonecos.Kit k = Bonecos.Kit.de(((ImageIcon) o).getImage(), c1, c2);
                    if (melhor == null || Partida.distancia(kits[0].media, k.media) > Partida.distancia(kits[0].media, melhor.media)) melhor = k;
                }
                if (melhor == null || parecidos(kits[0], melhor)) melhor = Bonecos.Kit.de(null, p.cor[1], p.aro[1]);
                kits[1] = melhor;
            }
        }
        goleiros[0] = Bonecos.Kit.de(null, new Color(0x2B2E2C), new Color(0xF2C14E));
        goleiros[1] = Bonecos.Kit.de(null, new Color(0xD8B35A), new Color(0x1A1408));
        torcida = null;
    }

    // de longe so se ve a cor media da camisa: perto na cor ou no claro/escuro confunde
    static boolean parecidos(Bonecos.Kit a, Bonecos.Kit b) {
        return Partida.distancia(a.media, b.media) < 110 && Math.abs(Partida.brilho(a.media) - Partida.brilho(b.media)) < 0.3;
    }

    Bonecos.Corpo corpo(Partida.Ator a) {
        Bonecos.Corpo c = corpos.get(a.f);
        if (c == null) { c = Bonecos.Corpo.de(Partida.nome(a.f)); corpos.put(a.f, c); }
        return c;
    }

    // ------------------------------------------------------------------ camera

    static final class Camera {
        final boolean orto;
        final double escala, foco, cx, cy, el;
        final double px, py, pz, fx, fy, fz, rx, rz, ux, uy, uz, hx, hz;

        Camera(double ax, double az, double azimute, double el, double dist, boolean orto, double escala, double foco, double cx, double cy) {
            this.orto = orto; this.escala = escala; this.foco = foco; this.cx = cx; this.cy = cy; this.el = el;
            hx = Math.sin(azimute); hz = -Math.cos(azimute);
            px = ax + dist * Math.cos(el) * hx; py = dist * Math.sin(el); pz = az + dist * Math.cos(el) * hz;
            double a = ax - px, b = -py, c = az - pz, n = Math.sqrt(a * a + b * b + c * c);
            fx = a / n; fy = b / n; fz = c / n;
            double r1 = fz, r3 = -fx, rn = Math.hypot(r1, r3);
            rx = r1 / rn; rz = r3 / rn;
            ux = fy * rz; uy = fz * rx - fx * rz; uz = -fy * rx;
        }

        double fundo(double x, double y, double z) { return (x - px) * fx + (y - py) * fy + (z - pz) * fz; }

        void projeta(double x, double y, double z, double[] o) {
            double dx = x - px, dy = y - py, dz = z - pz;
            double d = dx * fx + dy * fy + dz * fz;
            double k = orto ? escala : foco / Math.max(0.05, d);
            o[0] = cx + k * (dx * rx + dz * rz);
            o[1] = cy - k * (dx * ux + dy * uy + dz * uz);
            o[2] = d;
        }

        double pxPorM(double fundo) { return orto ? escala : foco / Math.max(0.05, fundo); }

        // angulo do boneco (0 = de frente para a camera) para quem olha na direcao (vx, vz)
        double giro(double vx, double vz) { return Math.atan2(vx * rx + vz * rz, vx * hx + vz * hz); }

        boolean veFrente(double nx, double nz) { return -(nx * fx + nz * fz) > 0.05; }
    }

    Camera iso(Rectangle2D.Double area) {
        double esc = Math.max(16, Math.min(34, area.width / 30.0));
        return new Camera(alvoX, alvoZ, AZ_ISO, EL_ISO, 300, true, esc, 0, area.getCenterX(), area.getCenterY() + area.height * 0.04);
    }

    // ------------------------------------------------------------------ fotos (para o replay)

    Foto foto(long agora) {
        Foto f = new Foto();
        f.t = agora;
        int n = p.atores.size();
        f.a = new Partida.Ator[n];
        f.x = new double[n]; f.y = new double[n]; f.vx = new double[n]; f.vy = new double[n]; f.passo = new double[n];
        int i = 0;
        for (Partida.Ator a : p.atores.values()) {
            f.a[i] = a; f.x[i] = a.x; f.y[i] = a.y; f.vx[i] = a.vx; f.vy[i] = a.vy; f.passo[i] = a.passo;
            i++;
        }
        f.bx = p.bolaX; f.by = p.bolaY; f.bh = p.bolaH; f.bv = p.bolaVisivel;
        f.dono = p.dono; f.receptor = p.receptor;
        return f;
    }

    void grava(long agora) {
        if (!carregou || tv == null) return;
        fotos.addLast(foto(agora));
        while (!fotos.isEmpty() && fotos.peekFirst().t < agora - 9000) fotos.removeFirst();
    }

    // chamado no gol: o replay comeca quando a comemoracao acaba
    void agendaReplay(long golDe, long golAte, int lado) {
        if (!pronto() || tv == null) return;
        golEm = golDe;
        replayDe = golAte;
        replayAte = golAte + REPLAY_MS;
        replayLado = lado;
        replay = null;
        replayPronto = false;
    }

    boolean emReplay(long agora) { return replayAte > 0 && agora >= replayDe && agora < replayAte; }

    long pula(long agora) {
        if (!emReplay(agora)) return 0;
        long resta = replayAte - agora;
        replayAte = agora;
        return resta;
    }

    Foto naHora(double t) {
        ArrayList<Foto> l = replay;
        if (l == null || l.isEmpty()) return null;
        if (t <= l.get(0).t) return l.get(0);
        for (int i = 1; i < l.size(); i++) {
            Foto b = l.get(i);
            if (b.t < t) continue;
            Foto a = l.get(i - 1);
            double s = (t - a.t) / Math.max(1, b.t - a.t);
            Foto f = new Foto();
            f.t = (long) t;
            f.a = b.a;
            int n = b.a.length;
            f.x = new double[n]; f.y = new double[n]; f.vx = new double[n]; f.vy = new double[n]; f.passo = new double[n];
            for (int k = 0; k < n; k++) {
                int j = indice(a.a, b.a[k], k);
                double ax = j < 0 ? b.x[k] : a.x[j], ay = j < 0 ? b.y[k] : a.y[j], ap = j < 0 ? b.passo[k] : a.passo[j];
                f.x[k] = ax + (b.x[k] - ax) * s; f.y[k] = ay + (b.y[k] - ay) * s;
                f.vx[k] = b.vx[k]; f.vy[k] = b.vy[k];
                f.passo[k] = ap + (b.passo[k] - ap) * s;
            }
            boolean pulo = Math.hypot(b.bx - a.bx, b.by - a.by) > 0.2;
            f.bx = pulo ? b.bx : a.bx + (b.bx - a.bx) * s; f.by = pulo ? b.by : a.by + (b.by - a.by) * s; f.bh = a.bh + (b.bh - a.bh) * s;
            f.bv = b.bv; f.dono = b.dono; f.receptor = b.receptor;
            return f;
        }
        return l.get(l.size() - 1);
    }

    static int indice(Partida.Ator[] l, Partida.Ator a, int dica) {
        if (dica < l.length && l[dica] == a) return dica;
        for (int i = 0; i < l.length; i++) if (l[i] == a) return i;
        return -1;
    }

    // ------------------------------------------------------------------ pintura

    static volatile long ms;          // tempo do ultimo quadro (para os testes)

    void pinta(Graphics2D g, Rectangle2D.Double area, long agora) {
        long t0 = System.nanoTime();
        try { pinta0(g, area, agora); } finally { ms = (System.nanoTime() - t0) / 1000000; }
    }

    void pinta0(Graphics2D g, Rectangle2D.Double area, long agora) {
        kits();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double dt = ultimo == 0 ? 0.03 : Math.min(0.2, (agora - ultimo) / 1000.0);
        ultimo = agora;
        if (emReplay(agora)) {
            pintaReplay(g, area, agora, dt);
            return;
        }
        // camera segue a bola com um pouco de atraso e sem sair do estadio
        double bx = p.bolaX * CX, bz = (1 - p.bolaY) * CZ;
        double k = Math.min(1, dt * 2.6);
        alvoX += (Math.max(9, Math.min(CX - 9, bx)) - alvoX) * k;
        alvoZ += (Math.max(9, Math.min(CZ - 7, bz)) - alvoZ) * k;
        Camera c = iso(area);
        cena(g, c, area, foto(agora), iso, agora, true);
        miniatura(g, area);
    }

    void pintaReplay(Graphics2D g, Rectangle2D.Double area, long agora, double dt) {
        if (!replayPronto) {
            replay = new ArrayList<Foto>();
            for (Foto f : fotos) if (f.t >= golEm - JANELA_ANTES && f.t <= golEm + JANELA_DEPOIS) replay.add(f);
            replayPronto = true;
            Foto f0 = replay.isEmpty() ? null : replay.get(0);
            rAlvoX = f0 == null ? CX / 2 : f0.bx * CX;
            rAlvoZ = f0 == null ? CZ / 2 : (1 - f0.by) * CZ;
        }
        double t = golEm - JANELA_ANTES + (agora - replayDe) * LENTO;
        Foto f = naHora(t);
        if (f == null) return;
        double bx = f.bx * CX, bz = (1 - f.by) * CZ;
        double gx = replayLado == 0 ? CX : 0;
        double k = Math.min(1, dt * 3.2);
        rAlvoX += (Math.max(4, Math.min(CX - 4, bx + (gx - bx) * 0.12)) - rAlvoX) * k;
        rAlvoZ += (Math.max(10, Math.min(CZ - 6, bz)) - rAlvoZ) * k;
        double az = replayLado == 0 ? -0.36 : 0.36;
        Camera c = new Camera(rAlvoX, rAlvoZ, az, EL_TV, DIST_TV, false, 0, area.width * 1.9, area.getCenterX(), area.getCenterY() + area.height * 0.06);
        cena(g, c, area, f, tv, agora, false);
        // tarja de replay: faixas pretas, selo dourado e barra de progresso
        double u = (agora - replayDe) / (double) Math.max(1, replayAte - replayDe);
        double faixa = area.height * 0.075;
        g.setColor(new Color(0, 0, 0, 200));
        g.fill(new Rectangle2D.Double(area.x, area.y, area.width, faixa));
        g.fill(new Rectangle2D.Double(area.x, area.y + area.height - faixa, area.width, faixa));
        g.setFont(Estreia.titulo((float) Math.max(16, faixa * 0.62)));
        FontMetrics fm = g.getFontMetrics();
        String s = "REPLAY";
        double sx = area.x + 20, sy = area.y + faixa + 14;
        double w = fm.stringWidth(s) + 30, h = fm.getHeight() + 2;
        g.setPaint(new GradientPaint(0, (float) sy, new Color(0xF6CE6A), 0, (float) (sy + h), new Color(0xD9A53B)));
        g.fill(new RoundRectangle2D.Double(sx, sy, w, h, h, h));
        g.setColor(new Color(0x1A1408));
        g.drawString(s, (float) (sx + 15), (float) (sy + fm.getAscent()));
        g.setFont(Estreia.texto(12f));
        fm = g.getFontMetrics();
        g.setColor(new Color(241, 238, 231, 200));
        g.drawString("câmera lenta · clique para pular", (float) (sx + w + 12), (float) (sy + h / 2 + fm.getAscent() / 2.0 - 2));
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(new Rectangle2D.Double(area.x, area.y + area.height - faixa - 3, area.width, 3));
        g.setColor(Tema.DOURADO);
        g.fill(new Rectangle2D.Double(area.x, area.y + area.height - faixa - 3, area.width * Math.min(1, u), 3));
        // entrada: clarao dourado rapido
        long desde = agora - replayDe;
        if (desde < 260) {
            g.setColor(new Color(242, 193, 78, (int) (170 * (1 - desde / 260.0))));
            g.fill(area);
        }
    }

    // um objeto da cena ordenado pela distancia ate a camera
    static final class Coisa {
        double fundo;
        int i = -1;          // jogador da foto; -1 bola; -2/-3 gols
    }

    void cena(Graphics2D g, Camera c, Rectangle2D.Double area, Foto f, Bonecos bon, long agora, boolean aoVivo) {
        double[] o = new double[3];
        g.setColor(FUNDO);
        g.fill(area);
        estadio(g, c, area);
        // gramado: entorno, faixas cortadas e as linhas
        g.setColor(GRAMA_FORA);
        g.fill(chao(c, -16, -14, CX + 16, CZ + 14));
        for (int i = 0; i < 20; i++) {
            g.setColor(i % 2 == 0 ? GRAMA_A : GRAMA_B);
            g.fill(chao(c, CX * i / 20.0, -3, CX * (i + 1) / 20.0 + 0.02, CZ + 3));
        }
        // luz dos refletores sobre o gramado
        g.setPaint(new RadialGradientPaint((float) area.getCenterX(), (float) (area.y + area.height * 0.42), (float) (area.width * 0.75),
            new float[]{0f, 0.55f, 1f}, new Color[]{new Color(255, 250, 220, 26), new Color(0, 0, 0, 0), new Color(0, 0, 0, 110)}));
        g.fill(area);
        linhas(g, c);
        placas(g, c);
        // sombras e o aro de quem esta com a bola
        for (int i = 0; i < f.a.length; i++) {
            double x = f.x[i] * CX, z = (1 - f.y[i]) * CZ;
            g.setColor(new Color(0, 0, 0, 80));
            g.fill(elipse(c, x + 0.16, z + 0.12, 0.46, 0.36));
            if (f.a[i] == f.dono || f.a[i] == f.receptor) {
                g.setColor(new Color(242, 193, 78, 200));
                g.setStroke(new BasicStroke((float) Math.max(1.4, c.pxPorM(c.fundo(x, 0, z)) * 0.07)));
                g.draw(elipse(c, x, z, 0.62, 0.62));
            }
        }
        double bx = f.bx * CX, bz = (1 - f.by) * CZ, by = 0.11 + f.bh * 5.0;
        if (f.bv) {
            g.setColor(new Color(0, 0, 0, 95));
            g.fill(elipse(c, bx + 0.05, bz + 0.05, 0.2, 0.2));
        }
        // jogadores, bola e gols, do mais longe para o mais perto
        ArrayList<Coisa> l = new ArrayList<Coisa>();
        for (int i = 0; i < f.a.length; i++) {
            Coisa k = new Coisa();
            k.i = i;
            k.fundo = c.fundo(f.x[i] * CX, 0.9, (1 - f.y[i]) * CZ);
            l.add(k);
        }
        Coisa bola = new Coisa();
        bola.fundo = c.fundo(bx, by, bz) - 0.3;
        if (f.bv) l.add(bola);
        for (int gi = 0; gi < 2; gi++) {
            Coisa k = new Coisa();
            k.i = -2 - gi;
            k.fundo = c.fundo(gi == 0 ? -1.0 : CX + 1.0, 1.2, CZ / 2);
            l.add(k);
        }
        Collections.sort(l, new Comparator<Coisa>() {
            public int compare(Coisa a, Coisa b) { return Double.compare(b.fundo, a.fundo); }
        });
        for (Coisa k : l) {
            if (k.i >= 0) boneco(g, c, f, k.i, bon, area);
            else if (k.i == -1) bola(g, c, bx, by, bz);
            else gol(g, c, k.i == -2 ? 0 : CX, k.i == -2 ? -1 : 1);
        }
        if (!aoVivo) return;
        // marcas (cartao, contusao, entrou) e nome de quem esta com a bola
        for (Partida.Marca m : p.marcas) {
            if (!p.atores.containsValue(m.ator)) continue;
            c.projeta(m.ator.x * CX, 2.35, (1 - m.ator.y) * CZ, o);
            double R = Math.max(7, c.pxPorM(o[2]) * 0.35);
            if (m.tipo >= 2 && m.tipo <= 4) {
                g.setColor(m.tipo == 2 ? new Color(0xF5D547) : new Color(0xE5463B));
                g.fill(new RoundRectangle2D.Double(o[0] - R * 0.4, o[1] - R * 1.1, R * 0.8, R * 1.1, 3, 3));
            } else if (m.tipo == 5) {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(o[0] - R * 0.55, o[1] - R * 1.1, R * 1.1, R * 1.1));
                g.setColor(new Color(0xE5463B));
                g.fill(new Rectangle2D.Double(o[0] - R * 0.11, o[1] - R * 0.95, R * 0.22, R * 0.8));
                g.fill(new Rectangle2D.Double(o[0] - R * 0.4, o[1] - R * 0.66, R * 0.8, R * 0.22));
            } else if (m.tipo == 6) {
                p.etiqueta(g, "entrou", o[0], o[1] - R * 0.5, 9, new Color(0x5FCF8E));
            }
        }
        // fora de posicao: "!" dourado sobre a cabeca
        for (Partida.Ator a : p.foraAgora.keySet()) {
            if (!p.atores.containsValue(a)) continue;
            c.projeta(a.x * CX, 2.35, (1 - a.y) * CZ, o);
            double R = Math.max(5, c.pxPorM(o[2]) * 0.2);
            Partida.exclamacao(g, o[0], o[1] - R * 1.4, R);
        }
        Partida.Ator mostra = p.dono != null ? p.dono : p.receptor;
        if (mostra != null && p.atores.containsValue(mostra)) {
            c.projeta(mostra.x * CX, 0, (1 - mostra.y) * CZ, o);
            p.etiqueta(g, Partida.curto(Partida.nome(mostra.f)) + "  " + Notas.texto(p.nota(mostra)), o[0], o[1] + 14, 9.5, null);
        }
    }

    void boneco(Graphics2D g, Camera c, Foto f, int i, Bonecos bon, Rectangle2D.Double area) {
        Partida.Ator a = f.a[i];
        double x = f.x[i] * CX, z = (1 - f.y[i]) * CZ;
        double[] o = new double[3];
        c.projeta(x, 0, z, o);
        if (o[2] < 1 || o[0] < area.x - 80 || o[0] > area.getMaxX() + 80 || o[1] < area.y - 20 || o[1] > area.getMaxY() + 220) return;
        double vx = f.vx[i] * CX, vz = -f.vy[i] * CZ, v = Math.hypot(vx, vz);
        int pose = 0;
        if (v > 1.1) pose = 1 + ((int) Math.floor(f.passo[i]) & 7);
        else {
            vx = (f.bx - f.x[i]) * CX; vz = -(f.by - f.y[i]) * CZ;
            if (Math.hypot(vx, vz) < 0.01) { vx = a.lado == 0 ? 1 : -1; vz = 0; }
        }
        double giro = c.giro(vx, vz);
        int dir = (int) Math.round(giro / (2 * Math.PI / bon.dirs));
        Bonecos.Corpo corpo = corpo(a);
        Bonecos.Kit kit = a.vaga == 1 ? goleiros[a.lado] : kits[a.lado];
        int q = bon.quadro(corpo.corte, dir, pose);
        if (bon.w[q] <= 0) return;
        tmp = bon.pinta(q, kit, corpo, a.numero, tmp);
        double s = c.pxPorM(o[2]) / bon.escala;
        double dx = o[0] + (bon.x0[q] - bon.largura / 2.0) * s, dy = o[1] + (bon.y0[q] - bon.peY) * s;
        double dw = bon.w[q] * s, dh = bon.h[q] * s;
        Image fonte = tmp;
        int sw = bon.w[q], sh = bon.h[q];
        if (s < 0.55) {
            // reduz em dois passos para nao serrilhar
            int hw = Math.max(1, sw / 2), hh = Math.max(1, sh / 2);
            if (meia == null || meia.getWidth() < hw || meia.getHeight() < hh)
                meia = new BufferedImage(Math.max(hw, 64), Math.max(hh, 96), BufferedImage.TYPE_INT_ARGB);
            Graphics2D m = meia.createGraphics();
            m.setComposite(AlphaComposite.Clear);
            m.fillRect(0, 0, meia.getWidth(), meia.getHeight());
            m.setComposite(AlphaComposite.SrcOver);
            m.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            m.drawImage(tmp, 0, 0, hw, hh, 0, 0, sw, sh, null);
            m.dispose();
            fonte = meia; sw = hw; sh = hh;
        }
        g.drawImage(fonte, (int) Math.round(dx), (int) Math.round(dy), (int) Math.round(dx + dw), (int) Math.round(dy + dh), 0, 0, sw, sh, null);
    }

    void bola(Graphics2D g, Camera c, double x, double y, double z) {
        double[] o = new double[3];
        c.projeta(x, y, z, o);
        double r = Math.max(2.6, c.pxPorM(o[2]) * 0.17);
        Ellipse2D.Double b = new Ellipse2D.Double(o[0] - r, o[1] - r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint((float) (o[0] - r * 0.35), (float) (o[1] - r * 0.4), (float) (r * 1.7),
            new float[]{0f, 1f}, new Color[]{Color.WHITE, new Color(0xB9B6AE)}));
        g.fill(b);
        g.setColor(new Color(0x2A2A2A));
        g.setStroke(new BasicStroke((float) Math.max(0.8, r * 0.18)));
        g.draw(b);
    }

    // ------------------------------------------------------------------ estadio

    // retangulo no chao (y = 0), cortado no plano da camera
    Shape chao(Camera c, double x0, double z0, double x1, double z1) {
        return poligono(c, new double[][]{{x0, 0, z0}, {x1, 0, z0}, {x1, 0, z1}, {x0, 0, z1}});
    }

    Path2D poligono(Camera c, double[][] pts) {
        ArrayList<double[]> l = new ArrayList<double[]>();
        int n = pts.length;
        for (int i = 0; i < n; i++) {
            double[] a = pts[i], b = pts[(i + 1) % n];
            double da = c.fundo(a[0], a[1], a[2]) - 1, db = c.fundo(b[0], b[1], b[2]) - 1;
            if (da >= 0) l.add(a);
            if ((da >= 0) != (db >= 0)) {
                double s = da / (da - db);
                l.add(new double[]{a[0] + (b[0] - a[0]) * s, a[1] + (b[1] - a[1]) * s, a[2] + (b[2] - a[2]) * s});
            }
        }
        Path2D.Double path = new Path2D.Double();
        double[] o = new double[3];
        for (int i = 0; i < l.size(); i++) {
            double[] q = l.get(i);
            c.projeta(q[0], q[1], q[2], o);
            if (i == 0) path.moveTo(o[0], o[1]); else path.lineTo(o[0], o[1]);
        }
        if (!l.isEmpty()) path.closePath();
        return path;
    }

    Path2D elipse(Camera c, double x, double z, double rx, double rz) {
        double[][] pts = new double[24][];
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            pts[i] = new double[]{x + Math.cos(a) * rx, 0.01, z + Math.sin(a) * rz};
        }
        return poligono(c, pts);
    }

    void linha(Graphics2D g, Camera c, double[][] pts, boolean fecha) {
        double[] a = new double[3], b = new double[3];
        int n = fecha ? pts.length : pts.length - 1;
        Path2D.Double path = new Path2D.Double();
        boolean aberto = false;
        for (int i = 0; i < n; i++) {
            double[] p0 = pts[i], p1 = pts[(i + 1) % pts.length];
            double d0 = c.fundo(p0[0], 0, p0[1]) - 1, d1 = c.fundo(p1[0], 0, p1[1]) - 1;
            if (d0 < 0 && d1 < 0) { aberto = false; continue; }
            double[] q0 = p0, q1 = p1;
            if (d0 < 0) { double s = d0 / (d0 - d1); q0 = new double[]{p0[0] + (p1[0] - p0[0]) * s, p0[1] + (p1[1] - p0[1]) * s}; aberto = false; }
            if (d1 < 0) { double s = d0 / (d0 - d1); q1 = new double[]{p0[0] + (p1[0] - p0[0]) * s, p0[1] + (p1[1] - p0[1]) * s}; }
            c.projeta(q0[0], 0.02, q0[1], a);
            c.projeta(q1[0], 0.02, q1[1], b);
            if (!aberto) path.moveTo(a[0], a[1]);
            path.lineTo(b[0], b[1]);
            aberto = d1 >= 0;
        }
        g.draw(path);
    }

    static double[][] arco(double cx, double cz, double r, double a0, double a1, int n) {
        double[][] p = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            p[i] = new double[]{cx + Math.cos(a) * r, cz + Math.sin(a) * r};
        }
        return p;
    }

    void linhas(Graphics2D g, Camera c) {
        double pm = c.pxPorM(c.fundo(alvoReal(c)[0], 0, alvoReal(c)[1]));
        g.setStroke(new BasicStroke((float) Math.max(1.2, pm * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(LINHA);
        linha(g, c, new double[][]{{0, 0}, {CX, 0}, {CX, CZ}, {0, CZ}}, true);
        linha(g, c, new double[][]{{CX / 2, 0}, {CX / 2, CZ}}, false);
        linha(g, c, arco(CX / 2, CZ / 2, 9.15, 0, Math.PI * 2, 56), true);
        for (int lado = 0; lado < 2; lado++) {
            double x0 = lado == 0 ? 0 : CX, s = lado == 0 ? 1 : -1;
            linha(g, c, new double[][]{{x0, CZ / 2 - 20.16}, {x0 + s * 16.5, CZ / 2 - 20.16}, {x0 + s * 16.5, CZ / 2 + 20.16}, {x0, CZ / 2 + 20.16}}, false);
            linha(g, c, new double[][]{{x0, CZ / 2 - 9.16}, {x0 + s * 5.5, CZ / 2 - 9.16}, {x0 + s * 5.5, CZ / 2 + 9.16}, {x0, CZ / 2 + 9.16}}, false);
            double ang = Math.acos(5.5 / 9.15);
            double pxm = x0 + s * 11;
            linha(g, c, s > 0 ? arco(pxm, CZ / 2, 9.15, -ang, ang, 20) : arco(pxm, CZ / 2, 9.15, Math.PI - ang, Math.PI + ang, 20), false);
            g.fill(elipse(c, pxm, CZ / 2, 0.14, 0.14));
            for (int k = 0; k < 2; k++) {
                double cz = k == 0 ? 0 : CZ;
                double a0 = lado == 0 ? (k == 0 ? 0 : -Math.PI / 2) : (k == 0 ? Math.PI / 2 : Math.PI);
                linha(g, c, arco(x0, cz, 1, a0, a0 + Math.PI / 2, 8), false);
            }
        }
        g.fill(elipse(c, CX / 2, CZ / 2, 0.14, 0.14));
    }

    double[] alvoReal(Camera c) {
        // ponto do chao no centro da tela
        double t = -c.py / Math.min(-1e-6, c.fy);
        return new double[]{c.px + c.fx * t, c.pz + c.fz * t};
    }

    void gol(Graphics2D g, Camera c, double x0, double s) {
        double z0 = CZ / 2 - 3.66, z1 = CZ / 2 + 3.66, h = 2.44, f = x0 + s * 2.0;
        double pm = c.pxPorM(c.fundo(x0, 1.2, CZ / 2));
        g.setColor(new Color(235, 235, 230, 80));
        g.setStroke(new BasicStroke((float) Math.max(0.7, pm * 0.03)));
        double[] a = new double[3], b = new double[3];
        for (int i = 0; i <= 14; i++) {
            double z = z0 + (z1 - z0) * i / 14;
            seg(g, c, f, 0, z, f, h * 0.8, z, a, b);
            seg(g, c, f, h * 0.8, z, x0, h, z, a, b);
        }
        for (int i = 0; i <= 8; i++) {
            double y = h * 0.8 * i / 8;
            seg(g, c, f, y, z0, f, y, z1, a, b);
            seg(g, c, x0, y / 0.8, z0, f, y, z0, a, b);
            seg(g, c, x0, y / 0.8, z1, f, y, z1, a, b);
        }
        g.setColor(new Color(250, 250, 248));
        g.setStroke(new BasicStroke((float) Math.max(1.5, pm * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        seg(g, c, x0, 0, z0, x0, h, z0, a, b);
        seg(g, c, x0, 0, z1, x0, h, z1, a, b);
        seg(g, c, x0, h, z0, x0, h, z1, a, b);
    }

    void seg(Graphics2D g, Camera c, double x0, double y0, double z0, double x1, double y1, double z1, double[] a, double[] b) {
        if (c.fundo(x0, y0, z0) < 1 || c.fundo(x1, y1, z1) < 1) return;
        c.projeta(x0, y0, z0, a);
        c.projeta(x1, y1, z1, b);
        g.draw(new Line2D.Double(a[0], a[1], b[0], b[1]));
    }

    // imagem num quadrilatero do mundo (p00 canto da imagem, p10 fim da largura, p01 fim da altura),
    // em pedacos: cada um vira uma transformacao afim (exata na isometrica, quase exata na TV)
    void textura(Graphics2D g, Camera c, BufferedImage img, double[] p00, double[] p10, double[] p01, int partes) {
        int W = img.getWidth(), H = img.getHeight();
        int pv = c.orto ? 1 : Math.max(1, partes / 3);
        double[] A = new double[3], B = new double[3], C = new double[3];
        for (int j = 0; j < pv; j++) {
            double v0 = j / (double) pv, v1 = (j + 1) / (double) pv;
            for (int i = 0; i < partes; i++) {
                double u0 = i / (double) partes, u1 = (i + 1) / (double) partes;
                double[] a = ponto(p00, p10, p01, u0, v0), b = ponto(p00, p10, p01, u1, v0), cc = ponto(p00, p10, p01, u0, v1);
                if (c.fundo(a[0], a[1], a[2]) < 1 || c.fundo(b[0], b[1], b[2]) < 1 || c.fundo(cc[0], cc[1], cc[2]) < 1) continue;
                c.projeta(a[0], a[1], a[2], A); c.projeta(b[0], b[1], b[2], B); c.projeta(cc[0], cc[1], cc[2], C);
                int sx0 = (int) Math.floor(u0 * W), sx1 = Math.min(W, (int) Math.ceil(u1 * W) + 1);
                int sy0 = (int) Math.floor(v0 * H), sy1 = Math.min(H, (int) Math.ceil(v1 * H) + 1);
                double du = W * (u1 - u0), dv = H * (v1 - v0);
                AffineTransform t = new AffineTransform((B[0] - A[0]) / du, (B[1] - A[1]) / du, (C[0] - A[0]) / dv, (C[1] - A[1]) / dv, A[0], A[1]);
                if (Math.abs(t.getDeterminant()) < 1e-6) continue;
                AffineTransform antes = g.getTransform();
                g.transform(t);
                g.drawImage(img, 0, 0, sx1 - sx0, sy1 - sy0, sx0, sy0, sx1, sy1, null);
                g.setTransform(antes);
            }
        }
    }

    static double[] ponto(double[] p00, double[] p10, double[] p01, double u, double v) {
        return new double[]{p00[0] + (p10[0] - p00[0]) * u + (p01[0] - p00[0]) * v, p00[1] + (p10[1] - p00[1]) * u + (p01[1] - p00[1]) * v,
            p00[2] + (p10[2] - p00[2]) * u + (p01[2] - p00[2]) * v};
    }

    // placa em pe entre (ax, az) e (bx, bz): o texto sempre da esquerda para a direita na tela
    void placa(Graphics2D g, Camera c, BufferedImage img, double ax, double az, double bx, double bz, int partes) {
        double[] o1 = new double[3], o2 = new double[3];
        c.projeta(ax, 0, az, o1); c.projeta(bx, 0, bz, o2);
        if (o1[0] > o2[0]) { double t = ax; ax = bx; bx = t; t = az; az = bz; bz = t; }
        textura(g, c, img, new double[]{ax, 0.95, az}, new double[]{bx, 0.95, bz}, new double[]{ax, 0, az}, partes);
    }

    void estadio(Graphics2D g, Camera c, Rectangle2D.Double area) {
        if (torcida == null) torcida = torcida();
        int partes = c.orto ? 1 : 18;
        // arquibancadas atras do lado de la e dos fundos (so as que a camera ve de frente)
        if (c.veFrente(0, -1)) textura(g, c, torcida, new double[]{-24, 1.1, 76}, new double[]{CX + 24, 1.1, 76}, new double[]{-24, 17, 94}, partes);
        if (c.orto && c.veFrente(-1, 0)) textura(g, c, torcida, new double[]{CX + 9, 1.1, -14}, new double[]{CX + 9, 1.1, CZ + 14}, new double[]{CX + 27, 17, -14}, partes);
        if (c.orto && c.veFrente(1, 0)) textura(g, c, torcida, new double[]{-9, 1.1, CZ + 14}, new double[]{-9, 1.1, -14}, new double[]{-27, 17, CZ + 14}, partes);
        if (c.veFrente(0, 1)) textura(g, c, torcida, new double[]{CX + 24, 1.1, -8}, new double[]{-24, 1.1, -8}, new double[]{CX + 24, 17, -26}, partes);
        // muro entre a arquibancada e o gramado
        g.setColor(new Color(0x101412));
        if (c.veFrente(0, -1)) g.fill(poligono(c, new double[][]{{-24, 0, 76}, {CX + 24, 0, 76}, {CX + 24, 1.15, 76}, {-24, 1.15, 76}}));
        if (c.veFrente(-1, 0)) g.fill(poligono(c, new double[][]{{CX + 9, 0, -14}, {CX + 9, 0, CZ + 14}, {CX + 9, 1.15, CZ + 14}, {CX + 9, 1.15, -14}}));
        if (c.veFrente(1, 0)) g.fill(poligono(c, new double[][]{{-9, 0, -14}, {-9, 0, CZ + 14}, {-9, 1.15, CZ + 14}, {-9, 1.15, -14}}));
    }

    void placas(Graphics2D g, Camera c) {
        if (placas[0] == null) for (int i = 0; i < 4; i++) placas[i] = placa(i);
        int partes = c.orto ? 1 : 4;
        int k = 0;
        if (c.veFrente(0, -1)) for (double x = -4; x < CX + 4; x += 12, k++) placa(g, c, placas[k % 4], x, 72.5, x + 12, 72.5, partes);
        if (c.veFrente(-1, 0)) for (double z = CZ / 2 - 30; z < CZ / 2 + 30; z += 12, k++) placa(g, c, placas[k % 4], CX + 5.5, z, CX + 5.5, z + 12, partes);
        if (c.veFrente(1, 0)) for (double z = CZ / 2 + 30; z > CZ / 2 - 30; z -= 12, k++) placa(g, c, placas[k % 4], -5.5, z, -5.5, z - 12, partes);
    }

    BufferedImage placa(int i) {
        // sem o ano por enquanto (as artes das placas ainda vao ser refeitas)
        String[] t = {"BROMBILA BRASFOOT", "CAMPEONATO BRASILEIRO", "BROMBILA BRASFOOT", "CAMPEONATO BRASILEIRO"};
        BufferedImage im = new BufferedImage(420, 34, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x0E100F));
        g.fillRect(0, 0, 420, 34);
        g.setColor(new Color(242, 193, 78, 170));
        g.fillRect(0, 0, 420, 2);
        g.setColor(new Color(0x202422));
        g.fillRect(0, 0, 2, 34);
        g.setFont(Estreia.titulo(27f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xF2C14E));
        g.drawString(t[i], (420 - fm.stringWidth(t[i])) / 2f, 17 + fm.getAscent() * 0.36f);
        g.dispose();
        return im;
    }

    // degraus escuros com a torcida nas cores dos dois times
    BufferedImage torcida() {
        int W = 2400, H = 240;
        BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int degraus = 24;
        for (int k = 0; k < degraus; k++) {
            int y0 = k * H / degraus, y1 = (k + 1) * H / degraus;
            int cinza = 22 + (k % 2) * 6 - k / 3;
            g.setColor(new Color(cinza, cinza + 3, cinza + 2));
            g.fillRect(0, y0, W, y1 - y0);
        }
        Color[] cs = p != null ? p.cor : coresTorcida;
        Color c0 = Partida.visivel(cs[0]), c1 = Partida.visivel(cs[1]);
        Color[] neutras = {new Color(0xE6E6DC), new Color(0x2A2A30), new Color(0xF2F2F2), new Color(0x151515), new Color(0xC9B27A)};
        Random r = new Random(7);
        for (int k = 0; k < degraus; k++) {
            int y = k * H / degraus + 3;
            for (int x = 2; x < W; x += 4) {
                if (r.nextInt(10) < 2) continue;
                double u = x / (double) W;
                int s = r.nextInt(100);
                Color cor = s < 38 ? (u < 0.5 ? c0 : c1) : s < 55 ? (u < 0.5 ? c1 : c0) : neutras[r.nextInt(neutras.length)];
                int luz = 55 + r.nextInt(45);
                g.setColor(new Color(cor.getRed() * luz / 100, cor.getGreen() * luz / 100, cor.getBlue() * luz / 100));
                g.fillOval(x + r.nextInt(2), y + r.nextInt(2), 3, 5);
            }
        }
        g.dispose();
        return im;
    }

    // mapa pequeno no canto: o campo inteiro com todos os jogadores
    void miniatura(Graphics2D g, Rectangle2D.Double area) {
        double mw = Math.max(110, Math.min(170, area.width * 0.2)), mh = mw * 0.648;
        double x = area.getMaxX() - mw - 14, y = area.getMaxY() - mh - 14;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x - 6, y - 6, mw + 12, mh + 12, 12, 12);
        g.setColor(new Color(8, 10, 9, 175));
        g.fill(r);
        g.setColor(new Color(242, 193, 78, 90));
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setColor(new Color(255, 255, 255, 70));
        g.draw(new Rectangle2D.Double(x, y, mw, mh));
        g.draw(new Line2D.Double(x + mw / 2, y, x + mw / 2, y + mh));
        g.draw(new Ellipse2D.Double(x + mw / 2 - mh * 0.135, y + mh / 2 - mh * 0.135, mh * 0.27, mh * 0.27));
        g.draw(new Rectangle2D.Double(x, y + mh * 0.2, mw * 0.157, mh * 0.6));
        g.draw(new Rectangle2D.Double(x + mw * 0.843, y + mh * 0.2, mw * 0.157, mh * 0.6));
        for (Partida.Ator a : p.atores.values()) {
            Color cc = a.vaga == 1 ? (a.lado == 0 ? new Color(0x2B2E2C) : new Color(0xD8B35A)) : Partida.visivel(p.cor[a.lado]);
            double ax = x + a.x * mw, ay = y + a.y * mh;
            g.setColor(cc);
            g.fill(new Ellipse2D.Double(ax - 3, ay - 3, 6, 6));
            g.setColor(a == p.dono ? Tema.DOURADO : new Color(0, 0, 0, 140));
            g.draw(new Ellipse2D.Double(ax - 3, ay - 3, 6, 6));
        }
        if (p.bolaVisivel) {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x + p.bolaX * mw - 2, y + p.bolaY * mh - 2, 4, 4));
        }
        // o que a camera esta mostrando
        g.setColor(new Color(242, 193, 78, 120));
        double vw = area.width / Math.max(16, Math.min(34, area.width / 30.0)) / CX * mw * 0.8;
        double vh = vw * 0.75;
        double vx = x + alvoX / CX * mw, vy = y + (1 - alvoZ / CZ) * mh;
        Shape clip = g.getClip();
        g.clip(new Rectangle2D.Double(x, y, mw, mh));
        g.draw(new RoundRectangle2D.Double(vx - vw / 2, vy - vh / 2, vw, vh, 6, 6));
        g.setClip(clip);
    }
}
