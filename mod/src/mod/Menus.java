package mod;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

// Menus suspensos no estilo "Noite de estreia": painel escuro com borda fina, itens altos
// com icone de linha dourado (escolhido pelo texto do item), texto Manrope, atalho (F2, F3...)
// numa etiqueta a direita e item sob o mouse com fundo dourado translucido e barrinha a esquerda.
// Itens de sair/demitir/apagar ganham icone vermelho.
final class Menus {
    private Menus() {}

    static final Color FUNDO = new Color(0x121614);
    static final Color PERIGO = new Color(0xFF7A6E);
    private static final int ALTURA = 34, LADO_ICONE = 18, MARGEM = 12, VAO = 11;

    // ------------------------------------------------------------- icones (grade 24x24)

    static final Map<String, Shape[]> ICONES = new HashMap<String, Shape[]>();

    private static void icone(String nome, Object... partes) {
        Shape[] s = new Shape[partes.length];
        for (int i = 0; i < partes.length; i++) s[i] = partes[i] instanceof Shape ? (Shape) partes[i] : Estreia.svg((String) partes[i]);
        ICONES.put(nome, s);
    }

    private static Ellipse2D circ(double cx, double cy, double r) { return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r); }

    static {
        icone("calendario", "M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v12a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5z",
            "M4 10h16M8 3v4M16 3v4", "M8 14h2M14 14h2M8 17h2");
        // aperto de mao (desenho baseado no icone "handshake" do Lucide, licenca ISC)
        icone("amistoso", "M11 17l2 2a1 1 0 1 0 3-3",
            "M14 14l2.5 2.5a1 1 0 1 0 3-3l-3.88-3.88a3 3 0 0 0-4.24 0l-.88.88a1 1 0 1 1-3-3l2.81-2.81a5.79 5.79 0 0 1 7.06-.87l.47.28a2 2 0 0 0 1.42.25L21 4",
            "M21 3l1 11h-2", "M3 3L2 14l6.5 6.5a1 1 0 1 0 3-3", "M3 4h8");
        icone("maleta", "M4 8.5A1.5 1.5 0 0 1 5.5 7h13A1.5 1.5 0 0 1 20 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 17.5z",
            "M9 7V5.5A1.5 1.5 0 0 1 10.5 4h3A1.5 1.5 0 0 1 15 5.5V7", "M4 12.5h16", "M11 12.5v1.8h2v-1.8");
        icone("disquete", "M5 5.5A1.5 1.5 0 0 1 6.5 4H16l3 3v11.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 18.5z",
            "M8 4v4h7V4", "M8 20v-6h8v6");
        icone("engrenagem", circ(12, 12, 3), circ(12, 12, 6.5),
            "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1");
        icone("demissao", "M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4", "M10 8l-4 4 4 4M6 12h10");
        icone("desligar", "M7.1 6.6A7.5 7.5 0 1 0 16.9 6.6", "M12 3v8");
        icone("info", circ(12, 12, 9), "M12 11v6", "M12 7.6v.3");
        icone("ranking", "M3 20h18", "M9 20V8h6v12", "M3 20v-7h6", "M21 20v-9h-6", "M12 4.5v.1");
        icone("tecnicos", circ(9.5, 8, 3.5), "M3 20v-1a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5v1", "M19 4.5v5M16.5 7h5", "M16.5 12.5h5");
        icone("trocas", "M4 8h15M15 4l4 4-4 4", "M20 16H5M9 12l-4 4 4 4");
        icone("taca", "M8 4h8v5a4 4 0 0 1-8 0z", "M8 6H5.5a2.5 2.5 0 0 0 2.6 4.2M16 6h2.5a2.5 2.5 0 0 1-2.6 4.2",
            "M12 13v4M8.5 20.5h7M9.5 17h5v3.5h-5z");
        icone("bola", circ(12, 12, 9), "M12 7.5l4.3 3.1-1.6 5H9.3l-1.6-5z",
            "M12 3v4.5M16.3 10.6l4.2-1.5M14.7 15.6l2.6 3.6M9.3 15.6l-2.6 3.6M7.7 10.6L3.5 9.1");
        icone("assistencia", circ(17, 7, 4), "M3.5 20.5l9-9", "M3.5 20.5h5.5M3.5 20.5V15");
        icone("estrela", "M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8-4.3-4.1 5.9-.9z");
        icone("medalha", circ(12, 15, 5.5), "M8.4 10.9L5.5 3h4.2l2.3 5 2.3-5h4.2l-2.9 7.9", "M12 12.8l.8 1.6 1.7.2-1.2 1.2.3 1.7-1.6-.8-1.6.8.3-1.7-1.2-1.2 1.7-.2z");
        icone("bolaOuro", circ(12, 13, 8), "M8 11a4.5 4.5 0 0 1 3.2-2.8", "M19.5 2v4M17.5 4h4");
        icone("vender", "M14.5 8.2A3 2.5 0 0 0 11.5 7a3 2.5 0 0 0 0 5 3 2.5 0 0 1 0 5 3 2.5 0 0 1-3-1.3", "M11.5 4.5v15",
            "M19.5 11V4M17 6.5l2.5-2.5 2.5 2.5");
        icone("comprar", "M14.5 8.2A3 2.5 0 0 0 11.5 7a3 2.5 0 0 0 0 5 3 2.5 0 0 1 0 5 3 2.5 0 0 1-3-1.3", "M11.5 4.5v15",
            "M19.5 4v7M17 8.5l2.5 2.5 2.5-2.5");
        icone("contrato", "M6 4.5A1.5 1.5 0 0 1 7.5 3H14l4 4v12.5a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 19.5z",
            "M14 3v4h4", "M9 11h6M9 14h6M9 17h3.5");
        icone("emprestimo", "M6.5 3h11M6.5 21h11", "M8 3v3.5L12 12l-4 5.5V21M16 3v3.5L12 12l4 5.5V21", "M10 18.5h4");
        icone("cancelaEmprestimo", "M6.5 3h11M6.5 21h11", "M8 3v3.5L12 12l-4 5.5V21M16 3v3.5L12 12l4 5.5V21", "M3 3l18 18");
        icone("historico", "M3.6 12A8.5 8.5 0 1 0 6 6", "M3 3.5V8h4.5", "M12 8v4.5l3 2");
        icone("confrontos", circ(8.5, 12, 5.5), circ(15.5, 12, 5.5));
        icone("camisa", "M8.5 4L3 7l2 4.5 2-1V20h10v-9.5l2 1L21 7l-5.5-3a3.5 3 0 0 1-7 0z", "M11 13h2v4");
        icone("lapis", "M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z", "M13.5 6.5l4 4M4 20l4.5-1-3.5-3.5z");
        icone("lixeira", "M4 7h16M10 4h4", "M6 7l1 13h10l1-13", "M10 11v6M14 11v6");
        icone("jogadorMais", circ(9.5, 8, 3.5), "M3 20v-1a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5v1", "M19 9v6M16 12h6");
        icone("aposentar", "M3 18h18", "M7 18a5 5 0 0 1 10 0", "M12 8v2.5M5.3 11.3l1.8 1.8M18.7 11.3l-1.8 1.8", "M8 21h8");
        icone("bandeira", "M5 21V3.5", "M5 4h12l-2.5 4L17 12H5");
        icone("alvo", circ(12, 12, 9), circ(12, 12, 5), circ(12, 12, 1.2));
        icone("capitao", "M4.5 7h15A1.5 1.5 0 0 1 21 8.5v7a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 15.5v-7A1.5 1.5 0 0 1 4.5 7z",
            "M14.2 9.8a3 3 0 1 0 0 4.4");
        icone("escanteio", "M3 20h18", "M8 20V4", "M8 4.5l9 3-9 3", "M3 15a5 5 0 0 1 5 5");
        icone("falso9", circ(12, 9, 4.5), "M16.5 9v4.5a5.5 5.5 0 0 1-5.5 5.5H8.5");
        icone("estadio", new Ellipse2D.Double(3, 5, 18, 9), "M3 9.5V16.5C3 19 7 21 12 21C17 21 21 19 21 16.5V9.5",
            "M8 13.6v6.8M16 13.6v6.8");
        icone("janela", "M4.5 4h15A1.5 1.5 0 0 1 21 5.5v13a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18.5v-13A1.5 1.5 0 0 1 4.5 4z",
            "M3 8.5h18", "M6 6.3h.1M8.5 6.3h.1");
        icone("telaCheia", "M3.5 9V3.5H9M15 3.5h5.5V9M20.5 15v5.5H15M9 20.5H3.5V15");
        icone("torcida", circ(7, 9, 2.5), circ(17, 9, 2.5), circ(12, 7.5, 3), "M2.5 19v-1a4 4 0 0 1 4-4h1",
            "M21.5 19v-1a4 4 0 0 0-4-4h-1", "M6.5 20v-1.5a4.5 4.5 0 0 1 4.5-4.5h2a4.5 4.5 0 0 1 4.5 4.5V20");
    }

    private static final String[][] REGRAS = {
        {"salvar", "disquete"}, {"prefer", "engrenagem"}, {"demiss", "demissao"}, {"sair", "desligar"},
        {"sobre", "info"}, {"ranking", "ranking"}, {"adicionar/remover", "tecnicos"}, {"trocas", "trocas"},
        {"trofeu", "taca"}, {"campeo", "taca"}, {"galeria", "taca"}, {"artilh", "bola"}, {"assist", "assistencia"},
        {"time da rodada", "estrela"}, {"time do ano", "medalha"}, {"bola de ouro", "bolaOuro"},
        {"transfer", "trocas"}, {"vender", "vender"}, {"comprar", "comprar"}, {"contrato", "contrato"},
        {"empregos", "maleta"}, {"cancelar empr", "cancelaEmprestimo"}, {"emprest", "emprestimo"},
        {"confronto", "confrontos"}, {"historic", "historico"}, {"numero", "camisa"}, {"editar", "lapis"},
        {"deletar", "lixeira"}, {"adicionar jogador", "jogadorMais"}, {"aposentar", "aposentar"},
        {"convocar", "bandeira"}, {"calend", "calendario"}, {"jogos do seu time", "calendario"},
        {"amistoso", "amistoso"}, {"falta", "alvo"}, {"capit", "capitao"}, {"escanteio", "escanteio"},
        {"falso 9", "falso9"}, {"modo janela", "janela"}, {"tela cheia", "telaCheia"},
    };

    static String normaliza(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    static String iconeDe(String texto) {
        String t = normaliza(texto);
        for (String[] r : REGRAS) if (t.contains(r[0])) return r[1];
        return null;
    }

    static boolean perigoso(String texto) {
        String t = normaliza(texto);
        return t.contains("demiss") || t.startsWith("sair") || t.contains("deletar") || t.contains("aposentar");
    }

    static void desenha(Graphics2D g, String nome, double x, double y, double lado, Color cor) {
        Shape[] s = ICONES.get(nome);
        if (s == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(lado / 24.0, lado / 24.0);
        g2.setColor(cor);
        g2.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Shape f : s) g2.draw(f);
        g2.dispose();
    }

    static String atalho(KeyStroke k) {
        if (k == null) return null;
        String m = "";
        int mod = k.getModifiers();
        if ((mod & InputEvent.CTRL_DOWN_MASK) != 0 || (mod & InputEvent.CTRL_MASK) != 0) m += "Ctrl+";
        if ((mod & InputEvent.ALT_DOWN_MASK) != 0 || (mod & InputEvent.ALT_MASK) != 0) m += "Alt+";
        if ((mod & InputEvent.SHIFT_DOWN_MASK) != 0 || (mod & InputEvent.SHIFT_MASK) != 0) m += "Shift+";
        return m + KeyEvent.getKeyText(k.getKeyCode());
    }

    // ------------------------------------------------------------------ entrada

    // Ao abrir, os itens descem 6 px e aparecem em cascata (14 ms entre um e outro, 130 ms cada).
    static final String ABRIU = "brombila.menuAbriu";
    static final int DURACAO_ENTRADA = 600;

    // ms desde que o menu abriu (grande = ja terminou a entrada).
    static long aberto(JComponent popup) {
        Object t = popup.getClientProperty(ABRIU);
        return t instanceof Long ? System.currentTimeMillis() - (Long) t : Long.MAX_VALUE / 2;
    }

    static float entrada(JComponent item) {
        java.awt.Container p = item.getParent();
        if (!(p instanceof javax.swing.JPopupMenu)) return 1f;
        long e = aberto((JComponent) p);
        if (e > DURACAO_ENTRADA) return 1f;
        int i = 0;
        for (java.awt.Component c : p.getComponents()) { if (c == item) break; i++; }
        return Animacao.suave((e - 14f * Math.min(i, 14)) / 130f);
    }

    // ------------------------------------------------------------------ item

    public static class Item extends BasicMenuItemUI {
        public static ComponentUI createUI(JComponent c) { return new Item(); }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            menuItem.setBorder(null);
            menuItem.setBorderPainted(false);
            menuItem.setOpaque(false);
            menuItem.setFont(Estreia.semi(12.5f));
        }

        private static Font fonteAtalho() { return Estreia.semi(10.5f); }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            JMenuItem m = (JMenuItem) c;
            FontMetrics fm = m.getFontMetrics(m.getFont());
            int w = MARGEM + LADO_ICONE + VAO + fm.stringWidth(m.getText() == null ? "" : m.getText()) + MARGEM + 8;
            String a = atalho(m.getAccelerator());
            if (a != null) w += 18 + m.getFontMetrics(fonteAtalho()).stringWidth(a) + 10;
            return new Dimension(Math.max(w, 190), ALTURA);
        }

        @Override
        public Dimension getMinimumSize(JComponent c) { return getPreferredSize(c); }

        @Override
        public Dimension getMaximumSize(JComponent c) {
            return new Dimension(Integer.MAX_VALUE, ALTURA);
        }

        @Override
        public void update(Graphics g, JComponent c) { paint(g, c); }

        @Override
        public void paint(Graphics g0, JComponent c) {
            JMenuItem m = (JMenuItem) c;
            ButtonModel bm = m.getModel();
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            int w = m.getWidth(), h = m.getHeight();
            float ent = entrada(m);
            if (ent <= 0f) { g.dispose(); return; }
            if (ent < 1f) {
                g.translate(0, -6f * (1f - ent));
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, ent));
            }
            boolean ativo = m.isEnabled() && (bm.isArmed() || bm.isSelected() || bm.isRollover());
            boolean perigo = perigoso(m.getText());
            Color acento = perigo ? PERIGO : Estreia.OURO;
            if (ativo) {
                g.setColor(Tema.alfa(acento, 34));
                g.fill(new RoundRectangle2D.Float(4, 2, w - 8, h - 4, 10, 10));
                g.setColor(acento);
                g.fill(new RoundRectangle2D.Float(4, 9, 3, h - 18, 3, 3));
            }
            // icone: o nosso (pelo texto); senao o do jogo
            String nome = iconeDe(m.getText());
            int ix = MARGEM, iy = (h - LADO_ICONE) / 2;
            Color corIcone = !m.isEnabled() ? Estreia.APAGADO : ativo ? Tema.mistura(acento, Color.WHITE, 0.25f)
                : Tema.alfa(acento, perigo ? 200 : 220);
            if (nome != null) {
                desenha(g, nome, ix, iy, LADO_ICONE, corIcone);
            } else if (m.getIcon() != null) {
                Icon ic = m.getIcon();
                ic.paintIcon(m, g, ix + (LADO_ICONE - ic.getIconWidth()) / 2, (h - ic.getIconHeight()) / 2);
            }
            // texto
            g.setFont(m.getFont());
            FontMetrics fm = g.getFontMetrics();
            int base = (h - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(!m.isEnabled() ? Estreia.APAGADO : ativo ? Color.WHITE : new Color(0xEDE9DF));
            if (m.getText() != null) g.drawString(m.getText(), ix + LADO_ICONE + VAO, base);
            // atalho
            String a = atalho(m.getAccelerator());
            if (a != null) {
                g.setFont(fonteAtalho());
                FontMetrics fa = g.getFontMetrics();
                int aw = fa.stringWidth(a) + 10, ax = w - MARGEM - aw, ay = (h - 18) / 2;
                g.setColor(new Color(244, 241, 234, ativo ? 34 : 20));
                g.fill(new RoundRectangle2D.Float(ax, ay, aw, 18, 6, 6));
                g.setColor(ativo ? Estreia.BRANCO : Estreia.FRACO);
                g.drawString(a, ax + 5, ay + (18 - fa.getHeight()) / 2 + fa.getAscent());
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ painel

    public static class Popup extends BasicPopupMenuUI {
        public static ComponentUI createUI(JComponent c) { return new Popup(); }

        @Override
        public void installDefaults() {
            super.installDefaults();
            popupMenu.setBorder(new Borda());
            popupMenu.setBackground(FUNDO);
            popupMenu.setOpaque(true);
        }

        private javax.swing.event.PopupMenuListener abre;

        @Override
        protected void installListeners() {
            super.installListeners();
            final javax.swing.JPopupMenu p = popupMenu;
            abre = new javax.swing.event.PopupMenuListener() {
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    if (!Animacao.ligada()) { p.putClientProperty(ABRIU, null); return; }
                    p.putClientProperty(ABRIU, System.currentTimeMillis());
                    Animacao.repinta(p);
                }
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            };
            p.addPopupMenuListener(abre);
        }

        @Override
        protected void uninstallListeners() {
            if (abre != null) popupMenu.removePopupMenuListener(abre);
            super.uninstallListeners();
        }

        @Override
        public void paint(Graphics g0, JComponent c) {
            Graphics2D g = (Graphics2D) g0.create();
            int w = c.getWidth(), h = c.getHeight();
            // enquanto os itens entram, pede os proximos quadros (o painel repinta os filhos)
            if (aberto(c) < DURACAO_ENTRADA) Animacao.repinta(c);
            g.setColor(FUNDO);
            g.fillRect(0, 0, w, h);
            // brilho suave de refletor no topo
            g.setPaint(new java.awt.GradientPaint(0, 0, Tema.alfa(Estreia.OURO, 14), 0, 60, Tema.alfa(Estreia.OURO, 0)));
            g.fillRect(0, 0, w, 60);
            g.dispose();
        }
    }

    static class Borda extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) { return new Insets(6, 4, 6, 4); }

        @Override
        public Insets getBorderInsets(Component c, Insets i) {
            i.top = i.bottom = 6; i.left = i.right = 4;
            return i;
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setColor(new Color(244, 241, 234, 34));
            g.drawRect(x, y, w - 1, h - 1);
            g.setColor(Tema.alfa(Estreia.OURO, 120));
            float f = c instanceof JComponent ? Animacao.suave(aberto((JComponent) c) / 260f) : 1f;
            g.fillRect(x + 1, y, Math.round((w - 2) * f), 1);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ separador

    public static class Separador extends BasicPopupMenuSeparatorUI {
        public static ComponentUI createUI(JComponent c) { return new Separador(); }

        @Override
        public Dimension getPreferredSize(JComponent c) { return new Dimension(0, 5); }

        @Override
        public void paint(Graphics g, JComponent c) {
            if (entrada(c) < 1f) return;
            g.setColor(new Color(244, 241, 234, 16));
            g.fillRect(MARGEM, c.getHeight() / 2, c.getWidth() - 2 * MARGEM, 1);
        }
    }
}
