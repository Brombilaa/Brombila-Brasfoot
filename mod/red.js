// Brombila Brasfoot - roda antes do jogo (Nashorn / Java 8)
// Troca o titulo e deixa toda a paleta da interface Swing em vermelho.
var Color = java.awt.Color;
var UIManager = javax.swing.UIManager;
var SwingUtilities = javax.swing.SwingUtilities;
var ColorUIResource = javax.swing.plaf.ColorUIResource;
var UIResource = javax.swing.plaf.UIResource;
var LineBorder = javax.swing.border.LineBorder;
var MatteBorder = javax.swing.border.MatteBorder;
var TitledBorder = javax.swing.border.TitledBorder;
var JTable = javax.swing.JTable;
var TableCellRenderer = Java.type("javax.swing.table.TableCellRenderer");
var WeakHashMap = java.util.WeakHashMap;

var NOVO_NOME = "Brombila Brasfoot";
var RED_HUE = 0.0;

// Cor saturada (verde, azul, amarelo...) -> mesmo brilho/saturacao, matiz vermelho.
// Branco, preto e cinzas ficam como estao.
function redify(c) {
    if (c == null) return c;
    var hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    if (hsb[1] < 0.12) return c;
    if (hsb[0] < 0.02 || hsb[0] > 0.98) return c;
    var rgb = Color.HSBtoRGB(RED_HUE, hsb[1], hsb[2]) & 0xFFFFFF;
    var n = new Color(rgb | (c.getAlpha() << 24), true);
    return (c instanceof UIResource) ? new ColorUIResource(n) : n;
}

function same(a, b) { return a === b || (a != null && a.equals(b)); }

function fixTitle(w) {
    try {
        var t = w.getTitle();
        if (t != null && t.indexOf("Brasfoot") >= 0 && t.indexOf("Brombila") < 0) {
            w.setTitle(t.replace("Brasfoot", NOVO_NOME));
        } else if (t != null && t.trim().length == 0 && w instanceof java.awt.Frame) {
            w.setTitle(NOVO_NOME);
        }
    } catch (e) {}
}

function fixBorder(jc) {
    var b = jc.getBorder();
    if (b == null) return;
    if (b instanceof LineBorder) {
        var lc = b.getLineColor(), nl = redify(lc);
        if (!same(lc, nl)) jc.setBorder(new LineBorder(nl, b.getThickness(), b.getRoundedCorners()));
    } else if (b instanceof MatteBorder && b.getMatteColor() != null) {
        var mc = b.getMatteColor(), nm = redify(mc), i = b.getBorderInsets();
        if (!same(mc, nm)) jc.setBorder(new MatteBorder(i.top, i.left, i.bottom, i.right, nm));
    } else if (b instanceof TitledBorder) {
        var tc = b.getTitleColor(), nt = redify(tc);
        if (tc != null && !same(tc, nt)) b.setTitleColor(nt);
    }
}

function fixShallow(comp) {
    if (comp == null) return;
    var bg = comp.getBackground(), nb = redify(bg);
    if (!same(bg, nb)) comp.setBackground(nb);
    var fg = comp.getForeground(), nf = redify(fg);
    if (!same(fg, nf)) comp.setForeground(nf);
}

// Renderers de tabela pintam cada celula na hora; embrulhamos cada um.
var wrappers = new WeakHashMap();
function wrapRenderer(orig) {
    if (orig == null || wrappers.containsKey(orig)) return orig;
    var w = new TableCellRenderer({
        getTableCellRendererComponent: function (t, v, sel, foc, row, col) {
            var c = orig.getTableCellRendererComponent(t, v, sel, foc, row, col);
            try { fixShallow(c); if (c instanceof javax.swing.JComponent) fixBorder(c); } catch (e) {}
            return c;
        }
    });
    wrappers.put(w, true);
    return w;
}

var TABLE_TYPES = [java.lang.Object.class, java.lang.Number.class, java.lang.Double.class,
                   java.lang.Float.class, java.lang.Boolean.class, javax.swing.Icon.class,
                   javax.swing.ImageIcon.class, java.util.Date.class, java.lang.String.class,
                   java.lang.Integer.class];
function fixTable(t) {
    var sb = t.getSelectionBackground(), nsb = redify(sb);
    if (!same(sb, nsb)) t.setSelectionBackground(nsb);
    var gc = t.getGridColor(), ngc = redify(gc);
    if (!same(gc, ngc)) t.setGridColor(ngc);
    for (var i = 0; i < TABLE_TYPES.length; i++) {
        var r = t.getDefaultRenderer(TABLE_TYPES[i]);
        if (r != null && !wrappers.containsKey(r)) t.setDefaultRenderer(TABLE_TYPES[i], wrapRenderer(r));
    }
    var cm = t.getColumnModel();
    for (var j = 0; j < cm.getColumnCount(); j++) {
        var col = cm.getColumn(j), cr = col.getCellRenderer();
        if (cr != null && !wrappers.containsKey(cr)) col.setCellRenderer(wrapRenderer(cr));
        var hr = col.getHeaderRenderer();
        if (hr != null && !wrappers.containsKey(hr)) col.setHeaderRenderer(wrapRenderer(hr));
    }
    var h = t.getTableHeader();
    if (h != null) {
        var dh = h.getDefaultRenderer();
        if (dh != null && !wrappers.containsKey(dh)) h.setDefaultRenderer(wrapRenderer(dh));
    }
}

function fixTree(comp) {
    if (comp == null) return;
    try {
        if (comp instanceof java.awt.Frame || comp instanceof java.awt.Dialog) fixTitle(comp);
        if (comp.isBackgroundSet()) {
            var bg = comp.getBackground(), nb = redify(bg);
            if (!same(bg, nb)) comp.setBackground(nb);
        }
        if (comp.isForegroundSet()) {
            var fg = comp.getForeground(), nf = redify(fg);
            if (!same(fg, nf)) comp.setForeground(nf);
        }
        if (comp instanceof javax.swing.JComponent) fixBorder(comp);
        if (comp instanceof javax.swing.JLabel && "Brasfoot".equals(comp.getText())) comp.setText(NOVO_NOME);
        if (comp instanceof JTable) fixTable(comp);
        if (comp instanceof javax.swing.text.JTextComponent) {
            var sc = comp.getSelectionColor(), ns = redify(sc);
            if (!same(sc, ns)) comp.setSelectionColor(ns);
        }
    } catch (e) {}
    if (comp instanceof java.awt.Container) {
        var kids = comp.getComponents();
        for (var i = 0; i < kids.length; i++) fixTree(kids[i]);
    }
    if (comp instanceof javax.swing.JMenu) {
        var pm = comp.getPopupMenu();
        if (pm != null) fixTree(pm);
    }
}

function fixAllWindows() {
    var ws = java.awt.Window.getWindows();
    for (var i = 0; i < ws.length; i++) fixTree(ws[i]);
}

// Paleta do Nimbus (o jogo usa Nimbus) e demais cores padrao do Swing.
function redifyDefaults() {
    var d = UIManager.getLookAndFeelDefaults();
    var keys = new java.util.ArrayList(d.keySet());
    for (var i = 0; i < keys.size(); i++) {
        var k = keys.get(i), v = d.get(k);
        if (v instanceof Color && v.getClass().getName().indexOf("Derived") < 0) {
            var n = redify(v);
            if (!same(v, n)) UIManager.put(k, n);
        } else if (v instanceof java.util.List) {
            // ex.: Button.gradient do tema Ocean
            var lst = new java.util.ArrayList(), changed = false;
            for (var j = 0; j < v.size(); j++) {
                var it = v.get(j);
                if (it instanceof Color) { var ni = redify(it); if (!same(it, ni)) changed = true; lst.add(ni); }
                else lst.add(it);
            }
            if (changed) UIManager.put(k, lst);
        }
    }
}
// Tema Metal/Ocean vermelho (usado quando o jogo escolhe o visual Metal).
var OceanTheme = Java.type("javax.swing.plaf.metal.OceanTheme");
var RedOcean = Java.extend(OceanTheme, {
    getPrimary1: function () { return new ColorUIResource(0xBF4040); },
    getPrimary2: function () { return new ColorUIResource(0xCCA3A3); },
    getPrimary3: function () { return new ColorUIResource(0xE5B8B8); },
    getName: function () { return "Brombila"; }
});
javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new RedOcean());

UIManager.put("nimbusBase", new Color(0x7A0000));
UIManager.put("nimbusBlueGrey", new Color(0xB07070));
UIManager.put("control", new Color(0xF3E3E3));
UIManager.put("nimbusFocus", new Color(0xE53935));
UIManager.put("nimbusSelectionBackground", new Color(0xB71C1C));
UIManager.put("nimbusSelectedText", Color.WHITE);
UIManager.put("nimbusInfoBlue", new Color(0xC62828));
UIManager.put("nimbusOrange", new Color(0xD32F2F));
UIManager.put("nimbusGreen", new Color(0xC62828));
UIManager.put("nimbusLightBackground", new Color(0xFFF5F5));
UIManager.put("info", new Color(0xFFE5E5));
UIManager.put("textHighlight", new Color(0xC62828));
UIManager.put("Table.alternateRowColor", new Color(0xF8E1E1));
UIManager.addPropertyChangeListener(function (e) {
    if ("lookAndFeel".equals(e.getPropertyName())) { try { redifyDefaults(); } catch (x) {} }
});
try { redifyDefaults(); } catch (x) {}

var MASK = java.awt.AWTEvent.CONTAINER_EVENT_MASK | java.awt.AWTEvent.WINDOW_EVENT_MASK;
java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(function (e) {
    var src = (e instanceof java.awt.event.ContainerEvent) ? e.getChild() : e.getSource();
    SwingUtilities.invokeLater(function () { fixTree(src); });
}, MASK);

SwingUtilities.invokeLater(function () {
    var t = new javax.swing.Timer(800, function () { fixAllWindows(); });
    t.start();
});

// Inicia o jogo original.
var loader = java.lang.Thread.currentThread().getContextClassLoader();
var gameMain = java.lang.Class.forName("best.h2", true, loader)
    .getMethod("main", Java.type("java.lang.String[]").class);
gameMain.invoke(null, Java.to([args], "java.lang.Object[]"));
