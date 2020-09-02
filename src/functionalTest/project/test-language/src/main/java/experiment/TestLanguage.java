package experiment;

import org.graalvm.polyglot.*;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;

@TruffleLanguage.Registration(
        characterMimeTypes = "text/plain",
        name = "test",
        id = "test"
)
public class TestLanguage extends TruffleLanguage<TestLanguage.State> {

    public static void main(String[] args) {
        Context ctx = Context.newBuilder()
                .allowAllAccess(true)
                .build();
        Value result = ctx.eval("test", args[0]);
        int four = result.execute(4).asInt();
        String hello = result.execute("hello").asString();
        if (four != 4 || !hello.equals("hello")) {
            System.err.println("Unexpected test output.");
            System.exit(100);
        }
    }

    @Override
    protected State createContext(Env env) {
        return new State();
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        String code = request.getSource().getCharacters().toString();
        if (code.equals("identity")) {
            return Truffle.getRuntime().createCallTarget(new TestLanguage.Root(this));
        } else {
            throw new RuntimeException("Only identity function is supported.");
        }
    }

    public static class State {}

    private static class Root extends RootNode {

        Root(TestLanguage language) {
            super(language);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return new Identity();
        }

    }

    @ExportLibrary(InteropLibrary.class)
    public static class Identity implements TruffleObject {

        @ExportMessage
        static boolean isExecutable(Identity receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(Identity receiver, Object[] arguments) {
            return arguments[0];
        }

    }

}