package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// "E campeao!" (a.dJ): a janela pequena com a imagem fixa vira uma festa animada. Estadio
// ao fundo com zoom lento, refletores que varrem, a taca da competicao subindo no podio
// com brilho, CAMPEAO! letra por letra, canhoes de confete nas cores do clube, fogos e o
// som da torcida (se os sons estiverem ligados). Clique pula a abertura; Continuar, X ou
// Esc fecham, como o X do jogo.
final class Campeao {
    private Campeao() {}

    static final int W = 640, H = 640;
    // Para gravar o video quadro a quadro: tempo fixo em segundos (null = relogio).
    static volatile Double tempoFixo = null;

    static void aplica(JPanel dj) {
        if (dj.getClientProperty(Janelas.FEITO) != null) return;
        dj.putClientProperty(Janelas.FEITO, Boolean.TRUE);
        String time = texto(Dialogos.campo(dj, "uu"));
        String comp = texto(Dialogos.campo(dj, "Sx"));
        String tec = texto(Dialogos.campo(dj, "Fs"));
        Object ic = Dialogos.campo(dj, "uu") instanceof JLabel ? ((JLabel) Dialogos.campo(dj, "uu")).getIcon() : null;
        Object at = Dialogos.campo(dj, "aeJ");
        best.ah clube = procuraTime(time);
        Festa f = new Festa(time, comp, tec, clube, ic instanceof Icon ? (Icon) ic : null, trofeu(at, comp));
        dj.removeAll();
        dj.setLayout(new BorderLayout());
        dj.setBorder(null);
        dj.setOpaque(true);
        dj.setBackground(Color.BLACK);
        dj.add(f, BorderLayout.CENTER);
        dj.revalidate();
    }

    private static String texto(Object o) {
        return o instanceof JLabel && ((JLabel) o).getText() != null ? ((JLabel) o).getText().trim() : "";
    }

    // O dialogo nao guarda o time: procura entre os times dos tecnicos humanos pelo nome.
    private static best.ah procuraTime(String nome) {
        try {
            for (Object o : c.a.SR.aN()) {
                if (o instanceof best.ah && nome.equals(((best.ah) o).getNome())) return (best.ah) o;
            }
        } catch (Throwable t) {
            // sem lista de times
        }
        return null;
    }

    // Taca: um dos seis modelos nossos (mod/tacas, tools/tacas/tacas.py) escolhido pelo tipo
    // da competicao, com o nome gravado na placa. Sem o modelo, usa a taca do jogo.
    private static Image trofeu(Object at, String comp) {
        String nome = "";
        try {
            nome = ((best.at) at).mA()[0];
        } catch (Throwable t) {
            // sem nome: modelo de liga
        }
        try {
            java.net.URL u = Campeao.class.getResource("/mod/tacas/" + modelo(nome) + ".png");
            if (u != null) return grava(javax.imageio.ImageIO.read(u), comp);
        } catch (Throwable t) {
            // taca do jogo abaixo
        }
        try {
            ImageIcon i = best.aq.x(nome);
            if (i != null && i.getIconWidth() > 0) return i.getImage();
        } catch (Throwable t) {
            // taca generica abaixo
        }
        try {
            return best.aq.mf().getImage();
        } catch (Throwable t) {
            return null;
        }
    }

    // Taca pequena (Tabelas, "Maior vencedor"): o modelo sem nome na placa, reduzido com suavizacao.
    static ImageIcon miniatura(String nome, int alt) {
        try {
            java.net.URL u = Campeao.class.getResource("/mod/tacas/" + modelo(nome) + ".png");
            if (u == null) return null;
            return new ImageIcon(reduz(grava(javax.imageio.ImageIO.read(u), ""), alt));
        } catch (Throwable t) {
            return null;
        }
    }

    static String modelo(String n) {
        n = n == null ? "" : n.toLowerCase().replace("tr_", "");
        String[] selecoes = {"copamundo", "copaamerica", "eurocopa", "copaafrica", "copaasia", "copaouro", "copaofc",
            "liganacoes", "finalissima", "mundial", "supermundial"};
        String[] continental = {"libertadores", "sulamericana", "ligacampeoes", "ligaeuropa", "conference", "ligaafc",
            "ligacaf", "ligaconcacaf", "ligaofc"};
        String[] estadual = {"estadual", "copanordeste", "copaverde", "riosaopaulo"};
        for (String s : selecoes) if (n.startsWith(s)) return "selecoes";
        for (String s : continental) if (n.startsWith(s)) return "continental";
        for (String s : estadual) if (n.startsWith(s)) return "estadual";
        if (n.startsWith("supercopa") || n.startsWith("recopa")) return "supercopa";
        if (n.startsWith("copa")) return "copa";
        return "liga";
    }

    // Placa da base (retangulo de tools/tacas/tacas.py, igual nos seis modelos).
    static final Rectangle PLACA = new Rectangle(132, 504, 176, 49);

    // Escreve o nome na placa, como gravado no metal, e corta as bordas transparentes.
    static BufferedImage grava(BufferedImage src, String comp) {
        BufferedImage im = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        String s = comp == null ? "" : comp.trim().toUpperCase();
        if (s.length() > 0) {
            Rectangle r = PLACA;
            float px = 26f;
            Font f;
            FontMetrics fm;
            do {
                f = Estreia.titulo(px);
                fm = g.getFontMetrics(f);
                px -= 0.5f;
            } while (fm.stringWidth(s) > r.width - 16 && px > 11);
            float x = r.x + (r.width - fm.stringWidth(s)) / 2f;
            float y = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2f;
            g.setFont(f);
            g.setColor(new Color(255, 236, 190, 120));
            g.drawString(s, x, y + 1.2f);
            g.setColor(new Color(0x3A2A0E));
            g.drawString(s, x, y);
        }
        g.dispose();
        // corta o transparente em volta
        int x0 = im.getWidth(), y0 = im.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < im.getHeight(); y++) {
            for (int x = 0; x < im.getWidth(); x++) {
                if ((im.getRGB(x, y) >>> 24) > 8) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                }
            }
        }
        if (x1 < 0) return im;
        return im.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }

    // ------------------------------------------------------------------------------------

    static class Festa extends JComponent {
        final String time, comp, tec;
        final Icon escudo;
        final BufferedImage taca, fundo;
        final Color cor1, cor2;
        final Random rnd = new Random();
        final List<Confete> confetes = new ArrayList<Confete>();
        final List<Faisca> faiscas = new ArrayList<Faisca>();
        final Timer timer;
        long inicio = -1;
        double ultimo = 0, proximoFogo = 1.7, chuva = 0;
        boolean som = false, canhoes = false;
        final JButton continuar = new JButton("Continuar"), fechar = new JButton();

        Festa(String time, String comp, String tec, best.ah clube, Icon pequeno, Image tr) {
            this.time = time;
            this.comp = comp;
            this.tec = tec;
            Icon e = pequeno;
            Color a = Estreia.OURO, b = Estreia.BRANCO;
            if (clube != null) {
                try { e = clube.x(64, 64); } catch (Throwable t) { /* escudo pequeno */ }
                try {
                    if (clube.kB() != null) a = clube.kB();
                    if (clube.kC() != null) b = clube.kC();
                } catch (Throwable t) { /* cores padrao */ }
            }
            escudo = e;
            cor1 = a;
            cor2 = b;
            taca = tr == null ? null : tr instanceof BufferedImage ? reduz((BufferedImage) tr, ALTURA_TACA) : escala(tr, 1.45);
            fundo = carregaFundo();
            setLayout(null);
            setOpaque(true);

            Editor.botao(continuar, "Continuar", "seta", true);
            continuar.setHorizontalTextPosition(SwingConstants.LEFT);
            continuar.setBounds((W - 170) / 2, H - 66, 170, 38);
            continuar.addActionListener(ev -> fecha());
            continuar.setVisible(false);
            add(continuar);
            Janelas.fechar(fechar);
            fechar.setBounds(W - 52, 14, 36, 36);
            fechar.addActionListener(ev -> fecha());
            add(fechar);

            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "fecha");
            getActionMap().put("fecha", new AbstractAction() {
                public void actionPerformed(java.awt.event.ActionEvent ev) { fecha(); }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent ev) {
                    // pula a abertura
                    if (tempo() < 2.6) inicio -= (long) ((2.6 - tempo()) * 1e9);
                }
            });
            timer = new Timer(16, ev -> passo());
        }

        void fecha() {
            timer.stop();
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            // a janela do jogo tem 306 x 413: aumenta e centraliza
            SwingUtilities.invokeLater(() -> {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null && w.getWidth() < W) {
                    w.setSize(W, H);
                    w.setLocationRelativeTo(w.getOwner());
                    w.validate();
                }
            });
            timer.start();
        }

        @Override
        public void removeNotify() {
            timer.stop();
            super.removeNotify();
        }

        double tempo() {
            if (tempoFixo != null) return tempoFixo;
            long agora = System.nanoTime();
            if (inicio < 0) inicio = agora;
            return (agora - inicio) / 1e9;
        }

        void passo() {
            double t = tempo();
            double dt = Math.max(0, Math.min(0.05, t - ultimo));
            ultimo = t;
            simula(t, dt);
            repaint();
        }

        // Fisica separada da pintura para o video quadro a quadro poder avancar o tempo.
        void simula(double t, double dt) {
            if (!canhoes && t > 0.35) {
                canhoes = true;
                canhao(-10, H - 40, -0.95);
                canhao(W + 10, H - 40, -2.19);
            }
            if (!som && t > 1.05) {
                som = true;
                torcida();
            }
            if (t > 0.9) {
                chuva += dt * 38;
                while (chuva >= 1) {
                    chuva -= 1;
                    confetes.add(new Confete(rnd.nextDouble() * W, -12, (rnd.nextDouble() - 0.5) * 30, 40 + rnd.nextDouble() * 50, cor()));
                }
            }
            if (t > proximoFogo) {
                proximoFogo = t + 0.7 + rnd.nextDouble() * 0.9;
                fogo(70 + rnd.nextDouble() * (W - 140), 70 + rnd.nextDouble() * 150);
            }
            for (int i = confetes.size() - 1; i >= 0; i--) {
                Confete c = confetes.get(i);
                c.passo(dt);
                if (c.y > H + 20 || c.x < -40 || c.x > W + 40) confetes.remove(i);
            }
            for (int i = faiscas.size() - 1; i >= 0; i--) {
                Faisca f = faiscas.get(i);
                f.passo(dt);
                if (f.vida <= 0) faiscas.remove(i);
            }
            continuar.setVisible(t > 2.3);
        }

        Color cor() {
            int k = rnd.nextInt(10);
            if (k < 4) return Estreia.OURO;
            if (k < 5) return new Color(0xFFE7A3);
            if (k < 6) return Estreia.BRANCO;
            if (k < 8) return cor1;
            return cor2;
        }

        void canhao(double x, double y, double ang) {
            for (int i = 0; i < 90; i++) {
                double a = ang + (rnd.nextDouble() - 0.5) * 0.5, v = 380 + rnd.nextDouble() * 420;
                confetes.add(new Confete(x, y, Math.cos(a) * v, Math.sin(a) * v, cor()));
            }
        }

        void fogo(double x, double y) {
            Color c = rnd.nextInt(3) == 0 ? cor1 : rnd.nextBoolean() ? Estreia.OURO : new Color(0xFFE7A3);
            if (c.getRed() + c.getGreen() + c.getBlue() < 160) c = Estreia.OURO; // cor escura some no ceu
            int n = 46 + rnd.nextInt(20);
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + rnd.nextDouble() * 0.1, v = 90 + rnd.nextDouble() * 70;
                faiscas.add(new Faisca(x, y, Math.cos(a) * v, Math.sin(a) * v, c));
            }
        }

        void torcida() {
            try {
                if (c.a.SR == null || !c.a.SR.isUsaSons()) return;
                java.io.File f = new java.io.File(System.getProperty("user.dir"), "sons/gol1.wav");
                if (!f.exists()) return;
                new Thread(() -> {
                    try {
                        javax.sound.sampled.AudioInputStream in = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
                        javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                        clip.open(in);
                        clip.start();
                    } catch (Throwable e) {
                        // sem som
                    }
                }, "campeao-som").start();
            } catch (Throwable e) {
                // sem som
            }
        }

        // ------------------------------------------------------------------ pintura

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            double t = tempo();
            ceu(g, t);
            refletores(g, t);
            for (Faisca f : faiscas) f.pinta(g);
            podio(g, t);
            trofeu(g, t);
            textos(g, t);
            for (Confete c : confetes) c.pinta(g);
            // clarao quando o titulo cai
            double fl = (t - 1.45) / 0.35;
            if (fl > 0 && fl < 1) {
                g.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, 150), 420, new float[] {0f, 1f},
                    new Color[] {new Color(255, 236, 180, (int) (150 * (1 - fl))), new Color(255, 236, 180, 0)}));
                g.fillRect(0, 0, W, H);
            }
            // entrada: sai do preto
            double fi = 1 - t / 0.5;
            if (fi > 0) {
                g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, fi))));
                g.fillRect(0, 0, W, H);
            }
            g.setColor(new Color(244, 241, 234, 40));
            g.drawRect(0, 0, W - 1, H - 1);
            g.dispose();
        }

        void ceu(Graphics2D g, double t) {
            g.setColor(new Color(0x07090A));
            g.fillRect(0, 0, W, H);
            if (fundo != null) {
                double z = 1.04 + 0.05 * Math.min(1, t / 14.0);
                int fw = (int) (W * z * 1.0), fh = (int) ((double) fundo.getHeight() / fundo.getWidth() * fw);
                if (fh < H * z) { fh = (int) (H * z); fw = (int) ((double) fundo.getWidth() / fundo.getHeight() * fh); }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
                g2.drawImage(fundo, (W - fw) / 2, (H - fh) / 2 - 30, fw, fh, null);
                g2.dispose();
            }
            // escurece embaixo e nas bordas, luz dourada no centro
            g.setPaint(new GradientPaint(0, 0, new Color(5, 6, 6, 170), 0, H, new Color(3, 4, 4, 245)));
            g.fillRect(0, 0, W, H);
            double pulso = 0.85 + 0.15 * Math.sin(t * 2.1);
            int a = (int) (Math.min(1, Math.max(0, (t - 0.4) / 0.9)) * 120 * pulso);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(W / 2f, 330), 330, new float[] {0f, 0.55f, 1f},
                new Color[] {Tema.alfa(Estreia.OURO, a), Tema.alfa(Estreia.OURO, a / 4), Tema.alfa(Estreia.OURO, 0)}));
            g.fillRect(0, 0, W, H);
        }

        void refletores(Graphics2D g, double t) {
            double liga = Math.min(1, Math.max(0, (t - 0.15) / 0.5));
            if (liga <= 0) return;
            // pisca ao acender
            if (t < 0.7 && ((int) (t * 30)) % 3 == 0) liga *= 0.4;
            double[][] fontes = {{70, -30, 0.0}, {W - 70, -30, 1.7}, {W / 2.0, -60, 3.1}};
            for (double[] f : fontes) {
                double alvoX = W / 2.0 + Math.sin(t * 0.9 + f[2]) * 150, alvoY = 380;
                double ang = Math.atan2(alvoY - f[1], alvoX - f[0]);
                double len = Math.hypot(alvoX - f[0], alvoY - f[1]) + 160, abre = 0.16;
                Path2D p = new Path2D.Double();
                p.moveTo(f[0], f[1]);
                p.lineTo(f[0] + Math.cos(ang - abre) * len, f[1] + Math.sin(ang - abre) * len);
                p.lineTo(f[0] + Math.cos(ang + abre) * len, f[1] + Math.sin(ang + abre) * len);
                p.closePath();
                int a = (int) (60 * liga);
                g.setPaint(new GradientPaint((float) f[0], (float) f[1], new Color(255, 240, 200, a),
                    (float) (f[0] + Math.cos(ang) * len), (float) (f[1] + Math.sin(ang) * len), new Color(255, 240, 200, 0)));
                g.fill(p);
            }
        }

        void podio(Graphics2D g, double t) {
            double e = suave((t - 0.3) / 0.6);
            if (e <= 0) return;
            float cx = W / 2f, cy = 412;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) e));
            // degraus
            for (int i = 2; i >= 0; i--) {
                float w = 300 - i * 70, h = 46 - i * 10, y = cy + i * 4 - i * 12;
                float top = y - (2 - i) * 0;
                Ellipse2D base = new Ellipse2D.Float(cx - w / 2, top + 10, w, h);
                Ellipse2D tampa = new Ellipse2D.Float(cx - w / 2, top, w, h);
                g2.setColor(new Color(0x111312));
                g2.fill(base);
                g2.fill(new Rectangle2D.Float(cx - w / 2, top + h / 2, w, 10));
                g2.setPaint(new GradientPaint(0, top, new Color(0x2A2D2B), 0, top + h, new Color(0x161817)));
                g2.fill(tampa);
                double brilho = 0.6 + 0.4 * Math.sin(t * 2.4 + i);
                g2.setStroke(new BasicStroke(1.6f));
                g2.setColor(Tema.alfa(Estreia.OURO, (int) (200 * brilho)));
                g2.draw(tampa);
            }
            g2.dispose();
        }

        void trofeu(Graphics2D g, double t) {
            if (taca == null) return;
            double e = (t - 0.55) / 0.85;
            if (e <= 0) return;
            double s = 0.55 + 0.45 * volta(Math.min(1, e));
            double sobe = (1 - suave(Math.min(1, e))) * 90;
            double flutua = t > 1.4 ? Math.sin((t - 1.4) * 1.8) * 4 : 0;
            int tw = taca.getWidth(), th = taca.getHeight();
            double cx = W / 2.0, base = 408 + sobe + flutua;
            // halo
            float a = (float) Math.min(1, e);
            double pulso = 0.8 + 0.2 * Math.sin(t * 3.0);
            g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, base - th * s * 0.55), (float) (th * s * 0.9),
                new float[] {0f, 1f}, new Color[] {Tema.alfa(new Color(0xFFE7A3), (int) (110 * a * pulso)), Tema.alfa(Estreia.OURO, 0)}));
            g.fillRect(0, 0, W, H);
            AffineTransform at = new AffineTransform();
            at.translate(cx - tw * s / 2, base - th * s);
            at.scale(s, s);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g2.drawImage(brilho(t), at, null);
            g2.dispose();
            // estrelinhas em volta da taca
            if (t > 1.3) {
                for (int i = 0; i < 7; i++) {
                    double fase = t * 1.3 + i * 1.7;
                    double v = Math.sin(fase * 2.2);
                    if (v <= 0) continue;
                    double ang = i * 0.9 + 0.3, r = 70 + (i % 3) * 22;
                    double x = cx + Math.cos(ang) * r, y = base - th * s * 0.55 + Math.sin(ang) * r * 0.9;
                    estrela(g, x, y, 3 + 5 * v, Tema.alfa(new Color(0xFFF3CF), (int) (230 * v)));
                }
            }
        }

        // Taca com uma faixa de brilho que passa a cada 3 segundos.
        BufferedImage brilho(double t) {
            double c = ((t - 1.6) % 3.0) / 0.9;
            if (t < 1.6 || c > 1) return taca;
            BufferedImage im = new BufferedImage(taca.getWidth(), taca.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            g.drawImage(taca, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);
            int w = im.getWidth(), h = im.getHeight();
            float x = (float) (-w + c * 3 * w);
            g.setPaint(new GradientPaint(x, 0, new Color(255, 255, 255, 0), x + w * 0.35f, h * 0.35f, new Color(255, 255, 255, 0), true));
            g.setPaint(new java.awt.LinearGradientPaint(x, 0, x + w * 0.7f, h * 0.7f, new float[] {0f, 0.5f, 1f},
                new Color[] {new Color(255, 255, 255, 0), new Color(255, 250, 230, 170), new Color(255, 255, 255, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g.fillRect(0, 0, w, h);
            g.dispose();
            return im;
        }

        void textos(Graphics2D g, double t) {
            // competicao
            double ec = suave((t - 1.75) / 0.5);
            if (ec > 0) {
                Font f = Estreia.espacada(Estreia.negrito(12.5f), 0.28f);
                String s = comp.toUpperCase();
                FontMetrics fm = g.getFontMetrics(f);
                g.setFont(f);
                g.setColor(Tema.alfa(new Color(0xFFE7A3), (int) (230 * ec)));
                float y = (float) (62 + (1 - ec) * -10);
                g.drawString(s, (W - fm.stringWidth(s)) / 2f, y);
                g.setColor(Tema.alfa(Estreia.OURO, (int) (120 * ec)));
                int lw = (int) (60 * ec);
                g.fillRect(W / 2 - fm.stringWidth(s) / 2 - 14 - lw, (int) y - 5, lw, 1);
                g.fillRect(W / 2 + fm.stringWidth(s) / 2 + 14, (int) y - 5, lw, 1);
            }
            titulo(g, t);
            // time e tecnico
            double et = suave((t - 1.95) / 0.55);
            if (et > 0) {
                Font ft = Estreia.titulo(36f);
                FontMetrics fm = g.getFontMetrics(ft);
                String s = time.toUpperCase();
                int ew = escudo == null ? 0 : escudo.getIconWidth(), eh = escudo == null ? 0 : escudo.getIconHeight();
                int gap = escudo == null ? 0 : 12, tw = fm.stringWidth(s), total = ew + gap + tw;
                float y = (float) (500 + (1 - et) * 18);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) et));
                int x0 = (W - total) / 2;
                if (escudo != null) escudo.paintIcon(this, g2, x0, (int) (y - eh / 2f));
                g2.setFont(ft);
                g2.setColor(Estreia.BRANCO);
                g2.drawString(s, x0 + ew + gap, y - fm.getHeight() / 2f + fm.getAscent() - 2);
                if (!tec.isEmpty()) {
                    Font fx = Estreia.texto(13.5f);
                    FontMetrics mx = g2.getFontMetrics(fx);
                    String l = "Técnico · " + tec;
                    g2.setFont(fx);
                    g2.setColor(Estreia.FRACO);
                    g2.drawString(l, (W - mx.stringWidth(l)) / 2f, y + 42);
                }
                g2.dispose();
            }
        }

        // CAMPEAO! letra por letra: cada uma cai grande e assenta; depois um brilho passa.
        void titulo(Graphics2D g, double t) {
            Font f = Estreia.espacada(Estreia.titulo(84f), 0.03f);
            GlyphVector gv = f.createGlyphVector(g.getFontRenderContext(), "CAMPEÃO!");
            Rectangle2D b = gv.getVisualBounds();
            double x0 = (W - b.getWidth()) / 2 - b.getX(), y0 = 150;
            Path2D todas = new Path2D.Double();
            for (int i = 0; i < gv.getNumGlyphs(); i++) {
                double e = (t - 1.0 - i * 0.065) / 0.32;
                if (e <= 0) continue;
                e = Math.min(1, e);
                Shape s = gv.getGlyphOutline(i, (float) x0, (float) y0);
                Rectangle2D r = s.getBounds2D();
                double k = 1 + (1 - volta(e)) * 0.9;
                AffineTransform at = new AffineTransform();
                at.translate(r.getCenterX(), r.getCenterY() - (1 - suave(e)) * 50);
                at.scale(k, k);
                at.translate(-r.getCenterX(), -r.getCenterY());
                Shape sl = at.createTransformedShape(s);
                float a = (float) Math.min(1, e * 1.6);
                // sombra e brilho por tras
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                g2.translate(0, 4);
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fill(sl);
                g2.translate(0, -4);
                g2.setPaint(new GradientPaint(0, (float) (y0 - 70), new Color(0xFFF1C4), 0, (float) y0, new Color(0xD9A12B)));
                g2.fill(sl);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(255, 248, 220, 120));
                g2.draw(sl);
                g2.dispose();
                if (e >= 1) todas.append(sl, false);
            }
            // brilho que atravessa as letras
            double c = ((t - 2.0) % 3.0) / 1.0;
            if (t > 2.0 && c <= 1) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.clip(todas);
                float x = (float) (x0 - 120 + c * (b.getWidth() + 240));
                g2.setPaint(new java.awt.LinearGradientPaint(x - 60, (float) y0 - 80, x + 60, (float) y0, new float[] {0f, 0.5f, 1f},
                    new Color[] {new Color(255, 255, 255, 0), new Color(255, 255, 255, 190), new Color(255, 255, 255, 0)},
                    MultipleGradientPaint.CycleMethod.NO_CYCLE));
                g2.fill(todas.getBounds2D());
                g2.dispose();
            }
        }

        static void estrela(Graphics2D g, double x, double y, double r, Color c) {
            Path2D p = new Path2D.Double();
            p.moveTo(x, y - r);
            p.quadTo(x, y, x + r, y);
            p.quadTo(x, y, x, y + r);
            p.quadTo(x, y, x - r, y);
            p.quadTo(x, y, x, y - r);
            p.closePath();
            g.setColor(c);
            g.fill(p);
        }

        static double suave(double x) {
            if (x <= 0) return 0;
            if (x >= 1) return 1;
            return 1 - Math.pow(1 - x, 3);
        }

        // Passa um pouco do ponto e volta (ease-out-back).
        static double volta(double x) {
            if (x <= 0) return 0;
            if (x >= 1) return 1;
            double c1 = 1.70158, c3 = c1 + 1;
            return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2);
        }

        // ------------------------------------------------------------------ particulas

        class Confete {
            double x, y, vx, vy, rot, vrot, fase, w, h;
            final Color c;
            Confete(double x, double y, double vx, double vy, Color c) {
                this.x = x;
                this.y = y;
                this.vx = vx;
                this.vy = vy;
                this.c = c;
                rot = rnd.nextDouble() * Math.PI;
                vrot = (rnd.nextDouble() - 0.5) * 12;
                fase = rnd.nextDouble() * 6;
                w = 5 + rnd.nextDouble() * 5;
                h = 3 + rnd.nextDouble() * 4;
            }
            void passo(double dt) {
                vy += 260 * dt;
                double ar = Math.pow(0.12, dt); // resistencia do ar
                vx *= ar;
                vy = Math.min(vy, 95 + 20 * Math.sin(fase)) * (vy > 95 ? ar : 1);
                fase += dt * 5;
                x += (vx + Math.sin(fase) * 22) * dt;
                y += vy * dt;
                rot += vrot * dt;
            }
            void pinta(Graphics2D g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);
                g2.rotate(rot);
                double vira = Math.abs(Math.cos(fase * 1.3)); // gira em 3D
                g2.scale(1, 0.25 + 0.75 * vira);
                g2.setColor(vira < 0.4 ? c.darker() : c);
                g2.fill(new Rectangle2D.Double(-w / 2, -h / 2, w, h));
                g2.dispose();
            }
        }

        class Faisca {
            double x, y, vx, vy, vida = 1;
            final double px, py;
            final Color c;
            Faisca(double x, double y, double vx, double vy, Color c) {
                this.x = x;
                this.y = y;
                px = x;
                py = y;
                this.vx = vx;
                this.vy = vy;
                this.c = c;
            }
            void passo(double dt) {
                vy += 70 * dt;
                vx *= Math.pow(0.4, dt);
                vy *= Math.pow(0.4, dt);
                x += vx * dt;
                y += vy * dt;
                vida -= dt / 1.3;
            }
            void pinta(Graphics2D g) {
                if (vida <= 0) return;
                int a = (int) (255 * Math.min(1, vida * 1.4));
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Tema.alfa(c, a / 3));
                g.draw(new java.awt.geom.Line2D.Double(x - vx * 0.12, y - vy * 0.12, x, y));
                g.setColor(Tema.alfa(c, a));
                g.fill(new Ellipse2D.Double(x - 1.6, y - 1.6, 3.2, 3.2));
            }
        }
    }

    // ------------------------------------------------------------------ imagens

    static final int ALTURA_TACA = 248;

    // Reduz em passos de metade ate a altura pedida: a taca grande fica nitida, sem serrilhado.
    static BufferedImage reduz(BufferedImage im, int alt) {
        BufferedImage cur = im;
        while (cur.getHeight() > alt) {
            int nh = Math.max(alt, cur.getHeight() / 2);
            int nw = (int) Math.round(cur.getWidth() * (double) nh / cur.getHeight());
            BufferedImage d = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = d.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, nw, nh, null);
            g.dispose();
            cur = d;
        }
        return cur;
    }

    static BufferedImage escala(Image im, double s) {
        ImageIcon i = new ImageIcon(im);
        int w = i.getIconWidth(), h = i.getIconHeight();
        if (w <= 0 || h <= 0) return null;
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        i.paintIcon(null, g, 0, 0);
        g.dispose();
        // sobe em dois passos bicubicos para nao ficar serrilhado
        int nw = (int) Math.round(w * s), nh = (int) Math.round(h * s);
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D d = dst.createGraphics();
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        d.drawImage(src, 0, 0, nw, nh, null);
        d.dispose();
        return dst;
    }

    static BufferedImage carregaFundo() {
        try {
            java.net.URL u = Campeao.class.getResource("/mod/estadio.jpg");
            if (u == null) return null;
            BufferedImage im = javax.imageio.ImageIO.read(u);
            // reduz uma vez: o zoom por quadro fica leve
            int w = 760, h = im.getHeight() * w / im.getWidth();
            BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = r.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(im, 0, 0, w, h, null);
            g.dispose();
            return r;
        } catch (Throwable t) {
            return null;
        }
    }
}
