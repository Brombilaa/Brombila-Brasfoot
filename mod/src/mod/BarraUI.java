package mod;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicProgressBarUI;

// Barras de progresso (energia, confianca...): trilho escuro arredondado e
// preenchimento neon com a ponta brilhando. A cor muda com o valor: verde,
// amarelo e vermelho, a menos que o jogo tenha escolhido uma cor propria.
public class BarraUI extends BasicProgressBarUI {
    public static ComponentUI createUI(JComponent c) {
        return new BarraUI();
    }

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        JProgressBar pb = (JProgressBar) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets in = pb.getInsets();
            int x = in.left, y = in.top, w = pb.getWidth() - in.left - in.right, h = pb.getHeight() - in.top - in.bottom;
            if (w <= 0 || h <= 0) return;
            boolean texto = pb.isStringPainted();
            // com texto a barra ocupa a celula toda; sem texto fica uma faixa fina
            int bh = texto ? h : Math.min(h, 10);
            int by = y + (h - bh) / 2;
            float raio = texto ? 8 : bh;
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fill(new RoundRectangle2D.Float(x, by, w, bh, raio, raio));
            double pct = pb.getPercentComplete();
            int fw = (int) Math.round(w * pct);
            Color cor = cor(pb, pct);
            if (fw > 0 && texto) {
                // com texto: preenchimento mais escuro (o texto branco continua legivel)
                // e uma linha neon embaixo
                g2.setPaint(new GradientPaint(0, by, Tema.mistura(cor, Tema.BASE, 0.45f), 0, by + bh, Tema.mistura(cor, Tema.BASE, 0.65f)));
                g2.fill(new RoundRectangle2D.Float(x, by, fw, bh, raio, raio));
                g2.setColor(cor);
                g2.fillRect(x + 2, by + bh - 2, Math.max(0, fw - 4), 2);
            } else if (fw > 0) {
                g2.setPaint(new GradientPaint(0, by, Tema.mistura(cor, Color.WHITE, 0.25f), 0, by + bh, Tema.mistura(cor, Color.BLACK, 0.15f)));
                g2.fill(new RoundRectangle2D.Float(x, by, fw, bh, raio, raio));
                float gr = Math.max(8f, bh * 1.2f);
                g2.setPaint(new RadialGradientPaint(x + fw, by + bh / 2f, gr, new float[] {0f, 1f},
                    new Color[] {Tema.alfa(cor, 150), Tema.alfa(cor, 0)}));
                g2.fillRect(Math.round(x + fw - gr), Math.round(by + bh / 2f - gr), Math.round(gr * 2), Math.round(gr * 2));
            }
            if (texto) {
                String s = pb.getString();
                FontMetrics fm = g2.getFontMetrics(pb.getFont());
                g2.setFont(pb.getFont());
                int tx = x + (w - fm.stringWidth(s)) / 2, ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(s, tx, ty + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(s, tx, ty);
            }
        } finally {
            g2.dispose();
        }
    }

    static Color cor(JProgressBar pb, double pct) {
        Color fg = pb.getForeground();
        if (fg != null && !(fg instanceof UIResource)) {
            float[] hsb = Color.RGBtoHSB(fg.getRed(), fg.getGreen(), fg.getBlue(), null);
            if (hsb[1] > 0.3f && hsb[2] > 0.3f) return Tema.texto(fg);
        }
        if (pct > 0.7) return Tema.NEON;
        if (pct > 0.4) return Tema.OURO;
        return Tema.VERMELHO;
    }
}
