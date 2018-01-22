package origami;

import blue.origami.Version;
import blue.origami.common.OConsole;
import blue.origami.parser.Parser;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;
import blue.origami.transpiler.Language;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Transpiler;

public class XEvalTest {

	public void testLiteral() throws Throwable {
		check("()", null);
		check("true", "true");
		check("false", "false");
		check("1", "1");
		check("1.0", "1.0");
		check("'a'", "a");
		check("'abc'", "abc");
		check("\"abc\"", "abc");
		check("[1,2,3]", "[1,2,3]");
		check("[false,true]", "[false,true]");
	}

	public void testBool() throws Throwable {
		check("true && true", "true");
		check("true || true", "true");
		check("!false", "true");
		check("!true", "false");
	}

	public void testInt() throws Throwable {
		check("-1", "-1");
		check("2+-3", "-1");
		check("1+2*3", "7");
		check("(1+2)*3", "9");
		check("7/2", "3");
		check("7%2", "1");
	}

	public void testBinary() throws Throwable {
		check("1+1.0", "2.0");
		check("1.0+1", "2.0");
	}

	public void testNumber() throws Throwable {
		check("0b10", "2");
		check("0_10", "8");
		check("0x10", "16");
	}

	public void testMathStyle() throws Throwable {
		check("n=2;nn", "4");
		check("n=2;2(n+1)", "6");
		check("n=2;n(n+1)", "6");
		check("n=2;(n+1)(n+1)", "9");
	}

	public void testRec() throws Throwable {
		check("sum(a: Int) = if a == 0 then 0 else a + sum(a-1);sum(3)", "6");
		check("sum(a) = if a == 0 then 0 else a + sum(a-1);sum(3)", "6");
	}

	public void testConst() throws Throwable {
		check("a = 1\na", "1");
	}

	public void testIntList() throws Throwable {
		check("a=[1,2];a", "[1,2]");
		check("a=[1,2];a[1]", "2");
		// a=[1,2].map(\n :Int n+1);a
		check("a=[1,2].map(\\n :Int n+1);a", "[2,3]");
		check("1::[]", "[1]");
	}

	public void testIntArray() throws Throwable {
		check("a$=[1,2];a$[0]=9;a$[0]", "9");
		check("a=[1,2].map(\\n n+1);a", "[2,3]");
	}

	public void testStringList() throws Throwable {
		check("a=[\"a\"];a", "[a]");
		check("a=[\"a\"].map(\\a :String |a|);a", "[1]");
		check("a=[\"a\"].map(\\a |a|);a", "[1]");
		// runScript("1::[]", "[1]");
	}

	public void testLambda() throws Throwable {
		check("(\\n:Int n+1)(0)", "1");
		check("f = \\n:Int n+1;f(0)", "1");
		check("f(m:Int) = \\n:Int m+n;f(1)(2)", "3");
	}

	public void testBlock() throws Throwable {
		check("{a=1;a}", "1");
		check("{a=1;b=a;a+b}", "2");
		check("{a=1;b={a=2;a};a+b}", "3");
		// {a=1;{b=a;b}}
	}

	public void testBind() throws Throwable {
		check("f(a,n)={m=a[n];m};f([0,1], 0)", "0");
	}

	public void testTuple() throws Throwable {
		check("(1, true)", "(1,true)");
		check("a=(1, true);a", "(1,true)");
		check("(a,b)=(1, \"a\");a", "1");
		check("f(a,b)=(a,b);f(1,false)", "(1,false)");
	}

	// Some(1) >>= (\n Some(n+1))
	public void testOption() throws Throwable {
		check("Some(1)", "1");
		check("Some(1) >>= (\\n Some(n+1))", "2");
	}

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

	//
	public static void check(String text, String checked) throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		Object result = env.testEval(text);
		System.out.printf("%s %s => %s\n", TFmt.Checked, text, result);
		if (checked != null) {
			assert (checked.equals("" + result)) : result + " != " + checked;
		}
	}

	public static void FIXME(String text, String checked) throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		Object result = env.testEval(text);
		if (!checked.equals("" + result)) {
			result = OConsole.color(OConsole.Red, "" + result);
		}
		System.out.printf("%s %s => %s\n", TFmt.Checked, text, result);
	}

}
