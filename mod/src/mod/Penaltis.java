package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Timer;

// Penaltis em 3D, na camera atras do batedor: serve para a cobranca durante a partida
// (a.gb) e para a disputa (a.bg). Quem decide o resultado continua sendo o jogo: a cena
// pergunta (decide) um instante antes do chute e so encena o que o jogo sorteou.
public final class Penaltis extends JComponent {

    // resultados que a cena sabe mostrar
    public static final int GOL = 0, DEFESA = 1, TRAVE = 2, FORA = 3;

    // animacoes do conjunto "pen" (tools/tv/penaltis.py, mesma ordem)
    static final int CORRIDA = 0, CHUTE = 1, FESTA = 2, LAMENTA = 3, PRONTO = 4, VOO_DIR = 5, VOO_ESQ = 6, MEIO = 7, GOLEIRO_FESTA = 8;

    static final double CX = Transmissao.CX, CZ = Transmissao.CZ;
    static final double MARCA = CX - 11, Z0 = CZ / 2 - 3.66, Z1 = CZ / 2 + 3.66, ALTO = 2.44;
    static final double GOLEIRO_X = CX - 0.35;

    // tempos de uma cobranca (ms desde o inicio)
    static final long PREPARA = 1100, CORRE = 1050, PASSO_CHUTE = 85;
    static final long CONTATO = PREPARA + CORRE + 3 * PASSO_CHUTE;
    static final long DECIDE = CONTATO - 160;

    final boolean disputa;
    final Object casa, fora;
    final Transmissao est;
    Bonecos pen, tv;
    final Bonecos.Kit[] kits = new Bonecos.Kit[2], goleiros = new Bonecos.Kit[2];
    final Random rnd = new Random();
    final Timer relogio;
    BufferedImage tmp;
    String titulo = "";                     // cobranca durante o jogo: "Pênalti para ..."

    public void titulo(String t) { titulo = t == null ? "" : t; repaint(); }

    // placar da disputa (o que ja foi mostrado)
    final List<Integer>[] marcas;
    final int[] gols = new int[2];

    // cobranca em andamento
    Cobranca c;
    // fim da disputa
    String vencedor;
    Color vencedorCor;
    Runnable aoContinuar;
    RoundRectangle2D.Double botao;

    static final class Cobranca {
        int lado;                    // 0 casa bate, 1 fora bate
        Object batedor, goleiro;
        int numero;
        Bonecos.Corpo corpoB, corpoG;
        Callable<Integer> decide;
        Runnable fim;
        long t0;                     // inicio (relogio da cena)
        int resultado = -1;
        boolean avisouFim;
        // trajetoria
        double tx, ty, tz;           // alvo da bola
        double arco = 0.25, voo = 380; // altura extra no meio do caminho, duracao do voo
        int lance;                   // 0 normal, 1 cavadinha
        int mergulho;                // -1 pula para a direita da tela (z menor), +1 para a esquerda, 0 fica
        double alcance;              // quanto o goleiro anda no voo (m)
        long chegou = -1;            // quando a bola chega ao gol (para o texto)
        double rvx, rvy, rvz;        // rebote
    }

    @SuppressWarnings("unchecked")
    public Penaltis(boolean disputa, Object casa, Object fora) {
        this.disputa = disputa;
        this.casa = casa;
        this.fora = fora;
        marcas = new List[]{new ArrayList<Integer>(), new ArrayList<Integer>()};
        est = new Transmissao(Partida.cor(casa, "kB", Color.WHITE), Partida.cor(fora, "kB", Color.BLACK));
        pen = Bonecos.carrega("pen");
        tv = Bonecos.carrega("tv");
        kits();
        setOpaque(true);
        setFocusable(true);
        relogio = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) { anda(); repaint(); }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (botao != null && aoContinuar != null && botao.contains(e.getPoint())) { continua(); return; }
                pula();
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                setCursor(botao != null && botao.contains(e.getPoint()) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if ((k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) && aoContinuar != null) continua();
                else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ESCAPE) pula();
            }
        });
    }

    public void addNotify() {
        super.addNotify();
        relogio.start();
        requestFocusInWindow();
    }

    public void removeNotify() {
        relogio.stop();
        super.removeNotify();
    }

    static long tempoFixo = -1;             // relogio parado (para gravar video quadro a quadro)

    long agora() { return tempoFixo >= 0 ? tempoFixo : System.currentTimeMillis(); }

    // ------------------------------------------------------------------ uniformes

    void kits() {
        for (int lado = 0; lado < 2; lado++) {
            Object time = lado == 0 ? casa : fora;
            Image img = null;
            Object ic = Partida.chama(time, "kS");
            if (ic instanceof ImageIcon) img = ((ImageIcon) ic).getImage();
            Color c1 = Partida.cor(time, "kB", lado == 0 ? Color.WHITE : Color.BLACK), c2 = Partida.cor(time, "kC", lado == 0 ? Color.BLACK : Color.WHITE);
            kits[lado] = Bonecos.Kit.de(img, c1, c2);
            if (lado == 1 && Partida.distancia(kits[0].media, kits[1].media) < 75) {
                Bonecos.Kit melhor = null;
                for (int u = 1; u <= 3; u++) {
                    Object o = Partida.chamaInt(time, "bU", u);
                    if (!(o instanceof ImageIcon)) continue;
                    Bonecos.Kit k = Bonecos.Kit.de(((ImageIcon) o).getImage(), c1, c2);
                    if (melhor == null || Partida.distancia(kits[0].media, k.media) > Partida.distancia(kits[0].media, melhor.media)) melhor = k;
                }
                if (melhor == null || Partida.distancia(kits[0].media, melhor.media) < 75) melhor = Bonecos.Kit.de(null, c2, c1);
                kits[1] = melhor;
            }
        }
        goleiros[0] = Bonecos.Kit.de(null, new Color(0x2B2E2C), new Color(0xF2C14E));
        goleiros[1] = Bonecos.Kit.de(null, new Color(0xD8B35A), new Color(0x1A1408));
        goleiros[0].luva = 0xF2C14E;
        goleiros[1].luva = 0x22201C;
    }

    // numero da camisa: ordem do jogador na escalacao (igual a partida ao vivo)
    public static int numero(Object jogador, List<?> escalados) {
        if (escalados == null || jogador == null) return 0;
        ArrayList<Object> l = new ArrayList<Object>(escalados);
        Collections.sort(l, new Comparator<Object>() {
            public int compare(Object a, Object b) { return Partida.ordem(Partida.inteiro(Partida.chama(a, "fT"))) - Partida.ordem(Partida.inteiro(Partida.chama(b, "fT"))); }
        });
        int i = l.indexOf(jogador);
        return i < 0 ? 0 : i + 1;
    }

    // ------------------------------------------------------------------ comandos

    // comeca uma cobranca; decide e chamado um instante antes do chute e devolve GOL, DEFESA, TRAVE ou FORA
    public void cobra(int lado, Object batedor, Object goleiro, int numero, Callable<Integer> decide, Runnable fim) {
        Cobranca k = new Cobranca();
        k.lado = lado;
        k.batedor = batedor;
        k.goleiro = goleiro;
        k.numero = numero;
        k.corpoB = Bonecos.Corpo.de(Partida.nome(batedor));
        k.corpoG = Bonecos.Corpo.de(goleiro == null ? "goleiro" + lado : Partida.nome(goleiro));
        k.decide = decide;
        k.fim = fim;
        k.t0 = agora();
        c = k;
        repaint();
    }

    public void fimDisputa(String vencedor, Color cor, Runnable continuar) {
        this.vencedor = vencedor;
        this.vencedorCor = cor;
        this.aoContinuar = continuar;
        repaint();
    }

    void continua() {
        Runnable r = aoContinuar;
        aoContinuar = null;
        if (r != null) r.run();
    }

    // clique: adianta a cobranca (o resultado ja sorteado nao muda)
    void pula() {
        Cobranca k = c;
        if (k == null) return;
        long t = agora() - k.t0;
        if (t < DECIDE - 200) k.t0 -= (DECIDE - 200 - t);
        else if (k.chegou >= 0 && t < k.chegou + duracaoFim(k) - 300) k.t0 -= (k.chegou + duracaoFim(k) - 300 - t);
    }

    long duracaoFim(Cobranca k) { return k.resultado == GOL ? 2300 : 2000; }

    // ------------------------------------------------------------------ andamento

    void anda() {
        Cobranca k = c;
        if (k == null) return;
        long t = agora() - k.t0;
        if (k.resultado < 0 && t >= DECIDE) {
            int r = DEFESA;
            try { r = k.decide.call(); } catch (Throwable e) { e.printStackTrace(); }
            k.resultado = r;
            planeja(k);
        }
        if (k.resultado >= 0 && k.chegou < 0 && t >= CONTATO + (long) k.voo) {
            k.chegou = t;
            if (disputa) {
                marcas[k.lado].add(k.resultado == GOL ? 1 : 0);
                if (k.resultado == GOL) gols[k.lado]++;
            }
        }
        if (k.chegou >= 0 && !k.avisouFim && t >= k.chegou + duracaoFim(k)) {
            k.avisouFim = true;
            if (k.fim != null) k.fim.run();
        }
    }

    // onde a bola vai e para onde o goleiro pula, de acordo com o resultado
    void planeja(Cobranca k) {
        int canto = rnd.nextBoolean() ? 1 : -1;              // +1: lado de z maior (esquerda da tela)
        k.tx = CX + 0.05;
        switch (k.resultado) {
            case GOL: {
                int s = rnd.nextInt(100);
                if (s < 8) {
                    // cavadinha: goleiro cai e a bola vai devagar no meio
                    k.lance = 1;
                    k.tz = CZ / 2 + (rnd.nextDouble() - 0.5) * 0.6; k.ty = 1.25; k.arco = 1.6; k.voo = 820;
                    k.mergulho = rnd.nextBoolean() ? 1 : -1; k.alcance = 2.3;
                } else {
                    k.tz = CZ / 2 + canto * (2.2 + rnd.nextDouble() * 1.15);
                    k.ty = 0.25 + rnd.nextDouble() * 1.8;
                    k.voo = 330 + rnd.nextInt(90);
                    if (s < 72) { k.mergulho = -canto; k.alcance = 2.4; }
                    else if (s < 90) { k.mergulho = canto; k.alcance = 1.35; }   // acertou o canto, nao alcancou
                    else { k.mergulho = 0; k.alcance = 0; }
                }
                break;
            }
            case DEFESA: {
                k.mergulho = canto;
                k.alcance = 1.6 + rnd.nextDouble() * 0.7;
                k.tz = CZ / 2 + canto * (k.alcance + 0.25);
                k.ty = 0.35 + rnd.nextDouble() * 0.9;
                k.tx = GOLEIRO_X;
                k.voo = 360;
                k.rvx = -5.5 - rnd.nextDouble() * 3; k.rvz = canto * (3 + rnd.nextDouble() * 4); k.rvy = 2.5 + rnd.nextDouble() * 2;
                break;
            }
            case TRAVE: {
                k.mergulho = rnd.nextBoolean() ? canto : -canto;
                k.alcance = 2.2;
                if (rnd.nextInt(4) == 0) { k.tz = CZ / 2 + canto * rnd.nextDouble() * 2.6; k.ty = ALTO; }   // travessao
                else { k.tz = canto > 0 ? Z1 : Z0; k.ty = 0.4 + rnd.nextDouble() * 1.8; }
                k.voo = 340;
                k.rvx = -7 - rnd.nextDouble() * 4; k.rvz = canto * (2 + rnd.nextDouble() * 3); k.rvy = k.ty >= ALTO ? 4 : 1.5;
                break;
            }
            default: {
                k.mergulho = rnd.nextInt(3) == 0 ? 0 : (rnd.nextBoolean() ? 1 : -1);
                k.alcance = 2.3;
                if (rnd.nextBoolean()) { k.tz = CZ / 2 + canto * (4.3 + rnd.nextDouble() * 1.6); k.ty = 0.3 + rnd.nextDouble() * 2.2; }
                else { k.tz = CZ / 2 + canto * rnd.nextDouble() * 3; k.ty = 3.0 + rnd.nextDouble() * 1.6; }
                k.voo = 420;
            }
        }
    }

    // ------------------------------------------------------------------ pintura

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            Rectangle2D.Double area = new Rectangle2D.Double(0, 0, getWidth(), getHeight());
            pinta(g, area);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            g.dispose();
        }
    }

    Transmissao.Camera camera(Rectangle2D.Double area, long t) {
        double u = Math.max(0, Math.min(1, t / (double) (PREPARA + CORRE)));
        u = u * u * (3 - 2 * u);
        double dist = 25.6 - 1.5 * u;
        // um pouco a direita do batedor (que vem da esquerda) para ele nao tampar o goleiro
        return new Transmissao.Camera(CX - 0.5, CZ / 2 - 0.3, -Math.PI / 2 + 0.085, 0.13, dist, false, 0, area.height * 2.45,
            area.getCenterX(), area.y + area.height * 0.57);
    }

    void pinta(Graphics2D g, Rectangle2D.Double area) {
        Cobranca k = c;
        long t = k == null ? 0 : agora() - k.t0;
        Transmissao.Camera cam = camera(area, k == null ? 0 : t);
        // fundo: estadio, arquibancada atras do gol, gramado e linhas
        est.alvoX = CX - 8; est.alvoZ = CZ / 2;
        est.estadio(g, cam, area);
        est.textura(g, cam, est.torcida, new double[]{CX + 9, 1.1, CZ / 2 + 76}, new double[]{CX + 9, 1.1, CZ / 2 - 76}, new double[]{CX + 27, 17, CZ / 2 + 76}, 24);
        g.setColor(new Color(0x101412));
        g.fill(est.poligono(cam, new double[][]{{CX + 9, 0, CZ / 2 - 34}, {CX + 9, 0, CZ / 2 + 34}, {CX + 9, 1.15, CZ / 2 + 34}, {CX + 9, 1.15, CZ / 2 - 34}}));
        g.setColor(Transmissao.GRAMA_FORA);
        g.fill(est.chao(cam, CX / 2 - 20, -14, CX + 9, CZ + 14));
        for (int i = 10; i < 20; i++) {
            g.setColor(i % 2 == 0 ? Transmissao.GRAMA_A : Transmissao.GRAMA_B);
            g.fill(est.chao(cam, CX * i / 20.0, -3, CX * (i + 1) / 20.0 + 0.02, CZ + 3));
        }
        est.linhas(g, cam);
        est.placas(g, cam);
        // luz dos refletores
        g.setPaint(new RadialGradientPaint((float) area.getCenterX(), (float) (area.y + area.height * 0.45), (float) (area.width * 0.8),
            new float[]{0f, 0.6f, 1f}, new Color[]{new Color(255, 250, 220, 24), new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
        g.fill(area);

        // posicoes da cena
        double[] bola = bola(k, t);
        double[] gol = goleiro(k, t);
        double[] bat = batedor(k, t);
        // sombras
        sombra(g, cam, gol[0], gol[2], gol[3] > 0.5 ? 0.9 : 0.5, 0.4);
        sombra(g, cam, bat[0], bat[2], 0.5, 0.4);
        if (bola != null) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(est.elipse(cam, bola[0] + 0.03, bola[2] + 0.03, 0.13, 0.13));
        }
        // rede e bola dentro do gol, traves, goleiro, bola na frente e batedor
        rede(g, cam, k, t, bola);
        boolean dentro = bola != null && bola[0] > CX + 0.02;
        if (dentro) bolaDesenho(g, cam, bola);
        traves(g, cam);
        boolean bolaAtras = bola != null && !dentro && bola[0] > gol[0];
        if (bolaAtras) bolaDesenho(g, cam, bola);
        if (k != null) desenhaGoleiro(g, cam, k, t, gol);
        if (bola != null && !dentro && !bolaAtras) bolaDesenho(g, cam, bola);
        if (k != null) desenhaBatedor(g, cam, k, t, bat);
        else if (bola != null) bolaDesenho(g, cam, bola);

        // por cima: placar, textos e fim
        placar(g, area);
        textos(g, area, k, t);
        if (aoContinuar != null) fim(g, area);
        // borda dourada da janela
        g.setColor(new Color(242, 193, 78, 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Rectangle2D.Double(area.x + 0.75, area.y + 0.75, area.width - 1.5, area.height - 1.5));
    }

    void sombra(Graphics2D g, Transmissao.Camera cam, double x, double z, double rx, double rz) {
        g.setColor(new Color(0, 0, 0, 85));
        g.fill(est.elipse(cam, x + 0.12, z + 0.08, rx, rz));
    }

    static double suave(double u) { u = Math.max(0, Math.min(1, u)); return u * u * (3 - 2 * u); }

    // bola: [x, y, z] ou null
    double[] bola(Cobranca k, long t) {
        if (k == null) return new double[]{MARCA, 0.11, CZ / 2};
        if (t < CONTATO || k.resultado < 0) return new double[]{MARCA, 0.11, CZ / 2};
        double dt = t - CONTATO;
        if (dt <= k.voo) {
            double s = dt / k.voo;
            double x = MARCA + (k.tx - MARCA) * s, z = CZ / 2 + (k.tz - CZ / 2) * s;
            double y = 0.11 + (k.ty - 0.11) * s + k.arco * 4 * s * (1 - s);
            return new double[]{x, y, z};
        }
        double d = (dt - k.voo) / 1000.0;
        if (k.resultado == GOL) {
            // entra, estufa a rede e cai
            double fundo = Math.min(1.75, 1.75 * (1 - Math.exp(-d * 9)));
            double y = Math.max(0.11, k.ty - 4.9 * d * d * (k.ty > 0.4 ? 1 : 0));
            return new double[]{k.tx + fundo, y, k.tz};
        }
        if (k.resultado == FORA) {
            double vx = (k.tx - MARCA) / (k.voo / 1000.0);
            double x = k.tx + vx * d, y = k.ty + (k.ty - 0.11) / (k.voo / 1000.0) * d * 0.4;
            if (x > CX + 9) return null;
            return new double[]{x, y, k.tz + (k.tz - CZ / 2) / (k.voo / 1000.0) * d};
        }
        // rebote (defesa e trave): quica e rola para fora
        double x = k.tx + k.rvx * d, z = k.tz + k.rvz * d;
        double y = k.ty + k.rvy * d - 4.9 * d * d;
        if (y < 0.11) {
            // depois do primeiro quique fica rasteira
            double tq = (k.rvy + Math.sqrt(k.rvy * k.rvy + 19.6 * (k.ty - 0.11))) / 9.8;
            double dd = d - tq;
            double v2 = Math.abs(k.rvy - 9.8 * tq) * 0.45;
            y = 0.11 + Math.max(0, v2 * dd - 4.9 * dd * dd);
            double atrito = Math.exp(-dd * 1.2);
            x = k.tx + k.rvx * tq + k.rvx * (1 - atrito) / 1.2;
            z = k.tz + k.rvz * tq + k.rvz * (1 - atrito) / 1.2;
        }
        return new double[]{x, y, z};
    }

    // goleiro: [x, altura, z, deitado(0/1), quadro]
    double[] goleiro(Cobranca k, long t) {
        double x = GOLEIRO_X, z = CZ / 2;
        if (k == null || k.resultado < 0 || t < CONTATO - 70) {
            int q = (int) ((t / 140) % 4);
            // passinhos para os lados enquanto espera
            z += Math.sin(t / 520.0) * 0.35;
            return new double[]{x, 0, z, 0, q};
        }
        double z0 = CZ / 2 + Math.sin((CONTATO - 70) / 520.0) * 0.35;
        double dt = t - (CONTATO - 70);
        if (k.mergulho == 0) {
            int q = dt < 120 ? 0 : dt < 260 ? 1 : 2;
            return new double[]{x, 0, z0, 0, q};
        }
        double u = Math.min(1, dt / 620.0);
        int q = Math.min(6, (int) (dt / 95));
        double lado = k.mergulho;
        double zz = z0 + lado * k.alcance * suave(Math.min(1, dt / 420.0));
        double alt = dt < 420 ? Math.sin(Math.PI * dt / 420.0) * 0.55 : 0;
        // o quadro gira em volta da pelve: compensa a altura dos pes para o corpo ficar no ar
        double[] rolo = {0, 0.35, 0.8, 1.2, 1.4, 1.5, 1.55};
        double desce = (1 - Math.cos(rolo[q])) * 0.72;
        return new double[]{x, alt - desce, zz, u > 0.6 ? 1 : 0, q};
    }

    // batedor: [x, y, z, anim, quadro]
    double[] batedor(Cobranca k, long t) {
        double bx = MARCA - 0.42, bz = CZ / 2 + 0.22;
        double sx = MARCA - 3.3, sz = CZ / 2 + 2.1;
        if (k == null || t < PREPARA) return new double[]{sx, 0, sz, -1, 0};
        if (t < PREPARA + CORRE) {
            double u = (t - PREPARA) / (double) CORRE;
            return new double[]{sx + (bx - sx) * u, 0, sz + (bz - sz) * u, CORRIDA, ((t - PREPARA) / 70) % 8};
        }
        long dc = t - PREPARA - CORRE;
        if (k.chegou < 0 || t < k.chegou + 450) return new double[]{bx, 0, bz, CHUTE, Math.min(5, dc / PASSO_CHUTE)};
        long d = t - k.chegou - 450;
        if (k.resultado == GOL) {
            // sai comemorando para o lado da torcida, sem tampar a tela
            double u = suave(Math.min(1, d / 1700.0));
            return new double[]{bx - 0.9 * u, 0, bz + 3.4 * u, FESTA, (d / 160) % 4};
        }
        return new double[]{bx, 0, bz, LAMENTA, d < 400 ? 0 : 1};
    }

    void desenhaGoleiro(Graphics2D g, Transmissao.Camera cam, Cobranca k, long t, double[] p) {
        if (pen == null) return;
        int lado = 1 - k.lado;
        Bonecos.Kit kit = goleiros[lado];
        int anim, q = (int) p[4];
        if (k.resultado < 0 || t < CONTATO - 70) anim = PRONTO;
        else if (k.mergulho == 0) anim = MEIO;
        else anim = k.mergulho < 0 ? VOO_DIR : VOO_ESQ;
        // depois da defesa ele levanta e comemora
        if (k.chegou >= 0 && (k.resultado == DEFESA) && t > k.chegou + 900) {
            anim = GOLEIRO_FESTA; q = (int) (((t - k.chegou) / 160) % 4);
            p = new double[]{p[0], 0, p[2], 0, q};
        }
        sprite(g, cam, pen, pen.quadro(k.corpoG.corte, anim, q), kit, k.corpoG, 1, p[0], p[1], p[2]);
    }

    void desenhaBatedor(Graphics2D g, Transmissao.Camera cam, Cobranca k, long t, double[] p) {
        Bonecos.Kit kit = kits[k.lado];
        if (p[3] < 0) {
            // parado esperando o apito (de costas, olhando a bola)
            if (tv == null) return;
            double giro = cam.giro(MARCA - p[0], -(CZ / 2 - p[2]));
            int dir = (int) Math.round(giro / (2 * Math.PI / tv.dirs));
            sprite(g, cam, tv, tv.quadro(k.corpoB.corte, dir, 0), kit, k.corpoB, k.numero, p[0], 0, p[2]);
            return;
        }
        if (pen == null) return;
        sprite(g, cam, pen, pen.quadro(k.corpoB.corte, (int) p[3], (int) p[4]), kit, k.corpoB, k.numero, p[0], p[1], p[2]);
    }

    // quadro de um conjunto de bonecos com o pe em (x, y, z)
    void sprite(Graphics2D g, Transmissao.Camera cam, Bonecos b, int q, Bonecos.Kit kit, Bonecos.Corpo corpo, int numero, double x, double y, double z) {
        if (b.w[q] <= 0) return;
        double[] o = new double[3];
        cam.projeta(x, y, z, o);
        if (o[2] < 1) return;
        tmp = b.pinta(q, kit, corpo, numero, tmp);
        double s = cam.pxPorM(o[2]) / b.escala;
        double dx = o[0] + (b.x0[q] - b.largura / 2.0) * s, dy = o[1] + (b.y0[q] - b.peY) * s;
        g.drawImage(tmp, (int) Math.round(dx), (int) Math.round(dy), (int) Math.round(dx + b.w[q] * s), (int) Math.round(dy + b.h[q] * s),
            0, 0, b.w[q], b.h[q], null);
    }

    void bolaDesenho(Graphics2D g, Transmissao.Camera cam, double[] b) {
        double[] o = new double[3];
        cam.projeta(b[0], b[1], b[2], o);
        if (o[2] < 1) return;
        double r = Math.max(2.2, cam.pxPorM(o[2]) * 0.115);
        Ellipse2D.Double e = new Ellipse2D.Double(o[0] - r, o[1] - r, r * 2, r * 2);
        g.setPaint(new RadialGradientPaint((float) (o[0] - r * 0.35), (float) (o[1] - r * 0.4), (float) (r * 1.7),
            new float[]{0f, 1f}, new Color[]{Color.WHITE, new Color(0xB9B6AE)}));
        g.fill(e);
        if (r > 4) {
            // gomos escuros
            g.setColor(new Color(0x2A2A2A));
            double a = (b[0] + b[2]) * 2.1;
            for (int i = 0; i < 3; i++) {
                double aa = a + i * 2.09;
                g.fill(new Ellipse2D.Double(o[0] + Math.cos(aa) * r * 0.45 - r * 0.2, o[1] + Math.sin(aa) * r * 0.45 - r * 0.2, r * 0.4, r * 0.4));
            }
        }
        g.setColor(new Color(0x2A2A2A));
        g.setStroke(new BasicStroke((float) Math.max(0.8, r * 0.12)));
        g.draw(e);
    }

    // rede com o estufar quando a bola entra
    void rede(Graphics2D g, Transmissao.Camera cam, Cobranca k, long t, double[] bola) {
        double amp = 0, by = 1, bz = CZ / 2;
        if (k != null && k.resultado == GOL && k.chegou >= 0) {
            double d = (t - k.chegou) / 1000.0;
            amp = 0.75 * Math.exp(-d * 2.2) * Math.cos(d * 9) + 0.25 * Math.exp(-d * 0.6);
            by = Math.max(0.3, k.ty); bz = k.tz;
        }
        double fx = CX + 2.0, hb = ALTO * 0.8;
        g.setColor(new Color(235, 235, 230, 95));
        double pm = cam.pxPorM(cam.fundo(CX, 1.2, CZ / 2));
        g.setStroke(new BasicStroke((float) Math.max(0.7, pm * 0.02)));
        int nu = 22, nv = 9;
        double[] a = new double[3], b = new double[3];
        // fundo e teto: linhas verticais e horizontais
        for (int i = 0; i <= nu; i++) {
            double z = Z0 + (Z1 - Z0) * i / nu;
            Path2D.Double p = new Path2D.Double();
            for (int j = 0; j <= nv + 4; j++) {
                double[] q = pontoRede(z, j, nv, fx, hb, amp, by, bz);
                cam.projeta(q[0], q[1], q[2], a);
                if (j == 0) p.moveTo(a[0], a[1]); else p.lineTo(a[0], a[1]);
            }
            g.draw(p);
        }
        for (int j = 0; j <= nv + 4; j++) {
            Path2D.Double p = new Path2D.Double();
            for (int i = 0; i <= nu; i++) {
                double z = Z0 + (Z1 - Z0) * i / nu;
                double[] q = pontoRede(z, j, nv, fx, hb, amp, by, bz);
                cam.projeta(q[0], q[1], q[2], a);
                if (i == 0) p.moveTo(a[0], a[1]); else p.lineTo(a[0], a[1]);
            }
            g.draw(p);
        }
        // laterais
        for (int s = 0; s < 2; s++) {
            double z = s == 0 ? Z0 : Z1;
            for (int i = 0; i <= 5; i++) {
                double x = CX + 2.0 * i / 5;
                double topo = ALTO + (hb - ALTO) * i / 5;
                est.seg(g, cam, x, 0, z, x, topo, z, a, b);
            }
            for (int j = 0; j <= 6; j++) {
                double y = hb * j / 6;
                est.seg(g, cam, CX, y / 0.8, z, fx, y, z, a, b);
            }
        }
    }

    // j de 0..nv: fundo, de baixo para cima; nv..nv+4: teto, do fundo ate o travessao
    double[] pontoRede(double z, int j, int nv, double fx, double hb, double amp, double by, double bz) {
        double x, y;
        if (j <= nv) { x = fx; y = hb * j / nv; }
        else { double u = (j - nv) / 4.0; x = fx + (CX - fx) * u; y = hb + (ALTO - hb) * u; }
        if (amp != 0) {
            double d2 = (z - bz) * (z - bz) + (y - by) * (y - by);
            double borda = Math.min(1, Math.min(z - Z0, Z1 - z) / 1.2) * Math.min(1, y / 0.6);
            x += amp * Math.exp(-d2 / 0.9) * Math.max(0, borda);
        }
        return new double[]{x, y, z};
    }

    void traves(Graphics2D g, Transmissao.Camera cam) {
        double pm = cam.pxPorM(cam.fundo(CX, 1.2, CZ / 2));
        double[] a = new double[3], b = new double[3];
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke((float) Math.max(2, pm * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        est.seg(g, cam, CX + 0.06, 0, Z0 + 0.06, CX + 0.06, ALTO, Z0 + 0.06, a, b);
        g.setColor(new Color(250, 250, 248));
        g.setStroke(new BasicStroke((float) Math.max(2, pm * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        est.seg(g, cam, CX, 0, Z0, CX, ALTO, Z0, a, b);
        est.seg(g, cam, CX, 0, Z1, CX, ALTO, Z1, a, b);
        est.seg(g, cam, CX, ALTO, Z0, CX, ALTO, Z1, a, b);
    }

    // ------------------------------------------------------------------ placar e textos

    void placar(Graphics2D g, Rectangle2D.Double area) {
        double w = Math.min(560, area.width - 40), h = disputa ? 92 : 54;
        double x = area.getCenterX() - w / 2, y = area.y + 16;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h, 16, 16);
        g.setColor(new Color(9, 11, 10, 215));
        g.fill(r);
        g.setColor(new Color(242, 193, 78, 110));
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        if (!disputa) {
            g.setFont(Estreia.espacada(Estreia.semi(11f), 0.18f));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Tema.NEON);
            String s = "PÊNALTI";
            g.drawString(s, (float) (x + 20), (float) (y + h / 2 + fm.getAscent() / 2.0 - 2));
            g.setFont(Estreia.titulo(24f));
            fm = g.getFontMetrics();
            g.setColor(Tema.TEXTO);
            String tt = Partida.corta(titulo, fm, w - 130);
            g.drawString(tt, (float) (x + w - 20 - fm.stringWidth(tt)), (float) (y + h / 2 + fm.getAscent() / 2.0 - 3));
            return;
        }
        // nomes, escudos e placar
        double meio = area.getCenterX();
        g.setFont(Estreia.titulo(40f));
        FontMetrics fm = g.getFontMetrics();
        String pl = gols[0] + "  ×  " + gols[1];
        g.setColor(Tema.TEXTO);
        g.drawString(pl, (float) (meio - fm.stringWidth(pl) / 2.0), (float) (y + 42));
        g.setFont(Estreia.titulo(22f));
        fm = g.getFontMetrics();
        for (int lado = 0; lado < 2; lado++) {
            Object time = lado == 0 ? casa : fora;
            String n = Partida.corta(Partida.nome(time), fm, w / 2 - 110);
            double cx = lado == 0 ? meio - 62 - fm.stringWidth(n) : meio + 62;
            g.setColor(Tema.TEXTO);
            g.drawString(n, (float) cx, (float) (y + 34));
            Object ic = Partida.chama(time, "kU");
            if (ic instanceof ImageIcon) {
                Image im = ((ImageIcon) ic).getImage();
                int iw = 30, ih = 30;
                double ix = lado == 0 ? x + 14 : x + w - 14 - iw;
                g.drawImage(im, (int) ix, (int) (y + 12), iw, ih, null);
            }
            // bolinhas: gol (dourada), perdeu (vermelha com x), a bater (vazia)
            List<Integer> m = marcas[lado];
            int n0 = Math.max(5, Math.max(marcas[0].size(), marcas[1].size()));
            int mostra = Math.min(n0, 10);
            int ini = Math.max(0, n0 - mostra);
            double R = 7, passo = 19;
            double total = (mostra - 1) * passo;
            double y0 = y + 66;
            double xc0 = lado == 0 ? meio - 40 - total : meio + 40;
            for (int i = 0; i < mostra; i++) {
                int idx = ini + i;
                double cx2 = xc0 + i * passo;
                Ellipse2D.Double e = new Ellipse2D.Double(cx2 - R, y0 - R, 2 * R, 2 * R);
                if (idx < m.size()) {
                    if (m.get(idx) == 1) {
                        g.setColor(Tema.NEON);
                        g.fill(e);
                    } else {
                        g.setColor(new Color(0xE5463B));
                        g.fill(e);
                        g.setColor(new Color(20, 10, 10));
                        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g.draw(new Line2D.Double(cx2 - 3, y0 - 3, cx2 + 3, y0 + 3));
                        g.draw(new Line2D.Double(cx2 - 3, y0 + 3, cx2 + 3, y0 - 3));
                    }
                } else {
                    g.setColor(new Color(255, 255, 255, 70));
                    g.setStroke(new BasicStroke(1.4f));
                    g.draw(e);
                }
            }
        }
        // de quem e a vez
        Cobranca k = c;
        if (k != null && vencedor == null) {
            double cx2 = k.lado == 0 ? x + 8 : x + w - 8;
            g.setColor(Tema.NEON);
            g.fill(new RoundRectangle2D.Double(cx2 - 2, y + 14, 4, h - 28, 4, 4));
        }
    }

    void textos(Graphics2D g, Rectangle2D.Double area, Cobranca k, long t) {
        if (k == null || aoContinuar != null) return;
        String grande = null, linha;
        Color cor = Tema.NEON;
        String nome = Partida.nome(k.batedor);
        if (k.chegou < 0) {
            if (t > CONTATO) return;
            linha = nome + (t < PREPARA ? " ajeita a bola" : " corre para a bola");
        } else {
            switch (k.resultado) {
                case GOL: grande = "GOL!"; linha = k.lance == 1 ? "De cavadinha! " + nome + " não perdoa" : nome + " bate e marca"; break;
                case DEFESA: grande = "DEFENDEU!"; cor = Tema.MENTA; linha = "Grande defesa de " + Partida.nome(k.goleiro); break;
                case TRAVE: grande = "NA TRAVE!"; cor = new Color(0xFF8A5B); linha = nome + " acerta a trave"; break;
                default: grande = "PRA FORA!"; cor = new Color(0xFF8A5B); linha = nome + " manda para fora";
            }
        }
        double base = area.getMaxY() - 26;
        if (grande != null) {
            long d = t - k.chegou;
            double u = suave(Math.min(1, d / 260.0));
            float px = (float) (64 + 26 * (1 - u));
            g.setFont(Estreia.titulo(px));
            FontMetrics fm = g.getFontMetrics();
            float x = (float) (area.getCenterX() - fm.stringWidth(grande) / 2.0), y = (float) (base - 38);
            g.setColor(new Color(0, 0, 0, (int) (150 * u)));
            g.drawString(grande, x + 3, y + 3);
            g.setPaint(new GradientPaint(0, y - fm.getAscent(), Partida.clareia(cor, 0.35f), 0, y, cor));
            g.drawString(grande, x, y);
        }
        g.setFont(Estreia.semi(15f));
        FontMetrics fm = g.getFontMetrics();
        linha = Partida.corta(linha, fm, area.width - 80);
        double w = fm.stringWidth(linha) + 36, h = 32;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(area.getCenterX() - w / 2, base - h + 4, w, h, h, h);
        g.setColor(new Color(9, 11, 10, 210));
        g.fill(r);
        g.setColor(new Color(242, 193, 78, 120));
        g.draw(r);
        g.setColor(Tema.TEXTO);
        g.drawString(linha, (float) (area.getCenterX() - fm.stringWidth(linha) / 2.0), (float) (base - h / 2 + 4 + fm.getAscent() / 2.0 - 2));
    }

    void fim(Graphics2D g, Rectangle2D.Double area) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(area);
        double cy = area.getCenterY() + 10;
        g.setFont(Estreia.espacada(Estreia.semi(12f), 0.2f));
        FontMetrics fm = g.getFontMetrics();
        String s = "FIM DA DISPUTA";
        g.setColor(Tema.NEON);
        g.drawString(s, (float) (area.getCenterX() - fm.stringWidth(s) / 2.0), (float) (cy - 58));
        g.setFont(Estreia.titulo(54f));
        fm = g.getFontMetrics();
        String v = vencedor == null ? "" : vencedor;
        v = Partida.corta(v, fm, area.width - 60);
        g.setColor(Tema.TEXTO);
        g.drawString(v, (float) (area.getCenterX() - fm.stringWidth(v) / 2.0), (float) (cy));
        g.setFont(Estreia.semi(14f));
        fm = g.getFontMetrics();
        String b = "Continuar";
        double w = fm.stringWidth(b) + 56, h = 36;
        botao = new RoundRectangle2D.Double(area.getCenterX() - w / 2, cy + 26, w, h, h, h);
        g.setPaint(new GradientPaint(0, (float) botao.y, new Color(0xF6CE6A), 0, (float) (botao.y + h), new Color(0xD9A53B)));
        g.fill(botao);
        g.setColor(new Color(0x1A1408));
        g.drawString(b, (float) (area.getCenterX() - fm.stringWidth(b) / 2.0), (float) (botao.y + h / 2 + fm.getAscent() / 2.0 - 2));
    }
}
