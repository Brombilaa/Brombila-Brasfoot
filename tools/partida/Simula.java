// Calibragem das ordens ao vivo (c.Ordens): roda milhares de partidas com o motor do
// jogo usando os confrontos e as escalacoes reais da rodada carregada e compara cada
// ordem com o jogo original. Roda dentro da JVM do jogo (driver de testes):
//   call partida.Simula#roda 3        -> 3 repeticoes de cada jogo da rodada, por cenario
// O resultado vai para o retorno e para simulacao.txt na pasta atual.
package partida;

import best.F;
import best.I;
import best.ah;
import c.Ordens;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Simula {

    static final String[] LISTAS = {"fF", "fG", "fH", "fI", "fJ", "fK", "fL", "fM"};

    static Object campo(Object o, String nome) throws Exception {
        Field f = I.class.getDeclaredField(nome);
        f.setAccessible(true);
        return f.get(o);
    }

    static void poe(Object o, String nome, Object v) throws Exception {
        Field f = I.class.getDeclaredField(nome);
        f.setAccessible(true);
        f.set(o, v);
    }

    // uma partida inteira, como a tela da rodada faz (1o tempo ate 48, 2o de 0 a 50)
    @SuppressWarnings("unchecked")
    static int[] partida(I modelo) throws Exception {
        return partida(modelo, null, 0, null, null);
    }

    // aos 65 minutos (20 do 2o tempo) o time de teste pode trocar de plano conforme o placar:
    // perdendo por 1 -> perdendo, ganhando por 1 -> ganhando. Devolve {gols casa, gols fora, saldo aos 65}
    static int[] partida(I modelo, ah teste, int lado, Ordens.Plano perdendo, Ordens.Plano ganhando) throws Exception {
        I j = new I(modelo.hc(), modelo.hd(), false);
        poe(j, "fx", campo(modelo, "fx"));
        poe(j, "dH", campo(modelo, "dH"));
        for (String l : LISTAS) poe(j, l, new ArrayList<Object>((List<Object>) campo(modelo, l)));
        // guarda vaga e energia de todo mundo: substituicoes e cansaco mexem nos jogadores
        Map<F, int[]> guarda = new IdentityHashMap<F, int[]>();
        for (String l : LISTAS) for (Object o : (List<Object>) campo(modelo, l)) {
            F f = (F) o;
            guarda.put(f, new int[]{f.fT(), f.fp()});
        }
        try {
            for (int m = 1; m <= 48; m++) j.q(m, 1);
            j.m(2, 0);
            int saldo65 = 0;
            for (int m = 0; m <= 50; m++) {
                if (m == 20) {
                    saldo65 = lado == 0 ? j.hu() - j.hw() : j.hw() - j.hu();
                    if (teste != null && saldo65 == -1 && perdendo != null) Ordens.define(teste, perdendo);
                    if (teste != null && saldo65 == 1 && ganhando != null) Ordens.define(teste, ganhando);
                }
                j.q(m, 2);
            }
            return new int[]{j.hu(), j.hw(), saldo65};
        } finally {
            for (Map.Entry<F, int[]> e : guarda.entrySet()) {
                e.getKey().as(e.getValue()[0]);
                e.getKey().ai(e.getValue()[1]);
            }
        }
    }

    static Ordens.Plano plano(String nome) {
        Ordens.Plano p = new Ordens.Plano();
        p.humano = true;
        if (nome.equals("retranca")) p.mentalidade = Ordens.RETRANCA;
        else if (nome.equals("defensiva")) p.mentalidade = Ordens.DEFENSIVA;
        else if (nome.equals("ofensiva")) p.mentalidade = Ordens.OFENSIVA;
        else if (nome.equals("tudo ou nada")) p.mentalidade = Ordens.TUDO_OU_NADA;
        else if (nome.equals("pressao alta")) p.pressao = Ordens.ALTA;
        else if (nome.equals("pressao baixa")) p.pressao = Ordens.BAIXA;
        else if (nome.equals("passe curto")) p.passe = Ordens.CURTO;
        else if (nome.equals("passe longo")) p.passe = Ordens.LONGO;
        else if (nome.equals("pelo meio")) p.ataque = Ordens.MEIO;
        else if (nome.equals("pelas laterais")) p.ataque = Ordens.LATERAIS;
        else if (nome.equals("melhor lado")) p.ataque = Ordens.MELHOR_LADO;
        return p;
    }

    // o que cada ordem faz quando o placar esta 1 gol para um lado aos 65 minutos.
    // O adversario se comporta como a maquina (reage ao placar).
    static String situacoes(List<I> jogos, int reps) throws Exception {
        String[] ordens = {"normal", "retranca", "defensiva", "ofensiva", "tudo ou nada", "pressao alta", "pressao baixa", "passe curto", "passe longo"};
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%nTroca de ordem aos 65' (adversario reage ao placar)%n"));
        sb.append(String.format("%-15s | %-26s | %-26s%n", "", "perdendo por 1", "ganhando por 1"));
        sb.append(String.format("%-15s | %6s %6s %6s %5s | %6s %6s %6s %5s%n", "ordem", "pts/j", "empata", "vira%", "n", "pts/j", "segura", "perde%", "n"));
        boolean ligadoAntes = Ordens.ligado, todosAntes = Ordens.todosOsJogos;
        try {
            for (String o : ordens) {
                long nP = 0, ptsP = 0, empP = 0, virP = 0, nG = 0, ptsG = 0, segG = 0, derG = 0;
                for (I jg : jogos) {
                    for (int lado = 0; lado < 2; lado++) {
                        ah teste = lado == 0 ? jg.hc() : jg.hd();
                        for (int r = 0; r < reps; r++) {
                            Ordens.limpa();
                            Ordens.ligado = true;
                            Ordens.todosOsJogos = true;
                            Ordens.define(teste, plano("normal"));
                            int[] g;
                            try { g = partida(jg, teste, lado, plano(o), plano(o)); } catch (Throwable t) { continue; }
                            int s = g[lado] - g[1 - lado];
                            int p = s > 0 ? 3 : s == 0 ? 1 : 0;
                            if (g[2] == -1) { nP++; ptsP += p; if (s == 0) empP++; if (s > 0) virP++; }
                            if (g[2] == 1) { nG++; ptsG += p; if (s > 0) segG++; if (s < 0) derG++; }
                        }
                    }
                }
                double kp = Math.max(1, nP), kg = Math.max(1, nG);
                sb.append(String.format("%-15s | %6.2f %5.1f%% %5.1f%% %5d | %6.2f %5.1f%% %5.1f%% %5d%n", o,
                    ptsP / kp, 100 * empP / kp, 100 * virP / kp, nP, ptsG / kg, 100 * segG / kg, 100 * derG / kg, nG));
            }
        } finally {
            Ordens.limpa();
            Ordens.ligado = ligadoAntes;
            Ordens.todosOsJogos = todosAntes;
        }
        return sb.toString();
    }

    // roda na thread da interface: o jogo fica parado enquanto simula
    public static String roda(final String arg) throws Exception {
        final String[] r = new String[1];
        final Exception[] erro = new Exception[1];
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try { r[0] = simula(arg); } catch (Exception e) { erro[0] = e; }
            }
        });
        if (erro[0] != null) throw erro[0];
        return r[0];
    }

    static String simula(String arg) throws Exception {
        // "100 ATAQUE_POR_NIVEL=0.3 BRECHA_POR_NIVEL=0.2 ..." muda os pesos antes de simular
        String[] partes = arg.trim().split("\\s+");
        int reps = Integer.parseInt(partes[0]);
        boolean soSituacoes = false;
        for (int i = 1; i < partes.length; i++) {
            if (partes[i].equals("so65")) { soSituacoes = true; continue; }
            String[] kv = partes[i].split("=");
            Ordens.class.getField(kv[0]).setDouble(null, Double.parseDouble(kv[1]));
        }
        List<?> rodada = c.a.SR.S();
        List<I> jogos = new ArrayList<I>();
        for (Object o : rodada) {
            I i = (I) o;
            if (i.hc() != null && i.hd() != null && i.hp().size() >= 11 && i.hq().size() >= 11) jogos.add(i);
        }
        String[] cenarios = {"original", "normal", "retranca", "defensiva", "ofensiva", "tudo ou nada", "pressao alta",
            "pressao baixa", "passe curto", "passe longo", "pelo meio", "pelas laterais", "melhor lado"};
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d jogos x %d repeticoes x 2 lados por cenario%n", jogos.size(), reps));
        for (Field f : Ordens.class.getFields()) if (f.getType() == double.class) sb.append(f.getName()).append('=').append(f.getDouble(null)).append(' ');
        sb.append('\n');
        sb.append(String.format("%-15s %7s %7s %7s %7s %7s %7s %7s%n", "cenario", "golsPro", "golsCon", "pts/j", "vit%", "emp%", "der%", "gols/j"));
        boolean ligadoAntes = Ordens.ligado, todosAntes = Ordens.todosOsJogos;
        int erros = 0;
        try {
            for (String c : soSituacoes ? new String[0] : cenarios) {
                long pro = 0, con = 0, pts = 0, v = 0, e = 0, d = 0, n = 0;
                for (I jg : jogos) {
                    for (int lado = 0; lado < 2; lado++) {
                        ah teste = lado == 0 ? jg.hc() : jg.hd(), outro = lado == 0 ? jg.hd() : jg.hc();
                        Ordens.limpa();
                        Ordens.ligado = !c.equals("original");
                        Ordens.todosOsJogos = true;
                        Ordens.define(teste, plano(c));
                        Ordens.define(outro, plano("normal"));
                        for (int r = 0; r < reps; r++) {
                            int[] g;
                            try { g = partida(jg); } catch (Throwable t) { erros++; continue; }
                            int a = g[lado], b = g[1 - lado];
                            pro += a; con += b; n++;
                            if (a > b) { v++; pts += 3; } else if (a == b) { e++; pts += 1; } else d++;
                        }
                    }
                }
                double k = Math.max(1, n);
                sb.append(String.format("%-15s %7.2f %7.2f %7.2f %7.1f %7.1f %7.1f %7.2f%n", c, pro / k, con / k, pts / k,
                    100 * v / k, 100 * e / k, 100 * d / k, (pro + con) / k));
            }
        } finally {
            Ordens.limpa();
            Ordens.ligado = ligadoAntes;
            Ordens.todosOsJogos = todosAntes;
        }
        sb.append("erros: ").append(erros).append('\n');
        sb.append(situacoes(jogos, reps));
        PrintWriter w = new PrintWriter(new FileWriter("simulacao.txt"));
        w.print(sb);
        w.close();
        return sb.toString();
    }
}
