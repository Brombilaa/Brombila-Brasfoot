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

    // Sem a moldura quadrada do Metal: so o trilho arredondado (mesmos espacos).
    @Override
    protected void installDefaults() {
        super.installDefaults();
        java.awt.Insets i = progressBar.getInsets();
        progressBar.setBorder(new javax.swing.border.EmptyBorder(i));
        javax.swing.LookAndFeel.installProperty(progressBar, "opaque", Boolean.FALSE);
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
            double pct = pb.getPercentComplete();
            Color cor = cor(pb, pct);
            int bx = x, bw = w;
            if (texto) {
                // valor escrito a esquerda, na cor da barra; a barra fica fina ao lado
                String s = pb.getString();
                java.awt.Font f = pb.getFont().deriveFont(java.awt.Font.BOLD);
                FontMetrics fm = g2.getFontMetrics(f);
                int larg = fm.stringWidth("100%");
                g2.setFont(f);
                int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(new Color(0, 0, 0, 110));
                g2.drawString(s, x + 2 + larg - fm.stringWidth(s), ty + 1);
                g2.setColor(Tema.mistura(cor, Color.WHITE, 0.25f));
                g2.drawString(s, x + 2 + larg - fm.stringWidth(s), ty);
                bx = x + larg + 8;
                bw = w - larg - 10;
                if (bw < 10) return;
            }
            float bh = Math.min(h, texto ? 6f : 8f);
            float by = y + (h - bh) / 2f;
            RoundRectangle2D trilho = new RoundRectangle2D.Float(bx, by, bw, bh, bh, bh);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fill(trilho);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.draw(new RoundRectangle2D.Float(bx - 0.5f, by - 0.5f, bw + 1, bh + 1, bh + 1, bh + 1));
            float fw = (float) (bw * pct);
            if (fw > 0.5f) {
                fw = Math.max(fw, bh);
                // brilho difuso por baixo do preenchimento
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(Tema.alfa(cor, 14));
                    g2.fill(new RoundRectangle2D.Float(bx - i, by - i, fw + 2 * i, bh + 2 * i, bh + 2 * i, bh + 2 * i));
                }
                g2.setPaint(new GradientPaint(bx, 0, Tema.mistura(cor, Tema.BASE, 0.45f), bx + fw, 0, cor));
                g2.fill(new RoundRectangle2D.Float(bx, by, fw, bh, bh, bh));
                // fio de luz em cima
                g2.setColor(new Color(255, 255, 255, 70));
                g2.fill(new RoundRectangle2D.Float(bx + bh / 2f, by + 1f, Math.max(0, fw - bh), Math.max(1f, bh / 4f), 2, 2));
                // ponta acesa
                float gr = bh * 1.1f;
                g2.setPaint(new RadialGradientPaint(bx + fw - bh / 2f, by + bh / 2f, gr, new float[] {0f, 1f},
                    new Color[] {Tema.alfa(Tema.mistura(cor, Color.WHITE, 0.5f), 120), Tema.alfa(cor, 0)}));
                g2.fill(new java.awt.geom.Ellipse2D.Float(bx + fw - bh / 2f - gr, by + bh / 2f - gr, gr * 2, gr * 2));
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
