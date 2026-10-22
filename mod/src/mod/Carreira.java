package mod;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

// Dados novos da carreira (contratos, estagio, torcida...) num arquivo do mod ao lado do save:
// sav/<nome>.bbf. O save do jogo (.s22) e gravado pelo Kryo na ordem exata dos campos de cada
// classe, entao acrescentar um campo em best.F ou best.ah faria os saves atuais pararem de abrir.
// Aqui cada jogador e cada time tem um mapa proprio.
//
// Codigos: o time tem um codigo fixo (ah.lk()). O jogador nao: o jogo numera todos os jogadores
// (c.a.SR.O() e depois Q()) logo antes de salvar (g.b.Ai -> F.az) e esse numero vai junto no .s22.
// Por isso, com o jogo aberto, os dados do jogador ficam presos ao proprio objeto; no arquivo, ao
// numero que o jogo acabou de dar; e ao carregar, cada numero volta para o seu jogador.
//
// O jogo salva em c.a.ae(nome) (chamado por a.hz) e carrega em c.a.c(nome, backup) (chamado por
// a.dR). As duas chamadas foram trocadas por salva() e carrega() abaixo (tools/carreira/
// remenda_salvar.py): primeiro o jogo faz o que sempre fez, depois o mod grava ou le o .bbf.
// Save sem .bbf (antigo, ou de antes do mod) abre normal e comeca com os mapas vazios.
public final class Carreira {

    static final int VERSAO = 1;
    static final String MAGICO = "BBF1";

    // chave -> (campo -> valor). Valores so de tipos do Java (Integer, Long, Double, String, Boolean,
    // arrays e listas deles), para o arquivo abrir em qualquer versao do mod.
    final HashMap<String, HashMap<String, Serializable>> dados = new HashMap<String, HashMap<String, Serializable>>();
    final IdentityHashMap<Object, HashMap<String, Serializable>> jogadores = new IdentityHashMap<Object, HashMap<String, Serializable>>();
    Object dono;                // o mundo do jogo (c.a.SR) a que estes dados pertencem

    static Carreira atual;

    // ------------------------------------------------------------------ acesso

    // a carreira do jogo aberto; jogo novo (ou nenhum jogo) comeca vazio
    public static synchronized Carreira atual() {
        Object sr = c.a.SR;
        if (atual == null || atual.dono != sr) {
            atual = new Carreira();
            atual.dono = sr;
        }
        return atual;
    }

    public static Map<String, Serializable> geral() { return atual().mapa("g"); }

    public static Map<String, Serializable> jogador(best.F f) {
        Carreira c = atual();
        synchronized (c) {
            HashMap<String, Serializable> m = c.jogadores.get(f);
            if (m == null) c.jogadores.put(f, m = new HashMap<String, Serializable>());
            return m;
        }
    }

    public static Map<String, Serializable> time(best.ah t) {
        return atual().mapa("t" + t.lk());
    }

    synchronized HashMap<String, Serializable> mapa(String chave) {
        HashMap<String, Serializable> m = dados.get(chave);
        if (m == null) dados.put(chave, m = new HashMap<String, Serializable>());
        return m;
    }

    // ------------------------------------------------------------------ salvar e carregar

    // no lugar de c.a.ae(nome) em a.hz
    public static void salva(String nome) {
        c.a.ae(nome);
        if (nome == null) return;
        try {
            grava(arquivo(nome, ".bbf"));
            // copia de seguranca, como o .sbck do jogo
            Files.copy(arquivo(nome, ".bbf").toPath(), arquivo(nome, ".bbck").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // no lugar de c.a.c(nome, backup) em a.dR
    public static boolean carrega(String nome, boolean backup) {
        boolean ok = c.a.c(nome, backup);
        if (!ok || nome == null) return ok;
        Carreira lida = null;
        for (String ext : new String[] {".bbf", ".bbck"}) {
            File f = arquivo(nome, ext);
            if (!f.isFile()) continue;
            try {
                lida = le(f);
                break;
            } catch (Throwable t) {
                t.printStackTrace();       // arquivo estragado: tenta a copia, senao comeca vazio
            }
        }
        synchronized (Carreira.class) {
            atual = lida != null ? lida : new Carreira();
            atual.dono = c.a.SR;
        }
        Tempo.carregou();
        return ok;
    }

    // todos os jogadores do jogo, na ordem em que o jogo os numera
    static java.util.List<Object> todos() {
        java.util.ArrayList<Object> l = new java.util.ArrayList<Object>();
        best.f sr = c.a.SR;
        if (sr == null) return l;
        if (sr.O() != null) l.addAll(sr.O());
        if (sr.Q() != null) l.addAll(sr.Q());
        return l;
    }

    static File arquivo(String nome, String ext) {
        return new File(System.getProperty("user.dir") + "/sav/" + nome + ext);
    }

    static void grava(File f) throws IOException {
        Carreira c = atual();
        File tmp = new File(f.getPath() + ".tmp");
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
        try {
            out.writeUTF(MAGICO);
            out.writeInt(VERSAO);
            ObjectOutputStream o = new ObjectOutputStream(out);
            HashMap<String, HashMap<String, Serializable>> tudo;
            synchronized (c) {
                tudo = new HashMap<String, HashMap<String, Serializable>>(c.dados);
                // so quem ainda esta no jogo, pelo numero que o jogo acabou de dar (ver acima)
                for (Object j : todos()) {
                    HashMap<String, Serializable> m = c.jogadores.get(j);
                    if (m != null && !m.isEmpty()) tudo.put("j" + ((best.F) j).gD(), m);
                }
            }
            o.writeObject(tudo);
            o.flush();
        } finally {
            out.close();
        }
        Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("unchecked")
    static Carreira le(File f) throws IOException, ClassNotFoundException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
            if (!MAGICO.equals(in.readUTF())) throw new IOException("nao e um arquivo da carreira: " + f);
            int versao = in.readInt();
            if (versao > VERSAO) throw new IOException("arquivo de uma versao mais nova do mod: " + versao);
            Carreira c = new Carreira();
            Map<String, HashMap<String, Serializable>> tudo = (Map<String, HashMap<String, Serializable>>) new ObjectInputStream(in).readObject();
            HashMap<Integer, Object> porNumero = new HashMap<Integer, Object>();
            for (Object j : todos()) porNumero.put(((best.F) j).gD(), j);
            for (Map.Entry<String, HashMap<String, Serializable>> e : tudo.entrySet()) {
                String k = e.getKey();
                if (k.startsWith("j")) {
                    Object j = porNumero.get(Integer.valueOf(k.substring(1)));
                    if (j != null) c.jogadores.put(j, e.getValue());
                } else c.dados.put(k, e.getValue());
            }
            return c;
        } finally {
            in.close();
        }
    }
}
