package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JTable;
import javax.swing.border.Border;

// Barrinha dourada na beira esquerda da linha sob o mouse, que desliza de uma linha para a
// outra junto com o mouse (e some devagar quando ele sai da tabela). Desenhada como borda
// da tabela, por cima das celulas, sem mexer nos renderers do jogo.
final class Barrinha implements Border {
    private static final String Y = "brombila.barrinhaY", H = "brombila.barrinhaH", A = "brombila.barrinhaA";
    private static final int LARG = 3;

    private final Border antes;
    private int ultimaY = -1, ultimaH = 0;

    private Barrinha(Border antes) { this.antes = antes; }

    static void instala(JTable t) {
        if (t.getBorder() instanceof Barrinha) return;
        t.setBorder(new Barrinha(t.getBorder()));
    }

    public Insets getBorderInsets(Component c) {
        return antes != null ? antes.getBorderInsets(c) : new Insets(0, 0, 0, 0);
    }

    public boolean isBorderOpaque() { return antes != null && antes.isBorderOpaque(); }

    public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
        if (antes != null) antes.paintBorder(c, g0, x, y, w, h);
        JTable t = (JTable) c;
        Object lm = t.getClientProperty(Tema.LINHA_MOUSE);
        int linha = lm instanceof Integer ? (Integer) lm : -1;
        boolean tem = linha >= 0 && linha < t.getRowCount() && !t.isRowSelected(linha);
        if (tem) {
            Rectangle r = t.getCellRect(linha, 0, true);
            ultimaY = r.y;
            ultimaH = r.height;
        }
        if (ultimaY < 0) return;
        // primeira vez: aparece direto na linha, sem vir deslizando do topo
        float ay = Animacao.valor(t, Y, ultimaY, 150, ultimaY);
        float ah = Animacao.valor(t, H, ultimaH, 150, ultimaH);
        float alfa = Animacao.valor(t, A, tem ? 1f : 0f, tem ? 120 : 220, 0f);
        if (alfa <= 0.01f) return;
        Rectangle vis = t.getVisibleRect();
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int bx = vis.x, by = Math.round(ay) + 3, bh = Math.max(4, Math.round(ah) - 6);
        // brilho suave ao lado e a barrinha
        g.setColor(new Color(242, 193, 78, Math.round(46 * alfa)));
        g.fillRoundRect(bx, by - 1, LARG + 4, bh + 2, 6, 6);
        g.setColor(new Color(242, 193, 78, Math.round(255 * alfa)));
        g.fillRoundRect(bx, by, LARG, bh, LARG, LARG);
        g.dispose();
    }
}
