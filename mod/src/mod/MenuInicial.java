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
import java.lang.reflect.Field;
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
    private static final String VERSAO = "22.23";

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
    private long ultimoQuadro = inicio;
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
            gq.setLocationRelativeTo(null);
            gq.validate();
            m.requestFocusInWindow();
        } catch (Throwable t) {
            t.printStackTrace();
            try { gq.setOpacity(1f); } catch (Throwable x) {}
        }
    }

    private static JLabel rotulo(Object dono, String campo) {
        try {
            Field f = dono.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            return (JLabel) f.get(dono);
        } catch (Throwable t) {
            return null;
        }
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
            public void actionPerformed(ActionEvent e) { repaint(); }
        });

        MouseAdapter mouse = new MouseAdapter() {
            public void mouseMoved(MouseEvent e) {
                int i = itemEm(e.getX(), e.getY());
                if (i >= 0) selecionado = i;
                setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
            public void mouseClicked(MouseEvent e) {
                int i = itemEm(e.getX(), e.getY());
                if (i >= 0 && SwingUtilities.isLeftMouseButton(e)) aciona(i);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int n = itens.size();
                if (e.getKeyCode() == KeyEvent.VK_DOWN) selecionado = (selecionado + 1) % n;
                else if (e.getKeyCode() == KeyEvent.VK_UP) selecionado = (selecionado + n - 1) % n;
                else if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) aciona(selecionado);
            }
        });
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

    // ------------------------------------------------------------- desenho

    // Entrada "sobe": some -> aparece subindo um pouco, como no modelo.
    private float entrada(float seg, float atraso, float duracao) {
        return Animacao.suave((seg - atraso) / duracao);
    }

    @Override
    protected void paintComponent(Graphics g) {
        long agora = System.currentTimeMillis();
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

            fundo(g2, w, h, seg);
            titulo(g2, u, seg);
            menu(g2, u, h, seg);
            rodape(g2, u, h, seg);

            g2.setColor(new Color(255, 255, 255, 22));
            g2.drawRect(0, 0, w - 1, h - 1);
        } finally {
            g2.dispose();
        }
    }

    // Foto do estadio com zoom lento (26 s, vai e volta), veu escuro e faixa de luz.
    private void fundo(Graphics2D g2, int w, int h, float seg) {
        g2.setColor(Estreia.NOITE);
        g2.fillRect(0, 0, w, h);
        BufferedImage f = Estreia.foto();
        float ciclo = (seg / 26f) % 2f;
        float t = ciclo < 1f ? ciclo : 2f - ciclo;
        t = t * t * (3f - 2f * t); // ease-in-out
        double escala = 1.04 + 0.10 * t;
        double cobre = Math.max(w / (double) f.getWidth(), h / (double) f.getHeight());
        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0 - 0.02 * t * w, h / 2.0 + 0.01 * t * h);
        at.scale(cobre * escala, cobre * escala);
        at.translate(-f.getWidth() / 2.0, -f.getHeight() / 2.0);
        g2.drawImage(f, at, null);

        if (veu == null || veu.getWidth() != w || veu.getHeight() != h) veu = veu(w, h);
        g2.drawImage(veu, 0, 0, null);

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
    private BufferedImage logoCache, sombraCache;
    // verde do logo na barrinha e no icone do item selecionado (o resto do jogo segue dourado)
    static final Color ACENTO = new Color(0x4CD93F);

    private void titulo(Graphics2D g, float u, float seg) {
        float e = entrada(seg, 0.2f, 1.1f);
        if (e <= 0f) return;
        BufferedImage lo = Logo.branco();
        int alt = Math.round(19f * u);
        int larg = Math.round(alt * lo.getWidth() / (float) lo.getHeight());
        if (logoCache == null || logoCache.getHeight() != alt) {
            logoCache = Logo.reduz(lo, larg, alt);
            sombraCache = Logo.sombra(logoCache, Math.max(2, Math.round(0.9f * u)));
        }
        int w = getWidth();
        float x = (w - larg) / 2f, y = 3.4f * u + (1f - e) * 1.2f * u;
        Graphics2D g2 = (Graphics2D) g.create();
        comAlfa(g2, e * 0.85f);
        int m = (sombraCache.getWidth() - larg) / 2;
        g2.drawImage(sombraCache, Math.round(x) - m, Math.round(y + 0.35f * u) - m, null);
        comAlfa(g2, e);
        g2.drawImage(logoCache, Math.round(x), Math.round(y), null);
        // brilho que atravessa o logo: na entrada e depois a cada 9 s
        float p = seg < 1.2f ? -1f : ((seg - 1.2f) % 9f) / 1.6f;
        if (p >= 0f && p <= 1f) g2.drawImage(Logo.brilho(logoCache, Animacao.suave(p)), Math.round(x), Math.round(y), null);
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
        Font fDica = Estreia.texto(0.95f * u);
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
                // dica do item selecionado, centrada embaixo do menu
                g2.setFont(fDica);
                FontMetrics fd = g2.getFontMetrics();
                g2.setColor(Tema.alfa(Estreia.BRANCO, Math.round(110 * a)));
                g2.drawString(it.dica, (w - fd.stringWidth(it.dica)) / 2f, y + total + 2.6f * u);
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
        g2.drawString("versão", 6.5f * u, base);
        float x = 6.5f * u + g2.getFontMetrics().stringWidth("versão") + 0.5f * u;
        g2.setFont(f2);
        g2.setColor(Estreia.BRANCO);
        g2.drawString(VERSAO, x, base);
        g2.dispose();
    }
}
