package mod;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

// Barra de rolagem fina, sem setas: trilho escuro e alca arredondada que
// acende em verde quando o mouse passa ou arrasta.
public class RolagemUI extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent c) {
        return new RolagemUI();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) { return semBotao(); }

    @Override
    protected JButton createIncreaseButton(int orientation) { return semBotao(); }

    private static JButton semBotao() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(Estreia.dentro(scrollbar) ? ConfigEstreia.CELULA : Tema.BASE);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean vertical = r.height > r.width;
        int esp = Math.max(4, Math.min(r.width, r.height) - 6);
        int x = vertical ? r.x + (r.width - esp) / 2 : r.x + 2;
        int y = vertical ? r.y + 2 : r.y + (r.height - esp) / 2;
        int w = vertical ? esp : r.width - 4, h = vertical ? r.height - 4 : esp;
        Color cor = isDragging ? Tema.NEON : isThumbRollover() ? Tema.MENTA : new Color(0x4A4740);
        if (Estreia.dentro(scrollbar)) cor = isDragging ? Estreia.OURO : isThumbRollover() ? Estreia.BRANCO : new Color(244, 241, 234, 70);
        g2.setColor(cor);
        g2.fillRoundRect(x, y, w, h, esp, esp);
        g2.dispose();
    }
}
