package mod;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Rolagem animada da rodinha do mouse: em vez de pular de uma vez, a lista
// desliza ate o destino (ease-out), e giros seguidos se somam.
final class RolagemSuave implements MouseWheelListener, ActionListener {
    private static final String CHAVE = "brombila.rolagemSuave";

    private final JScrollPane sp;
    private final Timer timer = new Timer(Animacao.QUADRO_MS, this);
    private double alvo = -1, atual;
    private boolean horizontal;

    private RolagemSuave(JScrollPane sp) {
        this.sp = sp;
    }

    static void instala(JScrollPane sp) {
        if (sp.getClientProperty(CHAVE) != null || !sp.isWheelScrollingEnabled()) return;
        RolagemSuave r = new RolagemSuave(sp);
        sp.putClientProperty(CHAVE, r);
        sp.setWheelScrollingEnabled(false);
        sp.addMouseWheelListener(r);
        sp.getVerticalScrollBar().setUnitIncrement(Math.max(sp.getVerticalScrollBar().getUnitIncrement(), 16));
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean h = e.isShiftDown() || !sp.getVerticalScrollBar().isVisible();
        JScrollBar bar = h ? sp.getHorizontalScrollBar() : sp.getVerticalScrollBar();
        JViewport vp = sp.getViewport();
        if (bar == null || !bar.isVisible() || vp == null || vp.getView() == null) {
            repassa(e);
            return;
        }
        int max = bar.getMaximum() - bar.getVisibleAmount();
        double rot = e.getPreciseWheelRotation();
        int pos = bar.getValue();
        // Ja no limite: deixa o painel de fora rolar.
        if ((rot < 0 && pos <= bar.getMinimum()) || (rot > 0 && pos >= max)) {
            if (!timer.isRunning()) { repassa(e); return; }
        }
        if (!timer.isRunning() || h != horizontal) {
            atual = pos;
            alvo = pos;
            horizontal = h;
        }
        int passo = Math.max(bar.getUnitIncrement(rot > 0 ? 1 : -1), 16) * 3;
        alvo = Math.max(bar.getMinimum(), Math.min(max, alvo + rot * passo));
        if (!timer.isRunning()) timer.start();
        e.consume();
    }

    public void actionPerformed(ActionEvent ev) {
        JScrollBar bar = horizontal ? sp.getHorizontalScrollBar() : sp.getVerticalScrollBar();
        if (bar == null) { timer.stop(); return; }
        // Se o jogo moveu a barra por conta propria, comeca dali.
        if (Math.abs(bar.getValue() - Math.round(atual)) > 2) atual = bar.getValue();
        atual += (alvo - atual) * 0.22;
        if (Math.abs(alvo - atual) < 0.6) {
            atual = alvo;
            timer.stop();
        }
        bar.setValue((int) Math.round(atual));
    }

    private void repassa(MouseWheelEvent e) {
        JScrollPane pai = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, sp);
        if (pai == null) return;
        Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), pai);
        pai.dispatchEvent(new MouseWheelEvent(pai, e.getID(), e.getWhen(), e.getModifiers(), p.x, p.y,
            e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(), e.getScrollType(),
            e.getScrollAmount(), e.getWheelRotation(), e.getPreciseWheelRotation()));
    }
}
