package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

// Celula da tabela de jogos da rodada (tela da partida), desenhada no estilo "Noite de estreia":
//  - time: cartao de vidro escuro com uma faixa nas duas cores do clube;
//  - placar: quadradinho escuro com o numero; no gol acende dourado;
//  - estadio e lances: texto solto; titulo de divisao: texto dourado com linha embaixo.
public class CelulaRodada extends JLabel {
    static final int ESTADIO = 0, TIME = 1, PLACAR = 2, LANCE = 3, TITULO = 4;
    static final Color PLACAR_PADRAO = new Color(84, 127, 59);

    int tipo;
    Color cor1, cor2;   // cores do clube
    boolean gol;        // placar acendendo (gol recente)
    float hover;        // linha sob o mouse

    CelulaRodada() { setOpaque(false); }

    void prepara(JLabel orig, int col, float hover) {
        String txt = orig.getText();
        Color bg = orig.getBackground();
        this.hover = hover;
        setFont(orig.getFont());
        setIcon(orig.getIcon());
        setText(txt);
        setHorizontalAlignment(orig.getHorizontalAlignment());
        setBorder(null);
        if (col == 1 || col == 4) {
            tipo = (txt == null || txt.isEmpty()) ? ESTADIO : TIME;
            cor1 = bg; cor2 = orig.getForeground();
            setForeground(Tema.TEXTO);
            setHorizontalAlignment(SwingConstants.LEFT);
        } else if (col == 2 || col == 3) {
            tipo = (txt == null || txt.isEmpty()) ? ESTADIO : PLACAR;
            gol = bg != null && !Tema.igual(bg, PLACAR_PADRAO);
            setForeground(gol ? new Color(0x1A1408) : Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD));
        } else if (col == 0 && bg != null && Tema.igual(bg, new Color(27, 50, 24))) {
            tipo = TITULO;
            setForeground(Tema.DOURADO);
            setFont(getFont().deriveFont(Font.BOLD));
        } else if (col == 0) {
            tipo = ESTADIO;
            setForeground(Tema.TEXTO_FRACO);
        } else {
            tipo = LANCE;
            setForeground(Tema.texto(orig.getForeground()));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (hover > 0 && tipo != TITULO) {
                g2.setColor(Tema.alfa(Tema.DOURADO, Math.round(16 * hover)));
                g2.fillRect(0, 0, w, h);
            }
            if (tipo == TIME) {
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x1C201E), 0, h, new Color(0x131614)));
                g2.fill(r);
                // faixa do clube: cor principal com a secundaria por dentro
                java.awt.Shape clip = g2.getClip();
                g2.clip(r);
                g2.setColor(vivo(cor1));
                g2.fillRect(0, 0, 5, h);
                g2.setColor(vivo(cor2));
                g2.fillRect(5, 0, 2, h);
                g2.setPaint(new GradientPaint(0, 0, Tema.alfa(vivo(cor1), 60), 60, 0, Tema.alfa(vivo(cor1), 0)));
                g2.fillRect(7, 0, 60, h);
                g2.setClip(clip);
                g2.setColor(new Color(255, 255, 255, 24 + Math.round(30 * hover)));
                g2.draw(r);
                desenhaConteudo(g2, 14, w, h);
            } else if (tipo == PLACAR) {
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
                if (gol) {
                    g2.setColor(Tema.alfa(Tema.DOURADO, 70));
                    g2.setStroke(new BasicStroke(4f));
                    g2.draw(r);
                    g2.setPaint(new GradientPaint(0, 0, new Color(0xFFE08A), 0, h, Tema.DOURADO_ESCURO));
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x0F1211), 0, h, new Color(0x080A09)));
                }
                g2.fill(r);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(gol ? Tema.DOURADO : Tema.alfa(Tema.DOURADO, 70));
                g2.draw(r);
                centraliza(g2, w, h);
            } else if (tipo == TITULO) {
                desenhaConteudo(g2, 6, w, h);
                g2.setPaint(new GradientPaint(0, 0, Tema.DOURADO, w, 0, Tema.alfa(Tema.DOURADO, 0)));
                g2.fillRect(4, h - 2, w - 4, 1);
            } else {
                // estadio e lances: o JLabel desenha (o texto dos lances vem em HTML)
                super.paintComponent(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    // Texto (e escudo) alinhado a esquerda a partir de x; corta com "..." se nao couber.
    private void desenhaConteudo(Graphics2D g2, int x, int w, int h) {
        Icon ic = getIcon();
        if (ic != null) {
            ic.paintIcon(this, g2, x, (h - ic.getIconHeight()) / 2);
            x += ic.getIconWidth() + 6;
        }
        String s = getText();
        if (s == null || s.isEmpty()) return;
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int max = w - x - 4;
        if (fm.stringWidth(s) > max) {
            while (s.length() > 1 && fm.stringWidth(s + "...") > max) s = s.substring(0, s.length() - 1);
            s = s + "...";
        }
        if (getHorizontalAlignment() == SwingConstants.RIGHT) x = w - 6 - fm.stringWidth(s);
        g2.setColor(getForeground());
        g2.drawString(s, x, (h - fm.getHeight()) / 2 + fm.getAscent());
    }

    private void centraliza(Graphics2D g2, int w, int h) {
        String s = getText();
        Icon ic = getIcon();
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int tw = (s == null ? 0 : fm.stringWidth(s)) + (ic == null ? 0 : ic.getIconWidth() + 4);
        int x = (w - tw) / 2;
        if (ic != null) { ic.paintIcon(this, g2, x, (h - ic.getIconHeight()) / 2); x += ic.getIconWidth() + 4; }
        if (s != null) {
            g2.setColor(getForeground());
            g2.drawString(s, x, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // Cor do clube legivel no escuro: preto vira cinza, branco fica branco.
    static Color vivo(Color c) {
        if (c == null) return Tema.TEXTO_FRACO;
        if (Tema.luminancia(c) < 0.12f) return new Color(0x47433C);
        return c;
    }

    // Os JLabels de renderer nao precisam revalidar/repintar a cada uso.
    @Override public void invalidate() {}
    @Override public void validate() {}
    @Override public void revalidate() {}
    @Override public void repaint(long tm, int x, int y, int width, int height) {}
    @Override public void repaint() {}
}
