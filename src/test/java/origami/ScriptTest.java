package origami;

import origami.nez.ast.SourcePosition;
import origami.nez.parser.ParserSource;

import origami.nez.peg.Grammar;

//import junit.framework.Assert;
//import org.junit.Test;

public class ScriptTest {

	public void testTest() {
		assert(true);
	}

//	public void testTest2() {
//		assert(false);
//	}

	public void testHello() throws Throwable {
		runScript("/iroha-test/hello.iroha");
	}
	
	public static void runScript(String file) throws Throwable {
		String ext = SourcePosition.extractFileExtension(file);
		Grammar g = Grammar.loadFile("/origami/grammar/" + ext + ".nez");
		Origami env = new Origami(g);
		env.loadScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
	}
	
}
