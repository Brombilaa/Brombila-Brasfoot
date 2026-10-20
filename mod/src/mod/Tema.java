package mod;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

// Tema verde do Brombila Brasfoot.
//
// O jogo pinta a interface com o tema azul-marinho dele (best.aq.E e ~1000 cores
// fixas no codigo). Aqui:
//   1. qualquer cor azul vira verde (mesmo brilho), amarelo puro vira dourado;
//   2. botoes ganham cantos arredondados, degrade e hover animado (BotaoUI);
//   3. paineis escuros ganham degrade suave + textura (PainelUI);
//   4. rolagem com a rodinha do mouse fica animada (RolagemSuave);
//   5. tabelas ganham linhas zebradas e destaque animado da linha sob o mouse;
//   6. janelas sem borda (popups do jogo) aparecem com fade-in.
public final class Tema {
    public static final String NOME = "Brombila Brasfoot";

    // Paleta
    public static final Color VERDE_NOITE = new Color(0x08200F);
    public static final Color VERDE_ESCURO = new Color(0x0E3A20);
    public static final Color VERDE = new Color(0x1E7A43);
    public static final Color VERDE_VIVO = new Color(0x2DB86A);
    public static final Color VERDE_CLARO = new Color(0x7FD8A4);
    public static final Color MENTA = new Color(0xE9F6EE);
    public static final Color ZEBRA = new Color(0xF2F9F4);
    public static final Color DOURADO = new Color(0xFFD45C);

    // Matiz alvo (0..1). 0.40 ~ 144 graus, verde-esmeralda.
    static final float MATIZ = 0.40f;

    private Tema() {}

    // ---------------------------------------------------------------- cores

    private static final Map<Integer, Color> cache = new HashMap<Integer, Color>();

    // Azul/ciano/anil -> verde com o mesmo brilho. Amarelo puro -> dourado.
    // Brancos, pretos, cinzas e as demais cores ficam como estao.
    public static Color verde(Color c) {
        if (c == null) return null;
        int argb = c.getRGB();
        Color n;
        synchronized (cache) {
            n = cache.get(argb);
            if (n == null) {
                n = converte(c);
                cache.put(argb, n);
            }
        }
        if (n.getRGB() == argb) return c;
        return (c instanceof UIResource) ? new ColorUIResource(n) : n;
    }

    private static Color converte(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = hsb[0], s = hsb[1], b = hsb[2];
        int rgb;
        if (s >= 0.10f && h >= 0.45f && h <= 0.78f) {
            // Mantem um pouco da variacao de matiz (ciano x anil) para nao ficar chapado.
            float nh = MATIZ + (h - 0.58f) * 0.25f;
            float ns = Math.min(1f, s * 0.95f + 0.03f);
            rgb = Color.HSBtoRGB(nh, ns, b);
        } else if (s > 0.75f && b > 0.9f && h > 0.14f && h < 0.19f) {
            rgb = DOURADO.getRGB();
        } else {
            return c;
        }
        return new Color((rgb & 0xFFFFFF) | (c.getAlpha() << 24), true);
    }

    // Cores dos 5 temas do jogo (best.aq.E: azul, verde-cinza, preto, vinho, roxo).
    // Qualquer que seja o tema escolhido nas opcoes, o fundo vira o verde do mod.
    private static final Map<Integer, Color> TEMAS = new HashMap<Integer, Color>();
    static {
        for (int rgb : new int[] {0x004469, 0x687864, 0x000000, 0x6E0000, 0x990099}) TEMAS.put(rgb, VERDE_ESCURO);
        for (int rgb : new int[] {0x003249, 0x2C3531, 0x303233, 0x520202, 0x6E016E}) TEMAS.put(rgb, VERDE_NOITE);
        for (int rgb : new int[] {0x0570A1, 0x474A4C, 0xA93737}) TEMAS.put(rgb, VERDE);
    }

    // Como verde(), mas para cor de fundo: tambem troca as cores de tema do jogo.
    public static Color fundo(Color c) {
        if (c == null || c.getAlpha() != 255) return verde(c);
        Color t = TEMAS.get(c.getRGB() & 0xFFFFFF);
        if (t == null) return verde(c);
        return (c instanceof UIResource) ? new ColorUIResource(t) : t;
    }

    static boolean igual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    static Color mistura(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static float luminancia(Color c) {
        return (0.299f * c.getRed() + 0.587f * c.getGreen() + 0.114f * c.getBlue()) / 255f;
    }

    // ------------------------------------------------------------- instalar

    public static void instalar() {
        MetalLookAndFeel.setCurrentTheme(new TemaMetal());

        // Delegates proprios. A classe vai junto (chave = nome da classe) para o
        // UIManager nao depender do class loader da thread.
        registraUI("ButtonUI", BotaoUI.class);
        registraUI("PanelUI", PainelUI.class);

        UIManager.put("Button.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("Button.border", new BorderUIResource.EmptyBorderUIResource(4, 12, 4, 12));
        UIManager.put("Button.margin", new javax.swing.plaf.InsetsUIResource(2, 10, 2, 10));
        UIManager.put("TableHeader.background", new ColorUIResource(VERDE_ESCURO));
        UIManager.put("TableHeader.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("TableHeader.cellBorder", new BorderUIResource(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 1, VERDE),
                BorderFactory.createEmptyBorder(3, 4, 2, 4))));
        UIManager.put("Table.selectionBackground", new ColorUIResource(VERDE_VIVO));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.gridColor", new ColorUIResource(0xDCEBE1));
        UIManager.put("ScrollBar.width", 14);
        UIManager.put("ToolTip.background", new ColorUIResource(VERDE_ESCURO));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border", new BorderUIResource(
            BorderFactory.createCompoundBorder(new LineBorder(VERDE_VIVO, 1),
                BorderFactory.createEmptyBorder(3, 6, 3, 6))));

        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if ("lookAndFeel".equals(e.getPropertyName())) {
                    try { esverdeiaDefaults(); } catch (Throwable t) {}
                }
            }
        });
        try { esverdeiaDefaults(); } catch (Throwable t) {}

        long mask = AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == WindowEvent.WINDOW_OPENED) Animacao.fadeIn((Window) e.getSource());
                final Object src = (e instanceof ContainerEvent) ? ((ContainerEvent) e).getChild() : e.getSource();
                if (src instanceof Component) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { aplica((Component) src); }
                    });
                }
            }
        }, mask);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Pega as cores que o jogo troca depois que a tela ja esta montada.
                Timer t = new Timer(700, new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        for (Window w : Window.getWindows()) {
                            if (w.isShowing()) aplica(w);
                        }
                    }
                });
                t.start();
            }
        });
    }

    private static void registraUI(String id, Class<?> cls) {
        UIManager.put(id, cls.getName());
        UIManager.put(cls.getName(), cls);
    }

    // Tema Metal (o jogo usa Metal nas telas principais).
    static class TemaMetal extends OceanTheme {
        protected javax.swing.plaf.ColorUIResource getPrimary1() { return new ColorUIResource(VERDE); }
        protected javax.swing.plaf.ColorUIResource getPrimary2() { return new ColorUIResource(VERDE_CLARO); }
        protected javax.swing.plaf.ColorUIResource getPrimary3() { return new ColorUIResource(0xBFE6CD); }
        public String getName() { return "Brombila Verde"; }
        public void addCustomEntriesToTable(UIDefaults table) {
            super.addCustomEntriesToTable(table);
            List<Object> grad = new ArrayList<Object>();
            grad.add(0.3f);
            grad.add(0f);
            grad.add(new ColorUIResource(0xD7F0DF));
            grad.add(new ColorUIResource(Color.WHITE));
            grad.add(new ColorUIResource(0xA9DDBB));
            for (String k : new String[] {"Button.gradient", "ToggleButton.gradient", "CheckBox.gradient",
                    "RadioButton.gradient", "ScrollBar.gradient", "Slider.gradient", "Slider.focusGradient",
                    "InternalFrame.activeTitleGradient", "MenuBar.gradient", "CheckBoxMenuItem.gradient",
                    "RadioButtonMenuItem.gradient"}) {
                table.put(k, grad);
            }
        }
    }

    // Passa todas as cores do look and feel atual pelo verde().
    static void esverdeiaDefaults() {
        UIDefaults d = UIManager.getLookAndFeelDefaults();
        for (Object k : new ArrayList<Object>(d.keySet())) {
            Object v = d.get(k);
            if (v instanceof Color && v.getClass().getName().indexOf("Derived") < 0) {
                Color n = verde((Color) v);
                if (!igual(v, n)) d.put(k, n);
            } else if (v instanceof List) {
                List<?> l = (List<?>) v;
                List<Object> nl = new ArrayList<Object>();
                boolean mudou = false;
                for (Object it : l) {
                    if (it instanceof Color) {
                        Color n = verde((Color) it);
                        mudou |= !igual(it, n);
                        nl.add(n);
                    } else {
                        nl.add(it);
                    }
                }
                if (mudou) d.put(k, nl);
            }
        }
    }

    // ------------------------------------------------------ arvore de telas

    static void aplica(Component c) {
        if (c == null) return;
        try {
            if (c instanceof Frame || c instanceof Dialog) trocaTitulo((Window) c);
            if (c.isBackgroundSet()) {
                Color bg = c.getBackground(), n = fundo(bg);
                if (!igual(bg, n)) c.setBackground(n);
            }
            if (c.isForegroundSet()) {
                Color fg = c.getForeground(), n = verde(fg);
                if (!igual(fg, n)) c.setForeground(n);
            }
            if (c instanceof JComponent) borda((JComponent) c);
            if (c instanceof JLabel && "Brasfoot".equals(((JLabel) c).getText())) ((JLabel) c).setText(NOME);
            if (c instanceof JTable) tabela((JTable) c);
            if (c instanceof JTableHeader) cabecalho((JTableHeader) c);
            if (c instanceof JScrollPane) RolagemSuave.instala((JScrollPane) c);
            if (c instanceof JTextComponent) {
                JTextComponent t = (JTextComponent) c;
                Color sc = t.getSelectionColor(), n = verde(sc);
                if (!igual(sc, n)) t.setSelectionColor(n);
            }
        } catch (Throwable t) {
            // nunca derrubar o jogo por causa do tema
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) aplica(k);
        }
        if (c instanceof JMenu) aplica(((JMenu) c).getPopupMenu());
    }

    static void trocaTitulo(Window w) {
        String t = (w instanceof Frame) ? ((Frame) w).getTitle() : ((Dialog) w).getTitle();
        String n = null;
        if (t != null && t.indexOf("Brasfoot") >= 0 && t.indexOf("Brombila") < 0) n = t.replace("Brasfoot", NOME);
        else if ((t == null || t.trim().isEmpty()) && w instanceof Frame) n = NOME;
        if (n == null) return;
        if (w instanceof Frame) ((Frame) w).setTitle(n); else ((Dialog) w).setTitle(n);
    }

    static void borda(JComponent c) {
        Border b = c.getBorder();
        if (b == null) return;
        if (b instanceof LineBorder) {
            LineBorder lb = (LineBorder) b;
            Color n = verde(lb.getLineColor());
            if (!igual(lb.getLineColor(), n)) c.setBorder(new LineBorder(n, lb.getThickness(), lb.getRoundedCorners()));
        } else if (b instanceof MatteBorder && ((MatteBorder) b).getMatteColor() != null) {
            MatteBorder mb = (MatteBorder) b;
            Color n = verde(mb.getMatteColor());
            java.awt.Insets i = mb.getBorderInsets();
            if (!igual(mb.getMatteColor(), n)) c.setBorder(new MatteBorder(i.top, i.left, i.bottom, i.right, n));
        } else if (b instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder) b;
            Color n = verde(tb.getTitleColor());
            if (tb.getTitleColor() != null && !igual(tb.getTitleColor(), n)) tb.setTitleColor(n);
        }
    }

    // ------------------------------------------------------------- tabelas

    static final String LINHA_MOUSE = "brombila.linhaMouse";
    static final String BRILHO = "brombila.brilho";
    private static final WeakHashMap<Object, Boolean> nossos = new WeakHashMap<Object, Boolean>();
    private static final WeakHashMap<Component, Color[]> pintados = new WeakHashMap<Component, Color[]>();
    private static final Class<?>[] TIPOS = {Object.class, Number.class, Double.class, Float.class,
        Boolean.class, Icon.class, ImageIcon.class, java.util.Date.class, String.class, Integer.class};

    static void tabela(final JTable t) {
        Color sb = t.getSelectionBackground(), n = verde(sb);
        if (!igual(sb, n)) t.setSelectionBackground(n);
        Color gc = t.getGridColor(), ng = verde(gc);
        if (!igual(gc, ng)) t.setGridColor(ng);
        for (Class<?> tipo : TIPOS) {
            TableCellRenderer r = t.getDefaultRenderer(tipo);
            if (r != null && !nossos.containsKey(r)) t.setDefaultRenderer(tipo, embrulha(r));
        }
        // O jogo registra renderers para as classes dele (ex.: b.F = jogador).
        for (int j = 0; j < t.getColumnCount(); j++) {
            Class<?> tipo = t.getColumnClass(j);
            TableCellRenderer r = t.getDefaultRenderer(tipo);
            if (r != null && !nossos.containsKey(r)) t.setDefaultRenderer(tipo, embrulha(r));
        }
        TableColumnModel cm = t.getColumnModel();
        for (int j = 0; j < cm.getColumnCount(); j++) {
            javax.swing.table.TableColumn col = cm.getColumn(j);
            TableCellRenderer cr = col.getCellRenderer();
            if (cr != null && !nossos.containsKey(cr)) col.setCellRenderer(embrulha(cr));
            TableCellRenderer hr = col.getHeaderRenderer();
            if (hr != null && !nossos.containsKey(hr)) col.setHeaderRenderer(embrulha(hr));
        }
        if (t.getClientProperty(LINHA_MOUSE) == null) {
            t.putClientProperty(LINHA_MOUSE, -1);
            MouseAdapter m = new MouseAdapter() {
                public void mouseMoved(MouseEvent e) { Animacao.linhaSobMouse(t, t.rowAtPoint(e.getPoint())); }
                public void mouseExited(MouseEvent e) { Animacao.linhaSobMouse(t, -1); }
            };
            t.addMouseListener(m);
            t.addMouseMotionListener(m);
        }
    }

    static void cabecalho(JTableHeader h) {
        TableCellRenderer dh = h.getDefaultRenderer();
        if (dh != null && !nossos.containsKey(dh)) h.setDefaultRenderer(embrulha(dh));
    }

    // Os renderers pintam celula por celula; embrulhamos cada um para trocar as
    // cores na hora, zebrar as linhas brancas e iluminar a linha sob o mouse.
    static TableCellRenderer embrulha(final TableCellRenderer orig) {
        TableCellRenderer w = new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
                try {
                    // Renderer que nao repoe o fundo a cada celula: volta para a cor
                    // original em vez de acumular zebra/hover da celula anterior.
                    Color atual = c.getBackground();
                    Color[] ult = pintados.get(c);
                    if (ult != null && igual(atual, ult[0])) atual = ult[1];
                    Color base = atual;
                    Color bg = fundo(atual);
                    if (row >= 0 && !sel && bg != null) {
                        if (row % 2 == 1 && bg.getRGB() == 0xFFFFFFFF) bg = ZEBRA;
                        Object lm = t.getClientProperty(LINHA_MOUSE);
                        if (lm instanceof Integer && (Integer) lm == row) {
                            Object br = t.getClientProperty(BRILHO);
                            float f = (br instanceof Float) ? (Float) br : 1f;
                            bg = mistura(bg, VERDE_VIVO, 0.22f * f);
                        }
                    }
                    if (!igual(c.getBackground(), bg)) c.setBackground(bg);
                    pintados.put(c, new Color[] {bg, base});
                    Color fg = c.getForeground(), nf = verde(fg);
                    if (!igual(fg, nf)) c.setForeground(nf);
                    if (row < 0 && c instanceof JComponent) {
                        // Cabecalho: mantem o renderer do jogo, so troca as cores de fundo claras.
                        JComponent jc = (JComponent) c;
                        if (luminancia(jc.getBackground()) > 0.6f) {
                            jc.setBackground(VERDE_ESCURO);
                            jc.setForeground(Color.WHITE);
                            jc.setOpaque(true);
                        }
                    }
                    if (c instanceof JComponent) borda((JComponent) c);
                } catch (Throwable e) {}
                return c;
            }
        };
        nossos.put(w, Boolean.TRUE);
        return w;
    }
}
