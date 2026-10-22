package mod;

// Ponto de entrada do mod (Rsrc-Main-Class no MANIFEST): instala o tema verde
// e depois chama o main original do jogo (best.h2).
public class Boot {
    public static void main(String[] args) throws Exception {
        // Texto com antialiasing (fica bem mais suave no Windows).
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        try {
            Tema.instalar();
        } catch (Throwable t) {
            // Se o tema falhar por qualquer motivo, o jogo abre normal.
            t.printStackTrace();
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class.forName("best.h2", true, loader)
            .getMethod("main", String[].class)
            .invoke(null, (Object) args);
    }
}
