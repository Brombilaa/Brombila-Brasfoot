// Classe do jogo (components.ao) reescrita pelo mod: mesma logica e mesmas posicoes do
// original, mas o desenho das camisas e dos rotulos dos jogadores passa por mod.Escalacao.
package components;

import best.F;
import best.ah;
import best.aq;
import c.a;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;

public class ao {
    private F U = null;
    private ImageIcon RC = null;
    private ah cg = null;
    private ImageIcon AH = new ImageIcon(this.getClass().getResource("/aicons/camisav.png"));
    private ImageIcon AJ = new ImageIcon(this.getClass().getResource("/aicons/camisat.png"));
    private ImageIcon AI = new ImageIcon(this.getClass().getResource("/aicons/camisag.png"));
    private static int RD = 435;
    private static int RE = 210;
    private static int RF = 580;
    private static int RG = 410;
    private static ArrayList RH = new ArrayList();
    private static ArrayList RI = new ArrayList();
    private static int[][] RJ = new int[][]{{-1, -1}, {190, 565}, {35, 425}, {100, 485}, {190, 485}, {285, 485}, {100, 405}, {190, 405}, {285, 405}, {345, 425}, {35, 265}, {100, 315}, {190, 315}, {285, 315}, {100, 205}, {190, 205}, {285, 205}, {345, 265}, {35, 55}, {100, 115}, {190, 115}, {285, 115}, {100, 35}, {190, 30}, {285, 35}, {345, 55}, {470, 30}, {558, 30}, {655, 30}, {755, 30}, {855, 30}, {950, 30}, {470, 105}, {558, 105}, {655, 105}, {755, 105}, {855, 105}};
    private static int[][] RK = new int[][]{{-1, -1}, {190, 565}, {35, 425}, {100, 485}, {190, 485}, {285, 485}, {100, 405}, {190, 405}, {285, 405}, {345, 425}, {35, 270}, {100, 315}, {190, 315}, {285, 315}, {100, 205}, {190, 205}, {285, 205}, {345, 270}, {35, 55}, {100, 115}, {190, 115}, {285, 115}, {100, 35}, {190, 30}, {285, 35}, {345, 55}, {20, 25}, {115, 35}, {20, 125}, {115, 135}, {20, 210}, {115, 220}, {20, 305}, {115, 315}, {20, 400}, {115, 410}, {20, 495}};
    private static int[][] RL = new int[][]{{-1, -1}, {200, 489}, {76, 376}, {135, 422}, {202, 421}, {273, 421}, {131, 356}, {202, 356}, {277, 356}, {328, 377}, {76, 243}, {131, 276}, {199, 276}, {275, 276}, {128, 188}, {201, 188}, {276, 188}, {323, 247}, {68, 62}, {129, 107}, {203, 107}, {276, 107}, {127, 37}, {200, 37}, {280, 37}, {331, 64}, {10, 480}, {10, 400}, {10, 320}, {10, 240}, {10, 160}, {10, 84}, {10, 1}};
    private static int[][] RM;

    public F x() {
        return this.U;
    }

    public void a(F f2) {
        this.U = f2;
    }

    public ImageIcon vf() {
        return this.RC;
    }

    public void h(ImageIcon imageIcon) {
        this.RC = imageIcon;
    }

    public static int S(int n2, int n3) {
        int n4 = 1;
        while (n4 <= 36) {
            if (n2 >= RJ[n4][0] && n2 <= RJ[n4][0] + 60 && n3 >= RJ[n4][1] && n3 <= RJ[n4][1] + 60) {
                return n4;
            }
            ++n4;
        }
        return -1;
    }

    public static int T(int n2, int n3) {
        int n4 = 1;
        while (n4 <= 36) {
            int n5 = RK[n4][0];
            int n6 = RK[n4][1];
            if (n4 <= 25) {
                n5 += 200;
                n6 += 10;
            }
            if (n2 >= n5 && n2 <= n5 + 50 && n3 >= n6 && n3 <= n6 + 50) {
                return n4;
            }
            ++n4;
        }
        return -1;
    }

    public static int U(int n2, int n3) {
        int n4 = 37;
        int n5 = 0;
        while (n5 < RH.size()) {
            if (n2 >= (Integer)RH.get(n5) - 20 && n2 <= (Integer)RH.get(n5) + 70 && n3 >= (Integer)RI.get(n5) && n3 <= (Integer)RI.get(n5) + 80) {
                return n5 + n4;
            }
            ++n5;
        }
        return -1;
    }

    public static boolean V(int n2, int n3) {
        if (n2 >= 0 && n2 <= 434 && n3 >= 0 && n3 <= 640) {
            return true;
        }
        return n2 >= 440 && n2 <= 1020 && n3 >= 20 && n3 <= 180;
    }

    public static boolean W(int n2, int n3) {
        if (n2 >= 200 && n2 <= 640 && n3 >= 0 && n3 <= 650) {
            return true;
        }
        return n2 >= 10 && n2 <= 200 && n3 >= 20 && n3 <= 650;
    }

    public static boolean X(int n2, int n3) {
        return n2 >= RD && n2 <= RD + RF && n3 >= 10 && n3 <= RE + RG;
    }

    public ImageIcon a(ArrayList arrayList, int n2, ImageIcon imageIcon, boolean bl2, int n3, ah ah2, int n4) {
        Image image = this.AH.getImage();
        Image image2 = imageIcon.getImage();
        ImageIcon imageIcon2 = new ImageIcon(this.getClass().getResource("/aeicons/ect.png"));
        Image image3 = imageIcon2.getImage();
        ImageIcon imageIcon3 = new ImageIcon(this.getClass().getResource("/aicons/kicker.png"));
        Image image4 = imageIcon3.getImage();
        ImageIcon imageIcon4 = new ImageIcon(this.getClass().getResource("/aicons/captain.png"));
        Image image5 = imageIcon4.getImage();
        ImageIcon imageIcon5 = new ImageIcon(this.getClass().getResource("/aicons/escanteiosicone.png"));
        Image image6 = imageIcon5.getImage();
        ImageIcon imageIcon6 = new ImageIcon(this.getClass().getResource("/aicons/falsonove.png"));
        Image image7 = imageIcon6.getImage();
        int n5 = 480;
        int n6 = 679;
        if (n4 == 1) {
            n5 = 580;
            n6 = 160;
        }
        if (n4 == 3) {
            n5 = 200;
            n6 = 650;
        }
        if (bl2) {
            n5 = 390;
            n6 = 566;
        }
        BufferedImage bufferedImage = new BufferedImage(n5, n6, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        mod.Escalacao.prepara(graphics2D);
        int n7 = 1;
        int n8 = 25;
        if (n4 == 1 || n4 == 3) {
            n7 = 26;
            n8 = 36;
        }
        int n9 = n7;
        while (n9 <= n8) {
            if (((ao)arrayList.get(n9)).vf() != null && n9 != n2) {
                if (n9 == 1) {
                    image = this.AI.getImage();
                } else if (n9 <= 25) {
                    image = ((ao)arrayList.get(n9)).x() != null ? image2 : this.AH.getImage();
                } else if (n9 > 25) {
                    image = ((ao)arrayList.get(n9)).x() != null ? (((ao)arrayList.get(n9)).x().getPosicao() == 0 ? this.AI.getImage() : image2) : this.AH.getImage();
                }
                int n10 = RJ[n9][0];
                int n11 = RJ[n9][1];
                if (n4 == 1) {
                    n10 = RJ[n9][0] - 450;
                    n11 = RJ[n9][1] - 30;
                }
                if (n4 == 2) {
                    n10 = RJ[n9][0];
                    n11 = RJ[n9][1];
                }
                if (n4 == 3) {
                    n10 = RK[n9][0];
                    n11 = RK[n9][1];
                }
                if (bl2) {
                    n10 = RK[n9][0];
                    n11 = RK[n9][1];
                }
                mod.Escalacao.camisa(graphics2D, image, n10, n11, 0, 0);
                if (((ao)arrayList.get(n9)).x() != null) {
                    int n12;
                    graphics2D.setFont(new Font("Arial", 1, 11));
                    graphics2D.setFont(graphics2D.getFont().deriveFont(11.0f));
                    graphics2D.drawImage(ao.a(((ao)arrayList.get(n9)).x(), 1, n9, ""), n10 - 52, n11 + 47, null);
                    if (((ao)arrayList.get(n9)).x().fP()) {
                        graphics2D.drawImage(image3, n10 - 15, n11 + 30, null);
                    }
                    if (n9 < 26) {
                        if (ah2.ke() == ((ao)arrayList.get(n9)).x()) {
                            graphics2D.drawImage(image4, n10 - 10, n11, null);
                        }
                        n12 = 10;
                        if (ah2.ke() == ah2.kd()) {
                            n12 = 25;
                        }
                        if (ah2.kd() == ((ao)arrayList.get(n9)).x()) {
                            graphics2D.drawImage(image5, n10 - n12, n11, null);
                        }
                        if (ah2.lq() == ((ao)arrayList.get(n9)).x()) {
                            graphics2D.drawImage(image6, n10 + 40, n11 + 20, null);
                        }
                        if (n9 > 14 && n9 != 17 && ah2.lr() == ((ao)arrayList.get(n9)).x()) {
                            graphics2D.drawImage(image7, n10 + 40, n11 + 35, null);
                        }
                    }
                    mod.Escalacao.numero(graphics2D, image, ((ao)arrayList.get(n9)).x().fn(), n10, n11, image.getWidth(null), image.getHeight(null));
                }
            }
            ++n9;
        }
        graphics2D.dispose();
        return new ImageIcon(bufferedImage);
    }

    public ImageIcon a(ArrayList arrayList, int n2, ImageIcon imageIcon) {
        int n3;
        Image image = this.AH.getImage();
        Image image2 = imageIcon.getImage();
        BufferedImage bufferedImage = new BufferedImage(RF, RG, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        mod.Escalacao.prepara(graphics2D);
        RH.clear();
        RI.clear();
        int n4 = 20;
        int n5 = 0;
        int n6 = n4;
        int n7 = 0;
        int n8 = 37;
        while (n8 < arrayList.size()) {
            if (((ao)arrayList.get(n8)).vf() != null) {
                Image image3 = ((ao)arrayList.get(n8)).x() != null && ((ao)arrayList.get(n8)).x().getPosicao() == 0 ? this.AI.getImage() : image2;
                mod.Escalacao.cartao(graphics2D, n6 - 21, n7 - 4, 87, 84);
                if (n8 != n2) {
                    mod.Escalacao.camisa(graphics2D, image3, n6, n7, 45, 45);
                }
                if (((ao)arrayList.get(n8)).x() != null) {
                    graphics2D.drawImage(ao.a(((ao)arrayList.get(n8)).x(), 2, n8, ""), n6 - 53, n7 + 42, null);
                    if (n8 != n2) {
                        mod.Escalacao.numero(graphics2D, image3, ((ao)arrayList.get(n8)).x().fn(), n6, n7, 45, 45);
                    }
                }
                RH.add(n6 + RD);
                RI.add(n7 + RE);
                n6 += 100;
                if (++n5 == 6) {
                    n5 = 0;
                    n6 = n4;
                    n7 += 88;
                }
            }
            ++n8;
        }
        int[][] nArray = new int[RH.size()][2];
        n3 = 0;
        while (n3 < RH.size()) {
            nArray[n3][0] = (Integer)RH.get(n3);
            nArray[n3][1] = (Integer)RI.get(n3);
            ++n3;
        }
        RM = nArray;
        graphics2D.dispose();
        return new ImageIcon(bufferedImage);
    }

    public static Image a(F f2, int n2, int n3, String string) {
        if (f2 == null) {
            return null;
        }
        boolean individual = a.SR.isHabilidadeIndividual();
        int forca = individual ? -1 : f2.fi();
        if (n2 == 1) {
            String pos = aq.rI[f2.getPosicao()];
            boolean improvisado = false;
            if (n3 >= 1 && n3 <= 25) {
                pos = aq.rI[aq.sE[n3][0]];
                if (n3 == 10 || n3 == 17) {
                    pos = aq.rI[3];
                }
                if (aq.sE[n3][0] != f2.getPosicao()) {
                    improvisado = true;
                    if (!(n3 != 10 && n3 != 17 || f2.getPosicao() != 1 && f2.getPosicao() != 3)) {
                        improvisado = false;
                    }
                    if (improvisado) {
                        pos = pos + " (" + aq.rI[f2.getPosicao()] + ")";
                    }
                }
            }
            return mod.Escalacao.rotulo(f2.getNome(), pos, improvisado, forca, f2.fp(), null, null, true);
        }
        if (n2 == 2) {
            // na lista de disponiveis o nome cabe no cartao (86 px), para nao invadir o vizinho
            return mod.Escalacao.rotulo(f2.getNome(), aq.rI[f2.getPosicao()], false, forca, f2.fp(), aq.rK[f2.getLado()], null, false, 92);
        }
        String pos = "";
        if (n3 >= 1 && n3 <= 25) {
            pos = aq.rI[aq.sE[n3][0]];
        }
        return mod.Escalacao.rotulo(f2.getNome(), pos, false, -1, -1, null, string == null ? "" : string, true);
    }

    public static void a(Graphics2D graphics2D, String string, int n2, int n3, int n4, boolean bl2, int n5) {
        Dimension dimension = new Dimension(n5, 40);
        graphics2D.setColor(Color.BLACK);
        graphics2D.drawString(string, (dimension.width - n3) / 2 + 1, n4 + 1);
        if (bl2) {
            graphics2D.setColor(Color.YELLOW);
        } else {
            graphics2D.setColor(Color.WHITE);
        }
        graphics2D.drawString(string, (dimension.width - n3) / 2, n4);
    }

    public void vg() {
        this.U = null;
        this.cg = null;
        this.RC = null;
    }

    public void ej(int n2) {
        this.U = null;
        if (n2 <= 36) {
            this.RC = this.AH;
        }
    }

    public void vh() {
        this.RC = this.AH;
    }

    public static void a(int[][] nArray) {
        RM = nArray;
    }

    public static int vi() {
        return RD;
    }

    public static int vj() {
        return RE;
    }

    public static ImageIcon i(ImageIcon imageIcon) {
        Image image = imageIcon.getImage();
        BufferedImage bufferedImage = new BufferedImage(40, 40, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, 40, 40, null);
        graphics2D.setComposite(AlphaComposite.Src);
        return new ImageIcon(bufferedImage);
    }

    public ImageIcon a(ArrayList arrayList, int n2, ImageIcon imageIcon, boolean bl2, int n3, F f2) {
        Image image = this.AH.getImage();
        Image image2 = imageIcon.getImage();
        ImageIcon imageIcon2 = new ImageIcon(this.getClass().getResource("/aicons/bolaouro_s2.png"));
        Image image3 = imageIcon2.getImage();
        int n4 = 480;
        int n5 = 679;
        if (bl2) {
            n4 = 390;
            n5 = 566;
        }
        BufferedImage bufferedImage = new BufferedImage(n4, n5, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        mod.Escalacao.prepara(graphics2D);
        int n6 = 1;
        while (n6 <= 25) {
            if (((ao)arrayList.get(n6)).vf() != null && n6 != n2) {
                if (n6 == 1) {
                    image = this.AI.getImage();
                } else if (n6 <= 25) {
                    image = ((ao)arrayList.get(n6)).x() != null ? ((ao)arrayList.get(n6)).vf().getImage() : this.AH.getImage();
                } else if (n6 > 25) {
                    image = ((ao)arrayList.get(n6)).x() != null ? (((ao)arrayList.get(n6)).x().getPosicao() == 0 ? this.AI.getImage() : image2) : this.AH.getImage();
                }
                int n7 = RJ[n6][0];
                int n8 = RJ[n6][1];
                if (bl2) {
                    n7 = RL[n6][0];
                    n8 = RL[n6][1];
                }
                mod.Escalacao.camisa(graphics2D, image, n7, n8, 0, 0);
                if (((ao)arrayList.get(n6)).x() != null) {
                    String string = "";
                    if (((ao)arrayList.get(n6)).fg() != null) {
                        string = ((ao)arrayList.get(n6)).fg().getNome();
                    }
                    graphics2D.drawImage(ao.a(((ao)arrayList.get(n6)).x(), 3, n6, string), n7 - 50, n8 + 47, null);
                    if (((ao)arrayList.get(n6)).x() == f2) {
                        graphics2D.drawImage(image3, n7 - 20, n8 + 20, null);
                    }
                }
            }
            ++n6;
        }
        graphics2D.dispose();
        return new ImageIcon(bufferedImage);
    }

    public void n(ah ah2) {
        this.cg = ah2;
    }

    public ah fg() {
        return this.cg;
    }
}

