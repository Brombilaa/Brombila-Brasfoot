package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
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
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Aviso "Carregando" quando o jogo trava a tela (o jogo faz contas pesadas na
// thread da interface e a janela para de responder). Um Timer bate a cada 100 ms
// na thread da interface; se ela fica mais de ATRASO ms sem bater, outra thread
// mostra um cartao proprio, desenhado por ela mesma (sem depender do Swing),
// no centro da janela do jogo. Quando a interface volta, o cartao some.
final class Carregando {
    private Carregando() {}

    static final int ATRASO = 450, MINIMO = 350;
    static final int LARG = 300, ALT = 96;
    static final Color VERDE = new Color(0x4CD93F);

    private static volatile long batida = System.currentTimeMillis();
    private static volatile Rectangle alvo;
    private static Window janela;
    private static Canvas tela;
    private static BufferedImage logo;

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
        try {
            janela.setShape(new RoundRectangle2D.Float(0, 0, LARG, ALT, 22, 22));
        } catch (Throwable t) {
            // sem cantos redondos
        }
        tela = new Canvas();
        tela.setBackground(Estreia.NOITE);
        tela.setIgnoreRepaint(true);
        janela.add(tela);
        try {
            BufferedImage b = Logo.branco();
            int h = 52, w = Math.round(h * b.getWidth() / (float) b.getHeight());
            logo = Logo.reduz(b, w, h);
        } catch (Throwable t) {
            logo = null;
        }

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

    private static void mostra() throws InterruptedException {
        Rectangle a = alvo;
        janela.setLocation(a.x + (a.width - LARG) / 2, a.y + (a.height - ALT) / 2);
        janela.setVisible(true);
        if (tela.getBufferStrategy() == null) tela.createBufferStrategy(2);
        BufferStrategy bs = tela.getBufferStrategy();
        long ini = System.currentTimeMillis();
        while (System.currentTimeMillis() - batida >= ATRASO || System.currentTimeMillis() - ini < MINIMO) {
            float t = (System.currentTimeMillis() - ini) / 1000f;
            do {
                Graphics2D g = (Graphics2D) bs.getDrawGraphics();
                try {
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
    }

    static void desenha(Graphics2D g, float t) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        float ent = Math.min(1f, t / 0.25f);
        ent = ent * ent * (3f - 2f * ent);
        g.setColor(Estreia.NOITE);
        g.fillRect(0, 0, LARG, ALT);
        // borda fina clara
        g.setColor(new Color(255, 255, 255, 30));
        g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, LARG - 1, ALT - 1, 22, 22));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ent));

        // logo branco a esquerda
        int x = 22;
        if (logo != null) {
            g.drawImage(logo, x, (ALT - logo.getHeight()) / 2, null);
            x += logo.getWidth() + 18;
            g.setColor(new Color(255, 255, 255, 26));
            g.fillRect(x - 9, 24, 1, ALT - 48);
        }

        // anel girando
        float cx = x + 16, cy = ALT / 2f, r = 13;
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, 28));
        g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 0, 360, Arc2D.OPEN));
        float giro = t * 300f % 360f;
        float abre = 70f + 110f * (0.5f + 0.5f * (float) Math.sin(t * 3.2f));
        g.setColor(VERDE);
        g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, -giro, abre, Arc2D.OPEN));

        // texto
        float tx = cx + r + 16;
        Font f1 = Estreia.semi(15f), f2 = Estreia.texto(11.5f);
        g.setFont(f1);
        FontMetrics m1 = g.getFontMetrics();
        g.setColor(Estreia.BRANCO);
        g.drawString("Carregando", tx, cy - 3);
        int pontos = ((int) (t * 3f)) % 4;
        g.setColor(VERDE);
        g.drawString("...".substring(0, pontos), tx + m1.stringWidth("Carregando"), cy - 3);
        g.setFont(f2);
        FontMetrics m2 = g.getFontMetrics();
        g.setColor(Estreia.FRACO);
        g.drawString("aguarde um instante", tx, cy + 3 + m2.getAscent());
        g.setComposite(AlphaComposite.SrcOver);
    }
}
