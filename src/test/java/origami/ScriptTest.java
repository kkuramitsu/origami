package origami;

import blue.origami.nez.parser.ParserSource;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.asm.APIs;

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
		runScript("/konoha5-test/hello.k", 0);
	}

	public void testMax() throws Throwable {
		runScript("/konoha5-test/max.k", 0);
	}

	public void testFact() throws Throwable {
		runScript("/konoha5-test/fact.k", 0);
	}

	public void testMutualRecursion() throws Throwable {
		runScript("/konoha5-test/mutual_recursion.k", 0);
	}

	//
	public static void runScript(String file, int pass) throws Throwable {
		Grammar g = SourceGrammar.loadFile("/blue/origami/grammar/konoha5.opeg");
		Transpiler env = new Transpiler(g, "jvm");
		APIs.resetCount();
		env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
		if (pass > 0) {
			assert (APIs.getPassCount() == pass);
		} else {
			assert ((APIs.getTestCount() - APIs.getPassCount()) == -pass);
		}
	}

}
