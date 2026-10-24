package mod;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Cartao "Proximo jogo" da tela principal: quando o adversario muda (depois da rodada), o
// cartao vira como uma carta, mostrando o adversario antigo na frente e o novo no verso.
// O jogo cria a tela principal de novo a cada rodada, entao o retrato do cartao antigo
// fica guardado aqui.
final class Virada {
    private Virada() {}

    private static String ultimoRival;
    private static BufferedImage ultimaFoto;
    private static final int ESPERA = 260, MS = 560;

    // area do cartao dentro do painel do time (Principal.painelDoTime)
    static final Rectangle AREA = new Rectangle(8, 198, 0, 96);

    static void liga(final JPanel painel, final Object rival) {
        if (!(rival instanceof JLabel)) return;
        // o jogo poe o nome do adversario depois de mostrar a tela (eg.on): espera uma volta
        final Runnable vai = new Runnable() {
            public void run() { if (painel.isShowing()) comeca(painel, (JLabel) rival); }
        };
        if (painel.isShowing()) {
            SwingUtilities.invokeLater(vai);
            return;
        }
        painel.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0 || !painel.isShowing()) return;
                painel.removeHierarchyListener(this);
                SwingUtilities.invokeLater(vai);
            }
        });
    }

    private static Rectangle area(JPanel painel) {
        return new Rectangle(AREA.x, AREA.y, painel.getWidth() - 2 * AREA.x, Math.min(AREA.height, painel.getHeight() - AREA.y - 3));
    }

    // retrato da area do cartao
    private static BufferedImage foto(JPanel painel) {
        Rectangle a = area(painel);
        if (a.width <= 0 || painel.getHeight() < a.y + a.height) return null;
        BufferedImage img = new BufferedImage(a.width, a.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.translate(-a.x, -a.y);
        painel.paint(g);
        g.dispose();
        return img;
    }

    // fundo do cartao sem o conteudo: a tela de verdade naquele ponto, com os textos e o
    // escudo do cartao escondidos so durante o retrato
    private static BufferedImage fundo(JPanel painel) {
        Rectangle a = area(painel);
        JRootPane rp = SwingUtilities.getRootPane(painel);
        if (rp == null || a.width <= 0 || a.height <= 0) return null;
        java.util.List<java.awt.Component> escondidos = new java.util.ArrayList<java.awt.Component>();
        for (java.awt.Component k : painel.getComponents()) {
            if (k.isVisible() && k.getBounds().intersects(a)) {
                k.setVisible(false);
                escondidos.add(k);
            }
        }
        BufferedImage img = new BufferedImage(a.width, a.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Point o = SwingUtilities.convertPoint(painel, a.x, a.y, rp.getContentPane());
        g.translate(-o.x, -o.y);
        g.clipRect(o.x, o.y, a.width, a.height);
        rp.getContentPane().paint(g);
        g.dispose();
        for (java.awt.Component k : escondidos) k.setVisible(true);
        return img;
    }

    private static void comeca(final JPanel painel, final JLabel rival) {
        final String nome = rival.getText();
        final BufferedImage antiga = ultimaFoto;
        boolean vira = antiga != null && ultimoRival != null && nome != null && !nome.equals(ultimoRival) && Animacao.ligada();
        ultimoRival = nome;
        JRootPane rp = SwingUtilities.getRootPane(painel);
        if (!vira || rp == null) {
            // so guarda o retrato quando o tema ja vestiu o cartao
            Timer t = new Timer(400, new ActionListener() {
                public void actionPerformed(ActionEvent e) { if (painel.isShowing()) ultimaFoto = foto(painel); }
            });
            t.setRepeats(false);
            t.start();
            return;
        }
        final JLayeredPane camadas = rp.getLayeredPane();
        final BufferedImage[] nova = {null}, fundo = {null};
        final long[] inicio = {0};
        final boolean[] refeita = {false}, tocou = {false};
        final JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Rectangle a = area(painel);
                Point o = SwingUtilities.convertPoint(painel, a.x, a.y, this);
                if (nova[0] == null) {
                    nova[0] = foto(painel);
                    fundo[0] = fundo(painel);
                    inicio[0] = System.currentTimeMillis() + ESPERA;
                }
                float t = Math.max(0f, (System.currentTimeMillis() - inicio[0]) / (float) MS);
                if (t > 0f && !tocou[0]) {
                    tocou[0] = true;
                    Sons.toca("carta");
                }
                // o verso e fotografado de novo na hora de aparecer: no comeco a tela ainda
                // esta entrando e o cartao sai mais claro
                if (t >= 0.5f && !refeita[0]) {
                    refeita[0] = true;
                    BufferedImage f = foto(painel);
                    if (f != null) nova[0] = f;
                }
                BufferedImage img = t < 0.5f ? antiga : nova[0];
                if (img == null) return;
                // a carta gira no eixo vertical: encolhe na largura ate a metade e abre do outro lado
                double k = Math.abs(Math.cos(Math.PI * Math.min(1f, t)));
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                // por baixo, o fundo do cartao (sem o conteudo) para nao aparecer o cartao novo
                g.translate(o.x, o.y);
                if (fundo[0] != null) g.drawImage(fundo[0], 0, 0, null);
                double cx = a.width / 2.0;
                g.translate(cx, 0);
                g.scale(Math.max(0.02, k), 1 + 0.06 * Math.sin(Math.PI * Math.min(1f, t)));
                g.drawImage(img, (int) -cx, 0, null);
                g.dispose();
            }
        };
        c.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        // mesma camada do esmaecer da troca de tela, por baixo dele (entra depois)
        camadas.add(c, JLayeredPane.DRAG_LAYER);
        final Timer timer = new Timer(Animacao.QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean fim = inicio[0] != 0 && System.currentTimeMillis() - inicio[0] > MS;
                if (fim || !painel.isShowing()) {
                    timer.stop();
                    camadas.remove(c);
                    camadas.repaint();
                    if (nova[0] != null) ultimaFoto = nova[0];
                    return;
                }
                Rectangle a = area(painel);
                Point o = SwingUtilities.convertPoint(painel, a.x, a.y, camadas);
                camadas.repaint(o.x - 4, o.y - 8, a.width + 8, a.height + 16);
            }
        });
        timer.start();
    }
}
