package origami.libnez;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

import origami.libnez.Expr.PTag;

public class PEG implements OStrings {
	PEG parent;
	String ns = "peg" + Objects.hashCode(this);
	ArrayList<String> pubList = new ArrayList<>();
	HashMap<String, Expr> prodMap = new HashMap<>();
	HashMap<String, Object> memoed = new HashMap<>();

	Expr get(String name) {
		Expr pe = this.prodMap.get(name);
		if (pe == null && this.parent != null) {
			return this.parent.get(name);
		}
		return pe;
	}

	void add(boolean export, boolean override, String name, Expr pe) {
		Expr p = this.prodMap.get(name);
		if (p == null) {
			if (export) {
				this.pubList.add(name);
			}
			this.prodMap.put(name, pe);
		} else if (override) {
			this.prodMap.put(name, pe);
		}
		if (this.parent != null && export) {
			this.parent.add(export, false, name, pe);
		}
	}

	PEG beginSection(String ns) {
		PEG child = new PEG();
		child.ns = ns;
		child.parent = this;
		return child;
	}

	PEG endSection() {
		return this.parent == null ? this : this.parent;
	}

	static boolean isPublicName(String name) {
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch == '_' || ch == '"') {
				return false;
			}
		}
		return true;
	}

	final static Expr Empty_ = new Empty();
	final static Expr Any_ = new Char(null);
	final static Expr Fail_ = new Not(new Empty());

	public static class Empty extends Expr {
		Empty() {
			this.ptag = PTag.Empty;
		}
	}

	public static class Char extends Expr {
		BitChar bc;

		public Char(BitChar ch) {
			this.ptag = PTag.Char;
			this.bc = ch == null ? BitChar.AnySet : ch;
		}

		public Char(byte b) {
			this(BitChar.byteChar(b));
		}

		public Char(byte b1, byte b2) {
			this(new BitChar(b1, b2));
		}

		@Override
		public Object param(int index) {
			return this.bc;
		}

		@Override
		public boolean isChar() {
			return true;
		}

		@Override
		public boolean isAny() {
			return this.bc.isAny();
		}

		@Override
		public Expr orElse(Expr pe) {
			if (pe instanceof Char) {
				BitChar bc = (BitChar) pe.param(0);
				return new Char(this.bc.union(bc));
			}
			return super.orElse(pe);
		}
	}

	public static class Seq extends Expr2 {
		public Seq(Expr left, Expr right) {
			this.ptag = PTag.Seq;
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean isStr() {
			Expr[] es = this.flatten(PTag.Seq);
			byte[] b = PEG.getstr2(es);
			return es.length == b.length;
		}
	}

	public static class Or extends Expr2 {
		public Or(Expr left, Expr right) {
			this.ptag = PTag.Or;
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean isOption() {
			if (this.right instanceof Or) {
				return this.right.isOption();
			}
			return this.right.isEmpty();
		}
	}

	public static class Alt extends Expr2 {
		public Alt(Expr left, Expr right) {
			this.ptag = PTag.Alt;
			this.left = left;
			this.right = right;
		}
	}

	public static class And extends Expr1 {
		public And(Expr inner) {
			this.ptag = PTag.And;
			this.inner = inner;
		}
	}

	public static class Not extends Expr1 {
		public Not(Expr inner) {
			this.ptag = PTag.Not;
			this.inner = inner;
		}

		@Override
		public Expr andThen(Expr pe) {
			if (this.get(0).isChar() && pe.isChar()) {
				BitChar bc1 = (BitChar) this.get(0).param(0);
				BitChar bc2 = (BitChar) pe.param(0);
				return new Char(bc1.not().and(bc2));
			}
			return super.andThen(pe);
		}
	}

	public static class Many extends Expr1 {
		public Many(Expr inner) {
			this.ptag = PTag.Many;
			this.inner = inner;
		}
	}

	public static class OneMore extends Expr1 {
		public OneMore(Expr inner) {
			this.ptag = PTag.OneMore;
			this.inner = inner;
		}
	}

	public static class Memoed extends ExprP {
		HashMap<String, Object> memoed;

		@Override
		public Object lookup(String key) {
			return this.memoed.get(key + this.label);
		}

		@Override
		public <V> V memo(String key, V u) {
			this.memoed.put(key + this.label, u);
			return u;
		}
	}

	public static class NonTerm extends Memoed {
		PEG peg;
		int index;

		public NonTerm(PEG peg, String name, int index) {
			this.ptag = PTag.NonTerm;
			this.peg = peg;
			this.label = name;
			this.index = index;
			this.memoed = peg.memoed;
		}

		@Override
		public Expr get(int index) {
			if (this.index == -1) {
				Expr deref = this.peg.prodMap.get(this.label);
				if (deref == null) {
					deref = Loader.s(this.label);
					this.peg.prodMap.put(this.label, deref);
				}
				return deref;
			}
			return null;
		}

		@Override
		public Object param(int index) {
			if (index == 0) {
				return this.label;
			}
			assert (index == 1);
			Expr inner = this.get(0);
			if (inner instanceof Param) {
				return ((Param) inner).args;
			}
			return null;
		}

		@Override
		public int psize() {
			return 2;
		}

	}

	/* Conditional */

	public static class If extends ExprP {
		public If(String label) {
			this.ptag = PTag.If;
			this.label = label;
		}
	}

	public static class On extends ExprP1 {
		public On(String label, Expr inner) {
			this.ptag = PTag.On;
			this.label = label;
			this.inner = inner;
		}
	}

	public static class Off extends ExprP1 {
		public Off(String label, Expr inner) {
			this.ptag = PTag.Off;
			this.label = label;
			this.inner = inner;
		}
	}

	/* State */

	public static class Scope extends Expr1 {
		public Scope(Expr inner) {
			this.ptag = PTag.Scope;
			this.inner = inner;
		}
	}

	public static class Symbol extends ExprP1 {
		public Symbol(Expr n) {
			this.ptag = PTag.Symbol;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Exists extends ExprP1 {
		public Exists(Expr n) {
			this.ptag = PTag.Exists;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Match extends ExprP1 {
		public Match(Expr n) {
			this.ptag = PTag.Match;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Equals extends ExprP1 {
		public Equals(Expr n) {
			this.ptag = PTag.Equals;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Contains extends ExprP1 {
		public Contains(Expr n) {
			this.ptag = PTag.Contains;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Eval extends ExprP {
		ParseFunc func;

		public Eval(ParseFunc func) {
			this.ptag = PTag.Eval;
			this.func = func;
		}

		@Override
		public Object param(int index) {
			return this.func;
		}
	}

	public static class Bugs extends ExprP {
		public Bugs(String fmt, Object... args) {
			this.ptag = PTag.Bugs;
			this.label = String.format(fmt, args);
		}
	}

	/* Showing */

	static void showing(boolean alwaysEnclosed, Expr pe, StringBuilder sb) {
		switch (pe.ptag) {
		case Empty:
			sb.append("''");
			break;
		case Char:
			sb.append(pe.param(0)); // FIXME
			break;
		case NonTerm:
			sb.append(pe.param(0));
			break;
		case Seq: {
			PTag tag = pe.get(0).ptag;
			enclosed(tag == PTag.Or || tag == PTag.Alt, pe.get(0), sb);
			sb.append(" ");
			tag = pe.get(1).ptag;
			enclosed(tag == PTag.Or || tag == PTag.Alt, pe.get(1), sb);
			break;
		}
		case Or:
			if (pe.get(1).isEmpty()) {
				enclosed(pe.get(0) instanceof Expr2, pe.get(0), sb);
				sb.append("?");
				break;
			} else {
				PTag tag = pe.get(0).ptag;
				enclosed(tag == PTag.Alt, pe.get(0), sb);
				sb.append(" / ");
				tag = pe.get(1).ptag;
				enclosed(tag == PTag.Alt, pe.get(1), sb);
				break;
			}
		case Alt: {
			pe.strOut(sb);
			sb.append(" | ");
			pe.strOut(sb);
			break;
		}
		case And:
			sb.append("&");
			enclosed(pe.get(0) instanceof Expr2, pe.get(0), sb);
			break;
		case Not:
			sb.append("!");
			enclosed(pe.get(0) instanceof Expr2, pe.get(0), sb);
			break;
		case Many:
			enclosed(pe.get(0) instanceof Expr2, pe.get(0), sb);
			sb.append("*");
			break;
		case OneMore:
			enclosed(pe.get(0) instanceof Expr2, pe.get(0), sb);
			sb.append("+");
			break;
		/* */
		case Tree:
			sb.append("{");
			pe.get(0).strOut(sb);
			sb.append("}");
			break;
		case Link:
			showingAsFunc("$" + pe.param(0), null, pe.get(0), sb);
			break;
		case Fold:
			sb.append("{$" + pe.param(0) + " ");
			pe.get(0).strOut(sb);
			sb.append("}");
			break;
		case Tag:
			sb.append("#" + pe.param(0));
			break;
		case Val:
			sb.append("`" + pe.param(0) + "`");
			break;
		case Untree:
			sb.append("@untree(" + pe.get(0) + ")");
			break;
		/* */
		case Var:
			sb.append(pe.param(0));
			break;
		case App:
			showingAsFunc(pe.get(0).toString(), null, pe.get(1), sb);
			break;

		/* */
		case Scope: /* @symbol(A) */
			showingAsFunc("@scope", null, pe.get(0), sb);
			break;
		case Symbol: /* @symbol(A) */
			showingAsFunc("@symbol", null, pe.get(0), sb);
			break;
		case Contains:
			showingAsFunc("@contains", null, pe.get(0), sb);
			break;
		case Equals:
			showingAsFunc("@equals", null, pe.get(0), sb);
			break;
		case Exists:
			showingAsFunc("@exists", null, pe.get(0), sb);
			break;
		case Match:
			showingAsFunc("@match", null, pe.get(0), sb);
			break;
		case Eval:
			sb.append("@eval(" + pe.param(0) + ")");
			break;
		/* */
		case If: /* @if(flag) */
			sb.append("@if(" + pe.param(0) + ")");
			break;
		case On: /* @on(f, ) */
			showingAsFunc("@on", "" + pe.param(0), pe.get(0), sb);
			break;
		case Off: /* @on(!f, e) */
			showingAsFunc("@on", "!" + pe.param(0), pe.get(0), sb);
			break;
		case DFA:
			pe.strOut(sb);
			break;
		default:
			sb.append("@TODO(" + pe.ptag + ")");
			break;
		}
	}

	private static void enclosed(boolean enclosed, Expr pe, StringBuilder sb) {
		if (enclosed) {
			sb.append("(");
			pe.strOut(sb);
			sb.append(")");
		} else {
			pe.strOut(sb);
		}
	}

	private static void showingAsFunc(String func, Object param, Expr pe, StringBuilder sb) {
		sb.append(func);
		sb.append("(");
		if (param != null) {
			sb.append(param);
			sb.append(", ");
		}
		pe.strOut(sb);
		sb.append(")");
	}

	static byte[] getstr2(Expr[] es) {
		int len = 0;
		for (int i = 0; i < es.length; i++) {
			if (es[i].isChar() && ((BitChar) es[i].param(0)).isSingle()) {
				len = i + 1;
				continue;
			}
			break;
		}
		byte[] b = new byte[len];
		for (int i = 0; i < len; i++) {
			b[i] = ((BitChar) es[i].param(0)).single();
		}
		return b;
	}

	static Expr seq(int offset, int endindex, Expr... es) {
		assert (offset <= endindex);
		if (endindex == offset) {
			return Empty_;
		}
		if (offset + 1 == endindex) {
			return es[offset];
		}
		return es[offset].andThen(seq(offset + 1, endindex, es));
	}

	static Expr dup(Expr pe, Function<Expr, Expr> f) {
		switch (pe.ptag) {
		case Empty:
		case Char:
		case NonTerm:
		case Tag:
		case Val:
		case Eval:
		case If:
			return pe;
		case Seq:
			return f.apply(pe.get(0)).andThen(f.apply(pe.get(1)));
		case Or:
			return f.apply(pe.get(0)).orElse(f.apply(pe.get(1)));
		case Alt:
			return new Alt(f.apply(pe.get(0)), f.apply(pe.get(1)));
		case And:
			return new And(f.apply(pe.get(0)));
		case Not:
			return new Not(f.apply(pe.get(0)));
		case Many:
			return new Many(f.apply(pe.get(0)));
		case OneMore:
			return new OneMore(f.apply(pe.get(0)));
		/* */
		case Tree:
			return pe.dup(null, f.apply(pe.get(0)));
		case Link:
			return new TPEG.Link(pe.p(0), f.apply(pe.get(0)));
		case Fold:
			return pe.dup(pe.p(0), f.apply(pe.get(0)));
		/* */
		case Scope:
			return new Scope(f.apply(pe.get(0)));
		case Symbol:
			return new Symbol(f.apply(pe.get(0)));
		case Contains:
			return new Contains(f.apply(pe.get(0)));
		case Equals:
			return new Equals(f.apply(pe.get(0)));
		case Exists:
			return new Exists(f.apply(pe.get(0)));
		case Match:
			return new Match(f.apply(pe.get(0)));
		/* */
		case On:
			return new On((String) pe.param(0), f.apply(pe.get(0)));
		case Off:
			return new On((String) pe.param(0), f.apply(pe.get(0)));
		case DFA:
			return new DFA(((DFA) pe).charMap, dup(((DFA) pe).indexed, f));
		default:
			System.err.println("@TODO(dup, " + pe + ")");
			break;
		}
		return pe;
	}

	static Expr[] dup(Expr[] es, Function<Expr, Expr> f) {
		return Arrays.stream(es).map(p -> f.apply(p)).toArray(Expr[]::new);
	}

	static Expr dup2(Expr pe, Function<Expr, Expr> f) {
		Expr pe2 = dup(pe, f);
		if (pe2.value != pe.value) {
			pe2.value = pe.value;
		}
		return pe2;
	}

	/* Grammar Interface */

	PEG newGrammar() {
		return new PEG();
	}

	public PEG load(String path) throws IOException {
		File f = new File(path);
		InputStream sin = f.isFile() ? new FileInputStream(path) : PEG.class.getResourceAsStream(path);
		if (sin == null) {
			throw new FileNotFoundException(path);
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(sin, "UTF8"));
		StringBuilder sb = new StringBuilder();
		try {
			String s = null;
			while ((s = in.readLine()) != null) {
				sb.append(s);
				sb.append("\n");
			}
		} finally {
			in.close();
		}
		PEG peg = this.newGrammar();
		Loader gl = new Loader(peg);
		gl.setBasePath(path);
		gl.load(sb.toString());
		return peg;
	}

	public Parser getParser() {
		return this.getParser(this.pubList.get(0));
	}

	public Parser getParser(String start) {
		String key = start + "@";
		Parser p = (Parser) this.memoed.get(key);
		if (p == null) {
			Optimizer gen = new Optimizer();
			p = gen.generate(start, this.prodMap.get(start), new ParserFuncGenerator());
			this.memoed.put(key, p);
		}
		return p;
	}

	public void log(String fmt, Object... args) {
		System.err.printf(fmt + "%n", args);
	}

	// public static <X> void testExpr(String expr, Function<Expr, X> f, X result) {
	// PEG peg = new PEG();
	// X r = f.apply(p(peg, new String[0], expr));
	// if (result == null) {
	// System.out.printf("%s <- %s\n", expr, r);
	// } else if (!r.equals(result)) {
	// System.err.printf("%s <- %s != %s\n", expr, r, result);
	// }
	// }
	//
	// public static void testMatch(String expr, String... args) throws Throwable {
	// PEG peg = new PEG();
	// if (expr.startsWith("/") || expr.endsWith(".opeg")) {
	// peg.load(expr);
	// } else {
	// GrammarLoader.def(peg, expr);
	// }
	// Parser p = peg.getParser();
	// for (int i = 0; i < args.length; i += 2) {
	// String r = p.parse(args[i]).toString();
	// if (r.equals(args[i + 1])) {
	// System.out.printf("[succ] %s %s => %s\n", expr, args[i], r);
	// } else {
	// System.err.printf("[fail] %s %s => %s != %s\n", expr, args[i], r, args[i +
	// 1]);
	// }
	// }
	// }

	public void testMatch(String start, String... args) throws Throwable {
		Parser p = this.getParser(start);
		for (int i = 0; i < args.length; i += 2) {
			String r = p.parse(args[i]).toString();
			if (r.equals(args[i + 1])) {
				System.out.printf("[succ] %s %s => %s\n", start, args[i], r);
			} else {
				System.err.printf("[fail] %s %s => %s != %s\n", start, args[i], r, args[i + 1]);
			}
		}
	}

	public static void main(String[] a) throws Throwable {
		// testExpr("!'a' .", (e) -> e.toString(), "[\\x00-`b-\\xff]");
		// testExpr("{ {'a'} }", (e) -> Trees.checkAST(e).toString(), "{$({'a'})}");
		// testExpr("{ $('a') }", (e) -> Trees.checkAST(e).toString(), "{$({'a'})}");
		// testExpr("{ $a }", (e) -> Trees.checkAST(e).toString(), "{$({a})}");
		// testExpr("{ ({a})* }", (e) -> Trees.checkAST(e).toString(), "{$({a})*}");
		// testExpr("{a} {a}", (e) -> Trees.checkAST(e).toString(), "{a} {$ a}");

		// /* Empty */
		// testMatch("A=''", "", "[# '']", "a", "[# '']");
		// /* Char */
		// testMatch("A='a'", "aa", "[# 'a']", "b", "[#err* '']");
		//
		// /* Or */
		// testMatch("A=a/aa", "aa", "[# 'a']", "a", "[# 'a']");
		// testMatch("A=ab/aa", "aa", "[# 'aa']", "ab", "[# 'ab']");
		// /* Option */
		// testMatch("A=a a?", "aa", "[# 'aa']", "ab", "[# 'a']");
		// testMatch("A=ab ab?", "abab", "[# 'abab']", "ab", "[# 'ab']");
		/* Many */
		// testMatch("A=a*", "aa", "[# 'aa']", "ab", "[# 'a']", "b", "[# '']");
		// testMatch("A=ab*", "abab", "[# 'abab']", "aba", "[# 'ab']");

		// testMatch("A={a #Hoge}", "aa", "[#Hoge 'a']");
		// testMatch("HIRA = [あ-を]", "ああ", "[# 'あ']", "を", "[# 'を']");
		//
		// testMatch(
		// "UTF8 = [\\x00-\\x7F] / [\\xC2-\\xDF] [\\x80-\\xBF] / [\\xE0-\\xEF]
		// [\\x80-\\xBF] [\\x80-\\xBF] / [\\xF0-\\xF7] [\\x80-\\xBF] [\\x80-\\xBF]
		// [\\x80-\\xBF]",
		// "aa", "[# 'a']", "ああ", "[# 'あ']");
		//
		// testMatch("/blue/origami/grammar/math.opeg", //
		// "1", "[#IntExpr '1']", //
		// "1+2", "[#AddExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]", //
		// "1+2*3", "[#AddExpr $right=[#MulExpr $right=[#IntExpr '3'] $left=[#IntExpr
		// '2']] $left=[#IntExpr '1']]", //
		// "1*2+3",
		// "[#AddExpr $right=[#IntExpr '3'] $left=[#MulExpr $right=[#IntExpr '2']
		// $left=[#IntExpr '1']]]");
		// testMatch("/blue/origami/grammar/xml.opeg", //
		// "<a/>", "[#Element $key=[#Name 'a']]", "<a></a>", "[#Element $key=[#Name
		// 'a']]");
		PEG peg = Loader.nez();
		System.out.println(peg);
		// peg.testMatch2("A = a A / ''", "?");
		// peg.testMatch2("Production", "A = a", "?");
		// peg.testMatch2("NonTerminal", "a", "[#Name 'a']");
		// peg.testMatch2("Term", "a", "[#Name 'a']");
		peg.testMatch("Expression", "''", "[#Char '']");
		peg.testMatch("Expression", "'a'", "[#Char 'a']");
		peg.testMatch("Expression", "\"a\"", "[#Name '\"a\"']");
		peg.testMatch("Expression", "[a]", "[#Class 'a']");
		peg.testMatch("Expression", "f(a)", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "f(a,b)", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "<f a>", "[#Func $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "<f a b>", "[#Func $=[#Name 'b'] $=[#Name 'a'] $=[#Name 'f']]");
		peg.testMatch("Expression", "&a", "[#And $=[#Name 'a']]");
		peg.testMatch("Expression", "!a", "[#Not $=[#Name 'a']]");
		peg.testMatch("Expression", "a?", "[#Option $=[#Name 'a']]");
		peg.testMatch("Expression", "a*", "[#Many $=[#Name 'a']]");
		peg.testMatch("Expression", "a+", "[#OneMore $=[#Name 'a']]");
		peg.testMatch("Expression", "{a}", "[#Tree $=[#Name 'a']]");
		peg.testMatch("Expression", "{$ a}", "[#Fold $=[#Name 'a']]");
		peg.testMatch("Expression", "$a", "[#Let $=[#Name 'a']]");
		peg.testMatch("Expression", "$(a)", "[#Let $=[#Name 'a']]");
		peg.testMatch("Expression", "$(name=)a", "[#Let $=[#Name 'a'] $=[#Name 'name']]");

		peg.testMatch("Expression", "a a", "[#Seq $=[#Name 'a'] $=[#Name 'a']]");
		peg.testMatch("Expression", "a b c", "[#Seq $=[#Seq $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
		peg.testMatch("Expression", "a/b / c", "[#Or $=[#Or $=[#Name 'c'] $=[#Name 'b']] $=[#Name 'a']]");
	}

	@Override
	public void strOut(StringBuilder sb) {
		for (String name : this.pubList) {
			sb.append(name);
			sb.append("=");
			sb.append(this.prodMap.get(name));
			sb.append(System.lineSeparator());
		}
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}
}
