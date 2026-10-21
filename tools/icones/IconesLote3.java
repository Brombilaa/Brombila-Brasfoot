import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

// Terceiro lote "Linha neon": botoes de baixo do painel (cores da tela, galeria de trofeus,
// retrospecto do time). Uso: java IconesLote3 <raiz do jar original> <pasta de saida> [folha.png]
public class IconesLote3 extends IconesLote2 {
    static List<Ic> lista3() {
        List<Ic> l = new ArrayList<Ic>();
        // paleta de pintor com as cores do tema
        Path2D pal = new Path2D.Double();
        pal.moveTo(12, 3);
        pal.curveTo(6.5, 3, 2.5, 7, 2.5, 12);
        pal.curveTo(2.5, 17, 6.5, 21, 11.5, 21);
        pal.curveTo(13.5, 21, 14, 19.5, 13.2, 18.2);
        pal.curveTo(12.3, 16.8, 13.2, 15, 15, 15);
        pal.lineTo(17.5, 15);
        pal.curveTo(19.8, 15, 21.5, 13.3, 21.5, 11);
        pal.curveTo(21.5, 6.5, 17.3, 3, 12, 3);
        pal.closePath();
        l.add(new Ic("iconcolorpicker", "Cores da tela", NEON).em("aicons").add(t(pal))
            .add(c(O(7.5, 11, 1.7)).cor(OURO)).add(c(O(10, 7, 1.7)).cor(VERM)).add(c(O(15, 6.8, 1.7)).cor(new Color(0x7FE3A8)))
            .add(c(O(18, 10.5, 1.7)).cor(new Color(0x6FC8FF))));
        // vitrine de trofeus: taca com estrela dourada
        Ic gal = new Ic("icon10", "Galeria de troféus", OURO).em("aicons");
        trofeu(gal, "1");
        gal.g.remove(gal.g.size() - 1);
        gal.add(cd(estrela(12, 8.3, 3.4)));
        l.add(gal);
        // retrospecto: grafico de linha com pontos de jogos
        l.add(new Ic("icon11", "Retrospecto do time", NEON).em("aicons").add(t(L(3, 3.5, 3, 20.5, 21, 20.5)))
            .add(td(L(6, 16, 10, 11, 13.5, 14, 19.5, 6))).add(cd(O(10, 11, 1.6))).add(cd(O(13.5, 14, 1.6))).add(cd(O(19.5, 6, 1.6))));
        return l;
    }

    public static void main(String[] a) throws Exception {
        gera(a, lista3(), "Botões do painel");
    }
}
