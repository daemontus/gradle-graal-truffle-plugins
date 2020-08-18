package experiment;

import org.graalvm.polyglot.Context;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public final class CheckJavaScript {

    public static void main(String[] args) throws ScriptException {
        // Run JavaScript using polyglot context:
        try (Context context = Context.create()) {
            boolean hasPolyglot = context.eval("js", "typeof Graal === \"object\"").asBoolean();
            if (!hasPolyglot) {
                System.err.println("GraalJS support not detected!");
                System.exit(100);
            }
        }
        // Run JavaScript using ScriptEngine API:
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
        if (engine == null) {
            System.err.println("No ScriptEngine support detected!");
            System.exit(101);
        }
        boolean hasPolyglot = (Boolean) engine.eval("typeof Graal === \"object\"");
        if (!hasPolyglot) {
            System.err.println("GraalJS support not detected!");
            System.exit(102);
        }
        System.out.println("Execution success.");
    }

}