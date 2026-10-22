package mod;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTable;
import javax.swing.Timer;

// Pequenas animacoes: fade-in de janelas e o brilho da linha sob o mouse.
final class Animacao {
    static final int QUADRO_MS = 15; // ~60 quadros por segundo

    private Animacao() {}

    // Curva suave (ease-out cubica): comeca rapido e desacelera no fim.
    static float suave(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float u = 1f - t;
        return 1f - u * u * u;
    }

    // Janelas sem borda (popups do jogo e a tela principal) aparecem com fade-in.
    // Janelas com borda nao aceitam opacidade no Java, entao ficam como estao.
    static void fadeIn(final Window w) {
        boolean semBorda = (w instanceof Frame && ((Frame) w).isUndecorated())
            || (w instanceof Dialog && ((Dialog) w).isUndecorated());
        if (!semBorda) return;
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (!gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) return;
            w.setOpacity(0f);
        } catch (Throwable t) {
            return;
        }
        final long inicio = System.currentTimeMillis();
        final int duracao = 180;
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
                float f = suave((System.currentTimeMillis() - inicio) / 120f);
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
