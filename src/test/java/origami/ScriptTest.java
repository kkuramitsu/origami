package origami;

import blue.origami.Version;
import blue.origami.asm.APIs;
import blue.origami.parser.Parser;
import blue.origami.parser.ParserSource;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;
import blue.origami.transpiler.Transpiler;

//import junit.framework.Assert;
//import org.junit.Test;

public class ScriptTest {

	public void testTest() {
		assert (true);
	}

	public void testHello() throws Throwable {
		runScript("/chibi-test/hello.chibi", 0);
	}

	public void testMax() throws Throwable {
		runScript("/chibi-test/max.chibi", 0);
	}

	public void testFact() throws Throwable {
		runScript("/chibi-test/fact.chibi", 0);
	}

	public void testTake() throws Throwable {
		runScript("/chibi-test/take.chibi", 0);
	}

	// public void testFib() throws Throwable {
	// runScript("/konoha5-test/fib.k", 0);
	// }

	public void testMutualRecursion() throws Throwable {
		runScript("/chibi-test/mutual_recursion.chibi", 0);
	}

	//
	static Grammar g = null;
	static Parser p = null;

	static Grammar g() throws Throwable {
		if (g == null) {
			g = SourceGrammar.loadFile(Version.ResourcePath + "/grammar/chibi.opeg");
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
		Transpiler env = new Transpiler(g(), p());
		APIs.resetCount();
		env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
		if (pass > 0) {
			assert (APIs.getPassCount() == pass);
		} else {
			assert ((APIs.getTestCount() - APIs.getPassCount()) == -pass);
		}
	}

	public static void runScript2(String file, int pass) throws Throwable {
		Transpiler env = new Transpiler(g(), p());
		try {
			env.testScriptFile(ParserSource.newFileSource(ScriptTest.class, file, null));
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
