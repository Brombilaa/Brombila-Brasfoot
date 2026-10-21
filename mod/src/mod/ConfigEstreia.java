package mod;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicRadioButtonUI;

// Tela "Configuracoes iniciais" (a.hU) no estilo "Noite de estreia".
// Os componentes do jogo continuam os mesmos (mesmas acoes e o mesmo layout);
// aqui so trocamos fundo, fontes, cores, chaves, opcoes e botoes.
final class ConfigEstreia {
    private ConfigEstreia() {}

    static final Color LINHA_TABELA = new Color(0x1A1F1C);
    static final Color CELULA = new Color(0x0E1210);
    static final Color ZEBRA = new Color(0x121714);
    static final Color CABECALHO = new Color(0x0A0D0C);
    static final Color SELECAO = new Color(0x3A3120);

    private static Object campo(Object dono, String nome) {
        try {
            Field f = dono.getClass().getDeclaredField(nome);
            f.setAccessible(true);
            return f.get(dono);
        } catch (Throwable t) {
            return null;
        }
    }

    static void aplica(final JPanel hU) {
        if (hU.getClientProperty(Estreia.MARCA) != null) return;
        hU.putClientProperty(Estreia.MARCA, Boolean.TRUE);
        hU.setOpaque(false);

        // Fundo: foto do estadio escurecida ocupando a janela toda.
        Container pai = hU.getParent();
        if (pai != null && !(pai instanceof Fundo)) {
            pai.remove(hU);
            pai.setLayout(new BorderLayout());
            Fundo f = new Fundo();
            f.add(hU);
            pai.add(f, BorderLayout.CENTER);
            pai.revalidate();
            pai.repaint();
        }

        for (String n : new String[] {"vd", "we", "wf", "wg"}) {
            Object p = campo(hU, n);
            if (p instanceof JPanel) {
                JPanel pp = (JPanel) p;
                pp.setOpaque(false);
                pp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estreia.LINHA));
            }
        }

        JLabel kicker = (JLabel) campo(hU, "a_");
        if (kicker != null) {
            kicker.setText("NOVO JOGO");
            kicker.setFont(Estreia.espacada(Estreia.semi(12f), 0.3f));
            kicker.setForeground(Estreia.OURO);
            kicker.setHorizontalAlignment(SwingConstants.LEFT);
            kicker.setBorder(BorderFactory.createEmptyBorder(6, 17, 0, 0));
        }
        JLabel titulo = (JLabel) campo(hU, "vf");
        if (titulo != null) {
            titulo.setText("CONFIGURAÇÕES INICIAIS");
            titulo.setFont(Estreia.titulo(44f));
            titulo.setForeground(Estreia.BRANCO);
            titulo.setHorizontalAlignment(SwingConstants.LEFT);
            titulo.setBorder(BorderFactory.createEmptyBorder(0, 17, 4, 0));
        }

        secao(hU, "Mo", "LIGAS NACIONAIS");
        secao(hU, "Mt", "COMPETIÇÕES INTERNACIONAIS");
        secao(hU, "Mp", "ESTADUAIS");
        secao(hU, "Mq", "REGIONAIS");
        secao(hU, "Mr", "SISTEMA DE SALÁRIOS");
        secao(hU, "Ms", "FORÇA DOS JOGADORES");
        secao(hU, "afB", "TEMPORADA DE INÍCIO");

        for (String n : new String[] {"Mh", "afz", "Mi", "Mj", "Mk"}) {
            Object o = campo(hU, n);
            if (o instanceof JCheckBox) {
                JCheckBox c = (JCheckBox) o;
                c.setFont(Estreia.texto(14f));
                c.setForeground(Estreia.BRANCO);
                c.setUI(new Interruptor());
                c.setOpaque(false);
            }
        }
        for (String n : new String[] {"Mu", "Mv", "Mw", "Mx"}) {
            Object o = campo(hU, n);
            if (o instanceof JRadioButton) {
                JRadioButton r = (JRadioButton) o;
                r.setUI(new Pilula());
                r.setFont(Estreia.semi(13f));
                r.setRolloverEnabled(true);
                r.setOpaque(false);
            }
        }

        botao(hU, "Bw", "Iniciar jogo", "seta", true);
        botao(hU, "Mg", "Voltar ao início", "volta", false);
        botao(hU, "Md", "Configurar", "ajuste", false);
        botao(hU, "Mb", "Configurar", "ajuste", false);
        botao(hU, "Me", "Configurar", "ajuste", false);
        botao(hU, "Mc", "Preferências", "ajuste", false);
        botao(hU, "Mf", null, "todos", false);

        for (String n : new String[] {"ut", "My"}) {
            Object o = campo(hU, n);
            if (o instanceof JScrollPane) {
                JScrollPane s = (JScrollPane) o;
                s.setBorder(BorderFactory.createLineBorder(Estreia.LINHA));
                s.getViewport().setBackground(CELULA);
                s.setOpaque(false);
            }
        }
        for (String n : new String[] {"JG", "Mz"}) {
            Object o = campo(hU, n);
            if (o instanceof JTable) {
                JTable t = (JTable) o;
                t.setBackground(CELULA);
                t.setForeground(Estreia.BRANCO);
                t.setGridColor(LINHA_TABELA);
                t.setFont(Estreia.texto(13f));
                if (t.getTableHeader() != null) t.getTableHeader().setFont(Estreia.espacada(Estreia.semi(11f), 0.12f));
            }
        }

        Object combo = campo(hU, "afA");
        if (combo instanceof javax.swing.JComboBox) {
            javax.swing.JComboBox<?> cb = (javax.swing.JComboBox<?>) combo;
            cb.setFont(Estreia.semi(13f));
            cb.setBackground(CELULA);
            cb.setForeground(Estreia.BRANCO);
            cb.setBorder(BorderFactory.createLineBorder(new Color(244, 241, 234, 46)));
        }
        // O titulo maior pede um pouco mais de altura do que os 728 px originais.
        hU.setPreferredSize(null);

        JLabel nota = (JLabel) campo(hU, "ug");
        if (nota != null) {
            nota.setFont(Estreia.texto(12f));
            nota.setForeground(Estreia.FRACO);
        }
        hU.revalidate();
        hU.repaint();
    }

    private static void secao(JPanel hU, String nome, String texto) {
        Object o = campo(hU, nome);
        if (!(o instanceof JLabel)) return;
        JLabel l = (JLabel) o;
        l.setText(texto);
        l.setOpaque(false);
        l.setFont(Estreia.espacada(Estreia.semi(11.5f), 0.2f));
        l.setForeground(Estreia.FRACO);
    }

    private static void botao(JPanel hU, String nome, String texto, String icone, boolean primario) {
        Object o = campo(hU, nome);
        if (!(o instanceof JButton)) return;
        JButton b = (JButton) o;
        b.putClientProperty(BotaoUI.PRIMARIO, primario ? Boolean.TRUE : Boolean.FALSE);
        if (texto != null) b.setText(texto);
        b.setFont(Estreia.negrito(13f));
        b.setIcon(new Estreia.Icone(icone, 16, primario ? Estreia.OURO_ESCURO : Estreia.OURO));
        b.setDisabledIcon(new Estreia.Icone(icone, 16, Estreia.APAGADO));
        b.setIconTextGap(8);
        if (primario) b.setHorizontalTextPosition(SwingConstants.LEADING);
        b.setFocusPainted(false);
    }

    // ------------------------------------------------------------- fundo

    static class Fundo extends JPanel {
        private BufferedImage cache;

        Fundo() {
            super(new GridBagLayout());
            putClientProperty(Estreia.MARCA, Boolean.TRUE);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            if (cache == null || cache.getWidth() != w || cache.getHeight() != h) cache = desenha(w, h);
            g.drawImage(cache, 0, 0, null);
        }

        private static BufferedImage desenha(int w, int h) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            BufferedImage f = Estreia.foto();
            double esc = Math.max(w / (double) f.getWidth(), h / (double) f.getHeight()) * 1.06;
            int fw = (int) Math.round(f.getWidth() * esc), fh = (int) Math.round(f.getHeight() * esc);
            g.drawImage(f, (w - fw) / 2, (h - fh) / 2, fw, fh, null);
            g.setPaint(new LinearGradientPaint(0, 0, 0, h, new float[] {0f, 1f},
                new Color[] {new Color(6, 9, 8, 224), new Color(6, 9, 8, 247)}));
            g.fillRect(0, 0, w, h);
            float rx = 0.45f * w, ry = 0.35f * h;
            AffineTransform el = new AffineTransform();
            el.translate(0.72f * w, 0.02f * h);
            el.scale(1.0, ry / rx);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(0, 0), rx, new Point2D.Float(0, 0), new float[] {0f, 1f},
                new Color[] {new Color(242, 193, 78, 30), new Color(242, 193, 78, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE, MultipleGradientPaint.ColorSpaceType.SRGB, el));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return img;
        }
    }

    // ------------------------------------------- chave liga/desliga (checkbox)

    private static final Map<AbstractButton, float[]> posicoes = new WeakHashMap<AbstractButton, float[]>();
    private static Timer animacao;

    static class Chave implements Icon {
        public int getIconWidth() { return 34; }
        public int getIconHeight() { return 18; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            boolean ativo = m.isEnabled();
            float alvo = m.isSelected() ? 1f : 0f;
            float[] p = posicoes.get(b);
            if (p == null) { p = new float[] {alvo}; posicoes.put(b, p); }
            if (p[0] != alvo) anima();
            float t = Animacao.suave(p[0]);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D trilho = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, 33, 17, 17, 17);
            Color desl = new Color(244, 241, 234, ativo ? (m.isRollover() ? 60 : 40) : 18);
            Color lig = ativo ? Estreia.OURO : Tema.alfa(Estreia.OURO, 90);
            g2.setColor(Tema.mistura(desl, lig, t));
            g2.fill(trilho);
            float kx = x + 2.5f + 16f * t;
            Color bola = Tema.mistura(Estreia.BRANCO, Estreia.OURO_ESCURO, t);
            g2.setColor(ativo ? bola : Tema.alfa(bola, 120));
            g2.fill(new Ellipse2D.Float(kx, y + 2.5f, 13, 13));
            g2.dispose();
        }
    }

    private static void anima() {
        if (animacao == null) {
            animacao = new Timer(Animacao.QUADRO_MS, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean mexendo = false;
                    for (Iterator<Map.Entry<AbstractButton, float[]>> it = posicoes.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<AbstractButton, float[]> en = it.next();
                        AbstractButton b = en.getKey();
                        if (b == null) continue;
                        float alvo = b.isSelected() ? 1f : 0f;
                        float[] p = en.getValue();
                        if (p[0] == alvo) continue;
                        p[0] = p[0] < alvo ? Math.min(alvo, p[0] + 0.09f) : Math.max(alvo, p[0] - 0.09f);
                        b.repaint();
                        mexendo = true;
                    }
                    if (!mexendo) animacao.stop();
                }
            });
        }
        if (!animacao.isRunning()) animacao.start();
    }

    // Caixa de marcar como chave liga/desliga; desativada (jogo nao registrado) fica apagada.
    static class Interruptor extends javax.swing.plaf.basic.BasicCheckBoxUI {
        private final Chave chave = new Chave();

        @Override
        public Dimension getPreferredSize(JComponent c) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = b.getFontMetrics(b.getFont());
            return new Dimension(chave.getIconWidth() + 12 + fm.stringWidth(b.getText()) + 4, Math.max(24, fm.getHeight() + 6));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int h = c.getHeight();
            chave.paintIcon(c, g2, 0, (h - chave.getIconHeight()) / 2);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            g2.setFont(b.getFont());
            g2.setColor(!b.isEnabled() ? Estreia.APAGADO : b.getModel().isRollover() ? Color.WHITE : Estreia.BRANCO);
            g2.drawString(b.getText(), chave.getIconWidth() + 12, (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    // -------------------------------------------- opcoes (radio) em pilula

    static class Pilula extends BasicRadioButtonUI {
        @Override
        public Dimension getPreferredSize(JComponent c) {
            AbstractButton b = (AbstractButton) c;
            FontMetrics fm = b.getFontMetrics(b.getFont());
            return new Dimension(fm.stringWidth(b.getText()) + 32, Math.max(30, fm.getHeight() + 14));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            FontMetrics fm = g2.getFontMetrics(b.getFont());
            String s = b.getText();
            int tw = fm.stringWidth(s);
            int h = Math.min(c.getHeight() - 2, 30), pw = tw + 30;
            int y = (c.getHeight() - h) / 2;
            RoundRectangle2D pil = new RoundRectangle2D.Float(0.5f, y + 0.5f, pw - 1, h - 1, h, h);
            Color fg;
            if (m.isSelected()) {
                g2.setColor(m.isEnabled() ? Estreia.BRANCO : Tema.alfa(Estreia.BRANCO, 90));
                g2.fill(pil);
                fg = new Color(0x121411);
            } else {
                g2.setColor(new Color(244, 241, 234, m.isRollover() && m.isEnabled() ? 110 : 46));
                g2.draw(pil);
                fg = !m.isEnabled() ? Estreia.APAGADO : m.isRollover() ? Estreia.BRANCO : Estreia.FRACO;
            }
            g2.setFont(b.getFont());
            g2.setColor(fg);
            g2.drawString(s, 15, y + (h - fm.getHeight()) / 2f + fm.getAscent());
            g2.dispose();
        }
    }
}
