// Penalti durante a partida (a.gb) reescrito pelo mod: o sorteio e as estatisticas sao os
// do original (batedor de penalti/artilheiro, goleiro pegador de penalti etc.), mas a
// cobranca aparece na cena 3D de mod.Penaltis em vez do texto em tres etapas.
package a;

import best.A;
import best.F;
import components.v;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mod.Penaltis;

public class gb
extends JPanel {
    private JDialog ub = null;
    private v Db = null;
    private A JO = null;
    private F ef = null;
    private F JP = null;
    private Timer zC;
    private int JQ = 0;
    private int JR = 0;
    private boolean JS = false;
    private dV JT = null;
    private Penaltis cena;

    public gb(JDialog jDialog, v v2, A a2, F f2, dV dV2) {
        this.ub = jDialog;
        this.JT = dV2;
        this.Db = v2;
        this.JO = a2;
        this.ef = f2;
        if (v2.tR().hc() == a2.cu()) {
            this.JQ = 1;
            this.JR = 2;
            this.JS = v2.tR().hc().jZ();
        } else if (v2.tR().hd() == a2.cu()) {
            this.JQ = 2;
            this.JR = 1;
            this.JS = v2.tR().hd().jZ();
        }
        this.JP = v2.tR().aT(this.JR);
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(900, 540));
        this.cena = new Penaltis(false, v2.tR().hc(), v2.tR().hd());
        this.cena.titulo(a2.cu().getNome());
        this.add(this.cena, BorderLayout.CENTER);
        this.getInputMap(1).put(KeyStroke.getKeyStroke(115, 512), "st");
        this.getActionMap().put("st", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { }
        });
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Window w = SwingUtilities.getWindowAncestor(gb.this);
                if (w != null) { w.setSize(900, 540); w.setLocationRelativeTo(null); }
            }
        });
        List<?> escalados = null;
        try {
            escalados = this.JQ == 1 ? v2.tR().hp() : v2.tR().hq();
        } catch (Exception exception) {
            // sem lista: numero 0 (camisa sem numero)
        }
        // time humano escolhe o canto (batendo) ou o pulo (defendendo)
        boolean defesa = false;
        try {
            defesa = (this.JR == 1 ? v2.tR().hc() : v2.tR().hd()).jZ();
        } catch (Exception exception) {
            // sem time: computador
        }
        this.cena.cobra(this.JQ - 1, f2, this.JP, Penaltis.numero(f2, escalados), this.JS, defesa, new Penaltis.Decide() {
            public int resultado(Penaltis.Escolha e) { return gb.this.nO(e); }
        }, new Runnable() {
            public void run() { gb.this.rK(); }
        });
    }

    public void py() {
    }

    private void rJ() {
    }

    // sorteio e efeitos do original (com time humano, o gol sai das escolhas, com a mesma
    // chance media); devolve o que a cena mostra
    private int nO(Penaltis.Escolha e) {
        int n2 = 70;
        int n3 = 30;
        if (this.ef.getCr1() == 9 || this.ef.getCr2() == 9 || this.ef.gm().booleanValue()) {
            n2 += 10;
            n3 -= 10;
        }
        if (this.ef.ff().booleanValue()) {
            n2 += 5;
            n3 -= 5;
        }
        if (this.JP != null) {
            if (this.JP.getCr1() == 1 || this.JP.getCr2() == 1) {
                n2 -= 10;
                n3 += 10;
            }
            if (this.JP.gm().booleanValue()) {
                n2 -= 10;
                n3 += 10;
            }
            if (this.JP.ff().booleanValue()) {
                n2 -= 5;
                n3 += 5;
            }
        }
        boolean bl2 = false;
        int escolha = -1;
        if (e != null && e.interativo) {
            escolha = Penaltis.resolve(n2, e, new Random());
            bl2 = escolha == Penaltis.GOL;
        } else {
            int n4 = new Random().nextInt(100) + 1;
            if (n4 <= n2) {
                bl2 = true;
            }
        }
        if (bl2) {
            this.Db.dJ(this.JQ);
            this.Db.a(this.JO);
            try {
                this.Db.tR().hW().eG(this.JQ - 1);
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (this.JQ == 1) {
                this.Db.tR().hv();
            } else {
                this.Db.tR().hx();
            }
            int[] nArray = this.Db.tR().hA();
            int n6 = this.JQ - 1;
            nArray[n6] = nArray[n6] + 1;
            int[] nArray2 = this.Db.tR().hZ();
            int n7 = this.JQ - 1;
            nArray2[n7] = nArray2[n7] + 1;
            this.ef.gB().z();
            if (this.JS && this.JT != null) {
                this.JT.dk(2);
            } else if (this.JT != null) {
                this.JT.dk(3);
            }
            return Penaltis.GOL;
        }
        int[] nArray = this.Db.tR().hA();
        int n8 = this.JQ - 1;
        nArray[n8] = nArray[n8] + 1;
        this.ef.gB().tF();
        int n9 = new Random().nextInt(7);
        if (n9 == 0 && this.JP == null) {
            n9 = 3;
        }
        if (escolha == Penaltis.DEFESA) n9 = 3;
        else if (escolha == Penaltis.TRAVE) n9 = 1;
        else if (escolha == Penaltis.FORA) n9 = 2;
        if (n9 == 2 || n9 == 6) {
            int[] nArray3 = this.Db.tR().ia();
            int n10 = this.JQ - 1;
            nArray3[n10] = nArray3[n10] + 1;
        } else {
            int[] nArray4 = this.Db.tR().hZ();
            int n11 = this.JQ - 1;
            nArray4[n11] = nArray4[n11] + 1;
        }
        this.JO.a(7);
        if ((n9 == 0 || n9 == 3 || n9 == 5) && this.JP != null) {
            this.JO.g(this.JP);
            this.JP.gB().tJ();
        }
        this.Db.a(this.JO);
        if (this.JS) {
            if (this.JT != null) this.JT.dk(3);
        } else if (this.JT != null) {
            this.JT.dk(2);
        }
        // 0/3 defesa, 1/5 trave, 2/4/6 para fora (4 = escorregou)
        if (n9 == 0 || n9 == 3) return Penaltis.DEFESA;
        if (n9 == 1 || n9 == 5) return Penaltis.TRAVE;
        return Penaltis.FORA;
    }

    private void rK() {
        this.ub.dispose();
    }

    static /* synthetic */ Timer a(gb gb2) {
        if (gb2.zC == null) gb2.zC = new Timer();
        return gb2.zC;
    }

    static /* synthetic */ void b(gb gb2) {
        gb2.rJ();
    }

    static /* synthetic */ void c(gb gb2) {
        gb2.nO(null);
    }

    static /* synthetic */ void d(gb gb2) {
        gb2.rK();
    }
}
