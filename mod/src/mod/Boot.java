package mod;

// Ponto de entrada do mod (Rsrc-Main-Class no MANIFEST): instala o tema verde
// e depois chama o main original do jogo (best.h2).
public class Boot {
    public static void main(String[] args) throws Exception {
        // Texto com antialiasing (fica bem mais suave no Windows).
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        // Datas no formato brasileiro (08/08/2022) mesmo num Windows em outro idioma. No Java 9+
        // o formato padrao do pt-BR virou "8 de ago. de 2022"; COMPAT volta o do Java 8.
        if (System.getProperty("java.locale.providers") == null) System.setProperty("java.locale.providers", "COMPAT,CLDR");
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, new java.util.Locale("pt", "BR"));
        atualizaFundos();
        try {
            Tema.instalar();
        } catch (Throwable t) {
            // Se o tema falhar por qualquer motivo, o jogo abre normal.
            t.printStackTrace();
        }
        try {
            Tempo.liga();       // avisa as novidades do mod quando passa um dia/semana/temporada
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class.forName("best.h2", true, loader)
            .getMethod("main", String[].class)
            .invoke(null, (Object) args);
    }

    // O jogo le img/f01.jpg e img/f02.jpg do disco (fundo da partida, do intervalo etc.), nao do
    // .jar. Se a pasta ainda tem os fundos antigos, troca pelos do tema que vem dentro do .exe.
    static void atualizaFundos() {
        for (String n : new String[] {"f01.jpg", "f02.jpg"}) {
            try {
                java.io.InputStream in = Boot.class.getResourceAsStream("/mod/img/" + n);
                if (in == null) continue;
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                for (int k; (k = in.read(buf)) > 0; ) b.write(buf, 0, k);
                in.close();
                byte[] novo = b.toByteArray();
                java.io.File f = new java.io.File(System.getProperty("user.dir"), "img/" + n);
                if (f.isFile() && java.util.Arrays.equals(java.nio.file.Files.readAllBytes(f.toPath()), novo)) continue;
                f.getParentFile().mkdirs();
                java.nio.file.Files.write(f.toPath(), novo);
            } catch (Throwable t) {
                // sem permissao de escrita: fica o fundo que estiver la
            }
        }
    }
}
