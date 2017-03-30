package origami;

import blue.nez.ast.SourcePosition;
import blue.nez.parser.ParserSource;
import blue.nez.peg.Grammar;
import blue.origami.OrigamiContext;

//import junit.framework.Assert;
//import org.junit.Test;

public class ScriptTest {

	public void testTest() {
		assert (true);
	}

	// public void testTest2() {
	// assert(false);
	// }

	public void testHello() throws Throwable {
		runScript("/iroha-test/hello.iroha");
	}

	public static void runScript(String file) throws Throwable {
		String ext = SourcePosition.extractFileExtension(file);
		Grammar g = Grammar.loadFile("/blue/origami/grammar/" + ext + ".opeg");
		OrigamiContext env = new OrigamiContext(g);
		env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
	}

}
