package origami;

import java.util.Objects;

import blue.origami.Version;
import blue.origami.common.OConsole;
import blue.origami.parser.Parser;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;
import blue.origami.transpiler.Language;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.type.Ty;

public class NameTest {

	public void testSingleName() throws Throwable {
		assert NameHint.isOneLetterName("a") : "a";
		assert NameHint.isOneLetterName("a2") : "a";
		assert NameHint.isOneLetterName("a123") : "a";
		assert NameHint.isOneLetterName("a'") : "a";
		assert NameHint.isOneLetterName("a''") : "a";
		assert NameHint.isOneLetterName("a$") : "a";
		assert NameHint.isOneLetterName("a?") : "a";
		assert NameHint.isOneLetterName("a$?") : "a";
		assert NameHint.isOneLetterName("a*") : "a";
		//
		assert NameHint.isOneLetterName("A");
		assert NameHint.isOneLetterName("α") : "α";
		assert !NameHint.isOneLetterName("aa");
		assert !NameHint.isOneLetterName("里");
		assert !NameHint.isOneLetterName("あ") : "あ";
	}

	public void testNameTest() throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		env.addNameHint("hoge", Ty.tList(Ty.tInt));
		this.check("n", env.findNameHint("n"), "Int");
		this.check("n'", env.findNameHint("n'"), "Int");
		this.check("ns", env.findNameHint("ns"), "List[Int]");
		this.check("ns'", env.findNameHint("ns'"), "List[Int]");
		this.check("hoge", env.findNameHint("hoge"), "List[Int]");
		this.check("shoge", env.findNameHint("shoge"), "List[Int]");
		this.check("firsthoge", env.findNameHint("firsthoge"), "List[Int]");
		this.check("left_hoge", env.findNameHint("left_hoge"), "List[Int]");
		this.check("rightHoge", env.findNameHint("rightHoge"), "List[Int]");
		this.check("rightHoges", env.findNameHint("rightHoges"), "List[List[Int]]");
	}

	public void testListNameTest() throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		env.addNameHint("num", Ty.tInt);
		this.check("num", env.findNameHint("num"), "Int");
		this.check("nums", env.findNameHint("nums"), "List[Int]");
		this.check("num*", env.findNameHint("num*"), "List[Int]");
	}

	public void testMutNameTest() throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		env.addNameHint("num", Ty.tInt);
		this.check("nums", env.findNameHint("nums"), "List[Int]");
		this.check("nums$", env.findNameHint("nums$"), "List[Int]$");
	}

	public void testOptionalNameTest() throws Throwable {
		Transpiler env = new Transpiler().initMe(g(), p(), new Language());
		env.addNameHint("linenum", Ty.tInt);
		this.check("linenum", env.findNameHint("linenum"), "Int");
		this.check("linenum?", env.findNameHint("linenum?"), "Option[Int]");
		this.check("linenums?", env.findNameHint("linenums?"), "Option[List[Int]]");
		this.check("linenums$?", env.findNameHint("linenums$?"), "Option[List[Int]$]");
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

	public void check(String p, Object o, Object o2) {
		if (!Objects.equals("" + o, "" + o2)) {
			OConsole.println(p + "  " + o + "" + OConsole.color(OConsole.Red, " != " + o2));
			assert (Objects.equals("" + o, "" + o2)) : p + "  " + o + "" + OConsole.color(OConsole.Red, " != " + o2);
		} else {
			OConsole.println(p + "  " + o + "" + OConsole.color(OConsole.Blue, " == " + o2));
		}
	}

}
