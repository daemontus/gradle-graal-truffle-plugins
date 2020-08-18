package experiment;

import org.graalvm.polyglot.Context;

public final class CheckCompiler {

    public static void main(String[] args){
        try (Context context = Context.create()) {
            if (!context.getEngine().getImplementationName().startsWith("Graal")) {
                System.err.println("Running with: "+context.getEngine().getImplementationName());
                System.exit(100);
            } else {
                System.err.println("Running with: "+context.getEngine().getImplementationName());
            }
        }
    }

}