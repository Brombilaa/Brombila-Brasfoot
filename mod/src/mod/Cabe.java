package mod;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

// Telas pequenas (1366x768, 1280x720 ou menos): o jogo foi desenhado para 1024x768 livres.
// - Janela principal: as telas de tamanho fixo (partida 1024x748, fim de temporada 1024x718,
//   configuracoes iniciais...) passam a rolar, usando a rolagem que o proprio jogo ja tem
//   para telas pequenas (best.S.gJ), que ele so ligava abaixo de 700 px de altura.
// - Janelas por cima (Escalar 1024x748 e outras): se passam da area livre da tela, encolhem
//   para caber, centralizadas, e o conteudo rola dentro delas.
public final class Cabe {
    private Cabe() {}

    static final int LARG = 1024, ALT = 768;

    static Rectangle area() {
        try {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        } catch (Throwable t) {
            return new Rectangle(0, 0, 4096, 4096);
        }
    }

    static boolean pequena() {
        Rectangle a = area();
        return a.width < LARG || a.height < ALT;
    }

    // Janela principal sendo montada: liga a rolagem do jogo antes da primeira troca de tela.
    static void principal() {
        if (!pequena() || Encolhe.ativa()) return;
        try {
            java.lang.reflect.Field f = Class.forName("best.S").getDeclaredField("gJ");
            f.setAccessible(true);
            if (!f.getBoolean(null)) f.setBoolean(null, true);
        } catch (Throwable t) {
            // versao do jogo diferente: fica como estava
        }
    }

    // Com a escala ligada a tela inteira cabe: se o jogo ja embrulhou a tela numa rolagem
    // (ele faz isso sozinho abaixo de 700 px), desembrulha e desliga a rolagem dele.
    static void semRolagem(javax.swing.JViewport vp, Component tela) {
        if (!Encolhe.ativa() || !tela.getClass().getName().equals("components.E")) return;
        if (!(vp.getParent() instanceof JScrollPane)) return;
        JScrollPane rola = (JScrollPane) vp.getParent();
        Container cp = rola.getParent();
        if (cp == null || !(SwingUtilities.getWindowAncestor(cp) instanceof javax.swing.JFrame)) return;
        try {
            java.lang.reflect.Field f = Class.forName("best.S").getDeclaredField("gJ");
            f.setAccessible(true);
            f.setBoolean(null, false);
        } catch (Throwable t) {
            return;
        }
        cp.remove(rola);
        cp.add(tela, java.awt.BorderLayout.CENTER);
        cp.revalidate();
    }

    static boolean rolagemLigada() {
        try {
            java.lang.reflect.Field f = Class.forName("best.S").getDeclaredField("gJ");
            f.setAccessible(true);
            return f.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    // Janela por cima que acabou de abrir: depois do tema e do layout, ve se cabe.
    static void janela(final Window w) {
        if (!(w instanceof JDialog)) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { encaixa((JDialog) w); }
        });
    }

    private static void encaixa(JDialog d) {
        if (!d.isShowing()) return;
        Rectangle a = area();
        Dimension t = d.getSize();
        if (t.width <= a.width && t.height <= a.height) {
            // o jogo abre algumas (Estadio) grudadas no canto da tela: centraliza sobre o jogo
            if (d.getX() == a.x && d.getY() == a.y && t.width < a.width - 40 && t.height < a.height - 40)
                d.setLocationRelativeTo(d.getOwner() != null && d.getOwner().isShowing() ? d.getOwner() : null);
            return;
        }
        Container cp = d.getContentPane();
        boolean jaRola = cp.getComponentCount() == 1 && cp.getComponent(0) instanceof JScrollPane;
        JScrollPane barra = jaRola ? (JScrollPane) cp.getComponent(0) : null;
        if (!jaRola && cp instanceof JComponent && d.getRootPane().getClientProperty("brombila.cabe") == null) {
            d.getRootPane().putClientProperty("brombila.cabe", Boolean.TRUE);
            JComponent conteudo = (JComponent) cp;
            conteudo.setPreferredSize(conteudo.getSize());
            JScrollPane rola = new JScrollPane(conteudo);
            rola.setBorder(BorderFactory.createEmptyBorder());
            rola.setViewportBorder(null);
            rola.setOpaque(false);
            rola.getViewport().setOpaque(false);
            rola.getVerticalScrollBar().setUnitIncrement(24);
            rola.getHorizontalScrollBar().setUnitIncrement(24);
            d.setContentPane(rola);
            barra = rola;
        } else if (jaRola) {
            JScrollPane rola = (JScrollPane) cp.getComponent(0);
            // a tela de dentro (Escalar, a.bz) foi feita para o tamanho cheio da janela
            Component v = rola.getViewport().getView();
            if (v instanceof JComponent) {
                Dimension pv = v.getPreferredSize(), c = cp.getSize();
                if (pv.width < c.width || pv.height < c.height)
                    ((JComponent) v).setPreferredSize(new Dimension(Math.max(pv.width, c.width), Math.max(pv.height, c.height)));
            }
            rola.setBorder(BorderFactory.createEmptyBorder());
            rola.getVerticalScrollBar().setUnitIncrement(24);
            rola.getHorizontalScrollBar().setUnitIncrement(24);
        }
        int nh = Math.min(t.height, a.height);
        // rolando na vertical, a barra precisa de espaco ao lado (senao aparece a horizontal)
        int extra = nh < t.height && barra != null ? barra.getVerticalScrollBar().getPreferredSize().width : 0;
        int nw = Math.min(t.width + extra, a.width);
        d.setBounds(a.x + (a.width - nw) / 2, a.y + (a.height - nh) / 2, nw, nh);
        d.validate();
        d.repaint();
    }

    // Janela do tamanho que a tela de dentro pede (penaltis): espera a janela existir, em vez
    // de um invokeLater que as vezes roda antes de o jogo por o painel na janela.
    public static void tamanho(final JComponent tela, final int w, final int h) {
        final Runnable poe = new Runnable() {
            public void run() {
                Window j = SwingUtilities.getWindowAncestor(tela);
                if (j == null || tela.getClientProperty("brombila.tamanho") != null) return;
                tela.putClientProperty("brombila.tamanho", Boolean.TRUE);
                if (Encolhe.ativa()) {
                    Encolhe.tamanho(j, w, h);
                    j.setLocationRelativeTo(null);
                    return;
                }
                Rectangle a = area();
                j.setSize(Math.min(w, a.width), Math.min(h, a.height));
                j.setLocationRelativeTo(null);
            }
        };
        tela.addHierarchyListener(new java.awt.event.HierarchyListener() {
            public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                if (SwingUtilities.getWindowAncestor(tela) != null) SwingUtilities.invokeLater(poe);
            }
        });
        SwingUtilities.invokeLater(poe);
    }

    // Area onde a tela grande cabe sem rolar: com a rolagem ligada, e o que a vista mostra.
    static Dimension vista(Component area) {
        if (!(area.getParent() instanceof javax.swing.JViewport)) return null;
        return ((javax.swing.JViewport) area.getParent()).getExtentSize();
    }
}
