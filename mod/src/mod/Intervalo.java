package mod;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

// Escalacao durante a partida e intervalo (a.cD). O jogo pinta img/f02.jpg por cima do
// painel inteiro e usa uma imagem de banco com ruido; aqui um fundo nosso cobre os dois, o
// banco vira um cartao escuro, a coluna da direita vira cartoes (placar, estadio, lances,
// estatisticas em barras) e "Voltar ao jogo" vira o botao dourado principal.
final class Intervalo {
    private Intervalo() {}

    static final int W = 966, H = 726;
    static final int DX = 652, DW = 300; // coluna da direita

    static boolean aplica(JPanel cd) {
        if (cd.getClientProperty(Janelas.FEITO) != null) return true;
        cd.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        Object c;

        // placar (cartao 1)
        for (String[] r : new String[][] {{"zO", "22"}, {"zP", "102"}}) {
            c = Dialogos.campo(cd, r[0]);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setOpaque(false);
                l.setBorder(null);
                l.setIconTextGap(8);
                l.setFont(Estreia.titulo(24f));
                l.setForeground(Estreia.BRANCO);
                l.setText(l.getText() == null ? "" : l.getText().trim());
                l.setHorizontalAlignment(SwingConstants.LEFT);
            }
        }
        Dialogos.pos(Dialogos.campo(cd, "zO"), DX + 18, 20, 220, 32);
        Dialogos.pos(Dialogos.campo(cd, "zP"), DX + 18, 74, 220, 32);
        for (String s : new String[] {"Dw", "Dx"}) {
            c = Dialogos.campo(cd, s);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setOpaque(false);
                l.setFont(Estreia.titulo(34f));
                l.setForeground(Estreia.OURO);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            }
        }
        Dialogos.pos(Dialogos.campo(cd, "Dw"), DX + DW - 70, 16, 52, 40);
        Dialogos.pos(Dialogos.campo(cd, "Dx"), DX + DW - 70, 70, 52, 40);
        for (String s : new String[] {"DB", "DC"}) {
            c = Dialogos.campo(cd, s);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setOpaque(false);
                l.setFont(Estreia.espacada(Estreia.negrito(10f), 0.14f));
                l.setForeground(Tema.alfa(Estreia.OURO, 210));
            }
        }
        Dialogos.pos(Dialogos.campo(cd, "DB"), DX + 18, 50, 120, 16);
        Dialogos.pos(Dialogos.campo(cd, "DC"), DX + 18, 104, 120, 16);

        // estadio e minuto (cartao 2)
        c = Dialogos.campo(cd, "ug");
        if (c instanceof JComponent) ((JComponent) c).setVisible(false);
        // o minuto (Dv/DE) passa a ser desenhado pelo fundo, que le os rotulos
        c = Dialogos.campo(cd, "we");
        if (c instanceof JComponent) ((JComponent) c).setVisible(false);
        c = Dialogos.campo(cd, "Cg");
        Dialogos.fonte(c, Estreia.semi(12.5f), Estreia.BRANCO);
        Dialogos.pos(c, DX + 78, 154, DW - 92, 20);
        Dialogos.fonte(Dialogos.campo(cd, "Dy"), Estreia.texto(12f), Estreia.FRACO);
        Dialogos.pos(Dialogos.campo(cd, "Dy"), DX + 78, 182, 100, 20);
        Dialogos.fonte(Dialogos.campo(cd, "Dz"), Estreia.texto(12f), Estreia.FRACO);
        Dialogos.pos(Dialogos.campo(cd, "Dz"), DX + 186, 182, 100, 20);

        // lances (cartao 3)
        c = Dialogos.campo(cd, "ut");
        if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
        }
        Dialogos.pos(c, DX + 10, 252, DW - 20, 262);
        JLabel lances = Dialogos.rotulo("Lances");
        Dialogos.adiciona(cd, lances, DX + 18, 232, 120, 16);

        // estatisticas (cartao 4)
        c = Dialogos.campo(cd, "xI");
        if (c instanceof JLabel) {
            ((JLabel) c).setVisible(false);
            Dialogos.adiciona(cd, new Placar.Barras((JLabel) c), DX + 18, 548, DW - 36, 104);
        }

        // rodape: substituicoes, taticas e voltar
        c = Dialogos.campo(cd, "DA");
        if (c instanceof JLabel) {
            ((JLabel) c).setUI(new Substituicoes());
            Dialogos.fonte(c, Estreia.negrito(12f), Estreia.BRANCO);
        }
        Dialogos.pos(c, 14, 672, 190, 44);
        String[][] rot = {{"vy", "Estilo de jogo"}, {"vz", "Marcação"}, {"vA", "Ataques"}};
        for (String[] r : rot) {
            c = Dialogos.campo(cd, r[0]);
            if (c instanceof JLabel) ((JLabel) c).setText(r[1]);
            Dialogos.rotulo(c);
        }
        c = Dialogos.campo(cd, "Dm");
        if (c instanceof JButton) {
            JButton b = (JButton) c;
            b.putClientProperty(BotaoUI.PRIMARIO, Boolean.TRUE);
            b.setFont(Estreia.negrito(13f));
            b.setText("Voltar ao jogo");
            b.setIcon(null);
        }
        Dialogos.pos(c, DX, 674, DW, 40);

        // banco: cartao escuro no lugar da imagem com ruido
        c = Dialogos.campo(cd, "Dq");
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            l.setText("");
            l.setIcon(new ImageIcon(banco(210, 650)));
        }

        // fundo por cima do f02.jpg que o jogo desenha (ultimo filho = pintado primeiro)
        final Fundo fundo = new Fundo(cd);
        for (String s : new String[] {"Dv", "DE"}) {
            c = Dialogos.campo(cd, s);
            if (c instanceof JLabel) ((JLabel) c).addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) { fundo.repaint(); }
            });
        }
        Dialogos.fundo(cd, fundo, 0, 0, W, H);
        cd.revalidate();
        cd.repaint();
        return true;
    }

    // Banco de reservas: cartao com o rotulo no alto e linhas finas de vidro.
    static BufferedImage banco(int w, int h) {
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        Placar.antialias(g);
        int cw = 194;
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, cw - 1, h - 1, 16, 16);
        g.setPaint(new GradientPaint(0, 0, new Color(0x181C19), 0, h, new Color(0x0D100E)));
        g.fill(r);
        g.setClip(r);
        g.setPaint(new GradientPaint(0, 0, Tema.alfa(Estreia.OURO, 0), cw / 2f, 0, Tema.alfa(Estreia.OURO, 180), true));
        g.fillRect(16, 0, cw - 32, 2);
        g.setColor(new Color(244, 241, 234, 8));
        for (int y = 30; y < h; y += 95) g.fillRect(12, y, cw - 24, 1);
        g.setClip(null);
        g.setColor(new Color(244, 241, 234, 30));
        g.draw(r);
        g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.2f));
        g.setColor(Estreia.OURO);
        g.drawString("RESERVAS", 14, 18);
        g.dispose();
        return im;
    }

    // "Substituicoes restantes: 5" vira numero grande dourado + rotulo.
    static class Substituicoes extends javax.swing.plaf.basic.BasicLabelUI {
        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            Placar.antialias(g);
            String s = ((JLabel) c).getText();
            String n = s == null ? "" : s.replaceAll("[^0-9]", "");
            g.setFont(Estreia.titulo(32f));
            g.setColor(Estreia.OURO);
            g.drawString(n, 2, 34);
            int x = g.getFontMetrics().stringWidth(n) + 12;
            g.setFont(Estreia.espacada(Estreia.negrito(9.5f), 0.16f));
            g.setColor(Estreia.OURO);
            g.drawString("SUBSTITUIÇÕES", x, 18);
            g.setFont(Estreia.texto(11.5f));
            g.setColor(Estreia.FRACO);
            g.drawString("restantes", x, 33);
            g.dispose();
        }
    }

    // Fundo inteiro: escuro com luz de refletor e os quatro cartoes da direita.
    static class Fundo extends JComponent {
        final JPanel cd;
        Fundo(JPanel cd) {
            this.cd = cd;
            setOpaque(true);
        }

        @Override
        public boolean contains(int x, int y) { return false; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Placar.antialias(g);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, new Color(0x161A17), 0, h, new Color(0x0A0C0B)));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(w * 0.35f, -40, w * 0.7f, new float[] {0f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, 26), Tema.alfa(Estreia.OURO, 0)}));
            g.fillRect(0, 0, w, h);
            Placar.cartao(g, DX, 10, DW, 118, 16, true);
            g.setColor(Estreia.LINHA);
            g.fillRect(DX + 18, 69, DW - 36, 1);
            Placar.cartao(g, DX, 138, DW, 76, 16, false);
            g.setColor(Estreia.LINHA);
            g.fillRect(DX + 72, 150, 1, 52);
            minuto(g, Placar.texto(Dialogos.campo(cd, "Dv")), Placar.texto(Dialogos.campo(cd, "DE")));
            Placar.cartao(g, DX, 224, DW, 300, 16, false);
            Placar.cartao(g, DX, 534, DW, 128, 16, false);
            g.setColor(new Color(244, 241, 234, 24));
            g.fillRect(14, 666, DX - 28, 1);
            g.setColor(new Color(244, 241, 234, 40));
            g.drawRect(0, 0, w - 1, h - 1);
            g.dispose();
        }

        // "46'" grande com "1º TEMPO" embaixo; texto (Intervalo, Fim) vira chip dourado.
        void minuto(Graphics2D g, String m, String t) {
            int cx = DX + 36;
            if (m.matches(".*\\d.*")) {
                g.setFont(Estreia.titulo(30f));
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.setColor(Estreia.OURO);
                g.drawString(m, cx - fm.stringWidth(m) / 2f, 182);
                String s = t.length() > 0 ? t.toUpperCase() + " T" : "";
                g.setFont(Estreia.espacada(Estreia.negrito(9f), 0.14f));
                fm = g.getFontMetrics();
                g.setColor(Estreia.FRACO);
                g.drawString(s, cx - fm.stringWidth(s) / 2f, 199);
            } else if (m.length() > 0) {
                String s = m.equals("I") ? "INTERVALO" : m.toUpperCase();
                g.setFont(Estreia.espacada(Estreia.negrito(8.5f), 0.1f));
                java.awt.FontMetrics fm = g.getFontMetrics();
                int w = Math.min(64, fm.stringWidth(s) + 12);
                g.setColor(Tema.alfa(Estreia.OURO, 46));
                g.fill(new RoundRectangle2D.Float(cx - w / 2f, 166, w, 20, 20, 20));
                g.setColor(Estreia.OURO);
                g.drawString(s, cx - fm.stringWidth(s) / 2f, 180);
            }
        }
    }
}
