package mod;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.RepaintManager;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

// Telas pequenas: o jogo foi desenhado para 1024x768 livres. Abaixo disso o jogo inteiro e
// desenhado menor, na proporcao que faz 1024x800 caber na area livre da tela (sem a barra do
// Windows), sem barras de rolagem.
//
// Como funciona, sem mexer nas telas do jogo:
// - cada janela tem as camadas (JLayeredPane) num tamanho "virtual" maior que a janela, dentro
//   de uma Lente que desenha tudo reduzido;
// - os pedidos de repintura de dentro da Lente sao convertidos para a area reduzida;
// - os eventos do mouse sao convertidos de volta para o tamanho virtual antes de o Swing
//   escolher o componente, entao cliques, arrastes, rolagem e foco funcionam como antes;
// - menus, listas de combos e dicas abrem dentro das camadas da propria janela.
// A escala vem da tela, ou de "escala=" no brombila.ini (1 desliga).
public final class Encolhe {
    private Encolhe() {}

    // a tela mais alta do jogo (Configuracoes iniciais) tem 798 px
    static final int LARG = 1024, ALT = 800;

    static double fator = 1;
    private static boolean iniciada;
    static final String SEM = "brombila.semEscala";
    private static final String FEITO = "brombila.escala";

    static boolean ativa() { return fator < 0.999; }

    // Medida virtual -> tamanho real na tela.
    public static int real(int v) { return ativa() ? (int) Math.round(v * fator) : v; }

    static void inicia() {
        if (iniciada) return;
        iniciada = true;
        double f = 1;
        try {
            Rectangle a = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            f = Math.min(1.0, Math.min(a.width / (double) LARG, a.height / (double) ALT));
        } catch (Throwable t) {
            // sem tela: fica 1
        }
        String p = Janela.pref("escala", "");
        if (!p.isEmpty()) {
            try {
                f = Math.max(0.5, Math.min(1.0, Double.parseDouble(p.replace(',', '.'))));
            } catch (NumberFormatException e) {
                // valor invalido: fica o da tela
            }
        }
        fator = f;
        if (!ativa()) return;
        // o Swing empurra menus para dentro da tela usando as medidas virtuais (maiores que a
        // tela de verdade): sem isso os menus de baixo subiriam a toa
        System.setProperty("javax.swing.adjustPopupLocationToFit", "false");
        RepaintManager.setCurrentManager(new Repinta());
        PopupFactory.setSharedInstance(new Popups());
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new Fila());
    }

    // ------------------------------------------------------------ janelas

    static boolean deFora(Window w) {
        // JWindow: so o mod usa (fantasmas das janelas fechando), com fotos da tela de verdade
        if (!(w instanceof RootPaneContainer) || w instanceof javax.swing.JWindow) return true;
        JRootPane rp = ((RootPaneContainer) w).getRootPane();
        return rp == null || rp.getClientProperty(SEM) != null;
    }

    // Janela sendo montada (antes de aparecer): poe a Lente entre a raiz e as camadas.
    static void instala(Window w) {
        if (!ativa() || deFora(w) || w.getClass().getName().equals("a.gq")) return;
        final JRootPane rp = ((RootPaneContainer) w).getRootPane();
        if (rp.getClientProperty(FEITO) != null) return;
        rp.putClientProperty(FEITO, Boolean.TRUE);
        JLayeredPane lp = rp.getLayeredPane();
        Lente lente = new Lente(lp);
        rp.setUI(new RaizUI());
        rp.remove(lp);
        rp.add(lente);
        rp.setLayout(new Raiz(rp, lente));
        rp.revalidate();
    }

    // Janela acabou de abrir. Se o tamanho dela veio do jogo (setSize) e nao do pack, esse
    // tamanho e virtual: a janela encolhe na escala e fica centralizada onde estava.
    static void abriu(Window w) {
        if (!ativa() || deFora(w)) return;
        if (Janela.ehPrincipal(w)) {
            // tela cheia: o pack do jogo deixou a janela do tamanho do conteudo ja reduzido;
            // se o sistema nao maximizou, ocupa a area livre
            if (w instanceof java.awt.Frame && ((java.awt.Frame) w).isUndecorated()) {
                Rectangle a = Cabe.area();
                if (w.getWidth() < a.width - 2 || w.getHeight() < a.height - 2) w.setBounds(a);
            }
            return;
        }
        JRootPane rp = ((RootPaneContainer) w).getRootPane();
        if (rp.getClientProperty(FEITO) == null) return;
        Insets in = w.getInsets();
        Dimension raiz = new Dimension(w.getWidth() - in.left - in.right, w.getHeight() - in.top - in.bottom);
        Dimension pack = rp.getPreferredSize();
        if (Math.abs(raiz.width - pack.width) <= 2 && Math.abs(raiz.height - pack.height) <= 2) return;
        tamanho(w, raiz.width, raiz.height);
    }

    // Poe a janela com o tamanho virtual vw x vh (sem as bordas do Windows), centralizada
    // no mesmo ponto.
    public static void tamanho(Window w, int vw, int vh) {
        Insets in = w.getInsets();
        int nw = real(vw) + in.left + in.right, nh = real(vh) + in.top + in.bottom;
        if (!ativa() || lenteDe(w) == null && !(w instanceof RootPaneContainer && ((RootPaneContainer) w).getRootPane().getClientProperty(FEITO) != null)) {
            nw = vw;
            nh = vh;
        }
        Rectangle r = w.getBounds();
        int cx = r.x + r.width / 2, cy = r.y + r.height / 2;
        Rectangle a = Cabe.area();
        int x = Math.max(a.x, Math.min(cx - nw / 2, a.x + a.width - nw));
        int y = Math.max(a.y, Math.min(cy - nh / 2, a.y + a.height - nh));
        w.setBounds(x, y, nw, nh);
        w.validate();
    }

    // Tamanho da janela em medidas virtuais (o que o jogo acha que ela tem).
    public static Dimension virtual(Window w) {
        if (ativa() && w instanceof RootPaneContainer && ((RootPaneContainer) w).getRootPane().getClientProperty(FEITO) != null) {
            JRootPane rp = ((RootPaneContainer) w).getRootPane();
            for (Component c : rp.getComponents()) if (c instanceof Lente) return ((Lente) c).lp.getSize();
        }
        return w.getSize();
    }

    // Quanto uma imagem tirada da tela de verdade encolhe para entrar nas medidas virtuais de c.
    static double paraVirtual(Component c) { return lenteDe(c) != null ? 1 / fator : 1; }

    // Ponto da tela de verdade -> coordenadas (virtuais) de um componente.
    public static Point daTela(Point tela, Component c) {
        Lente l = lenteDe(c);
        if (l == null) {
            Point p = new Point(tela);
            SwingUtilities.convertPointFromScreen(p, c);
            return p;
        }
        Point o = l.getLocationOnScreen();
        Point v = new Point((int) Math.floor((tela.x - o.x) / fator), (int) Math.floor((tela.y - o.y) / fator));
        return SwingUtilities.convertPoint(l.lp, v, c);
    }

    static Lente lenteDe(Component c) {
        for (Component p = c; p != null && !(p instanceof Window); p = p.getParent()) {
            if (p instanceof Lente) return (Lente) p;
        }
        return null;
    }

    // ------------------------------------------------------------ desenho

    // Desenha as camadas da janela reduzidas. E a origem da pintura: o Swing repinta tudo que
    // esta dentro dela a partir dela, com a area ja convertida pelo Repinta.
    static final class Lente extends JComponent {
        final JLayeredPane lp;

        Lente(JLayeredPane lp) {
            this.lp = lp;
            setLayout(null);
            add(lp);
            setOpaque(false);
        }

        @Override
        protected boolean isPaintingOrigin() { return true; }

        // o Swing procura o componente do mouse em medidas virtuais
        @Override
        public boolean contains(int x, int y) { return dentroVirtual(this, x, y); }

        @Override
        public void doLayout() {
            int vw = (int) Math.ceil(getWidth() / fator), vh = (int) Math.ceil(getHeight() / fator);
            lp.setBounds(0, 0, vw, vh);
        }

        @Override
        protected void paintChildren(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.scale(fator, fator);
            super.paintChildren(g);
            g.dispose();
        }
    }

    static boolean dentroVirtual(Component c, int x, int y) {
        return x >= 0 && y >= 0 && x < Math.ceil(c.getWidth() / fator) + 1 && y < Math.ceil(c.getHeight() / fator) + 1;
    }

    // A raiz da janela (JRootPane) e de tamanho real, mas o Swing confere se o ponto do mouse,
    // ja em medidas virtuais, esta dentro dela antes de descer para as camadas.
    static final class RaizUI extends javax.swing.plaf.basic.BasicRootPaneUI {
        @Override
        public boolean contains(JComponent c, int x, int y) { return dentroVirtual(c, x, y); }
    }

    // Layout da raiz: a Lente ocupa a raiz; dentro dela, as camadas no tamanho virtual com a
    // barra de menu e o conteudo, como o RootLayout do Swing faria.
    static final class Raiz implements LayoutManager2 {
        final JRootPane rp;
        final Lente lente;

        Raiz(JRootPane rp, Lente lente) {
            this.rp = rp;
            this.lente = lente;
        }

        private Dimension conteudo(Dimension c) {
            Dimension d = c == null ? new Dimension(0, 0) : new Dimension(c);
            JMenuBar mb = rp.getJMenuBar();
            if (mb != null) {
                Dimension m = mb.getPreferredSize();
                d.width = Math.max(d.width, m.width);
                d.height += m.height;
            }
            Insets i = rp.getInsets();
            return new Dimension(real(d.width) + i.left + i.right, real(d.height) + i.top + i.bottom);
        }

        public Dimension preferredLayoutSize(Container p) {
            return conteudo(rp.getContentPane() == null ? null : rp.getContentPane().getPreferredSize());
        }

        public Dimension minimumLayoutSize(Container p) {
            return conteudo(rp.getContentPane() == null ? null : rp.getContentPane().getMinimumSize());
        }

        public Dimension maximumLayoutSize(Container p) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }

        public void layoutContainer(Container p) {
            Insets i = rp.getInsets();
            int w = rp.getWidth() - i.left - i.right, h = rp.getHeight() - i.top - i.bottom;
            lente.setBounds(i.left, i.top, w, h);
            lente.doLayout();
            JLayeredPane lp = lente.lp;
            int vw = lp.getWidth(), vh = lp.getHeight(), y = 0;
            JMenuBar mb = rp.getJMenuBar();
            if (mb != null) {
                int mh = mb.getPreferredSize().height;
                mb.setBounds(0, 0, vw, mh);
                y = mh;
            }
            if (rp.getContentPane() != null) rp.getContentPane().setBounds(0, y, vw, vh - y);
            // o vidro fica no espaco virtual, onde chegam os eventos do mouse
            if (rp.getGlassPane() != null) rp.getGlassPane().setBounds(i.left, i.top, vw, vh);
        }

        public void addLayoutComponent(String n, Component c) {}
        public void addLayoutComponent(Component c, Object o) {}
        public void removeLayoutComponent(Component c) {}
        public float getLayoutAlignmentX(Container t) { return 0f; }
        public float getLayoutAlignmentY(Container t) { return 0f; }
        public void invalidateLayout(Container t) {}
    }

    // Repintura pedida por um componente de dentro da Lente: vira a area reduzida da Lente.
    static final class Repinta extends RepaintManager {
        @Override
        public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
            Lente l = c instanceof Lente ? null : lenteDe(c);
            if (l == null || w <= 0 || h <= 0) {
                super.addDirtyRegion(c, x, y, w, h);
                return;
            }
            Rectangle r = SwingUtilities.convertRectangle(c, new Rectangle(x, y, w, h), l.lp);
            int x0 = (int) Math.floor(r.x * fator) - 1, y0 = (int) Math.floor(r.y * fator) - 1;
            int x1 = (int) Math.ceil((r.x + r.width) * fator) + 1, y1 = (int) Math.ceil((r.y + r.height) * fator) + 1;
            super.addDirtyRegion(l, x0, y0, x1 - x0, y1 - y0);
        }
    }

    // ------------------------------------------------------------ mouse

    // Os eventos do mouse chegam na janela com a posicao real; antes de o Swing procurar o
    // componente, a posicao vira a virtual (a mesma conta do desenho, ao contrario).
    static final class Fila extends EventQueue {
        @Override
        protected void dispatchEvent(AWTEvent e) {
            if (e instanceof MouseEvent && ((MouseEvent) e).getSource() instanceof Window) {
                MouseEvent m = (MouseEvent) e;
                Window w = (Window) m.getSource();
                if (!deFora(w)) {
                    JRootPane rp = ((RootPaneContainer) w).getRootPane();
                    Component l = rp.getComponentCount() > 0 ? lenteNa(rp) : null;
                    if (l != null) {
                        Point o = SwingUtilities.convertPoint(l, 0, 0, w);
                        int x = m.getX(), y = m.getY();
                        if (x >= o.x && y >= o.y) {
                            int vx = o.x + (int) Math.floor((x - o.x) / fator);
                            int vy = o.y + (int) Math.floor((y - o.y) / fator);
                            e = copia(m, w, vx, vy);
                        }
                    }
                }
            }
            super.dispatchEvent(e);
        }

        private static Component lenteNa(JRootPane rp) {
            for (Component c : rp.getComponents()) if (c instanceof Lente) return c;
            return null;
        }

        private static MouseEvent copia(MouseEvent m, Window w, int x, int y) {
            if (m instanceof MouseWheelEvent) {
                MouseWheelEvent r = (MouseWheelEvent) m;
                return new MouseWheelEvent(w, r.getID(), r.getWhen(), r.getModifiers() | r.getModifiersEx(), x, y,
                    r.getXOnScreen(), r.getYOnScreen(), r.getClickCount(), r.isPopupTrigger(), r.getScrollType(),
                    r.getScrollAmount(), r.getWheelRotation(), r.getPreciseWheelRotation());
            }
            return new MouseEvent(w, m.getID(), m.getWhen(), m.getModifiers() | m.getModifiersEx(), x, y, m.getXOnScreen(),
                m.getYOnScreen(), m.getClickCount(), m.isPopupTrigger(), m.getButton());
        }
    }

    // ------------------------------------------------------------ menus e dicas

    // Menus, listas de combos e dicas de janelas com Lente abrem dentro das camadas dela
    // (desenhados na mesma escala), nunca numa janelinha a parte com medidas erradas.
    static final class Popups extends PopupFactory {
        @Override
        public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
            JRootPane rp = owner == null ? null : SwingUtilities.getRootPane(owner);
            if (rp != null && rp.getClientProperty(FEITO) != null && rp.isShowing()) return new NaLente(rp.getLayeredPane(), contents, x, y);
            return super.getPopup(owner, contents, x, y);
        }
    }

    static final class NaLente extends Popup {
        final JLayeredPane lp;
        final JPanel caixa = new JPanel(new BorderLayout());

        NaLente(JLayeredPane lp, Component contents, int x, int y) {
            this.lp = lp;
            caixa.setOpaque(false);
            caixa.add(contents, BorderLayout.CENTER);
            Point o = lp.getLocationOnScreen();
            Dimension d = contents.getPreferredSize();
            int vx = x - o.x, vy = y - o.y;
            // lista do combo: o Swing decide abrir para cima comparando com a tela de verdade,
            // que e menor que as medidas virtuais; aqui abre embaixo sempre que couber
            Component dono = contents instanceof javax.swing.JPopupMenu ? ((javax.swing.JPopupMenu) contents).getInvoker() : null;
            if (dono instanceof javax.swing.JComboBox && dono.isShowing()) {
                Point c = SwingUtilities.convertPoint(dono, 0, 0, lp);
                vy = c.y + dono.getHeight() + d.height <= lp.getHeight() ? c.y + dono.getHeight() : c.y - d.height;
            }
            vx = Math.max(0, Math.min(vx, lp.getWidth() - d.width));
            vy = Math.max(0, Math.min(vy, lp.getHeight() - d.height));
            caixa.setBounds(vx, vy, d.width, d.height);
        }

        @Override
        public void show() {
            lp.add(caixa, JLayeredPane.POPUP_LAYER, 0);
            caixa.validate();
            caixa.repaint();
        }

        @Override
        public void hide() {
            Rectangle r = caixa.getBounds();
            lp.remove(caixa);
            lp.repaint(r);
        }
    }

    // Rolagem por copia de pixels (blit) nao combina com o desenho reduzido.
    static void viewport(Component c) {
        if (ativa() && c instanceof JViewport) ((JViewport) c).setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    }
}
