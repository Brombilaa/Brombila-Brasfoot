package mod;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferStrategy;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Aviso "Carregando" quando o jogo trava a tela (o jogo faz contas pesadas na
// thread da interface e a janela para de responder). Um Timer bate a cada 100 ms
// na thread da interface; se ela fica mais de ATRASO ms sem bater, outra thread
// cobre a janela do jogo inteira com uma janela propria, desenhada por ela mesma
// (sem depender do Swing): a imagem da tela escurecida e desfocada, com o cartao
// no centro. Assim o mouse fica sempre em cima de uma janela que responde e o
// cursor azul de espera do Windows nao aparece. Quando a interface volta, tudo some.
final class Carregando {
    private Carregando() {}

    static final int ATRASO = 450, MINIMO = 350;
    static final int LARG = 330, ALT = 104, CANTO = 30;
    static final Color VERDE = new Color(0x4CD93F);

    private static volatile long batida = System.currentTimeMillis();
    private static volatile Rectangle alvo;
    private static Window janela;
    private static Canvas tela;

    static void liga() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { monta(); }
        });
    }

    private static void monta() {
        janela = new Window((Frame) null);
        janela.setAlwaysOnTop(true);
        janela.setFocusableWindowState(false);
        janela.setBackground(Estreia.NOITE);
        janela.setSize(LARG, ALT);
        janela.setCursor(java.awt.Cursor.getDefaultCursor());
        tela = new Canvas();
        tela.setCursor(java.awt.Cursor.getDefaultCursor());
        tela.setBackground(Estreia.NOITE);
        tela.setIgnoreRepaint(true);
        janela.add(tela);

        new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                batida = System.currentTimeMillis();
                Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                if (w == janela) w = null;
                if (w == null || !w.isShowing()) {
                    for (Window x : Window.getWindows()) if (x != janela && x.isShowing() && x instanceof Frame) w = x;
                }
                if (w != null && w.isShowing()) {
                    Point p = w.getLocationOnScreen();
                    alvo = new Rectangle(p.x, p.y, w.getWidth(), w.getHeight());
                } else {
                    alvo = null;
                }
            }
        }).start();

        Thread vigia = new Thread(new Runnable() {
            public void run() { vigia(); }
        }, "brombila-carregando");
        vigia.setDaemon(true);
        vigia.start();
    }

    private static void vigia() {
        while (true) {
            try {
                Thread.sleep(60);
                if (System.currentTimeMillis() - batida < ATRASO || alvo == null) continue;
                mostra();
            } catch (Throwable t) {
                // tenta de novo na proxima volta
            }
        }
    }

    private static java.awt.Robot robo;

    private static void mostra() throws InterruptedException {
        Rectangle a = alvo;
        // foto da janela do jogo do jeito que ela esta, para servir de fundo
        java.awt.image.BufferedImage foto = null;
        try {
            if (robo == null) robo = new java.awt.Robot();
            foto = robo.createScreenCapture(a);
        } catch (Throwable t) {
            // sem foto: fundo escuro liso
        }
        java.awt.image.BufferedImage veu = veu(foto, a.width, a.height);
        janela.setBounds(a);
        janela.setVisible(true);
        if (tela.getBufferStrategy() == null) tela.createBufferStrategy(2);
        BufferStrategy bs = tela.getBufferStrategy();
        long ini = System.currentTimeMillis();
        while (System.currentTimeMillis() - batida >= ATRASO || System.currentTimeMillis() - ini < MINIMO) {
            float t = (System.currentTimeMillis() - ini) / 1000f;
            float ent = suave(Math.min(1f, t / 0.25f));
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
                    if (foto != null) g.drawImage(foto, 0, 0, null);
                    else {
                        g.setColor(Estreia.NOITE);
                        g.fillRect(0, 0, a.width, a.height);
                    }
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ent));
                    g.drawImage(veu, 0, 0, null);
                    g.setComposite(AlphaComposite.SrcOver);
                    g.translate((a.width - LARG) / 2, (a.height - ALT) / 2);
                    // cartao sobe um pouco ao entrar
                    g.translate(0, Math.round(10 * (1f - ent)));
                    desenha(g, t);
                } finally {
                    g.dispose();
                }
                bs.show();
            } while (bs.contentsLost());
            java.awt.Toolkit.getDefaultToolkit().sync();
            Thread.sleep(16);
        }
        janela.setVisible(false);
        if (System.getProperty("brombila.travas") != null)
            System.err.println("TRAVA " + (System.currentTimeMillis() - ini + ATRASO) + " ms");
    }

    // A foto da tela desfocada (reduz e amplia) e escurecida, com um brilho dourado
    // fraco atras do cartao.
    private static java.awt.image.BufferedImage veu(java.awt.image.BufferedImage foto, int w, int h) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Estreia.NOITE);
        g.fillRect(0, 0, w, h);
        if (foto != null) {
            int pw = Math.max(1, w / 10), ph = Math.max(1, h / 10);
            java.awt.image.BufferedImage p = Logo.reduz(foto, pw, ph);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(p, 0, 0, w, h, null);
        }
        g.setColor(new Color(6, 9, 8, 168));
        g.fillRect(0, 0, w, h);
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.6f,
            new float[] {0f, 1f}, new Color[] {new Color(242, 193, 78, 22), new Color(0, 0, 0, 70)}));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    static void desenha(Graphics2D g, float t) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        float ent = suave(Math.min(1f, t / 0.3f));
        RoundRectangle2D cartao = new RoundRectangle2D.Float(0, 0, LARG, ALT, CANTO, CANTO);
        for (int k = 6; k >= 1; k--) {
            g.setColor(new Color(0, 0, 0, 14));
            g.fill(new RoundRectangle2D.Float(-k * 2, -k * 2 + 6, LARG + k * 4, ALT + k * 4, CANTO + k * 4, CANTO + k * 4));
        }
        g = (Graphics2D) g.create();
        g.setColor(Estreia.NOITE);
        g.fill(cartao);
        g.clip(cartao);
        g.drawImage(fundo(), 0, 0, null);
        g.setColor(new Color(255, 255, 255, 34));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, LARG - 1, ALT - 1, CANTO - 1, CANTO - 1));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ent));

        // ---- bola quicando no gramado (quique de 0,8 s, com amassado no chao)
        float chao = ALT - 24, cx = 58, r = 15;
        float ciclo = 0.8f, f = (t % ciclo) / ciclo;
        float subida = 1f - (2f * f - 1f) * (2f * f - 1f);       // parabola 0..1..0
        float altura = 34f * subida;
        float toque = Math.max(0f, 1f - Math.min(f, 1f - f) * 2f / 0.12f); // perto do chao
        float amassa = 0.18f * toque;
        // linha do gramado que some nas pontas
        g.setPaint(new java.awt.LinearGradientPaint(cx - 38, 0, cx + 38, 0, new float[] {0f, 0.5f, 1f},
            new Color[] {new Color(76, 217, 63, 0), new Color(76, 217, 63, 150), new Color(76, 217, 63, 0)}));
        g.fill(new java.awt.geom.Rectangle2D.Float(cx - 38, chao, 76, 1.5f));
        // sombra que encolhe quando a bola sobe
        float sl = r * (1.5f - 0.7f * subida), sa = 0.55f - 0.4f * subida;
        g.setColor(new Color(0f, 0f, 0f, Math.max(0f, sa)));
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - sl, chao - 2.5f, 2 * sl, 5f));
        // rastro de quadros anteriores
        for (int k = 3; k >= 1; k--) {
            float fk = ((t - k * 0.035f) % ciclo + ciclo) % ciclo / ciclo;
            float hk = 34f * (1f - (2f * fk - 1f) * (2f * fk - 1f));
            g.setColor(new Color(76, 217, 63, 26 - 6 * k));
            g.fill(new java.awt.geom.Ellipse2D.Float(cx - r * 0.8f, chao - r - hk - r * 0.8f + 2, r * 1.6f, r * 1.6f));
        }
        Graphics2D gb = (Graphics2D) g.create();
        gb.translate(cx, chao);
        gb.scale(1f + amassa, 1f - amassa);
        Bola.desenha(gb, 0, -r - altura / (1f - amassa), r, t * 7.0);
        gb.dispose();

        // ---- texto com as letras em onda
        float tx = 108, base = ALT / 2f + 2;
        g.setFont(Estreia.titulo(30f));
        FontMetrics m = g.getFontMetrics();
        String txt = "CARREGANDO";
        float x = tx;
        for (int i = 0; i < txt.length(); i++) {
            String c = txt.substring(i, i + 1);
            float onda = (float) Math.sin(t * 5.0 - i * 0.55);
            float dy = -2.2f * Math.max(0f, onda);
            g.setColor(Tema.mistura(Estreia.BRANCO, VERDE, 0.55f * Math.max(0f, onda) * Math.max(0f, onda)));
            g.drawString(c, x, base + dy);
            x += m.stringWidth(c) + 1.2f;
        }
        // trilho com um cometa verde que vai e volta
        float tw = x - tx - 1.2f, ty = base + 12;
        g.setColor(new Color(255, 255, 255, 22));
        g.fill(new RoundRectangle2D.Float(tx, ty, tw, 3, 3, 3));
        float v = suave((float) (0.5 - 0.5 * Math.cos(t * 2.2)));
        float cl = tw * 0.32f, cxm = tx + v * (tw - cl);
        g.setPaint(new java.awt.LinearGradientPaint(cxm, 0, cxm + cl, 0, new float[] {0f, 0.6f, 1f},
            new Color[] {new Color(76, 217, 63, 0), VERDE, new Color(200, 255, 190)}));
        g.fill(new RoundRectangle2D.Float(cxm, ty, cl, 3, 3, 3));
        g.setFont(Estreia.texto(11f));
        g.setColor(Estreia.FRACO);
        g.drawString("aguarde um instante", tx, ty + 18);
        g.dispose();
    }

    private static java.awt.image.BufferedImage fundo;

    // Fundo igual ao do menu: estadio escurecido, brilho dourado de refletor no
    // alto e uma luz verde fraca atras da bola. Feito uma vez so.
    static synchronized java.awt.image.BufferedImage fundo() {
        if (fundo != null) return fundo;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(LARG, ALT, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Estreia.NOITE);
        g.fillRect(0, 0, LARG, ALT);
        try {
            java.awt.image.BufferedImage f = Estreia.foto();
            // pedaco da arquibancada iluminada, como no alto do menu
            double esc = LARG * 1.6 / f.getWidth();
            int fw = (int) Math.round(f.getWidth() * esc), fh = (int) Math.round(f.getHeight() * esc);
            g.drawImage(Logo.reduz(f, fw, fh), (LARG - fw) / 2, -(int) (fh * 0.18), null);
        } catch (Throwable t) {
            // so a cor de fundo
        }
        g.setColor(new Color(6, 9, 8, 205));
        g.fillRect(0, 0, LARG, ALT);
        java.awt.geom.AffineTransform el = new java.awt.geom.AffineTransform();
        el.translate(LARG * 0.62, -ALT * 0.15);
        el.scale(1.0, 0.45);
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(0, 0), LARG * 0.6f,
            new java.awt.geom.Point2D.Float(0, 0), new float[] {0f, 1f},
            new Color[] {new Color(242, 193, 78, 60), new Color(242, 193, 78, 0)},
            java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE, java.awt.MultipleGradientPaint.ColorSpaceType.SRGB, el));
        g.fillRect(0, 0, LARG, ALT);
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(58, ALT - 18), 70,
            new float[] {0f, 1f}, new Color[] {new Color(76, 217, 63, 30), new Color(76, 217, 63, 0)}));
        g.fillRect(0, 0, LARG, ALT);
        // filete dourado no alto, como o do logo no menu
        g.setPaint(new java.awt.LinearGradientPaint(LARG * 0.2f, 0, LARG * 0.8f, 0, new float[] {0f, 0.5f, 1f},
            new Color[] {Tema.alfa(Estreia.OURO, 0), Tema.alfa(Estreia.OURO, 150), Tema.alfa(Estreia.OURO, 0)}));
        g.fillRect((int) (LARG * 0.2f), 1, (int) (LARG * 0.6f), 1);
        g.dispose();
        fundo = img;
        return img;
    }

    private static float suave(float x) {
        x = Math.max(0f, Math.min(1f, x));
        return x * x * (3f - 2f * x);
    }
}
