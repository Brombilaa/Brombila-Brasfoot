package mod;

// Temporada de inicio: o jogo so oferecia 2022 ou 2023; o mod troca por 2026 ou 2027.
// a.hU (Configuracoes iniciais) e best.p (comeco do jogo novo) chamam estes metodos no lugar dos
// numeros 2022 e 2023 (tools/ano/remenda_ano.py). 2027 segue as regras do 2023 do jogo: a Copa
// do Mundo vem na 4a temporada (2030). O combo da tela mostra os anos novos (ConfigEstreia).
public final class Ano {
    private Ano() {}

    public static final int PRIMEIRA = 2026, SEGUNDA = 2027;

    public static int primeira() { return PRIMEIRA; }

    public static int segunda() { return SEGUNDA; }
}
