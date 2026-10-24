package mod;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Classificacao com setas: o jogo nao guarda a posicao da rodada anterior, entao o mod tira um
// retrato da classificacao de cada competicao (f.s) a cada troca de tela da janela principal
// (antes e depois de cada rodada) e quando a Tabela abre. Na Tabela, cada linha (components.au)
// ganha uma seta verde (subiu) ou vermelha (caiu) com quantas posicoes, e na primeira vez que a
// rodada e vista as linhas deslizam da posicao antiga para a nova.
public final class Classificacao {
    private Classificacao() {}

    // competicao -> rodada (zb) -> time -> posicao no grupo
    private static final Map<Object, TreeMap<Integer, Map<String, Integer>>> HIST =
        new WeakHashMap<Object, TreeMap<Integer, Map<String, Integer>>>();
    // competicao -> ultima rodada que ja deslizou
    private static final Map<Object, Integer> DESLIZOU = new WeakHashMap<Object, Integer>();

    private static final String SETA = "brombila.seta";

    // Retrato de todas as competicoes do jogo (a.SR.R() -> best.a.j()).
    public static void registraTudo() {
        try {
            if (c.a.SR == null) return;
            for (Object p : c.a.SR.R()) {
                for (Object comp : ((best.a) p).j()) registra(comp);
            }
        } catch (Throwable t) {
            // jogo ainda carregando
        }
    }

    static void registra(Object comp) {
        if (!(comp instanceof f.s)) return;
        f.s s = (f.s) comp;
        try {
            Map<String, Integer> pos = new HashMap<String, Integer>();
            if (s.yQ().size() == 0) {
                poe(pos, s.yK());
            } else {
                for (Object g : s.yQ()) poe(pos, ((best.G) g).gR());
            }
            if (pos.isEmpty()) return;
            TreeMap<Integer, Map<String, Integer>> h = HIST.get(s);
            if (h == null) HIST.put(s, h = new TreeMap<Integer, Map<String, Integer>>());
            h.put(s.zb(), pos);
            while (h.size() > 4) h.remove(h.firstKey());
        } catch (Throwable t) {
            // competicao sem tabela
        }
    }

    private static void poe(Map<String, Integer> pos, List<?> times) {
        for (int i = 0; i < times.size(); i++) {
            Object t = times.get(i);
            if (t instanceof best.ah) pos.put(((best.ah) t).getNome(), i + 1);
        }
    }

    // Linhas da Tabela acabaram de entrar (components.au): setas e, se for a primeira vez nesta
    // rodada, o deslize. Chamado com o painel das linhas (xb do a.ay).
    private static final Map<Container, Boolean> PENDENTE = new WeakHashMap<Container, Boolean>();

    public static void linhas(final Container xb) {
        if (PENDENTE.containsKey(xb)) return;
        PENDENTE.put(xb, Boolean.TRUE);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                PENDENTE.remove(xb);
                aplica(xb);
            }
        });
    }

    private static void aplica(Container xb) {
        Component ay = xb;
        while (ay != null && !ay.getClass().getName().equals("a.ay")) ay = ay.getParent();
        if (ay == null) return;
        Object comp = Reflexao.campo(ay, "wV");
        if (!(comp instanceof f.s)) return;
        f.s s = (f.s) comp;
        registra(s);
        TreeMap<Integer, Map<String, Integer>> h = HIST.get(s);
        int rodada;
        try {
            rodada = s.zb();
        } catch (Throwable t) {
            return;
        }
        // a rodada anterior precisa ter tido jogos (zb - 1 = rodadas jogadas)
        Map.Entry<Integer, Map<String, Integer>> antes = h == null ? null : h.lowerEntry(rodada);
        Map<String, Integer> velho = antes != null && antes.getKey() >= 2 ? antes.getValue() : null;

        List<Component> linhas = new ArrayList<Component>();
        List<Integer> deltas = new ArrayList<Integer>();
        int n = 0;
        for (Component c : xb.getComponents()) {
            if (!(c instanceof components.au)) {
                n = 0;
                continue;
            }
            n++;
            Object nome = Reflexao.campo(c, "uF");
            String time = nome instanceof JLabel ? ((JLabel) nome).getText() : null;
            Integer p0 = velho == null || time == null ? null : velho.get(time);
            int d = p0 == null ? 0 : p0 - n;
            seta((JComponent) c, velho == null ? null : d);
            linhas.add(c);
            deltas.add(d);
        }
        Integer ja = DESLIZOU.get(s);
        if (velho != null && Animacao.ligada() && (ja == null || ja != rodada)) {
            DESLIZOU.put(s, rodada);
            desliza(linhas, deltas);
        }
    }

    // Seta entre o numero da posicao e a faixa colorida (o vao de 18 a 28 px da linha).
    private static void seta(JComponent linha, Integer delta) {
        Seta s = null;
        for (Component c : linha.getComponents()) if (c instanceof Seta) s = (Seta) c;
        if (delta == null) {
            if (s != null) linha.remove(s);
            return;
        }
        if (s == null) {
            s = new Seta();
            try {
                linha.add(s, new components.a(18, 0, 10, 25));
            } catch (Throwable t) {
                linha.add(s);
            }
            s.setBounds(18, 0, 10, 25);
        }
        s.delta = delta;
        s.inicio = System.currentTimeMillis();
        s.repaint();
    }

    private static final class Seta extends JComponent {
        int delta;
        long inicio;

        Seta() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float cx = getWidth() / 2f;
            if (delta == 0) {
                g.setColor(new Color(244, 241, 234, 60));
                g.fillRoundRect(Math.round(cx - 3), 12, 6, 2, 2, 2);
                g.dispose();
                return;
            }
            // a seta entra subindo (ou descendo) 4 px e clareando, depois que a linha assenta
            float t = Animacao.ligada() ? Math.min(1f, Math.max(0f, (System.currentTimeMillis() - inicio - 350) / 300f)) : 1f;
            if (t < 1f) repaint();
            float e = 1f - (1f - t) * (1f - t);
            boolean sobe = delta > 0;
            Color cor = sobe ? new Color(0x5BD66E) : new Color(0xEF5A5A);
            float off = (1f - e) * 4f * (sobe ? 1 : -1);
            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, e));
            Path2D.Float p = new Path2D.Float();
            float y = 8 + off;
            if (sobe) {
                p.moveTo(cx, y);
                p.lineTo(cx + 3.5f, y + 4.5f);
                p.lineTo(cx - 3.5f, y + 4.5f);
            } else {
                p.moveTo(cx, y + 4.5f);
                p.lineTo(cx + 3.5f, y);
                p.lineTo(cx - 3.5f, y);
            }
            p.closePath();
            g.setColor(cor);
            g.fill(p);
            String num = Integer.toString(Math.abs(delta));
            g.setFont(Estreia.negrito(8.5f));
            int w = g.getFontMetrics().stringWidth(num);
            g.drawString(num, cx - w / 2f, y + 14);
            g.dispose();
        }
    }

    // As linhas saem da posicao antiga (26 px por posicao) e deslizam ate a nova, em cascata.
    private static void desliza(final List<Component> linhas, final List<Integer> deltas) {
        final int[] base = new int[linhas.size()];
        boolean algum = false;
        for (int i = 0; i < linhas.size(); i++) {
            base[i] = linhas.get(i).getY();
            if (deltas.get(i) != 0) algum = true;
        }
        if (!algum) return;
        final long inicio = System.currentTimeMillis();
        final int dur = 650;
        final Timer timer = new Timer(Animacao.QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long agora = System.currentTimeMillis();
                boolean fim = true;
                for (int i = 0; i < linhas.size(); i++) {
                    Component c = linhas.get(i);
                    int d = deltas.get(i);
                    float t = Math.min(1f, Math.max(0f, (agora - inicio - 12 * Math.min(i, 20)) / (float) dur));
                    if (t < 1f) fim = false;
                    float u = 1f - t;
                    float k = 1f - u * u * u;
                    int y = base[i] + Math.round(d * 26 * (1f - k));
                    if (c.getY() != y) c.setLocation(c.getX(), y);
                }
                if (!linhas.isEmpty()) linhas.get(0).getParent().repaint();
                if (fim) timer.stop();
            }
        });
        // primeiro quadro ja na posicao antiga
        for (int i = 0; i < linhas.size(); i++) {
            Component c = linhas.get(i);
            c.setLocation(c.getX(), base[i] + deltas.get(i) * 26);
        }
        timer.start();
    }
}
