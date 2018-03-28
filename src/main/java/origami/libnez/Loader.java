package origami.libnez;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import origami.libnez.PEG.And;
import origami.libnez.PEG.Char;
import origami.libnez.PEG.Contains;
import origami.libnez.PEG.Equals;
import origami.libnez.PEG.Exists;
import origami.libnez.PEG.If;
import origami.libnez.PEG.Many;
import origami.libnez.PEG.Match;
import origami.libnez.PEG.NonTerm;
import origami.libnez.PEG.Not;
import origami.libnez.PEG.Off;
import origami.libnez.PEG.On;
import origami.libnez.PEG.OneMore;
import origami.libnez.PEG.Or;
import origami.libnez.PEG.Symbol;
import origami.libnez.TPEG.Fold;
import origami.libnez.TPEG.Link;
import origami.libnez.TPEG.Tag;
import origami.libnez.TPEG.Tree;
import origami.libnez.TPEG.Val;

class Loader {

	String basePath = "";
	PEG peg;

	Loader(PEG peg) {
		this.peg = peg;
	}

	void setBasePath(String filename) {
		int p = filename.lastIndexOf("/");
		this.basePath = (p >= 0) ? filename.substring(0, p + 1) : "";
	}

	// peg
	static ParseTree[] emptyTreeNodes = new ParseTree[0];

	Expr conv(String[] ns, ParseTree t) {
		String key = t.tag();
		ParseTree[] ts = emptyTreeNodes;
		switch (key) {
		case "Char":
			return s(t.asString());
		case "Class":
			return c(t.asString(), 0);
		case "Name":
			return n(this.peg, ns, t.asString());
		case "Many":
			return new Many(this.conv(ns, t.get(ParseTree.EmptyLabel)));
		case "OneMore":
			return new OneMore(this.conv(ns, t.get(ParseTree.EmptyLabel)));
		case "Option":
			return new Or(this.conv(ns, t.get(ParseTree.EmptyLabel)), PEG.Empty_);
		case "And":
			return new And(this.conv(ns, t.get(ParseTree.EmptyLabel)));
		case "Not":
			return new Not(this.conv(ns, t.get(ParseTree.EmptyLabel)));
		case "Seq":
			ts = t.list();
			return this.conv(ns, ts[0]).andThen(this.conv(ns, ts[1]));
		case "Or":
			ts = t.list();
			return this.conv(ns, ts[0]).orElse(this.conv(ns, ts[1]));
		case "Alt":
			ts = t.list();
			return this.conv(ns, ts[0]).orAlso(this.conv(ns, ts[1]));
		case "Tree":
			return new TPEG.Tree(this.conv(ns, t.get(ParseTree.EmptyLabel)));
		case "Fold":
			ts = t.list();
			if (ts.length == 2) {
				return new TPEG.Fold(ts[0].asString(), this.conv(ns, ts[1]));
			}
			return new TPEG.Fold(null, this.conv(ns, ts[0]));
		case "Link":
			ts = t.list();
			if (ts.length == 2) {
				return new TPEG.Link(ts[0].asString(), this.conv(ns, ts[1]));
			}
			return new TPEG.Link(null, this.conv(ns, ts[0]));
		default:
			return PEG.Fail_;
		}
	}

	static final boolean Public = true;
	static final boolean Private = false;
	static final boolean Override = true;
	static final String[] emptyStrings = new String[0];

	void load(String source) throws IOException {
		Parser p = nez().getParser();
		ParseTree t = p.parse(source);
		for (ParseTree sub : t.list()) {
			this.loadEach(sub);
		}
	}

	void loadEach(ParseTree t) throws IOException {
		String key = t.tag();
		switch (key) {
		case "Production": {
			ParseTree[] ts = t.list();
			if (ts.length == 2) {
				String name = ts[0].asString();
				this.peg.add(PEG.isPublicName(name), Override, name, this.conv(emptyStrings, ts[1]));
			} else {
				assert (ts.length == 3);
				boolean export = ts[0].asString().equals("public");
				this.peg.add(export, Override, ts[1].asString(), this.conv(emptyStrings, ts[2]));
			}
			break;
		}
		case "Macro": { // list x = x 1
			ParseTree[] ts = t.list();
			if (ts.length == 3) {
				String name = ts[0].asString();
				String[] ns = Arrays.stream(ts[1].list()).map(x -> t.asString()).toArray(String[]::new);
				this.peg.add(PEG.isPublicName(name), Override, name, this.conv(ns, ts[2]));
			} else {
				assert (ts.length == 4);
				boolean export = ts[0].asString().equals("public");
				String[] ns = Arrays.stream(ts[2].list()).map(x -> t.asString()).toArray(String[]::new);
				this.peg.add(export, Override, ts[1].asString(), this.conv(ns, ts[3]));
			}
			break;
		}
		case "Import": { // import name, name, name from 'hoge.oxml'
			ParseTree[] ts = t.list();
			PEG lpeg = this.peg.load(this.basePath + ts[1].asString());
			String[] ns = Arrays.stream(ts[0].list()).map(x -> t.asString()).toArray(String[]::new);
			boolean override = true;
			if (ns.length == 1 && ns[0].equals("*")) {
				ns = lpeg.pubList.stream().toArray(String[]::new);
				override = false;
			}
			for (String n : ns) {
				Expr pe = lpeg.get(n);
				this.peg.add(Public, override, n, pe);
			}
		}
		case "Section": // section hoge
			this.peg = this.peg.endSection();
			this.peg = this.peg.beginSection(t.get(ParseTree.EmptyLabel).asString());
			break;
		case "Example":
		default:
			break;
		}
	}

	// static lib

	final static String[] rules = { //
			"Start = _ Source EOF", //
			"_ = (S / COMMENT)*", //
			"S = [ \t\r\n]", //
			"COMMENT = '/*' (!'*/' .)* '*/' /  '//' (!EOL .)* EOL", //
			"EOL = '\n' / '\r\n' / EOF", //
			"EOS = ([ \t] / COMMENT)* (';' / EOL)", // _
			"EOF = !.", //
			// "list x = { $x (',' _ $x)* }", //
			// "text x = this.x EOL { (!(EOL x) .)* } EOL x", //

			"Source = { ($Statement)* #Source } ", //
			"Statement = Import/Section/Production", //

			"Import = { 'import' S  $Name #Import } EOS", //
			"Section = { 'section' S $Name #Section } EOS", //
			// Example = 'example' S $(name=)list(Name) $(body=)(text('\'\'\'') / text(""")
			// / text(```)) EOS
			"Production = { $Name _ '=' _ ([/|] _)? $Expression #Production }", //

			"NAME = (![ \t\r\n(,){};<>[|/*+?='`] .)+", //
			"Name = { NAME #Name }", //

			"Expression = UChoice", //
			"UChoice    = Choice {$ _ '|' _ $UChoice #Alt }?", //
			"Choice     = Sequence {$ _ '/' _ $Choice #Or }?", //
			"Sequence   = Predicate {$ (S / COMMENT)+ $Sequence #Seq }?", //
			"Predicate  = { ('!' #Not / '&' #And ) $Predicate } / Suffix", //
			"Suffix     = Term {$ ('*' #Many / '+' #OneMore / '?' #Option) }?", //

			"Term       = Char / Class / Val / Tag / Cons / '(' _ Expression _ ')' / Let / Func / NonTerminal", //

			"Char  = ['] { (!['\n] .)* #Char } [']", //
			"Class = '[' { (![\\]\n] .)* #Class } ']'", //
			"Val   = '`' { (![`\n] .)* #Val } '`'", //
			"Tag   = '#' { NAME #Tag }", //

			"Cons = '{' { ('$' $(Name)? S #Fold / #Tree) _ $(Expression) } _ '}'", //

			"Let = '$' { ('(' $Name '=)')? $Expression #Let } / '$' { $Name '(' _ $Expression _ ')' #Let } / '$' { $Expression #Let }", //

			// if(flag) on(flag, e) on(!flag, e)
			// symbol(A) <symbol A>

			"Func = { $Name '(' ($Expression _ ',' _ )* $Expression _ ')' #Func } / { '<' $Name S ($Predicate S _ )* $Predicate #Func '>' }", //
			"NonTerminal = Name",//
	};

	static PEG nez = null;

	static PEG nez() {
		if (nez == null) {
			PEG peg = new PEG();
			for (String s : rules) {
				def(peg, s);
			}
			nez = peg;
		}
		return nez;
	}

	static void def(PEG peg, String prod) {
		String[] ts = split2(prod, (s) -> flatIndexOf(s, '='));
		if (ts.length == 2) {
			Expr pe = p(peg, emptyStrings, ts[1]);
			peg.add(Public, Override, ts[0].trim(), pe);
		}
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

	/* Parser Interface */

	static Expr cons(PEG peg, String[] ns, String expr, char op, BiFunction<Expr, Expr, Expr> f, Supplier<Expr> next) {
		int pos = flatIndexOf(expr, op);
		if (pos > 0) {
			return f.apply(p(peg, ns, expr.substring(0, pos)), p(peg, ns, expr.substring(pos + 1)));
		}
		return next.get();
	}

	static Expr newStateFunc(Expr nonTerm, Function<Expr, Expr> f) {
		return f.apply(nonTerm);
	}

	static Expr unary(PEG peg, String[] ns, String expr) {
		expr = expr.trim();
		int length = expr.length();
		if (length == 0) {
			return PEG.Empty_;
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
			return new Or(p(peg, ns, expr.substring(0, expr.length() - 1)), PEG.Empty_);
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
			// if (start == '@') {
			// String[] t = split2(expr.substring(0, expr.length() - 1), (s) ->
			// s.indexOf('('));
			// switch (t[0]) {
			// case "@if":
			// return new If(t[1]);
			// case "@on":
			// String flag = left(t[1], ',');
			// if (flag.startsWith("!")) {
			// return new Off(flag.substring(1), p(peg, ns, right(t[1], ',')));
			// }
			// return new On(flag, p(peg, ns, right(t[1], ',')));
			// case "@symbol":
			// return newStateFunc(p(peg, ns, t[1]), Symbol::new);
			// case "@match":
			// return newStateFunc(p(peg, ns, t[1]), Match::new);
			// case "@exists":
			// return newStateFunc(p(peg, ns, t[1]), Exists::new);
			// case "@equals":
			// return newStateFunc(p(peg, ns, t[1]), Equals::new);
			// case "@contains":
			// return newStateFunc(p(peg, ns, t[1]), Contains::new);
			// }
			// }
			// String[] t = split2(expr.substring(0, expr.length() - 1), (s) ->
			// s.indexOf('('));
			// Expr[] p = Arrays.stream(split(t[1], (s) -> flatIndexOf(s, ','))).map(s ->
			// p(peg, ns, s))
			// .toArray(Expr[]::new);
			// return new App(p(peg, ns, t[0]), p);
		}
		if (start == '{' && end == '}') {
			if (expr.startsWith("{$")) {
				String[] t = split2(expr.substring(2, expr.length() - 1), (s) -> s.indexOf(' '));
				return new Fold(t[0], p(peg, ns, t[1]));
			}
			return new Tree(p(peg, ns, expr.substring(1, expr.length() - 1)));
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
			return PEG.Any_;
		}
		if (start == '"' && start == end) {
			expr = u(expr.substring(1, expr.length() - 1));
		}
		return n(peg, ns, expr);
	}

	static Expr n(PEG peg, String[] ns, String name) {
		for (int i = 0; i < ns.length; i++) {
			if (ns[i].equals(name)) {
				return new NonTerm(peg, name, i);
			}
		}
		if (isOctet(name)) {
			return octet(name);
		}
		return new NonTerm(peg, name, -1);
	}

	private static boolean isOctet(String octet) {
		if (octet.length() == 8) {
			for (int i = 0; i < 8; i++) {
				char c = octet.charAt(i);
				if (c == '0' || c == '1' || c == 'x' || c == 'X') {
					continue;
				}
				return false;
			}
			return true;
		}
		return false;
	}

	private static Expr octet(String octet) {
		BitChar bc = new BitChar((byte) 0, (byte) 255);
		for (int i = 0; i < 8; i++) {
			int position = 0x80 >> i;
			switch (octet.charAt(i)) {
			case '0':
				for (int j = 0; j < 256; j++) {
					if ((j & position) == 0) {
						continue;
					}
					bc.set2(j, false);
				}
				break;
			case '1':
				for (int j = 0; j < 256; j++) {
					if ((j & position) != 0) {
						continue;
					}
					bc.set2(j, false);
				}
				break;
			}
		}
		return new Char(bc);
	}

	static Expr app(PEG peg, String[] ns, String name, Expr[] es) {
		switch (name) {
		case "if":
			return new If(es[0].toString());
		case "on":
			String flag = es[0].toString();
			if (flag.startsWith("!")) {
				return new Off(flag.substring(1), es[1]);
			}
			return new On(flag, es[1]);
		case "off":
			return new Off(es[0].toString(), es[1]);
		case "scope":
			return new PEG.Scope(es[0]);
		case "symbol":
			return new Symbol(es[0]);
		case "match":
			return new Match(es[0]);
		case "exists":
			return new Exists(es[0]);
		case "equals":
			return new Equals(es[0]);
		case "contains":
			return new Contains(es[0]);
		case "many":
			return new Many(es[0]);
		case "onemore":
			return new OneMore(es[0]);
		case "option":
			return es[0].orElse(PEG.Empty_);
		case "and":
			return new And(es[0]);
		case "not":
			return new Not(es[0]);
		case "new":
			return new TPEG.Tree(es[0]);
		case "fold":
			return new TPEG.Fold(null, es[0]);
		case "add":
			return new TPEG.Link(null, es[0]);
		case "set":
			return new TPEG.Link(es[0].toString(), es[1]);
		default:
			return new App(n(peg, ns, name), es);
		}

	}

	static byte[] encode(String text) {
		if (text == null) {
			return new byte[0];
		}
		try {
			return text.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return text.getBytes();
	}

	static Expr s(String s) {
		byte[] b = encode(s);
		if (b.length == 0) {
			return PEG.Empty_;
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

	private static char charAt(String expr, int offset) {
		char c1 = expr.charAt(offset);
		if (c1 == '\\') {
			c1 = expr.charAt(offset + 1);
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
				return hex2(expr.charAt(offset + 2), expr.charAt(offset + 3));
			case 'u':
			case 'U':
				return hex4(expr.charAt(offset + 2), expr.charAt(offset + 3), expr.charAt(offset + 4),
						expr.charAt(offset + 5));
			}
		}
		return c1;
	}

	private static int charNext(String expr, int offset) {
		char c1 = expr.charAt(offset);
		if (c1 == '\\') {
			c1 = expr.charAt(offset + 1);
			if (c1 == 'x' || c1 == 'X') {
				return offset + 4;
			}
			if (c1 == 'u' || c1 == 'U') {
				return offset + 6;
			}
			return offset + 2;
		}
		return offset + 1;
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

	static char hex2(char ch1, char ch2) {
		int c = hex(ch1);
		c = (c * 16) + hex(ch2);
		return (char) c;
	}

	static char hex4(char ch1, char ch2, char ch3, char ch4) {
		int c = hex(ch1);
		c = (c * 16) + hex(ch2);
		c = (c * 16) + hex(ch3);
		c = (c * 16) + hex(ch4);
		return (char) c;
	}

	static Expr c(String expr, int offset) {
		char c1 = charAt(expr, offset);
		offset = charNext(expr, offset);
		Expr pe;
		if (offset < expr.length() && expr.charAt(offset) == '-') {
			char c2 = charAt(expr, offset + 1);
			pe = range(c1, c2);
			offset = charNext(expr, offset + 1);
		} else {
			pe = s(String.valueOf(c1));
		}
		return offset < expr.length() ? pe.orElse(c(expr, offset)) : pe;
	}

	private static Expr range(char c1, char c2) {
		if (c1 < 256 && c2 < 256) {
			return new Char((byte) c1, (byte) c2);
		}
		byte[] b1 = encode(String.valueOf(c1));
		byte[] b2 = encode(String.valueOf(c2));
		if (b1.length == b2.length && b1[0] == b2[0]) {
			if (b1.length == 2) {
				// System.err.println("@@1 " + c1 + " " + c2);
				return (new Char(b1[0])).andThen(new Char(b1[1], b2[1]));
			}
			if (b1[1] != b2[1]) {
				// System.err.println("@@2 " + c1 + " " + c2);
				return new Char(b1[0]).andThen(range(1, c1, c2));
			}
			if (b1.length == 3) {
				// System.err.println("@@3 " + c1 + " " + c2);
				return new Char(b1[0]).andThen(new Char(b1[1]).andThen(new Char(b1[2], b2[2])));
			}
			if (b1[2] != b2[2]) {
				// System.err.println("@@4 " + c1 + " " + c2);
				return new Char(b1[0]).andThen(new Char(b1[1]).andThen(range(2, c1, c2)));
			}
			if (b1.length == 4) {
				// System.err.println("@@5 " + c1 + " " + c2);
				return new Char(b1[0])
						.andThen(new Char(b1[1]).andThen(new Char(b1[2]).andThen(new Char(b1[3], b2[3]))));
			}
		}
		return range(0, c1, c2);
	}

	private static Expr range(int offset, char c1, char c2) {
		if (c2 < c1) {
			return range(offset, c2, c1);
		}
		Expr pe = s(encode(String.valueOf(c2)), offset);
		for (int c = c2 - 1; c1 <= c; c--) {
			byte[] b = encode(String.valueOf((char) c));
			pe = s(b, offset).orElse(pe);
		}
		return pe;
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

}