package origami;

import blue.origami.asm.APIs;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserSource;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.transpiler.Transpiler;

//import junit.framework.Assert;
//import org.junit.Test;

public class ScriptTest {

	public void testTest() {
		assert (true);
	}

	public void testHello() throws Throwable {
		runScript("/konoha5-test/hello.k", 0);
	}

	public void testMax() throws Throwable {
		runScript("/konoha5-test/max.k", 0);
	}

	public void testFact() throws Throwable {
		runScript("/konoha5-test/fact.k", 0);
	}

	public void testTake() throws Throwable {
		runScript("/konoha5-test/take.k", 0);
	}

	// public void testFib() throws Throwable {
	// runScript("/konoha5-test/fib.k", 0);
	// }

	public void testMutualRecursion() throws Throwable {
		runScript2("/konoha5-test/mutual_recursion.k", 0);
	}

	//
	static Grammar g = null;
	static Parser p = null;

	static Grammar g() throws Throwable {
		if (g == null) {
			g = SourceGrammar.loadFile("/blue/origami/grammar/konoha5.opeg");
		}
		return g;
	}

	static Parser p() throws Throwable {
		if (p == null) {
			p = g().newParser();
		}
		return p;
	}

	public static void runScript(String file, int pass) throws Throwable {
		Transpiler env = new Transpiler(g(), p(), "jvm", null);
		APIs.resetCount();
		env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
		if (pass > 0) {
			assert (APIs.getPassCount() == pass);
		} else {
			assert ((APIs.getTestCount() - APIs.getPassCount()) == -pass);
		}
	}

	public static void runScript2(String file, int pass) throws Throwable {
		Transpiler env = new Transpiler(g(), p(), "jvm", null);
		try {
			env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
