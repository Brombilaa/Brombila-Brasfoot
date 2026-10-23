package mod;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

// Sons do mod (mod/sons, dentro do .exe). Carrega numa thread para nao travar a tela e,
// se o som ficar pronto depois da hora, comeca do ponto certo (fica junto da animacao).
// -Dbrombila.som=0 ou "Som da abertura" nas Preferencias (brombila.ini som=0) desliga.
final class Som {
    private final String recurso;
    private volatile Clip clip;
    private volatile long comeco = -1; // hora (ms) em que o som deveria ter comecado
    private volatile boolean parado;

    private Som(String recurso) { this.recurso = recurso; }

    static Som carrega(String recurso) {
        final Som s = new Som(recurso);
        if ("0".equals(System.getProperty("brombila.som")) || "0".equals(Janela.pref("som", "1"))) {
            s.parado = true;
            return s;
        }
        Thread t = new Thread(new Runnable() {
            public void run() { s.abre(); }
        }, "brombila-som");
        t.setDaemon(true);
        t.start();
        return s;
    }

    private void abre() {
        try {
            InputStream in = Som.class.getResourceAsStream(recurso);
            if (in == null) return;
            AudioInputStream a = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
            Clip c = AudioSystem.getClip();
            c.open(a);
            a.close();
            synchronized (this) {
                clip = c;
                if (parado) { c.close(); clip = null; return; }
                if (comeco >= 0) inicia();
            }
        } catch (Throwable t) {
            // sem placa de som ou formato nao suportado: segue sem som
        }
    }

    // Toca como se tivesse comecado em 'quando' (System.currentTimeMillis()).
    synchronized void toca(long quando) {
        if (parado) return;
        comeco = quando;
        if (clip != null) inicia();
    }

    private void inicia() {
        long atraso = System.currentTimeMillis() - comeco;
        float taxa = clip.getFormat().getFrameRate();
        int quadro = (int) Math.max(0, atraso * taxa / 1000f);
        if (quadro >= clip.getFrameLength()) return;
        clip.setFramePosition(quadro);
        clip.start();
    }

    // Abaixa o volume em 'ms' e para (ao pular a abertura).
    void some(final int ms) {
        final Clip c;
        synchronized (this) {
            parado = true;
            c = clip;
        }
        if (c == null) return;
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                        float de = g.getValue(), ate = Math.max(g.getMinimum(), -40f);
                        for (int i = 1; i <= 15; i++) {
                            g.setValue(de + (ate - de) * i / 15f);
                            Thread.sleep(Math.max(1, ms / 15));
                        }
                    }
                } catch (Throwable x) {
                    // para direto
                }
                c.stop();
                c.close();
            }
        }, "brombila-som-some");
        t.setDaemon(true);
        t.start();
    }
}
