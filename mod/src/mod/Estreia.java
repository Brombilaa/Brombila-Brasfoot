package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JComponent;

// Estilo "Noite de estreia" do menu inicial e das Configuracoes iniciais:
// foto do estadio escurecida, titulo condensado (Saira Extra Condensed),
// texto Manrope e detalhes dourados de refletor.
final class Estreia {
    private Estreia() {}

    static final Color OURO = new Color(0xF2C14E);
    static final Color OURO_ESCURO = new Color(0x1A1408);
    static final Color BRANCO = new Color(0xF4F1EA);
    static final Color FRACO = new Color(244, 241, 234, 158);
    static final Color APAGADO = new Color(244, 241, 234, 102);
    static final Color LINHA = new Color(244, 241, 234, 36);
    static final Color NOITE = new Color(0x060908);

    // Marca os paineis no estilo: o tema "Noite de jogo" nao troca as cores deles.
    static final String MARCA = "brombila.estreia";

    static boolean dentro(Component c) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (p instanceof JComponent && ((JComponent) p).getClientProperty(MARCA) != null) return true;
        }
        return false;
    }

    // ------------------------------------------------------------- fontes

    private static final Map<String, Font> fontes = new HashMap<String, Font>();

    private static Font base(String arquivo, String reserva, int estilo) {
        Font f = fontes.get(arquivo);
        if (f != null) return f;
        InputStream in = null;
        try {
            in = Estreia.class.getResourceAsStream("/mod/fontes/" + arquivo);
            f = Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Throwable t) {
            f = new Font(reserva, estilo, 12);
        } finally {
            try { if (in != null) in.close(); } catch (Throwable t) {}
        }
        fontes.put(arquivo, f);
        return f;
    }

    static Font titulo(float px) { return base("SairaExtraCondensed-ExtraBold.ttf", "Dialog", Font.BOLD).deriveFont(px); }
    static Font texto(float px) { return base("Manrope-Medium.ttf", "Dialog", Font.PLAIN).deriveFont(px); }
    static Font semi(float px) { return base("Manrope-SemiBold.ttf", "Dialog", Font.BOLD).deriveFont(px); }
    static Font negrito(float px) { return base("Manrope-Bold.ttf", "Dialog", Font.BOLD).deriveFont(px); }

    // Letras espacadas (rotulos em caixa alta).
    static Font espacada(Font f, float em) {
        Map<TextAttribute, Object> a = new HashMap<TextAttribute, Object>();
        a.put(TextAttribute.TRACKING, em);
        return f.deriveFont(a);
    }

    // ---------------------------------------------------------------- foto

    private static BufferedImage foto;

    static BufferedImage foto() {
        if (foto == null) {
            try {
                foto = ImageIO.read(Estreia.class.getResource("/mod/estadio.jpg"));
            } catch (Throwable t) {
                foto = new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB);
            }
        }
        return foto;
    }

    // -------------------------------------------------------------- icones

    // Icones de linha (grade 24x24, traco 1.7), os mesmos do modelo aprovado.
    static final Map<String, Shape[]> ICONES = new HashMap<String, Shape[]>();
    static {
        ICONES.put("bola", new Shape[] {new Ellipse2D.Double(3, 3, 18, 18),
            svg("M12 7.5l4.3 3.1-1.6 5H9.3l-1.6-5z"),
            svg("M12 3v4.5M16.3 10.6l4.2-1.5M14.7 15.6l2.6 3.6M9.3 15.6l-2.6 3.6M7.7 10.6L3.5 9.1")});
        ICONES.put("pasta", new Shape[] {
            svg("M3 7.5A1.5 1.5 0 0 1 4.5 6h4l2 2.2h9A1.5 1.5 0 0 1 21 9.7v8.8a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5z"),
            svg("M8 14h8M12 11.5v5")});
        ICONES.put("lapis", new Shape[] {svg("M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z"), svg("M13.5 6.5l4 4M4 20l4.5-1-3.5-3.5z")});
        ICONES.put("chave", new Shape[] {new Ellipse2D.Double(4, 11, 8, 8), svg("M11 12l8-8M16 7l2.5 2.5M14 9l2 2")});
        ICONES.put("sair", new Shape[] {svg("M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4"), svg("M10 8l-4 4 4 4M6 12h10")});
        ICONES.put("seta", new Shape[] {svg("M5 12h14M13 6l6 6-6 6")});
        ICONES.put("volta", new Shape[] {svg("M19 12H5M11 6l-6 6 6 6")});
        ICONES.put("ajuste", new Shape[] {svg("M4 7h10M18 7h2M4 17h4M12 17h8"),
            new Ellipse2D.Double(14, 5, 4, 4), new Ellipse2D.Double(8, 15, 4, 4)});
        ICONES.put("mais", new Shape[] {svg("M12 5v14M5 12h14")});
        ICONES.put("fechar", new Shape[] {svg("M7 7l10 10M17 7L7 17")});
        ICONES.put("busca", new Shape[] {new Ellipse2D.Double(4.5, 4.5, 11, 11), svg("M14 14l5.5 5.5")});
        ICONES.put("cima", new Shape[] {svg("M12 19V5M6 11l6-6 6 6")});
        ICONES.put("etiqueta", new Shape[] {svg("M3.5 12.5V4.5a1 1 0 0 1 1-1h8l8 8-9 9z"), new Ellipse2D.Double(7, 7, 3, 3)});
        ICONES.put("obra", new Shape[] {svg("M3 20h18M5 20V11l7-5 7 5v9M9.5 20v-5h5v5")});
        ICONES.put("todos", new Shape[] {svg("M4 6.5l2 2 3.5-4M4 13.5l2 2 3.5-4M12 7h8M12 14h8M12 20h8")});
    }

    // Desenha o icone com o canto superior esquerdo em (x, y) e lado "lado".
    static void icone(Graphics2D g, String nome, double x, double y, double lado, Color cor) {
        Shape[] s = ICONES.get(nome);
        if (s == null) s = Menus.ICONES.get(nome); // icones dos menus (calendario, taca...)
        if (s == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(lado / 24.0, lado / 24.0);
        g2.setColor(cor);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Shape f : s) g2.draw(f);
        g2.dispose();
    }

    // Icone Swing com cor fixa ou a cor do texto do componente.
    static class Icone implements Icon {
        final String nome;
        final int lado;
        final Color cor;
        Icone(String nome, int lado, Color cor) { this.nome = nome; this.lado = lado; this.cor = cor; }
        public int getIconWidth() { return lado; }
        public int getIconHeight() { return lado; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            icone((Graphics2D) g, nome, x, y, lado, cor != null ? cor : c.getForeground());
        }
    }

    // Icone das perguntas e avisos do jogo (JOptionPane): circulo fino com o sinal.
    static class Aviso implements Icon {
        final String sinal;
        final Color cor;
        Aviso(String sinal, Color cor) { this.sinal = sinal; this.cor = cor; }
        public int getIconWidth() { return 34; }
        public int getIconHeight() { return 34; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 36));
            g2.fill(new Ellipse2D.Float(x + 1, y + 1, 32, 32));
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new Ellipse2D.Float(x + 1.5f, y + 1.5f, 31, 31));
            g2.setFont(negrito(18f));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sinal, x + 17 - fm.stringWidth(sinal) / 2f, y + 17 - fm.getHeight() / 2f + fm.getAscent());
            g2.dispose();
        }
    }

    // ------------------------------------------------- caminhos SVG simples

    static Shape svg(String d) {
        Path2D p = new Path2D.Double();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[MmLlHhVvAaZz]|-?\\d*\\.?\\d+").matcher(d);
        List<String> tk = new ArrayList<String>();
        while (m.find()) tk.add(m.group());
        double x = 0, y = 0, sx = 0, sy = 0;
        char cmd = 'M';
        int i = 0;
        while (i < tk.size()) {
            String s0 = tk.get(i);
            if (Character.isLetter(s0.charAt(0))) {
                cmd = s0.charAt(0);
                i++;
                if (cmd == 'z' || cmd == 'Z') { p.closePath(); x = sx; y = sy; continue; }
            }
            boolean rel = Character.isLowerCase(cmd);
            switch (Character.toUpperCase(cmd)) {
                case 'M': {
                    double nx = num(tk, i++), ny = num(tk, i++);
                    if (rel) { nx += x; ny += y; }
                    p.moveTo(nx, ny); x = sx = nx; y = sy = ny;
                    cmd = rel ? 'l' : 'L';
                    break;
                }
                case 'L': {
                    double nx = num(tk, i++), ny = num(tk, i++);
                    if (rel) { nx += x; ny += y; }
                    p.lineTo(nx, ny); x = nx; y = ny;
                    break;
                }
                case 'H': { double nx = num(tk, i++); if (rel) nx += x; p.lineTo(nx, y); x = nx; break; }
                case 'V': { double ny = num(tk, i++); if (rel) ny += y; p.lineTo(x, ny); y = ny; break; }
                case 'A': {
                    double rx = num(tk, i++), ry = num(tk, i++), rot = num(tk, i++), la = num(tk, i++), sw = num(tk, i++);
                    double nx = num(tk, i++), ny = num(tk, i++);
                    if (rel) { nx += x; ny += y; }
                    arco(p, x, y, rx, ry, rot, la != 0, sw != 0, nx, ny); x = nx; y = ny;
                    break;
                }
                default: i++;
            }
        }
        return p;
    }

    private static double num(List<String> tk, int i) { return Double.parseDouble(tk.get(i)); }

    // Arco SVG (forma de ponto final) convertido para o centro (especificacao SVG F.6.5).
    private static void arco(Path2D p, double x1, double y1, double rx, double ry, double rotGraus,
                             boolean grande, boolean horario, double x2, double y2) {
        double phi = Math.toRadians(rotGraus), c = Math.cos(phi), s = Math.sin(phi);
        double dx = (x1 - x2) / 2, dy = (y1 - y2) / 2;
        double x1p = c * dx + s * dy, y1p = -s * dx + c * dy;
        rx = Math.abs(rx); ry = Math.abs(ry);
        double lam = x1p * x1p / (rx * rx) + y1p * y1p / (ry * ry);
        if (lam > 1) { rx *= Math.sqrt(lam); ry *= Math.sqrt(lam); }
        double n = rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p;
        double k = Math.sqrt(Math.max(0, n / (rx * rx * y1p * y1p + ry * ry * x1p * x1p))) * (grande == horario ? -1 : 1);
        double cxp = k * rx * y1p / ry, cyp = -k * ry * x1p / rx;
        double cx = c * cxp - s * cyp + (x1 + x2) / 2, cy = s * cxp + c * cyp + (y1 + y2) / 2;
        double t1 = Math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx), t2 = Math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);
        double dt = t2 - t1;
        if (horario && dt < 0) dt += 2 * Math.PI;
        if (!horario && dt > 0) dt -= 2 * Math.PI;
        for (int j = 1; j <= 12; j++) {
            double t = t1 + dt * j / 12;
            double ex = rx * Math.cos(t), ey = ry * Math.sin(t);
            p.lineTo(c * ex - s * ey + cx, s * ex + c * ey + cy);
        }
    }

}
