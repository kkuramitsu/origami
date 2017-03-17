package origami;

import origami.nez.ast.Source;
import origami.nez.parser.CommonSource;
import origami.nez.parser.ParserFactory;
import origami.nez.peg.OGrammar;

//import junit.framework.Assert;
//import org.junit.Test;

public class OrigamiTest {

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
		ParserFactory fac = new ParserFactory();
		String ext = CommonSource.extractFileExtension(file);
		OGrammar g = OGrammar.loadFile("/origami/grammar/" + ext + ".nez");
		Origami env = new Origami(g);
		env.loadScriptFile(CommonSource.newFileSource(OrigamiTest.class, file, null));
	}
	
}
