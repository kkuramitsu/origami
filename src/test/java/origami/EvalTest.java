package origami;

import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.transpiler.Transpiler;

public class EvalTest {

	public void testLiteral() throws Throwable {
		runScript("()", "()");
		runScript("true", "Bool");
		runScript("false", "Bool");
		runScript("1", "Int");
		runScript("1.0", "Float");
		runScript("'a'", "Char");
		runScript("'abc'", "String");
		runScript("\"abc\"", "String");
		runScript("[1,2,3]", "Int*");
		runScript("{1,2,3}", "Int[]");
	}

	public void testBinary() throws Throwable {
		runScript("1+1.0", "Float");
		runScript("1.0+1", "Float");
	}

	public void testLet() throws Throwable {
		runScript("a = 1\na", "Int");
	}

	public void testLambda() throws Throwable {
		runScript("\\n n+1", "Int->Int");
	}

	//
	public static void runScript(String text, String checked) throws Throwable {
		Grammar g = SourceGrammar.loadFile("/blue/origami/grammar/konoha5.opeg");
		Transpiler env = new Transpiler(g, "jvm");
		Object result = env.testEval(text);
		System.out.printf("%s => %s\n", text, result);
		// if (checked != null) {
		// assert (checked.equals(ty.toString())) : ty + " != " + checked;
		// }
	}

}
