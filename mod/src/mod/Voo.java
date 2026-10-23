package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Escalar time animado: quando um jogador muda de lugar (arrastado, trocado com outro ou
// pelo "Escalar titulares"), ele voa do lugar antigo (ou de onde foi solto) ate o novo,
// em vez de pular. A logica da escalacao e toda do jogo; aqui so se compara a lista antes
// e depois de cada desenho do campo (components.ao chama renderizando) e se desenha o voo
// numa camada por cima do painel do Escalar (a.bz). Enquanto voa, o jogador nao e
// desenhado no lugar novo (ao pergunta voando).
public final class Voo {
    private Voo() {}

    private static final int MS = 300;
    private static final int POUSO_MS = 260;

    // ultimo retrato da lista de jogadores do Escalar: indice -> jogador
    private static WeakReference<List<?>> listaVista = new WeakReference<List<?>>(null);
    private static Object[] retrato;
    private static int[][] campo;                                 // RJ do components.ao
    private static Map<Integer, Point> naLista = new HashMap<Integer, Point>(), naListaAntes;
    private static Image goleiro, time, vazia;

    private static final class Viagem {
        Object jogador;
        int de, para;
        float x0, y0;
        BufferedImage sprite;
        long inicio;
        boolean solto;
    }

    private static final Map<Object, Viagem> voando = new IdentityHashMap<Object, Viagem>();
    private static Painel painel;
    private static Timer timer;
    private static Object bz;

    // components.ao: posicoes do campo e do banco (RJ) e as camisas da vez.
    public static void posicoes(int[][] rj, Image gol, Image camisaTime, Image camisaVazia) {
        campo = rj;
        goleiro = gol;
        if (camisaTime != null) time = camisaTime;
        vazia = camisaVazia;
    }

    // Lista "Jogadores disponiveis" sendo desenhada: indice -> canto da camisa (no painel).
    public static void comecaLista() {
        naListaAntes = naLista;
        naLista = new HashMap<Integer, Point>();
    }

    public static void naLista(int indice, int x, int y) {
        naLista.put(indice, new Point(x, y));
    }

    public static boolean voando(Object jogador) {
        return jogador != null && voando.containsKey(jogador);
    }

    // Chamado pelo components.ao antes de desenhar o campo do Escalar.
    public static void renderizando(ArrayList<?> lista, Object ao) {
        if (!Animacao.ligada()) {
            retrato = null;
            return;
        }
        Object[] agora = new Object[lista.size()];
        for (int i = 0; i < agora.length; i++) agora[i] = jogador(lista.get(i));
        Object[] antes = listaVista.get() == lista ? retrato : null;
        listaVista = new WeakReference<List<?>>(lista);
        retrato = agora;
        if (antes == null || campo == null) return;
        Map<Object, Integer> onde = new IdentityHashMap<Object, Integer>();
        for (int i = 1; i < antes.length; i++) if (antes[i] != null) onde.put(antes[i], i);
        List<Viagem> novas = new ArrayList<Viagem>();
        for (int i = 1; i < agora.length; i++) {
            Object j = agora[i];
            if (j == null) continue;
            Integer de = onde.get(j);
            if (de == null || de == i) continue;
            if (de > 36 && i > 36) continue; // so reordenou a lista de disponiveis
            Viagem v = new Viagem();
            v.jogador = j;
            v.de = de;
            v.para = i;
            novas.add(v);
        }
        if (novas.isEmpty() || novas.size() > 30) return;
        Object dono = escalar(lista);
        if (dono == null) return;
        JLayeredPane bp = camada(dono);
        if (bp == null || !bp.isShowing()) return;
        // o que foi arrastado sai de onde o mouse o soltou
        Point mouse = null;
        try {
            mouse = java.awt.MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mouse, bp);
        } catch (Throwable t) {
            mouse = null;
        }
        long agoraMs = System.currentTimeMillis();
        Viagem solto = null;
        double melhor = 70;
        for (Viagem v : novas) {
            Point p = canto(v.para, false);
            if (p == null || mouse == null) continue;
            double d = mouse.distance(p.x + 25, p.y + 25);
            if (d < melhor) {
                melhor = d;
                solto = v;
            }
        }
        for (Viagem v : novas) {
            Point p0 = v == solto ? new Point(mouse.x - 25, mouse.y - 25) : canto(v.de, true);
            if (p0 == null) continue;
            v.solto = v == solto;
            v.x0 = p0.x;
            v.y0 = p0.y;
            v.sprite = sprite(v.jogador, v.para);
            if (v.sprite == null) continue;
            // os trocados saem um pouquinho depois do que foi solto, em cascata
            v.inicio = agoraMs + (v.solto ? 0 : 40 + 18 * Math.min(10, voando.size()));
            voando.put(v.jogador, v);
        }
        if (voando.isEmpty()) return;
        bz = dono;
        instala(bp);
    }

    private static Object jogador(Object item) {
        return item instanceof components.ao ? ((components.ao) item).x() : null;
    }

    // Canto da camisa do indice i no painel do Escalar (antes = posicao da lista antiga).
    private static Point canto(int i, boolean antes) {
        if (i >= 1 && i <= 25) return new Point(campo[i][0], campo[i][1]);
        if (i >= 26 && i <= 36) return new Point(campo[i][0] - 10 + 4, campo[i][1] + 4);
        Map<Integer, Point> m = antes && naListaAntes != null ? naListaAntes : naLista;
        Point p = m.get(i);
        if (p == null && antes) p = naLista.get(i);
        return p == null ? null : new Point(p.x, p.y);
    }

    private static int tamanho(int i) {
        return i <= 25 ? 0 : i <= 36 ? 42 : 45;
    }

    // Camisa + numero + rotulo como o components.ao desenha no destino. O canto da camisa fica em (60, 6).
    private static BufferedImage sprite(Object jogador, int para) {
        try {
            best.F f = (best.F) jogador;
            Image camisa = para == 1 || para > 25 && f.getPosicao() == 0 ? goleiro : time;
            if (camisa == null) return null;
            int t = tamanho(para);
            int w = t > 0 ? t : camisa.getWidth(null), h = t > 0 ? t : camisa.getHeight(null);
            BufferedImage img = new BufferedImage(190, 120, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            Escalacao.prepara(g);
            int x = 60, y = 6;
            Escalacao.camisa(g, camisa, x, y, t, t);
            Image rot;
            if (para <= 25) {
                rot = components.ao.a(f, 1, para, "");
                if (rot != null) g.drawImage(rot, x - 52, y + 47, null);
            } else if (para <= 36) {
                rot = components.ao.a(f, 4, para, "");
                if (rot != null) g.drawImage(rot, x + 21 - 75, y + 39, null);
            } else {
                rot = components.ao.a(f, 2, para, "");
                if (rot != null) g.drawImage(rot, x - 53, y + 42, null);
            }
            Escalacao.numero(g, camisa, f.fn(), x, y, w, h);
            g.dispose();
            return img;
        } catch (Throwable t) {
            return null;
        }
    }

    // O painel do Escalar (a.bz) que usa esta lista.
    private static Object escalar(List<?> lista) {
        for (Window w : Window.getWindows()) {
            if (!w.isShowing()) continue;
            Object b = procura(w);
            if (b != null && campoDe(b, "AE") == lista) return b;
        }
        return null;
    }

    private static Object procura(Component c) {
        if (c.getClass().getName().equals("a.bz")) return c;
        if (c instanceof Container) {
            for (Component f : ((Container) c).getComponents()) {
                Object r = procura(f);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static Object campoDe(Object o, String nome) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(nome);
            f.setAccessible(true);
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static JLayeredPane camada(Object b) {
        Object c = campoDe(b, "Bp");
        return c instanceof JLayeredPane ? (JLayeredPane) c : null;
    }

    private static void instala(JLayeredPane bp) {
        if (painel == null || painel.getParent() != bp) {
            if (painel != null && painel.getParent() != null) painel.getParent().remove(painel);
            painel = new Painel();
            try {
                bp.add(painel, new components.a(0, 0, bp.getWidth(), bp.getHeight()));
            } catch (Throwable t) {
                bp.add(painel);
            }
            painel.setBounds(0, 0, bp.getWidth(), bp.getHeight());
            bp.setLayer(painel, JLayeredPane.DRAG_LAYER.intValue() + 1);
        }
        if (timer == null) {
            timer = new Timer(Animacao.QUADRO_MS, new ActionListener() {
                public void actionPerformed(ActionEvent e) { quadro(); }
            });
        }
        timer.start();
    }

    private static void quadro() {
        long agora = System.currentTimeMillis();
        boolean pousou = false;
        boolean resta = false;
        for (Viagem v : new ArrayList<Viagem>(voando.values())) {
            if (agora - v.inicio >= MS) {
                if (v.sprite != null) {
                    pousos.add(new long[] {agora});
                    pousoXY.add(canto(v.para, false));
                }
                v.sprite = null;
                voando.remove(v.jogador);
                pousou = true;
            } else {
                resta = true;
            }
        }
        if (pousou) redesenhaEscalar();
        // pousos: um anel dourado que abre e some
        for (int i = pousos.size() - 1; i >= 0; i--) {
            if (agora - pousos.get(i)[0] > POUSO_MS) {
                pousos.remove(i);
                pousoXY.remove(i);
            }
        }
        if (painel != null) painel.repaint();
        if (!resta && pousos.isEmpty()) {
            timer.stop();
            if (painel != null && painel.getParent() != null) {
                Container p = painel.getParent();
                p.remove(painel);
                p.repaint();
            }
            painel = null;
        }
    }

    private static final List<long[]> pousos = new ArrayList<long[]>();
    private static final List<Point> pousoXY = new ArrayList<Point>();

    // Redesenha o campo, o banco e a lista do jogo agora com quem pousou (metodos do a.bz).
    private static void redesenhaEscalar() {
        if (bz == null) return;
        for (String m : new String[] {"cS", "cT"}) {
            try {
                java.lang.reflect.Method mt = bz.getClass().getDeclaredMethod(m, int.class);
                mt.setAccessible(true);
                mt.invoke(bz, -1);
            } catch (Throwable t) {
                // fica como esta
            }
        }
    }

    private static float sai(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static final class Painel extends JComponent {
        Painel() {
            setOpaque(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false; // nao pega clique
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            long agora = System.currentTimeMillis();
            for (int i = 0; i < pousos.size(); i++) {
                Point p = pousoXY.get(i);
                if (p == null) continue;
                float t = (agora - pousos.get(i)[0]) / (float) POUSO_MS;
                float r = 18 + 22 * sai(t);
                g.setStroke(new BasicStroke(2.2f * (1f - t) + 0.5f));
                g.setColor(Tema.alfa(Tema.OURO, Math.round(200 * (1f - t))));
                g.draw(new Ellipse2D.Float(p.x + 27 - r, p.y + 27 - r * 0.62f, 2 * r, 2 * r * 1.24f));
            }
            for (Viagem v : voando.values()) {
                if (v.sprite == null) continue;
                float t = (agora - v.inicio) / (float) MS;
                if (t < 0) {
                    // ainda nao saiu: fica parado onde estava
                    t = 0;
                }
                Point p1 = canto(v.para, false);
                if (p1 == null) continue;
                float e = sai(Math.min(1f, t));
                float x = v.x0 + (p1.x - v.x0) * e;
                float y = v.y0 + (p1.y - v.y0) * e;
                // arco leve e a camisa "erguida" (maior) no meio do caminho
                float dist = (float) Math.hypot(p1.x - v.x0, p1.y - v.y0);
                float arco = (float) Math.sin(Math.PI * e) * Math.min(26f, dist * 0.12f);
                float esc = 1f + 0.10f * (float) Math.sin(Math.PI * e) + (v.solto ? 0.06f * (1f - e) : 0f);
                // sombra no chao
                float sx = x + 27, sy = y + 54;
                g.setPaint(new RadialGradientPaint(sx, sy, 24, new float[] {0f, 1f},
                    new Color[] {new Color(0, 0, 0, Math.round(60 + 50 * (float) Math.sin(Math.PI * e))), new Color(0, 0, 0, 0)}));
                g.fill(new Ellipse2D.Float(sx - 24, sy - 7, 48, 14));
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x + 27, y - arco + 27);
                g2.scale(esc, esc);
                g2.translate(-(60 + 27), -(6 + 27));
                g2.drawImage(v.sprite, 0, 0, null);
                g2.dispose();
            }
            g.dispose();
        }
    }
}
