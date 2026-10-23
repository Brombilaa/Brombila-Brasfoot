package mod;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.Properties;
import javax.swing.*;

// Modo janela: a janela principal do jogo (best.S.cC) abre em tela cheia sem borda.
// F11 (ou a chave "Modo janela" nas Preferencias) troca para uma janela normal do Windows,
// com barra de titulo, que pode mudar de tamanho e de lugar. A escolha, o tamanho e a
// posicao ficam guardados em brombila.ini na pasta do jogo.
public class Janela {
    static final String ARQUIVO = "brombila.ini";
    static final int MIN_LARG = 1024, MIN_ALT = 768;

    private static final Properties cfg = new Properties();
    private static boolean carregado;
    private static Timer salvar;
    private static boolean trocando;
    private static long ignorarAte;  // mudancas de tamanho feitas por nos mesmos, nao pelo jogador

    // ------------------------------------------------------------ preferencias

    private static File arquivo() {
        return new File(System.getProperty("user.dir"), ARQUIVO);
    }

    private static synchronized void carrega() {
        if (carregado) return;
        carregado = true;
        try (InputStream in = new FileInputStream(arquivo())) {
            cfg.load(in);
        } catch (Throwable t) {
            // primeira vez: tela cheia, como o jogo original
        }
    }

    private static synchronized void grava() {
        try (OutputStream out = new FileOutputStream(arquivo())) {
            cfg.store(out, "Brombila Brasfoot");
        } catch (Throwable t) {
            // pasta sem permissao de escrita: vale so ate fechar o jogo
        }
    }

    static boolean emJanela() {
        carrega();
        return "1".equals(cfg.getProperty("janela"));
    }

    private static int num(String k, int padrao) {
        try { return Integer.parseInt(cfg.getProperty(k).trim()); } catch (Throwable t) { return padrao; }
    }

    // ------------------------------------------------------------ janela do jogo

    static JFrame principal() {
        try {
            return (JFrame) Class.forName("best.S").getMethod("db").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    static boolean ehPrincipal(Object o) {
        return o != null && o == principal();
    }

    private static Rectangle area(Window w) {
        GraphicsConfiguration gc = w.getGraphicsConfiguration();
        if (gc == null) gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle r = gc.getBounds();
        Insets i = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        return new Rectangle(r.x + i.left, r.y + i.top, r.width - i.left - i.right, r.height - i.top - i.bottom);
    }

    // Instalado pelo Tema: troca de janela com F11 em qualquer tela do jogo.
    static void instala() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_F11) return false;
                if (e.getID() == KeyEvent.KEY_PRESSED && !trocando) alterna(!emJanela());
                return true;
            }
        });
    }

    // O jogo ja montou a janela (sem borda, maximizada) mas ainda nao mostrou: se a escolha
    // guardada e janela, ja nasce com borda, sem piscar em tela cheia.
    static void antesDeMostrar(JFrame f) {
        if (!emJanela() || f.isDisplayable()) return;
        try {
            f.setOpacity(1f);
            f.setUndecorated(false);
            f.setExtendedState(Frame.NORMAL);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // A janela principal abriu (WINDOW_OPENED).
    static void abriu(JFrame f) {
        f.setTitle("Brombila Brasfoot");
        if (!f.isUndecorated()) poeEmJanela(f);
        acompanha(f);
        segueTamanho(f);
    }

    // As telas grandes do jogo (principal, partida...) nascem do tamanho da tela inteira
    // (best.S.gS x gT) dentro de um painel com FlowLayout, que nao as encolhe. Em janela,
    // essas telas passam a ter o tamanho da area da janela, e acompanham quando ela muda.
    private static void segueTamanho(JFrame f) {
        Container cp = f.getContentPane();
        if (!(cp instanceof JComponent) || ((JComponent) cp).getClientProperty("brombila.segue") != null) return;
        ((JComponent) cp).putClientProperty("brombila.segue", Boolean.TRUE);
        // o jogo cria um painel novo (components.E) a cada troca de tela
        cp.addContainerListener(new ContainerAdapter() {
            public void componentAdded(ContainerEvent e) { segue(e.getChild()); }
        });
        for (Component c : cp.getComponents()) segue(c);
    }

    private static void segue(Component c) {
        if (!(c instanceof JPanel) || !c.getClass().getName().equals("components.E")) return;
        final JPanel area = (JPanel) c;
        if (area.getClientProperty("brombila.segue") != null) return;
        area.putClientProperty("brombila.segue", Boolean.TRUE);
        area.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { ajusta(area); }
        });
        area.addContainerListener(new ContainerAdapter() {
            public void componentAdded(ContainerEvent e) { ajusta(area); }
        });
        ajusta(area);
    }

    private static int estatico(String nome, Integer novo) {
        try {
            java.lang.reflect.Field f = Class.forName("best.S").getDeclaredField(nome);
            f.setAccessible(true);
            int v = f.getInt(null);
            if (novo != null) f.setInt(null, novo);
            return v;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void ajusta(JPanel area) {
        int w = area.getWidth(), h = area.getHeight();
        // o FlowLayout do painel deixa uma folga em cima; sem descontar, a tela passa da borda de baixo
        if (area.getLayout() instanceof FlowLayout) h -= ((FlowLayout) area.getLayout()).getVgap();
        if (w <= 0 || h <= 0) return;
        int gS = estatico("gS", null), gT = estatico("gT", null);
        boolean mudou = false;
        for (Component c : area.getComponents()) {
            if (!(c instanceof JComponent) || !c.isPreferredSizeSet()) continue;
            JComponent j = (JComponent) c;
            Dimension p = j.getPreferredSize();
            boolean cheia = j.getClientProperty("brombila.cheia") != null || (p.width == gS && p.height == gT);
            if (!cheia) continue;
            j.putClientProperty("brombila.cheia", Boolean.TRUE);
            if (p.width != w || p.height != h) {
                j.setPreferredSize(new Dimension(w, h));
                mudou = true;
            }
        }
        // telas criadas daqui para frente ja nascem do tamanho certo
        estatico("gS", w);
        estatico("gT", h);
        if (mudou) area.revalidate();
    }

    private static void poeEmJanela(JFrame f) {
        Rectangle a = area(f);
        int minL = Math.min(MIN_LARG, a.width), minA = Math.min(MIN_ALT, a.height);
        int w = num("largura", Math.min(1366, a.width - 80));
        int h = num("altura", Math.min(860, a.height - 40));
        w = Math.max(minL, Math.min(w, a.width));
        h = Math.max(minA, Math.min(h, a.height));
        int x = num("x", a.x + (a.width - w) / 2), y = num("y", a.y + (a.height - h) / 2);
        // fora da tela (monitor desligado, resolucao trocada): centraliza
        if (x < a.x - w / 2 || x > a.x + a.width - 80 || y < a.y - 10 || y > a.y + a.height - 60) {
            x = a.x + (a.width - w) / 2;
            y = a.y + (a.height - h) / 2;
        }
        ignorarAte = System.currentTimeMillis() + 1500;
        f.setResizable(true);
        f.setMinimumSize(new Dimension(minL, minA));
        f.setExtendedState(Frame.NORMAL);
        f.setBounds(x, y, w, h);
        if ("1".equals(cfg.getProperty("maximizada"))) f.setExtendedState(Frame.MAXIMIZED_BOTH);
        f.validate();
    }

    // Guarda tamanho e posicao da janela quando o jogador mexe nela.
    private static void acompanha(final JFrame f) {
        if (f.getRootPane().getClientProperty("brombila.janela") != null) return;
        f.getRootPane().putClientProperty("brombila.janela", Boolean.TRUE);
        salvar = new Timer(700, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (trocando || f.isUndecorated() || !f.isShowing()) return;
                if (System.currentTimeMillis() < ignorarAte) {
                    salvar.restart();
                    return;
                }
                boolean max = (f.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
                cfg.setProperty("maximizada", max ? "1" : "0");
                if (!max) {
                    Rectangle b = f.getBounds();
                    cfg.setProperty("x", "" + b.x);
                    cfg.setProperty("y", "" + b.y);
                    cfg.setProperty("largura", "" + b.width);
                    cfg.setProperty("altura", "" + b.height);
                }
                grava();
            }
        });
        salvar.setRepeats(false);
        ComponentAdapter mexeu = new ComponentAdapter() {
            public void componentResized(ComponentEvent e) { salvar.restart(); }
            public void componentMoved(ComponentEvent e) { salvar.restart(); }
        };
        f.addComponentListener(mexeu);
        f.addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) { salvar.restart(); }
        });
    }

    // ------------------------------------------------------------ trocar

    // Muda a escolha e aplica. Trocar a borda exige fechar e reabrir a janela, o que fecharia
    // junto as janelas abertas por cima dela; por isso espera elas fecharem.
    static void alterna(final boolean janela) {
        carrega();
        cfg.setProperty("janela", janela ? "1" : "0");
        grava();
        final JFrame f = principal();
        if (f == null || !f.isShowing()) return;
        if (!temJanelaPorCima(f)) {
            aplica(f, janela);
            return;
        }
        final Timer espera = new Timer(150, null);
        espera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (temJanelaPorCima(f)) return;
                espera.stop();
                if (emJanela() == janela) aplica(f, janela);
            }
        });
        espera.start();
    }

    private static boolean temJanelaPorCima(Frame f) {
        for (Window w : f.getOwnedWindows()) {
            if (w.isShowing() && (w instanceof Dialog || w instanceof JWindow)) return true;
        }
        return false;
    }

    private static void aplica(JFrame f, boolean janela) {
        if (f.isUndecorated() != janela) return;  // ja esta assim
        trocando = true;
        try {
            Component foco = f.getMostRecentFocusOwner();
            f.dispose();
            f.setOpacity(1f);
            if (janela) {
                f.setUndecorated(false);
                f.setVisible(true);
                poeEmJanela(f);
            } else {
                f.setUndecorated(true);
                f.setMinimumSize(null);
                Rectangle a = area(f);
                f.setMaximizedBounds(new Rectangle(0, 0, a.width, a.height));
                f.setBounds(a);
                f.setExtendedState(Frame.MAXIMIZED_BOTH);
                f.setVisible(true);
            }
            f.validate();
            f.repaint();
            if (foco != null) foco.requestFocusInWindow();
            aviso(f, janela ? "Modo janela" : "Tela cheia", janela ? "F11 volta para a tela cheia" : "F11 volta para o modo janela");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { trocando = false; }
            });
        }
    }

    // ------------------------------------------------------------ chave nas Preferencias

    static JCheckBox chave() {
        final JCheckBox c = new JCheckBox("Modo janela (F11)", emJanela());
        Dialogos.interruptor(c);
        c.setToolTipText("Muda ao fechar as Preferências");
        c.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) { alterna(c.isSelected()); }
        });
        return c;
    }

    // ------------------------------------------------------------ item no menu Opcoes

    static void itemMenu(final JPopupMenu menu, JMenuItem filho) {
        String t = Menus.normaliza(filho.getText());
        if (!t.startsWith("prefer") || menu.getClientProperty("brombila.janela") != null) return;
        final JMenuItem item = new JMenuItem(emJanela() ? "Tela cheia" : "Modo janela");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { alterna(!emJanela()); }
        });
        menu.putClientProperty("brombila.janela", item);
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                item.setText(emJanela() ? "Tela cheia" : "Modo janela");
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        final JMenuItem prefs = filho;
        // depois do evento: o jogo ainda esta montando o menu
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (menu.getComponentIndex(item) < 0) menu.insert(item, menu.getComponentIndex(prefs) + 1);
            }
        });
    }

    // ------------------------------------------------------------ aviso rapido

    // Cartao pequeno no alto da tela que some sozinho ("Modo janela · F11 volta...").
    private static void aviso(JFrame f, final String titulo, final String dica) {
        final JLayeredPane camada = f.getLayeredPane();
        final long inicio = System.currentTimeMillis();
        final JComponent c = new JComponent() {
            protected void paintComponent(Graphics g) {
                long t = System.currentTimeMillis() - inicio;
                float a = t < 200 ? t / 200f : t > 1900 ? Math.max(0f, 1f - (t - 1900) / 400f) : 1f;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2.setComposite(AlphaComposite.SrcOver.derive(a));
                int w = getWidth(), h = getHeight();
                RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 22, 22);
                g2.setColor(new Color(10, 14, 12, 236));
                g2.fill(r);
                g2.setColor(new Color(0xF2, 0xC1, 0x4E, 90));
                g2.draw(r);
                g2.setColor(Estreia.OURO);
                g2.fillRoundRect(18, h / 2 - 9, 4, 18, 4, 4);
                g2.setFont(Estreia.titulo(22f));
                g2.setColor(Estreia.BRANCO);
                g2.drawString(titulo.toUpperCase(), 34, 30);
                g2.setFont(Estreia.texto(12.5f));
                g2.setColor(Estreia.FRACO);
                g2.drawString(dica, 34, 50);
                g2.dispose();
            }
        };
        int w = 300, h = 66;
        c.setBounds((camada.getWidth() - w) / 2, camada.getHeight() - h - 36, w, h);
        camada.add(c, JLayeredPane.POPUP_LAYER);
        final Timer t = new Timer(Animacao.QUADRO_MS, null);
        t.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (System.currentTimeMillis() - inicio > 2300) {
                    t.stop();
                    camada.remove(c);
                    camada.repaint(c.getBounds());
                } else {
                    // a janela pode ainda estar mudando de tamanho: fica sempre no centro de baixo
                    c.setLocation((camada.getWidth() - c.getWidth()) / 2, camada.getHeight() - c.getHeight() - 36);
                    c.repaint();
                }
            }
        });
        t.start();
    }
}
