package mod;

import java.util.ArrayList;
import java.util.Calendar;

// Avisa as novidades do mod quando o tempo do jogo passa: um dia, uma semana, uma temporada.
// Em vez de mexer no calendario do jogo (best.f, 3.000 linhas), um Timer olha a data do jogo
// aberto (c.a.SR.bb()) e compara com a ultima data vista, que fica guardada na Carreira. Assim
// carregar um save nao repete avisos e cada dia e avisado uma vez so.
public final class Tempo {

    public interface Ouvinte {
        // de: data anterior, ate: data nova (o jogo pode pular varios dias de uma vez)
        void passou(Calendar de, Calendar ate, boolean semana, boolean temporada);
    }

    static final ArrayList<Ouvinte> ouvintes = new ArrayList<Ouvinte>();
    static javax.swing.Timer relogio;
    static Object visto;               // jogo (c.a.SR) da ultima olhada
    // tela principal do jogo (a.eg, ver Principal): so conta o tempo com ela aberta, porque ao
    // criar um jogo novo o calendario passa por datas provisorias antes da primeira
    static java.lang.ref.WeakReference<javax.swing.JPanel> principal;
    public static int avisos;          // para os testes
    public static String ultimoAviso = "";

    public static void ouve(Ouvinte o) { ouvintes.add(o); }

    public static void liga() {
        if (relogio != null) return;
        relogio = new javax.swing.Timer(400, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                try { olha(); } catch (Throwable t) { /* jogo ainda montando */ }
            }
        });
        relogio.start();
    }

    // logo depois de carregar: a data do save vira o ponto de partida, sem aviso
    static void carregou() {
        visto = null;
    }

    static void olha() {
        best.f sr = c.a.SR;
        javax.swing.JPanel eg = principal == null ? null : principal.get();
        if (sr == null || eg == null || !eg.isShowing()) return;
        Calendar agora = sr.bb();
        if (agora == null) return;
        long dia = dia(agora);
        java.util.Map<String, java.io.Serializable> g = Carreira.geral();
        Object antes = g.get("data");
        if (sr != visto || !(antes instanceof Long)) {
            // jogo novo ou recem-carregado: so marca a data
            visto = sr;
            if (!(antes instanceof Long)) g.put("data", dia);
            return;
        }
        long ultimo = (Long) antes;
        if (dia <= ultimo) return;
        g.put("data", dia);
        Calendar de = Calendar.getInstance();
        de.setTimeInMillis(ultimo);
        Calendar ate = Calendar.getInstance();
        ate.setTimeInMillis(dia);
        boolean temporada = ate.get(Calendar.YEAR) != de.get(Calendar.YEAR);
        boolean semana = temporada || ate.get(Calendar.WEEK_OF_YEAR) != de.get(Calendar.WEEK_OF_YEAR)
            || dia - ultimo >= 7L * 24 * 3600 * 1000;
        avisos++;
        ultimoAviso = de.getTime() + " -> " + ate.getTime() + (semana ? " semana" : "") + (temporada ? " temporada" : "");
        for (Ouvinte o : new ArrayList<Ouvinte>(ouvintes)) {
            try {
                o.passou(de, ate, semana, temporada);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static long dia(Calendar c) {
        Calendar d = Calendar.getInstance();
        d.clear();
        d.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return d.getTimeInMillis();
    }
}
