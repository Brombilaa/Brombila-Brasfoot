package mod;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;

// Sons do mod (mod/sons dentro do .exe, feitos por tools/som/monta.py).
// - Os 7 sons que o proprio jogo toca na partida (gol, penalti...) sao lidos do disco
//   (pasta sons/): instalaJogo() grava os nossos la, ja no volume escolhido.
// - Os sons novos tocam por aqui: toca("nome"). Tudo respeita a opcao do jogo "usar sons"
//   e as chaves das Preferencias (brombila.ini: torcida, sonsui, passa, volume).
// O audio abre e toca numa fila propria, para nunca travar a tela.
final class Sons {
    private Sons() {}

    static final String[] JOGO = {"intervalo", "fimjogo", "gol1", "goladv", "penalty", "expulsao", "contusao"};
    private static final String[] INTERFACE = {"clique", "passa", "moedas", "mensagem", "carimbo", "carta", "janela"};

    private static final ExecutorService fila = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "brombila-sons");
            t.setDaemon(true);
            return t;
        }
    });
    // cada som tem algumas copias abertas, para tocar de novo antes do anterior acabar
    private static final Map<String, Clip[]> abertos = new HashMap<String, Clip[]>();
    private static final Map<String, Integer> vez = new HashMap<String, Integer>();
    private static Clip ambiente;
    private static final boolean DEBUG = System.getProperty("brombila.debug") != null;
    private static volatile boolean ambienteLigado;

    // ------------------------------------------------------------------ chaves

    // volume geral do mod, 0 a 100
    static int volume() {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(Janela.pref("volume", "80"))));
        } catch (NumberFormatException e) {
            return 80;
        }
    }

    // opcao "usar sons" do jogo salvo; no menu (sem jogo aberto) vale ligada
    static boolean jogoUsaSons() {
        try {
            return c.a.SR == null || c.a.SR.isUsaSons();
        } catch (Throwable t) {
            return true;
        }
    }

    private static boolean ehInterface(String nome) {
        for (String s : INTERFACE) if (s.equals(nome)) return true;
        return false;
    }

    static boolean pode(String nome) {
        if ("0".equals(System.getProperty("brombila.som")) || volume() == 0 || !jogoUsaSons()) return false;
        if ("passa".equals(nome)) return "1".equals(Janela.pref("passa", "0"));
        if (ehInterface(nome)) return !"0".equals(Janela.pref("sonsui", "1"));
        if ("ambiente".equals(nome)) return !"0".equals(Janela.pref("torcida", "1"));
        return true;
    }

    // ------------------------------------------------------------------ tocar

    static void toca(final String nome) {
        if (DEBUG) System.err.println("[som] " + nome + (pode(nome) ? "" : " (desligado)"));
        if (!pode(nome)) return;
        fila.execute(new Runnable() {
            public void run() {
                try {
                    Clip c = clip(nome);
                    if (c == null) return;
                    ganho(c, 0f);
                    c.stop();
                    c.setFramePosition(0);
                    c.start();
                } catch (Throwable t) {
                    // sem placa de som: segue sem
                }
            }
        });
    }

    // torcida de fundo da partida, em loop; entra e sai devagar
    static void ambiente(final boolean liga) {
        if (liga == ambienteLigado) return;
        if (DEBUG) System.err.println("[som] ambiente " + liga);
        ambienteLigado = liga;
        if (liga && !pode("ambiente")) { ambienteLigado = false; return; }
        fila.execute(new Runnable() {
            public void run() {
                try {
                    if (liga) {
                        if (ambiente == null) ambiente = abre("ambiente");
                        if (ambiente == null) return;
                        ganho(ambiente, -40f);
                        ambiente.loop(Clip.LOOP_CONTINUOUSLY);
                        rampa(ambiente, -40f, 0f, 900);
                    } else if (ambiente != null && ambiente.isRunning()) {
                        rampa(ambiente, 0f, -40f, 700);
                        ambiente.stop();
                    }
                } catch (Throwable t) {
                    // sem som
                }
            }
        });
    }

    // as Preferencias mudaram: aplica na hora
    static void mudou() {
        if (ambienteLigado && !pode("ambiente")) {
            ambiente(false);
        }
        fila.execute(new Runnable() {
            public void run() {
                if (ambiente != null && ambiente.isRunning()) ganho(ambiente, 0f);
            }
        });
        instalaJogo();
    }

    private static Clip clip(String nome) throws Exception {
        Clip[] cs = abertos.get(nome);
        if (cs == null) {
            cs = new Clip[ehInterface(nome) ? 3 : 2];
            abertos.put(nome, cs);
        }
        int i = vez.containsKey(nome) ? (vez.get(nome) + 1) % cs.length : 0;
        vez.put(nome, i);
        if (cs[i] == null) cs[i] = abre(nome);
        return cs[i];
    }

    private static Clip abre(String nome) throws Exception {
        byte[] b = le("/mod/sons/" + nome + ".wav");
        if (b == null) return null;
        AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(b));
        Clip c = AudioSystem.getClip();
        c.open(in);
        in.close();
        return c;
    }

    // volume geral (e um ajuste em dB por cima)
    private static void ganho(Clip c, float extra) {
        try {
            if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (20 * Math.log10(Math.max(0.01, volume() / 100.0))) + extra;
            g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
        } catch (Throwable t) {
            // sem controle de volume
        }
    }

    private static void rampa(Clip c, float de, float ate, int ms) throws InterruptedException {
        for (int i = 1; i <= 20; i++) {
            ganho(c, de + (ate - de) * i / 20f);
            Thread.sleep(ms / 20);
        }
    }

    private static byte[] le(String recurso) {
        try {
            InputStream in = Sons.class.getResourceAsStream(recurso);
            if (in == null) return null;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            for (int k; (k = in.read(buf)) > 0; ) b.write(buf, 0, k);
            in.close();
            return b.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ sons do jogo (disco)

    // O jogo le sons/*.wav da pasta dele. Grava os nossos la, com o volume ja aplicado
    // (o jogo nao tem controle de volume). Volume novo vale a partir da proxima partida.
    static void instalaJogo() {
        final int vol = volume();
        fila.execute(new Runnable() {
            public void run() {
                for (String n : JOGO) {
                    try {
                        byte[] b = le("/mod/sons/jogo/" + n + ".wav");
                        if (b == null) continue;
                        byte[] novo = escala(b, vol);
                        File f = new File(System.getProperty("user.dir"), "sons/" + n + ".wav");
                        if (f.isFile() && java.util.Arrays.equals(java.nio.file.Files.readAllBytes(f.toPath()), novo)) continue;
                        f.getParentFile().mkdirs();
                        java.nio.file.Files.write(f.toPath(), novo);
                    } catch (Throwable t) {
                        // sem permissao de escrita: ficam os sons que estiverem la
                    }
                }
            }
        });
    }

    // WAV PCM 16 bits com o volume aplicado nas amostras
    static byte[] escala(byte[] wav, int vol) throws Exception {
        if (vol >= 100) return wav;
        AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav));
        AudioFormat f = in.getFormat();
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        for (int k; (k = in.read(buf)) > 0; ) pcm.write(buf, 0, k);
        byte[] d = pcm.toByteArray();
        double g = vol / 100.0;
        boolean grande = f.isBigEndian();
        for (int i = 0; i + 1 < d.length; i += 2) {
            int lo = d[grande ? i + 1 : i] & 0xFF, hi = d[grande ? i : i + 1];
            int s = (int) Math.round(((hi << 8) | lo) * g);
            d[grande ? i + 1 : i] = (byte) s;
            d[grande ? i : i + 1] = (byte) (s >> 8);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(d), f, d.length / f.getFrameSize()),
            javax.sound.sampled.AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ cliques

    // Clique em qualquer botao, item de menu ou chave; som de passar o mouse so nos
    // botoes grandes (e desligado de fabrica).
    static void instalaCliques() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (!(e instanceof MouseEvent)) return;
                MouseEvent m = (MouseEvent) e;
                Component c = m.getComponent();
                if (!(c instanceof AbstractButton) || !c.isEnabled()) return;
                if (m.getID() == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(m)) toca("clique");
                else if (m.getID() == MouseEvent.MOUSE_ENTERED && c.getHeight() >= 30) toca("passa");
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    // ------------------------------------------------------------------ Preferencias

    // Botao "Sons" das Preferencias: abre um cartao com as chaves e o volume.
    static javax.swing.JButton botaoPreferencias() {
        final javax.swing.JButton b = new javax.swing.JButton();
        Editor.botao(b, "Sons", null, false);
        b.setToolTipText("Sons da partida, da torcida e da interface, e o volume");
        b.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                javax.swing.JPopupMenu m = new javax.swing.JPopupMenu();
                javax.swing.JPanel p = new javax.swing.JPanel(null);
                p.setOpaque(false);
                String[][] chaves = {
                    {"Som da abertura", "som", "1", "Som da animação de abertura"},
                    {"Torcida na partida", "torcida", "1", "Torcida de fundo durante a partida"},
                    {"Sons da interface", "sonsui", "1", "Cliques, dinheiro, mensagens, carimbos"},
                    {"Som ao passar o mouse", "passa", "0", "Um toque baixinho nos botões grandes"},
                };
                int y = 12;
                javax.swing.JLabel tit = new javax.swing.JLabel("SONS");
                tit.setFont(Estreia.negrito(11f));
                tit.setForeground(Estreia.OURO);
                tit.setBounds(16, y, 200, 16);
                p.add(tit);
                y += 24;
                for (final String[] c : chaves) {
                    final javax.swing.JCheckBox cb = new javax.swing.JCheckBox(c[0], !"0".equals(Janela.pref(c[1], c[2])));
                    cb.setUI(new ConfigEstreia.Interruptor());
                    cb.setOpaque(false);
                    cb.setFont(Estreia.texto(13f));
                    cb.setForeground(Estreia.BRANCO);
                    cb.setToolTipText(c[3]);
                    cb.addItemListener(new java.awt.event.ItemListener() {
                        public void itemStateChanged(java.awt.event.ItemEvent ev) {
                            Janela.grava(c[1], cb.isSelected() ? "1" : "0");
                            mudou();
                        }
                    });
                    cb.setBounds(12, y, 236, 26);
                    p.add(cb);
                    y += 30;
                }
                javax.swing.JLabel v = new javax.swing.JLabel("Volume");
                v.setFont(Estreia.semi(12f));
                v.setForeground(Estreia.FRACO);
                v.setBounds(16, y + 4, 60, 18);
                p.add(v);
                final javax.swing.JSlider vol = new javax.swing.JSlider(0, 100, volume());
                vol.setUI(new Editor.Trilho(vol));
                vol.setOpaque(false);
                vol.setBounds(76, y, 172, 26);
                vol.addChangeListener(new javax.swing.event.ChangeListener() {
                    public void stateChanged(javax.swing.event.ChangeEvent ev) {
                        if (vol.getValueIsAdjusting()) return;
                        Janela.grava("volume", String.valueOf(vol.getValue()));
                        mudou();
                        toca("clique");
                    }
                });
                p.add(vol);
                y += 34;
                javax.swing.JLabel nota = new javax.swing.JLabel("<html>Tudo segue a opção “Sons nos eventos dos jogos”.</html>");
                nota.setFont(Estreia.texto(11f));
                nota.setForeground(Estreia.FRACO);
                nota.setBounds(16, y, 232, 30);
                p.add(nota);
                y += 38;
                p.setPreferredSize(new java.awt.Dimension(262, y));
                m.add(p);
                m.show(b, 0, b.getHeight() + 4);
            }
        });
        return b;
    }
}
