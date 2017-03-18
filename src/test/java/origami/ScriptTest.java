package origami;

import origami.nez.parser.CommonSource;
import origami.nez.parser.ParserFactory;
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
		String ext = CommonSource.extractFileExtension(file);
		Grammar g = Grammar.loadFile("/origami/grammar/" + ext + ".nez");
		Origami env = new Origami(g);
		env.loadScriptFile(CommonSource.newFileSource(ScriptTest.class, file, null));
	}
	
}
