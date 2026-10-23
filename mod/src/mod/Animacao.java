package mod;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.CellRendererPane;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.JWindow;
import javax.swing.RootPaneContainer;
import javax.swing.Timer;

// Pequenas animacoes: janelas entrando e saindo, valores que deslizam (abas, barras...)
// e o brilho da linha sob o mouse. Tudo curto (0,12 a 0,6 s) e nada segura o jogo.
// "Animacoes" nas Preferencias (brombila.ini: animacoes=0) desliga tudo de uma vez.
final class Animacao {
    static final int QUADRO_MS = 15; // ~60 quadros por segundo
    // janela que o mod nao deve animar (fantasmas, avisos proprios)
    static final String SEM = "brombila.semAnimacao";

    private Animacao() {}

    static boolean ligada() { return !"0".equals(Janela.pref("animacoes", "1")); }

    // Curva suave (ease-out cubica): comeca rapido e desacelera no fim.
    static float suave(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    // ------------------------------------------------------------ valores animados

    private static final class Estado {
        float de, alvo;
        long t0;
        int ms;

        float atual(long agora) {
            float t = (agora - t0) / (float) ms;
            if (t >= 1f) { de = alvo; return alvo; }
            return de + (alvo - de) * suave(t);
        }
    }

    private static final Set<Component> repintar = new LinkedHashSet<Component>();
    private static Timer relogio;

    // Valor que desliza ate 'alvo' em 'ms' sempre que o alvo muda. Chamado dentro do
    // paint: devolve o valor do quadro e pede o proximo quadro enquanto nao chega.
    // 'primeiro' e de onde comeca na primeira vez (NaN = ja no alvo, sem animar).
    // Componente de tabela (desenhado por carimbo) nao anima: muda de linha a linha.
    static float valor(JComponent c, String chave, float alvo, int ms, float primeiro) {
        if (c.getParent() instanceof CellRendererPane || !ligada()) {
            c.putClientProperty(chave, null);
            return alvo;
        }
        long agora = System.currentTimeMillis();
        Object o = c.getClientProperty(chave);
        Estado e;
        if (o instanceof Estado) {
            e = (Estado) o;
        } else {
            e = new Estado();
            e.de = Float.isNaN(primeiro) ? alvo : primeiro;
            e.alvo = alvo;
            e.t0 = agora;
            e.ms = ms;
            c.putClientProperty(chave, e);
        }
        float atual = e.atual(agora);
        if (e.alvo != alvo) {
            e.de = atual;
            e.alvo = alvo;
            e.t0 = agora;
            e.ms = ms;
        }
        if (atual != e.alvo) repinta(c);
        return atual;
    }

    // Pede um quadro novo para c no proximo tique (60 por segundo, um relogio so).
    static void repinta(Component c) {
        repintar.add(c);
        if (relogio == null) {
            relogio = new Timer(QUADRO_MS, new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    List<Component> lista = new ArrayList<Component>(repintar);
                    repintar.clear();
                    for (Component c : lista) c.repaint();
                    if (repintar.isEmpty() && lista.isEmpty()) relogio.stop();
                }
            });
        }
        if (!relogio.isRunning()) relogio.start();
    }

    // ------------------------------------------------------------ janelas

    private static boolean semBorda(Window w) {
        return (w instanceof Frame && ((Frame) w).isUndecorated())
            || (w instanceof Dialog && ((Dialog) w).isUndecorated())
            || w instanceof JWindow;
    }

    private static boolean translucida() {
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            return gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean marcada(Window w) {
        return w instanceof RootPaneContainer && ((RootPaneContainer) w).getRootPane().getClientProperty(SEM) != null;
    }

    // Caixas e janelas por cima (dialogos): o conteudo sobe 12 px e clareia (entraConteudo);
    // as sem borda tambem fazem fade da janela inteira e, ao fechar, somem com fade (sai).
    // Outras janelas sem borda so fazem o fade. A janela em si nao muda de lugar: o
    // gerenciador de janelas pode estar ajeitando a posicao dela nesse momento.
    // Janelas com borda nao aceitam opacidade no Java, entao nelas so o conteudo anima.
    static void fadeIn(final Window w) {
        if (marcada(w)) return;
        boolean dialogo = w instanceof Dialog || (w instanceof JWindow && w.getOwner() != null);
        boolean fade = semBorda(w) && translucida();
        if (dialogo && fade && w instanceof RootPaneContainer) ((RootPaneContainer) w).getRootPane().putClientProperty(SAI, Boolean.TRUE);
        if (!ligada()) return;
        if (w instanceof Dialog) entraConteudo((Dialog) w);
        if (!fade) return;
        try {
            w.setOpacity(0f);
        } catch (Throwable t) {
            return;
        }
        final long inicio = System.currentTimeMillis();
        final int duracao = dialogo ? 210 : 180;
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                float t = (System.currentTimeMillis() - inicio) / (float) duracao;
                try {
                    w.setOpacity(t >= 1f ? 1f : suave(t));
                } catch (Throwable x) {
                    t = 1f;
                }
                if (t >= 1f || !w.isDisplayable()) timer.stop();
            }
        });
        timer.start();
    }

    // O conteudo do dialogo entra subindo 12 px e clareando do fundo escuro em 0,22 s,
    // desenhado por cima numa camada que nao pega clique e sai sozinha no fim.
    private static void entraConteudo(Dialog d) {
        if (!ligada() || !(d instanceof RootPaneContainer)) return;
        final JRootPane rp = ((RootPaneContainer) d).getRootPane();
        final javax.swing.JLayeredPane camadas = rp.getLayeredPane();
        final Component conteudo = rp.getContentPane();
        final int larg = camadas.getWidth(), alt = camadas.getHeight();
        if (larg <= 0 || alt <= 0) return;
        final BufferedImage img = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_ARGB);
        try {
            Graphics2D g = img.createGraphics();
            conteudo.paint(g);
            g.dispose();
        } catch (Throwable t) {
            return;
        }
        final long inicio = System.currentTimeMillis();
        final JComponent veu = new JComponent() {
            protected void paintComponent(Graphics g0) {
                float t = suave((System.currentTimeMillis() - inicio) / 220f);
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(Estreia.NOITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, t));
                g.drawImage(img, 0, Math.round(12 * (1f - t)), null);
                g.dispose();
            }
        };
        veu.setBounds(0, 0, larg, alt);
        camadas.add(veu, javax.swing.JLayeredPane.DRAG_LAYER);
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (System.currentTimeMillis() - inicio >= 230) {
                    timer.stop();
                    camadas.remove(veu);
                    camadas.repaint();
                    return;
                }
                veu.repaint();
            }
        });
        timer.start();
    }

    private static final String SAI = "brombila.sai";

    // Janela fechou (WINDOW_CLOSED, depois do dispose): se entrou animada, sai animada.
    static void fechou(Window w) {
        if (w instanceof RootPaneContainer && ((RootPaneContainer) w).getRootPane().getClientProperty(SAI) != null) sai(w);
    }

    // Dialogo fechou: um "fantasma" com a imagem dele fica no lugar e some em 0,15 s,
    // descendo um pouco. A janela de verdade ja foi embora, entao nada espera por isso.
    static void sai(Window w) {
        if (!ligada() || !translucida() || !(w instanceof RootPaneContainer)) return;
        final int larg = w.getWidth(), alt = w.getHeight();
        if (larg <= 0 || alt <= 0 || larg * alt > 4000000) return;
        final BufferedImage img;
        try {
            img = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            ((RootPaneContainer) w).getRootPane().paint(g);
            g.dispose();
        } catch (Throwable t) {
            return;
        }
        final Rectangle r = w.getBounds();
        Shape forma = null;
        try { forma = w.getShape(); } catch (Throwable t) {}
        final JWindow f = new JWindow(w.getOwner());
        f.getRootPane().putClientProperty(SEM, Boolean.TRUE);
        f.setFocusableWindowState(false);
        f.setContentPane(new JComponent() {
            protected void paintComponent(Graphics g) { g.drawImage(img, 0, 0, null); }
        });
        f.setBounds(r);
        try {
            if (forma != null) f.setShape(forma);
            f.setOpacity(1f);
        } catch (Throwable t) {
            return;
        }
        f.setVisible(true);
        final long inicio = System.currentTimeMillis();
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                float t = (System.currentTimeMillis() - inicio) / 150f;
                if (t >= 1f) {
                    timer.stop();
                    f.dispose();
                    return;
                }
                try {
                    f.setOpacity(1f - suave(t));
                    f.setLocation(r.x, r.y + Math.round(8 * suave(t)));
                } catch (Throwable x) {
                    timer.stop();
                    f.dispose();
                }
            }
        });
        timer.start();
    }

    // ------------------------------------------------------------ preferencias

    // Chave liga/desliga das Preferencias guardada no brombila.ini (padrao: ligada).
    static JCheckBox chave(String texto, final String pref, String dica) {
        final JCheckBox c = new JCheckBox(texto, !"0".equals(Janela.pref(pref, "1")));
        Dialogos.interruptor(c);
        if (dica != null) c.setToolTipText(dica);
        c.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent e) { Janela.grava(pref, c.isSelected() ? "1" : "0"); }
        });
        return c;
    }

    // ------------------------------------------------------------ linha sob o mouse

    // Quando o mouse muda de linha, a nova linha "acende" em ~120 ms.
    static void linhaSobMouse(final JTable t, int linha) {
        Object antes = t.getClientProperty(Tema.LINHA_MOUSE);
        if (antes instanceof Integer && (Integer) antes == linha) return;
        t.putClientProperty(Tema.LINHA_MOUSE, linha);
        repintaLinha(t, antes instanceof Integer ? (Integer) antes : -1);
        if (linha < 0) return;
        final int alvo = linha;
        final long inicio = System.currentTimeMillis();
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object atual = t.getClientProperty(Tema.LINHA_MOUSE);
                float f = ligada() ? suave((System.currentTimeMillis() - inicio) / 120f) : 1f;
                if (!(atual instanceof Integer) || (Integer) atual != alvo) {
                    timer.stop();
                    return;
                }
                t.putClientProperty(Tema.BRILHO, f);
                repintaLinha(t, alvo);
                if (f >= 1f) timer.stop();
            }
        });
        t.putClientProperty(Tema.BRILHO, 0f);
        timer.start();
    }

    static void repintaLinha(JTable t, int linha) {
        if (linha < 0 || linha >= t.getRowCount()) return;
        java.awt.Rectangle r = t.getCellRect(linha, 0, true);
        t.repaint(0, r.y, t.getWidth(), r.height);
    }
}
