package origami.nez2;

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
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import origami.main.Main;
import origami.nez2.Expr.PTag;
import origami.nez2.TPEG.OptimizedTree;
import origami.nez2.TPEG.Val;

public class PEG implements OStrings {
	static int serialId = 0;
	PEG parent;
	String ns = "peg" + serialId++;
	ArrayList<String> pubList = new ArrayList<>();
	HashMap<String, Expr> prodMap = new HashMap<>();
	HashMap<String, Object> memoed = new HashMap<>();

	public void forEach(Consumer<String> c) {
		for (int i = 0; i < this.pubList.size(); i++) {
			c.accept(this.pubList.get(i));
		}
	}

	public Expr get(String name) {
		Expr pe = this.prodMap.get(name);
		if (pe == null && this.parent != null) {
			return this.parent.get(name);
		}
		return pe;
	}

	public void set(String name, Expr pe) {
		this.prodMap.put(name, pe);
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
	final static Expr Fail_ = new Not(Empty_);
	final static Expr Any_ = new Char(null);
	final static Expr EOF_ = new Not(Any_);
	final static Expr[] Char_ = IntStream.range(0, 256).mapToObj(x -> new Char((byte) x, 1)).toArray(Expr[]::new);

	public static class Empty extends Expr {
		Empty() {
			this.ptag = PTag.Empty;
		}
	}

	public static class Char extends Expr {
		BitChar bc;

		Char(byte b, int a) {
			this(BitChar.byteChar(b));
		}

		public Char(BitChar ch) {
			this.ptag = PTag.Char;
			this.bc = ch == null ? BitChar.AnySet : ch;
		}

		public Char(byte b1, byte b2) {
			this(new BitChar(b1, b2));
		}

		@Override
		public BitChar bitChar() {
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
				BitChar bc = pe.bitChar();
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

		@Override
		public Expr car() {
			return this.left;
		}

		@Override
		public Expr cdr() {
			return this.right;
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
				BitChar bc1 = this.get(0).bitChar();
				BitChar bc2 = pe.bitChar();
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
				return deref instanceof Param ? deref.get(0) : deref;
			}
			return null;
		}

		@Override
		public String label() {
			return this.label;
		}

		public String uname() {
			return this.peg.ns + ":" + this.label;
		}

		@Override
		public int index() {
			return this.index;
		}

		@Override
		public String[] params() {
			if (this.index == -1) {
				Expr deref = this.peg.prodMap.get(this.label);
				if (deref instanceof Param) {
					return deref.params();
				}
			}
			return null;
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

	public static class State extends Expr1 {
		public State(Expr inner) {
			this.ptag = PTag.State;
			this.inner = inner;
		}
	}

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

		public Eval(String label, ParseFunc func) {
			this.ptag = PTag.Eval;
			this.label = label;
			this.func = func;
		}
	}

	@FunctionalInterface
	public interface UnaryFunc {
		boolean apply(ParserContext px, ParseFunc f);
	}

	public static class Unary extends ExprP1 {
		UnaryFunc func;

		public Unary(String label, Expr pe, UnaryFunc func) {
			this.ptag = PTag.Unary;
			this.label = label;
			this.inner = pe;
			this.func = func;
		}

		public static UnaryFunc cov(final int[] enterCounts, final int[] exitCounts, final int index) {
			return (px, f) -> {
				enterCounts[index]++;
				boolean b = f.apply(px);
				if (b) {
					exitCounts[index]++;
				}
				return b;
			};
		}
	}

	public static class Bugs extends ExprP {
		public Bugs(String fmt, Object... args) {
			this.ptag = PTag.Bugs;
			this.label = String.format(fmt, args);
		}

	}

	/* Showing */
	static void showing(StringBuilder sb, Expr pe) {
		switch (pe.ptag) {
		case Empty:
			sb.append("''");
			break;
		case Char:
			sb.append(pe.bitChar()); // FIXME
			break;
		case NonTerm:
			sb.append(pe.label());
			if (pe instanceof NonTerm) {
				showingSuffix((NonTerm) pe, sb);
			}
			break;
		case Seq:
			showingInner(isOr(pe.get(0)), pe.get(0), sb);
			sb.append(" ");
			showingInner(isOr(pe.get(1)), pe.get(1), sb);
			break;
		case Or:
			if (pe.isOption()) {
				showingInner(isBinary(pe.get(0)), pe.get(0), sb);
				sb.append("?");
				break;
			} else {
				showingInner(isAlt(pe.get(0)), pe.get(0), sb);
				sb.append(" / ");
				showingInner(isAlt(pe.get(1)), pe.get(1), sb);
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
			showingInner(isBinary(pe.get(0)), pe.get(0), sb);
			break;
		case Not:
			sb.append("!");
			showingInner(isBinary(pe.get(0)), pe.get(0), sb);
			break;
		case Many:
			showingInner(isBinary(pe.get(0)), pe.get(0), sb);
			sb.append("*");
			break;
		case OneMore:
			showingInner(isBinary(pe.get(0)), pe.get(0), sb);
			sb.append("+");
			break;
		/* */
		case Tree:
			sb.append("{");
			pe.get(0).strOut(sb);
			showingTag((OptimizedTree) pe, sb);
			sb.append("}");
			showingSuffix((OptimizedTree) pe, sb);
			break;
		case Fold:
			sb.append("{$" + pe.label() + " ");
			pe.get(0).strOut(sb);
			sb.append("}");
			break;
		case Link:
			showingAsFunc("$" + pe.label(), sb, pe.get(0));
			break;
		case Tag:
			sb.append("#" + pe.label());
			break;
		case Val:
			sb.append("`" + pe.label() + "`");
			break;
		case Untree:
			sb.append("untree(" + pe.get(0) + ")");
			break;
		/* */
		case Var:
			sb.append(pe.label());
			break;
		case App:
			showingAsFunc(pe.get(0).toString(), sb, pe.get(1));
			break;

		/* */
		case Scope: /* @symbol(A) */
			showingAsFunc("block", sb, pe.get(0));
			break;
		case State: /* @symbol(A) */
			showingAsFunc("state", sb, pe.get(0));
			break;
		case Symbol: /* @symbol(A) */
			showingAsFunc("symbol", sb, pe.get(0));
			break;
		case Contains:
			showingAsFunc("contains", sb, pe.get(0));
			break;
		case Equals:
			showingAsFunc("equals", sb, pe.get(0));
			break;
		case Exists:
			showingAsFunc("exists", sb, pe.get(0));
			break;
		case Match:
			showingAsFunc("match", sb, pe.get(0));
			break;
		case Eval:
			sb.append(pe.label() + "()");
			break;
		/* */
		case If: /* @if(flag) */
			sb.append("if(" + pe.label() + ")");
			break;
		case On: /* @on(f, ) */
			showingAsFunc("on", sb, pe.label(), pe.get(0));
			break;
		case Off: /* @on(!f, e) */
			showingAsFunc("on", sb, "!" + pe.label(), pe.get(0));
			break;
		case DFA:
			pe.strOut(sb);
			break;
		default:
			sb.append("@TODO(" + pe.ptag + ")");
			break;
		}
	}

	private static void showingInner(boolean enclosed, Expr pe, StringBuilder sb) {
		if (enclosed) {
			sb.append("(");
			pe.strOut(sb);
			sb.append(")");
		} else {
			pe.strOut(sb);
		}
	}

	private static boolean isOr(Expr pe) {
		return (pe.ptag == PTag.Or && !pe.isOption() || pe.ptag == PTag.Alt);
	}

	private static boolean isAlt(Expr pe) {
		return (pe.ptag == PTag.Alt);
	}

	private static boolean isBinary(Expr pe) {
		return (pe.ptag == PTag.Or && !pe.isOption() || pe.ptag == PTag.Alt || pe.ptag == PTag.Seq);
	}

	private static void showingSuffix(NonTerm pe, StringBuilder sb) {

	}

	private static void showingTag(OptimizedTree pe, StringBuilder sb) {
		if (pe.epos != 0 || pe.spos != 0) {
			sb.append("[");
			sb.append(pe.spos);
			sb.append(",");
			sb.append(pe.epos);
			sb.append("]");
		}
	}

	private static void showingSuffix(OptimizedTree pe, StringBuilder sb) {
		if (pe.tag != null) {
			sb.append(" ");
			sb.append(pe.tag);
		}
		if (pe.val != null) {
			sb.append(" ");
			sb.append(new Val(pe.val));
		}
	}

	private static void showingAsFunc(String func, StringBuilder sb, Object... params) {
		sb.append(func);
		sb.append("(");
		int c = 0;
		for (Object p : params) {
			if (c > 0) {
				sb.append(",");
			}
			OStrings.append(sb, p);
			c++;
		}
		sb.append(")");
	}

	static byte[] getstr2(Expr[] es) {
		int len = 0;
		for (int i = 0; i < es.length; i++) {
			if (es[i].isChar() && es[i].bitChar().isSingle()) {
				len = i + 1;
				continue;
			}
			break;
		}
		byte[] b = new byte[len];
		for (int i = 0; i < len; i++) {
			b[i] = es[i].bitChar().single();
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

	static Expr compose(int offset, int endindex, BiFunction<Expr, Expr, Expr> f, Expr... es) {
		assert (offset < endindex);
		Expr tail = es[endindex - 1];
		for (int i = endindex - 2; i >= offset; i--) {
			tail = f.apply(es[i], tail);
		}
		return tail;
		// if (offset + 1 == endindex) {
		// return es[offset];
		// }
		// return f.apply(es[offset], compose(offset + 1, endindex, f, es));
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
			return dup2(pe, f, (e0, e1) -> e0.andThen(e1));
		case Or: {
			Expr[] es = pe.flatten(pe.tag());
			for (int i = 0; i < es.length; i++) {
				es[i] = f.apply(es[i]);
			}
			return PEG.compose(0, es.length, (e0, e1) -> e0.orElse(e1), es);
		}
		case Alt:
			return dup2(pe, f, (e0, e1) -> e0.orAlso(e1));
		case And:
			return dup1(pe, f, (e0) -> new And(e0));
		case Not:
			return dup1(pe, f, (e0) -> new Not(e0));
		case Many:
			return dup1(pe, f, (e0) -> new Many(e0));
		case OneMore:
			return dup1(pe, f, (e0) -> new OneMore(e0));
		/* */
		case Tree:
			return dup1(pe, f, (e0) -> pe.dup(e0));
		case Link:
			return dup1(pe, f, (e0) -> new TPEG.Link(pe.label(), e0));
		case Fold:
			return dup1(pe, f, (e0) -> pe.dup(e0));
		/* */
		case State:
			return dup1(pe, f, (e0) -> new State(e0));
		case Scope:
			return dup1(pe, f, (e0) -> new Scope(e0));
		case Symbol:
			return dup1(pe, f, (e0) -> new Symbol(e0));
		case Contains:
			return dup1(pe, f, (e0) -> new Contains(e0));
		case Equals:
			return dup1(pe, f, (e0) -> new Equals(e0));
		case Exists:
			return dup1(pe, f, (e0) -> new Exists(e0));
		case Match:
			return dup1(pe, f, (e0) -> new Match(e0));

		/* */
		case On:
			return new On(pe.label(), f.apply(pe.get(0)));
		case Off:
			return new On(pe.label(), f.apply(pe.get(0)));
		case DFA:
			return new DFA(((DFA) pe).charMap, dup(((DFA) pe).indexed, f));
		default:
			Hack.TODO("dup", pe);
			break;
		}
		return pe;
	}

	static Expr dup1(Expr pe, Function<Expr, Expr> f, Function<Expr, Expr> c) {
		Expr e0 = f.apply(pe.get(0));
		if (pe.get(0) == e0) {
			return pe;
		}
		return c.apply(e0);
	}

	static Expr dup2(Expr pe, Function<Expr, Expr> f, BiFunction<Expr, Expr, Expr> c) {
		Expr e0 = f.apply(pe.get(0));
		Expr e1 = f.apply(pe.get(1));
		if (pe.get(0) == e0 && pe.get(1) == e1) {
			return pe;
		}
		return c.apply(e0, e1);
	}

	static Expr[] dup(Expr[] es, Function<Expr, Expr> f) {
		return Arrays.stream(es).map(p -> f.apply(p)).toArray(Expr[]::new);
	}

	/* Grammar Interface */

	public void load(String path) throws IOException {
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
		Loader gl = new Loader(this);
		gl.setBasePath(path);
		gl.load(sb.toString());
	}

	public void define(String peg) throws IOException {
		Loader gl = new Loader(this);
		gl.load(peg);
	}

	public String getStart() {
		return this.pubList.size() == 0 ? "EMPTY" : this.pubList.get(0);
	}

	public Parser getParser() {
		return this.getParser(this.pubList.get(0));
	}

	public Parser getParser(String start) {
		String key = start + "@";
		Parser p = (Parser) this.memoed.get(key);
		if (p == null) {
			p = this.generate(start, new BasicGenerator());
			// p = this.generate(start, new ParserFuncGenerator());
			this.memoed.put(key, p);
		}
		return p;
	}

	public <X> X generate(String start, Generator<X> gen) {
		Optimizer optm = new Optimizer();
		Expr pe = this.prodMap.get(start);
		if (pe == null) {
			pe = PEG.Empty_;
		}
		return optm.generate(start, pe, gen);
	}

	public void log(String fmt, Object... args) {
		System.err.printf(fmt + "%n", args);
	}

	public static PEG nez() {
		return Loader.nez();
	}

	public void testMatch(String start, String... args) throws Throwable {
		Parser p = this.getParser(start);
		if (start.equals("A")) {
			start = this.get("A").toString();
		}
		for (int i = 0; i < args.length; i += 2) {
			String r = p.parse(args[i]).toString();
			if (i + 1 < args.length) {
				if (r.equals(args[i + 1])) {
					System.out.printf("[succ] %s:: %s => %s\n", start, args[i], r);
				} else {
					System.err.printf("[fail] %s:: %s => %s != %s\n", start, args[i], r, args[i + 1]);
					if (Hack.AssertMode) {
						assert r.equals(args[i + 1]);
					}
				}
			} else {
				System.out.printf("[TODO] %s:: %s => %s\n", start, args[i], r);
			}
		}
	}

	public static void main2(String[] a) throws Throwable {
		// testExpr("!'a' .", (e) -> e.toString(), "[\\x00-`b-\\xff]");
		// testExpr("{ {'a'} }", (e) -> Trees.checkAST(e).toString(), "{$({'a'})}");
		// testExpr("{ $('a') }", (e) -> Trees.checkAST(e).toString(), "{$({'a'})}");
		// testExpr("{ $a }", (e) -> Trees.checkAST(e).toString(), "{$({a})}");
		// testExpr("{ ({a})* }", (e) -> Trees.checkAST(e).toString(), "{$({a})*}");
		// testExpr("{a} {a}", (e) -> Trees.checkAST(e).toString(), "{a} {$ a}");

		PEG peg = Loader.nez();
		System.out.println(peg);
		peg.testMatch("Production", "A = a", "?");
		peg.testMatch("COMMENT", "//hoge\nhoge", "[# '//hoge']");
	}

	public static void main(String[] a) throws Throwable {
		Hack.AssertMode = false;

		PEG nez = PEG.nez();
		// TPEG.dump(nez);
		// PEG peg = new PEG();
		// peg.load("/blue/origami/grammar/math.opeg");
		// peg.testMatch("Expression", "1+2");
		// TPEG
		// Hack.testFunc("isTree", "T; T = {.}", e -> TPEG.isTree(e), "true");

		// Hack.testLoad("/blue/origami/grammar/xml.opeg");
		// Hack.testLoad("/blue/origami/grammar/js.opeg");
		// Hack.testLoad("/blue/origami/grammar/java.opeg");

		// Hack.testLoad2("/blue/origami/grammar/xml.opeg");
		// Hack.testLoad2("/blue/origami/grammar/js.opeg");
		// Hack.testLoad2("/blue/origami/grammar/java.opeg");

		// Main.testMain("example", "-g", "chibi.opeg");
		Main.testMain("example", "-g", "java.opeg");
		// Main.testMain("example", "-g", "js.opeg");
		Class<?> c = Main.class;
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

	@SuppressWarnings("unchecked")
	public <X> X getMemo(String key) {
		return (X) this.memoed.get(key);
	}

	void setMemo(String key, Object value) {
		this.memoed.put(key, value);
	}

}
