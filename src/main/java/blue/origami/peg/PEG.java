package blue.origami.peg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import blue.origami.common.OConsole;
import blue.origami.common.OStrings;
import blue.origami.parser.nezcc.SourceGenerator;

public class PEG {
	ArrayList<String> prodList = new ArrayList<>();
	HashMap<String, Expr> prodMap = new HashMap<>();
	HashMap<String, String[]> paramMap = null;
	HashMap<String, Object> memoed = new HashMap<>();

	public static enum CTag {
		Empty, Char, Seq, Or, Alt, And, Not, Many, OneMore, NonTerm, // pattern
		Tree, Link, Fold, Tag, Val, Untree, // tree construction
		Scope, Symbol, Exists, Equals, Contains, Match, // state
		If, On, Off, // conditional parsing
		Var, App, // Higher ordered
		DFA, Bugs, Eval;
	}

	public static class Empty extends Expr {
		public Empty() {
			this.ctag = CTag.Empty;
		}
	}

	public static class Char extends Expr {
		BitChar bc;

		public Char(BitChar ch) {
			this.ctag = CTag.Char;
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
			this.ctag = CTag.Seq;
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean isStr() {
			Expr[] es = this.flatten();
			byte[] b = PEG.getstr2(es);
			return es.length == b.length;
		}
	}

	public static class Or extends Expr2 {
		public Or(Expr left, Expr right) {
			this.ctag = CTag.Or;
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
			this.ctag = CTag.Alt;
			this.left = left;
			this.right = right;
		}
	}

	public static class And extends Expr1 {
		public And(Expr inner) {
			this.ctag = CTag.And;
			this.inner = inner;
		}
	}

	public static class Not extends Expr1 {
		public Not(Expr inner) {
			this.ctag = CTag.Not;
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
			this.ctag = CTag.Many;
			this.inner = inner;
		}
	}

	public static class OneMore extends Expr1 {
		public OneMore(Expr inner) {
			this.ctag = CTag.OneMore;
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
			this.ctag = CTag.NonTerm;
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
					deref = s(this.label);
					this.peg.add(this.label, null, deref);
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
			return this.peg.getParam(this.label);
		}

		@Override
		public int psize() {
			return 2;
		}

	}

	/* Tree Construction */

	public static class Tree extends Expr1 {

		public Tree(Expr inner) {
			this.ctag = CTag.Tree;
			this.inner = inner;
		}

		@Override
		Expr dup(Object label, Expr... es) {
			return new Tree(es[0]);
		}
	}

	public static class Fold extends ExprP1 {
		public Fold(String label, Expr inner) {
			this.ctag = CTag.Fold;
			this.label = label;
			this.inner = inner;
		}

		@Override
		Expr dup(Object label, Expr... es) {
			return new Fold((String) label, es[0]);
		}
	}

	public static class Link extends ExprP1 {
		public Link(String label, Expr inner) {
			this.ctag = CTag.Link;
			this.label = label == null || label.length() == 0 ? blue.origami.peg.TreeNode.EmptyTag : label;
			this.inner = inner;
		}
	}

	public static class Tag extends ExprP {
		public Tag(String label) {
			this.ctag = CTag.Tag;
			this.label = label;
		}
	}

	public static class Val extends Expr {
		byte[] val;

		public Val(byte[] val) {
			this.ctag = CTag.Val;
			this.val = val;
		}
	}

	public static class Untree extends Expr1 {
		public Untree(Expr inner) {
			this.ctag = CTag.Untree;
			this.inner = inner;
		}
	}

	/* Conditional */

	public static class If extends ExprP {
		public If(String label) {
			this.ctag = CTag.If;
			this.label = label;
		}
	}

	public static class On extends ExprP1 {
		public On(String label, Expr inner) {
			this.ctag = CTag.On;
			this.label = label;
			this.inner = inner;
		}
	}

	public static class Off extends ExprP1 {
		public Off(String label, Expr inner) {
			this.ctag = CTag.Off;
			this.label = label;
			this.inner = inner;
		}
	}

	/* State */

	public static class Scope extends Expr1 {
		public Scope(Expr inner) {
			this.ctag = CTag.Scope;
			this.inner = inner;
		}
	}

	public static class Symbol extends ExprP1 {
		public Symbol(Expr n) {
			this.ctag = CTag.Symbol;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Exists extends ExprP1 {
		public Exists(Expr n) {
			this.ctag = CTag.Exists;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Match extends ExprP1 {
		public Match(Expr n) {
			this.ctag = CTag.Match;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Equals extends ExprP1 {
		public Equals(Expr n) {
			this.ctag = CTag.Equals;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Contains extends ExprP1 {
		public Contains(Expr n) {
			this.ctag = CTag.Contains;
			this.inner = n;
			this.label = n.toString();
		}
	}

	public static class Eval extends ExprP {
		ParserFunc func;

		public Eval(ParserFunc func) {
			this.ctag = CTag.Eval;
			this.func = func;
		}

		@Override
		public Object param(int index) {
			return this.func;
		}
	}

	public static class Bugs extends ExprP {
		public Bugs(String fmt, Object... args) {
			this.ctag = CTag.Bugs;
			this.label = String.format(fmt, args);
		}
	}

	/* Misc */

	public static class Expr implements OStrings {
		CTag ctag;
		Object value;

		public boolean eq(Expr pe) {
			return this.ctag == pe.ctag && this.toString().equals(pe.toString());
		}

		Expr dup(Object p, Expr... inners) {
			return this;
		}

		public int size() {
			return 0;
		}

		public Expr get(int index) {
			return null;
		}

		public int psize() {
			return 0;
		}

		public Object param(int index) {
			return null;
		}

		String p(int index) {
			return Objects.toString(this.param(index));
		}

		public Expr[] flatten() {
			return new Expr[] { this };
		}

		public final Object get() {
			return this.value;
		}

		public final Expr set(Object value) {
			this.value = value;
			return this;
		}

		public Object lookup(String key) {
			return null;
		}

		public <V> V memo(String key, V u) {
			return u;
		}

		@Override
		public void strOut(StringBuilder sb) {
			showing(false, this, sb);
		}

		@Override
		public final String toString() {
			return OStrings.stringfy(this);
		}

		public boolean isEmpty() {
			return this instanceof Empty;
		}

		public boolean isFail() {
			return this instanceof Not && this.get(0).isEmpty();
		}

		public boolean isChar() {
			return this instanceof Char;
		}

		public boolean isStr() {
			return false;
		}

		public boolean isNonTerm() {
			return this.ctag == CTag.NonTerm;
		}

		public boolean isAny() {
			return false;
		}

		public boolean isOption() {
			return false;
		}

		public Expr andThen(Expr pe) {
			if (pe == null || pe.isEmpty()) {
				return this;
			}
			return new Seq(this, pe);
		}

		public Expr orElse(Expr pe) {
			if (this.isEmpty() || this.isOption()) {
				return this;
			}
			return new Or(this, pe);
		}

		public Expr orAlso(Expr pe) {
			return new Alt(this, pe);
		}

		public boolean isNullable() {
			return !First.nonnull(this);
		}

		public Expr deref() {
			return this;
		}

		public boolean isApp() {
			return false;
		}

	}

	static class ExprP extends Expr {
		String label;

		@Override
		public Object param(int index) {
			return this.label;
		}

		@Override
		public int psize() {
			return 1;
		}

	}

	static class Expr1 extends Expr {
		Expr inner;

		@Override
		public int size() {
			return 1;
		}

		@Override
		public Expr get(int index) {
			return (index == 0) ? this.inner : null;
		}

	}

	static class ExprP1 extends Expr1 {
		String label;

		@Override
		public String param(int index) {
			return this.label;
		}

		@Override
		public int psize() {
			return 1;
		}
	}

	static class Expr2 extends Expr {
		Expr left;
		Expr right;

		@Override
		public int size() {
			return 2;
		}

		@Override
		public Expr get(int index) {
			switch (index) {
			case 0:
				return this.left;
			case 1:
				return this.right;
			default:
				return null;
			}
		}

		@Override
		public Expr[] flatten() {
			ArrayList<Expr> l = new ArrayList<>();
			this.listup(l);
			return l.toArray(new Expr[l.size()]);
		}

		private void listup(ArrayList<Expr> l) {
			if (this.left instanceof Expr2 && this.left.ctag == this.ctag) {
				((Expr2) this.left).listup(l);
			} else {
				l.add(this.left);
			}
			if (this.right instanceof Expr2 && this.right.ctag == this.ctag) {
				((Expr2) this.right).listup(l);
			} else {
				l.add(this.right);
			}
		}
	}

	/* Showing */

	static void showing(boolean alwaysEnclosed, Expr pe, StringBuilder sb) {
		switch (pe.ctag) {
		case Empty:
			sb.append("''");
			break;
		case Char:
			sb.append(pe.param(0)); // FIXME
			break;
		case NonTerm:
			sb.append(pe.param(0));
			break;
		case Seq:
			showingGroup(alwaysEnclosed, (Expr2) pe, " ", sb);
			break;
		case Or:
			showingGroup(alwaysEnclosed, (Expr2) pe, " / ", sb);
			break;
		case Alt:
			showingGroup(alwaysEnclosed, (Expr2) pe, " | ", sb);
			break;
		case And:
			sb.append("&");
			showing(true, pe.get(0), sb);
			break;
		case Not:
			sb.append("!");
			showing(true, pe.get(0), sb);
			break;
		case Many:
			showing(true, pe.get(0), sb);
			sb.append("*");
			break;
		case OneMore:
			showing(true, pe.get(0), sb);
			sb.append("+");
			break;
		/* */
		case Tree:
			sb.append("{");
			showing(false, pe.get(0), sb);
			sb.append("}");
			break;
		case Link:
			showingAsFunc("$" + pe.param(0), null, pe.get(0), sb);
			break;
		case Fold:
			sb.append("{");
			sb.append("$" + pe.param(0));
			sb.append(" ");
			showing(false, pe.get(0), sb);
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
			sb.append("@TODO(" + pe.ctag + ")");
			break;
		}
	}

	private static void showingGroup(boolean alwaysEnclosed, Expr2 pe, String delim, StringBuilder sb) {
		int c = 0;
		if (alwaysEnclosed) {
			sb.append("(");
		}
		for (Expr sub : pe.flatten()) {
			if (c > 0) {
				sb.append(delim);
			}
			showing(true, sub, sb);
			c++;
		}
		if (alwaysEnclosed) {
			sb.append(")");
		}
	}

	private static void showingAsFunc(String func, Object param, Expr pe, StringBuilder sb) {
		sb.append(func);
		sb.append("(");
		if (param != null) {
			sb.append(param);
			sb.append(", ");
		}
		showing(false, pe, sb);
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
		switch (pe.ctag) {
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
			return new Link(pe.p(0), f.apply(pe.get(0)));
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

	/* Parser Interface */

	static byte[] encode(String text) {
		if (text == null) {
			return new byte[0];
		}
		try {
			return text.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			OConsole.exit(1, e);
		}
		return text.getBytes();
	}

	static Expr p(PEG peg, String[] ns, String expr0) {
		String expr = expr0.trim();
		return cons(peg, ns, expr, '|', (e, e2) -> e.orAlso(e2), () -> {
			return cons(peg, ns, expr, '/', (e, e2) -> e.orElse(e2), () -> {
				return cons(peg, ns, expr, ' ', (e, e2) -> e.andThen(e2), () -> {
					return unary(peg, ns, expr);
				});
			});
		});
	}

	@FunctionalInterface
	static interface Cons {
		Expr apply(Expr e, Expr e2);
	}

	static Expr cons(PEG peg, String[] ns, String expr, char op, Cons f, Supplier<Expr> next) {
		int pos = flatIndexOf(expr, op);
		if (pos > 0) {
			return f.apply(p(peg, ns, expr.substring(0, pos)), p(peg, ns, expr.substring(pos + 1)));
		}
		return next.get();
	}

	final static Expr Empty_ = new Empty();
	final static Expr Any_ = new Char(null);
	final static Expr Fail_ = new Not(new Empty());

	static Expr newStateFunc(Expr nonTerm, Function<Expr, Expr> f) {
		return f.apply(nonTerm);
	}

	static Expr unary(PEG peg, String[] ns, String expr) {
		expr = expr.trim();
		int length = expr.length();
		if (length == 0) {
			return Empty_;
		}
		char start = expr.charAt(0);
		char end = expr.charAt(expr.length() - 1);
		if (start == '!') {
			return new Not(p(peg, ns, expr.substring(1)));
		}
		if (start == '&') {
			return new And(p(peg, ns, expr.substring(1)));
		}
		if (end == '*') {
			return new Many(p(peg, ns, expr.substring(0, expr.length() - 1)));
		}
		if (end == '+') {
			return new OneMore(p(peg, ns, expr.substring(0, expr.length() - 1)));
		}
		if (end == '?') {
			return new Or(p(peg, ns, expr.substring(0, expr.length() - 1)), Empty_);
		}
		if (start == '$') {
			if (end == ')') {
				String[] t = split2(expr.substring(1, expr.length() - 1), (s) -> s.indexOf('('));
				return new Link(t[0], p(peg, ns, t[1]));
			}
			return new Link("", p(peg, ns, expr.substring(1)));
		}
		if (end == ')') {
			if (start == '(') {
				return p(peg, ns, expr.substring(1, expr.length() - 1));
			}
			if (start == '@') {
				String[] t = split2(expr.substring(0, expr.length() - 1), (s) -> s.indexOf('('));
				switch (t[0]) {
				case "@if":
					return new If(t[1]);
				case "@on":
					String flag = left(t[1], ',');
					if (flag.startsWith("!")) {
						return new Off(flag.substring(1), p(peg, ns, right(t[1], ',')));
					}
					return new On(flag, p(peg, ns, right(t[1], ',')));
				case "@symbol":
					return newStateFunc(p(peg, ns, t[1]), Symbol::new);
				case "@match":
					return newStateFunc(p(peg, ns, t[1]), Match::new);
				case "@exists":
					return newStateFunc(p(peg, ns, t[1]), Exists::new);
				case "@equals":
					return newStateFunc(p(peg, ns, t[1]), Equals::new);
				case "@contains":
					return newStateFunc(p(peg, ns, t[1]), Contains::new);
				}
			}
			String[] t = split2(expr.substring(0, expr.length() - 1), (s) -> s.indexOf('('));
			Expr[] p = Arrays.stream(split(t[1], (s) -> flatIndexOf(s, ','))).map(s -> p(peg, ns, s))
					.toArray(Expr[]::new);
			return new App(p(peg, ns, t[0]), p);
		}
		if (start == '{' && end == '}') {
			if (expr.startsWith("{$")) {
				String[] t = split2(expr.substring(2, expr.length() - 1), (s) -> s.indexOf(' '));
				return new Fold(t[0], p(peg, ns, t[1]));
			}
			String s = expr.substring(1, expr.length() - 1);
			// System.err.println("@@@@ '" + s + "' @@@@ " + e);
			return new Tree(p(peg, ns, s));
		}
		if (start == '[' && end == ']') {
			return c(expr.substring(1, expr.length() - 1), 0);
		}
		if (start == '\'' && start == end) {
			return s(u(expr.substring(1, expr.length() - 1)));
		}
		if (start == '`' && start == end) {
			return new Val(encode(u(expr.substring(1, expr.length() - 1))));
		}
		if (start == '#') {
			return new Tag(expr.substring(1));
		}
		if (expr.equals(".")) {
			return Any_;
		}
		if (start == '"' && start == end) {
			expr = u(expr.substring(1, expr.length() - 1));
		}
		for (int i = 1; i < ns.length; i++) {
			if (ns[i].equals(expr)) {
				return new NonTerm(peg, expr, i - 0);
			}
		}
		return new NonTerm(peg, expr, -1);
	}

	static Expr s(String s) {
		byte[] b = encode(s);
		if (b.length == 0) {
			return Empty_;
		}
		return s(b, 0);
	}

	static Expr s(byte[] b, int offset) {
		Expr pe = new Char(b[offset]);
		return (offset + 1 < b.length) ? pe.andThen(s(b, offset + 1)) : pe;
	}

	static String u(String s) {
		StringBuilder sb = new StringBuilder();
		for (int p = 0; p < s.length(); p = charNext(s, p)) {
			sb.append(charAt(s, p));
		}
		return sb.toString();
	}

	private static char charAt(String expr, int p) {
		char c1 = expr.charAt(p);
		if (c1 == '\\') {
			c1 = expr.charAt(p + 1);
			switch (c1) {
			case '\\':
				return '\\';
			case 'n':
				return '\n';
			case 'r':
				return '\r';
			case 't':
				return '\n';
			case '0':
				return '\0';
			case 'x':
			case 'X':
				return hex2(expr.charAt(p + 2), expr.charAt(p + 3));
			case 'u':
			case 'U':
				return hex4(expr.charAt(p + 2), expr.charAt(p + 3), expr.charAt(p + 4), expr.charAt(p + 5));
			}
		}
		return c1;
	}

	private static int charNext(String expr, int p) {
		char c1 = expr.charAt(p);
		if (c1 == '\\') {
			c1 = expr.charAt(p + 1);
			if (c1 == 'x' || c1 == 'X') {
				return p + 4;
			}
			if (c1 == 'u' || c1 == 'U') {
				return p + 6;
			}
			return p + 2;
		}
		return p + 1;
	}

	private static int hex(char c) {
		if ('0' <= c && c <= '9') {
			return c - '0';
		}
		if ('a' <= c && c <= 'f') {
			return c - 'a' + 10;
		}
		if ('A' <= c && c <= 'F') {
			return c - 'A' + 10;
		}
		return 0;
	}

	private static char hex2(char ch1, char ch2) {
		int c = hex(ch1);
		c = (c * 16) + hex(ch2);
		return (char) c;
	}

	private static char hex4(char ch1, char ch2, char ch3, char ch4) {
		int c = hex(ch1);
		c = (c * 16) + hex(ch2);
		c = (c * 16) + hex(ch3);
		c = (c * 16) + hex(ch4);
		return (char) c;
	}

	private static Expr c(String expr, int p) {
		char c1 = charAt(expr, p);
		p = charNext(expr, p);
		Expr pe;
		if (p < expr.length() && expr.charAt(p) == '-') {
			char c2 = charAt(expr, p + 1);
			pe = range(c1, c2);
			p = charNext(expr, p + 1);
		} else {
			pe = s(String.valueOf(c1));
		}
		return p < expr.length() ? pe.orElse(c(expr, p)) : pe;
	}

	private static Expr range(char c1, char c2) {
		if (c1 == c2) {
			return s(String.valueOf(c1));
		}
		byte[] b1 = encode(String.valueOf(c1));
		byte[] b2 = encode(String.valueOf(c2));
		assert (b1.length == b2.length);
		switch (b1.length) {
		case 1:
			return new Char(b1[0], b2[0]);
		case 2:
			assert (b1[0] == b2[0]);
			return (new Char(b1[0])).andThen(new Char(b1[1], b2[1]));
		case 3:
			assert (b1[0] == b2[0]);
			assert (b1[1] == b2[1]);
			return new Char(b1[0]).andThen(new Char(b1[1]).andThen(new Char(b1[2], b2[2])));
		}
		throw new RuntimeException("UnsupportedException");
	}

	static String[] split(String s, Function<String, Integer> f) {
		String[] t = split2(s, f);
		if (t.length == 1) {
			return t;
		}
		String[] t2 = split2(s, f);
		if (t2.length == 1) {
			return t;
		}
		String[] t3 = new String[1 + t2.length];
		t3[0] = t[0];
		System.arraycopy(t2, 0, t3, 1, t2.length);
		return t3;
	}

	static String[] split2(String t, Function<String, Integer> f) {
		int p = f.apply(t);
		if (p >= 0) {
			return new String[] { t.substring(0, p).trim(), t.substring(p + 1).trim() };
		}
		return new String[] { t };
	}

	static String left(String t, char c) {
		int p = t.indexOf(c);
		return t.substring(0, p).trim();
	}

	static String right(String t, char c) {
		int p = t.indexOf(c);
		return t.substring(p + 1).trim();
	}

	static int flatIndexOf(String expr, char c) {
		int level = 0;
		for (int i = 0; i < expr.length(); i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && level == 0) {
				return i;
			}
			if (c0 == '(' || c0 == '{') {
				level++;
				continue;
			}
			if (c0 == ')' || c0 == '}') {
				level--;
				continue;
			}
			if (c0 == '\'') {
				i = skip(expr, i + 1, '\'');
				continue;
			}
			if (c0 == '[') {
				i = skip(expr, i + 1, ']');
				continue;
			}
			if (c0 == '"') {
				i = skip(expr, i + 1, '"');
				continue;
			}
			if (c0 == '`') {
				i = skip(expr, i + 1, '`');
				continue;
			}
		}
		return -1;
	}

	private static int skip(String expr, int start, char c) {
		char prev = '\0';
		for (int i = start; i < expr.length() - 1; i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && prev != '\\') {
				return i;
			}
			prev = c0;
		}
		return expr.length() - 1;
	}

	static int skipIndexOf(String expr, char c, char c2) {
		for (int i = 0; i < expr.length() - 1; i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && expr.charAt(i + 1) == c2) {
				return i;
			}
			if (c0 == '\'') {
				i = skip(expr, i + 1, '\'');
				continue;
			}
			if (c0 == '[') {
				i = skip(expr, i + 1, ']');
				continue;
			}
			if (c0 == '"') {
				i = skip(expr, i + 1, '"');
				continue;
			}
			if (c0 == '`') {
				i = skip(expr, i + 1, '`');
				continue;
			}
		}
		return -1;
	}

	/* Grammar Interface */

	void add(String name, String[] ns, Expr pe) {
		Expr p = this.prodMap.get(name);
		if (p == null) {
			this.prodList.add(name);
		} else {
			this.log("redefined %s = %s\n\t=>", name, p, pe);
		}
		this.prodMap.put(name, pe);
		if (ns != null && ns.length > 1) {
			if (this.prodMap == null) {
				this.prodMap = new HashMap<>();
			}
			this.paramMap.put(name, ns);
		}
	}

	String[] getParam(String key) {
		if (this.paramMap != null) {
			return this.paramMap.get(key);
		}
		return null;
	}

	void addProd(String name, String expr) {
		if (name.startsWith("public ")) {
			name = name.substring(7).trim();
		}
		String[] names = split2(name, (s) -> {
			int p = flatIndexOf(s, '#');
			if (p > 0) {
				return p;
			}
			return flatIndexOf(s, ':');
		});
		String[] ns = split(names[0], s -> flatIndexOf(s, ' '));
		ns = Arrays.stream(ns).map(s -> {
			if (s.startsWith("\"") && s.endsWith("\"")) {
				return s.substring(1, s.length() - 1);
			}
			return s;
		}).toArray(String[]::new);
		if (names.length == 2) {
			this.add(ns[0], ns.length == 1 ? null : ns, new Tree(p(this, ns, expr).andThen(new Tag(names[1]))));
		} else {
			this.add(ns[0], ns.length == 1 ? null : ns, p(this, ns, expr));
		}
	}

	public void def(String prod) {
		String[] stmts = split2(prod, (s) -> flatIndexOf(s, ';'));
		if (stmts.length == 2) {
			this.def(stmts[0]);
			this.def(stmts[1]);
		} else {
			// System.err.println("@" + prod);
			String[] t = split2(prod, (s) -> flatIndexOf(s, '='));
			if (t.length == 2) {
				this.addProd(t[0], t[1]);
			}
		}
	}

	public void load(InputStream in0) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(in0, "UTF8"));
		StringBuilder sb = new StringBuilder();
		String s = null;
		while ((s = in.readLine()) != null) {
			int p = skipIndexOf(s, '/', '/');
			if (p > 0) {
				s = s.substring(0, p - 1);
			}
			if (p == 0) {
				continue;
			}
			if (s.startsWith(" ") || s.startsWith("\t")) {
				sb.append(s);
				continue;
			}
			if (sb.length() > 0) {
				this.def(sb.toString().trim());
				sb = new StringBuilder();
			}
			sb.append(s);
		}
		if (sb.length() > 0) {
			this.def(sb.toString().trim());
		}
	}

	public void load(String path) throws IOException {
		File f = new File(path);
		InputStream s = f.isFile() ? new FileInputStream(path) : SourceGenerator.class.getResourceAsStream(path);
		if (s == null) {
			throw new FileNotFoundException(path);
		}
		this.load(s);
		s.close();
	}

	public Parser newParser() {
		ParserGen gen = new ParserGen();
		String start = this.prodList.get(0);
		return gen.generate(start, this.prodMap.get(start));
	}

	public void log(String fmt, Object... args) {
		System.err.printf(fmt + "%n", args);
	}

	public static <T> void testExpr(String expr, Function<Expr, T> f, T result) {
		PEG peg = new PEG();
		T r = f.apply(p(peg, new String[0], expr));
		if (result == null) {
			System.out.printf("%s <- %s\n", expr, r);
		} else if (!r.equals(result)) {
			System.err.printf("%s <- %s != %s\n", expr, r, result);
		}
	}

	public static void testMatch(String expr, String... args) throws Throwable {
		PEG peg = new PEG();
		if (expr.startsWith("/") || expr.endsWith(".opeg")) {
			peg.load(expr);
		} else {
			peg.def(expr);
		}
		Parser p = peg.newParser();
		for (int i = 0; i < args.length; i += 2) {
			String r = p.parse(args[i]).toString();
			if (r.equals(args[i + 1])) {
				System.out.printf("[succ] %s %s => %s\n", expr, args[i], r);
			} else {
				System.err.printf("[fail] %s %s => %s != %s\n", expr, args[i], r, args[i + 1]);
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
		testMatch("A=a a?", "aa", "[# 'aa']", "ab", "[# 'a']");
		testMatch("A=ab ab?", "abab", "[# 'abab']", "ab", "[# 'ab']");
		/* Many */
		testMatch("A=a*", "aa", "[# 'aa']", "ab", "[# 'a']", "b", "[# '']");
		testMatch("A=ab*", "abab", "[# 'abab']", "aba", "[# 'ab']");

		testMatch("A={a #Hoge}", "aa", "[#Hoge 'a']");

		testMatch("/blue/origami/grammar/math.opeg", //
				"1", "[#IntExpr '1']", //
				"1+2", "[#AddExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]", //
				"1+2*3", "[#AddExpr $right=[#MulExpr $right=[#IntExpr '3'] $left=[#IntExpr '2']] $left=[#IntExpr '1']]", //
				"1*2+3",
				"[#AddExpr $right=[#IntExpr '3'] $left=[#MulExpr $right=[#IntExpr '2'] $left=[#IntExpr '1']]]");
		testMatch("/blue/origami/grammar/xml.opeg", //
				"<a/>", "[#Element $key=[#Name 'a']]", "<a></a>", "[#Element $key=[#Name 'a']]");
	}

}