package mod;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.Timer;

// Numeros que contam ate o valor novo: quando o texto de um rotulo muda so nos numeros
// ("15 milhões" -> "17 milhões", "2.944 mil" -> "3.120 mil"), os numeros correm do valor
// antigo ao novo em ~0,7 s. O valor antigo e guardado por chave, porque o jogo cria a tela
// principal de novo depois de cada rodada.
final class Contador {
    private Contador() {}

    private static final String ANIMANDO = "brombila.contando";
    private static final Pattern NUMERO = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+(?:,\\d+)?");
    private static final Map<String, String> ultimo = new HashMap<String, String>();
    private static final int MS = 700;

    // Liga a contagem num rotulo. 'chave' identifica o valor entre telas (ex.: "caixa");
    // 'doZero' faz o valor contar a partir de zero na primeira vez que aparece.
    static void liga(Object o, final String chave, final boolean doZero) {
        if (!(o instanceof JLabel)) return;
        final JLabel l = (JLabel) o;
        if (l.getClientProperty(ANIMANDO) != null) return;
        l.putClientProperty(ANIMANDO, new Timer[1]);
        l.addPropertyChangeListener("text", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (Boolean.TRUE.equals(l.getClientProperty(ANIMANDO + ".meu"))) return;
                novo(l, chave, (String) e.getOldValue(), (String) e.getNewValue(), doZero);
            }
        });
        String t = l.getText();
        if (t != null && !t.isEmpty()) novo(l, chave, null, t, doZero);
    }

    private static void novo(JLabel l, String chave, String antes, String depois, boolean doZero) {
        if (depois == null) return;
        String guardado = chave != null ? ultimo.get(chave) : null;
        if (chave != null) ultimo.put(chave, depois);
        String de = guardado != null ? guardado : antes;
        if (de == null && doZero) de = zera(depois);
        if ("caixa".equals(chave) && guardado != null && !guardado.equals(depois)) Sons.toca("moedas");
        if (de == null || de.equals(depois) || !Animacao.ligada()) return;
        anima(l, de, depois);
    }

    private static String zera(String s) {
        Matcher m = NUMERO.matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) m.appendReplacement(b, m.group().contains(",") ? "0,0" : "0");
        m.appendTail(b);
        return b.toString();
    }

    // Dinheiro do jogo por extenso: "12 milhões 901 mil", "850 mil", "1 bilhão 20 milhões".
    private static final Pattern DINHEIRO = Pattern.compile("^(-?)\\s*((?:\\d+\\s*(?:bilhões|bilhão|milhões|milhão|mil)\\s*)+)$");
    private static final Pattern PARTE = Pattern.compile("(\\d+)\\s*(bilhões|bilhão|milhões|milhão|mil)");

    private static double dinheiro(String s) {
        Matcher m = DINHEIRO.matcher(s.trim());
        if (!m.matches()) return Double.NaN;
        double v = 0;
        Matcher p = PARTE.matcher(m.group(2));
        while (p.find()) {
            String u = p.group(2);
            v += Long.parseLong(p.group(1)) * (u.startsWith("bilh") ? 1e9 : u.startsWith("milh") ? 1e6 : 1e3);
        }
        return m.group(1).isEmpty() ? v : -v;
    }

    private static String extenso(double v) {
        long mil = Math.round(Math.abs(v) / 1e3);
        long b = mil / 1000000, mi = mil / 1000 % 1000, k = mil % 1000;
        StringBuilder s = new StringBuilder(v < 0 ? "-" : "");
        if (b > 0) s.append(b).append(b == 1 ? " bilhão" : " bilhões");
        if (mi > 0) s.append(s.length() > 1 || b > 0 ? " " : "").append(mi).append(mi == 1 ? " milhão" : " milhões");
        if (k > 0) s.append(b > 0 || mi > 0 ? " " : "").append(k).append(" mil");
        if (b == 0 && mi == 0 && k == 0) s.append("0 mil");
        return s.toString();
    }

    private static void anima(final JLabel l, String de, final String para) {
        final double da = dinheiro(de), dp = dinheiro(para);
        final List<double[]> a = numeros(de), b = numeros(para);
        final List<String> forma = moldes(para);
        final boolean grana = !Double.isNaN(da) && !Double.isNaN(dp);
        if (!grana && (a.size() != b.size() || b.isEmpty() || !moldes(de).equals(forma))) return;
        final Timer[] atual = (Timer[]) l.getClientProperty(ANIMANDO);
        if (atual[0] != null) atual[0].stop();
        final long inicio = System.currentTimeMillis();
        final Timer timer = new Timer(Animacao.QUADRO_MS, null);
        timer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                float t = Math.min(1f, (System.currentTimeMillis() - inicio) / (float) MS);
                String texto = para;
                if (t < 1f) {
                    float k = Animacao.suave(t);
                    if (grana) {
                        texto = extenso(da + (dp - da) * k);
                    } else {
                        StringBuilder s = new StringBuilder(forma.get(0));
                        for (int i = 0; i < b.size(); i++) {
                            double v = a.get(i)[0] + (b.get(i)[0] - a.get(i)[0]) * k;
                            s.append(escreve(v, (int) b.get(i)[1], b.get(i)[2] > 0)).append(forma.get(i + 1));
                        }
                        texto = s.toString();
                    }
                }
                l.putClientProperty(ANIMANDO + ".meu", Boolean.TRUE);
                l.setText(texto);
                l.putClientProperty(ANIMANDO + ".meu", null);
                if (t >= 1f || !l.isDisplayable()) {
                    timer.stop();
                    if (!para.equals(l.getText())) {
                        l.putClientProperty(ANIMANDO + ".meu", Boolean.TRUE);
                        l.setText(para);
                        l.putClientProperty(ANIMANDO + ".meu", null);
                    }
                }
            }
        });
        atual[0] = timer;
        timer.start();
        timer.getActionListeners()[0].actionPerformed(null);
    }

    // valor, casas decimais, usa ponto de milhar (1 = sim)
    private static List<double[]> numeros(String s) {
        List<double[]> r = new ArrayList<double[]>();
        Matcher m = NUMERO.matcher(s);
        while (m.find()) {
            String g = m.group();
            int casas = g.contains(",") ? g.length() - g.indexOf(',') - 1 : 0;
            double v = Double.parseDouble(g.replace(".", "").replace(',', '.'));
            r.add(new double[] {v, casas, g.contains(".") ? 1 : 0});
        }
        return r;
    }

    // o texto sem os numeros (o que fica entre eles)
    private static List<String> moldes(String s) {
        List<String> r = new ArrayList<String>();
        Matcher m = NUMERO.matcher(s);
        int fim = 0;
        while (m.find()) {
            r.add(s.substring(fim, m.start()));
            fim = m.end();
        }
        r.add(s.substring(fim));
        return r;
    }

    private static String escreve(double v, int casas, boolean milhar) {
        String s = String.format(java.util.Locale.ROOT, "%." + casas + "f", v);
        String inteiro = casas > 0 ? s.substring(0, s.indexOf('.')) : s, resto = casas > 0 ? "," + s.substring(s.indexOf('.') + 1) : "";
        if (milhar) {
            StringBuilder b = new StringBuilder(inteiro);
            for (int i = b.length() - 3; i > 0; i -= 3) b.insert(i, '.');
            inteiro = b.toString();
        }
        return inteiro + resto;
    }
}
