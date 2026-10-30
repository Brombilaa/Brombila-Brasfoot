package mod;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
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
import javax.swing.SwingUtilities;
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

    // No Windows a janela translucida (por pixel ou com setOpacity) desenha quadrados escuros atras de
    // icones e botoes e apaga o texto; la as caixas ficam opacas e so o conteudo anima.
    // -Dbrombila.opaco=1 forca o modo opaco em qualquer sistema, =0 forca o translucido.
    static boolean opacoJanela() {
        String o = System.getProperty("brombila.opaco");
        if (o != null) return o.equals("1");
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean translucida() {
        if (opacoJanela()) return false;
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
        ordem.put(w, ++aberturas);
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
        // uma so tela: o conteudo fica no meio da pagina, o veu cobre so ele
        final Rectangle area = Pagina.eh(d) ? conteudo.getBounds() : new Rectangle(0, 0, camadas.getWidth(), camadas.getHeight());
        final int larg = area.width, alt = area.height;
        if (larg <= 0 || alt <= 0) return;
        // A foto do conteudo sai no primeiro quadro, depois que o tema ja vestiu a janela
        // (o tema entra na fila logo depois deste evento); ate la, so o fundo escuro.
        final BufferedImage[] img = {null};
        final long[] inicio = {0};
        // perguntas (Sim/Nao) e avisos entram com zoom; demissao e convite, com mais impacto
        final int tipo = marcante(conteudo) ? 2 : temPergunta(conteudo) ? 1 : 0;
        final int dura = tipo == 2 ? 420 : tipo == 1 ? 260 : 220;
        final JComponent veu = new JComponent() {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setColor(Estreia.NOITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (img[0] != null) {
                    float x = (System.currentTimeMillis() - inicio[0]) / (float) dura;
                    float t = suave(x);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.min(1f, t * 1.3f)));
                    if (tipo == 0) {
                        g.drawImage(img[0], 0, Math.round(12 * (1f - t)), null);
                    } else {
                        double z = tipo == 1 ? 0.9 + 0.1 * t : 0.8 + 0.2 * sobra(Math.min(1f, x));
                        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.translate(getWidth() / 2.0, getHeight() / 2.0);
                        g.scale(z, z);
                        g.drawImage(img[0], -img[0].getWidth() / 2, -img[0].getHeight() / 2, null);
                    }
                }
                g.dispose();
            }
        };
        veu.setBounds(area);
        camadas.add(veu, javax.swing.JLayeredPane.DRAG_LAYER);
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (img[0] == null) {
                    try {
                        BufferedImage i = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = i.createGraphics();
                        conteudo.paint(g);
                        g.dispose();
                        img[0] = i;
                        Sons.toca("janela");
                        inicio[0] = System.currentTimeMillis();
                    } catch (Throwable t) {
                        inicio[0] = 1; // sem foto: tira o veu ja
                    }
                }
                if (System.currentTimeMillis() - inicio[0] >= dura + 10) {
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

    // Curva que passa um pouco do fim e volta (entrada com impacto).
    static float sobra(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = t - 1f, k = 1.9f;
        return 1f + (k + 1f) * u * u * u + k * u * u;
    }

    private static boolean temPergunta(Component c) {
        if (c instanceof javax.swing.JOptionPane) return true;
        if (c instanceof java.awt.Container)
            for (Component k : ((java.awt.Container) c).getComponents()) if (temPergunta(k)) return true;
        return false;
    }

    // demissoes da rodada (a.bl) e convites para ser tecnico (a.gO)
    private static boolean marcante(Component c) {
        String n = c.getClass().getName();
        if (n.equals("a.bl") || n.equals("a.gO")) return true;
        if (c instanceof java.awt.Container)
            for (Component k : ((java.awt.Container) c).getComponents()) if (marcante(k)) return true;
        return false;
    }

    // ------------------------------------------------------------ troca de tela

    private static long ultimaTroca;

    // Uma tela grande (mais da metade da janela) saiu da janela principal (ex.: Jogar >> troca
    // a tela do time pela partida): a imagem dela fica por cima, numa camada que nao pega clique,
    // e some em 0,22 s. A imagem e desenhada do componente que acabou de sair (ainda inteiro,
    // so fora da janela). O fade so comeca no primeiro quadro, depois que o tema vestiu a tela
    // nova, entao a tela ainda sem tema nunca aparece.
    static void trocaTela(java.awt.event.ContainerEvent e) {
        if (!ligada()) return;
        final Component velho = e.getChild();
        java.awt.Container pai = e.getContainer();
        // as camadas do proprio mod (veus) saem do JLayeredPane: nao sao troca de tela
        if (pai instanceof javax.swing.JLayeredPane || pai instanceof javax.swing.CellRendererPane) return;
        Window w = SwingUtilities.getWindowAncestor(pai);
        if (!Janela.ehPrincipal(w) || !(w instanceof RootPaneContainer) || !w.isShowing()) return;
        int larg = velho.getWidth(), alt = velho.getHeight();
        JRootPane rp = ((RootPaneContainer) w).getRootPane();
        final javax.swing.JLayeredPane camadas = rp.getLayeredPane();
        if (larg * alt < camadas.getWidth() * camadas.getHeight() / 2) return;
        long agora = System.currentTimeMillis();
        if (agora - ultimaTroca < 400) return; // varias pecas saindo juntas: uma troca so
        ultimaTroca = agora;
        final BufferedImage img;
        final Point onde;
        try {
            onde = SwingUtilities.convertPoint(pai, velho.getX(), velho.getY(), camadas);
            img = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            velho.paint(g);
            g.dispose();
        } catch (Throwable t) {
            return;
        }
        final long[] inicio = {0};
        final JComponent veu = new JComponent() {
            protected void paintComponent(Graphics g0) {
                float t = inicio[0] == 0 ? 0f : suave((System.currentTimeMillis() - inicio[0]) / 220f);
                Graphics2D g = (Graphics2D) g0.create();
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f - t));
                g.drawImage(img, onde.x, onde.y, null);
                g.dispose();
            }
        };
        veu.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        camadas.add(veu, javax.swing.JLayeredPane.DRAG_LAYER);
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                long t = System.currentTimeMillis();
                if (inicio[0] == 0) inicio[0] = t;
                if (t - inicio[0] >= 240) {
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
        Long n = ordem.remove(w);
        if (!(w instanceof RootPaneContainer) || ((RootPaneContainer) w).getRootPane().getClientProperty(SAI) == null) return;
        // Se outra janela abriu depois desta e esta na tela, esta ja tinha sido escondida antes
        // do dispose (ex.: Escalar -> Jogar): nada de fantasma por cima da tela nova.
        if (n != null) {
            for (java.util.Map.Entry<Window, Long> e : ordem.entrySet()) {
                if (e.getValue() > n && e.getKey().isShowing()) return;
            }
        }
        sai(w);
    }

    private static final java.util.Map<Window, Long> ordem = new java.util.WeakHashMap<Window, Long>();
    private static long aberturas;

    // Dialogo fechou: um "fantasma" com a imagem dele fica no lugar e some em 0,15 s,
    // descendo um pouco. A janela de verdade ja foi embora, entao nada espera por isso.
    static void sai(Window w) {
        if (!ligada() || !translucida() || !(w instanceof RootPaneContainer) || Pagina.eh(w)) return;
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
        Shape s = null;
        try { s = w.getShape(); } catch (Throwable t) {}
        final Shape forma = s;
        final Window dono = w.getOwner();
        final long fechou = System.currentTimeMillis();
        // Dialogo por cima da janela do jogo: o fantasma e desenhado dentro dela, sem janela
        // nova (que leva um quadro ou dois para aparecer e deixaria a tela piscar).
        if (dono instanceof RootPaneContainer && dono.isShowing() && dentro(dono, r)) {
            fantasmaDentro(img, r, forma, (RootPaneContainer) dono, fechou);
            return;
        }
        // O fantasma sai na proxima volta da fila: se o jogo prender a tela logo depois do
        // dispose (ex.: Jogar >> montando a partida), ja passou da hora e nem aparece.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (System.currentTimeMillis() - fechou <= 60) fantasma(img, r, forma, dono);
            }
        });
    }

    private static boolean dentro(Window dono, Rectangle r) {
        try {
            Rectangle d = dono.getBounds();
            d.grow(2, 2);
            return d.contains(r);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void fantasmaDentro(final BufferedImage img, Rectangle r, final Shape forma, RootPaneContainer dono,
            final long fechou) {
        final javax.swing.JLayeredPane camadas = dono.getRootPane().getLayeredPane();
        final Point p;
        try {
            p = Encolhe.daTela(new Point(r.x, r.y), camadas);
        } catch (Throwable t) {
            return;
        }
        // a foto e da tela de verdade; numa janela reduzida ela entra menor
        final double k = Encolhe.paraVirtual(camadas);
        final int iw = (int) Math.ceil(img.getWidth() * k), ih = (int) Math.ceil(img.getHeight() * k);
        final long[] inicio = {0};
        final JComponent veu = new JComponent() {
            protected void paintComponent(Graphics g0) {
                long agora = System.currentTimeMillis();
                // o jogo prendeu a tela depois de fechar: nada de fantasma atrasado
                if (inicio[0] == 0 && agora - fechou > 60 || inicio[0] < 0) return;
                float t = inicio[0] == 0 ? 0f : Math.min(1f, (agora - inicio[0]) / 150f);
                Graphics2D g = (Graphics2D) g0.create();
                g.translate(p.x, p.y + Math.round(8 * suave(t)));
                if (k != 1) {
                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.scale(k, k);
                }
                if (forma != null) g.clip(forma);
                g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f - suave(t)));
                g.drawImage(img, 0, 0, null);
                g.dispose();
            }
        };
        veu.setBounds(0, 0, camadas.getWidth(), camadas.getHeight());
        camadas.add(veu, javax.swing.JLayeredPane.DRAG_LAYER);
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long agora = System.currentTimeMillis();
                // a tela ficou presa (o jogo montando a proxima): some de uma vez
                if (inicio[0] == 0) inicio[0] = agora - fechou > 60 ? -1 : agora;
                if (inicio[0] < 0 || agora - inicio[0] >= 150) {
                    timer.stop();
                    camadas.remove(veu);
                    camadas.repaint(p.x, p.y, iw, ih + 10);
                    return;
                }
                camadas.repaint(p.x, p.y, iw, ih + 10);
            }
        });
        timer.start();
    }

    private static void fantasma(final BufferedImage img, final Rectangle r, Shape forma, Window dono) {
        final JWindow f = new JWindow(dono);
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
        // pinta ja, antes de o jogo seguir: se ele prender a tela logo depois (ex.: montando
        // a partida), o fantasma fica com a imagem certa em vez de um quadro cinza
        f.getRootPane().paintImmediately(0, 0, r.width, r.height);
        java.awt.Toolkit.getDefaultToolkit().sync();
        final long criado = System.currentTimeMillis();
        final long[] inicio = {0};
        final Timer timer = new Timer(QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long agora = System.currentTimeMillis();
                if (inicio[0] == 0) {
                    if (agora - criado > 90) { // a tela ficou presa: deixa pra la
                        timer.stop();
                        f.dispose();
                        return;
                    }
                    inicio[0] = agora;
                }
                float t = (agora - inicio[0]) / 150f;
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
