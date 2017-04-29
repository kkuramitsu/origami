package origami;

import org.junit.Test;

import blue.nez.ast.SourcePosition;
import blue.nez.parser.ParserSource;
import blue.nez.peg.Grammar;
import blue.nez.peg.SourceGrammar;
import blue.origami.OrigamiContext;

public class ScriptTest {

    @Test
	public void testHello() throws Throwable {
		runScript("/iroha-test/hello.iroha");
	}

	private static void runScript(String file) throws Throwable {
		String ext = SourcePosition.extractFileExtension(file);
		Grammar g = SourceGrammar.loadFile("/blue/origami/grammar/" + ext + ".opeg");
		OrigamiContext env = new OrigamiContext(g);
		env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
	}

}
