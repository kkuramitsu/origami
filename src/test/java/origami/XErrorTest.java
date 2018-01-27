package origami;

import blue.origami.Version;
import blue.origami.common.OConsole;
import blue.origami.common.OFormat;
import blue.origami.common.TLog;
import blue.origami.parser.Parser;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;
import blue.origami.transpiler.Language;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Transpiler;

public class XErrorTest {

	public void testUndefinedName() throws Throwable {
		check("abn", TFmt.undefined_name__YY1);
	}

	public void testMismatched() throws Throwable {
		check("1+true", TFmt.mismatched_SSS);
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
	public static void check(String text, OFormat fmt) throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		TLog logs = env.setLogger();
		env.testEval(text);
		TLog log = logs.find(fmt);
		if (log != null) {
			String ok = OConsole.color(OConsole.Blue, "" + log);
			System.out.printf("%s %s => %s\n", TFmt.Checked, text, ok);
		} else {
			String ng = OConsole.color(OConsole.Red, fmt.toString());
			logs.emit(TLog.Info, TLog::report);
			assert (false) : "(" + text + ")'s error != " + ng;
		}
	}

}