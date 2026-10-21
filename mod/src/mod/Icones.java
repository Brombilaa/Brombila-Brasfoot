package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.plaf.UIResource;

// Caixinhas de marcar e bolinhas de opcao no estilo "Noite de jogo".
final class Icones {
    private Icones() {}

    private static Graphics2D prepara(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static class Check implements Icon, UIResource {
        public int getIconWidth() { return 17; }
        public int getIconHeight() { return 17; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel m = ((AbstractButton) c).getModel();
            boolean on = m.isSelected(), ativo = m.isEnabled();
            Graphics2D g2 = prepara(g);
            RoundRectangle2D q = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, 15, 15, 5, 5);
            boolean ouro = Estreia.dentro(c);
            if (on && ativo) {
                g2.setColor(ouro ? Estreia.OURO : Tema.NEON);
                g2.fill(q);
                g2.setColor(new Color(0x04150B));
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D v = new Path2D.Float();
                v.moveTo(x + 4, y + 8.5);
                v.lineTo(x + 7, y + 11.5);
                v.lineTo(x + 12.5, y + 4.5);
                g2.draw(v);
            } else {
                g2.setColor(new Color(255, 255, 255, ativo ? 18 : 8));
                g2.fill(q);
                g2.setColor(on ? Tema.alfa(ouro ? Estreia.OURO : Tema.NEON, 90) : new Color(255, 255, 255, ativo && m.isRollover() ? 150 : ativo ? 80 : 35));
                g2.draw(q);
            }
            g2.dispose();
        }
    }

    static class Radio implements Icon, UIResource {
        public int getIconWidth() { return 17; }
        public int getIconHeight() { return 17; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            ButtonModel m = ((AbstractButton) c).getModel();
            boolean on = m.isSelected(), ativo = m.isEnabled();
            Graphics2D g2 = prepara(g);
            Ellipse2D o = new Ellipse2D.Float(x + 0.5f, y + 0.5f, 15, 15);
            g2.setColor(new Color(255, 255, 255, ativo ? 18 : 8));
            g2.fill(o);
            g2.setColor(on && ativo ? Tema.NEON : new Color(255, 255, 255, ativo && m.isRollover() ? 150 : ativo ? 80 : 35));
            g2.draw(o);
            if (on) {
                if (ativo) {
                    g2.setPaint(new RadialGradientPaint(x + 8f, y + 8f, 9f, new float[] {0f, 1f},
                        new Color[] {Tema.alfa(Tema.NEON, 110), Tema.alfa(Tema.NEON, 0)}));
                    g2.fill(new Ellipse2D.Float(x - 1, y - 1, 18, 18));
                }
                g2.setColor(ativo ? Tema.NEON : Tema.TEXTO_FRACO);
                g2.fill(new Ellipse2D.Float(x + 4.5f, y + 4.5f, 8, 8));
            }
            g2.dispose();
        }
    }
}
