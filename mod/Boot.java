package mod;

import java.io.InputStreamReader;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

// Ponto de entrada do mod: roda mod/red.js (Nashorn), que aplica o tema
// e depois chama o main original do jogo (best.h2).
public class Boot {
    public static void main(String[] args) throws Exception {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("nashorn");
        js.put("args", args);
        js.eval(new InputStreamReader(Boot.class.getResourceAsStream("/mod/red.js"), "UTF-8"));
    }
}
