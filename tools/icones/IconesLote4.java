import java.util.ArrayList;
import java.util.List;

// Quarto lote "Linha neon": Transferencias (quinto botao da janela Times).
// Uso: java IconesLote4 <raiz do jar original> <pasta de saida> [folha.png]
// Depois: python3 tools/tema/dourado.py <saida>/aicons/icon12.png (tema dourado).
public class IconesLote4 extends IconesLote3 {
    static List<Ic> lista4() {
        List<Ic> l = new ArrayList<Ic>();
        // jogador com setas de ida e volta
        Ic tr = new Ic("icon12", "Transferências", NEON).em("aicons").add(t(pessoa(7.5, 3.5, 0.9)));
        seta(tr, 15, 8, 22, 8);
        seta(tr, 22, 14.5, 15, 14.5);
        l.add(tr);
        return l;
    }

    public static void main(String[] a) throws Exception {
        gera(a, lista4(), "Janela Times");
    }
}
