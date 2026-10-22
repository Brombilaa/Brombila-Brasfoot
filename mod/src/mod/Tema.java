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
import javax.swing.AbstractButton;
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

// Tema "Noite de jogo" do Brombila Brasfoot: interface escura, verde neon,
// cartoes de vidro e brilho de refletor.
//
// O jogo pinta a interface com o tema azul-marinho dele (best.aq.E e ~1000 cores
// fixas no codigo). Aqui:
//   1. fundos: as cores de tema do jogo e os azuis viram verde quase preto, e os
//      fundos claros (tabelas, caixas brancas) viram superficies escuras;
//   2. textos: escuros viram claros, amarelo vira neon, cores fortes clareiam;
//   3. botoes neon com brilho animado (BotaoUI), paineis de vidro com refletores
//      (PainelUI), barras de progresso (BarraUI), check/radio (Icones) e barras
//      de rolagem finas (RolagemUI);
//   4. rolagem animada (RolagemSuave), linha sob o mouse acende nas tabelas e
//      popups sem borda aparecem com fade-in (Animacao).
public final class Tema {
    public static final String NOME = "Brombila Brasfoot";

    // Paleta
    // Desde 2026-10-21 a paleta e a "Noite de estreia" (dourado sobre quase preto),
    // a mesma do menu inicial. Os nomes NEON/MENTA ficaram por compatibilidade:
    // NEON e o dourado de destaque e MENTA o creme dos cabecalhos.
    public static final Color BASE = new Color(0x090B0A);
    public static final Color FUNDO = new Color(0x0D100F);
    public static final Color PAINEL = new Color(0x121614);
    public static final Color SUPERFICIE = new Color(0x131715);
    public static final Color ZEBRA = new Color(0x1A1F1C);
    public static final Color BORDA = new Color(0x2E3430);
    public static final Color SELECAO = new Color(0x3A3120);
    public static final Color TEXTO = new Color(0xF1EEE7);
    public static final Color TEXTO_FRACO = new Color(0x9E9B92);
    public static final Color NEON = new Color(0xF2C14E);
    public static final Color NEON_ESCURO = new Color(0xC28E2C);
    public static final Color MENTA = new Color(0xE8D7AE);
    public static final Color OURO = new Color(0xFFD84D);
    public static final Color VERMELHO = new Color(0xFF5A4D);
    public static final Color BOM = new Color(0x5FCF8E); // verde so para "bom" (energia, confianca)

    // Matiz alvo (0..1). 0.11 ~ 40 graus (ambar).
    static final float MATIZ = 0.11f;

    private Tema() {}

    // ---------------------------------------------------------------- cores

    private static final Map<Integer, Color> cacheFundo = new HashMap<Integer, Color>();
    private static final Map<Integer, Color> cacheTexto = new HashMap<Integer, Color>();

    // Cores dos 5 temas do jogo (best.aq.E: azul, verde-cinza, preto, vinho, roxo).
    // Qualquer que seja o tema escolhido nas opcoes, o fundo vira o do mod.
    private static final Map<Integer, Color> TEMAS = new HashMap<Integer, Color>();
    static {
        for (int rgb : new int[] {0x004469, 0x687864, 0x000000, 0x6E0000, 0x990099}) TEMAS.put(rgb, PAINEL);
        for (int rgb : new int[] {0x003249, 0x2C3531, 0x303233, 0x520202, 0x6E016E}) TEMAS.put(rgb, BASE);
        TEMAS.put(0x231C0E, BASE); // faixa marrom do titulo na janela de tabelas (a.o)
        for (int rgb : new int[] {0x0570A1, 0x474A4C, 0xA93737}) TEMAS.put(rgb, FUNDO);
    }

    // Cor de fundo -> versao do tema escuro.
    public static Color fundo(Color c) {
        return converte(c, cacheFundo, true);
    }

    // Cor de texto/linha -> versao do tema escuro.
    public static Color texto(Color c) {
        return converte(c, cacheTexto, false);
    }

    // Compatibilidade: cores "neutras" (bordas etc.) seguem a regra de texto.
    public static Color verde(Color c) {
        return texto(c);
    }

    private static Color converte(Color c, Map<Integer, Color> cache, boolean ehFundo) {
        if (c == null) return null;
        int argb = c.getRGB();
        Color n;
        synchronized (cache) {
            n = cache.get(argb);
            if (n == null) {
                n = ehFundo ? calculaFundo(c) : calculaTexto(c);
                if (n.getAlpha() != c.getAlpha()) n = new Color((n.getRGB() & 0xFFFFFF) | (c.getAlpha() << 24), true);
                cache.put(argb, n);
            }
        }
        if (n.getRGB() == argb) return c;
        return (c instanceof UIResource) ? new ColorUIResource(n) : n;
    }

    private static boolean amarelo(float h, float s, float b) {
        return s > 0.6f && b > 0.85f && h > 0.12f && h < 0.19f;
    }

    private static Color calculaFundo(Color c) {
        Color t = TEMAS.get(c.getRGB() & 0xFFFFFF);
        if (t != null) return t;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = hsb[0], s = hsb[1], b = hsb[2];
        if (amarelo(h, s, b)) return SELECAO; // selecao amarela do jogo
        Color v = c;
        if (s >= 0.10f && h >= 0.45f && h <= 0.78f) {
            // azul -> quase preto levemente quente (fundo "noite")
            v = new Color(Color.HSBtoRGB(MATIZ, Math.min(0.2f, s * 0.2f), Math.min(b, 0.5f) * 0.5f));
        }
        float l = luminancia(v);
        if (l > 0.55f) {
            // fundo claro (branco, cinza claro, pastel): vira superficie escura com
            // um toque da cor original
            return s > 0.2f ? mistura(SUPERFICIE, v, 0.10f) : SUPERFICIE;
        }
        if (s >= 0.4f && h >= 0.2f && h <= 0.45f && b < 0.6f) {
            // faixas verdes do jogo (ex.: "Banco de reservas"): viram faixa escura com toque dourado
            return mistura(PAINEL, NEON, 0.06f);
        }
        if (s < 0.12f && b >= 0.45f && b < 0.7f) {
            // cinza medio (paineis da escalacao): superficie escura
            return SUPERFICIE;
        }
        if (s < 0.12f && b < 0.45f) {
            // cinzas/pretos escuros: puxa para os escuros da paleta
            return mistura(BASE, PAINEL, Math.min(1f, b * 2.5f));
        }
        return v;
    }

    private static Color calculaTexto(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float h = hsb[0], s = hsb[1], b = hsb[2];
        if (amarelo(h, s, b)) return NEON;
        if (s >= 0.10f && h >= 0.45f && h <= 0.78f) {
            // azul -> creme dourado, sempre claro o bastante para ler no escuro
            return new Color(Color.HSBtoRGB(MATIZ, Math.min(0.45f, s * 0.6f), Math.max(b, 0.86f)));
        }
        if (s < 0.15f) {
            // preto/cinza escuro -> texto claro; cinza medio -> texto fraco
            if (b < 0.40f) return TEXTO;
            if (b < 0.70f) return TEXTO_FRACO;
            return c;
        }
        // cor forte (vermelho, laranja, verde): clareia se estiver escura
        if (b < 0.75f) return new Color(Color.HSBtoRGB(h, s * 0.85f, 0.95f));
        return c;
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

    static Color alfa(Color c, int a) {
        return new Color((c.getRGB() & 0xFFFFFF) | (a << 24), true);
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
        registraUI("ProgressBarUI", BarraUI.class);
        registraUI("ScrollBarUI", RolagemUI.class);
        registraUI("MenuItemUI", Menus.Item.class);
        registraUI("PopupMenuUI", Menus.Popup.class);
        registraUI("PopupMenuSeparatorUI", Menus.Separador.class);
        registraUI("OptionPaneUI", Pergunta.class);
        UIManager.put("OptionPane.buttonPadding", 12);

        // Perguntas do jogo em portugues (o Java mostra "Yes/No" por padrao).
        UIManager.put("OptionPane.yesButtonText", "Sim");
        UIManager.put("OptionPane.noButtonText", "Não");
        UIManager.put("OptionPane.cancelButtonText", "Cancelar");
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.questionIcon", new Pergunta.Sinal("?", Estreia.OURO));
        UIManager.put("OptionPane.informationIcon", new Pergunta.Sinal("i", Estreia.OURO));
        UIManager.put("OptionPane.warningIcon", new Pergunta.Sinal("!", Estreia.OURO));
        UIManager.put("OptionPane.errorIcon", new Pergunta.Sinal("×", VERMELHO));
        UIManager.put("Button.foreground", new ColorUIResource(BASE));
        UIManager.put("Button.border", new BorderUIResource.EmptyBorderUIResource(4, 12, 4, 12));
        UIManager.put("Button.margin", new javax.swing.plaf.InsetsUIResource(2, 10, 2, 10));
        UIManager.put("CheckBox.icon", new Icones.Check());
        UIManager.put("RadioButton.icon", new Icones.Radio());
        UIManager.put("TableHeader.background", new ColorUIResource(BASE));
        UIManager.put("TableHeader.foreground", new ColorUIResource(MENTA));
        UIManager.put("TableHeader.cellBorder", new BorderUIResource(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, alfa(NEON, 110)),
                BorderFactory.createEmptyBorder(4, 6, 3, 6))));
        UIManager.put("Table.selectionBackground", new ColorUIResource(SELECAO));
        UIManager.put("Table.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("Table.gridColor", new ColorUIResource(0x232825));
        UIManager.put("ScrollPane.border", new BorderUIResource(BorderFactory.createLineBorder(BORDA)));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ToolTip.background", new ColorUIResource(PAINEL));
        UIManager.put("ToolTip.foreground", new ColorUIResource(TEXTO));
        UIManager.put("ToolTip.border", new BorderUIResource(
            BorderFactory.createCompoundBorder(new LineBorder(alfa(NEON, 140), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8))));
        UIManager.put("TextField.caretForeground", new ColorUIResource(NEON));
        UIManager.put("TextArea.caretForeground", new ColorUIResource(NEON));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(SELECAO));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("List.selectionBackground", new ColorUIResource(SELECAO));
        UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));

        UIManager.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if ("lookAndFeel".equals(e.getPropertyName())) {
                    try { escureceDefaults(); } catch (Throwable t) {}
                }
            }
        });
        try { escureceDefaults(); } catch (Throwable t) {}

        long mask = AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e instanceof ContainerEvent && e.getID() == ContainerEvent.COMPONENT_ADDED) {
                    // pergunta (JOptionPane) entrando na janela: tira a barra do Windows antes de abrir
                    if (((ContainerEvent) e).getChild() instanceof javax.swing.JOptionPane) {
                        Pergunta.prepara((javax.swing.JOptionPane) ((ContainerEvent) e).getChild());
                    }
                    // tela inicial do jogo sendo montada: esconde ate trocar pelo menu novo
                    Object s = e.getSource();
                    Window w = (s instanceof Window) ? (Window) s : SwingUtilities.getWindowAncestor((Component) s);
                    if (w != null && w.getClass().getName().equals("a.gq") && !w.isShowing()) MenuInicial.prepara(w);
                }
                if (e.getID() == WindowEvent.WINDOW_OPENED) {
                    Window w = (Window) e.getSource();
                    if (w.getClass().getName().equals("a.gq")) MenuInicial.instala((javax.swing.JFrame) w);
                    if (w instanceof javax.swing.JDialog && temPergunta(w)) {
                        // perguntas (JOptionPane): a fonte nova muda a largura do texto,
                        // entao a janela e refeita no tamanho certo
                        aplica(w);
                        w.pack();
                        w.setLocationRelativeTo(w.getOwner() != null && w.getOwner().isShowing() ? w.getOwner() : null);
                    }
                    Animacao.fadeIn(w);
                }
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

    private static boolean temPergunta(Container c) {
        for (Component k : c.getComponents()) {
            if (k instanceof javax.swing.JOptionPane) return true;
            if (k instanceof Container && temPergunta((Container) k)) return true;
        }
        return false;
    }

    private static void registraUI(String id, Class<?> cls) {
        UIManager.put(id, cls.getName());
        UIManager.put(cls.getName(), cls);
    }

    // Tema Metal escuro (o jogo usa Metal nas telas principais).
    static class TemaMetal extends OceanTheme {
        private static ColorUIResource cor(Color c) { return new ColorUIResource(c); }
        protected ColorUIResource getPrimary1() { return cor(NEON_ESCURO); }
        protected ColorUIResource getPrimary2() { return cor(SELECAO); }
        protected ColorUIResource getPrimary3() { return cor(SELECAO); }
        protected ColorUIResource getSecondary1() { return cor(BORDA); }
        protected ColorUIResource getSecondary2() { return cor(ZEBRA); }
        protected ColorUIResource getSecondary3() { return cor(PAINEL); }
        protected ColorUIResource getBlack() { return cor(TEXTO); }
        protected ColorUIResource getWhite() { return cor(SUPERFICIE); }
        public ColorUIResource getControlTextColor() { return cor(TEXTO); }
        public ColorUIResource getSystemTextColor() { return cor(TEXTO); }
        public ColorUIResource getUserTextColor() { return cor(TEXTO); }
        public ColorUIResource getMenuForeground() { return cor(TEXTO); }
        public ColorUIResource getInactiveControlTextColor() { return cor(TEXTO_FRACO); }
        public ColorUIResource getInactiveSystemTextColor() { return cor(TEXTO_FRACO); }
        public ColorUIResource getHighlightedTextColor() { return cor(Color.WHITE); }
        public ColorUIResource getTextHighlightColor() { return cor(SELECAO); }
        public String getName() { return "Brombila Noite de Jogo"; }
        public void addCustomEntriesToTable(UIDefaults table) {
            super.addCustomEntriesToTable(table);
            List<Object> grad = new ArrayList<Object>();
            grad.add(0.3f);
            grad.add(0f);
            grad.add(new ColorUIResource(ZEBRA));
            grad.add(new ColorUIResource(PAINEL));
            grad.add(new ColorUIResource(BASE));
            for (String k : new String[] {"Button.gradient", "ToggleButton.gradient", "CheckBox.gradient",
                    "RadioButton.gradient", "ScrollBar.gradient", "Slider.gradient", "Slider.focusGradient",
                    "InternalFrame.activeTitleGradient", "MenuBar.gradient", "CheckBoxMenuItem.gradient",
                    "RadioButtonMenuItem.gradient"}) {
                table.put(k, grad);
            }
        }
    }

    // Passa as cores do look and feel atual pelo tema: chaves de fundo por fundo(),
    // o resto por texto().
    static void escureceDefaults() {
        UIDefaults d = UIManager.getLookAndFeelDefaults();
        for (Object k : new ArrayList<Object>(d.keySet())) {
            Object v = d.get(k);
            String nome = String.valueOf(k).toLowerCase();
            boolean ehFundo = nome.contains("background") || nome.endsWith("control") || nome.equals("window")
                || nome.contains("shadow") || nome.contains("highlight") || nome.equals("text") || nome.equals("menu")
                || nome.equals("info") || nome.contains("selection") && !nome.contains("foreground");
            if (v instanceof java.awt.Font && nome.endsWith("font")) {
                java.awt.Font n = fonte((java.awt.Font) v);
                if (n != v) d.put(k, new javax.swing.plaf.FontUIResource(n));
            } else if (v instanceof Color && v.getClass().getName().indexOf("Derived") < 0) {
                Color n = ehFundo ? fundo((Color) v) : texto((Color) v);
                if (!igual(v, n)) d.put(k, n);
            } else if (v instanceof List && nome.contains("gradient")) {
                // gradientes ja vem do TemaMetal
            }
        }
    }

    // ---------------------------------------------------------------- fontes

    // O jogo usa Tahoma (e a fonte padrao do Java) em tudo. Trocamos pela Manrope,
    // a mesma do menu: largura parecida com a Tahoma, entao o tamanho fica igual.
    private static final java.util.Set<String> FAMILIAS = new java.util.HashSet<String>(java.util.Arrays.asList(
        "tahoma", "dialog", "sansserif", "arial", "verdana", "segoe ui", "lucida sans", "lucida grande", "microsoft sans serif"));
    private static final Map<String, java.awt.Font> cacheFontes = new HashMap<String, java.awt.Font>();

    static java.awt.Font fonte(java.awt.Font f) {
        if (f == null || !FAMILIAS.contains(f.getFamily().toLowerCase())) return f;
        String chave = f.getStyle() + "/" + f.getSize2D();
        java.awt.Font n = cacheFontes.get(chave);
        if (n == null) {
            n = f.isBold() ? Estreia.negrito(f.getSize2D()) : Estreia.texto(f.getSize2D());
            if (f.isItalic()) n = n.deriveFont(n.getStyle() | java.awt.Font.ITALIC);
            if (f instanceof UIResource) n = new javax.swing.plaf.FontUIResource(n);
            cacheFontes.put(chave, n);
        }
        return n;
    }

    private static void trocaFonte(Component c) {
        if (!c.isFontSet()) return;
        java.awt.Font f = c.getFont(), n = fonte(f);
        if (n != f) c.setFont(n);
    }

    // ------------------------------------------------------ arvore de telas

    // Telas em que o jogo guarda a cor do quadrinho (getBackground) como cor escolhida:
    // Cores do Time (a.bb), Editar Time do editor (d.ac), cores da lista (a.aX) e o
    // seletor de cores. Trocar essas cores pelo tema gravaria a cor errada no time.
    static boolean guardaCores(Component c) {
        String n = c.getClass().getName();
        return n.equals("a.bb") || n.equals("d.ac") || n.equals("a.aX") || c instanceof javax.swing.JColorChooser;
    }

    static void aplica(Component c) {
        boolean cores = false;
        for (Component p = c; p != null && !cores; p = p.getParent()) cores = guardaCores(p);
        aplica(c, cores);
    }

    // Janela "Cores do Time" (a.bb): os quadrinhos e a previa mostram as cores do
    // clube e nao podem ser trocados pelas cores do tema.
    static void aplica(Component c, boolean coresDoTime) {
        if (c == null) return;
        coresDoTime = coresDoTime || guardaCores(c);
        boolean amostra = coresDoTime && c instanceof JLabel && c.isOpaque();
        if (c.getClass().getName().equals("a.hU") && c instanceof javax.swing.JPanel) {
            try { ConfigEstreia.aplica((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.bV") && c instanceof javax.swing.JPanel) {
            try { ConfigEstreia.tecnico((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("d.n") && c instanceof javax.swing.JFrame) {
            try { Editor.aplica((javax.swing.JFrame) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c instanceof javax.swing.JPanel && c.getClass().getName().startsWith("d.")) {
            String nome = c.getClass().getName();
            try {
                if (nome.equals("d.ac")) Editor.janela((javax.swing.JPanel) c, "VB", "QU");
                else if (nome.equals("d.f")) Editor.janela((javax.swing.JPanel) c, "vd", "ug");
                else if (nome.equals("d.ai")) Editor.janela((javax.swing.JPanel) c, null, "QU");
            } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.ay") && c instanceof javax.swing.JPanel) {
            try { Telas.tabelas((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.iA") && c instanceof javax.swing.JPanel) {
            try { Telas.times((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.cl") && c instanceof javax.swing.JPanel) {
            try { Janelas.estadio((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.dV") && c instanceof javax.swing.JPanel) {
            try { Partida.instala((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
            try { Placar.instala((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c.getClass().getName().equals("a.ds") && c instanceof javax.swing.JPanel) {
            try { Janelas.juniores((javax.swing.JPanel) c); } catch (Throwable t) { t.printStackTrace(); }
        }
        if (c instanceof javax.swing.JPanel && c.getClass().getName().startsWith("a.")) {
            try { Dialogos.aplica(c); } catch (Throwable t) { t.printStackTrace(); }
        }
        // Telas "Noite de estreia" (menu e configuracoes) tem cores proprias.
        boolean estreia = Estreia.dentro(c);
        try {
            if (c instanceof Frame || c instanceof Dialog) trocaTitulo((Window) c);
            boolean botao = c instanceof AbstractButton && !(c instanceof javax.swing.JCheckBox) && !(c instanceof javax.swing.JRadioButton);
            if (c.isBackgroundSet() && !botao && !amostra && !estreia) {
                Color bg = c.getBackground(), n = fundo(bg);
                if (!igual(bg, n)) c.setBackground(n);
            }
            if (c.isForegroundSet() && !botao && !amostra && !estreia) {
                Color fg = c.getForeground(), n = texto(fg);
                if (!igual(fg, n)) c.setForeground(n);
            }
            trocaFonte(c);
            if (c instanceof JComponent && !estreia) borda((JComponent) c);
            if (c instanceof JLabel && "Brasfoot".equals(((JLabel) c).getText())) ((JLabel) c).setText(NOME);
            if (c instanceof JTable) tabela((JTable) c);
            if (c instanceof JTableHeader) cabecalho((JTableHeader) c);
            if (c instanceof JScrollPane) RolagemSuave.instala((JScrollPane) c);
            if (c instanceof JTextComponent) {
                JTextComponent t = (JTextComponent) c;
                if (!igual(t.getSelectionColor(), SELECAO)) t.setSelectionColor(SELECAO);
                if (!igual(t.getCaretColor(), NEON)) t.setCaretColor(NEON);
                if (!igual(t.getSelectedTextColor(), Color.WHITE)) t.setSelectedTextColor(Color.WHITE);
                Color fg = t.getForeground(), n = texto(fg);
                if (!igual(fg, n)) t.setForeground(n);
            }
        } catch (Throwable t) {
            // nunca derrubar o jogo por causa do tema
        }
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) aplica(k, coresDoTime);
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
            Color n = linha(lb.getLineColor());
            if (!igual(lb.getLineColor(), n)) c.setBorder(new LineBorder(n, lb.getThickness(), lb.getRoundedCorners()));
        } else if (b instanceof MatteBorder && ((MatteBorder) b).getMatteColor() != null) {
            MatteBorder mb = (MatteBorder) b;
            Color n = linha(mb.getMatteColor());
            java.awt.Insets i = mb.getBorderInsets();
            if (!igual(mb.getMatteColor(), n)) c.setBorder(new MatteBorder(i.top, i.left, i.bottom, i.right, n));
        } else if (b instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder) b;
            Color n = texto(tb.getTitleColor());
            if (tb.getTitleColor() != null && !igual(tb.getTitleColor(), n)) tb.setTitleColor(n);
        }
    }

    // Linhas de borda: cinzas/brancos viram a borda discreta do tema, cores viram neon.
    static Color linha(Color c) {
        if (c == null) return null;
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        if (igual(c, BORDA) || igual(c, alfa(NEON, 120))) return c;
        if (hsb[1] < 0.15f) return BORDA;
        return alfa(NEON, 120);
    }

    // ------------------------------------------------------------- tabelas

    static final String LINHA_MOUSE = "brombila.linhaMouse";
    static final String BRILHO = "brombila.brilho";
    private static final WeakHashMap<Object, Boolean> nossos = new WeakHashMap<Object, Boolean>();
    private static final WeakHashMap<Component, Color[]> pintados = new WeakHashMap<Component, Color[]>();
    private static final Class<?>[] TIPOS = {Object.class, Number.class, Double.class, Float.class,
        Boolean.class, Icon.class, ImageIcon.class, java.util.Date.class, String.class, Integer.class};

    static void tabela(final JTable t) {
        if (!igual(t.getSelectionBackground(), SELECAO)) t.setSelectionBackground(SELECAO);
        if (!igual(t.getSelectionForeground(), Color.WHITE)) t.setSelectionForeground(Color.WHITE);
        Color gc = t.getGridColor();
        if (!igual(gc, new Color(0x232825))) t.setGridColor(new Color(0x232825));
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
    // cores na hora, zebrar as linhas e acender a linha sob o mouse.
    static TableCellRenderer embrulha(final TableCellRenderer orig) {
        // tabela de jogos da rodada (tela da partida): celulas redesenhadas
        final CelulaRodada rodada = orig.getClass().getName().equals("b.aG") ? new CelulaRodada() : null;
        TableCellRenderer w = new TableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
                try { trocaFonte(c); } catch (Throwable e) {}
                if (rodada != null && c instanceof JLabel && row >= 0) {
                    try {
                        if (!igual(t.getBackground(), FUNDO)) t.setBackground(FUNDO);
                        Object lm = t.getClientProperty(LINHA_MOUSE), br = t.getClientProperty(BRILHO);
                        float hv = (lm instanceof Integer && (Integer) lm == row) ? ((br instanceof Float) ? (Float) br : 1f) : 0f;
                        rodada.prepara((JLabel) c, t.convertColumnIndexToModel(col), hv);
                        return rodada;
                    } catch (Throwable e) {}
                }
                try {
                    // Renderer que nao repoe as cores a cada celula: volta para a cor
                    // original em vez de acumular zebra/hover da celula anterior.
                    Color atual = c.getBackground(), atualFg = c.getForeground();
                    Color[] ult = pintados.get(c);
                    if (ult != null && igual(atual, ult[0])) atual = ult[1];
                    if (ult != null && igual(atualFg, ult[2])) atualFg = ult[3];
                    Color base = atual, baseFg = atualFg;
                    Color bg, fg;
                    if (Estreia.dentro(t)) {
                        // tabelas da tela de configuracoes: neutras com destaque dourado
                        if (row < 0) {
                            bg = ConfigEstreia.CABECALHO;
                            fg = Estreia.FRACO;
                            if (c instanceof JComponent) ((JComponent) c).setOpaque(true);
                        } else if (sel) {
                            bg = ConfigEstreia.SELECAO;
                            fg = Estreia.BRANCO;
                        } else {
                            bg = (row % 2 == 1) ? ConfigEstreia.ZEBRA : ConfigEstreia.CELULA;
                            Object lm = t.getClientProperty(LINHA_MOUSE);
                            if (lm instanceof Integer && (Integer) lm == row) {
                                Object br = t.getClientProperty(BRILHO);
                                bg = mistura(bg, Estreia.OURO, 0.10f * ((br instanceof Float) ? (Float) br : 1f));
                            }
                            fg = new Color(0xE6E2DA);
                            // time marcado no editor (o jogo pinta de vermelho)
                            if (igual(atualFg, Color.RED)) fg = new Color(0xFF7A6E);
                        }
                        if (!igual(c.getBackground(), bg)) c.setBackground(bg);
                        if (!igual(c.getForeground(), fg)) c.setForeground(fg);
                        pintados.put(c, new Color[] {bg, base, fg, baseFg});
                        if (c instanceof JComponent && row < 0) ((JComponent) c).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Estreia.LINHA), BorderFactory.createEmptyBorder(4, 6, 3, 6)));
                        return c;
                    }
                    if (row < 0) {
                        // Cabecalho: escuro com texto menta.
                        bg = BASE;
                        fg = MENTA;
                        if (c instanceof JComponent) ((JComponent) c).setOpaque(true);
                    } else if (sel) {
                        bg = SELECAO;
                        fg = Color.WHITE;
                    } else {
                        bg = fundo(atual);
                        if (bg != null && luminancia(bg) < 0.2f && (row % 2 == 1) && igual(bg, SUPERFICIE)) bg = ZEBRA;
                        Object lm = t.getClientProperty(LINHA_MOUSE);
                        if (bg != null && lm instanceof Integer && (Integer) lm == row) {
                            Object br = t.getClientProperty(BRILHO);
                            float f = (br instanceof Float) ? (Float) br : 1f;
                            bg = mistura(bg, NEON, 0.13f * f);
                        }
                        fg = texto(atualFg);
                    }
                    if (bg != null && !igual(c.getBackground(), bg)) c.setBackground(bg);
                    if (fg != null && !igual(c.getForeground(), fg)) c.setForeground(fg);
                    pintados.put(c, new Color[] {bg, base, fg, baseFg});
                    if (c instanceof JComponent) borda((JComponent) c);
                    // posicao na lista do elenco da tela principal: chip colorido
                    Object pc = t.getClientProperty(Principal.POSICAO);
                    if (row >= 0 && pc instanceof Integer && (Integer) pc == t.convertColumnIndexToModel(col) && c instanceof JLabel)
                        return Principal.chip((JLabel) c);
                } catch (Throwable e) {}
                return c;
            }
        };
        nossos.put(w, Boolean.TRUE);
        return w;
    }
}
