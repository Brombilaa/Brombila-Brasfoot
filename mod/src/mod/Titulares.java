package mod;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.Timer;

// Botao "Escalar titulares" no Escalar time (a.bz). Usa a escalacao automatica do proprio
// jogo (o seletor "Automatica", que chama bz.cU com a formacao escolhida), so que sem
// precisar escolher: se o campo ja tem 11 numa formacao conhecida, refaz essa formacao
// com os melhores jogadores; senao escolhe a formacao em que o elenco rende mais
// (soma das forcas, descontando quem joga fora de posicao).
final class Titulares {
    private Titulares() {}

    static final String TEXTO = "Escalar titulares";

    static void instala(final JPanel bz) {
        final JButton b = new JButton();
        Editor.botao(b, TEXTO, "camisa", true);
        b.setFont(Estreia.negrito(12f));
        b.setMargin(new java.awt.Insets(2, 6, 2, 6));
        b.setToolTipText("Monta o time titular com cada jogador na sua posição");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { escala(bz, b); }
        });
        Dialogos.adiciona(bz, b, 12, 626, 152, 28);  // estreito: nao cobre o rotulo do goleiro
    }

    private static void escala(JPanel bz, final JButton b) {
        try {
            int k = formacaoAtual(bz);
            if (k < 1) k = melhorFormacao(bz);
            if (k < 1) return;
            Object o = Reflexao.campo(bz, "Bf");
            if (!(o instanceof JComboBox)) return;
            JComboBox combo = (JComboBox) o;
            if (k + 1 >= combo.getItemCount()) return;
            combo.setSelectedIndex(k + 1);   // o jogo escala ao escolher (a.bR -> cU(k))
            bz.repaint();
            b.setText("Feito: " + combo.getItemAt(k + 1));
            Timer volta = new Timer(2200, new ActionListener() {
                public void actionPerformed(ActionEvent e) { b.setText(TEXTO); }
            });
            volta.setRepeats(false);
            volta.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // Formacao (indice de aq.sJ) que bate com as 11 posicoes ocupadas no campo, ou -1.
    static int formacaoAtual(JPanel bz) {
        Object o = Reflexao.campo(bz, "AE");
        if (!(o instanceof ArrayList)) return -1;
        ArrayList<?> slots = (ArrayList<?>) o;
        Set<Integer> ocupadas = new HashSet<Integer>();
        for (int i = 0; i <= 25 && i < slots.size(); i++) {
            Object s = slots.get(i);
            if (s instanceof components.ao && ((components.ao) s).x() != null) ocupadas.add(i);
        }
        if (ocupadas.size() != 11) return -1;
        int[][] f = best.aq.sJ;
        for (int k = 1; k < f.length; k++) {
            Set<Integer> s = new HashSet<Integer>();
            for (int j = 0; j < 11 && j < f[k].length; j++) s.add(f[k][j]);
            if (s.equals(ocupadas)) return k;
        }
        return -1;
    }

    // Repete a escolha do jogo (mesma ordem e mesmo criterio) para cada formacao e fica com
    // a de maior soma de forca; cada jogador improvisado custa 8 pontos.
    @SuppressWarnings("unchecked")
    static int melhorFormacao(JPanel bz) {
        Object o = Reflexao.campo(bz, "AF");
        if (!(o instanceof ArrayList)) return -1;
        int[][] f = best.aq.sJ;
        int melhor = -1;
        double maior = -1e9;
        for (int k = 1; k < f.length; k++) {
            ArrayList<Object> lista = new ArrayList<Object>((ArrayList<Object>) o);
            Collections.sort(lista, (java.util.Comparator<Object>) h.a.abh);
            double soma = 0;
            for (int j = 0; j < 11 && j < f[k].length; j++) {
                int slot = f[k][j];
                best.F p = best.ah.a(lista, slot, false, false);
                if (p == null) {
                    soma -= 100;
                    continue;
                }
                soma += p.fi();
                if (improvisado(p, slot)) soma -= 8;
                lista.remove(p);
            }
            // empate: fica a primeira formacao da lista do jogo
            if (soma > maior + 0.001) {
                maior = soma;
                melhor = k;
            }
        }
        return melhor;
    }

    // Mesma regra do rotulo do campo (components.ao): laterais (10 e 17) aceitam zagueiro.
    static boolean improvisado(best.F p, int slot) {
        if (slot < 1 || slot > 25) return false;
        if (best.aq.sE[slot][0] == p.getPosicao()) return false;
        return !((slot == 10 || slot == 17) && (p.getPosicao() == 1 || p.getPosicao() == 3));
    }
}
