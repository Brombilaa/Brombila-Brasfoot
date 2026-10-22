package mod;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.Map;

// Nota ao vivo de cada jogador durante a partida. E so uma estimativa para a tela:
// a nota oficial continua sendo a do jogo (F.a), calculada no fim. Segue as mesmas
// regras dela (resultado e forca, fora de posicao, posse para o meio) e soma o que
// ja aconteceu no jogo (gols, assistencias, cartoes, defesas, gols sofridos).
final class Notas {

    static final String[] POSICAO = {"GOL", "LAT", "ZAG", "MEI", "ATA"};

    static final class Lances {
        int gols, penaltis, contra, assist, amarelo, vermelho;
    }

    // gols, assistencias e cartoes de cada jogador ate agora
    static Map<Object, Lances> lances(Object jogo) {
        Map<Object, Lances> r = new IdentityHashMap<Object, Lances>();
        for (Object ev : Partida.lista(Partida.chama(jogo, "hE"))) {
            if (!Boolean.TRUE.equals(Partida.chama(ev, "isDone"))) continue;
            int tipo = Partida.inteiro(Partida.chama(ev, "b"));
            Object f = Partida.chama(ev, "eo");
            if (f == null) continue;
            Lances l = pega(r, f);
            if (tipo == 1) {
                int dz = Partida.inteiro(Partida.chama(ev, "el"));
                if (dz == 2) l.contra++;
                else {
                    if (dz == 3) l.penaltis++;
                    else l.gols++;
                    Object passe = Partida.chama(ev, "ep");
                    if (passe != null && passe != f) pega(r, passe).assist++;
                }
            } else if (tipo == 2) l.amarelo++;
            else if (tipo == 3 || tipo == 4) l.vermelho++;
        }
        return r;
    }

    static Lances pega(Map<Object, Lances> r, Object f) {
        Lances l = r.get(f);
        if (l == null) { l = new Lances(); r.put(f, l); }
        return l;
    }

    // base do jogo no fim da partida, pelo resultado e pela forca (F.a)
    static double base(int saldo, int forca) {
        int faixa = forca <= 30 ? 0 : forca <= 60 ? 1 : forca <= 90 ? 2 : 3;
        double[] empate = {5.5, 5.8, 6.2, 6.8}, vitoria = {6.0, 6.0, 6.7, 7.2}, derrota = {5.0, 5.2, 5.5, 6.0};
        return (saldo > 0 ? vitoria : saldo < 0 ? derrota : empate)[faixa];
    }

    // minuto: 0..90 (corrido)
    static double nota(Object f, int vaga, int lado, Object jogo, int minuto, Map<Object, Lances> ev) {
        double prog = Math.max(0, Math.min(1, minuto / 90.0));
        int pro = Partida.inteiro(Partida.chama(jogo, lado == 0 ? "hu" : "hw"));
        int contra = Partida.inteiro(Partida.chama(jogo, lado == 0 ? "hw" : "hu"));
        int forca = Partida.inteiro(Partida.chama(f, "fi"));
        double n = 6.0 + (base(pro - contra, forca) - 6.0) * prog;
        if (Boolean.TRUE.equals(Partida.chama(f, "gF"))) n -= vaga == 1 ? 3.0 : 1.5;
        int papel = Partida.papel(vaga);
        if (papel == Partida.MEIO) {
            Object hz = Partida.chama(jogo, "hz");
            if (hz instanceof int[] && ((int[]) hz).length >= 2) {
                int[] p = (int[]) hz;
                int soma = p[0] + p[1];
                if (soma > 0) n += Math.max(-0.8, Math.min(0.8, (p[lado] / (double) soma - 0.5) * 2.4)) * prog;
            }
        }
        if (papel == Partida.GOLEIRO) {
            int noAlvo = alvo(jogo, 1 - lado);
            n += Math.min(1.2, Math.max(0, noAlvo - contra) * 0.2) - contra * 0.3;
        } else if (papel == Partida.DEFESA) {
            n -= contra * 0.2;
            if (contra == 0) n += 0.4 * prog;
        }
        Lances l = ev.get(f);
        if (l != null) {
            n += l.gols * 1.0 + l.penaltis * 0.8 + l.assist * 0.5 - l.contra * 1.0 - l.amarelo * 0.3 - l.vermelho * 1.5;
        }
        int energia = Partida.inteiro(Partida.chama(f, "fp"));
        if (energia < 40) n -= 0.3;
        n = Math.max(3, Math.min(10, n));
        return Math.round(n * 10) / 10.0;
    }

    static int alvo(Object jogo, int lado) {
        Object v = Partida.chama(jogo, "hZ");
        return v instanceof int[] && ((int[]) v).length > lado ? ((int[]) v)[lado] : 0;
    }

    static Color cor(double n) {
        return n >= 7.5 ? new Color(0x3FD47E) : n >= 6.5 ? new Color(0x9ED8A8) : n >= 5.5 ? new Color(0xE9E4D6) : n >= 4.5 ? new Color(0xF2C14E) : new Color(0xFF6B5E);
    }

    static String texto(double n) {
        return String.format(java.util.Locale.ROOT, "%.1f", n).replace('.', ',');
    }

    // posicao natural do jogador (GOL, LAT, ZAG, MEI, ATA)
    static String natural(Object f) {
        int p = Partida.inteiro(Partida.chama(f, "getPosicao"));
        return p >= 0 && p < POSICAO.length ? POSICAO[p] : "?";
    }

    // funcao da vaga em que esta jogando (tabela do jogo aq.sE; se faltar, pela vaga)
    static String funcao(int vaga) {
        try {
            java.lang.reflect.Field t = Class.forName("best.aq").getDeclaredField("sE");
            t.setAccessible(true);
            int[][] se = (int[][]) t.get(null);
            if (vaga >= 0 && vaga < se.length && se[vaga][0] >= 0 && se[vaga][0] < POSICAO.length) return POSICAO[se[vaga][0]];
        } catch (Throwable e) { }
        int p = Partida.papel(vaga);
        if (p == Partida.GOLEIRO) return "GOL";
        if (p == Partida.DEFESA) return vaga == 2 || vaga == 9 ? "LAT" : "ZAG";
        return p == Partida.MEIO ? "MEI" : "ATA";
    }
}
