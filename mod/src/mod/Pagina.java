package mod;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Uma so tela: as janelas que o jogo abre por cima da tela principal (Tabelas, Estadio,
// Escalar, fichas, avisos...) deixam de ser janelas soltas, cada uma de um tamanho. Cada uma
// passa a ocupar exatamente a area da janela do jogo, com o conteudo no meio:
// - estilo "pagina" (padrao): o fundo e o mesmo do menu e das Configuracoes iniciais (foto
//   do estadio escura), como se o jogo tivesse trocado de pagina;
// - estilo "veu": o fundo e a propria tela do jogo, escurecida.
// Perguntas Sim/Nao (JOptionPane) continuam pequenas, mas escurecem a tela de tras.
// Em modo janela, a "pagina" fica em cima da janela do jogo (antes abria no meio do monitor).
//
// Como funciona, sem mexer nas telas do jogo: quando o dialogo vai aparecer (ja tem o tamanho
// que o jogo pediu), esse tamanho vira o tamanho do conteudo; a janela cresce para a area do
// jogo e o layout da raiz poe o conteudo no centro, com um fundo numa camada de baixo.
// brombila.ini: umaTela=0 desliga; estiloTela=veu troca o fundo.
public final class Pagina {
    private Pagina() {}

    static final String FEITO = "brombila.pagina";
    private static final Integer CAMADA_FUNDO = Integer.valueOf(JLayeredPane.FRAME_CONTENT_LAYER.intValue() - 1);
    private static final List<JDialog> abertas = new ArrayList<JDialog>();

    static boolean ligada() { return !"0".equals(prop("umaTela", "1")); }

    static boolean veu() { return "veu".equals(prop("estiloTela", "pagina")); }

    private static String prop(String k, String padrao) {
        String s = System.getProperty("brombila." + k);
        return s != null ? s : Janela.pref(k, padrao);
    }

    static void instala() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                try {
                    if (e instanceof HierarchyEvent) {
                        HierarchyEvent h = (HierarchyEvent) e;
                        // a janela acabou de ganhar o "peer" (setVisible), mas ainda nao apareceu
                        if ((h.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
                                && h.getChanged() instanceof JDialog && h.getComponent() == h.getChanged()
                                && h.getChanged().isDisplayable() && !h.getChanged().isShowing()) {
                            prepara((JDialog) h.getChanged());
                        }
                    } else if (e.getID() == WindowEvent.WINDOW_OPENED && e.getSource() instanceof JDialog) {
                        abriu((JDialog) e.getSource());
                    } else if (e.getID() == WindowEvent.WINDOW_CLOSED && e.getSource() instanceof JDialog) {
                        fechou((JDialog) e.getSource());
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, AWTEvent.HIERARCHY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
    }

    static boolean eh(Window w) {
        return w instanceof RootPaneContainer && ((RootPaneContainer) w).getRootPane().getClientProperty(FEITO) != null;
    }

    // ------------------------------------------------------------ quais janelas

    private static boolean pergunta(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof JOptionPane) return true;
            if (k instanceof Container && pergunta((Container) k)) return true;
        }
        return false;
    }

    private static String tela(JDialog d) {
        Container cp = d.getContentPane();
        return cp.getComponentCount() > 0 ? cp.getComponent(0).getClass().getName() : "";
    }

    private static boolean serve(JDialog d) {
        if (!ligada() || !d.isUndecorated() || eh(d)) return false;
        JFrame p = Janela.principal();
        if (p == null || !p.isShowing()) return false;
        Window dono = d.getOwner();
        // so as do jogo: donas a janela principal, outra pagina ou nenhuma (o Swing poe uma escondida)
        if (dono != null && dono != p && !eh(dono) && dono.isShowing()) return false;
        if (pergunta(d.getRootPane())) return false;
        String t = tela(d);
        // Carregando do jogo (a.hz) e a janela do proprio mod que mostra a trava
        if (t.equals("a.hz")) return false;
        Dimension s = d.getSize();
        return s.width >= 120 && s.height >= 80;
    }

    // ------------------------------------------------------------ area do jogo

    // Area da janela do jogo na tela (sem a barra de titulo do Windows, em modo janela).
    static Rectangle area() {
        JFrame p = Janela.principal();
        if (p == null || !p.isShowing()) return Cabe.area();
        JRootPane rp = p.getRootPane();
        Point o = rp.getLocationOnScreen();
        return new Rectangle(o.x, o.y, rp.getWidth(), rp.getHeight());
    }

    // ------------------------------------------------------------ abrir / fechar

    private static void prepara(final JDialog d) {
        if (!serve(d)) return;
        final JRootPane rp = d.getRootPane();
        // tamanho que o jogo deu (medidas do jogo; com a escala ligada, virtuais)
        Dimension conteudo = d.getSize();
        if (conteudo.width < 50 || conteudo.height < 50) conteudo = rp.getPreferredSize();
        final Dimension[] tam = {new Dimension(conteudo)};
        final BufferedImage atras = veu() ? foto() : null;
        rp.putClientProperty(FEITO, tam);

        final Fundo fundo = new Fundo(atras);
        rp.getLayeredPane().add(fundo, CAMADA_FUNDO);
        rp.setLayout(new Centro(rp.getLayout(), tam, fundo));

        final Rectangle a = area();
        d.setBounds(a);
        rp.revalidate();

        // O jogo pode mudar o tamanho depois (pack, setSize) ou mudar de lugar: vira o novo
        // tamanho do conteudo e a janela volta para a area do jogo.
        d.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { volta(d, tam); }
            public void componentMoved(ComponentEvent e) { volta(d, tam); }
        });
        synchronized (abertas) { abertas.add(d); }
    }

    private static void volta(final JDialog d, final Dimension[] tam) {
        if (!d.isDisplayable()) return;
        Rectangle a = area();
        Rectangle r = d.getBounds();
        if (r.equals(a)) return;
        if (r.width != a.width || r.height != a.height) {
            // o jogo pediu outro tamanho: medidas do jogo (virtuais com a escala)
            Dimension v = Encolhe.ativa() ? new Dimension((int) Math.round(r.width / Encolhe.fator), (int) Math.round(r.height / Encolhe.fator)) : r.getSize();
            if (v.width >= 50 && v.height >= 50 && (v.width < a.width || v.height < a.height)) tam[0] = v;
        }
        d.setBounds(a);
        d.getRootPane().revalidate();
        d.repaint();
    }

    private static void abriu(JDialog d) {
        if (pergunta(d.getRootPane())) {
            escurece(d);
            // no meio do jogo (o Swing poe no meio do monitor quando nao acha a janela dona)
            // (depois do Tema, que refaz o tamanho dela ao abrir)
            final JDialog p = d;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (!ligada() || Janela.principal() == null || !Janela.principal().isShowing() || !p.isShowing()) return;
                    Rectangle a = area();
                    p.setLocation(a.x + (a.width - p.getWidth()) / 2, a.y + (a.height - p.getHeight()) / 2);
                }
            });
            return;
        }
        if (!eh(d)) return;
        confere(d);
        // alguns gerenciadores de janela (Linux) ainda poem a janela onde querem depois de abrir
        final JDialog dd = d;
        Timer t = new Timer(120, new ActionListener() {
            public void actionPerformed(ActionEvent e) { confere(dd); }
        });
        t.setRepeats(false);
        t.start();
    }

    private static void confere(JDialog d) {
        if (!d.isShowing()) return;
        Rectangle a = area();
        Point o = d.getLocationOnScreen();
        if (o.x != a.x || o.y != a.y || d.getWidth() != a.width || d.getHeight() != a.height) {
            d.setBounds(a);
            d.getRootPane().revalidate();
        }
    }

    private static void fechou(JDialog d) {
        synchronized (abertas) { abertas.remove(d); }
        clareia(d);
    }

    // ------------------------------------------------------------ perguntas

    private static final String VEU = "brombila.veuPergunta";

    // Pergunta Sim/Nao aberta: a janela de tras (o jogo ou a pagina aberta) escurece.
    private static void escurece(JDialog d) {
        if (!ligada()) return;
        Window dono = d.getOwner();
        if (!(dono instanceof RootPaneContainer) || !dono.isShowing()) {
            JFrame p = Janela.principal();
            if (p == null || !p.isShowing()) return;
            dono = topo(p);
        }
        final JLayeredPane lp = ((RootPaneContainer) dono).getRootPane().getLayeredPane();
        final JComponent v = new JComponent() {
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(4, 6, 5, 175));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        v.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(v, JLayeredPane.MODAL_LAYER);
        lp.repaint();
        d.getRootPane().putClientProperty(VEU, v);
    }

    private static void clareia(JDialog d) {
        Object v = d.getRootPane().getClientProperty(VEU);
        if (!(v instanceof JComponent)) return;
        d.getRootPane().putClientProperty(VEU, null);
        Container lp = ((JComponent) v).getParent();
        if (lp == null) return;
        lp.remove((JComponent) v);
        lp.repaint();
    }

    // Janela que esta por cima de todas agora (a ultima pagina aberta, ou o jogo).
    private static Window topo(JFrame p) {
        synchronized (abertas) {
            for (int i = abertas.size() - 1; i >= 0; i--) if (abertas.get(i).isShowing()) return abertas.get(i);
        }
        return p;
    }

    // ------------------------------------------------------------ fundo

    // Foto do que esta na tela agora (o jogo e as paginas abertas), para o estilo "veu".
    private static BufferedImage foto() {
        JFrame p = Janela.principal();
        if (p == null) return null;
        Rectangle a = area();
        if (a.width <= 0 || a.height <= 0) return null;
        BufferedImage img = new BufferedImage(a.width, a.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            p.getRootPane().paint(g);
            synchronized (abertas) {
                for (JDialog d : abertas) {
                    if (!d.isShowing()) continue;
                    Point o = d.getRootPane().getLocationOnScreen();
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.translate(o.x - a.x, o.y - a.y);
                    d.getRootPane().paint(g2);
                    g2.dispose();
                }
            }
        } catch (Throwable t) {
            // sem foto: fundo liso
        }
        g.dispose();
        return img;
    }

    static final class Fundo extends JComponent {
        private final BufferedImage atras;
        private BufferedImage cache;

        Fundo(BufferedImage atras) {
            this.atras = atras;
            setOpaque(true);
            // clique fora do conteudo nao passa para nada
            addMouseListener(new MouseAdapter() {});
        }

        @Override
        protected void paintComponent(Graphics g0) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) cache = desenha(w, h);
            g0.drawImage(cache, 0, 0, null);
        }

        private BufferedImage desenha(int w, int h) {
            if (atras == null) return ConfigEstreia.Fundo.desenha(w, h);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(atras, 0, 0, w, h, null);
            g.setColor(new Color(4, 6, 5, 170));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return img;
        }
    }

    // ------------------------------------------------------------ layout

    // Layout da raiz: o do Swing (ou o da escala) arruma tudo e depois o conteudo vai para o
    // centro, no tamanho que o jogo pediu; o fundo ocupa as camadas inteiras.
    static final class Centro implements LayoutManager2 {
        final LayoutManager orig;
        final Dimension[] tam;
        final Fundo fundo;

        Centro(LayoutManager orig, Dimension[] tam, Fundo fundo) {
            this.orig = orig;
            this.tam = tam;
            this.fundo = fundo;
        }

        public void layoutContainer(Container c) {
            orig.layoutContainer(c);
            JRootPane rp = (JRootPane) c;
            JLayeredPane lp = rp.getLayeredPane();
            Container cp = rp.getContentPane();
            int W = lp.getWidth(), H = lp.getHeight();
            fundo.setBounds(0, 0, W, H);
            int w = Math.min(tam[0].width, W), h = Math.min(tam[0].height, H);
            cp.setBounds((W - w) / 2, (H - h) / 2, w, h);
            Component vidro = rp.getGlassPane();
            if (vidro != null) vidro.setBounds(cp.getBounds());
        }

        public Dimension preferredLayoutSize(Container c) { return new Dimension(tam[0]); }
        public Dimension minimumLayoutSize(Container c) { return orig.minimumLayoutSize(c); }
        public Dimension maximumLayoutSize(Container c) {
            return orig instanceof LayoutManager2 ? ((LayoutManager2) orig).maximumLayoutSize(c) : new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        public void addLayoutComponent(String n, Component k) { orig.addLayoutComponent(n, k); }
        public void addLayoutComponent(Component k, Object o) {
            if (orig instanceof LayoutManager2) ((LayoutManager2) orig).addLayoutComponent(k, o);
        }
        public void removeLayoutComponent(Component k) { orig.removeLayoutComponent(k); }
        public float getLayoutAlignmentX(Container c) { return 0.5f; }
        public float getLayoutAlignmentY(Container c) { return 0.5f; }
        public void invalidateLayout(Container c) {
            if (orig instanceof LayoutManager2) ((LayoutManager2) orig).invalidateLayout(c);
        }
    }
}
