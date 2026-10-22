package mod;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.ActionMap;
import javax.swing.GroupLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Partida ao vivo (rodada, a.dV): campo visto de cima com os 22 jogadores se mexendo.
// O motor do jogo (c.b) continua decidindo tudo: a cada minuto esta tela le o que ele
// registrou na partida do jogador (finalizacao, defesa, chute para fora, desarme, passe
// errado, gol, cartao, substituicao) e anima um lance que conta essa mesma historia.
// O resultado nunca muda; a animacao so mostra o que ja aconteceu.
// A tabela com os outros jogos continua la, na aba "Outros jogos".
public class Partida extends JComponent implements ActionListener {

    // posicao de cada vaga do Escalar (components.ao): x 35..345 (esquerda..direita), y 30 (ataque)..565 (goleiro)
    static final int[][] VAGA = {{190, 315}, {190, 565}, {35, 425}, {100, 485}, {190, 485}, {285, 485}, {100, 405},
        {190, 405}, {285, 405}, {345, 425}, {35, 265}, {100, 315}, {190, 315}, {285, 315}, {100, 205}, {190, 205},
        {285, 205}, {345, 265}, {35, 55}, {100, 115}, {190, 115}, {285, 115}, {100, 35}, {190, 30}, {285, 35}, {345, 55}};
    static final int GOLEIRO = 0, DEFESA = 1, MEIO = 2, ATAQUE = 3;
    // tipos de lance
    static final int NADA = 0, GOL = 1, DEFENDE = 2, FORA = 3, DESARME = 4, ERRO = 5, PENALTI_PERDIDO = 6;

    static final Color GRAMA_A = new Color(0x1C5A2A), GRAMA_B = new Color(0x184F25);
    static final Color LINHA = new Color(255, 255, 255, 190);
    static final Color OURO = Tema.NEON;

    static boolean preferirCampo = true;   // ultima aba escolhida (vale para as proximas rodadas)
    static Partida atual;

    final JPanel dv;
    final JScrollPane tabela;
    final JPanel cartas;
    final Abas abas = new Abas();
    final Timer relogio = new Timer(30, this);
    final Random sorte = new Random();
    long ultimoQuadro;
    boolean foiMostrado, verCampo;

    // partida acompanhada
    Object jogo, casa, fora;
    int humano;                 // lado do time do jogador (0 casa, 1 fora)
    int gt = -1, tempo = -1;
    int[][] antes;
    int eventosVistos;
    final Map<Object, Ator> atores = new IdentityHashMap<Object, Ator>();
    final int[] proximoNumero = {12, 12};
    final int[] pressao = new int[104];
    final ArrayList<Linha> lances = new ArrayList<Linha>();
    Color[] cor = {Color.WHITE, Color.BLACK}, aro = {Color.BLACK, Color.WHITE};

    // bola e lance atual
    Lance lance;
    double bolaX = 0.5, bolaY = 0.5, bolaH;
    double refX = 0.5, refY = 0.5;          // bola "suavizada" que o bloco do time acompanha
    double ritmo = 1, acelera = 1;          // 1 = ritmo de verdade; acelera > 1 nas velocidades rapidas
    boolean bolaVisivel = true;
    Ator dono, receptor;
    boolean conduz;
    int posse;
    double rastroX0, rastroY0;
    long rastroAte;
    String legenda = "";
    int legendaMin;
    final ArrayList<Anuncio> anuncios = new ArrayList<Anuncio>();

    // momentos especiais
    long golDe, golAte;
    String golNome = "", golTime = "";
    int golLado;
    Ator golAutor;
    final ArrayList<Marca> marcas = new ArrayList<Marca>();
    boolean pausado, fim;
    long retomaEm;
    String aviso;
    long avisoAte;

    // ordens ao vivo (c.Ordens): cartao aberto, botao na coluna e plano que cada lado esta usando
    PainelOrdens ordens;
    boolean paramosGo;
    final Rectangle2D.Double botaoOrdens = new Rectangle2D.Double();
    final int[] mentalidade = new int[2], pressaoTime = {1, 1};
    final boolean[] planoVisto = new boolean[2];

    // notas ao vivo (Notas) e aviso de fora de posicao
    final Map<Object, Double> notas = new IdentityHashMap<Object, Double>();
    final Map<Object, double[]> tendencia = new IdentityHashMap<Object, double[]>();   // {nota de referencia, ate quando mostra a seta, direcao}
    String chaveNotas = "";
    final Map<Object, Boolean> foraAvisados = new IdentityHashMap<Object, Boolean>();
    final Map<Ator, Boolean> foraAgora = new IdentityHashMap<Ator, Boolean>();
    String alerta, alertaSub;
    long alertaDe, alertaAte;

    // camada pintada
    final Transmissao transmissao = new Transmissao(this);
    BufferedImage gramado;
    Rectangle2D.Double campo = new Rectangle2D.Double();

    static final class Ator {
        Object f;
        int lado, numero, vaga;
        double x, y, vx, vy;
        final double fase = Math.random() * 6.28;
        double passo = Math.random() * 8;     // passo da corrida do boneco 3D
        long visto;
        boolean solto;
    }

    static final class Chave {
        double t, x, y, h;
        Ator dono;
        boolean salto, chute;
        boolean disputa;    // quem chega toma a bola de quem esta conduzindo (nao e passe)
        Chave(double t, double x, double y, double h, Ator dono, boolean salto) {
            this.t = t; this.x = x; this.y = y; this.h = h; this.dono = dono; this.salto = salto;
        }
    }

    final class Lance {
        int tipo, lado;
        long inicio;
        double dur;
        final ArrayList<Chave> chaves = new ArrayList<Chave>();
        Runnable depois;
        boolean acabou;
        Linha linha;        // linha dos lances e legenda: aparecem quando a cena chega no marco
        String texto;
        Chave marco;
        Chave chave(double t, double x, double y, double h, Ator d) { return chave(t, x, y, h, d, false); }
        Chave chave(double t, double x, double y, double h, Ator d, boolean salto) {
            Chave c = new Chave(t, x, y, h, d, salto);
            chaves.add(c);
            return c;
        }
    }

    static final class Linha {
        String minuto, texto;
        int tipo, lado;
        Linha(String minuto, String texto, int tipo, int lado) { this.minuto = minuto; this.texto = texto; this.tipo = tipo; this.lado = lado; }
    }

    // linha e legenda guardadas ate a hora em que o lance acontece na cena
    static final class Anuncio {
        long em;
        Linha linha;
        String texto;
        int min;
    }

    static final class Marca {
        Ator ator;
        int tipo;   // 2 amarelo, 3/4 vermelho, 5 contusao, 6 substituicao
        long ate;
        Marca(Ator ator, int tipo, long ate) { this.ator = ator; this.tipo = tipo; this.ate = ate; }
    }

    // ------------------------------------------------------------------ instalacao

    static void instala(final JPanel dv) {
        if (dv.getClientProperty("mod.partida") != null) return;
        dv.putClientProperty("mod.partida", Boolean.TRUE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try { monta(dv); } catch (Throwable t) { t.printStackTrace(); }
            }
        });
    }

    static void monta(JPanel dv) throws Exception {
        if (!(dv.getLayout() instanceof GroupLayout)) return;
        JScrollPane ut = (JScrollPane) campo(dv.getClass(), dv, "ut");
        if (ut == null || ut.getParent() != dv) return;
        JPanel cartas = new JPanel(new CardLayout());
        cartas.setOpaque(false);
        Partida p = new Partida(dv, ut, cartas);
        JPanel embrulho = new JPanel(new BorderLayout(0, 8));
        embrulho.setOpaque(false);
        ((GroupLayout) dv.getLayout()).replace(ut, embrulho);
        cartas.add(p, "campo");
        cartas.add(ut, "tabela");
        embrulho.add(p.abas, BorderLayout.NORTH);
        embrulho.add(cartas, BorderLayout.CENTER);
        p.atalhos();
        p.mostra(false);
        p.abas.setVisible(false);
        p.relogio.start();
        atual = p;
        dv.revalidate();
    }

    Partida(JPanel dv, JScrollPane tabela, JPanel cartas) {
        this.dv = dv;
        this.tabela = tabela;
        this.cartas = cartas;
        setOpaque(false);
        relogio.setCoalesce(true);
        MouseAdapter m = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                long agora = System.currentTimeMillis();
                if (ordens == null && transmissao.emReplay(agora)) {
                    long resta = transmissao.pula(agora);
                    if (pausado) retomaEm = Math.max(agora, retomaEm - resta);
                    repaint();
                    return;
                }
                if (ordens != null) {
                    int r = ordens.clique(e.getPoint());
                    if (r == 2) fechaOrdens(true);
                    else if (r == 3) fechaOrdens(false);
                    repaint();
                } else if (botaoOrdens.contains(e.getPoint())) abreOrdens();
            }
            public void mouseMoved(MouseEvent e) {
                if (ordens != null) { ordens.mouse = e.getPoint(); repaint(); }
                boolean sobre = ordens == null && botaoOrdens.contains(e.getPoint());
                setCursor(java.awt.Cursor.getPredefinedCursor(sobre ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    // F1..F5 (velocidade) e Espaco/Enter (substituicoes) so funcionavam com a tabela em foco
    void atalhos() {
        try {
            JTable hs = (JTable) campo(dv.getClass(), dv, "Hs");
            InputMap de = hs.getInputMap();
            ActionMap acoes = hs.getActionMap();
            InputMap para = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            KeyStroke[] teclas = de.keys();
            if (teclas == null) return;
            for (KeyStroke k : teclas) {
                Object nome = de.get(k);
                if (nome != null && acoes.get(nome) != null) {
                    para.put(k, nome);
                    getActionMap().put(nome, acoes.get(nome));
                }
            }
        } catch (Throwable t) { t.printStackTrace(); }
        // O abre e fecha as ordens; Esc fecha sem aplicar
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, 0), "mod.ordens");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "mod.ordens.sai");
        getActionMap().put("mod.ordens", new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (ordens != null) fechaOrdens(false); else if (verCampo) abreOrdens();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, 0), "mod.camera");
        getActionMap().put("mod.camera", new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) { Transmissao.ligado = !Transmissao.ligado; repaint(); }
        });
        getActionMap().put("mod.ordens.sai", new javax.swing.AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (ordens != null) fechaOrdens(false); }
        });
    }

    // ------------------------------------------------------------------ ordens ao vivo

    c.b motor() {
        try { return ((best.I) jogo).hW(); } catch (Throwable t) { return null; }
    }

    Object timeHumano() { return humano == 0 ? casa : fora; }

    int minutoCorrido() { return tempo <= 1 ? gt : 45 + gt; }

    void abreOrdens() {
        if (jogo == null || fim || ordens != null || !Boolean.TRUE.equals(chama(timeHumano(), "jZ"))) return;
        best.ah meu = (best.ah) timeHumano();
        c.Ordens.Plano atual = c.Ordens.de(meu);
        if (!atual.humano) {
            c.b mt = motor();
            atual = mt != null && mt.plano(humano) != null ? mt.plano(humano) : c.Ordens.doEscalar(meu);
        }
        c.b mt = motor();
        int lado = mt != null ? mt.melhorLado(humano) : c.Ordens.MISTO;
        ordens = new PainelOrdens(atual, Math.max(0, gt) + "' do " + (tempo <= 1 ? "1º" : "2º") + " tempo", lado, dicas(lado));
        try {
            Timer go = (Timer) campo(dv.getClass(), dv, "GO");
            if (go != null && go.isRunning()) { go.stop(); paramosGo = true; }
        } catch (Throwable t) { t.printStackTrace(); }
        repaint();
    }

    void fechaOrdens(boolean aplica) {
        if (ordens == null) return;
        if (aplica) {
            best.ah meu = (best.ah) timeHumano();
            c.b mt = motor();
            c.Ordens.Plano antes = c.Ordens.de(meu).humano ? c.Ordens.de(meu) : mt != null ? mt.plano(humano) : null;
            c.Ordens.Plano p = ordens.plano.copia();
            p.humano = true;
            c.Ordens.marcaEscalar(p, meu);
            c.Ordens.define(meu, p);
            if (antes == null || !p.igual(antes)) {
                lances.add(0, new Linha(Math.max(0, gt) + "'", "Novas ordens: " + PainelOrdens.resumo(p) + ".", 30, humano));
                mentalidade[humano] = p.mentalidade;
                pressaoTime[humano] = p.pressao;
            }
        }
        ordens = null;
        if (paramosGo && !pausado) {
            try {
                Timer go = (Timer) campo(dv.getClass(), dv, "GO");
                if (go != null && !go.isRunning() && dv.isDisplayable()) go.restart();
            } catch (Throwable t) { t.printStackTrace(); }
        }
        paramosGo = false;
        setCursor(java.awt.Cursor.getDefaultCursor());
        repaint();
    }

    // o que o auxiliar diz: lado do ataque, placar e cansaco
    String[] dicas(int lado) {
        ArrayList<String> r = new ArrayList<String>();
        int saldo = inteiro(chama(jogo, humano == 0 ? "hu" : "hw")) - inteiro(chama(jogo, humano == 0 ? "hw" : "hu"));
        int min = minutoCorrido();
        c.b mt = motor();
        c.Ordens.Plano deles = mt == null ? null : mt.plano(1 - humano);
        if (deles != null && deles.mentalidade != c.Ordens.NORMAL) {
            String[] jeito = {"está na retranca", "está jogando na defensiva", "", "está jogando no ataque", "partiu para o tudo ou nada"};
            r.add(time(1 - humano) + " " + jeito[deles.mentalidade - c.Ordens.RETRANCA] + (deles.mentalidade > 0 ? ": sobra espaço nas costas deles." : ": vai ser difícil entrar."));
        }
        if (saldo < 0 && min >= 55) r.add("Perdendo na reta final: Ofensiva ou Tudo ou nada dão mais chance de empatar, mas abrem a defesa.");
        else if (saldo > 0 && min >= 55) r.add("Ganhando: Defensiva ou Retranca ajudam a segurar o placar, mas o time quase não ataca.");
        if (lado == c.Ordens.LATERAIS) r.add("Nossos pontas e laterais levam vantagem sobre a defesa deles: ataque pelas laterais.");
        else if (lado == c.Ordens.MEIO) r.add("Nosso meio é mais forte que a defesa deles por dentro: ataque pelo meio.");
        else r.add("Meio e laterais estão equilibrados contra essa defesa.");
        int soma = 0, n = 0;
        for (Ator a : atores.values()) if (a.lado == humano) { soma += inteiro(chama(a.f, "fp")); n++; }
        if (n > 0 && soma / n < 62) r.add("O time está cansado: pressão alta vai gastar ainda mais o fôlego.");
        while (r.size() > 3) r.remove(r.size() - 1);
        return r.toArray(new String[0]);
    }

    // a maquina muda de postura conforme o placar: avisa nos lances e mexe no posicionamento
    void lePlanos() {
        c.b mt = motor();
        if (mt == null) return;
        for (int lado = 0; lado < 2; lado++) {
            c.Ordens.Plano p = mt.plano(lado);
            if (p == null) continue;
            if (planoVisto[lado] && p.mentalidade != mentalidade[lado] && lado != humano) {
                String[] frases = {" se fecha na retranca.", " recua e passa a se defender.", " volta a jogar normal.", " vai para o ataque.", " parte para o tudo ou nada."};
                lances.add(0, new Linha(Math.max(0, gt) + "'", time(lado) + frases[p.mentalidade - c.Ordens.RETRANCA], 30, lado));
            }
            mentalidade[lado] = p.mentalidade;
            pressaoTime[lado] = p.pressao;
            planoVisto[lado] = true;
        }
    }

    void mostra(boolean campo) {
        verCampo = campo;
        ((CardLayout) cartas.getLayout()).show(cartas, campo ? "campo" : "tabela");
        abas.repaint();
    }

    // ------------------------------------------------------------------ leitura do jogo

    private static final Map<String, Method> metodos = new HashMap<String, Method>();

    static Object chama(Object o, String nome) {
        if (o == null) return null;
        try { return metodo(o.getClass(), nome).invoke(o); } catch (Throwable t) { return null; }
    }

    static Object chamaInt(Object o, String nome, int arg) {
        if (o == null) return null;
        for (Class<?> k = o.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Method m = k.getDeclaredMethod(nome, int.class);
                m.setAccessible(true);
                return m.invoke(o, arg);
            } catch (NoSuchMethodException e) {
                continue;
            } catch (Throwable t) { return null; }
        }
        return null;
    }

    static Object estatico(Class<?> c, String nome) {
        try { return metodo(c, nome).invoke(null); } catch (Throwable t) { return null; }
    }

    static Method metodo(Class<?> c, String nome) throws NoSuchMethodException {
        String k = c.getName() + "#" + nome;
        Method m = metodos.get(k);
        if (m != null) return m;
        for (Class<?> k2 = c; k2 != null && m == null; k2 = k2.getSuperclass()) {
            for (Method x : k2.getDeclaredMethods()) {
                if (x.getName().equals(nome) && x.getParameterTypes().length == 0) { m = x; break; }
            }
        }
        if (m == null) throw new NoSuchMethodException(nome);
        m.setAccessible(true);
        metodos.put(k, m);
        return m;
    }

    static Object campo(Class<?> c, Object o, String nome) {
        try {
            Field f = c.getDeclaredField(nome);
            f.setAccessible(true);
            return f.get(o);
        } catch (Throwable t) { return null; }
    }

    static int inteiro(Object o) { return o instanceof Number ? ((Number) o).intValue() : 0; }

    static List<?> lista(Object o) { return o instanceof List ? (List<?>) o : Collections.emptyList(); }

    static String nome(Object f) {
        Object n = chama(f, "getNome");
        return n == null ? "" : n.toString();
    }

    String time(int lado) { return nome(lado == 0 ? casa : fora); }

    int atraso() {
        int d = inteiro(campo(dv.getClass(), null, "d"));
        return d <= 0 ? 1200 : d;
    }

    int[][] estatisticas() {
        String[] nomes = {"hA", "hZ", "ia", "hB", "hC"};
        int[][] r = new int[nomes.length][2];
        for (int i = 0; i < nomes.length; i++) {
            Object v = chama(jogo, nomes[i]);
            if (v instanceof int[] && ((int[]) v).length >= 2) { r[i][0] = ((int[]) v)[0]; r[i][1] = ((int[]) v)[1]; }
        }
        return r;
    }

    public void actionPerformed(ActionEvent e) {
        if (dv.isDisplayable()) foiMostrado = true;
        else if (foiMostrado) { relogio.stop(); retoma(); return; }
        long agora = System.currentTimeMillis();
        double dt = ultimoQuadro == 0 ? 0.03 : Math.min(0.1, (agora - ultimoQuadro) / 1000.0);
        ultimoQuadro = agora;
        try {
            le(agora);
            if (jogo != null) { anuncia(agora); anima(agora, dt); transmissao.grava(agora); }
        } catch (Throwable t) { t.printStackTrace(); }
        if (pausado && agora >= retomaEm && ordens == null) retoma();
        if (jogo != null && isShowing()) repaint();
    }

    void le(long agora) {
        Class<?> c = dv.getClass();
        List<?> vk = lista(campo(c, null, "vK"));
        int gw = inteiro(campo(c, dv, "GW"));
        Object v = gw >= 0 && gw < vk.size() ? vk.get(gw) : null;
        Object j = v == null ? null : chama(v, "tR");
        if (j == null || chama(j, "hc") == null) {
            if (abas.isVisible()) { abas.setVisible(false); mostra(false); }
            return;
        }
        int m = inteiro(estatico(c, "qj")), t = inteiro(estatico(c, "en"));
        if (j != jogo) comeca(j, m, t, agora);
        if (pausado || fim || (m == gt && t == tempo)) return;
        boolean acabou = t == 2 && m >= 51;
        Timer go = (Timer) campo(c, dv, "GO");
        // janela aberta no meio do minuto (penalti, substituicao): espera ela fechar
        if (!acabou && go != null && !go.isRunning()) return;
        boolean virou = t != tempo;
        gt = m;
        tempo = t;
        if (acabou) { termina(agora); return; }
        if (virou) segundoTempo(agora);
        confereFora(agora);
        minuto(agora);
    }

    void comeca(Object j, int m, int t, long agora) {
        jogo = j;
        casa = chama(j, "hc");
        fora = chama(j, "hd");
        humano = Boolean.TRUE.equals(chama(fora, "jZ")) && !Boolean.TRUE.equals(chama(casa, "jZ")) ? 1 : 0;
        atores.clear();
        lances.clear();
        anuncios.clear();
        marcas.clear();
        java.util.Arrays.fill(pressao, 0);
        proximoNumero[0] = proximoNumero[1] = 12;
        antes = estatisticas();
        eventosVistos = lista(chama(j, "hE")).size();
        c.Ordens.limpa();
        planoVisto[0] = planoVisto[1] = false;
        mentalidade[0] = mentalidade[1] = 0;
        pressaoTime[0] = pressaoTime[1] = 1;
        ordens = null;
        notas.clear();
        tendencia.clear();
        chaveNotas = "";
        foraAvisados.clear();
        foraAgora.clear();
        alertaAte = 0;
        gt = m;
        tempo = t;
        fim = false;
        golAte = 0;
        cores();
        transmissao.novoJogo();
        for (int lado = 0; lado < 2; lado++) {
            List<?> l = lista(chama(j, lado == 0 ? "hp" : "hq"));
            ArrayList<Ator> novos = new ArrayList<Ator>();
            for (Object f : l) {
                int vaga = inteiro(chama(f, "fT"));
                if (vaga < 1 || vaga > 25) continue;
                Ator a = new Ator();
                a.f = f; a.lado = lado; a.vaga = vaga; a.visto = agora;
                novos.add(a);
            }
            Collections.sort(novos, new Comparator<Ator>() {
                public int compare(Ator a, Ator b) { return ordem(a.vaga) - ordem(b.vaga); }
            });
            int n = 1;
            for (Ator a : novos) {
                a.numero = n++;
                atores.put(a.f, a);
            }
        }
        bolaX = 0.5; bolaY = 0.5; bolaH = 0;
        refX = 0.5; refY = 0.5;
        posse = 0;
        for (Ator a : atores.values()) {
            double[] p = base(a, 0);
            a.x = p[0]; a.y = p[1];
        }
        dono = saida(0);
        lance = null;
        legenda = "Bola rolando: " + time(0) + " x " + time(1) + ".";
        legendaMin = 0;
        abas.setVisible(true);
        mostra(preferirCampo);
        dv.revalidate();
        confereFora(agora);
    }

    // nota ao vivo de um jogador (refeita quando muda o minuto, o placar ou sai um evento)
    double nota(Ator a) {
        String chave = minutoCorrido() + "/" + lista(chama(jogo, "hE")).size() + "/" + inteiro(chama(jogo, "hu")) + "/" + inteiro(chama(jogo, "hw"));
        if (!chave.equals(chaveNotas)) {
            chaveNotas = chave;
            notas.clear();
            Map<Object, Notas.Lances> ev = Notas.lances(jogo);
            long agora = System.currentTimeMillis();
            for (Ator b : atores.values()) {
                double n = Notas.nota(b.f, b.vaga, b.lado, jogo, Math.max(0, minutoCorrido()), ev);
                notas.put(b.f, n);
                double[] t = tendencia.get(b.f);
                // seta so quando a nota muda de uma vez (gol, cartao, defesa...), nao no desgaste minuto a minuto
                if (t == null) tendencia.put(b.f, new double[]{n, 0, 0});
                else {
                    if (Math.abs(n - t[0]) >= 0.25 - 1e-9) { t[2] = Math.signum(n - t[0]); t[1] = agora + 5000; }
                    t[0] = n;
                }
            }
        }
        Double n = notas.get(a.f);
        if (n == null) {
            n = Notas.nota(a.f, a.vaga, a.lado, jogo, Math.max(0, minutoCorrido()), Notas.lances(jogo));
            notas.put(a.f, n);
        }
        return n;
    }

    // avisa quando um jogador do seu time entra (ou passa a jogar) fora de posicao
    void confereFora(long agora) {
        foraAgora.clear();
        if (!Boolean.TRUE.equals(chama(timeHumano(), "jZ"))) return;
        ArrayList<Ator> novos = new ArrayList<Ator>();
        for (Ator a : atores.values()) {
            if (a.lado != humano || !Boolean.TRUE.equals(chama(a.f, "gF"))) continue;
            foraAgora.put(a, Boolean.TRUE);
            if (foraAvisados.put(a.f, Boolean.TRUE) == null) novos.add(a);
        }
        if (novos.isEmpty()) return;
        Collections.sort(novos, new Comparator<Ator>() {
            public int compare(Ator a, Ator b) { return ordem(a.vaga) - ordem(b.vaga); }
        });
        String min = Math.max(0, gt) + "'";
        for (int i = novos.size() - 1; i >= 0; i--) {
            Ator a = novos.get(i);
            lances.add(0, new Linha(min, "Fora de posição: " + nome(a.f) + " é " + Notas.natural(a.f) + " e está jogando de " + Notas.funcao(a.vaga)
                + ". Rende a metade da força.", 31, humano));
        }
        if (novos.size() == 1) {
            Ator a = novos.get(0);
            alerta = curto(nome(a.f)) + " fora de posição";
            alertaSub = Notas.natural(a.f) + " jogando de " + Notas.funcao(a.vaga) + " · rende a metade";
        } else {
            alerta = novos.size() + " jogadores fora de posição";
            alertaSub = "Rendem a metade da força · Espaço: substituições";
        }
        alertaDe = agora;
        alertaAte = agora + 6500;
    }

    void pintaAlerta(Graphics2D g, long agora) {
        double u = (agora - alertaDe) / (double) (alertaAte - alertaDe);
        float alfa = (float) Math.max(0, Math.min(1, Math.min(u / 0.06, (1 - u) / 0.12)));
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa));
        g.setFont(Estreia.semi(14f));
        FontMetrics f1 = g.getFontMetrics();
        Font sub = Estreia.texto(11.5f);
        FontMetrics f2 = g.getFontMetrics(sub);
        double w = Math.max(f1.stringWidth(alerta), f2.stringWidth(alertaSub)) + 70, h = 50;
        double x = campo.getCenterX() - w / 2, y = campo.y + 14 + (1 - Math.min(1, u / 0.06)) * -10;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h, 16, 16);
        g.setColor(new Color(10, 12, 11, 225));
        g.fill(r);
        g.setColor(new Color(242, 193, 78, 170));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(r);
        exclamacao(g, x + 28, y + h / 2, 11);
        g.setColor(Tema.TEXTO);
        g.drawString(alerta, (float) (x + 50), (float) (y + 21));
        g.setFont(sub);
        g.setColor(new Color(0xFFB4A8));
        g.drawString(alertaSub, (float) (x + 50), (float) (y + 39));
        g.setComposite(antes);
    }

    // bolinha dourada com "!" (fora de posicao)
    static void exclamacao(Graphics2D g, double cx, double cy, double r) {
        g.setColor(OURO);
        g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
        g.setColor(new Color(0x14110A));
        g.setStroke(new BasicStroke((float) Math.max(1.4, r * 0.26), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy - r * 0.5, cx, cy + r * 0.1));
        g.fill(new Ellipse2D.Double(cx - r * 0.15, cy + r * 0.36, r * 0.3, r * 0.3));
    }

    // ordem dos numeros das camisas: goleiro, defesa da direita para a esquerda, meio, ataque
    static int ordem(int vaga) {
        int papel = papel(vaga);
        return papel * 1000 + (1000 - VAGA[vaga][0]);
    }

    static int papel(int vaga) {
        if (vaga == 1) return GOLEIRO;
        if (vaga <= 9) return DEFESA;
        if (vaga <= 17) return MEIO;
        return ATAQUE;
    }

    void cores() {
        Color c0 = cor(casa, "kB", Color.WHITE), c1 = cor(fora, "kB", Color.BLACK);
        Color a0 = cor(casa, "kC", Color.BLACK), a1 = cor(fora, "kC", Color.WHITE);
        if (distancia(c0, c1) < 110 && distancia(a1, c0) > distancia(c1, c0)) { Color x = c1; c1 = a1; a1 = x; }
        if (distancia(c0, c1) < 60) { c1 = new Color(0xE8E4DA); a1 = new Color(0x1A1A1A); }
        cor = new Color[]{c0, c1};
        aro = new Color[]{a0, a1};
    }

    static Color cor(Object time, String m, Color reserva) {
        Object c = chama(time, m);
        return c instanceof Color ? (Color) c : reserva;
    }

    static double distancia(Color a, Color b) {
        int r = a.getRed() - b.getRed(), g = a.getGreen() - b.getGreen(), bl = a.getBlue() - b.getBlue();
        return Math.sqrt(r * r * 0.3 + g * g * 0.59 + bl * bl * 0.11) * 1.7;
    }

    static double brilho(Color c) { return (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) / 255.0; }

    // ------------------------------------------------------------------ os lances

    // a cada minuto: compara o que o motor contou antes e depois e monta o lance
    void minuto(long agora) {
        lePlanos();
        int[][] depois = estatisticas();
        List<?> ev = lista(chama(jogo, "hE"));
        ArrayList<Object> novos = new ArrayList<Object>();
        for (int i = eventosVistos; i < ev.size(); i++) novos.add(ev.get(i));
        eventosVistos = ev.size();
        sincroniza(agora);

        Object gol = null, perdido = null;
        for (Object a : novos) {
            int tipo = inteiro(chama(a, "b"));
            if (tipo == 1) gol = a;
            else if (tipo == 7) perdido = a;
            else if (tipo >= 2 && tipo <= 6) marca(a, tipo, agora);
        }
        int tipo = NADA, lado = posse;
        boolean achou = false;
        for (int s = 0; s < 2 && !achou; s++) {
            if (depois[0][s] > antes[0][s]) {
                lado = s;
                tipo = depois[1][s] > antes[1][s] ? DEFENDE : FORA;
                achou = true;
            }
        }
        for (int s = 0; s < 2 && !achou; s++) {
            if (depois[3][s] > antes[3][s]) { lado = 1 - s; tipo = DESARME; achou = true; }
            else if (depois[4][s] > antes[4][s]) { lado = s; tipo = ERRO; achou = true; }
        }
        if (gol != null) {
            tipo = GOL;
            lado = chama(gol, "cu") == fora ? 1 : 0;
        } else if (perdido != null) {
            tipo = PENALTI_PERDIDO;
            lado = chama(perdido, "cu") == fora ? 1 : 0;
        }
        antes = depois;
        int i = Math.max(0, Math.min(pressao.length - 1, (tempo - 1) * 52 + gt));
        int peso = tipo == GOL ? 3 : (tipo == DEFENDE || tipo == FORA || tipo == PENALTI_PERDIDO) ? 2 : (tipo == NADA ? 0 : 1);
        pressao[i] += (lado == 0 ? 1 : -1) * peso;
        int d = atraso();
        // 1 no ritmo de verdade (minuto de 300 ms ou mais) ate 0 na velocidade maxima (30 ms)
        ritmo = Math.max(0, Math.min(1, Math.log(d / 30.0) / Math.log(300 / 30.0)));
        acelera = 1 + (1 - ritmo) * 1.5;
        if (d < 60) {           // simulacao instantanea: so atualiza o fim
            posse = lado;
            return;
        }
        constroi(tipo, lado, gol != null ? gol : perdido, agora, d);
    }

    void constroi(int tipo, int lado, Object ev, long agora, int d) {
        boolean chance = chance(tipo);
        // o lance anterior ainda esta acontecendo: so as chances de gol interrompem; nas
        // velocidades rapidas (o relogio nao espera) so o gol interrompe
        if (lance != null && !lance.acabou && (!chance || (chance(lance.tipo) && tipo != GOL))) return;
        anuncia(Long.MAX_VALUE);     // o lance interrompido conta o que ja tinha para contar
        Lance l = new Lance();
        l.tipo = tipo;
        l.lado = lado;
        l.inicio = agora;
        l.dur = 1000;
        int adv = 1 - lado;
        int dz = ev == null ? 0 : inteiro(chama(ev, "el"));
        Ator d0 = dono != null && dono.lado == lado ? dono : perto(lado, bolaX, bolaY, -1, null, true);
        if (d0 == null) return;
        Ator ant = dono != null && dono.lado != lado && atores.containsValue(dono)
            && dist(dono.x, dono.y, bolaX, bolaY) < 0.05 ? dono : null;
        if (ant != null) {
            // a bola estava com o outro time: alguem chega e toma dele (nada de passe para o rival)
            l.chave(0, bolaX, bolaY, 0, ant);
            l.chave(0, bolaX + (ant.lado == 0 ? 0.03 : -0.03), bolaY, 0, d0).disputa = true;
        } else if (dist(d0.x, d0.y, bolaX, bolaY) > 0.03) {
            // bola solta: espera quem vai ficar com ela chegar, sem a bola andar sozinha
            l.chave(0, bolaX, bolaY, 0, null);
            l.chave(0, bolaX, bolaY, 0, d0);
        } else l.chave(0, bolaX, bolaY, 0, d0);
        // a partir daqui o lance continua de onde a bola fica depois da chegada/disputa
        final double bx = l.chaves.get(l.chaves.size() - 1).x, by = l.chaves.get(l.chaves.size() - 1).y;
        String min = gt + "'";
        if (tipo == GOL || tipo == DEFENDE || tipo == FORA || tipo == PENALTI_PERDIDO) {
            Ator chuta = null, serve = null;
            if (tipo == GOL && dz != 2) chuta = atores.get(chama(ev, "eo"));
            if (tipo == PENALTI_PERDIDO) chuta = atores.get(chama(ev, "eo"));
            if (tipo == GOL) serve = atores.get(chama(ev, "ep"));
            if (chuta == null || chuta.lado != lado) chuta = finalizador(lado);
            if (serve != null && (serve.lado != lado || serve == chuta)) serve = null;
            if (chuta == null) return;
            boolean penalti = dz == 3 || tipo == PENALTI_PERDIDO, falta = dz == 4, olimpico = dz == 5;
            double fimChute;
            if (penalti) {
                l.chave(0.30, frente(lado, 0.895), 0.5, 0, chuta, true);
                fimChute = 0.78;
            } else if (falta) {
                l.chave(0.30, frente(lado, 0.74 + sorte.nextDouble() * 0.04), 0.3 + sorte.nextDouble() * 0.4, 0, chuta, true);
                fimChute = 0.8;
            } else if (olimpico) {
                l.chave(0.30, frente(lado, 1.0), sorte.nextBoolean() ? 0.0 : 1.0, 0, chuta, true);
                fimChute = 0.82;
            } else {
                double p0 = progresso(lado, bx);
                Ator meia = null;
                if (p0 < 0.5) {
                    meia = perto(lado, frente(lado, 0.55), 0.3 + sorte.nextDouble() * 0.4, MEIO, chuta, false);
                    if (meia == d0 || meia == serve) meia = null;
                }
                if (meia != null) l.chave(0.26, frente(lado, Math.max(p0 + 0.08, 0.52 + sorte.nextDouble() * 0.08)), meia.y, 0.08, meia);
                Ator ultimo = serve != null ? serve : (sorte.nextInt(10) < 7 ? perto(lado, frente(lado, 0.8), sorte.nextBoolean() ? 0.2 : 0.8, -1, chuta, false) : null);
                if (ultimo != null && ultimo != chuta && ultimo.numero != 1 && papel(ultimo.vaga) != GOLEIRO) {
                    boolean ala = sorte.nextInt(10) < 5;
                    double ly = ala ? (ultimo.y < 0.5 ? 0.12 + sorte.nextDouble() * 0.12 : 0.76 + sorte.nextDouble() * 0.12) : 0.32 + sorte.nextDouble() * 0.36;
                    l.chave(0.52, frente(lado, 0.78 + sorte.nextDouble() * 0.07), ly, 0.1, ultimo);
                    double cy = 0.38 + sorte.nextDouble() * 0.24;
                    l.chave(0.72, frente(lado, 0.85 + sorte.nextDouble() * 0.05), cy, ala ? 0.5 : 0.08, chuta);
                } else {
                    l.chave(0.62, frente(lado, 0.82 + sorte.nextDouble() * 0.06), 0.36 + sorte.nextDouble() * 0.28, 0, chuta);
                }
                fimChute = 0.86;
            }
            Ator goleiro = goleiro(adv);
            if (tipo == GOL) {
                l.chave(fimChute, frente(lado, 1.018), 0.5 + (sorte.nextDouble() - 0.5) * 0.085, olimpico ? 0.7 : 0.25, null).chute = true;
                final Chave rede = l.chaves.get(l.chaves.size() - 1);
                cronometra(l);
                final Ator autor = dz == 2 ? atores.get(chama(ev, "eo")) : chuta;
                final int ladoGol = lado;
                final String quem = autor == null ? nome(chama(ev, "eo")) : nome(autor.f);
                golDe = agora + (long) (l.dur * rede.t);
                golAte = golDe + 2300;
                golNome = quem;
                golTime = time(lado);
                golLado = lado;
                golAutor = autor != null && autor.lado == lado ? autor : chuta;
                String texto;
                if (dz == 2) texto = "Gol contra de " + quem + ", a favor do " + time(lado) + ".";
                else if (dz == 3) texto = quem + " bate o pênalti e marca para o " + time(lado) + ".";
                else if (dz == 4) texto = quem + " cobra a falta direto no gol!";
                else if (dz == 5) texto = "Gol olímpico de " + quem + "!";
                else if (serve != null) texto = nome(serve.f) + " serve e " + quem + " marca para o " + time(lado) + ".";
                else texto = quem + " marca para o " + time(lado) + ".";
                l.linha = new Linha(min, "GOL! " + texto, GOL, lado);
                l.texto = texto;
                l.marco = rede;
                l.depois = new Runnable() {
                    public void run() {
                        bolaX = 0.5; bolaY = 0.5; bolaH = 0;
        refX = 0.5; refY = 0.5;
                        dono = saida(1 - ladoGol);
                        posse = 1 - ladoGol;
                    }
                };
                long extra = 0;
                // penalti ja foi mostrado na cena 3D da cobranca (mod.Penaltis): sem replay
                if (d >= 200 && dz != 3 && isShowing() && verCampo && transmissao.pronto()) {
                    transmissao.agendaReplay(golDe, golAte, lado);
                    extra = Transmissao.REPLAY_MS;
                }
                pausa(agora, (l.dur + 2300) * Math.max(ritmo, 0.5) + extra, d);
            } else if (tipo == DEFENDE || tipo == PENALTI_PERDIDO && chama(ev, "ep") != null) {
                double gy = goleiro != null ? Math.max(0.42, Math.min(0.58, goleiro.y)) : 0.5;
                l.marco = l.chave(fimChute, frente(lado, 0.968), gy + (sorte.nextDouble() - 0.5) * 0.04, 0.2, goleiro);
                l.marco.chute = true;
                String g = goleiro != null ? nome(goleiro.f) : "o goleiro";
                String texto = tipo == PENALTI_PERDIDO ? g + " defende o pênalti de " + nome(chuta.f) + "!"
                    : escolhe(nome(chuta.f) + " finaliza e " + g + " defende.",
                              nome(chuta.f) + " chuta forte, " + g + " espalma.",
                              "Boa chance para " + nome(chuta.f) + ", mas " + g + " segura.");
                l.linha = new Linha(min, texto, DEFENDE, lado);
                l.texto = texto;
            } else {
                boolean porCima = sorte.nextBoolean();
                double fy = porCima ? 0.5 + (sorte.nextDouble() - 0.5) * 0.06 : (sorte.nextBoolean() ? 0.36 - sorte.nextDouble() * 0.08 : 0.64 + sorte.nextDouble() * 0.08);
                l.marco = l.chave(fimChute, frente(lado, 1.035), fy, porCima ? 0.9 : 0.3, null);
                l.marco.chute = true;
                l.chave(1.0, frente(lado, 0.955), 0.5 + (sorte.nextDouble() - 0.5) * 0.2, 0, goleiro, true);
                String texto = tipo == PENALTI_PERDIDO ? nome(chuta.f) + " perde o pênalti: a bola vai para fora!"
                    : porCima ? escolhe(nome(chuta.f) + " arrisca e manda por cima do gol.", nome(chuta.f) + " cabeceia por cima.")
                              : escolhe(nome(chuta.f) + " finaliza para fora.", nome(chuta.f) + " bate cruzado e a bola sai rente à trave.");
                l.linha = new Linha(min, texto, FORA, lado);
                l.texto = texto;
            }
        } else if (tipo == DESARME) {
            double p0 = progresso(lado, bx);
            double alvo = Math.max(0.3, Math.min(0.8, p0 + 0.1 + sorte.nextDouble() * 0.14));
            double ay = Math.max(0.1, Math.min(0.9, by + (sorte.nextDouble() - 0.5) * 0.4));
            Ator recebe = perto(lado, frente(lado, alvo + 0.04), ay, -1, d0, false);
            if (recebe == null) recebe = d0;
            l.chave(0.42, frente(lado, alvo), ay, 0.04, recebe);
            Ator ladrao = perto(adv, frente(lado, alvo), ay, -1, null, false);
            l.marco = l.chave(0.74, frente(lado, alvo + 0.03), ay + (sorte.nextDouble() - 0.5) * 0.04, 0, ladrao);
            l.marco.disputa = true;
            l.texto = escolhe(nome(ladrao.f) + " desarma " + nome(recebe.f) + ".",
                              "Boa roubada de " + nome(ladrao.f) + ".",
                              nome(ladrao.f) + " chega firme e fica com a bola.");
        } else if (tipo == ERRO) {
            double p0 = progresso(lado, bx);
            if (sorte.nextInt(10) < 3) {
                double sy = by < 0.5 ? -0.03 : 1.03;
                double sx = frente(lado, Math.min(0.92, p0 + 0.12 + sorte.nextDouble() * 0.1));
                l.marco = l.chave(0.6, sx, sy, 0.15, null);
                Ator cobra = perto(adv, sx, sy, -1, null, false);
                l.chave(1.0, sx, sy < 0 ? 0.0 : 1.0, 0, cobra, true);
                l.texto = escolhe("Passe longo demais do " + time(lado) + ": lateral para o adversário.", nome(d0.f) + " erra o passe e a bola sai pela lateral.");
            } else {
                double alvo = Math.min(0.85, p0 + 0.15 + sorte.nextDouble() * 0.12);
                double ay = Math.max(0.08, Math.min(0.92, by + (sorte.nextDouble() - 0.5) * 0.5));
                // o passe vai na direcao de um companheiro e o adversario corta no meio do caminho
                Ator mira = perto(lado, frente(lado, alvo), ay, -1, d0, false);
                double tx = mira != null ? mira.x : frente(lado, alvo), ty = mira != null ? mira.y : ay;
                double mx = bx + (tx - bx) * 0.55, my = by + (ty - by) * 0.55;
                Ator corta = perto(adv, mx, my, -1, null, false);
                l.marco = l.chave(0.55, mx, my, 0.06, corta);
                l.texto = escolhe(nome(d0.f) + " erra o passe, " + nome(corta.f) + " intercepta.", "Passe errado do " + time(lado) + ".");
            }
        } else {
            Ator para = perto(lado, bx + (lado == 0 ? -0.04 : 0.04), by + (sorte.nextDouble() - 0.5) * 0.3, -1, d0, false);
            if (para != null && dist(para.x, para.y, bx, by) < 0.35) l.chave(0.6, para.x, para.y, 0.02, para);
            l.texto = escolhe(time(lado) + " troca passes.", time(lado) + " tem a bola.", time(lado) + " gira a bola no campo.");
        }
        if (tipo != GOL) {
            cronometra(l);
            // chance de gol: o relogio do jogo espera o lance terminar, no ritmo de verdade
            if (chance) pausa(agora, (l.dur + 700) * ritmo, d);
        }
        if (l.texto != null) {
            Anuncio an = new Anuncio();
            an.em = l.marco == null ? agora : agora + (long) (l.dur * l.marco.t);
            an.linha = l.linha;
            an.texto = l.texto;
            an.min = gt;
            anuncios.add(an);
        }
        Chave ultima = l.chaves.get(l.chaves.size() - 1);
        if (ultima.dono != null) posse = ultima.dono.lado;
        lance = l;
    }

    // ritmo de verdade, em metros por segundo
    static final double TROTE = 4.2, CORRIDA = 6.3, CONDUZ = 5.6, SPRINT = 8.2, ACELERA = 7.5;
    static final double PASSE_CURTO = 17, PASSE_LONGO = 24, CHUTE = 28;

    static String ultimoCrono = "";
    public static String crono() { return ultimoCrono; }

    static double metros(double ax, double ay, double bx, double by) {
        return Math.hypot((ax - bx) * Transmissao.CX, (ay - by) * Transmissao.CZ);
    }

    // da a cada ponto do lance o tempo que ele levaria de verdade: a bola anda na velocidade
    // de um passe, de uma conducao ou de um chute, e quem recebe precisa ter tempo de chegar
    void cronometra(Lance l) {
        ArrayList<Chave> k = l.chaves;
        double[] tt = new double[k.size()];
        StringBuilder dbg = new StringBuilder(l.tipo + ":");
        for (int i = 1; i < k.size(); i++) {
            Chave a = k.get(i - 1), b = k.get(i);
            double dm = metros(a.x, a.y, b.x, b.y), seg;
            if (b.salto) seg = 1.5 + dm / 30;                               // bola parada: arruma e cobra
            else if (b.chute) seg = 0.35 + dm / CHUTE;                      // ajeita e chuta
            else if (b.disputa || a.dono != null && a.dono == b.dono) seg = dm / CONDUZ; // conduz
            else seg = 0.3 + dm / (dm > 28 ? PASSE_LONGO : PASSE_CURTO);    // domina e passa
            double t = tt[i - 1] + seg;
            if (b.dono != null && !b.chute && !b.salto && atores.containsValue(b.dono)) {
                // quem recebe esta longe: o passe vai um pouco mais na direcao dele (ate 40%)
                double chega = metros(b.dono.x, b.dono.y, b.x, b.y) / SPRINT + 0.7;   // + arrancada
                if (chega > t + 0.6) {
                    double m = Math.min(0.4, 1 - (t + 0.6) / chega);
                    b.x += (b.dono.x - b.x) * m;
                    b.y += (b.dono.y - b.y) * m;
                    dm = metros(a.x, a.y, b.x, b.y);
                    t = tt[i - 1] + (b.disputa || a.dono != null && a.dono == b.dono ? dm / CONDUZ : 0.3 + dm / (dm > 28 ? PASSE_LONGO : PASSE_CURTO));
                    chega = metros(b.dono.x, b.dono.y, b.x, b.y) / SPRINT + 0.7;
                }
                t = Math.max(t, chega);
            }
            tt[i] = t;
            dbg.append(String.format(" %s%.1fm/%.2fs", b.chute ? "C" : b.salto ? "S" : a.dono == b.dono ? "c" : "p", dm, t - tt[i - 1]));
        }
        ultimoCrono = dbg.toString();
        double total = Math.max(0.6, tt[k.size() - 1]);
        l.dur = total * 1000 / acelera;
        for (int i = 0; i < k.size(); i++) k.get(i).t = tt[i] / total;
    }

    // mostra a linha e a legenda dos lances que ja aconteceram na cena
    void anuncia(long agora) {
        for (Iterator<Anuncio> it = anuncios.iterator(); it.hasNext();) {
            Anuncio an = it.next();
            if (an.em > agora) continue;
            it.remove();
            if (an.linha != null) lances.add(0, an.linha);
            if (an.linha != null && an.linha.tipo == GOL) dv.repaint();   // o placar de cima conta o gol agora
            legenda = an.texto;
            legendaMin = an.min;
        }
    }

    // o placar de cima espera a bola entrar na cena para contar o gol deste lado
    public static boolean segura(int lado) {
        Partida p = atual;
        return p != null && p.jogo != null && p.lance != null && p.lance.tipo == GOL && p.golLado == lado
            && System.currentTimeMillis() < p.golDe;
    }

    static boolean chance(int tipo) { return tipo == GOL || tipo == DEFENDE || tipo == FORA || tipo == PENALTI_PERDIDO; }

    String escolhe(String... op) { return op[sorte.nextInt(op.length)]; }

    void marca(Object ev, int tipo, long agora) {
        Ator a = atores.get(chama(ev, "eo"));
        String quem = nome(chama(ev, "eo"));
        Object t = chama(ev, "cu");
        int lado = t == fora ? 1 : 0;
        String min = inteiro(chama(ev, "em")) == 0 && inteiro(chama(ev, "en")) == 2 ? "Int." : inteiro(chama(ev, "em")) + "'";
        String texto;
        if (tipo == 2) texto = "Cartão amarelo para " + quem + ".";
        else if (tipo == 3) texto = "Segundo amarelo: " + quem + " está expulso.";
        else if (tipo == 4) texto = "Cartão vermelho direto para " + quem + "!";
        else if (tipo == 5) texto = quem + " se machuca e precisa de atendimento.";
        else {
            Object entra = chama(ev, "ep");
            if (entra == null) return;
            texto = "Substituição no " + time(lado) + ": sai " + quem + ", entra " + nome(entra) + ".";
            sincroniza(agora);
            Ator novo = atores.get(entra);
            if (novo != null) marcas.add(new Marca(novo, 6, agora + 3500));
        }
        lances.add(0, new Linha(min, texto, 10 + tipo, lado));
        if (a != null && tipo != 6) marcas.add(new Marca(a, tipo, agora + 3500));
    }

    void segundoTempo(long agora) {
        anuncia(Long.MAX_VALUE);
        lances.add(0, new Linha("Int.", "Começa o segundo tempo.", 20, 0));
        bolaX = 0.5; bolaY = 0.5; bolaH = 0;
        refX = 0.5; refY = 0.5;
        lance = null;
        posse = 1;
        dono = saida(1);
        aviso = "Segundo tempo";
        avisoAte = agora + 1600;
    }

    void termina(long agora) {
        anuncia(Long.MAX_VALUE);
        fim = true;
        Object g0 = chama(jogo, "hu"), g1 = chama(jogo, "hw");
        lances.add(0, new Linha("Fim", "Fim de jogo: " + time(0) + " " + inteiro(g0) + " x " + inteiro(g1) + " " + time(1) + ".", 20, 0));
        legenda = "Fim de jogo.";
        aviso = "Fim de jogo";
        avisoAte = Long.MAX_VALUE;
    }

    // gol: segura o relogio do jogo para a comemoracao aparecer
    void pausa(long agora, double ms, int d) {
        try {
            Timer go = (Timer) campo(dv.getClass(), dv, "GO");
            if (go == null || !isShowing() || ms <= d) return;
            go.stop();
            pausado = true;
            retomaEm = agora + (long) Math.max(0, ms - d);
        } catch (Throwable t) { t.printStackTrace(); }
    }

    void retoma() {
        if (!pausado) return;
        pausado = false;
        try {
            Timer go = (Timer) campo(dv.getClass(), dv, "GO");
            if (go != null && !go.isRunning() && dv.isDisplayable()) go.restart();
        } catch (Throwable t) { t.printStackTrace(); }
    }

    // para os testes: plano que o motor esta usando em cada lado
    public static String planos() {
        Partida p = atual;
        if (p == null || p.jogo == null) return "sem";
        c.b mt = p.motor();
        if (mt == null) return "sem motor";
        return "casa " + PainelOrdens.resumo(mt.plano(0)) + " | fora " + PainelOrdens.resumo(mt.plano(1)) + " | humano " + p.humano + " visto " + p.mentalidade[0] + "/" + p.mentalidade[1];
    }

    // para os testes: quantos jogadores estao fora das linhas sem estar comemorando ou entrando
    public static String fora() {
        Partida p = atual;
        if (p == null) return "sem";
        int n = 0;
        for (Ator a : p.atores.values()) if (!a.solto && (a.x < -0.001 || a.x > 1.001 || a.y < -0.001 || a.y > 1.001)) n++;
        return n + " ritmo " + String.format("%.2f", p.ritmo) + " acelera " + String.format("%.2f", p.acelera);
    }

    // para os testes: em que momento a partida esta
    public static String estado() {
        Partida p = atual;
        if (p == null || p.jogo == null) return "sem";
        long agora = System.currentTimeMillis();
        if (p.fim) return "fim";
        if (p.transmissao.emReplay(agora)) return "replay " + (agora - p.transmissao.replayDe) + " " + Transmissao.ms + "ms";
        if (agora >= p.golDe && agora < p.golAte) return "gol " + (agora - p.golDe);
        if (p.lance != null) return (p.lance.acabou ? "lance " : "andando ") + p.lance.tipo + " " + p.gt + " " + Transmissao.ms + "ms dur " + (int) p.lance.dur + " u " + String.format("%.2f", (agora - p.lance.inicio) / p.lance.dur) + (p.pausado ? " pausado" : "");
        return "jogo " + p.gt + " " + Transmissao.ms + "ms";
    }

    // ------------------------------------------------------------------ jogadores

    static double frente(int lado, double v) { return lado == 0 ? v : 1 - v; }

    static double progresso(int lado, double x) { return lado == 0 ? x : 1 - x; }

    static double dist(double ax, double ay, double bx, double by) { return Math.hypot(ax - bx, ay - by); }

    Ator perto(int lado, double x, double y, int papel, Ator exceto, boolean comGoleiro) {
        Ator melhor = null;
        double md = 9;
        for (Ator a : atores.values()) {
            if (a.lado != lado || a == exceto) continue;
            int p = papel(a.vaga);
            if (p == GOLEIRO && !comGoleiro && papel != GOLEIRO) continue;
            double dd = dist(a.x, a.y, x, y) + (papel >= 0 && p != papel ? 0.25 : 0);
            if (dd < md) { md = dd; melhor = a; }
        }
        if (melhor == null && exceto != null && exceto.lado == lado) return exceto;
        return melhor;
    }

    Ator goleiro(int lado) {
        for (Ator a : atores.values()) if (a.lado == lado && a.vaga == 1) return a;
        return null;
    }

    Ator finalizador(int lado) {
        ArrayList<Ator> l = new ArrayList<Ator>();
        for (Ator a : atores.values()) {
            if (a.lado != lado) continue;
            int p = papel(a.vaga);
            double perto = 1.2 - dist(a.x, a.y, frente(lado, 0.86), 0.5);
            int peso = (int) Math.round((p == ATAQUE ? 6 : p == MEIO ? 3 : p == DEFESA ? 1 : 0) * Math.max(0.1, perto) * 3);
            for (int i = 0; i < peso; i++) l.add(a);
        }
        return l.isEmpty() ? null : l.get(sorte.nextInt(l.size()));
    }

    Ator saida(int lado) {
        Ator a = perto(lado, 0.5, 0.5, ATAQUE, null, false);
        if (a == null) a = perto(lado, 0.5, 0.5, -1, null, false);
        return a;
    }

    // entra/sai do campo conforme as listas do jogo (substituicoes, expulsoes)
    void sincroniza(long agora) {
        for (int lado = 0; lado < 2; lado++) {
            for (Object f : lista(chama(jogo, lado == 0 ? "hp" : "hq"))) {
                int vaga = inteiro(chama(f, "fT"));
                if (vaga < 1 || vaga > 25) continue;
                Ator a = atores.get(f);
                if (a == null) {
                    a = new Ator();
                    a.f = f; a.lado = lado;
                    a.numero = proximoNumero[lado]++;
                    a.x = 0.5; a.y = 1.04;
                    atores.put(f, a);
                }
                a.vaga = vaga;
                a.visto = agora;
            }
        }
        for (Iterator<Ator> it = atores.values().iterator(); it.hasNext();) {
            Ator a = it.next();
            if (a.visto != agora) {
                it.remove();
                if (dono == a) dono = null;
            }
        }
    }

    // onde o jogador estaria pela formacao: o time anda em bloco atras da bola, com as linhas
    // de defesa, meio e ataque do Escalar; estica e abre com a bola, encurta e fecha sem ela
    double[] base(Ator a, double t) {
        int vaga = a.vaga < 1 || a.vaga > 25 ? 12 : a.vaga;
        double prof = (565 - VAGA[vaga][1]) / 535.0;
        double lat = (VAGA[vaga][0] - 35) / 310.0;
        double perto = progresso(a.lado, refX);
        double by = a.lado == 0 ? refY : 1 - refY;          // bola no lado do time
        boolean comBola = posse == a.lado;
        double x, y;
        if (vaga == 1) {
            x = 0.025 + Math.max(0, Math.min(0.04, (perto - 0.5) * 0.08));
            y = 0.5 + (by - 0.5) * 0.22;
        } else {
            // mentalidade: o time inteiro sobe ou recua; pressao alta adianta a marcacao sem a bola
            double linha = 0.10 + (perto - 0.3) * 0.62 + (comBola ? 0.04 : -0.02)
                + mentalidade[a.lado] * 0.028 + (comBola ? 0 : (pressaoTime[a.lado] - 1) * 0.03);
            linha = Math.max(0.07, Math.min(0.52, linha));
            x = linha + prof * (comBola ? 0.42 : 0.33);
            double d = Math.abs(lat - 0.5), f = d < 0.3 ? d * 0.62 : 0.186 + (d - 0.3) * 1.27;
            f *= comBola ? 0.95 : 0.72;
            y = 0.5 + (lat < 0.5 ? -f : f) + (by - 0.5) * (comBola ? 0.2 : 0.3);
        }
        x += 0.003 * Math.sin(t * 0.9 + a.fase);
        y += 0.004 * Math.sin(t * 0.7 + a.fase * 1.3);
        x = Math.max(0.02, Math.min(0.97, x));
        y = Math.max(0.04, Math.min(0.96, y));
        if (a.lado == 1) { x = 1 - x; y = 1 - y; }
        return new double[]{x, y};
    }

    void anima(long agora, double dt) {
        double t = agora / 1000.0;
        posicionaBola(agora);
        // nas velocidades rapidas a cena anda acelerada (como um avancar), sem mudar o jeito
        dt *= acelera;
        double kr = 1 - Math.exp(-dt / 0.7);
        refX += (bolaX - refX) * kr;
        refY += (bolaY - refY) * kr;
        // quem marca: o mais perto da bola do time que nao esta com ela
        Ator pressiona = null;
        int semBola = dono != null ? 1 - dono.lado : (receptor != null ? 1 - receptor.lado : -1);
        if (semBola >= 0) pressiona = perto(semBola, bolaX, bolaY, -1, null, false);
        double u = lance == null ? 1 : (agora - lance.inicio) / lance.dur;
        for (Ator a : atores.values()) {
            double[] alvo = base(a, t);
            double vmax = TROTE, ganho = 1.4;                  // m/s e 1/s
            boolean solto = false;                             // comemorando ou entrando: pode sair das linhas
            Chave c = a == dono && conduz ? null : proxima(a, u);
            if (c != null) {
                // vai para onde vai receber a bola, chegando na hora certa
                alvo[0] = c.x; alvo[1] = c.y;
                double resta = Math.max(0.15, (c.t - u) * lance.dur / 1000.0 * acelera);
                vmax = Math.max(2.5, Math.min(SPRINT, metros(a.x, a.y, c.x, c.y) / resta * 1.1));
                ganho = 3;
            } else if (agora > golDe && agora < golAte && lance != null && lance.acabou && a.lado == golLado && golAutor != null
                       && atores.containsValue(golAutor)) {
                // comemoracao: o autor corre para a bandeirinha e os companheiros vao atras
                double cy = golAutor.y < 0.5 ? 0.05 : 0.95;
                solto = true;
                if (a == golAutor) { alvo[0] = frente(golLado, 0.95); alvo[1] = cy; vmax = 7; ganho = 3; }
                else if (a.vaga != 1 && dist(a.x, a.y, golAutor.x, golAutor.y) < 0.45) {
                    alvo[0] = golAutor.x - (golLado == 0 ? 0.03 : -0.03) * (1 + a.numero % 3);
                    alvo[1] = golAutor.y + ((a.numero % 5) - 2) * 0.025;
                    vmax = 6.5; ganho = 2.5;
                }
            } else if (a == dono) {
                // conduzindo: corre pelo caminho do lance e a bola vai colada no pe (ver pe());
                // parado: fica logo atras da bola
                double tras = conduz ? 0 : (a.lado == 0 ? -0.006 : 0.006);
                alvo[0] = bolaX + tras; alvo[1] = bolaY;
                vmax = SPRINT; ganho = conduz ? 6 : 4;
            } else if (a == pressiona) {
                double tras = a.lado == 0 ? -0.03 : 0.03;
                alvo[0] = bolaX + tras; alvo[1] = bolaY + (a.y < bolaY ? -0.02 : 0.02);
                vmax = CORRIDA; ganho = 2;
            }
            if (a.y > 1.0 || a.y < 0.0) { vmax = Math.max(vmax, CORRIDA); solto = true; }   // entrando em campo
            if (!solto) {
                // dentro das linhas (a cobranca de lateral fica em cima da linha)
                alvo[0] = Math.max(0.004, Math.min(0.996, alvo[0]));
                alvo[1] = Math.max(c != null ? 0.0 : 0.012, Math.min(c != null ? 1.0 : 0.988, alvo[1]));
            }
            if (dist(a.x, a.y, alvo[0], alvo[1]) > 0.2) vmax = Math.max(vmax, CORRIDA);   // voltando para a posicao
            // velocidade desejada em metros, com limite e aceleracao de gente
            double qx = (alvo[0] - a.x) * Transmissao.CX * ganho, qy = (alvo[1] - a.y) * Transmissao.CZ * ganho;
            double v = Math.hypot(qx, qy);
            if (v > vmax) { qx *= vmax / v; qy *= vmax / v; }
            double ux = a.vx * Transmissao.CX, uy = a.vy * Transmissao.CZ;
            double ax = qx - ux, ay = qy - uy, am = Math.hypot(ax, ay), lim = ACELERA * dt;
            if (am > lim) { ax *= lim / am; ay *= lim / am; }
            ux += ax; uy += ay;
            a.vx = ux / Transmissao.CX;
            a.vy = uy / Transmissao.CZ;
            a.x += a.vx * dt;
            a.y += a.vy * dt;
            // passada: mais curta e rapida no trote, mais longa no pique
            double vm = Math.hypot(ux, uy);
            a.passo += vm * dt * Math.max(1.9, 3.4 - 0.17 * vm);
            a.solto = solto;
        }
        afasta();
        pe(agora);
        for (Ator a : atores.values()) {
            if (a.solto) continue;
            if (a.x < 0) { a.x = 0; a.vx = Math.max(0, a.vx); }
            if (a.x > 1) { a.x = 1; a.vx = Math.min(0, a.vx); }
            if (a.y < 0) { a.y = 0; a.vy = Math.max(0, a.vy); }
            if (a.y > 1) { a.y = 1; a.vy = Math.min(0, a.vy); }
        }
        for (Iterator<Marca> it = marcas.iterator(); it.hasNext();) if (it.next().ate < agora) it.remove();
    }

    // conducao: a bola fica meio metro a frente do pe de quem conduz, na direcao em que ele corre
    double peX = -1, peY;
    long peDesde;
    void pe(long agora) {
        if (!conduz || dono == null) { peX = -1; return; }
        if (peX < 0) peDesde = agora;
        // logo depois de dominar, a bola vai do ponto do passe para o pe em 0,25 s
        double w = Math.min(1, (agora - peDesde) / 250.0);
        double vx = dono.vx * Transmissao.CX, vy = dono.vy * Transmissao.CZ, v = Math.hypot(vx, vy);
        double dx = v > 0.8 ? vx / v : (dono.lado == 0 ? 1 : -1), dy = v > 0.8 ? vy / v : 0;
        bolaX += (dono.x + dx * 0.55 / Transmissao.CX - bolaX) * w;
        bolaY += (dono.y + dy * 0.55 / Transmissao.CZ - bolaY) * w;
        peX = bolaX; peY = bolaY;
    }

    // proximo ponto do lance em que este jogador recebe a bola
    Chave proxima(Ator a, double u) {
        if (lance == null || lance.acabou) return null;
        for (Chave c : lance.chaves) if (c.dono == a && c.t > u && !c.salto) return c;
        return null;
    }

    // dois jogadores nao ocupam o mesmo lugar
    void afasta() {
        double min = 0.024;
        ArrayList<Ator> l = new ArrayList<Ator>(atores.values());
        for (int i = 0; i < l.size(); i++) {
            for (int j = i + 1; j < l.size(); j++) {
                Ator a = l.get(i), b = l.get(j);
                double dx = b.x - a.x, dy = (b.y - a.y) * 0.648, d = Math.hypot(dx, dy);
                if (d >= min || d < 1e-6) continue;
                double emp = (min - d) * 0.25 / d;
                boolean fixoA = a == dono || a == receptor, fixoB = b == dono || b == receptor;
                if (!fixoA) { a.x -= dx * emp; a.y -= dy * emp / 0.648; }
                if (!fixoB) { b.x += dx * emp; b.y += dy * emp / 0.648; }
            }
        }
    }

    Lance trechoDe;
    int trecho;

    void posicionaBola(long agora) {
        receptor = null;
        conduz = false;
        bolaVisivel = true;
        if (lance == null) { bolaH = 0; return; }
        ArrayList<Chave> k = lance.chaves;
        double u = (agora - lance.inicio) / lance.dur;
        Chave ultima = k.get(k.size() - 1);
        if (k.size() == 1 || u >= ultima.t) {
            if (!lance.acabou && peX >= 0) { ultima.x = peX; ultima.y = peY; }
            bolaX = ultima.x; bolaY = ultima.y; bolaH = 0;
            if (!lance.acabou) {
                lance.acabou = true;
                dono = ultima.dono;
                if (lance.depois != null && lance.tipo != GOL) lance.depois.run();
            }
            if (lance.tipo == GOL && lance.depois != null && agora > golAte - 300) {
                Runnable r = lance.depois;
                lance.depois = null;
                r.run();
            }
            return;
        }
        int i = 0;
        while (i + 1 < k.size() && k.get(i + 1).t <= u) i++;
        // a conducao acabou: o proximo trecho sai de onde a bola esta no pe, sem pulo
        if (lance != trechoDe) { trechoDe = lance; trecho = i; }
        else if (i != trecho) {
            if (peX >= 0) { k.get(i).x = peX; k.get(i).y = peY; }
            trecho = i;
        }
        Chave a = k.get(i), b = k.get(i + 1);
        double s = Math.max(0, Math.min(1, (u - a.t) / Math.max(1e-6, b.t - a.t)));
        if (b.salto) {
            // bola parada (penalti, falta, tiro de meta): some e reaparece no lugar da cobranca
            bolaVisivel = s > 0.55;
            Chave c = s > 0.55 ? b : a;
            bolaX = c.x; bolaY = c.y; bolaH = 0;
            dono = c.dono;
            if (s > 0.55) receptor = null; else receptor = b.dono;
            return;
        }
        boolean passe = !b.disputa && (a.dono != b.dono || b.dono == null);
        double e = passe ? 1 - (1 - s) * (1 - s) : s;
        bolaX = a.x + (b.x - a.x) * e;
        bolaY = a.y + (b.y - a.y) * e;
        bolaH = b.h * 4 * s * (1 - s);
        if (passe) {
            dono = null;
            receptor = b.dono;
            rastroX0 = a.x; rastroY0 = a.y;
            rastroAte = agora + 450;
        } else {
            dono = a.dono;
            conduz = true;
        }
    }

    // ------------------------------------------------------------------ pintura

    static final int COLUNA = 316;

    protected void paintComponent(Graphics g0) {
        if (jogo == null) return;
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        long agora = System.currentTimeMillis();
        int W = getWidth(), H = getHeight();
        int col = W >= 860 ? COLUNA : 0;
        double areaX = col, areaW = W - col;
        double legendaH = 36, pressaoH = 52, feedMin = Math.min(150, H * 0.2);
        double fw = Math.min(areaW / 1.07, (H - legendaH - pressaoH - feedMin - 16) / (0.648 * 1.1));
        fw = Math.max(200, fw);
        double pw = fw * 1.07, ph = fw * 0.648 * 1.1;
        double px = areaX + (areaW - pw) / 2;
        Shape clip = g.getClip();
        if (transmissao.pronto()) {
            // 3D: estadio com camera isometrica (e replay de TV depois dos gols)
            campo.setRect(px, 0, pw, ph);
            g.clip(new RoundRectangle2D.Double(px, 0, pw, ph, 18, 18));
            transmissao.pinta(g, new Rectangle2D.Double(px, 0, pw, ph), agora);
            if (agora >= golDe && agora < golAte) pintaGol(g, agora);
            if (aviso != null && agora < avisoAte) pintaAviso(g, aviso, agora);
            if (agora < alertaAte) pintaAlerta(g, agora);
            g.setClip(clip);
            g.setColor(new Color(242, 193, 78, 60));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Double(px + 0.6, 0.6, pw - 1.2, ph - 1.2, 18, 18));
        } else {
            campo.setRect(px + fw * 0.035, fw * 0.648 * 0.05, fw, fw * 0.648);
            pintaGramado(g, (int) Math.round(px), 0, (int) Math.round(pw), (int) Math.round(ph));
            g.clip(new RoundRectangle2D.Double(px, 0, pw, ph, 18, 18));
            pintaJogo(g, agora);
            g.setClip(clip);
        }
        double y = ph + 8;
        pintaLegenda(g, px, y, pw, legendaH);
        y += legendaH + 8;
        pintaPressao(g, px, y, pw, pressaoH);
        y += pressaoH + 8;
        pintaLances(g, px, y, pw, H - y);
        if (col > 0) pintaTime(g, 0, 0, col - 14, H);
        if (ordens != null) ordens.pinta(g, px, 0, pw, Math.max(ph, Math.min(H, 540)));
        g.dispose();
    }

    double sx(double x) { return campo.x + x * campo.width; }

    double sy(double y) { return campo.y + y * campo.height; }

    void pintaGramado(Graphics2D g, int x, int y, int w, int h) {
        if (gramado == null || gramado.getWidth() != w || gramado.getHeight() != h) {
            gramado = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
            Graphics2D b = gramado.createGraphics();
            b.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            b.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            RoundRectangle2D.Double forma = new RoundRectangle2D.Double(0, 0, w, h, 18, 18);
            b.setClip(forma);
            b.setColor(GRAMA_B);
            b.fillRect(0, 0, w, h);
            double ox = campo.x - x, oy = campo.y - y, fw = campo.width, fh = campo.height;
            int faixas = 14;
            for (int i = 0; i < faixas; i++) {
                b.setColor(i % 2 == 0 ? GRAMA_A : GRAMA_B);
                double a = ox + fw * i / faixas;
                b.fill(new Rectangle2D.Double(a, 0, fw / faixas + 0.5, h));
            }
            // textura: pontinhos claros e escuros, fixos
            Random r = new Random(7);
            for (int i = 0; i < w * h / 26; i++) {
                b.setColor(r.nextBoolean() ? new Color(255, 255, 255, 7) : new Color(0, 0, 0, 12));
                b.fillRect(r.nextInt(w), r.nextInt(h), 1 + r.nextInt(2), 1);
            }
            // luz dos refletores no meio e cantos escuros
            b.setPaint(new RadialGradientPaint((float) (w / 2.0), (float) (h * 0.35), (float) (w * 0.62),
                new float[]{0f, 0.6f, 1f}, new Color[]{new Color(255, 250, 220, 30), new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
            b.fillRect(0, 0, w, h);
            // linhas oficiais (105 x 68 m)
            double m = fw / 105.0;
            b.setColor(LINHA);
            b.setStroke(new BasicStroke((float) Math.max(1.4, fw / 380.0)));
            b.draw(new Rectangle2D.Double(ox, oy, fw, fh));
            b.draw(new Line2D.Double(ox + fw / 2, oy, ox + fw / 2, oy + fh));
            b.draw(new Ellipse2D.Double(ox + fw / 2 - 9.15 * m, oy + fh / 2 - 9.15 * m, 18.3 * m, 18.3 * m));
            b.fill(new Ellipse2D.Double(ox + fw / 2 - 0.45 * m, oy + fh / 2 - 0.45 * m, 0.9 * m, 0.9 * m));
            for (int lado = 0; lado < 2; lado++) {
                double gx = lado == 0 ? ox : ox + fw, dir = lado == 0 ? 1 : -1;
                area(b, gx, oy + fh / 2, dir, 16.5 * m, 40.32 * m);
                area(b, gx, oy + fh / 2, dir, 5.5 * m, 18.32 * m);
                double pxm = gx + dir * 11 * m;
                b.fill(new Ellipse2D.Double(pxm - 0.45 * m, oy + fh / 2 - 0.45 * m, 0.9 * m, 0.9 * m));
                b.draw(new Arc2D.Double(pxm - 9.15 * m, oy + fh / 2 - 9.15 * m, 18.3 * m, 18.3 * m,
                    lado == 0 ? -53 : 127, 106, Arc2D.OPEN));
                // gol com rede
                double gw = 2.2 * m, gh = 7.32 * m;
                Rectangle2D.Double trave = new Rectangle2D.Double(lado == 0 ? gx - gw : gx, oy + fh / 2 - gh / 2, gw, gh);
                b.setColor(new Color(255, 255, 255, 40));
                for (double k = trave.y; k <= trave.y + gh; k += m * 0.8) b.draw(new Line2D.Double(trave.x, k, trave.x + gw, k));
                for (double k = trave.x; k <= trave.x + gw; k += m * 0.8) b.draw(new Line2D.Double(k, trave.y, k, trave.y + gh));
                b.setColor(new Color(255, 255, 255, 230));
                b.draw(trave);
                b.setColor(LINHA);
                for (int c = 0; c < 2; c++) {
                    double cy = c == 0 ? oy : oy + fh;
                    b.draw(new Arc2D.Double(gx - m, cy - m, 2 * m, 2 * m, lado == 0 ? (c == 0 ? 270 : 0) : (c == 0 ? 180 : 90), 90, Arc2D.OPEN));
                }
            }
            b.setClip(null);
            b.setColor(new Color(242, 193, 78, 60));
            b.setStroke(new BasicStroke(1.2f));
            b.draw(new RoundRectangle2D.Double(0.6, 0.6, w - 1.2, h - 1.2, 18, 18));
            b.dispose();
        }
        g.drawImage(gramado, x, y, null);
    }

    static void area(Graphics2D b, double gx, double cy, double dir, double fundo, double largura) {
        double x = dir > 0 ? gx : gx - fundo;
        b.draw(new Rectangle2D.Double(x, cy - largura / 2, fundo, largura));
    }

    void pintaJogo(Graphics2D g, long agora) {
        double R = Math.max(7, campo.width * 0.0165);
        // sombras
        ArrayList<Ator> ordem = new ArrayList<Ator>(atores.values());
        Collections.sort(ordem, new Comparator<Ator>() {
            public int compare(Ator a, Ator b) { return Double.compare(a.y, b.y); }
        });
        g.setColor(new Color(0, 0, 0, 80));
        for (Ator a : ordem) g.fill(new Ellipse2D.Double(sx(a.x) - R * 0.95, sy(a.y) - R * 0.1, R * 2.1, R * 1.35));
        Font num = Estreia.negrito((float) (R * 1.0));
        for (Ator a : ordem) {
            double cx = sx(a.x), cy = sy(a.y);
            boolean destaque = a == dono || a == receptor;
            if (destaque) {
                g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (R * 2.3), new float[]{0f, 1f},
                    new Color[]{new Color(242, 193, 78, 110), new Color(242, 193, 78, 0)}));
                g.fill(new Ellipse2D.Double(cx - R * 2.3, cy - R * 2.3, R * 4.6, R * 4.6));
            }
            Color fundo = a.vaga == 1 ? (a.lado == 0 ? new Color(0x2B2E2C) : new Color(0xD8B35A)) : cor[a.lado];
            Color borda = a.vaga == 1 ? (a.lado == 0 ? new Color(0xF2C14E) : new Color(0x1A1408)) : aro[a.lado];
            if (distancia(fundo, borda) < 70) borda = brilho(fundo) > 0.5 ? new Color(0x14110A) : new Color(0xF4F1EA);
            Ellipse2D.Double corpo = new Ellipse2D.Double(cx - R, cy - R, 2 * R, 2 * R);
            g.setPaint(new GradientPaint((float) cx, (float) (cy - R), clareia(fundo, 0.18f), (float) cx, (float) (cy + R), fundo));
            g.fill(corpo);
            g.setStroke(new BasicStroke((float) Math.max(1.6, R * 0.17)));
            g.setColor(destaque ? OURO : borda);
            g.draw(corpo);
            g.setFont(num);
            FontMetrics fm = g.getFontMetrics();
            String n = String.valueOf(a.numero);
            g.setColor(brilho(fundo) > 0.55 ? new Color(0x14110A) : Color.WHITE);
            centro(g, n, cx, cy);
        }
        // marcas (cartao, contusao, substituicao)
        for (Marca m : marcas) {
            if (!atores.containsValue(m.ator) && m.tipo != 3 && m.tipo != 4) continue;
            double cx = sx(m.ator.x) + R * 0.9, cy = sy(m.ator.y) - R * 1.5;
            if (m.tipo >= 2 && m.tipo <= 4) {
                g.setColor(m.tipo == 2 ? new Color(0xF5D547) : new Color(0xE5463B));
                g.fill(new RoundRectangle2D.Double(cx, cy, R * 0.75, R * 1.05, 3, 3));
                g.setColor(new Color(0, 0, 0, 120));
                g.setStroke(new BasicStroke(1f));
                g.draw(new RoundRectangle2D.Double(cx, cy, R * 0.75, R * 1.05, 3, 3));
            } else if (m.tipo == 5) {
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(cx, cy, R * 1.1, R * 1.1));
                g.setColor(new Color(0xE5463B));
                double c = R * 0.55, t = R * 0.22;
                g.fill(new Rectangle2D.Double(cx + c - t / 2, cy + R * 0.2, t, R * 0.7));
                g.fill(new Rectangle2D.Double(cx + R * 0.2, cy + c - t / 2, R * 0.7, t));
            } else if (m.tipo == 6) {
                etiqueta(g, "entrou", sx(m.ator.x), sy(m.ator.y) - R * 2.2, R, new Color(0x5FCF8E));
            }
        }
        // bola
        if (bolaVisivel) {
            double br = R * 0.46 * (1 + bolaH * 0.5), alto = bolaH * R * 3.2;
            double bx = sx(bolaX), by = sy(bolaY);
            g.setColor(new Color(0, 0, 0, 110));
            g.fill(new Ellipse2D.Double(bx - br + alto * 0.25, by - br * 0.5, br * 2, br * 1.1));
            Ellipse2D.Double bola = new Ellipse2D.Double(bx - br, by - br - alto, br * 2, br * 2);
            g.setPaint(new RadialGradientPaint((float) (bx - br * 0.35), (float) (by - br * 0.45 - alto), (float) (br * 1.6),
                new float[]{0f, 1f}, new Color[]{Color.WHITE, new Color(0xC9C6BE)}));
            g.fill(bola);
            g.setColor(new Color(0x2A2A2A));
            g.setStroke(new BasicStroke((float) Math.max(1, br * 0.18)));
            g.draw(bola);
        }
        for (Ator a : foraAgora.keySet()) {
            if (!atores.containsValue(a)) continue;
            exclamacao(g, sx(a.x) - R * 1.05, sy(a.y) - R * 1.05, Math.max(5, R * 0.48));
        }
        // nome de quem esta com a bola (e a nota dele)
        Ator mostra = dono != null ? dono : receptor;
        if (mostra != null) {
            String n = curto(nome(mostra.f)) + "  " + Notas.texto(nota(mostra));
            etiqueta(g, n, sx(mostra.x), sy(mostra.y) + R * 2.15, R, null);
        }
        // gol!
        if (agora >= golDe && agora < golAte) pintaGol(g, agora);
        if (aviso != null && agora < avisoAte) pintaAviso(g, aviso, agora);
        if (agora < alertaAte) pintaAlerta(g, agora);
    }

    static Color clareia(Color c, float k) {
        return new Color(Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * k)), Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * k)),
            Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * k)));
    }

    static String curto(String n) {
        return n.length() > 18 ? n.substring(0, 17) + "…" : n;
    }

    void etiqueta(Graphics2D g, String texto, double cx, double cy, double R, Color cor) {
        g.setFont(Estreia.semi((float) Math.max(10, R * 0.95)));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(texto) + R * 1.1, h = fm.getHeight() + 2;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(cx - w / 2, cy - h / 2, w, h, h, h);
        g.setColor(new Color(10, 12, 11, 205));
        g.fill(r);
        g.setColor(cor != null ? cor : new Color(242, 193, 78, 150));
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setColor(cor != null ? cor : Tema.TEXTO);
        g.drawString(texto, (float) (cx - fm.stringWidth(texto) / 2.0), (float) (cy + fm.getAscent() / 2.0 - 2));
    }

    void pintaGol(Graphics2D g, long agora) {
        double total = golAte - golDe, u = (agora - golDe) / total;
        float alfa = (float) Math.min(1, Math.min(u / 0.12, (1 - u) / 0.18));
        Composite antes = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, alfa)));
        double cx = campo.getCenterX(), cy = campo.getCenterY();
        g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) (campo.width * 0.6), new float[]{0f, 1f},
            new Color[]{new Color(0, 0, 0, 190), new Color(0, 0, 0, 90)}));
        g.fill(campo.getBounds2D().createUnion(new Rectangle2D.Double(campo.x - 40, campo.y - 40, campo.width + 80, campo.height + 80)));
        double escala = 1 + 0.08 * Math.max(0, 1 - u * 4);
        Font f = Estreia.titulo((float) (campo.height * 0.34 * escala));
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String txt = "GOL!";
        float tx = (float) (cx - fm.stringWidth(txt) / 2.0), ty = (float) (cy + fm.getAscent() * 0.28);
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(242, 193, 78, 14));
            g.setStroke(new BasicStroke(i * 5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(f.createGlyphVector(g.getFontRenderContext(), txt).getOutline(tx, ty));
        }
        g.setPaint(new GradientPaint(tx, ty - fm.getAscent() * 0.7f, new Color(0xFFE7A0), tx, ty, new Color(0xE0A73A)));
        g.drawString(txt, tx, ty);
        g.setColor(new Color(0x3A2A08));
        g.setFont(Estreia.espacada(Estreia.semi((float) Math.max(12, campo.height * 0.042)), 0.18f));
        fm = g.getFontMetrics();
        String linha = (golNome + "  ·  " + golTime).toUpperCase();
        double lw = fm.stringWidth(linha) + 48, lh = fm.getHeight() + 12, ly = ty + fm.getHeight() * 1.5 - fm.getAscent() - 6;
        g.setColor(new Color(8, 10, 9, 220));
        g.fill(new RoundRectangle2D.Double(cx - lw / 2, ly, lw, lh, lh, lh));
        g.setColor(visivel(cor[golLado]));
        g.fill(new RoundRectangle2D.Double(cx - lw / 2 + 10, ly + lh / 2 - 3, 6, 6, 6, 6));
        g.fill(new RoundRectangle2D.Double(cx + lw / 2 - 16, ly + lh / 2 - 3, 6, 6, 6, 6));
        g.setColor(OURO);
        g.drawString(linha, (float) (cx - fm.stringWidth(linha) / 2.0), (float) (ty + fm.getHeight() * 1.5));
        g.setComposite(antes);
    }

    void pintaAviso(Graphics2D g, String txt, long agora) {
        g.setFont(Estreia.titulo((float) Math.max(20, campo.height * 0.1)));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(txt) + 56, h = fm.getHeight() + 10;
        double x = campo.getCenterX() - w / 2, y = campo.getCenterY() - h / 2;
        g.setColor(new Color(8, 10, 9, 215));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(new Color(242, 193, 78, 160));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, h, h));
        g.setColor(OURO);
        g.drawString(txt, (float) (x + 28), (float) (y + 5 + fm.getAscent()));
    }

    void pintaLegenda(Graphics2D g, double x, double y, double w, double h) {
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h, 14, 14);
        g.setColor(new Color(0x121614));
        g.fill(r);
        g.setColor(Tema.BORDA);
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setFont(Estreia.titulo(19f));
        FontMetrics fm = g.getFontMetrics();
        String min = fim ? "Fim" : legendaMin + "'";
        g.setColor(OURO);
        g.drawString(min, (float) (x + 16), (float) (y + h / 2 + fm.getAscent() / 2.0 - 3));
        int mw = fm.stringWidth(min);
        g.setFont(Estreia.texto(13.5f));
        fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        g.drawString(corta(legenda, fm, w - mw - 46), (float) (x + 28 + mw), (float) (y + h / 2 + fm.getAscent() / 2.0 - 2));
    }

    // Nome do jogador inteiro sempre que der: primeiro do tamanho normal, depois com o primeiro
    // nome abreviado ("G. Benedetti"), depois um pouco menor; so no fim corta com reticencias.
    // Mede com metricas fracionarias, as mesmas do desenho (deixa a fonte escolhida no g).
    static String cabe(Graphics2D g, String s, double max) {
        if (s == null) s = "";
        String abrev = s;
        int esp = s.indexOf(' ');
        if (esp > 0 && esp < s.length() - 1) abrev = s.charAt(0) + ". " + s.substring(esp + 1).trim();
        for (float t : new float[] {12.5f, 11.5f}) {
            g.setFont(Estreia.semi(t));
            for (String v : new String[] {s, abrev})
                if (g.getFont().getStringBounds(v, g.getFontRenderContext()).getWidth() <= max) return v;
        }
        FontMetrics fm = g.getFontMetrics();
        while (abrev.length() > 1 && fm.getStringBounds(abrev + "…", g).getWidth() > max) abrev = abrev.substring(0, abrev.length() - 1);
        return abrev + "…";
    }

    static String corta(String s, FontMetrics fm, double max) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= max) return s;
        while (s.length() > 1 && fm.stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    static Color visivel(Color c) {
        double b = brilho(c);
        return b < 0.32 ? clareia(c, (float) (0.32 - b) * 1.8f + 0.1f) : c;
    }

    void pintaPressao(Graphics2D g, double x, double y, double w, double h) {
        g.setFont(Estreia.espacada(Estreia.semi(10.5f), 0.16f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(OURO);
        g.drawString("PRESSÃO", (float) x + 2, (float) (y + fm.getAscent()));
        int lw = fm.stringWidth("PRESSÃO") + 14;
        g.setFont(Estreia.texto(11f));
        fm = g.getFontMetrics();
        g.setColor(visivel(cor[0]));
        g.drawString(curto(time(0)), (float) x + 2, (float) (y + h * 0.5 + fm.getAscent() / 2.0));
        g.setColor(visivel(cor[1]));
        g.drawString(curto(time(1)), (float) x + 2, (float) (y + h - 2));
        double gx = x + Math.max(lw, 130), gw = w - (gx - x), meio = y + h / 2;
        g.setColor(new Color(255, 255, 255, 30));
        g.fill(new Rectangle2D.Double(gx, meio - 0.5, gw, 1));
        double meioTempo = gx + gw * 52.0 / pressao.length;
        g.fill(new Rectangle2D.Double(meioTempo, y + 4, 1, h - 8));
        double bw = gw / pressao.length;
        int ate = (tempo - 1) * 52 + gt;
        for (int i = 0; i < pressao.length && i <= ate; i++) {
            // suaviza: soma do minuto com os vizinhos
            // soma dos ultimos minutos: mostra quem esta mandando no jogo
            double v = 0;
            for (int k = 0; k < 4 && i - k >= 0; k++) v += pressao[i - k] * (1 - k * 0.22);
            if (v == 0) continue;
            double alt = Math.min(1, Math.abs(v) / 4.0) * (h / 2 - 4);
            g.setColor(v > 0 ? visivel(cor[0]) : visivel(cor[1]));
            g.fill(new Rectangle2D.Double(gx + i * bw + 0.5, v > 0 ? meio - alt - 1 : meio + 1, Math.max(1, bw - 1), alt));
        }
    }

    void pintaLances(Graphics2D g, double x, double y, double w, double h) {
        if (h < 40) return;
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h - 4, 14, 14);
        g.setColor(new Color(0x121614));
        g.fill(r);
        g.setColor(Tema.BORDA);
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setFont(Estreia.espacada(Estreia.semi(10.5f), 0.16f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(OURO);
        g.drawString("LANCES", (float) (x + 16), (float) (y + 22));
        double ly = y + 34;
        Shape clip = g.getClip();
        g.clip(new Rectangle2D.Double(x, y, w, h - 8));
        for (Linha l : lances) {
            if (ly > y + h - 12) break;
            g.setFont(Estreia.titulo(15f));
            fm = g.getFontMetrics();
            g.setColor(l.tipo == GOL ? OURO : Tema.TEXTO_FRACO);
            g.drawString(l.minuto, (float) (x + 16), (float) (ly + 13));
            double ix = x + 62;
            icone(g, l, ix, ly + 3);
            g.setFont(l.tipo == GOL ? Estreia.negrito(13f) : Estreia.texto(13f));
            fm = g.getFontMetrics();
            g.setColor(l.tipo == GOL ? Tema.TEXTO : new Color(0xD9D5CB));
            g.drawString(corta(l.texto, fm, w - (ix - x) - 40), (float) (ix + 22), (float) (ly + 13));
            ly += 24;
        }
        g.setClip(clip);
        if (lances.isEmpty()) {
            g.setFont(Estreia.texto(13f));
            g.setColor(Tema.TEXTO_FRACO);
            g.drawString("Finalizações, gols, cartões e substituições aparecem aqui.", (float) (x + 16), (float) (ly + 13));
        }
    }

    void icone(Graphics2D g, Linha l, double x, double y) {
        double s = 13;
        if (l.tipo == GOL || l.tipo == DEFENDE || l.tipo == FORA) {
            Ellipse2D.Double b = new Ellipse2D.Double(x, y, s, s);
            g.setColor(l.tipo == GOL ? OURO : new Color(0xE9E6DF));
            g.fill(b);
            g.setColor(new Color(0x1A1A1A));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(b);
            if (l.tipo != GOL) {
                g.setColor(l.tipo == DEFENDE ? new Color(0x5FCF8E) : new Color(0xFF8A7A));
                g.fill(new Ellipse2D.Double(x + s * 0.62, y - 2, s * 0.48, s * 0.48));
            }
        } else if (l.tipo == 12 || l.tipo == 13 || l.tipo == 14) {
            g.setColor(l.tipo == 12 ? new Color(0xF5D547) : new Color(0xE5463B));
            g.fill(new RoundRectangle2D.Double(x + 2, y - 1, s * 0.7, s, 3, 3));
        } else if (l.tipo == 15) {
            g.setColor(new Color(0xE5463B));
            g.fill(new Rectangle2D.Double(x + s * 0.38, y, s * 0.26, s));
            g.fill(new Rectangle2D.Double(x, y + s * 0.38, s, s * 0.26));
        } else if (l.tipo == 16) {
            g.setColor(new Color(0x5FCF8E));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(x + 3, y + s, x + 3, y + 2));
            g.draw(new Line2D.Double(x + 3, y + 2, x, y + 5));
            g.setColor(new Color(0xFF8A7A));
            g.draw(new Line2D.Double(x + 10, y, x + 10, y + s - 2));
            g.draw(new Line2D.Double(x + 10, y + s - 2, x + 13, y + s - 5));
        } else if (l.tipo == 31) {
            exclamacao(g, x + s / 2, y + s / 2, s / 2);
        } else if (l.tipo == 30) {
            // prancheta: mudanca de ordens
            g.setColor(l.lado == humano ? OURO : Tema.TEXTO_FRACO);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new RoundRectangle2D.Double(x + 1, y, s - 2, s, 3, 3));
            g.draw(new Line2D.Double(x + 4, y + 4.5, x + s - 4, y + 4.5));
            g.draw(new Line2D.Double(x + 4, y + 8.5, x + s - 6, y + 8.5));
        } else {
            g.setColor(Tema.TEXTO_FRACO);
            g.fill(new Ellipse2D.Double(x + 4, y + 4, 5, 5));
        }
    }

    // coluna "Seu time": numero, nome, funcao em campo, forca e energia
    void pintaTime(Graphics2D g, double x, double y, double w, double h) {
        RoundRectangle2D.Double r = new RoundRectangle2D.Double(x, y, w, h - 4, 16, 16);
        g.setColor(new Color(0x121614));
        g.fill(r);
        g.setColor(Tema.BORDA);
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
        g.setFont(Estreia.titulo(21f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Tema.TEXTO);
        g.drawString("Seu time", (float) (x + 16), (float) (y + 30));
        g.setFont(Estreia.espacada(Estreia.semi(9.5f), 0.14f));
        fm = g.getFontMetrics();
        g.setColor(OURO);
        // cabecalho das colunas: nota, forca e energia
        g.drawString("NOTA", (float) (x + w - 95 - fm.stringWidth("NOTA") / 2.0), (float) (y + 29));
        g.drawString("FOR", (float) (x + w - 50 - fm.stringWidth("FOR")), (float) (y + 29));
        g.drawString("EN", (float) (x + w - 29 - fm.stringWidth("EN") / 2.0), (float) (y + 29));
        g.setFont(Estreia.texto(11.5f));
        g.setColor(Tema.TEXTO_FRACO);
        g.drawString(curto(time(humano)), (float) (x + 16), (float) (y + 48));

        ArrayList<Ator> meus = new ArrayList<Ator>();
        for (Ator a : atores.values()) if (a.lado == humano) meus.add(a);
        Collections.sort(meus, new Comparator<Ator>() {
            public int compare(Ator a, Ator b) { return ordem(a.vaga) - ordem(b.vaga); }
        });
        Map<Object, int[]> extras = extras();
        double topo = y + 62, rh = Math.max(24, Math.min(36, (h - topo - 190) / Math.max(11, meus.size())));
        double ly = topo;
        for (Ator a : meus) {
            boolean comBola = a == dono;
            if (comBola) {
                g.setColor(new Color(242, 193, 78, 26));
                g.fill(new RoundRectangle2D.Double(x + 6, ly - 2, w - 12, rh - 2, 10, 10));
            }
            double cy = ly + rh / 2 - 2;
            String[] pap = {"GOL", "ZAG", "MEI", "ATA"};
            int p = papel(a.vaga);
            String chip = p == DEFESA && (a.vaga == 2 || a.vaga == 9) ? "LAT" : pap[p];
            Color cc = p == GOLEIRO ? new Color(0xD8B35A) : p == DEFESA ? new Color(0x7FA7D9) : p == MEIO ? new Color(0x7CCB98) : new Color(0xE58C7E);
            g.setFont(Estreia.negrito(9.5f));
            fm = g.getFontMetrics();
            RoundRectangle2D.Double ch = new RoundRectangle2D.Double(x + 14, cy - 8, 32, 16, 6, 6);
            g.setColor(cc);
            g.fill(ch);
            g.setColor(new Color(0x14110A));
            centro(g, chip, x + 30, cy);
            // numero
            Color fundo = a.vaga == 1 ? (a.lado == 0 ? new Color(0x2B2E2C) : new Color(0xD8B35A)) : cor[a.lado];
            Ellipse2D.Double bol = new Ellipse2D.Double(x + 52, cy - 9, 18, 18);
            g.setColor(fundo);
            g.fill(bol);
            g.setColor(comBola ? OURO : new Color(255, 255, 255, 90));
            g.draw(bol);
            g.setFont(Estreia.negrito(9.5f));
            fm = g.getFontMetrics();
            String n = String.valueOf(a.numero);
            g.setColor(brilho(fundo) > 0.55 ? new Color(0x14110A) : Color.WHITE);
            centro(g, n, x + 61, cy);
            // nome: vai ate a nota; os gols e cartoes ficam numa linha embaixo dele, sem empurrar
            boolean foraPos = Boolean.TRUE.equals(chama(a.f, "gF"));
            int[] ex = extras.get(a.f);
            boolean marcas = ex != null && (ex[0] > 0 || ex[1] > 0);
            boolean embaixo = marcas && rh >= 30;
            double nx = x + 78, fimNome = x + w - 122 - (foraPos ? 16 : 0);
            double reserva = marcas && !embaixo ? ex[0] * 13 + (ex[1] > 0 ? 11 : 0) : 0;
            double ny = embaixo ? cy - 5 : cy;
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            String nome = cabe(g, nome(a.f), fimNome - nx - reserva);
            fm = g.getFontMetrics();
            g.setColor(foraPos ? new Color(0xFFB4A8) : Tema.TEXTO);
            g.drawString(nome, (float) nx, (float) (ny + fm.getAscent() / 2.0 - 2));
            double ix = nx + fm.getStringBounds(nome, g).getWidth() + 5;
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            if (foraPos) {
                exclamacao(g, ix + 6, ny, 6);
                ix += 16;
            }
            if (marcas) {
                double my = embaixo ? cy + 8 : cy, d = embaixo ? 8 : 10;
                if (embaixo) ix = nx;
                for (int i = 0; i < ex[0]; i++) {
                    g.setColor(Color.WHITE);
                    g.fill(new Ellipse2D.Double(ix, my - d / 2, d, d));
                    g.setColor(new Color(0x1A1A1A));
                    g.draw(new Ellipse2D.Double(ix, my - d / 2, d, d));
                    ix += d + 3;
                }
                if (ex[1] > 0) {
                    g.setColor(ex[1] == 1 ? new Color(0xF5D547) : new Color(0xE5463B));
                    g.fill(new RoundRectangle2D.Double(ix, my - d * 0.6, d * 0.8, d * 1.2, 2, 2));
                }
            }
            // nota ao vivo (com seta quando sobe ou cai)
            double nt = nota(a);
            Color ncor = Notas.cor(nt);
            RoundRectangle2D.Double nc = new RoundRectangle2D.Double(x + w - 112, cy - 9, 34, 18, 9, 9);
            g.setColor(new Color(ncor.getRed(), ncor.getGreen(), ncor.getBlue(), 38));
            g.fill(nc);
            g.setColor(new Color(ncor.getRed(), ncor.getGreen(), ncor.getBlue(), 150));
            g.draw(nc);
            g.setFont(Estreia.negrito(11f));
            fm = g.getFontMetrics();
            String ns = Notas.texto(nt);
            g.setColor(ncor);
            centro(g, ns, nc.getCenterX(), cy);
            double[] td = tendencia.get(a.f);
            long agoraT = System.currentTimeMillis();
            if (td != null && td[1] > agoraT && td[2] != 0) {
                float al = (float) Math.min(1, (td[1] - agoraT) / 1200.0);
                Composite antes = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, al));
                double ax = x + w - 118, s3 = 4;
                Path2D.Double seta = new Path2D.Double();
                if (td[2] > 0) { seta.moveTo(ax - s3, cy + s3 * 0.6); seta.lineTo(ax + s3, cy + s3 * 0.6); seta.lineTo(ax, cy - s3 * 0.9); }
                else { seta.moveTo(ax - s3, cy - s3 * 0.6); seta.lineTo(ax + s3, cy - s3 * 0.6); seta.lineTo(ax, cy + s3 * 0.9); }
                seta.closePath();
                g.setColor(td[2] > 0 ? new Color(0x3FD47E) : new Color(0xFF6B5E));
                g.fill(seta);
                g.setComposite(antes);
            }
            // forca e energia
            int forca = inteiro(chama(a.f, "fi"));
            int energia = Math.max(0, Math.min(100, inteiro(chama(a.f, "fp"))));
            g.setFont(Estreia.titulo(15f));
            fm = g.getFontMetrics();
            String fs = String.valueOf(forca);
            g.setColor(foraPos ? new Color(0xFFB4A8) : Tema.TEXTO);
            g.drawString(fs, (float) (x + w - 50 - fm.stringWidth(fs)), (float) (cy + fm.getAscent() / 2.0 - 2));
            double bx = x + w - 42, bw = 28;
            g.setColor(new Color(255, 255, 255, 28));
            g.fill(new RoundRectangle2D.Double(bx, cy - 2, bw, 4, 4, 4));
            g.setColor(energia >= 70 ? Tema.BOM : energia >= 45 ? new Color(0xF2C14E) : new Color(0xFF6B5E));
            g.fill(new RoundRectangle2D.Double(bx, cy - 2, bw * energia / 100.0, 4, 4, 4));
            ly += rh;
        }
        // ordens: resumo do plano atual e botao
        double oy = y + h - 4 - 112;
        boolean podeOrdens = Boolean.TRUE.equals(chama(timeHumano(), "jZ")) && !fim;
        if (podeOrdens && oy > ly + 4) {
            g.setColor(Tema.BORDA);
            g.draw(new Line2D.Double(x + 14, oy, x + w - 14, oy));
            g.setFont(Estreia.espacada(Estreia.semi(9.5f), 0.14f));
            g.setColor(OURO);
            g.drawString("ORDENS", (float) (x + 16), (float) (oy + 22));
            c.Ordens.Plano pl = c.Ordens.de((best.ah) timeHumano());
            if (!pl.humano) { c.b mt = motor(); if (mt != null && mt.plano(humano) != null) pl = mt.plano(humano); }
            g.setFont(Estreia.semi(12f));
            fm = g.getFontMetrics();
            String m1 = c.Ordens.MENTALIDADES[pl.mentalidade - c.Ordens.RETRANCA];
            g.setColor(PainelOrdens.corMentalidade(pl.mentalidade - c.Ordens.RETRANCA));
            g.drawString(m1, (float) (x + 16), (float) (oy + 42));
            g.setFont(Estreia.texto(11f));
            g.setColor(Tema.TEXTO_FRACO);
            String m2 = c.Ordens.ATAQUES[pl.ataque] + " · pressão " + c.Ordens.PRESSOES[pl.pressao].toLowerCase() + " · passe " + c.Ordens.PASSES[pl.passe].toLowerCase();
            g.drawString(corta(m2, g.getFontMetrics(), w - 32), (float) (x + 16), (float) (oy + 59));
            botaoOrdens.setRect(x + 14, oy + 70, w - 28, 34);
            RoundRectangle2D.Double b = new RoundRectangle2D.Double(botaoOrdens.x, botaoOrdens.y, botaoOrdens.width, 34, 34, 34);
            g.setPaint(new GradientPaint(0, (float) botaoOrdens.y, new Color(0xF6CE6A), 0, (float) (botaoOrdens.y + 34), new Color(0xD9A53B)));
            g.fill(b);
            g.setFont(Estreia.semi(13f));
            fm = g.getFontMetrics();
            String bt = "Dar ordens";
            g.setColor(new Color(0x1A1408));
            g.drawString(bt, (float) (botaoOrdens.getCenterX() - fm.stringWidth(bt) / 2.0 - 8), (float) (botaoOrdens.y + 17 + fm.getAscent() / 2.0 - 2));
            g.setFont(Estreia.negrito(10f));
            FontMetrics fk = g.getFontMetrics();
            double kx = botaoOrdens.getCenterX() + fm.stringWidth(bt) / 2.0 + 2;
            g.setColor(new Color(26, 20, 8, 60));
            g.fill(new RoundRectangle2D.Double(kx, botaoOrdens.y + 9, 16, 16, 5, 5));
            g.setColor(new Color(0x1A1408));
            g.drawString("O", (float) (kx + 8 - fk.stringWidth("O") / 2.0), (float) (botaoOrdens.y + 21));
        } else {
            botaoOrdens.setRect(0, 0, 0, 0);
            oy = y + h;
        }
        // legenda: teclas desenhadas e o simbolo de fora de posicao
        ly += 10;
        String[][] linhas = {{"F1–F5", "Velocidade"}, {"Espaço", "Substituições"}, {"V", "Câmera 3D / prancheta"}, {"O", "Dar ordens"}, {"!", "Fora de posição (rende menos)"}};
        if (ly + 40 < oy) {
            g.setColor(Tema.BORDA);
            g.draw(new Line2D.Double(x + 14, ly, x + w - 14, ly));
            g.setFont(Estreia.espacada(Estreia.semi(9.5f), 0.14f));
            g.setColor(OURO);
            g.drawString("CONTROLES", (float) (x + 16), (float) (ly + 22));
            ly += 34;
            for (String[] l : linhas) {
                if (ly + 24 > oy - 6) break;
                if (l[1].equals("Dar ordens") && botaoOrdens.width <= 0) continue;
                double cy = ly + 10;
                if (l[0].equals("!")) exclamacao(g, x + 16 + 9, cy, 8);
                else tecla(g, l[0], x + 16, cy);
                g.setFont(Estreia.texto(12f));
                g.setColor(l[0].equals("!") ? new Color(0xFFB4A8) : Tema.TEXTO);
                fm = g.getFontMetrics();
                g.drawString(l[1], (float) (x + 16 + 58), (float) (cy + fm.getAscent() / 2.0 - 2));
                ly += 26;
            }
        }
    }

    // escreve o texto com o centro do desenho das letras (nao da fonte) em cx, cy
    static void centro(Graphics2D g, String t, double cx, double cy) {
        Rectangle2D v = g.getFont().createGlyphVector(g.getFontRenderContext(), t).getVisualBounds();
        g.drawString(t, (float) (cx - v.getX() - v.getWidth() / 2), (float) (cy - v.getY() - v.getHeight() / 2));
    }

    // tecla de teclado desenhada (com a "sombra" de baixo, como uma tecla de verdade)
    static void tecla(Graphics2D g, String t, double x, double cy) {
        g.setFont(Estreia.negrito(10f));
        FontMetrics fm = g.getFontMetrics();
        double w = Math.max(20, fm.stringWidth(t) + 12), h = 19, y = cy - h / 2;
        g.setColor(new Color(0x0A0C0B));
        g.fill(new RoundRectangle2D.Double(x, y + 2, w, h, 7, 7));
        g.setPaint(new GradientPaint(0, (float) y, new Color(0x2C322E), 0, (float) (y + h), new Color(0x1B201D)));
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 7, 7));
        g.setColor(new Color(242, 193, 78, 120));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 7, 7));
        g.setColor(OURO);
        centro(g, t, x + w / 2, y + h / 2);
    }

    // gols e cartoes de cada jogador nesta partida
    Map<Object, int[]> extras() {
        Map<Object, int[]> r = new IdentityHashMap<Object, int[]>();
        for (Object a : lista(chama(jogo, "hE"))) {
            if (!Boolean.TRUE.equals(chama(a, "isDone"))) continue;
            int tipo = inteiro(chama(a, "b"));
            Object f = chama(a, "eo");
            if (f == null) continue;
            int[] e = r.get(f);
            if (e == null) { e = new int[2]; r.put(f, e); }
            if (tipo == 1 && inteiro(chama(a, "el")) != 2) e[0]++;
            else if (tipo == 2) e[1] = Math.max(e[1], 1);
            else if (tipo == 3 || tipo == 4) e[1] = 2;
        }
        return r;
    }

    // ------------------------------------------------------------------ abas

    final class Abas extends JComponent {
        Rectangle2D.Double a1 = new Rectangle2D.Double(), a2 = new Rectangle2D.Double();

        Abas() {
            setOpaque(false);
            setPreferredSize(new java.awt.Dimension(400, 34));
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (a1.contains(e.getPoint())) { preferirCampo = true; mostra(true); }
                    else if (a2.contains(e.getPoint())) { preferirCampo = false; mostra(false); }
                }
            });
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setFont(Estreia.semi(13f));
            FontMetrics fm = g.getFontMetrics();
            String[] t = {"Partida ao vivo", "Outros jogos"};
            double x = 0, h = getHeight() - 4;
            Rectangle2D.Double[] r = {a1, a2};
            for (int i = 0; i < 2; i++) {
                double w = fm.stringWidth(t[i]) + 36;
                r[i].setRect(x, 2, w, h);
                boolean ativo = (i == 0) == verCampo;
                RoundRectangle2D.Double p = new RoundRectangle2D.Double(x, 2, w, h, h, h);
                if (ativo) {
                    g.setPaint(new GradientPaint(0, 2, new Color(0xF6CE6A), 0, (float) h, new Color(0xD9A53B)));
                    g.fill(p);
                } else {
                    g.setColor(new Color(0x151917));
                    g.fill(p);
                    g.setColor(Tema.BORDA);
                    g.draw(p);
                }
                g.setColor(ativo ? new Color(0x1A1408) : Tema.TEXTO);
                g.drawString(t[i], (float) (x + 18), (float) (2 + h / 2 + fm.getAscent() / 2.0 - 2));
                x += w + 8;
            }
            g.dispose();
        }
    }
}
