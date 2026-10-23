package mod;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

// Leitura de campos internos (ofuscados) das classes do jogo. O Field achado fica guardado por
// classe e nome, porque a partida le os mesmos campos varias vezes por segundo.
final class Reflexao {
    private Reflexao() {}

    private static final Field NENHUM;
    static {
        Field f = null;
        try { f = Reflexao.class.getDeclaredField("NENHUM"); } catch (Throwable t) { /* nao acontece */ }
        NENHUM = f;
    }
    private static final Map<String, Field> campos = new HashMap<String, Field>();

    // Procura o campo na classe do objeto e nas superclasses.
    static Object campo(Object dono, String nome) {
        if (dono == null) return null;
        Field f = acha(dono.getClass(), nome, true);
        try { return f == null ? null : f.get(dono); } catch (Throwable t) { return null; }
    }

    // So na classe c (o objeto pode ser nulo para campos estaticos).
    static Object campo(Class<?> c, Object dono, String nome) {
        Field f = acha(c, nome, false);
        try { return f == null ? null : f.get(dono); } catch (Throwable t) { return null; }
    }

    private static Field acha(Class<?> c, String nome, boolean supers) {
        String k = c.getName() + (supers ? "+" : "#") + nome;
        Field f;
        synchronized (campos) { f = campos.get(k); }
        if (f == null) {
            f = NENHUM;
            for (Class<?> x = c; x != null; x = supers ? x.getSuperclass() : null) {
                try {
                    Field d = x.getDeclaredField(nome);
                    d.setAccessible(true);
                    f = d;
                    break;
                } catch (Throwable t) {
                    // tenta na superclasse
                }
            }
            synchronized (campos) { campos.put(k, f); }
        }
        return f == NENHUM ? null : f;
    }
}
