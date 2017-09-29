package origami;

import blue.origami.nez.parser.Parser;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.SourceGrammar;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Transpiler;

public class EvalTest {

	public void testLiteral() throws Throwable {
		runScript("()", null);
		runScript("true", "true");
		runScript("false", "false");
		runScript("1", "1");
		runScript("1.0", "1.0");
		runScript("'a'", "a");
		runScript("'abc'", "abc");
		runScript("\"abc\"", "abc");
		runScript("[1,2,3]", "[1,2,3]");
		runScript("{1,2,3}", "{1,2,3}");
	}

	public void testBoolean() throws Throwable {
		runScript("true && true", "true");
		runScript("true || true", "true");
		runScript("!false", "true");
		runScript("!true", "false");
	}

	public void testInt() throws Throwable {
		runScript("-1", "-1");
		runScript("2+-3", "-1");
		runScript("1+2*3", "7");
		runScript("(1+2)*3", "9");
		runScript("7/2", "3");
		runScript("7%2", "1");
	}

	public void testBinary() throws Throwable {
		runScript("1+1.0", "2.0");
		runScript("1.0+1", "2.0");
	}

	public void testNumber() throws Throwable {
		runScript("0b10", "2");
		runScript("0_10", "8");
		runScript("0x10", "16");
	}

	public void testRec() throws Throwable {
		runScript("sum(a: Int) = if a == 0 then 0 else a + sum(a-1);sum(3)", "6");
		runScript("sum(a) = if a == 0 then 0 else a + sum(a-1);sum(3)", "6");
	}

	public void testConst() throws Throwable {
		runScript("a = 1\na", "1");
	}

	public void testIntList() throws Throwable {
		runScript("a=[1,2];a", "[1,2]");
		runScript("a=[1,2];a[1]", "2");
		runScript("a=[1,2].map(\\n n+1);a", "[2,3]");
		runScript("1::[]", "[1]");
	}

	public void testIntArray() throws Throwable {
		runScript("a={1,2};a[0]=9;a[0]", "9");
		runScript("a={1,2}.map(\\n n+1);a", "{2,3}");
	}

	public void testLambda() throws Throwable {
		runScript("(\\n n+1)(0)", "1");
	}

	public void testBlock() throws Throwable {
		runScript("{a=1;a}", "1");
		runScript("{a=1;b=a;a+b}", "2");
		runScript("{a=1;b={a=2;a};a+b}", "3");
		// {a=1;{b=a;b}}
	}

	public void testOption() throws Throwable {
		runScript("Some(1)", "1");
		runScript("Some(1) >>= (\\n Some(n+1))", "2");
	}

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

	//
	public static void runScript(String text, String checked) throws Throwable {
		Transpiler env = new Transpiler(g(), p(), "jvm", null);
		Object result = env.testEval(text);
		System.out.printf("%s %s => %s\n", TFmt.Checked, text, result);
		if (checked != null) {
			assert (checked.equals(result.toString())) : result + " != " + checked;
		}
	}

}
