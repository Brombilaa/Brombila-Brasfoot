package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Menu inicial "Noite de estreia": substitui a tela pequena do jogo (a.gq).
// Os itens chamam as mesmas acoes dos rotulos originais (que continuam na
// janela, so que fora da tela), entao Novo jogo, Jogo salvo, Editor e
// Registrar funcionam exatamente como antes.
public class MenuInicial extends JComponent {
    private static final String VERSAO = "10/2026";   // mes e ano da versao do mod

    private static class Item {
        final String nome, dica, icone;
        final JLabel acao; // rotulo original do jogo (null = sair)
        float ativo;       // 0..1, animado
        Item(String nome, String dica, String icone, JLabel acao) {
            this.nome = nome; this.dica = dica; this.icone = icone; this.acao = acao;
        }
    }

    private final JFrame janela;
    private final List<Item> itens = new ArrayList<Item>();
    private final Rectangle[] areas;
    private int selecionado = 0;
    private final long inicio = System.currentTimeMillis();
    // Abertura com o logo antes do menu (ABERTURA s). Termina com o logo no lugar dele no
    // menu; ai o menu comeca a entrar. Clique ou tecla pula. -Dbrombila.abertura=0 desliga.
    static final float ABERTURA = 3.7f;
    // Preferencias: "Abertura" (brombila.ini abertura=0 desliga).
    private long inicioMenu = "0".equals(System.getProperty("brombila.abertura")) || "0".equals(Janela.pref("abertura", "1"))
        ? inicio : inicio + Math.round(ABERTURA * 1000);
    private static final float[][] POEIRA = poeira(46);
    // som da abertura (tools/som/abertura.py); a torcida continua um pouco no menu
    private final Som som = inicioMenu > inicio ? Som.carrega("/mod/sons/abertura.wav") : null;
    private long ultimoQuadro = inicio;
    // a abertura conta a partir do primeiro desenho (no Windows a janela demora a aparecer)
    private boolean pintou;
    private final Timer timer;
    private BufferedImage veu;   // degrades por cima da foto (pre-desenhados)

    // ------------------------------------------------------------ instalar

    // Chamado quando a janela do jogo ainda esta sendo montada: some ate o menu
    // novo ficar pronto, para nao piscar a tela antiga.
    static void prepara(Window w) {
        try { w.setOpacity(0f); } catch (Throwable t) {}
    }

    static void instala(JFrame gq) {
        try {
            if (gq.getContentPane() instanceof MenuInicial) return;
            MenuInicial m = new MenuInicial(gq);
            Dimension tela = Toolkit.getDefaultToolkit().getScreenSize();
            int w = Math.max(800, Math.min(1120, Math.round(tela.width * 0.64f)));
            w = Math.min(w, tela.width - 40);
            int h = Math.round(w * 9f / 16f);
            gq.setBackground(Estreia.NOITE);
            gq.getRootPane().setBackground(Estreia.NOITE);
            gq.setContentPane(m);
            gq.setSize(w, h);
            // logo grande e foto ja prontos antes da janela aparecer (a janela ainda esta
            // invisivel): o primeiro quadro sai na hora, sem tela escura
            if (m.inicioMenu > m.inicio) {
                m.preparaAbertura(h, w / 100f);
                Estreia.foto();
            }
            gq.setLocationRelativeTo(null);
            gq.validate();
            m.requestFocusInWindow();
        } catch (Throwable t) {
            t.printStackTrace();
            try { gq.setOpacity(1f); } catch (Throwable x) {}
        }
    }

    private static JLabel rotulo(Object dono, String campo) {
        Object o = Reflexao.campo(dono, campo);
        return o instanceof JLabel ? (JLabel) o : null;
    }

    MenuInicial(JFrame gq) {
        janela = gq;
        JLabel registrar = rotulo(gq, "afu");
        itens.add(new Item("Novo jogo", "Escolher ligas e técnico", "bola", rotulo(gq, "aft")));
        itens.add(new Item("Jogo salvo", "Continuar carreira", "pasta", rotulo(gq, "afx")));
        itens.add(new Item("Editor", "Times e jogadores", "lapis", rotulo(gq, "afs")));
        // Jogo registrado: o proprio jogo esconde o "Registrar jogo".
        if (registrar != null && registrar.isVisible()) {
            itens.add(new Item("Registrar jogo", "Versão não registrada", "chave", registrar));
        }
        itens.add(new Item("Sair", "Fechar o jogo", "sair", null));
        areas = new Rectangle[itens.size()];
        itens.get(0).ativo = 1f;

        setOpaque(true);
        setFocusable(true);
        timer = new Timer(Animacao.QUADRO_MS, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repaint();
                int d = (int) intervalo();
                if (timer.getDelay() != d) timer.setDelay(d);
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int i = itemEm(e.getX(), e.getY());
                if (i >= 0 && i != selecionado) { selecionado = i; acorda(); }
                setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
            public void mouseClicked(MouseEvent e) {
                if (naAbertura()) { pula(); return; }
                int i = itemEm(e.getX(), e.getY());
                if (i >= 0 && SwingUtilities.isLeftMouseButton(e)) aciona(i);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (naAbertura()) { pula(); return; }
                int n = itens.size();
                if (e.getKeyCode() == KeyEvent.VK_DOWN) { selecionado = (selecionado + 1) % n; acorda(); }
                else if (e.getKeyCode() == KeyEvent.VK_UP) { selecionado = (selecionado + n - 1) % n; acorda(); }
                else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) aciona(selecionado);
            }
        });
    }

    // Intervalo do proximo quadro conforme o que esta mexendo: entrada, troca de item,
    // brilho do logo e faixa de luz a 60 quadros/s; parado (so o zoom lento do estadio,
    // que anda menos de 1 px por quadro) a 12.
    // Cada tique do relogio custa, entao o relogio tambem desacelera (nao so o desenho).
    // Os relogios sao os mesmos do desenho: faixa pelo inicio, brilho pelo do titulo.
    private long intervalo() {
        if (naAbertura()) return Animacao.QUADRO_MS;
        long agora = System.currentTimeMillis();
        float seg = (agora - inicioMenu) / 1000f;
        if (seg < 2.5f) return Animacao.QUADRO_MS;
        for (int i = 0; i < itens.size(); i++) {
            float a = itens.get(i).ativo;
            if (a != (i == selecionado ? 1f : 0f)) return Animacao.QUADRO_MS;
        }
        float fundo = (agora - inicio) / 1000f;
        float faixa = ((fundo - 1.5f) % 7f) / 7f;
        float brilho = ((segTitulo(seg) - 1.2f) % 9f) / 1.6f;
        if (brilho <= 1.05f || faixa > 0.53f) return Animacao.QUADRO_MS;
        return 80;
    }

    // Mudou o item escolhido: volta a 60 quadros/s na hora.
    private void acorda() {
        if (timer.getDelay() != Animacao.QUADRO_MS) {
            timer.setDelay(Animacao.QUADRO_MS);
            timer.restart();
        }
    }

    @Override public void addNotify() { super.addNotify(); timer.start(); }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }

    private int itemEm(int x, int y) {
        for (int i = 0; i < areas.length; i++) if (areas[i] != null && areas[i].contains(x, y)) return i;
        return -1;
    }

    private void aciona(int i) {
        final Item it = itens.get(i);
        selecionado = i;
        if (it.acao == null) {
            System.exit(0);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                MouseEvent ev = new MouseEvent(it.acao, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                    0, 1, 1, 1, false, MouseEvent.BUTTON1);
                for (MouseListener l : it.acao.getMouseListeners()) l.mouseClicked(ev);
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    // ------------------------------------------------------------- abertura

    private boolean naAbertura() { return System.currentTimeMillis() < inicioMenu; }

    private void pula() {
        inicioMenu = System.currentTimeMillis();
        if (som != null) som.some(250);
        acorda();
        repaint();
    }

    // Particulas de poeira na luz do refletor: x, y (0..1), tamanho, velocidade, fase.
    private static float[][] poeira(int n) {
        java.util.Random r = new java.util.Random(22);
        float[][] p = new float[n][5];
        for (float[] q : p) {
            q[0] = r.nextFloat(); q[1] = r.nextFloat(); q[2] = 0.5f + r.nextFloat() * 1.6f;
            q[3] = 0.02f + r.nextFloat() * 0.05f; q[4] = r.nextFloat() * 6.28f;
        }
        return p;
    }

    private BufferedImage aberturaSem, aberturaBola, aberturaSombra;

    // O estadio ja aparece (escurecido) -> a bola do logo entra rolando pela esquerda e
    // desenha os rastros -> bate no lugar dela e o B e o nome aparecem com um estouro de luz
    // -> brilho e flashes de camera -> o logo sobe e diminui ate o lugar do logo do menu.
    private static final float BATE = 1.1f, SOBE = 2.6f;

    private static float alturaAbertura(int h, float u) { return Math.min(44f * u, h * 0.72f); }

    // Logo grande da abertura, sem a bola, a bola e a sombra (devolve a altura).
    private int preparaAbertura(int h, float u) {
        int a0 = Math.round(alturaAbertura(h, u));
        if (aberturaSem == null || aberturaSem.getHeight() != a0) {
            BufferedImage lo = Logo.branco();
            BufferedImage cheio = Logo.reduz(lo, Math.round(a0 * lo.getWidth() / (float) lo.getHeight()), a0);
            BufferedImage[] p = Logo.separa(cheio);
            aberturaSem = p[0];
            aberturaBola = p[1];
            aberturaSombra = Logo.sombra(cheio, Math.max(2, Math.round(0.9f * u)));
        }
        return a0;
    }

    private void abertura(Graphics2D g, int w, int h, float u, float seg, float t) {
        g.setColor(Estreia.NOITE);
        g.fillRect(0, 0, w, h);
        float mv = vaiEVem((t - SOBE) / 1.05f);
        // estadio: ja comeca visivel (meio apagado) e acende de vez quando o logo sobe
        float est = Math.min(1f, t / 0.25f) * (0.5f + 0.5f * Animacao.suave((t - 1.3f) / 1.6f));
        if (est > 0f) {
            Graphics2D gf = (Graphics2D) g.create();
            comAlfa(gf, est);
            fundo(gf, w, h, seg);
            gf.dispose();
        }
        // logo: grande no centro, depois vai para o lugar do menu (alt 19u, topo 3.4u)
        float alt1 = 19f * u, y1 = 3.4f * u;
        float alt0 = alturaAbertura(h, u);
        float alt = alt0 + (alt1 - alt0) * mv;
        float cx = w / 2f, cy = h * 0.48f + (y1 + alt1 / 2f - h * 0.48f) * mv;
        int a0 = preparaAbertura(h, u);
        // pulinho do logo inteiro na batida
        float pulo = (t > BATE && t < BATE + 0.32f) ? 0.045f * (float) Math.sin(Math.PI * (t - BATE) / 0.32f) : 0f;
        float esc = alt / a0 * (1f + pulo);
        float lw = aberturaSem.getWidth() * esc, lh = aberturaSem.getHeight() * esc;
        float lx = cx - lw / 2f, ly = cy - lh / 2f;
        float bx1 = lx + Logo.BOLA_X * lw, by1 = ly + Logo.BOLA_Y * lh, tam1 = aberturaBola.getWidth() * esc;

        // refletor dourado atras do logo
        float luz = Animacao.suave(t / 0.5f) * (1f - mv);
        if (luz > 0f) {
            float r = 46f * u;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r, new float[] {0f, 0.45f, 1f},
                new Color[] {new Color(242, 193, 78, Math.round(70 * luz)), new Color(242, 193, 78, Math.round(22 * luz)), new Color(242, 193, 78, 0)}));
            g.fill(new java.awt.geom.Rectangle2D.Float(cx - r, cy - r, 2 * r, 2 * r));
            // poeira subindo devagar dentro da luz
            for (float[] q : POEIRA) {
                float py = ((q[1] - seg * q[3]) % 1f + 1f) % 1f;
                float px = q[0] + 0.012f * (float) Math.sin(seg * 0.9f + q[4]);
                float dx = (px - 0.5f) * w, dy = py * h - cy;
                float perto = Math.max(0f, 1f - (float) Math.sqrt(dx * dx + dy * dy) / r);
                int al = Math.round(150 * luz * perto * (0.5f + 0.5f * (float) Math.sin(seg * 2.1f + q[4])));
                if (al <= 2) continue;
                float tm = q[2] * u * 0.22f;
                g.setColor(new Color(255, 236, 190, al));
                g.fill(new java.awt.geom.Ellipse2D.Float(px * w - tm, py * h - tm, 2 * tm, 2 * tm));
            }
        }
        // flashes de camera espalhados pela arquibancada (depois da batida)
        java.util.Random fr = new java.util.Random(7);
        for (int i = 0; i < 18; i++) {
            float quando = BATE + 0.05f + fr.nextFloat() * 1.4f, fx = fr.nextFloat() * w, fy = h * (0.08f + fr.nextFloat() * 0.5f);
            float k = (t - quando) / 0.18f;
            float rr = (0.5f + fr.nextFloat()) * 2.2f * u;
            if (k < 0f || k > 1f) continue;
            float b = (float) Math.sin(k * Math.PI);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(fx, fy), rr, new float[] {0f, 1f},
                new Color[] {new Color(255, 255, 255, Math.round(200 * b)), new Color(255, 255, 255, 0)}));
            g.fill(new java.awt.geom.Ellipse2D.Float(fx - rr, fy - rr, 2 * rr, 2 * rr));
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // voo da bola: entra por baixo, a esquerda, numa curva ate o lugar dela no B
        float k = (t - 0.12f) / (BATE - 0.12f);
        if (k > 0f) {
            // rastros e perna do B vao aparecendo atras da bola (so a parte de cima do logo)
            float bxAgora = k >= 1f ? bx1 : vooX(k, w, bx1);
            Graphics2D gc = (Graphics2D) g2.create();
            gc.clip(new java.awt.geom.Rectangle2D.Float(lx, ly, Math.max(0f, bxAgora - lx), 0.62f * lh));
            gc.drawImage(aberturaSem, Math.round(lx), Math.round(ly), Math.round(lw), Math.round(lh), null);
            gc.dispose();
        }
        // depois da batida: o resto (B e nome) aparece, com a sombra
        float resto = Animacao.suave((t - BATE) / 0.28f);
        if (resto > 0f) {
            float sw = aberturaSombra.getWidth() * esc, sh = aberturaSombra.getHeight() * esc;
            comAlfa(g2, resto * 0.85f);
            g2.drawImage(aberturaSombra, Math.round(cx - sw / 2f), Math.round(cy - sh / 2f + 0.35f * u), Math.round(sw), Math.round(sh), null);
            comAlfa(g2, resto);
            g2.drawImage(aberturaSem, Math.round(lx), Math.round(ly), Math.round(lw), Math.round(lh), null);
            comAlfa(g2, 1f);
            float p = (t - 1.45f) / 0.85f;
            if (p >= 0f && p <= 1f)
                g2.drawImage(Logo.brilho(aberturaSem, Animacao.suave(p)), Math.round(lx), Math.round(ly), Math.round(lw), Math.round(lh), null);
        }
        if (k > 0f) {
            if (k < 1f) {
                // fantasmas atras da bola (rastro de velocidade)
                for (int i = 6; i >= 1; i--) {
                    float kk = k - i * 0.03f;
                    if (kk <= 0f) continue;
                    comAlfa(g2, 0.32f * (1f - i / 7f));
                    Logo.bola(g2, aberturaBola, vooX(kk, w, bx1), vooY(kk, h, by1), tam1 * (1.6f - 0.6f * voo(kk)), 8 * Math.PI * voo(kk));
                }
                comAlfa(g2, 1f);
                Logo.bola(g2, aberturaBola, vooX(k, w, bx1), vooY(k, h, by1), tam1 * (1.6f - 0.6f * voo(k)), 8 * Math.PI * voo(k));
            } else {
                // na batida a bola ainda balanca um pouco e para
                float d = t - BATE;
                double ang = 8 * Math.PI + 0.28 * Math.sin(d * 13f) * Math.exp(-d * 4f);
                Logo.bola(g2, aberturaBola, bx1, by1, tam1, ang);
            }
        }
        g2.dispose();
        // estouro de luz e onda na batida
        float d = (t - BATE) / 0.5f;
        if (d >= 0f && d <= 1f) {
            float rb = tam1 / 2f;
            float fl = (float) Math.max(0f, 1f - d * 2.2f);
            if (fl > 0f) {
                float rf = rb * 3.2f;
                g.setPaint(new RadialGradientPaint(new Point2D.Float(bx1, by1), rf, new float[] {0f, 1f},
                    new Color[] {new Color(255, 248, 225, Math.round(170 * fl)), new Color(255, 248, 225, 0)}));
                g.fill(new java.awt.geom.Ellipse2D.Float(bx1 - rf, by1 - rf, 2 * rf, 2 * rf));
            }
            float ro = rb * (1f + 2.6f * Animacao.suave(d));
            g.setStroke(new java.awt.BasicStroke(Math.max(1f, 0.35f * u * (1f - d))));
            g.setColor(Tema.alfa(Estreia.OURO, Math.round(200 * (1f - d))));
            g.draw(new java.awt.geom.Ellipse2D.Float(bx1 - ro, by1 - ro, 2 * ro, 2 * ro));
        }
        // filete dourado embaixo do logo, igual ao do menu
        float fe = Animacao.suave((t - 1.3f) / 0.8f);
        if (fe > 0f) {
            float lf = 16f * u * fe, yf = ly + lh + 1.6f * u;
            g.setPaint(new LinearGradientPaint(cx - lf / 2f, 0, cx + lf / 2f + 0.01f, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 0), Tema.alfa(Estreia.OURO, 170), Tema.alfa(Estreia.OURO, 0)}));
            g.fill(new java.awt.geom.Rectangle2D.Float(cx - lf / 2f, yf, lf, Math.max(1f, 0.12f * u)));
        }
        // o menu comeca a entrar enquanto o logo termina de chegar
        float sm = t - ABERTURA + ENTRA;
        if (sm > 0.4f) menu(g, u, h, sm);
        // aviso discreto de que da para pular
        float av = Animacao.suave((t - 0.4f) / 0.5f) * (1f - mv);
        if (av > 0f) {
            g.setFont(Estreia.texto(1.0f * u));
            String dica = "clique para pular";
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Tema.alfa(Estreia.BRANCO, Math.round(90 * av)));
            g.drawString(dica, w - 3f * u - fm.stringWidth(dica), h - 2.8f * u);
        }
    }

    // Depois da abertura o menu ja comecou a entrar no fim dela (ENTRA s antes): segue dali.
    private static final float ENTRA = 0.75f;

    private float segMenu(float ms) { return inicioMenu > inicio ? ms + ENTRA : ms; }

    // Depois da abertura o logo ja esta no lugar: o titulo pula a entrada e o primeiro brilho.
    private float segTitulo(float ms) { return inicioMenu > inicio ? ms + 3f : ms; }

    // Comeca devagar, acelera e chega devagar (sem tranco na saida).
    private static float vaiEVem(float k) {
        k = Math.max(0f, Math.min(1f, k));
        return k < 0.5f ? 4f * k * k * k : 1f - (float) Math.pow(-2f * k + 2f, 3) / 2f;
    }

    // Andamento do voo (0..1): rapido no comeco, ainda com velocidade na batida.
    private static float voo(float k) {
        k = Math.max(0f, Math.min(1f, k));
        return 1f - (1f - k) * (1f - k) * 0.85f - 0.15f * (1f - k);
    }

    // Curva do voo: do canto de baixo, a esquerda, ate (bx1, by1).
    private static float vooX(float k, int w, float bx1) {
        float s = voo(k), a = -0.08f * w, c = 0.16f * w;
        return (1 - s) * (1 - s) * a + 2 * (1 - s) * s * c + s * s * bx1;
    }

    private static float vooY(float k, int h, float by1) {
        float s = voo(k), a = 1.02f * h, c = by1 + 0.06f * h;
        return (1 - s) * (1 - s) * a + 2 * (1 - s) * s * c + s * s * by1;
    }

    // ------------------------------------------------------------- desenho

    // Entrada "sobe": some -> aparece subindo um pouco, como no modelo.
    private float entrada(float seg, float atraso, float duracao) {
        return Animacao.suave((seg - atraso) / duracao);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!pintou && inicioMenu > inicio) {
            // prepara o logo grande antes de comecar a contar, para o 1o quadro nao atrasar
            preparaAbertura(getHeight(), getWidth() / 100f);
            Estreia.foto();
        }
        long agora = System.currentTimeMillis();
        if (!pintou) {
            pintou = true;
            if (inicioMenu > inicio) inicioMenu = agora + Math.round(ABERTURA * 1000);
            if (som != null) som.toca(agora);
            if (inicioMenu > agora) aquece(getWidth(), getHeight());
        }
        float dt = Math.min(0.1f, (agora - ultimoQuadro) / 1000f);
        ultimoQuadro = agora;
        float seg = (agora - inicio) / 1000f;
        for (int i = 0; i < itens.size(); i++) {
            Item it = itens.get(i);
            float alvo = (i == selecionado) ? 1f : 0f;
            it.ativo += (alvo - it.ativo) * Math.min(1f, dt * 9f);
            if (Math.abs(alvo - it.ativo) < 0.002f) it.ativo = alvo;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            float u = w / 100f;

            if (agora < inicioMenu) {
                abertura(g2, w, h, u, seg, ABERTURA - (inicioMenu - agora) / 1000f);
                return;
            }
            float ms = (agora - inicioMenu) / 1000f;
            fundo(g2, w, h, seg);
            // depois da abertura o logo ja esta no lugar: sem entrada e sem brilho logo de cara
            titulo(g2, u, segTitulo(ms));
            menu(g2, u, h, segMenu(ms));
            rodape(g2, u, h, segMenu(ms));

            g2.setColor(new Color(255, 255, 255, 22));
            g2.drawRect(0, 0, w - 1, h - 1);
        } finally {
            g2.dispose();
        }
    }

    // Foto do estadio com zoom lento (26 s, vai e volta), veu escuro e faixa de luz.
    // A foto ampliada com o veu fica pronta numa imagem e so e refeita quando o zoom anda
    // um pouco (~0,5 px na borda); nos outros quadros e so copiada. O zoom e lento demais
    // para a diferenca aparecer, e o menu parado gasta bem menos processador.
    private BufferedImage fundoPronto;
    private float fundoT = -1f;

    private void fundo(Graphics2D g2, int w, int h, float seg) {
        float ciclo = (seg / 26f) % 2f;
        float t = ciclo < 1f ? ciclo : 2f - ciclo;
        t = t * t * (3f - 2f * t); // ease-in-out
        if (fundoPronto == null || fundoPronto.getWidth() != w || fundoPronto.getHeight() != h
                || Math.abs(t - fundoT) * Math.max(w, h) > 6f) {
            if (fundoPronto == null || fundoPronto.getWidth() != w || fundoPronto.getHeight() != h) {
                java.awt.GraphicsConfiguration gc = getGraphicsConfiguration();
                fundoPronto = gc != null ? gc.createCompatibleImage(w, h) : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            }
            Graphics2D gf = fundoPronto.createGraphics();
            gf.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gf.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gf.setColor(Estreia.NOITE);
            gf.fillRect(0, 0, w, h);
            BufferedImage f = Estreia.foto();
            double escala = 1.04 + 0.10 * t;
            double cobre = Math.max(w / (double) f.getWidth(), h / (double) f.getHeight());
            AffineTransform at = new AffineTransform();
            at.translate(w / 2.0 - 0.02 * t * w, h / 2.0 + 0.01 * t * h);
            at.scale(cobre * escala, cobre * escala);
            at.translate(-f.getWidth() / 2.0, -f.getHeight() / 2.0);
            gf.drawImage(f, at, null);
            if (veu == null || veu.getWidth() != w || veu.getHeight() != h) veu = veu(w, h);
            gf.drawImage(veu, 0, 0, null);
            gf.dispose();
            fundoT = t;
        }
        g2.drawImage(fundoPronto, 0, 0, null);

        // faixa de luz: espera 1,5 s, depois passa a cada 7 s
        float p = ((seg - 1.5f) % 7f) / 7f;
        if (seg > 1.5f && p > 0.55f) {
            float q = (p - 0.55f) / 0.45f;
            q = q * q * (3f - 2f * q);
            float x = (-0.30f + 1.50f * q) * w, lb = 0.30f * w;
            float inc = (float) Math.tan(Math.toRadians(12)) * h * 1.4f;
            Path2D faixa = new Path2D.Float();
            faixa.moveTo(x + inc / 2, -0.2f * h);
            faixa.lineTo(x + lb + inc / 2, -0.2f * h);
            faixa.lineTo(x + lb - inc / 2, 1.2f * h);
            faixa.lineTo(x - inc / 2, 1.2f * h);
            faixa.closePath();
            g2.setPaint(new LinearGradientPaint(x, 0, x + lb, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, 18), new Color(255, 255, 255, 0)}));
            g2.fill(faixa);
        }
    }

    private static BufferedImage veu(int w, int h) {
        BufferedImage v = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = v.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // escuro no meio (logo e menu centrados) e um pouco mais nas bordas
        g.setColor(new Color(6, 9, 8, 196));
        g.fillRect(0, 0, w, h);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h * 0.55f), Math.max(w, h) * 0.75f,
            new float[] {0f, 1f}, new Color[] {new Color(6, 9, 8, 0), new Color(6, 9, 8, 120)}));
        g.fillRect(0, 0, w, h);
        // brilho dourado de refletor no alto, atras do logo
        float rx = 0.6f * w * 0.7f, ry = 0.5f * h * 0.7f;
        AffineTransform elipse = new AffineTransform();
        elipse.translate(0.50f * w, 0.02f * h);
        elipse.scale(1.0, ry / rx);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), rx, new Point2D.Float(0, 0), new float[] {0f, 1f},
            new Color[] {new Color(242, 193, 78, 46), new Color(242, 193, 78, 0)},
            MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, elipse));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return v;
    }

    private static void comAlfa(Graphics2D g2, float a) {
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
    }

    // Logo do Leo (B com bola + BROMBILA BRASFOOT) no lugar do titulo em texto.
    private BufferedImage logoCache, sombraCache, logoSem, logoBola;
    // verde do logo na barrinha e no icone do item selecionado (o resto do jogo segue dourado)
    static final Color ACENTO = new Color(0x4CD93F);

    private synchronized void preparaTitulo(float u) {
        BufferedImage lo = Logo.branco();
        int alt = Math.round(19f * u);
        int larg = Math.round(alt * lo.getWidth() / (float) lo.getHeight());
        if (logoCache == null || logoCache.getHeight() != alt) {
            BufferedImage cheio = Logo.reduz(lo, larg, alt);
            sombraCache = Logo.sombra(cheio, Math.max(2, Math.round(0.9f * u)));
            BufferedImage[] p = Logo.separa(cheio);
            logoSem = p[0];
            logoBola = p[1];
            logoCache = cheio;
        }
    }

    // Durante a abertura, prepara numa thread o que o primeiro quadro do menu teria que
    // fazer (logo reduzido, sombra, letras e icones); sem isso a passagem para o menu da
    // um tranco de ~0,2 s.
    private void aquece(final int w, final int h) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    float u = w / 100f;
                    preparaTitulo(u);
                    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = img.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    menu(g, u, h, 5f);
                    rodape(g, u, h, 5f);
                    g.dispose();
                } catch (Throwable x) {
                    // so perde o aquecimento
                }
            }
        }, "brombila-menu");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void titulo(Graphics2D g, float u, float seg) {
        float e = entrada(seg, 0.2f, 1.1f);
        if (e <= 0f) return;
        preparaTitulo(u);
        int alt = logoCache.getHeight(), larg = logoCache.getWidth();
        int w = getWidth();
        float x = (w - larg) / 2f, y = 3.4f * u + (1f - e) * 1.2f * u;
        Graphics2D g2 = (Graphics2D) g.create();
        comAlfa(g2, e * 0.85f);
        int m = (sombraCache.getWidth() - larg) / 2;
        g2.drawImage(sombraCache, Math.round(x) - m, Math.round(y + 0.35f * u) - m, null);
        comAlfa(g2, e);
        // brilho que atravessa o logo: na entrada e depois a cada 9 s; junto a bola da uma volta
        float p = seg < 1.2f ? -1f : ((seg - 1.2f) % 9f) / 1.6f;
        if (p >= 0f && p <= 1f) {
            g2.drawImage(logoSem, Math.round(x), Math.round(y), null);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            float q = p * p * (3f - 2f * p);
            Logo.bola(g2, logoBola, Math.round(x) + Logo.BOLA_X * larg, Math.round(y) + Logo.BOLA_Y * alt, logoBola.getWidth(), 2 * Math.PI * q);
            g2.drawImage(Logo.brilho(logoSem, Animacao.suave(p)), Math.round(x), Math.round(y), null);
        } else {
            g2.drawImage(logoCache, Math.round(x), Math.round(y), null);
        }
        g2.dispose();
        // filete dourado embaixo do logo
        float fe = entrada(seg, 0.7f, 0.9f);
        if (fe > 0f) {
            Graphics2D g3 = (Graphics2D) g.create();
            float lf = 16f * u * Animacao.suave(fe), yf = y + alt + 1.6f * u;
            g3.setPaint(new LinearGradientPaint(w / 2f - lf / 2f, 0, w / 2f + lf / 2f + 0.01f, 0, new float[] {0f, 0.5f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 0), Tema.alfa(Estreia.OURO, 170), Tema.alfa(Estreia.OURO, 0)}));
            g3.fill(new java.awt.geom.Rectangle2D.Float(w / 2f - lf / 2f, yf, lf, Math.max(1f, 0.12f * u)));
            g3.dispose();
        }
    }

    private void menu(Graphics2D g, float u, int h, float seg) {
        Font fNome = Estreia.semi(1.75f * u);
        Font fDica = Estreia.texto(1.05f * u);
        FontMetrics fm = g.getFontMetrics(fNome);
        float alt = fm.getHeight() + 1.6f * u, gap = 0.3f * u;
        float total = itens.size() * alt + (itens.size() - 1) * gap;
        float y = h - 8f * u - total;
        int w = getWidth();
        float largura = 30f * u;
        for (int i = 0; i < itens.size(); i++) {
            Item it = itens.get(i);
            float top = y + i * (alt + gap);
            float lado = 2f * u;
            float grupo = lado + 1.6f * u + fm.stringWidth(it.nome);
            float x0 = (w - grupo) / 2f;
            areas[i] = new Rectangle(Math.round((w - largura) / 2f), Math.round(top), Math.round(largura), Math.round(alt));
            float e = entrada(seg, 0.45f + 0.08f * i, 0.7f);
            if (e <= 0f) continue;
            float a = Animacao.suave(it.ativo);
            Graphics2D g2 = (Graphics2D) g.create();
            comAlfa(g2, e);
            g2.translate(0, (1f - e) * 1.4f * u);
            float meio = top + alt / 2f;
            // sublinhado verde que abre do centro no item selecionado
            float sl = grupo * a;
            if (sl > 1f) {
                g2.setColor(ACENTO);
                g2.fill(new java.awt.geom.Rectangle2D.Float(w / 2f - sl / 2f, meio + fm.getHeight() / 2f + 0.15f * u, sl, Math.max(1.5f, 0.18f * u)));
            }
            Color cor = Tema.mistura(Estreia.FRACO, Estreia.BRANCO, a);
            Estreia.icone(g2, it.icone, x0, meio - lado / 2f, lado, Tema.mistura(Estreia.FRACO, ACENTO, a));
            g2.setFont(fNome);
            g2.setColor(cor);
            g2.drawString(it.nome, x0 + lado + 1.6f * u, meio - fm.getHeight() / 2f + fm.getAscent());
            if (a > 0.01f) {
                // dica do item selecionado num balaozinho logo a direita dele, com a
                // ponta virada para o item; entra deslizando junto com o sublinhado
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setFont(fDica);
                FontMetrics fd = g2.getFontMetrics();
                float pad = 0.9f * u, bh = fd.getHeight() + 0.8f * u, bw = fd.stringWidth(it.dica) + 2f * pad;
                float bx = x0 + grupo + 2.2f * u - (1f - a) * 0.8f * u, by = meio - bh / 2f, ponta = 0.55f * u;
                Graphics2D gd = (Graphics2D) g2.create();
                comAlfa(gd, e * a);
                java.awt.geom.Path2D balao = new java.awt.geom.Path2D.Float();
                balao.append(new java.awt.geom.RoundRectangle2D.Float(bx, by, bw, bh, bh, bh), false);
                java.awt.geom.Path2D seta = new java.awt.geom.Path2D.Float();
                seta.moveTo(bx + 0.3f * u, meio - ponta);
                seta.lineTo(bx - ponta + 0.1f * u, meio);
                seta.lineTo(bx + 0.3f * u, meio + ponta);
                seta.closePath();
                java.awt.geom.Area forma = new java.awt.geom.Area(balao);
                forma.add(new java.awt.geom.Area(seta));
                gd.setColor(new Color(12, 16, 14, 215));
                gd.fill(forma);
                gd.setColor(Tema.alfa(ACENTO, 120));
                gd.setStroke(new java.awt.BasicStroke(Math.max(1f, 0.08f * u)));
                gd.draw(forma);
                gd.setColor(Tema.alfa(Estreia.BRANCO, 225));
                gd.drawString(it.dica, bx + pad, meio - fd.getHeight() / 2f + fd.getAscent());
                gd.dispose();
            }
            g2.dispose();
        }
    }

    private void rodape(Graphics2D g, float u, int h, float seg) {
        float e = entrada(seg, 0.9f, 0.8f);
        if (e <= 0f) return;
        Graphics2D g2 = (Graphics2D) g.create();
        comAlfa(g2, e);
        g2.translate(0, (1f - e) * 1.4f * u);
        Font f1 = Estreia.texto(1.05f * u), f2 = Estreia.semi(1.05f * u);
        float base = h - 2.8f * u;
        g2.setFont(f1);
        g2.setColor(Estreia.FRACO);
        g2.drawString("versão:", 6.5f * u, base);
        float x = 6.5f * u + g2.getFontMetrics().stringWidth("versão:") + 0.5f * u;
        g2.setFont(f2);
        g2.setColor(Estreia.BRANCO);
        g2.drawString(VERSAO, x, base);
        g2.dispose();
    }
}
