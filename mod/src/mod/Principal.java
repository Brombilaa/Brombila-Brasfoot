package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.table.TableColumn;

// Tela principal (a.eg): painel do time em cartao com nome grande, rotulos dourados,
// cartao do jogador escolhido e, na lista do elenco, a posicao como chip colorido com
// uma linha fina separando goleiros, defesa, meio e ataque.
final class Principal {
    private Principal() {}

    // coluna (indice no modelo) com a posicao, marcada na tabela para Tema.embrulha
    static final String POSICAO = "brombila.posicao";

    static void aplica(JPanel eg) {
        if (eg.getClientProperty(Janelas.FEITO) != null) return;
        eg.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        painelDoTime(Dialogos.campo(eg, "wf"), eg);
        jogador(Dialogos.campo(eg, "If"));
        Object t = Dialogos.campo(eg, "Iy");
        if (t instanceof JTable) elenco((JTable) t);
    }

    private static Object campo(Object dono, String nome) { return Dialogos.campo(dono, nome); }
    private static void pos(Object o, int x, int y, int w, int h) { Dialogos.pos(o, x, y, w, h); }

    // ------------------------------------------------------------- painel do time

    private static void painelDoTime(Object o, JPanel eg) {
        if (!(o instanceof JPanel)) return;
        JPanel wf = (JPanel) o;
        Dialogos.cartao(wf);
        if (!Dialogos.absoluto(wf)) return;
        pos(campo(eg, "Fq"), 14, 14, 60, 60);
        Object nome = campo(eg, "ug");
        Dialogos.fonte(nome, Estreia.titulo(30f), Estreia.BRANCO);
        pos(nome, 84, 16, 200, 34);
        Object tecnico = campo(eg, "a_");
        rotulo(tecnico);
        pos(tecnico, 84, 52, 200, 16);
        pos(campo(eg, "Iw"), 14, 88, 225, 21);
        pos(campo(eg, "ze"), 243, 86, 40, 23);
        Object divisao = campo(eg, "uh");
        Dialogos.fonte(divisao, Estreia.semi(12f), Estreia.FRACO);
        pos(divisao, 14, 113, 264, 17);
        for (String s : new String[] {"ur", "us"}) Dialogos.fonte(campo(eg, s), Estreia.semi(11.5f), Estreia.FRACO);
        pos(campo(eg, "ur"), 14, 142, 125, 16);
        pos(campo(eg, "us"), 151, 142, 125, 16);
        pos(campo(eg, "Hf"), 14, 162, 125, 23);
        pos(campo(eg, "Ig"), 151, 162, 125, 23);
        Object proximo = campo(eg, "vz");
        if (proximo instanceof JLabel) ((JLabel) proximo).setOpaque(false);
        rotulo(proximo);
        pos(proximo, 14, 204, 264, 18);
        pos(campo(eg, "yt"), 14, 230, 60, 60);
        Object rival = campo(eg, "vw");
        Dialogos.fonte(rival, Estreia.titulo(22f), Estreia.BRANCO);
        pos(rival, 84, 240, 160, 26);
        Object comp = campo(eg, "Bj");
        Dialogos.fonte(comp, Estreia.semi(11.5f), Estreia.FRACO);
        pos(comp, 84, 268, 200, 17);
        pos(campo(eg, "zb"), 247, 230, 30, 39);
        wf.revalidate();
    }

    // Rotulo dourado em caixa alta; "Proximo Jogo - Fora" vira "PROXIMO JOGO · FORA".
    private static void rotulo(Object o) {
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setUI(new Rotulo());
        l.setFont(Estreia.espacada(Estreia.negrito(10.5f), 0.16f));
        l.setForeground(Estreia.OURO);
    }

    static class Rotulo extends BasicLabelUI {
        @Override
        protected void paintEnabledText(JLabel l, Graphics g0, String s, int x, int y) {
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String t = l.getText() == null ? "" : l.getText().trim().replace(" - ", "  ·  ").toUpperCase();
            g.setColor(Estreia.OURO);
            g.drawString(t, x, y);
        }
    }

    // ------------------------------------------------------------- jogador escolhido

    private static void jogador(Object o) {
        if (!(o instanceof JPanel)) return;
        JPanel m = (JPanel) o;
        Dialogos.cartao(m);
        Dialogos.fonte(campo(m, "uF"), Estreia.negrito(13.5f), Estreia.BRANCO);
        Object forca = campo(m, "OM");
        if (forca instanceof JLabel) {
            ((JLabel) forca).setUI(new Fichas.Chip());
            ((JLabel) forca).setFont(Estreia.negrito(11f));
            ((JLabel) forca).setForeground(Estreia.OURO);
            ((JLabel) forca).setHorizontalAlignment(SwingConstants.RIGHT);
        }
        for (String s : new String[] {"OT", "yB", "yA"}) Dialogos.fonte(campo(m, s), Estreia.texto(11.5f), Estreia.FRACO);
        for (String s : new String[] {"OZ", "OK", "OL"}) Dialogos.fonte(campo(m, s), Estreia.semi(10.5f), Estreia.OURO);
        for (String s : new String[] {"Pa", "Ec", "yE", "OA", "Oz", "OU"}) Dialogos.fonte(campo(m, s), Estreia.semi(11.5f), Estreia.FRACO);
        for (String s : new String[] {"Pb", "OY"}) Dialogos.fonte(campo(m, s), Estreia.negrito(12f), Estreia.BRANCO);
    }

    // ------------------------------------------------------------- lista do elenco

    private static void elenco(final JTable t) {
        t.putClientProperty(POSICAO, 1);
        Border b = t.getBorder();
        t.setBorder(b == null ? new Setores() : javax.swing.BorderFactory.createCompoundBorder(b, new Setores()));
        larguras(t);
        t.addPropertyChangeListener("model", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent e) {
                javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { larguras(t); } });
            }
        });
    }

    // A coluna Idade ficava cortada ("Id...").
    static void larguras(JTable t) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            TableColumn c = t.getColumnModel().getColumn(i);
            if ("Idade".equals(c.getHeaderValue())) {
                c.setMaxWidth(52);
                c.setPreferredWidth(48);
            }
            if ("P".equals(c.getHeaderValue())) {
                c.setMaxWidth(40);
                c.setPreferredWidth(34);
            }
        }
    }

    static int setor(String p) {
        if (p == null) return -1;
        p = p.trim();
        if (p.equals("G")) return 0;
        if (p.equals("L") || p.equals("Z")) return 1;
        if (p.equals("M")) return 2;
        if (p.equals("A")) return 3;
        return -1;
    }

    static Color cor(int setor) {
        switch (setor) {
            case 0: return new Color(0xF2C14E);
            case 1: return new Color(0x6CB8E6);
            case 2: return new Color(0x7CCB7A);
            case 3: return new Color(0xFF8A6E);
            default: return Estreia.FRACO;
        }
    }

    private static final Posicao celula = new Posicao();

    // Chamado por Tema.embrulha para a celula da posicao.
    static Component chip(JLabel c) {
        celula.texto = c.getText();
        celula.setBackground(c.getBackground());
        celula.setOpaque(c.isOpaque());
        return celula;
    }

    static class Posicao extends JComponent {
        String texto;
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = getWidth(), h = getHeight();
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, w, h);
            }
            String s = texto == null ? "" : texto.trim();
            if (s.isEmpty()) { g.dispose(); return; }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color c = cor(setor(s));
            int cw = 22, ch = Math.min(16, h - 2);
            RoundRectangle2D r = new RoundRectangle2D.Float((w - cw) / 2f, (h - ch) / 2f, cw, ch, 6, 6);
            g.setColor(Tema.alfa(c, 44));
            g.fill(r);
            g.setColor(Tema.alfa(c, 150));
            g.draw(r);
            java.awt.Font f = Estreia.negrito(11f);
            FontMetrics fm = g.getFontMetrics(f);
            g.setFont(f);
            g.setColor(c);
            g.drawString(s, (w - fm.stringWidth(s)) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    // Linha fina dourada entre os setores (goleiros, defesa, meio, ataque).
    static class Setores extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        @Override
        public Insets getBorderInsets(Component c, Insets i) { i.set(0, 0, 0, 0); return i; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            JTable t = (JTable) c;
            int col = t.convertColumnIndexToView(1);
            if (col < 0 || t.getRowCount() < 2) return;
            Rectangle v = t.getVisibleRect();
            int r0 = Math.max(1, t.rowAtPoint(new java.awt.Point(0, v.y)));
            int r1 = t.rowAtPoint(new java.awt.Point(0, v.y + v.height - 1));
            if (r1 < 0) r1 = t.getRowCount() - 1;
            int largura = 0;
            for (int i = 0; i < t.getColumnCount(); i++) largura += t.getColumnModel().getColumn(i).getWidth();
            String antes = texto(t, r0 - 1, col);
            for (int r = r0; r <= r1; r++) {
                String agora = texto(t, r, col);
                if (setor(agora) != setor(antes) && setor(agora) >= 0) {
                    Rectangle cel = t.getCellRect(r, 0, true);
                    g.setColor(Tema.alfa(Estreia.OURO, 70));
                    g.fillRect(0, cel.y, largura, 1);
                }
                antes = agora;
            }
        }

        private static String texto(JTable t, int r, int col) {
            try {
                Object v = t.getValueAt(r, col);
                Component k = t.getCellRenderer(r, col).getTableCellRendererComponent(t, v, false, false, r, col);
                if (k instanceof Posicao) return ((Posicao) k).texto;
                if (k instanceof JLabel) return ((JLabel) k).getText();
            } catch (Throwable e) {
                // sem linha
            }
            return null;
        }
    }
}
