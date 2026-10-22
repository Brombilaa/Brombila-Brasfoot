// Disputa de penaltis (a.bg) reescrita pelo mod: a regra e a mesma do original (70% de
// acerto, cinco cobrancas alternadas e depois uma a uma), mas a tela e a cena 3D de
// mod.Penaltis, com o tempo de cada cobranca ditado pela animacao.
package a;

import best.F;
import best.I;
import best.ah;
import components.X;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import mod.Penaltis;

public class bg
extends JPanel {
    private JDialog ub;
    private I zz;
    private int[] zA = new int[2];
    private int[] zB = new int[3];
    private Timer zC;
    private ArrayList zD = new ArrayList();
    private ArrayList zE = new ArrayList();
    private int zF = 0;
    private int zG = 0;
    private int zH = 1;
    private int zI = -1;
    ArrayList zJ = new ArrayList();
    private Penaltis cena;
    private boolean acabou;

    public bg(JDialog jDialog, I i2) {
        this.ub = jDialog;
        this.zz = i2;
        this.zD.addAll(dV.qk());
        this.zE.addAll(dV.ql());
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(980, 600));
        this.cena = new Penaltis(true, i2.hc(), i2.hd());
        this.add(this.cena, BorderLayout.CENTER);
        this.getInputMap(1).put(KeyStroke.getKeyStroke(115, 512), "st");
        this.getActionMap().put("st", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { }
        });
        // o jogo abre a janela no tamanho da tela antiga: aumenta para a cena
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Window w = SwingUtilities.getWindowAncestor(bg.this);
                if (w != null) { w.setSize(980, 600); w.setLocationRelativeTo(null); }
            }
        });
        this.nJ();
    }

    public void mH() {
    }

    // proxima cobranca
    public void nJ() {
        if (this.acabou) return;
        ArrayList arrayList;
        if (this.zH == 1) {
            arrayList = this.zD;
            if (this.zF >= arrayList.size()) {
                this.zF = 0;
            }
        } else {
            arrayList = this.zE;
            if (this.zG >= arrayList.size()) {
                this.zG = 0;
            }
        }
        F batedor = arrayList.isEmpty() ? null : (F) arrayList.get(this.zH == 1 ? this.zF : this.zG);
        X x2 = new X();
        x2.a(batedor);
        x2.dO(this.zH);
        this.zJ.add(x2);
        F goleiro = null;
        try {
            goleiro = this.zz.aT(this.zH == 1 ? 2 : 1);
        } catch (Exception exception) {
            // sem goleiro: a cena usa um generico
        }
        final int lado = this.zH - 1;
        // time humano escolhe o canto (batendo) ou o pulo (defendendo)
        boolean chute = humano(this.zH == 1 ? this.zz.hc() : this.zz.hd());
        boolean defesa = humano(this.zH == 1 ? this.zz.hd() : this.zz.hc());
        this.cena.cobra(lado, batedor, goleiro, Penaltis.numero(batedor, arrayList), chute, defesa, new Penaltis.Decide() {
            public int resultado(Penaltis.Escolha e) { return bg.this.nM(e); }
        }, new Runnable() {
            public void run() { bg.this.nK(); }
        });
    }

    // fim da animacao de uma cobranca
    public void nK() {
        if (this.acabou) {
            this.nO();
        } else {
            this.nJ();
        }
    }

    public void nL() {
        this.nJ();
    }

    private static boolean humano(ah time) {
        try {
            return time != null && time.jZ();
        } catch (Exception exception) {
            return false;
        }
    }

    // sorteio do original (ou, com time humano, o resultado das escolhas, com a mesma media
    // de 70%); devolve o que a cena mostra
    private int nM(Penaltis.Escolha e) {
        int n2 = 0;
        int n3 = 70;
        int escolha = -1;
        if (e != null && e.interativo) {
            escolha = Penaltis.resolve(n3, e, new Random());
            if (escolha == Penaltis.GOL) n2 = 1;
        } else {
            int n5 = new Random().nextInt(100) + 1;
            if (n5 <= n3) {
                n2 = 1;
            }
        }
        X x2 = (X)this.zJ.get(this.zJ.size() - 1);
        x2.dN(n2);
        if (this.zH == 1) {
            ++this.zF;
            if (n2 == 1) {
                this.zA[0] = this.zA[0] + 1;
            }
            this.zB[1] = this.zB[1] + 1;
        } else if (this.zH == 2) {
            ++this.zG;
            if (n2 == 1) {
                this.zA[1] = this.zA[1] + 1;
            }
            this.zB[2] = this.zB[2] + 1;
        }
        if (this.cP(n2)) {
            this.acabou = true;
        } else {
            if (this.zH == 1) {
                this.zH = 2;
            } else if (this.zH == 2) {
                this.zH = 1;
            }
        }
        if (escolha >= 0) return escolha;
        if (n2 == 1) return Penaltis.GOL;
        int s = new Random().nextInt(100);
        return s < 55 ? Penaltis.DEFESA : s < 75 ? Penaltis.TRAVE : Penaltis.FORA;
    }

    private void nN() {
    }

    private void nO() {
        if (this.zI == 1) {
            this.zz.u(this.zz.hc());
        } else if (this.zI == 2) {
            this.zz.u(this.zz.hd());
        }
        this.zz.i(this.zA);
        ah v = this.zz.hR();
        String nome = v != null ? v.getNome() + " venceu" : "";
        Color cor = v != null ? v.kB() : Color.WHITE;
        this.cena.fimDisputa(nome, cor, new Runnable() {
            public void run() { bg.this.ub.dispose(); }
        });
    }

    private boolean cP(int n2) {
        if (this.zH == 1 && this.zB[1] <= 5) {
            if (n2 == 1 && this.zA[0] > 5 - this.zB[2] + this.zA[1]) {
                this.zI = 1;
                return true;
            }
            if (5 - this.zB[1] + this.zA[0] < this.zA[1]) {
                this.zI = 2;
                return true;
            }
        } else if (this.zH == 2 && this.zB[2] <= 5) {
            if (n2 == 1 && 5 - this.zB[1] + this.zA[0] < this.zA[1]) {
                this.zI = 2;
                return true;
            }
            if (this.zA[0] > 5 - this.zB[2] + this.zA[1]) {
                this.zI = 1;
                return true;
            }
        } else if (this.zH == 2 && this.zB[2] > 5) {
            if (this.zA[0] > this.zA[1]) {
                this.zI = 1;
                return true;
            }
            if (this.zA[0] < this.zA[1]) {
                this.zI = 2;
                return true;
            }
        }
        return false;
    }

    static /* synthetic */ JDialog a(bg bg2) {
        return bg2.ub;
    }

    static /* synthetic */ Timer b(bg bg2) {
        if (bg2.zC == null) bg2.zC = new Timer();
        return bg2.zC;
    }

    static /* synthetic */ void c(bg bg2) {
        bg2.nM(null);
    }
}
