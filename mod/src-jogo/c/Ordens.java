// Classe nova do mod (nao existe no jogo original): ordens dadas durante a partida.
// O motor (c.b, reescrito pelo mod) consulta estes pesos so nos jogos que tem um time
// humano; nos outros jogos da rodada tudo continua exatamente como no Brasfoot original.
// Nada daqui vai para o save: as ordens valem so para a partida em andamento.
package c;

import best.ah;
import java.util.IdentityHashMap;
import java.util.Map;

public class Ordens {
    // mentalidade
    public static final int RETRANCA = -2, DEFENSIVA = -1, NORMAL = 0, OFENSIVA = 1, TUDO_OU_NADA = 2;
    // atacar por
    public static final int MEIO = 0, MISTO = 1, LATERAIS = 2, MELHOR_LADO = 3;
    // pressao e passe: 0 baixa/curto, 1 media/misto, 2 alta/longo
    public static final int BAIXA = 0, MEDIA = 1, ALTA = 2;
    public static final int CURTO = 0, LONGO = 2;

    // pesos (calibrados por simulacao, ver tools/partida/Simula.java)
    public static double ATAQUE_POR_NIVEL = 0.35;      // chance de passar pela defesa, por nivel de mentalidade
    public static double BRECHA_POR_NIVEL = 0.25;      // quanto a propria defesa abre, por nivel ofensivo
    public static double FECHA_POR_NIVEL = 0.36;       // quanto a propria defesa fecha, por nivel defensivo
    public static double RECUO_POR_NIVEL = 0.20;       // quanto o proprio ataque perde, por nivel defensivo
    public static double MEIO_POR_NIVEL = 0.05;         // gente a mais (ou a menos) no meio, por nivel de mentalidade
    public static double PRESSAO_MEIO = 0.08;          // disputa do meio-campo, por nivel de pressao
    public static double PRESSAO_DEFESA = 0.10;        // marcar mais atras fecha espaco; pressao alta deixa as costas
    public static double PASSE_CURTO_ATAQUE = 0.03;    // toque curto chega menos rapido na area
    public static double PASSE_CURTO_MEIO = 0.04;      // toque curto segura a bola no meio
    public static double PASSE_LONGO_ATAQUE = 0.04;    // bola longa chega mais rapido la na frente
    public static double PASSE_LONGO_MEIO = 0.07;      // bola longa abre mao do meio-campo
    public static double LIGACAO_DIRETA = 0.08;        // chance de atacar mesmo perdendo o meio (passe longo)
    public static double LIGACAO_FORCA = 0.8;          // forca desse ataque direto
    public static double LADO_PESO = 0.8;              // quanto o setor escolhido pesa no duelo ataque x defesa
    public static double CANSACO_PRESSAO = 0.0005;     // perda de forca por minuto com pressao alta
    public static double CANSACO_OFENSIVO = 0.0003;    // por minuto, por nivel ofensivo
    public static double CANSACO_MAXIMO = 0.12;

    public static final String[] MENTALIDADES = {"Retranca", "Defensiva", "Normal", "Ofensiva", "Tudo ou nada"};
    public static final String[] ATAQUES = {"Pelo meio", "Misto", "Pelas laterais"};
    public static final String[] PRESSOES = {"Baixa", "Média", "Alta"};
    public static final String[] PASSES = {"Curto", "Misto", "Longo"};

    public static final class Plano {
        public int mentalidade = NORMAL, ataque = MISTO, pressao = MEDIA, passe = 1;
        public boolean humano;
        // taticas do Escalar quando a ordem foi dada: se o jogador mudar la, o Escalar volta a valer
        public int estilo = -1, jogadas = -1;
        public Plano copia() {
            Plano p = new Plano();
            p.mentalidade = mentalidade; p.ataque = ataque; p.pressao = pressao; p.passe = passe; p.humano = humano;
            p.estilo = estilo; p.jogadas = jogadas;
            return p;
        }
        public boolean igual(Plano o) {
            return o != null && mentalidade == o.mentalidade && ataque == o.ataque && pressao == o.pressao && passe == o.passe;
        }
        public boolean neutro() { return mentalidade == NORMAL && ataque == MISTO && pressao == MEDIA && passe == 1; }
    }

    private static final Map<ah, Plano> planos = new IdentityHashMap<ah, Plano>();
    public static boolean ligado = true;
    public static boolean todosOsJogos = false;   // so para a simulacao de calibragem

    public static synchronized Plano de(ah time) {
        Plano p = planos.get(time);
        return p == null ? new Plano() : p;
    }

    public static synchronized void define(ah time, Plano p) {
        if (time == null) return;
        if (p == null) planos.remove(time); else planos.put(time, p.copia());
    }

    public static synchronized void limpa() { planos.clear(); }

    // ponto de partida: as taticas do Escalar que o motor original ignorava
    // Estilo (kj()[1]): 0 Equilibrado, 1 Ataque total, 2 Contra-ataque
    // Jogadas (kj()[3]): 0 Pelo meio, 1 Pelas laterais
    public static Plano doEscalar(ah time) {
        Plano p = new Plano();
        try {
            int[] t = time.kj();
            if (t != null && t.length > 1) {
                if (t[1] == 1) p.mentalidade = OFENSIVA;
                else if (t[1] == 2) { p.mentalidade = DEFENSIVA; p.passe = LONGO; }
            }
            if (t != null && t.length > 3) p.ataque = t[3] == 1 ? LATERAIS : MEIO;
        } catch (Throwable e) {
            // time sem taticas: fica no normal
        }
        return p;
    }

    // guarda as taticas do Escalar no momento da ordem
    public static void marcaEscalar(Plano p, ah time) {
        try {
            int[] t = time.kj();
            p.estilo = t != null && t.length > 1 ? t[1] : -1;
            p.jogadas = t != null && t.length > 3 ? t[3] : -1;
        } catch (Throwable e) {
            p.estilo = p.jogadas = -1;
        }
    }

    // o jogador mudou Estilo ou Jogadas no Escalar/substituicoes depois de dar a ordem?
    public static boolean escalarMudou(Plano p, ah time) {
        if (p.estilo < 0 && p.jogadas < 0) return false;
        try {
            int[] t = time.kj();
            int e = t != null && t.length > 1 ? t[1] : -1, j = t != null && t.length > 3 ? t[3] : -1;
            return e != p.estilo || j != p.jogadas;
        } catch (Throwable e) {
            return false;
        }
    }

    // ---- pesos usados pelo motor (publicos para a tela mostrar "o que muda")

    public static double meio(Plano p, double cansaco) {
        double m = (1 + PRESSAO_MEIO * (p.pressao - MEDIA)) * (1 + MEIO_POR_NIVEL * p.mentalidade);
        if (p.passe == CURTO) m *= 1 + PASSE_CURTO_MEIO;
        else if (p.passe == LONGO) m *= 1 - PASSE_LONGO_MEIO;
        return m * (1 - cansaco);
    }

    public static double ataque(Plano p, double cansaco) {
        double m = p.mentalidade >= 0 ? 1 + ATAQUE_POR_NIVEL * p.mentalidade : 1 + RECUO_POR_NIVEL * p.mentalidade;
        if (p.passe == LONGO) m *= 1 + PASSE_LONGO_ATAQUE;
        else if (p.passe == CURTO) m *= 1 - PASSE_CURTO_ATAQUE;
        return m * (1 - cansaco);
    }

    public static double defesa(Plano p, double cansaco) {
        double m = p.mentalidade > 0 ? 1 - BRECHA_POR_NIVEL * p.mentalidade : 1 + FECHA_POR_NIVEL * -p.mentalidade;
        m *= 1 + PRESSAO_DEFESA * (MEDIA - p.pressao);
        return m * (1 - cansaco);
    }

    public static double cansacoPorMinuto(Plano p) {
        double c = 0;
        if (p.pressao == ALTA) c += CANSACO_PRESSAO;
        else if (p.pressao == BAIXA) c -= CANSACO_PRESSAO * 0.5;
        if (p.mentalidade > 0) c += CANSACO_OFENSIVO * p.mentalidade;
        return c;
    }
}
