// Motor da partida do jogo (c.b) reescrito pelo mod: e o mesmo codigo do original
// (descompilado), com as ordens ao vivo (c.Ordens) entrando nos tres duelos de cada minuto:
// meio-campo (vR), ataque x defesa (vS) e a ligacao direta do passe longo (vO).
// As ordens so valem em jogos com time humano; nos outros o motor e identico ao original.
package c;

import best.A;
import best.F;
import best.I;
import best.ah;
import best.aq;
import c.a;
import forms.Randoms;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

public class b {
    private static final double[] SW = new double[]{44.55, 23.86, 7.4};
    private static final double[] SX = new double[]{30.69, 18.35, 12.99};
    private static final double[] SY = new double[]{7.8, 45.78, 53.52};
    private static final double[] SZ = new double[]{10.8, 43.78, 53.52};
    private static final double[] Ta = new double[]{13.2, 36.78, 44.52};
    private static final double[] Tb = new double[]{7.8, 37.78, 45.52};
    private static final double[] Tc = new double[]{5.8, 37.78, 45.52};
    private static final double[] Td = new double[]{2.8, 37.78, 45.52};
    private static final double[] Te = new double[]{1.8, 45.78, 53.52};
    private static final double[] Tf = new double[]{1.0, 55.78, 63.52};
    private static final double[] Tg = new double[]{55.0, 45.0};
    private static final double[] Th = new double[]{50.0, 50.0};
    private static final double Ti = 4.15;
    private static final double Tj = 3.81;
    private static final double Tk = 1.0;
    private static final double Tl = 10.0;
    private static final double Tm = 18.0;
    private Random Tn = new Random();
    private Random To = new Random();
    private Randoms Tp = new Randoms();
    private static final double Tq = 3.0;
    private static final double Tr = 5.0;
    private static final double Ts = 5.0;
    private static final double Tt = 3.0;
    private static final double Tu = 24.0;
    private static final double Tv = 20.0;
    private static final double Tw = 12.0;
    public static final double[][] Tx = new double[][]{{-1.0, -1.0, -1.0}, {-1.0, -1.0, -1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {8.0, 8.0, 8.0}, {4.0, 4.0, 4.0}, {4.0, 4.0, 4.0}, {4.0, 4.0, 4.0}, {8.0, 8.0, 8.0}, {8.0, 8.0, 8.0}, {8.0, 8.0, 8.0}, {8.0, 8.0, 8.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}, {22.0, 22.0, 22.0}};
    public static final double[][] Ty = new double[][]{{1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {10.0, 10.0, 10.0}, {2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}, {2.0, 2.0, 2.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {4.0, 4.0, 4.0}, {4.0, 4.0, 4.0}, {4.0, 4.0, 4.0}, {20.0, 20.0, 20.0}, {20.0, 20.0, 20.0}, {20.0, 20.0, 20.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}, {10.0, 10.0, 10.0}};
    public static final double[][] Tz = new double[][]{{-1.0, -1.0, -1.0}, {1.0, 1.0, 1.0}, {5.0, 5.0, 5.0}, {18.0, 18.0, 18.0}, {18.0, 18.0, 18.0}, {18.0, 18.0, 18.0}, {18.0, 18.0, 18.0}, {18.0, 18.0, 18.0}, {18.0, 18.0, 18.0}, {5.0, 5.0, 5.0}, {1.0, 1.0, 1.0}, {5.0, 5.0, 5.0}, {5.0, 5.0, 5.0}, {5.0, 5.0, 5.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}};
    private I zz = null;
    private ah[] TA = null;
    private int TB = 0;
    private int TC = 0;
    private boolean TD = false;
    F TE = null;
    private double TF = 0.0;
    private int[] zA = new int[2];
    private int[] TG = new int[2];
    private int[] TH = new int[2];
    private int[] TI = new int[2];
    private int[] TJ = new int[2];
    private int[] TK = new int[2];
    private int[] TL = new int[2];
    private int[] TM = new int[2];
    private int[] TN = new int[2];
    private int[] TO = new int[2];
    private int[] TP = new int[2];
    private int[] fX = new int[2];
    private int[] TQ = new int[2];
    private int[] TR = new int[2];
    private int[] TS = new int[3];
    private int[] TT = new int[26];
    private static final double[] TU = new double[]{5.5, 35.55, 15.0};
    private static final double[] TV = new double[]{4.5, 40.55, 15.0};
    private static final double[] TW = new double[]{3.0, 40.55, 15.0};
    private static final double[] TX = new double[]{0.5, 40.55, 15.0};
    int[] TY = new int[]{1, 22, 24, 11, 13, 14, 16, 2, 9, 3, 5};
    // ordens ao vivo (mod)
    private boolean comOrdens;
    private final Ordens.Plano[] plano = new Ordens.Plano[]{new Ordens.Plano(), new Ordens.Plano()};
    private final double[] cansaco = new double[2];
    private boolean direta;

    public int vN() {
        boolean bl2 = this.Tn.nextBoolean();
        if (bl2) {
            return 0;
        }
        return 1;
    }

    public b(I i2) {
        this.zz = i2;
        ah[] ahArray = new ah[]{i2.hc(), i2.hd()};
        this.TA = ahArray;
        this.TB = this.vN();
        this.TD = i2.hX();
        this.comOrdens = Ordens.ligado && (Ordens.todosOsJogos || this.TA[0].jZ().booleanValue() || this.TA[1].jZ().booleanValue());
        if (i2.hp().size() == 0) {
            System.out.println("erro escala: " + i2.hc().getNome());
            a.SR.bN = true;
            ah.a(i2.hc(), i2, 1, -1, true);
        }
        if (i2.hq().size() == 0) {
            System.out.println("erro escala: " + i2.hd().getNome());
            a.SR.bN = true;
            ah.a(i2.hd(), i2, 1, -1, true);
        }
        int n2 = 0;
        while (n2 < i2.hl().size()) {
            ((F)i2.hl().get(n2)).fe();
            ++n2;
        }
        n2 = 0;
        while (n2 < i2.hm().size()) {
            ((F)i2.hm().get(n2)).fe();
            ++n2;
        }
        n2 = 0;
        while (n2 < i2.hn().size()) {
            ((F)i2.hn().get(n2)).fe();
            ++n2;
        }
        n2 = 0;
        while (n2 < i2.ho().size()) {
            ((F)i2.ho().get(n2)).fe();
            ++n2;
        }
    }

    public b() {
    }

    public A vO() {
        A a2 = null;
        ++this.TC;
        if (this.comOrdens) {
            this.atualizaPlanos();
        }
        int n2 = this.vR();
        this.direta = false;
        if (n2 != this.TB && this.comOrdens && this.plano[this.TB].passe == Ordens.LONGO && this.To.nextDouble() < Ordens.LIGACAO_DIRETA) {
            // passe longo: perdeu o meio, mas a bola foi direto para o ataque
            this.direta = true;
            n2 = this.TB;
        }
        if (n2 == this.TB) {
            if (this.vS() == 0) {
                int[] nArray = this.zz.hA();
                int n3 = this.TB;
                nArray[n3] = nArray[n3] + 1;
                a2 = this.vT();
            } else if (this.To.nextInt(100) < 50) {
                int[] nArray = this.zz.hB();
                int n4 = this.vQ();
                nArray[n4] = nArray[n4] + 1;
            } else {
                int[] nArray = this.zz.hC();
                int n5 = this.TB;
                nArray[n5] = nArray[n5] + 1;
            }
        } else if (this.To.nextInt(100) < 50) {
            int[] nArray = this.zz.hB();
            int n6 = this.vQ();
            nArray[n6] = nArray[n6] + 1;
        } else {
            int[] nArray = this.zz.hC();
            int n7 = this.TB;
            nArray[n7] = nArray[n7] + 1;
        }
        this.vP();
        return a2;
    }

    // ordens do time humano (c.Ordens) e reacao simples do adversario ao placar
    private void atualizaPlanos() {
        int[] gols = new int[]{this.zz.hu(), this.zz.hw()};
        int n2 = 0;
        while (n2 < 2) {
            Ordens.Plano p0 = Ordens.de(this.TA[n2]);
            if (p0.humano && Ordens.escalarMudou(p0, this.TA[n2])) {
                // mudou a tatica no Escalar depois da ordem: o Escalar volta a mandar
                Ordens.define(this.TA[n2], null);
                p0 = Ordens.doEscalar(this.TA[n2]);
            }
            if (p0.humano) {
                this.plano[n2] = p0;
            } else if (this.TA[n2].jZ().booleanValue()) {
                this.plano[n2] = Ordens.doEscalar(this.TA[n2]);
            } else {
                Ordens.Plano p = Ordens.doEscalar(this.TA[n2]);
                int saldo = gols[n2] - gols[1 - n2];
                if (saldo < 0 && this.TC > 65) {
                    // perdendo: vai para o ataque; no fim (ou perdendo de 2) e tudo ou nada
                    p.mentalidade = saldo <= -2 && this.TC > 75 || this.TC > 82 ? Ordens.TUDO_OU_NADA : Math.max(p.mentalidade, Ordens.OFENSIVA);
                } else if (saldo > 0 && this.TC > 70) {
                    // ganhando: recua; ganhando de 1 nos minutos finais, retranca
                    p.mentalidade = saldo == 1 && this.TC > 82 ? Ordens.RETRANCA : Math.min(p.mentalidade, Ordens.DEFENSIVA);
                }
                this.plano[n2] = p;
            }
            this.cansaco[n2] = Math.max(0.0, Math.min(Ordens.CANSACO_MAXIMO, this.cansaco[n2] + Ordens.cansacoPorMinuto(this.plano[n2])));
            ++n2;
        }
    }

    public Ordens.Plano plano(int n2) {
        return this.plano[n2];
    }

    public double cansaco(int n2) {
        return this.cansaco[n2];
    }

    // onde o ataque do time n2 leva mais vantagem sobre a defesa adversaria (dica do auxiliar)
    public int melhorLado(int n2) {
        double meio = this.setor(n2, ATAQUE_MEIO) / Math.max(0.1, this.setor(1 - n2, DEFESA_MEIO));
        double lados = this.setor(n2, ATAQUE_LADOS) / Math.max(0.1, this.setor(1 - n2, DEFESA_LADOS));
        if (Math.abs(meio - lados) < 0.04) {
            return Ordens.MISTO;
        }
        return meio > lados ? Ordens.MEIO : Ordens.LATERAIS;
    }

    // forca media de um setor do campo (vagas do Escalar)
    private double setor(int n2, int[][] faixas) {
        ArrayList arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        double d2 = 0.0;
        int n3 = 0;
        int n4 = 0;
        while (n4 < arrayList.size()) {
            int n5 = ((F)arrayList.get(n4)).fT();
            int n6 = 0;
            while (n6 < faixas.length) {
                if (n5 >= faixas[n6][0] && n5 <= faixas[n6][1]) {
                    d2 += this.B((F)arrayList.get(n4));
                    ++n3;
                    break;
                }
                ++n6;
            }
            ++n4;
        }
        return n3 == 0 ? -1.0 : d2 / (double)n3;
    }

    private static final int[][] ATAQUE_MEIO = new int[][]{{11, 16}, {19, 24}};
    private static final int[][] ATAQUE_LADOS = new int[][]{{2, 2}, {9, 10}, {17, 18}, {25, 25}};
    private static final int[][] DEFESA_MEIO = new int[][]{{3, 8}, {11, 13}};
    private static final int[][] DEFESA_LADOS = new int[][]{{2, 2}, {9, 10}, {17, 17}};

    private void vP() {
        if (this.TB == 1) {
            this.TB = 0;
        } else if (this.TB == 0) {
            this.TB = 1;
        }
    }

    private int vQ() {
        if (this.TB == 1) {
            return 0;
        }
        return 1;
    }

    private double ez(int n2) {
        double[] dArray;
        ArrayList arrayList = null;
        arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        int n3 = this.TA[n2].kj()[2];
        if (n3 >= (dArray = new double[]{0.0, 0.04, 0.08}).length) {
            n3 = 2;
        }
        double d2 = 0.0 + dArray[n3];
        int n4 = 0;
        double d3 = 0.1;
        int n5 = 0;
        while (n5 < arrayList.size()) {
            if (n4 < 5 && ((F)arrayList.get(n5)).fT() >= 10 && ((F)arrayList.get(n5)).fT() <= 17) {
                d2 += this.B((F)arrayList.get(n5));
                ++n4;
            }
            ++n5;
        }
        d3 = d2 / 5.0;
        if (n4 < 3) {
            d3 = 0.01;
        }
        return d3;
    }

    private double eA(int n2) {
        ArrayList arrayList = null;
        arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        double d2 = 0.0;
        int n3 = 0;
        double d3 = 0.1;
        int n4 = 0;
        while (n4 < arrayList.size()) {
            if (n3 < 3 && ((F)arrayList.get(n4)).fT() >= 19 && ((F)arrayList.get(n4)).fT() <= 25) {
                d2 += this.B((F)arrayList.get(n4));
                ++n3;
            }
            ++n4;
        }
        d3 = d2 / 3.0;
        if (n3 < 1) {
            d3 = 0.0;
        }
        return d3;
    }

    private double eB(int n2) {
        ArrayList arrayList = null;
        arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        F f2 = null;
        double d2 = 0.1;
        int n3 = 0;
        while (n3 < arrayList.size()) {
            if (((F)arrayList.get(n3)).fT() == 1) {
                f2 = (F)arrayList.get(n3);
                d2 = this.B((F)arrayList.get(n3));
                break;
            }
            ++n3;
        }
        if (f2 != null && f2.gF()) {
            d2 = (int)Math.round(d2 * 0.2);
        }
        return d2;
    }

    private double eC(int n2) {
        double d2 = 0.1;
        this.TE = this.vU();
        if (this.TE != null) {
            d2 = this.B(this.TE);
        }
        return d2;
    }

    private int eD(int n2) {
        ArrayList arrayList = null;
        int n3 = 0;
        arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        int n4 = 0;
        while (n4 < arrayList.size()) {
            if (((F)arrayList.get(n4)).fT() >= 3 && ((F)arrayList.get(n4)).fT() <= 8) {
                ++n3;
            }
            ++n4;
        }
        return n3;
    }

    private double eE(int n2) {
        ArrayList arrayList = null;
        arrayList = n2 == 0 ? this.zz.hp() : this.zz.hq();
        double d2 = 0.0;
        int n3 = 0;
        double d3 = 0.1;
        int n4 = 0;
        while (n4 < arrayList.size()) {
            if (n3 < 5 && ((F)arrayList.get(n4)).fT() >= 2 && ((F)arrayList.get(n4)).fT() <= 9) {
                d2 += this.B((F)arrayList.get(n4));
                ++n3;
            }
            ++n4;
        }
        d3 = d2 / 5.0;
        if (n3 < 3) {
            d3 = 0.01;
        }
        return d3;
    }

    public double B(F f2) {
        int n2 = f2.fi();
        if (a.SR.isHabilidadeIndividual()) {
            n2 = 0;
            if (f2.fT() == 1) {
                n2 += (int)Math.round((double)f2.gK() * 0.6);
                n2 += (int)Math.round((double)f2.gL() * 0.15);
                n2 += (int)Math.round((double)f2.gJ() * 0.15);
                n2 += (int)Math.round((double)f2.gM() * 0.1);
            } else if (f2.fT() >= 3 && f2.fT() <= 8) {
                n2 += (int)Math.round((double)f2.gN() * 0.5);
                n2 += (int)Math.round((double)f2.gL() * 0.1);
                n2 += (int)Math.round((double)f2.gJ() * 0.25);
                n2 += (int)Math.round((double)f2.gM() * 0.1);
                n2 += (int)Math.round((double)f2.gO() * 0.05);
            } else if (f2.fT() == 2 || f2.fT() == 9) {
                n2 += (int)Math.round((double)f2.gN() * 0.4);
                n2 += (int)Math.round((double)f2.gJ() * 0.1);
                n2 += (int)Math.round((double)f2.gL() * 0.1);
                n2 += (int)Math.round((double)f2.gM() * 0.3);
                n2 += (int)Math.round((double)f2.gO() * 0.05);
                n2 += (int)Math.round((double)f2.gP() * 0.05);
            } else if (f2.fT() >= 11 && f2.fT() <= 13) {
                n2 += (int)Math.round((double)f2.gN() * 0.4);
                n2 += (int)Math.round((double)f2.gJ() * 0.15);
                n2 += (int)Math.round((double)f2.gL() * 0.1);
                n2 += (int)Math.round((double)f2.gM() * 0.2);
                n2 += (int)Math.round((double)f2.gO() * 0.1);
                n2 += (int)Math.round((double)f2.gP() * 0.05);
            } else if (f2.fT() >= 14 && f2.fT() <= 16) {
                n2 += (int)Math.round((double)f2.gN() * 0.05);
                n2 += (int)Math.round((double)f2.gJ() * 0.1);
                n2 += (int)Math.round((double)f2.gL() * 0.1);
                n2 += (int)Math.round((double)f2.gM() * 0.25);
                n2 += (int)Math.round((double)f2.gO() * 0.4);
                n2 += (int)Math.round((double)f2.gP() * 0.1);
            } else if (f2.fT() == 10 || f2.fT() == 17) {
                n2 += (int)Math.round((double)f2.gN() * 0.05);
                n2 += (int)Math.round((double)f2.gJ() * 0.25);
                n2 += (int)Math.round((double)f2.gL() * 0.15);
                n2 += (int)Math.round((double)f2.gM() * 0.25);
                n2 += (int)Math.round((double)f2.gO() * 0.2);
                n2 += (int)Math.round((double)f2.gP() * 0.1);
            } else if (f2.fT() >= 19 && f2.fT() <= 24) {
                n2 += (int)Math.round((double)f2.gJ() * 0.25);
                n2 += (int)Math.round((double)f2.gL() * 0.25);
                n2 += (int)Math.round((double)f2.gM() * 0.05);
                n2 += (int)Math.round((double)f2.gO() * 0.05);
                n2 += (int)Math.round((double)f2.gP() * 0.4);
            } else if (f2.fT() == 18 || f2.fT() == 25) {
                n2 += (int)Math.round((double)f2.gJ() * 0.25);
                n2 += (int)Math.round((double)f2.gL() * 0.15);
                n2 += (int)Math.round((double)f2.gM() * 0.15);
                n2 += (int)Math.round((double)f2.gO() * 0.05);
                n2 += (int)Math.round((double)f2.gP() * 0.4);
            }
        }
        if (f2.gF()) {
            n2 = (int)Math.round((double)n2 * 0.5);
        }
        if (n2 <= 0) {
            n2 = 1;
        }
        if (this.zz != null && this.zz.hy().b() == 7) {
            if (this.zz.hc() != null && f2.getPais() == this.zz.hc().getPais()) {
                if (this.zz.hc().getReputacao() < 3) {
                    n2 = (int)Math.round((double)n2 * 0.65);
                } else if (this.zz.hc().getReputacao() == 3) {
                    n2 = (int)Math.round((double)n2 * 0.85);
                } else if (this.zz.hc().getReputacao() == 4) {
                    n2 = (int)Math.round((double)n2 * 0.95);
                }
            } else if (this.zz.hd() != null && f2.getPais() == this.zz.hd().getPais()) {
                if (this.zz.hd().getReputacao() < 3) {
                    n2 = (int)Math.round((double)n2 * 0.65);
                } else if (this.zz.hd().getReputacao() == 3) {
                    n2 = (int)Math.round((double)n2 * 0.85);
                } else if (this.zz.hd().getReputacao() == 4) {
                    n2 = (int)Math.round((double)n2 * 0.95);
                }
            }
        }
        if (this.zz != null && this.zz.hy().b() == 4) {
            if (f2.fg() != null && f2.fg().getReputacao() < 3) {
                n2 = (int)Math.round((double)n2 * 0.75);
            } else if (f2.fg() != null && f2.fg().getReputacao() == 3) {
                n2 = (int)Math.round((double)n2 * 0.85);
            } else if (f2.fg() != null && f2.fg().getPais() == 29) {
                n2 = (int)Math.round((double)n2 * 0.9);
            }
        }
        if (this.zz != null && this.zz.hy().b() == 5) {
            if (f2.fg() != null && f2.fg().getReputacao() < 3) {
                n2 = (int)Math.round((double)n2 * 0.55);
            } else if (f2.fg() != null && f2.fg().getReputacao() == 3) {
                n2 = (int)Math.round((double)n2 * 0.75);
            } else if (f2.fg() != null && f2.fg().gg() != 0) {
                n2 = (int)Math.round((double)n2 * 0.9);
            }
        }
        if (this.zz != null && this.zz.hy().b() == 1) {
            if (f2.fg() != null && f2.fg().getReputacao() < 3) {
                n2 = (int)Math.round((double)n2 * 0.85);
            } else if (f2.fg() != null && f2.fg().getReputacao() == 3) {
                n2 = (int)Math.round((double)n2 * 0.95);
            }
        }
        if (this.zz != null && (this.zz.hy().b() == 3 || this.zz.hy().b() == 2) && this.zz.hc().getReputacao() < 3 && this.zz.hd().getReputacao() >= 3 && f2.fg() != null && f2.fg().equals(this.zz.hd())) {
            n2 = (int)Math.round((double)n2 * 0.8);
        }
        return (double)n2 / 10.0;
    }

    private void eF(int n2) {
        int[] nArray = this.zz.hY();
        int n3 = n2;
        nArray[n3] = nArray[n3] + 1;
        int n4 = aq.A(this.zz.hY()[0], this.zz.hY()[0] + this.zz.hY()[1]);
        int n5 = aq.A(this.zz.hY()[1], this.zz.hY()[0] + this.zz.hY()[1]);
        this.zz.hz()[0] = n4;
        this.zz.hz()[1] = n5;
    }

    public int vR() {
        double[] dArray = new double[]{0.1, 0.1};
        dArray[this.TB] = this.ez(this.TB);
        dArray[this.vQ()] = this.ez(this.vQ());
        double d2 = 1.0 + this.a(dArray[this.TB], dArray[this.vQ()]);
        double d3 = 1.0 + this.a(dArray[this.vQ()], dArray[this.TB]);
        if (!this.TD && this.TB == 0) {
            d2 += 0.3;
        }
        int cfr_ignored_0 = this.TA[this.TB].kj()[1];
        if (this.comOrdens) {
            d2 *= Ordens.meio(this.plano[this.TB], this.cansaco[this.TB]);
            d3 *= Ordens.meio(this.plano[this.vQ()], this.cansaco[this.vQ()]);
        }
        if (d2 < 0.2) {
            d2 = 0.2;
        }
        if (d3 < 0.2) {
            d3 = 0.2;
        }
        double[] dArray2 = new double[]{d2, d3};
        int n2 = -1;
        n2 = this.b(Tg, dArray2);
        int n3 = 0;
        if (n2 == 0) {
            int n4 = this.vQ();
            this.TQ[n4] = this.TQ[n4] + 1;
            int n5 = this.TB;
            this.fX[n5] = this.fX[n5] + 1;
            n3 = this.TB;
            int n6 = this.TB;
            this.TN[n6] = this.TN[n6] + 1;
            this.eF(this.TB);
        } else if (n2 == 1) {
            int n7 = this.TB;
            this.TQ[n7] = this.TQ[n7] + 1;
            int n8 = this.vQ();
            this.fX[n8] = this.fX[n8] + 1;
            n3 = this.vQ();
            int n9 = this.vQ();
            this.TO[n9] = this.TO[n9] + 1;
            this.eF(this.vQ());
        }
        return n3;
    }

    public int vS() {
        double d2;
        this.TF = d2 = this.eE(this.vQ());
        double d3 = this.eA(this.TB);
        int lado = this.comOrdens ? this.plano[this.TB].ataque : Ordens.MISTO;
        if (lado == Ordens.MELHOR_LADO) {
            lado = this.melhorLado(this.TB);
        }
        if (lado != Ordens.MISTO && d3 > 0.0 && d2 > 0.0) {
            // atacar pelo meio ou pelas laterais: pesa quem joga naquele setor dos dois lados
            boolean bl2 = lado == Ordens.LATERAIS;
            double d6 = this.setor(this.TB, bl2 ? ATAQUE_LADOS : ATAQUE_MEIO);
            double d7 = this.setor(this.vQ(), bl2 ? DEFESA_LADOS : DEFESA_MEIO);
            d3 = (1.0 - Ordens.LADO_PESO) * d3 + Ordens.LADO_PESO * (d6 < 0.0 ? d3 * 0.7 : d6);
            d2 = (1.0 - Ordens.LADO_PESO) * d2 + Ordens.LADO_PESO * (d7 < 0.0 ? d2 * 0.7 : d7);
        }
        double d4 = 1.0 + this.a(d3, d2);
        double d5 = 1.0 + this.a(d2, d3);
        if (this.comOrdens) {
            d4 *= Ordens.ataque(this.plano[this.TB], this.cansaco[this.TB]) * (this.direta ? Ordens.LIGACAO_FORCA : 1.0);
            d5 *= Ordens.defesa(this.plano[this.vQ()], this.cansaco[this.vQ()]);
        }
        if (d2 == 0.0) {
            d5 = 0.1;
        }
        if (!this.TD && this.TB == 0) {
            d4 += 0.3;
        }
        if (d3 == 0.0) {
            d4 = 0.1;
        }
        if (d4 < 0.2) {
            d4 = 0.2;
        }
        if (d5 < 0.2) {
            d5 = 0.2;
        }
        int n2 = 0;
        if (this.TA[0].jZ().booleanValue() || this.TA[1].jZ().booleanValue()) {
            n2 = this.eD(this.vQ());
            if (n2 == 0) {
                d5 = 0.1;
            } else if (n2 == 1) {
                d5 = 0.05;
            }
        }
        double[] dArray = new double[]{d4, d5};
        int n3 = -1;
        n3 = this.b(Th, dArray);
        if (n3 == 0) {
            int n4 = this.TB;
            this.TM[n4] = this.TM[n4] + 1;
        } else if (n3 == 1) {
            int n5 = this.vQ();
            this.TP[n5] = this.TP[n5] + 1;
        }
        return n3;
    }

    public double a(double d2, double d3) {
        double d4 = 0.1;
        int n2 = 8;
        if (a.SR.H() >= 5) {
            n2 = 11;
        } else if (a.SR.H() >= 9) {
            n2 = 12;
        }
        d4 = d2 - d3;
        return d4 /= (double)n2;
    }

    public double b(double d2, double d3) {
        double d4 = 0.1;
        int n2 = 8;
        if (a.SR.H() >= 5) {
            n2 = 10;
        }
        d4 = d2 - d3;
        return d4 /= (double)n2;
    }

    public double c(double d2, double d3) {
        double d4 = 0.1;
        d4 = d2 - d3;
        return d4 /= 12.0;
    }

    public A vT() {
        double d2;
        A a2 = null;
        this.TF = d2 = this.eE(this.vQ());
        double d3 = this.eA(this.TB);
        double d4 = this.eC(this.TB);
        double d5 = this.eB(this.vQ());
        double d6 = 1.0 + this.b(d5, d4);
        double d7 = 1.0 + this.b(d2, d3);
        int n2 = 0;
        if (this.TA[0].jZ().booleanValue() || this.TA[1].jZ().booleanValue()) {
            n2 = this.eD(this.vQ());
            if (n2 == 0) {
                d6 = (int)Math.round(d6 * 0.2);
            } else if (n2 == 1) {
                d6 = (int)Math.round(d6 * 0.4);
            }
        }
        if (!this.TD) {
            if (this.TB == 0) {
                d7 = (d6 += 0.1) + 0.1;
            }
            if (this.TB == 1) {
                d7 = (d6 -= 0.1) - 0.1;
            }
        }
        double[] dArray = TU;
        if (this.zA[this.TB] >= 6) {
            dArray = TX;
        } else if (this.zA[this.TB] >= 5) {
            dArray = TW;
        } else if (this.zA[this.TB] >= 3) {
            dArray = TV;
        }
        if (this.zA[this.TB] >= 2 && this.TA[this.vQ()].getReputacao() - this.TA[this.TB].getReputacao() >= 2) {
            dArray = TW;
        }
        if (d6 < 0.2) {
            d6 = 0.2;
        }
        if (d7 < 0.2) {
            d7 = 0.2;
        }
        double[] dArray2 = new double[]{1.0, d6, d7};
        int n3 = -1;
        n3 = this.a(dArray, dArray2);
        int n4 = this.TB;
        this.TG[n4] = this.TG[n4] + 1;
        if (n3 == 0) {
            a2 = new A(this.TB);
            a2.k(this.TA[this.TB]);
            this.a(a2, this.TE);
            int[] nArray = this.zz.hZ();
            int n5 = this.TB;
            nArray[n5] = nArray[n5] + 1;
        } else if (n3 == 1) {
            int[] nArray = this.zz.hZ();
            int n6 = this.TB;
            nArray[n6] = nArray[n6] + 1;
            if (this.TE != null) {
                this.TE.gB().tL();
            }
        } else if (n3 == 2) {
            int[] nArray = this.zz.ia();
            int n7 = this.TB;
            nArray[n7] = nArray[n7] + 1;
        }
        return a2;
    }

    private int a(double[] dArray, double[] dArray2) {
        double d2 = 0.0;
        double[] dArray3 = new double[dArray.length];
        int n2 = 0;
        while (n2 < dArray.length) {
            dArray3[n2] = dArray[n2] * dArray2[n2];
            d2 += dArray3[n2];
            ++n2;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n3 = 0;
        while (n3 < dArray3.length) {
            if (d3 < (d4 += dArray3[n3])) {
                return n3;
            }
            ++n3;
        }
        return dArray.length;
    }

    private int b(double[] dArray, double[] dArray2) {
        double d2 = 0.0;
        double[] dArray3 = new double[dArray.length];
        int n2 = 0;
        while (n2 < dArray.length) {
            dArray3[n2] = dArray[n2] * dArray2[n2];
            d2 += dArray3[n2];
            ++n2;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n3 = 0;
        while (n3 < dArray3.length) {
            if (d3 < (d4 += dArray3[n3])) {
                return n3;
            }
            ++n3;
        }
        return dArray.length;
    }

    public F vU() {
        F f2 = null;
        ArrayList arrayList = null;
        arrayList = this.TB == 0 ? this.zz.hp() : this.zz.hq();
        double d2 = 0.0;
        int n2 = 0;
        while (n2 < arrayList.size()) {
            if (((F)arrayList.get(n2)).fT() != 1 && ((F)arrayList.get(n2)).getPosicao() != 0 && ((F)arrayList.get(n2)).fT() > 0 && ((F)arrayList.get(n2)).fT() < Tx.length) {
                d2 += Tx[((F)arrayList.get(n2)).fT()][0];
                if (((F)arrayList.get(n2)).getCr1() == 9 || ((F)arrayList.get(n2)).getCr2() == 9) {
                    d2 += 4.0;
                } else if (((F)arrayList.get(n2)).getCr1() == 5 || ((F)arrayList.get(n2)).getCr2() == 5) {
                    d2 += 2.0;
                    if (((F)arrayList.get(n2)).getPosicao() == 2) {
                        d2 += 2.0;
                    }
                }
            }
            ++n2;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n3 = 0;
        while (n3 < arrayList.size()) {
            if (((F)arrayList.get(n3)).fT() != 1 && ((F)arrayList.get(n3)).getPosicao() != 0 && ((F)arrayList.get(n3)).fT() > 0 && ((F)arrayList.get(n3)).fT() < Tx.length) {
                d4 += Tx[((F)arrayList.get(n3)).fT()][0];
                if (((F)arrayList.get(n3)).getCr1() == 9 || ((F)arrayList.get(n3)).getCr2() == 9) {
                    d4 += 4.0;
                } else if (((F)arrayList.get(n3)).getCr1() == 5 || ((F)arrayList.get(n3)).getCr2() == 5) {
                    d4 += 2.0;
                    if (((F)arrayList.get(n3)).getPosicao() == 2) {
                        d4 += 2.0;
                    }
                }
            }
            if (d3 <= d4) {
                f2 = (F)arrayList.get(n3);
                return (F)arrayList.get(n3);
            }
            ++n3;
        }
        if (f2 == null) {
            n3 = arrayList.size() - 1;
            while (n3 > 0) {
                if (n3 < arrayList.size() && arrayList.get(n3) != null) {
                    f2 = (F)arrayList.get(n3);
                    break;
                }
                --n3;
            }
        }
        return f2;
    }

    public F C(F f2) {
        if (new Random().nextInt(100) > 80) {
            return null;
        }
        F f3 = null;
        ArrayList arrayList = null;
        int n2 = 0;
        if (this.TB == 0) {
            arrayList = this.zz.hp();
            n2 = this.zz.hc().kj()[2];
        } else {
            arrayList = this.zz.hq();
            n2 = this.zz.hd().kj()[2];
        }
        double d2 = 0.0;
        int n3 = 0;
        while (n3 < arrayList.size()) {
            if (arrayList.get(n3) != f2 && ((F)arrayList.get(n3)).fT() > 0 && ((F)arrayList.get(n3)).fT() < Ty.length) {
                d2 += Ty[((F)arrayList.get(n3)).fT()][0];
                if (((F)arrayList.get(n3)).getCr1() == 11 || ((F)arrayList.get(n3)).getCr2() == 11) {
                    d2 += 10.0;
                    if (((F)arrayList.get(n3)).getCr1() == 4 || ((F)arrayList.get(n3)).getCr2() == 4) {
                        d2 += 5.0;
                    }
                } else if (((F)arrayList.get(n3)).getCr1() == 4 || ((F)arrayList.get(n3)).getCr2() == 4) {
                    d2 += 2.0;
                    if (((F)arrayList.get(n3)).getCr1() == 8) {
                        d2 += 2.0;
                    }
                } else if (((F)arrayList.get(n3)).getCr1() == 8 || ((F)arrayList.get(n3)).getCr2() == 8) {
                    d2 += 2.0;
                    if (((F)arrayList.get(n3)).getCr1() == 13) {
                        d2 += 2.0;
                    }
                } else if (((F)arrayList.get(n3)).getCr1() == 13 || ((F)arrayList.get(n3)).getCr2() == 13) {
                    d2 += 1.0;
                    if (((F)arrayList.get(n3)).getPosicao() == 1) {
                        d2 += 2.0;
                    }
                } else if (((F)arrayList.get(n3)).getCr1() == 6 || ((F)arrayList.get(n3)).getCr2() == 6) {
                    d2 += 5.0;
                    if (((F)arrayList.get(n3)).getPosicao() == 1) {
                        d2 += 2.0;
                    }
                }
                if (n2 == 1 && ((F)arrayList.get(n3)).getPosicao() == 1) {
                    d2 += 20.0;
                }
            }
            ++n3;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n4 = 0;
        while (n4 < arrayList.size()) {
            if (arrayList.get(n4) != f2 && ((F)arrayList.get(n4)).fT() > 0 && ((F)arrayList.get(n4)).fT() < Ty.length) {
                d4 += Ty[((F)arrayList.get(n4)).fT()][0];
                if (((F)arrayList.get(n4)).getCr1() == 11 || ((F)arrayList.get(n4)).getCr2() == 11) {
                    d4 += 10.0;
                    if (((F)arrayList.get(n4)).getCr1() == 4 || ((F)arrayList.get(n4)).getCr2() == 4) {
                        d4 += 5.0;
                    }
                } else if (((F)arrayList.get(n4)).getCr1() == 4 || ((F)arrayList.get(n4)).getCr2() == 4) {
                    d4 += 2.0;
                    if (((F)arrayList.get(n4)).getCr1() == 8) {
                        d4 += 2.0;
                    }
                } else if (((F)arrayList.get(n4)).getCr1() == 8 || ((F)arrayList.get(n4)).getCr2() == 8) {
                    d4 += 2.0;
                    if (((F)arrayList.get(n4)).getCr1() == 13) {
                        d4 += 2.0;
                    }
                } else if (((F)arrayList.get(n4)).getCr1() == 13 || ((F)arrayList.get(n4)).getCr2() == 13) {
                    d4 += 2.0;
                    if (((F)arrayList.get(n4)).getPosicao() == 1) {
                        d4 += 2.0;
                    }
                } else if (((F)arrayList.get(n4)).getCr1() == 6 || ((F)arrayList.get(n4)).getCr2() == 6) {
                    d4 += 5.0;
                    if (((F)arrayList.get(n4)).getPosicao() == 1) {
                        d4 += 2.0;
                    }
                }
                if (n2 == 1 && ((F)arrayList.get(n4)).getPosicao() == 1) {
                    d4 += 20.0;
                }
            }
            if (d3 <= d4) {
                f3 = (F)arrayList.get(n4);
                return (F)arrayList.get(n4);
            }
            ++n4;
        }
        return f3;
    }

    public F vV() {
        F f2 = null;
        ArrayList arrayList = null;
        arrayList = this.TB == 0 ? this.zz.hq() : this.zz.hp();
        double d2 = 0.0;
        int n2 = 0;
        while (n2 < arrayList.size()) {
            if (((F)arrayList.get(n2)).fT() >= 0 && ((F)arrayList.get(n2)).fT() < Tz.length) {
                d2 += Tz[((F)arrayList.get(n2)).fT()][0];
            }
            ++n2;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n3 = 0;
        while (n3 < arrayList.size()) {
            if (((F)arrayList.get(n3)).fT() >= 0 && ((F)arrayList.get(n3)).fT() < Tz.length) {
                d4 += Tz[((F)arrayList.get(n3)).fT()][0];
            }
            if (d3 <= d4) {
                return (F)arrayList.get(n3);
            }
            ++n3;
        }
        return f2;
    }

    public int vW() {
        double d2 = 0.0;
        int n2 = 0;
        while (n2 < this.TY.length) {
            if (this.TY[n2] != 1) {
                d2 += Tx[this.TY[n2]][0];
            }
            ++n2;
        }
        double d3 = this.Tn.nextDouble() * d2;
        double d4 = 0.0;
        int n3 = 0;
        while (n3 < this.TY.length) {
            if (this.TY[n3] != 1) {
                d4 += Tx[this.TY[n3]][0];
            }
            if (d3 <= d4) {
                return this.TY[n3];
            }
            ++n3;
        }
        return -1;
    }

    public void a(A a2, F f2) {
        Serializable serializable;
        a2.a(1);
        a2.f(f2);
        int n2 = 1;
        int n3 = new Random().nextInt(1000);
        n2 = n3 < 900 ? 1 : (n3 < 950 ? 3 : (n3 < 980 ? 4 : (n3 < 990 ? 2 : (n3 < 995 ? 5 : 1))));
        if (f2 == null) {
            serializable = null;
            serializable = this.TB == 0 ? this.zz.hp() : this.zz.hq();
            if (((ArrayList)serializable).size() == 0) {
                ah.a(this.TA[this.TB], this.zz, this.TB + 1, -1, true);
            }
            f2 = this.vU();
        }
        if (f2 != null && n2 != 3 && n2 != 2) {
            f2.gB().z();
        }
        if (n2 != 3 && n2 != 2 && n2 != 5 && n2 != 4 && (serializable = this.C(f2)) != null && serializable != f2) {
            ((F)serializable).gB().gV();
            if (((F)serializable).fg() != null && !((F)serializable).fC().booleanValue()) {
                ((F)serializable).a(8, a2.cu(), this.zz.hy());
            }
            a2.g((F)serializable);
        }
        if (f2 != null && n2 == 5) {
            serializable = null;
            serializable = this.TB == 0 ? this.zz.hp() : this.zz.hq();
            if (this.TA[this.TB].lq() != null && ((ArrayList)serializable).contains(this.TA[this.TB].lq())) {
                a2.f(this.TA[this.TB].lq());
            } else if (f2.getPosicao() == 0) {
                n2 = 1;
            }
        }
        if (n2 == 2) {
            serializable = this.vV();
            if (serializable != null) {
                a2.f((F)serializable);
                ((F)serializable).gB().tE();
            } else {
                n2 = 1;
                if (f2 != null) {
                    f2.gB().z();
                }
            }
        }
        if (n2 == 3 || n2 == 4) {
            serializable = null;
            serializable = this.TB == 0 ? this.zz.hp() : this.zz.hq();
            if (this.TA[this.TB].ke() != null && ((ArrayList)serializable).contains(this.TA[this.TB].ke())) {
                a2.f(this.TA[this.TB].ke());
            }
        }
        boolean bl2 = false;
        if (n2 == 3) {
            a2.q(true);
            if (this.TA[0].jZ().booleanValue() || this.TA[1].jZ().booleanValue()) {
                bl2 = true;
                n2 = 3;
                a2.q(false);
            }
        }
        if (!bl2) {
            int n4 = this.TB;
            this.zA[n4] = this.zA[n4] + 1;
            if (this.TB == 0) {
                this.zz.hv();
            } else {
                this.zz.hx();
            }
            if (f2 != null) {
                f2.gB().z();
            }
        }
        a2.R(n2);
    }

    public void eG(int n2) {
        int n3 = n2;
        this.zA[n3] = this.zA[n3] + 1;
    }

    public int[] vX() {
        return this.zA;
    }

    public int[] vY() {
        return this.TG;
    }

    public int[] vZ() {
        return this.TH;
    }

    public int[] wa() {
        return this.TI;
    }

    public int[] wb() {
        return this.TJ;
    }

    public int[] wc() {
        return this.TK;
    }

    public int[] wd() {
        return this.TL;
    }

    public int[] we() {
        return this.TM;
    }

    public int[] wf() {
        return this.TN;
    }

    public int[] wg() {
        return this.TO;
    }

    public int[] wh() {
        return this.TP;
    }

    public int[] hY() {
        return this.fX;
    }

    public int[] wi() {
        return this.TR;
    }
}

