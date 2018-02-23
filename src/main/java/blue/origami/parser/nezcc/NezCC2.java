package blue.origami.parser.nezcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import blue.origami.Version;
import blue.origami.common.OConsole;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.common.OWriter;
import blue.origami.common.SourcePosition;
import blue.origami.main.MainOption;
import blue.origami.parser.ParserGrammar;
import blue.origami.parser.peg.ByteSet;
import blue.origami.parser.peg.Production;
import blue.origami.parser.peg.Stateful;

public class NezCC2 implements OFactory<NezCC2> {
	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int EMPTY = 1 << 3;
	private int mask = POS;

	@Override
	public Class<?> keyClass() {
		return NezCC2.class;
	}

	@Override
	public NezCC2 clone() {
		return new NezCC2();
	}

	@Override
	public void init(OOption options) {
		String file0 = options.stringValue(MainOption.GrammarFile, "parser.opeg");
		String base = SourcePosition.extractFileBaseName(file0);
		this.defineSymbol("base", base);
		this.defineSymbol("nezcc", "nezcc/2.0");
		this.defineSymbol("space", " ");
		if (options.is(MainOption.TreeConstruction, true)) {
			this.mask |= TREE;
		}
		String[] files = options.stringList(MainOption.InputFiles);
		if (files.length == 0) {
			files = new String[] { "chibi.nezcc" };
		}
		for (String file : files) {
			if (!file.endsWith(".nezcc")) {
				continue;
			}
			if (!new File(file).isFile()) {
				file = Version.ResourcePath + "/nezcc2/" + file;
			}
			this.importNezccFile(file);
		}

	}

	HashMap<String, String> formatMap = new HashMap<>();

	void importNezccFile(String path) {
		try {
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : SourceGenerator.class.getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(s));
			String line = null;
			String name = null;
			String delim = null;
			StringBuilder text = null;
			while ((line = reader.readLine()) != null) {
				if (text == null) {
					if (line.startsWith("#")) {
						continue;
					}
					int loc = line.indexOf(" = ");
					if (loc <= 0) {
						continue;
					}
					name = line.substring(0, loc).trim();
					String value = line.substring(loc + 2).trim();
					// System.out.printf("%2$s : %1$s\n", value, name);
					if (value == null) {
						continue;
					}
					// what is this?
					// if (name.equals("memoentries")) {
					// this.useMemoentries = true;
					// this.defineOriginalSymbol(name, value);
					// }
					if (value.equals("'''") || value.equals("\"\"\"")) {
						delim = value;
						text = new StringBuilder();
					} else {
						this.defineSymbol(name, value);
					}
				} else {
					if (line.trim().equals(delim)) {
						this.defineSymbol(name, text.toString());
						text = null;
					} else {
						if (text.length() > 0) {
							text.append("\n");
						}
						text.append(line);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			OConsole.exit(1, e);
		}
	}

	public boolean isDefined(String key) {
		return this.formatMap.containsKey(key);
	}

	void defineSymbol(String key, String symbol) {
		if (!this.formatMap.containsKey(key)) {
			if (symbol != null) {
				int s = symbol.indexOf("$|");
				while (s >= 0) {
					int e = symbol.indexOf('|', s + 2);
					String skey = symbol.substring(s + 2, e);
					symbol = symbol.replace("$|" + skey + "|", this.formatMap.getOrDefault(skey, skey));
					e = s;
					s = symbol.indexOf("$|");
					if (e == s) {
						break; // avoid infinite looping
					}
					// System.out.printf("'%s': %s\n", key, symbol);
				}
			}
			// System.err.println(key + " = " + symbol);
			this.formatMap.put(key, symbol);
		}
	}

	String s(String skey) {
		return this.formatMap.getOrDefault(skey, skey);
	}

	String T(String skey) {
		return this.formatMap.getOrDefault("T" + skey, skey);
	}

	void setupSymbols() {
		// if (this.isDefined("include")) {
		// this.importNezccFile(this.s("include"));
		// }
		// this.importNezccFile(Version.ResourcePath + "/nezcc/default.nezcc");
		//
		this.defineSymbol("tab", " ");
		this.defineSymbol("Tspos", this.s("Tpos"));
		this.defineSymbol("Tepos", this.s("Tpos"));
		this.defineSymbol("Tmp", this.s("Tpos"));
		this.defineSymbol("Tlabel", this.s("Ttag"));
		this.defineSymbol("Tchild", this.s("Ttree"));
		this.defineSymbol("Tprev", this.s("Ttree"));
		this.defineSymbol("Tee", this.s("Te"));
		this.defineSymbol("Ostring", "1");
		this.defineSymbol("Oinline", "1");
	}

	/* */

	class Writer {
		StringBuilder sb = new StringBuilder();
		int indent = 0;

		Writer() {
		}

		void incIndent() {
			this.indent++;
		}

		void decIndent() {
			assert (this.indent > 0);
			this.indent--;
		}

		void format(String format, Object... args) {
			int start = 0;
			int index = 0;
			for (int i = 0; i < format.length(); i++) {
				char c = format.charAt(i);
				if (c == '\t') {
					this.formatBuf(format, start, i);
					this.wIndent("");
					start = i + 1;
				} else if (c == '%') {
					this.formatBuf(format, start, i);
					c = i + 1 < format.length() ? format.charAt(i + 1) : 0;
					if (c == 's') {
						this.push(args[index]);
						index++;
						i++;
					} else if (c == '%') {
						this.push("%");
						i++;
					} else if ('1' <= c && c <= '9') { // %1$s
						int n = c - '1';
						if (!(n < args.length)) {
							System.err.printf("FIXME: n=%s  %d,%s\n", format, n, args.length);
						}
						this.push(args[n]);
						i += 3;
					}
					start = i + 1;
				}
			}
			this.formatBuf(format, start, format.length());
		}

		void push(Object value) {
			if (value instanceof Expression) {
				((Expression) value).emit(this);
			} else {
				this.sb.append(value);
			}
		}

		String Indent(String tab, String stmt) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.indent; i++) {
				sb.append(tab);
			}
			sb.append(stmt);
			return sb.toString();
		}

		private void wIndent(String line) {
			this.sb.append(this.Indent("  ", line));
		}

		private void formatBuf(String format, int start, int end) {
			if (start < end) {
				this.push(format.substring(start, end));
			}
		}

		@Override
		public String toString() {
			return this.sb.toString();
		}

	}

	public abstract class Expression {
		abstract void emit(Writer w);

		boolean isDefined(String key) {
			return NezCC2.this.formatMap.containsKey(key);
		}

		String formatOf(String key) {
			return NezCC2.this.formatMap.get(key);
		}

		String formatOf(String key, String format) {
			return NezCC2.this.formatMap.getOrDefault(key, format);
		}

		String formatOf(String key, String key2, String format) {
			if (NezCC2.this.formatMap.containsKey(key)) {
				return NezCC2.this.formatMap.get(key);
			}
			return NezCC2.this.formatMap.getOrDefault(key2, format);
		}

		String formatOf(String key, String key2, String key3, String format) {
			if (NezCC2.this.formatMap.containsKey(key)) {
				return NezCC2.this.formatMap.get(key);
			}
			if (NezCC2.this.formatMap.containsKey(key2)) {
				return NezCC2.this.formatMap.get(key2);
			}
			return NezCC2.this.formatMap.getOrDefault(key3, format);
		}

		String typeOf(String key) {
			return NezCC2.this.formatMap.getOrDefault("T" + key, "T" + key);
		}

		String fieldOf(String base, String field) {
			return this.formatOf(base + field, field);
		}

		@Override
		public final String toString() {
			Writer w = new Writer();
			this.emit(w);
			return w.toString();
		}

		public Expression ret() {
			return new Return(this);
		}

		public Expression deret() {
			return this;
		}

		public Expression and(Expression next) {
			return new Infix(this, "&&", next);
		}

		public Expression add(Expression next) {
			return new Block(this, next);
		}

	}

	class Symbol extends Expression {
		String symbol;

		Symbol(String symbol) {
			this.symbol = symbol;
		}

		@Override
		void emit(Writer w) {
			w.push(this.formatOf(this.symbol, this.symbol));
		}
	}

	class FuncName extends Expression {
		String name;

		FuncName(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			w.push(this.name);
		}
	}

	class Var extends Expression {
		String name;

		Var(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			w.push(this.formatOf("V" + this.name, this.name));
		}
	}

	class Type extends Expression {
		String name;

		Type(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			w.push(this.typeOf(this.name));
		}

	}

	public class DefFunc extends Expression {
		String name;
		String ret;
		String[] params;
		Expression body;

		public DefFunc(String name, String... params) {
			this.name = name;
			this.params = params;
			this.ret = this.typeOf("matched");
			this.body = null;
		}

		@Override
		public DefFunc add(Expression e) {
			this.body = e.ret();
			return this;
		}

		public DefFunc is(String code, Object... a) {
			return this.add(NezCC2.this.p(code, a));
		}

		public DefFunc asType(String t) {
			this.ret = this.typeOf(t);
			return this;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("function", "%2$s(%3$s) :%1$s = "), this.ret, this.name, new Params(this.params));
			w.incIndent();
			w.format(this.formatOf("body function", "\n\t%s"), this.body);
			w.decIndent();
			w.push(this.formatOf("end function", "end", "\n"));
		}

		class Params extends Expression {
			String[] params;

			public Params(String[] params) {
				this.params = params;
			}

			@Override
			void emit(Writer w) {
				if (this.params.length > 0) {
					w.push(new Param(this.params[0]));
				}
				for (int i = 1; i < this.params.length; i++) {
					w.format(this.formatOf("paramdelim", "delim", " "));
					w.push(new Param(this.params[i]));
				}
			}
		}

		class Param extends Expression {
			String name;

			Param(String name) {
				this.name = name;
			}

			@Override
			void emit(Writer w) {
				w.format(this.formatOf("param", "%s %s"), new Type(this.name), new Var(this.name));
			}
		}

	}

	class Return extends Expression {
		Expression e;

		public Return(Expression e) {
			this.e = e;
		}

		@Override
		public Expression ret() {
			return this;
		}

		@Override
		public Expression deret() {
			return this.e;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("return", "%s"), this.e);
		}
	}

	class IfExpr extends Expression {
		Expression cnd;
		Expression thn;
		Expression els;

		public IfExpr(Expression cnd, Expression thn, Expression els) {
			this.cnd = cnd;
			this.thn = thn;
			this.els = els;
		}

		@Override
		public Expression ret() {
			if (!this.isDefined("ifexpr")) {
				this.thn = this.thn.ret();
				this.els = this.els.ret();
			}
			return super.ret();
		}

		@Override
		public Expression deret() {
			if (!this.isDefined("ifexpr")) {
				this.thn = this.thn.deret();
				this.els = this.els.deret();
			}
			return this;
		}

		@Override
		void emit(Writer w) {
			// if (this.isDefined("ifexpr")) {
			w.format(this.formatOf("ifexpr", "%s ? %s : %s"), this.cnd, this.thn, this.els);
			// }
		}

	}

	class IntValue extends Expression {
		int value;

		public IntValue(int v) {
			this.value = v;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("int", "%s"), this.value);
		}
	}

	class CharValue extends Expression {
		int uchar;

		public CharValue(char uchar) {
			this.uchar = uchar;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("char", "%s"), this.uchar & 0xff);
		}

	}

	class StringValue extends Expression {
		String value;

		public StringValue(String sym) {
			this.value = sym;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("string", "str", "\"%s\""), this.value);
		}
	}

	class SymbolValue extends Expression {
		String sym;

		public SymbolValue(String sym) {
			this.sym = sym;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("symbol", "\"%s\""), this.sym);
		}
	}

	class IndexValue extends Expression {
		byte[] indexMap;

		IndexValue(byte[] data) {
			this.indexMap = data;
		}

		@Override
		void emit(Writer w0) {
			if (this.isDefined("Obase64")) {
				byte[] encoded = Base64.getEncoder().encode(this.indexMap);
				Expression index = NezCC2.this.apply("b64", new StringValue(new String(encoded)));
				w0.format(this.formatOf("constname", "%s",
						NezCC2.this.constName(this.typeOf("alt"), "alt", this.indexMap.length, index.toString())));
			} else {
				Writer w = new Writer();
				w.push(this.formatOf("array", "["));
				for (int i = 0; i < this.indexMap.length; i++) {
					if (i > 0) {
						w.push(this.formatOf("delim array", "delim", " "));
					}
					w.push(new IntValue(this.indexMap[i] & 0xff));
				}
				w.push(this.formatOf("end array", "]"));
				w0.format(this.formatOf("constname", "%s",
						NezCC2.this.constName(this.typeOf("alt"), "alt", this.indexMap.length, w.toString())));
			}
		}
	}

	class ByteSetValue extends Expression {
		ByteSet bs;

		public ByteSetValue(ByteSet bs) {
			this.bs = bs;
		}

		@Override
		void emit(Writer w0) {
			if (this.isDefined("Obits32")) {
				Writer w = new Writer();
				w.push(this.formatOf("array", "["));
				for (int i = 0; i < 8; i++) {
					if (i > 0) {
						w.push(this.formatOf("delim array", "delim", " "));
					}
					w.push(new IntValue(this.bs.bits()[i]));
				}
				w.push(this.formatOf("end array", "]"));
				w0.format(this.formatOf("constname", "%s",
						NezCC2.this.constName(this.typeOf("bs"), "bs", 8, w.toString())));
			} else {
				Writer w = new Writer();
				w.push(this.formatOf("array", "["));
				for (int i = 0; i < 256; i++) {
					if (i > 0) {
						w.push(this.formatOf("delim array", "delim", " "));
					}
					if (this.bs.is(i)) {
						w.push(this.formatOf("true", "true"));
					} else {
						w.push(this.formatOf("false", "false"));
					}
				}
				w.push(this.formatOf("end array", "]"));
				w0.format(this.formatOf("constname", "%s",
						NezCC2.this.constName(this.typeOf("bs"), "bs", 256, w.toString())));
			}
		}
	}

	HashSet<String> usedNames = new HashSet<>();

	void used(String name) {
		this.usedNames.add(name);
	}

	class FuncRef extends Expression {
		String name;

		public FuncRef(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			NezCC2.this.usedNames.add(this.name);
			if (this.isDefined("funcref")) {
				w.format(this.formatOf("funcref", "%s"), this.name);
				return;
			}
			if (this.isDefined("lambda")) {
				w.format(this.formatOf("lambda", "\\%s %s"), this.name, NezCC2.this.apply(this.name, "px"));
				return;
			}
			w.push(this.name);
		}
	}

	class Lambda extends Expression {
		String name;
		Expression body;

		public Lambda(String name, Expression body) {
			this.name = name;
			this.body = body;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("lambda", "\\%s %s"), this.name, this.body);
		}
	}

	// @Override
	// protected String emitParserLambda(String match) {
	// String px = this.V("px");
	// String lambda = String.format(this.s("lambda"), px, match);
	// String p = "p" + this.varSuffix();
	// return lambda.replace("px", p);
	// }

	ArrayList<String> constList = new ArrayList<>();
	private HashMap<String, String> constNameMap = new HashMap<>();

	private String constName(String typeName, String prefix, int arraySize, String value) {
		String key = typeName + value;
		String constName = this.constNameMap.get(key);
		if (constName == null) {
			constName = prefix + this.constNameMap.size();
			this.constNameMap.put(key, constName);
			NezCC2.Expression c = new Const(typeName, constName, arraySize, value);
			this.constList.add(c.toString());
		}
		return constName;
	}

	class Const extends Expression {
		String typeName;
		String constName;
		int arraySize;
		String value;

		public Const(String typeName, String constName, int arraySize, String value) {
			this.typeName = typeName;
			this.constName = constName;
			this.arraySize = arraySize;
			this.value = value;
		}

		@Override
		void emit(Writer w) {
			if (this.arraySize == -1) {
				w.format(this.formatOf("const", "%2$s = %3$s"), this.typeName, this.constName, this.value);
			} else {
				w.format(this.formatOf("const_array", "const", "%2$s = %3$s"), this.typeName, this.constName,
						this.value, this.arraySize);
			}
		}
	}

	void declConst(String typeName, String constName, String value) {
		NezCC2.Expression c = new Const(typeName, constName, -1, value);
		this.constList.add(c.toString());
	}

	// Expression op(Expression left, String op, Expression right) {
	// return new Infix(left, op, right);
	// }

	class LetIn extends Expression {
		String name;
		Expression right;
		Expression next;

		LetIn(String name, Expression right) {
			this.name = name;
			this.right = right;
		}

		@Override
		public Expression ret() {
			this.next = this.next.ret();
			return this;
		}

		@Override
		public Expression deret() {
			this.next = this.next.deret();
			return this;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("let", "val", "var", "%s %s = %s\n\t%s"), new Type(this.name), new Var(this.name),
					this.right, this.next);
		}

		@Override
		public Expression add(Expression next) {
			if (this.next == null) {
				this.next = next;
			} else {
				this.next = this.next.add(next);
			}
			return this;
		}

	}

	class Getter extends Expression {
		String base;
		String field;

		Getter(String name, String field) {
			this.base = name;
			this.field = field;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("getter", "%s.%s"), new Var(this.base), this.fieldOf(this.base, this.field));
		}
	}

	// class Mutate extends Expression {
	// String base;
	// String[] fields;
	// Expression[] values;
	//
	// Mutate(String base, Object... args) {
	// this.base = base;
	// this.fields = new String[args.length / 2];
	// this.values = new Expression[args.length / 2];
	// for (int i = 0; i < args.length / 2; i++) {
	// this.fields[i] = (String) args[i * 2];
	// this.values[i] = (Expression) args[i * 2 + 1];
	// }
	// }
	//
	// @Override
	// void emit(Writer w) {
	// if (this.isDefined("mut")) {
	// // Haskell
	// }
	// if (this.base == null) {
	// for (int i = 0; i < this.fields.length; i++) {
	// if (i > 0) {
	// w.format(this.formatOf(";", "\n\t"));
	// }
	// w.format(this.formatOf("=", "%s = %s"), new Var(this.fields[i]),
	// this.values[i]);
	// }
	// } else {
	// for (int i = 0; i < this.fields.length; i++) {
	// if (i > 0) {
	// w.format(this.formatOf(";", "\n\t"));
	// }
	// w.format(this.formatOf("setter", "%s.%s = %s"), new Var(this.base),
	// this.fieldOf(this.base, this.fields[i]), this.values[i]);
	// }
	// }
	// }
	// }

	class Block extends Expression {
		Expression[] sub;

		Block(Expression... sub) {
			this.sub = sub;
		}

		@Override
		public Expression ret() {
			this.sub[this.sub.length - 1] = this.sub[this.sub.length - 1].ret();
			return this;
		}

		@Override
		public Expression deret() {
			this.sub[this.sub.length - 1] = this.sub[this.sub.length - 1].deret();
			return this;
		}

		@Override
		void emit(Writer w) {
			// w.incIndent();
			int c = 0;
			for (Expression e : this.sub) {
				if (c > 0) {
					w.wIndent("");
				}
				w.format(this.formatOf("stmt", "%s\n"), e);
				c++;
			}
			// w.decIndent();
		}

		@Override
		public Expression add(Expression next) {
			Expression[] sub2 = new Expression[this.sub.length + 1];
			System.arraycopy(this.sub, 0, sub2, 0, this.sub.length);
			sub2[this.sub.length] = next;
			return new Block(sub2);
		}
	}

	Expression block(Expression... sub) {
		Expression first = sub[0];
		for (int i = 1; i < sub.length; i++) {
			first = first.add(sub[i]);
		}
		return first;
	}

	class Apply extends Expression {
		Expression left;
		Expression right;

		Apply(Expression left, Expression... right) {
			this.left = left;
			this.right = right.length == 1 ? right[0] : new Args(right);
		}

		Apply(String left, Expression[] right) {
			this(new FuncName(left), right);
			NezCC2.this.usedNames.add(left);
		}

		@Override
		void emit(Writer w) {
			String key = this.left + "apply";
			if (this.isDefined(key)) {
				w.format(this.formatOf(key), this.left, this.right);
			} else {
				w.format(this.formatOf("apply", "%s(%s)"), this.left, this.right);
			}
		}

		class Args extends Expression {
			Expression[] args;

			Args(Expression... args) {
				this.args = args;
			}

			@Override
			void emit(Writer w) {
				String delim = this.formatOf("delim", " ");
				if (this.args.length > 0) {
					w.push(this.args[0]);
				}
				for (int i = 1; i < this.args.length; i++) {
					w.push(delim);
					w.push(this.args[i]);
				}
			}
		}

	}

	class Macro extends Expression {
		String name;
		Expression e;

		Macro(String name, Expression e) {
			this.name = name;
			this.e = e;
		}

		@Override
		void emit(Writer w) {
			NezCC2.this.used(this.name);
			w.format(this.formatOf(this.name, "%s"), this.e);
		}
	}

	class GetIndex extends Expression {
		Expression left;
		Expression right;

		GetIndex(Expression left, Expression right) {
			this.left = left;
			this.right = right;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("index", "%s[%s]"), this.left, this.right);
		}
	}

	class Infix extends Expression {
		String op;
		Expression left;
		Expression right;

		Infix(Expression left, String op, Expression right) {
			this.op = op;
			this.left = left;
			this.right = right;
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined(this.op)) {
				w.format(this.formatOf(this.op), this.left, this.right);
			} else {
				w.format(this.formatOf("infix", "%s %s %s"), this.left, this.op, this.right);
			}
		}
	}

	class Unary extends Expression {
		String op;
		Expression inner;

		Unary(String op, Expression inner) {
			this.inner = inner;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf(this.op, "%s"), this.inner);
		}
	}

	private int flatIndexOf(String expr, char c) {
		int level = 0;
		for (int i = 0; i < expr.length(); i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && level == 0) {
				return i;
			}
			if (c0 == '(' || c0 == '[') {
				level++;
			}
			if (c0 == ')' || c0 == ']') {
				level--;
			}
		}
		return -1;
	}

	private int flatIndexOf(String expr, char c, char c2) {
		int level = 0;
		for (int i = 0; i < expr.length() - 1; i++) {
			char c0 = expr.charAt(i);
			if (c0 == c && expr.charAt(i + 1) == c2 && level == 0) {
				return i;
			}
			if (c0 == '(' || c0 == '[') {
				level++;
			}
			if (c0 == ')' || c0 == ']') {
				level--;
			}
		}
		return -1;
	}

	Expression unary(String expr, final Expression... args) {
		expr = expr.trim();
		// this.dump(expr, args);
		if (expr.endsWith("]")) {
			int pos = this.flatIndexOf(expr, '[');
			Expression b = this.unary(expr.substring(0, pos), args);
			Expression e = this.p_(expr.substring(pos + 1, expr.length() - 1), args);
			return new GetIndex(b, e);
		}
		if (expr.endsWith(")")) {
			int pos = this.flatIndexOf(expr, '(');
			if (pos > 0) {
				String[] tokens = expr.substring(pos + 1, expr.length() - 1).split(",");
				Expression[] a = Arrays.stream(tokens).map(s -> this.p_(s, args)).toArray(Expression[]::new);
				String fname = expr.substring(0, pos).trim();
				if (fname.endsWith("!")) {
					assert a.length == 1;
					return new Macro(fname.substring(0, fname.length() - 1), a[0]);
				}
				return new Apply(fname, a);
			}
			return new Unary("group", this.p_(expr.substring(1, expr.length() - 1), args));
		}
		int pos = this.flatIndexOf(expr, '.');
		if (pos > 0) {
			return new Getter(expr.substring(0, pos), expr.substring(pos + 1));
		}
		if (expr.startsWith("$")) {
			return args[Integer.parseInt(expr.substring(1))];
		}
		if (expr.startsWith("^")) {
			return new FuncRef(expr.substring(1));
		}
		if (expr.startsWith("'")) {
			return new Symbol(expr.substring(1));
		}
		if (expr.startsWith("!")) {
			return new Unary("!", this.p_(expr.substring(1, expr.length() - 1), args));
		}
		if (expr.length() > 0 && Character.isDigit(expr.charAt(0))) {
			return new IntValue(Integer.parseInt(expr));
		}
		return new Var(expr);
	}

	@FunctionalInterface
	static interface Bin {
		Expression apply(Expression e, Expression e2);
	}

	Expression bin(String expr, String op, Expression[] args, Bin f) {
		assert (op.length() < 3);
		int pos = op.length() == 2 ? this.flatIndexOf(expr, op.charAt(0), op.charAt(1))
				: this.flatIndexOf(expr, op.charAt(0));
		if (pos > 0) {
			return f.apply(this.p_(expr.substring(0, pos).trim(), args),
					this.p_(expr.substring(pos + op.length()).trim(), args));
		}
		return null;
	}

	void dump(String expr, final Expression[] args) {
		switch (args.length) {
		case 0:
			// System.err.printf("@0'%s'\n", expr);
			break;
		case 1:
			System.err.printf("@1'%s' %s :%s\n", expr, args[0], args[0].getClass().getSimpleName());
			break;
		case 2:
			System.err.printf("@2'%s' %s :%s   %s :%s\n", expr, args[0], args[0].getClass().getSimpleName(), args[1],
					args[1].getClass().getSimpleName());
			break;
		case 3:
			System.err.printf("@3'%s' %s :%s   %s :%s   %s :%s\n", expr, args[0], args[0].getClass().getSimpleName(),
					args[1], args[1].getClass().getSimpleName(), args[2], args[2].getClass().getSimpleName());
			break;
		case 4:
			System.err.printf("@4'%s' %s :%s   %s :%s   %s :%s   %s :%s\n", expr, args[0],
					args[0].getClass().getSimpleName(), args[1], args[1].getClass().getSimpleName(), args[2],
					args[2].getClass().getSimpleName(), args[3], args[3].getClass().getSimpleName());
			break;
		default:
			System.err.printf("@%d'%s' ...\n", expr, args.length);
			break;

		}
	}

	Expression p_(String expr, final Expression[] args) {
		expr = expr.trim();
		// this.dump(expr, args);
		Expression p = null;
		// let n = expr ; ...
		int pos = expr.indexOf(';');
		if (pos > 0) {
			String[] tokens = expr.split(";");
			Expression[] a = Arrays.stream(tokens).map(s -> this.p_(s.trim(), args)).toArray(Expression[]::new);
			return this.block(a);
		}
		if (expr.startsWith("let ")) {
			String[] t = expr.substring(4).split("=");
			return new LetIn(t[0].trim(), this.p_(t[1].trim(), args));
		}
		p = this.bin(expr, "==", args, (e, e2) -> new Infix(e, "==", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "= ", args, (e, e2) -> new Infix(e, "=", e2));
		if (p != null) {
			return p;
		}
		pos = expr.indexOf("?");
		if (pos > 0) {
			String[] tokens = expr.split("\\?");
			Expression[] a = Arrays.stream(tokens).map(s -> this.p_(s.trim(), args)).toArray(Expression[]::new);
			return new IfExpr(a[0], a[1], a[2]);
		}
		p = this.bin(expr, "||", args, (e, e2) -> new Infix(e, "||", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "&&", args, (e, e2) -> new Infix(e, "&&", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "<", args, (e, e2) -> new Infix(e, "<", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "+", args, (e, e2) -> new Infix(e, "+", e2));
		if (p != null) {
			return p;
		}
		// p = this.bin(expr, "*", args, (e, e2) -> new Infix(e, "*", e2));
		// if (p != null) {
		// return p;
		// }
		return this.unary(expr, args);
	}

	private Expression[] filter(Object... args) {
		if (args instanceof Expression[]) {
			return (Expression[]) args;
		}
		return Arrays.stream(args).map(o -> {
			if (o instanceof Expression) {
				return (Expression) o;
			}
			if (o instanceof String) {
				return new Var(o.toString());
			}
			if (o instanceof Character) {
				return new CharValue(((Character) o).charValue());
			}
			if (o instanceof Integer) {
				return new IntValue(((Integer) o).intValue());
			}
			if (o instanceof blue.origami.common.Symbol) {
				return new SymbolValue(o.toString());
			}
			if (o instanceof ByteSet) {
				return new ByteSetValue((ByteSet) o);
			}
			if (o instanceof byte[]) {
				return new IndexValue((byte[]) o);
			}
			if (o == null) {
				return new Var("EmptyTag");
			}
			return new Var(o.toString() + ":" + o.getClass().getSimpleName());
		}).toArray(Expression[]::new);
	}

	public Expression p(String expr, Object... args) {
		return this.p_(expr, this.filter(args));
	}

	public Expression apply(String func, Object... args) {
		if (func == null) {
			return new Lambda((String) args[0], (Expression) args[1]);
		}
		return new Apply(func, this.filter(args));
	}

	public Expression ifexpr(Expression cnd, Expression thn, Expression els) {
		return new IfExpr(cnd, thn, els);
	}

	public DefFunc define(String name, String... params) {
		return new DefFunc(name, params);
	}

	/* */

	public void emit(ParserGrammar g, OWriter out) throws IOException {
		// this.grammar = g;
		Production start = g.getStartProduction();
		// if (!Typestate.compute(start) == Typestate.Tree) {
		// this.mask = this
		// }
		if (Stateful.isStateful(start)) {
			this.mask |= (TREE | STATE);
		}
		// this.isBinary = g.isBinaryGrammar();
		// this.isStateful = Stateful.isStateful(start);
		this.setupSymbols();
		NezCC2Visitor2 pgv = new NezCC2Visitor2(this.mask);
		pgv.start(g, this);
		ArrayList<String> funcList = pgv.sortFuncList("start");
		if (this.isDefined("Dhead" + this.mask)) {
			out.println(this.formatMap.get("Dhead" + this.mask));
		} else {
			if (this.isDefined("Dhead")) {
				out.println(this.formatMap.get("Dhead"));
			}
		}
		for (String cs : this.constList) {
			out.println(cs);
		}
		out.println("");
		out.println("");
		for (String fn : runtimeFuncs1) {
			if (this.usedNames.contains(fn)) {
				out.println(this.getDefined(fn));
			}
		}
		if (this.isDefined("prototype")) {
			for (String funcName : pgv.crossRefNames) {
				out.println(String.format(this.s("prototype"), funcName));
			}
		}
		for (String funcName : funcList) {
			String code = pgv.getParseFunc(funcName);
			if (code != null) {
				out.println(code);
			}
		}
		if (this.isDefined("Dmain" + this.mask)) {
			out.println(this.formatMap.get("Dmain" + this.mask));
		} else {
			if (this.isDefined("Dmain")) {
				out.println(this.formatMap.get("Dmain"));
			}
		}
	}

	/* */

	String getDefined(String key) {
		if (this.isDefined("D" + key)) {
			return this.formatMap.get("D" + key);
		}
		try {
			Field f = this.getClass().getField(key);
			return ((Lib) f.get(this)).gen().toString();
		} catch (Exception e) {
			return "TODO " + key + " => " + e;
		}
	}

	interface Lib {
		Expression gen();
	}

	public Lib neof = () -> {
		return new DefFunc("neof", "px").is("px.pos < px.length");
	};

	public Lib mnext1 = () -> {
		return new DefFunc("mnext1", "px").is("px.pos = px.pos + 1; true");
	};

	public Lib mback1 = () -> {
		return new DefFunc("mback1", "px", "pos").is("px.pos = pos; true");
	};

	public Lib mback3 = () -> {
		return new DefFunc("mback3", "px", "pos", "tree").is("px.pos = pos; px.tree = tree; true");
	};

	public Lib mback7 = () -> {
		return new DefFunc("mback7", "px", "pos", "tree", "state")
				.is("px.pos = pos; px.tree = tree; px.state = state; true");
	};

	public Lib choice1 = () -> {
		return new DefFunc("choice1", "px", "e", "ee").is("let pos = px.pos; e(px) || mback1(px, pos) && ee(px)");
	};

	public Lib choice3 = () -> {
		return new DefFunc("many3", "px", "e", "ee")
				.is("let pos = px.pos; let tree = px.tree; e(px) ? e(px) || mback3(px, pos, tree) && ee(px)");
	};

	public Lib choice7 = () -> {
		return new DefFunc("choice7", "px", "e", "ee").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; e(px) || mback7(px, pos, tree, state) && ee(px)");
	};

	public Lib many1 = () -> {
		return new DefFunc("many1", "px", "e").is("let pos = px.pos; e(px) ? many1(px, e) ? mback1(px, pos)");
	};

	public Lib many3 = () -> {
		return new DefFunc("many3", "px", "e")
				.is("let pos = px.pos; let tree = px.tree; e(px) ? many3(px, e) ? mback3(px, pos, tree)");
	};

	public Lib many7 = () -> {
		return new DefFunc("many7", "px", "e").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; e(px) ? many7(px, e) ? mback7(px, pos, tree, state)");
	};

	public Lib many9 = () -> {
		return new DefFunc("many9", "px", "e")
				.is("let pos = px.pos; e(px) && px.pos < pos ? many9(px, e) ? mback1(px, pos)");
	};

	public Lib many12 = () -> {
		return new DefFunc("many12", "px", "e").is(
				"let pos = px.pos; let tree = px.tree; e(px) && px.pos < pos ? many12(px, e) ? mback3(px, pos, tree)");
	};

	public Lib many16 = () -> {
		return new DefFunc("many16", "px", "e").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; e(px) && px.pos < pos ? many16(px, e) ? mback7(px, pos, tree, state)");
	};

	public Lib and1 = () -> {
		return new DefFunc("and1", "px", "e").is("let pos = px.pos; e(px) && mback1(px, pos)");
	};

	public Lib not1 = () -> {
		return new DefFunc("and1", "px", "e").is("let pos = px.pos; !e(px) && mback1(px, pos)");
	};

	public Lib not3 = () -> {
		return new DefFunc("not3", "px", "e")
				.is("let pos = px.pos; let tree = px.tree; !e(px) && mback3(px, pos, tree)");
	};

	public Lib not7 = () -> {
		return new DefFunc("not7", "px", "e").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; !e(px) && mback7(px, pos, tree, state)");
	};

	public Lib inc = () -> {
		return new DefFunc("inc", "px").is("let pos = px.pos; px.pos = px.pos + 1; pos");
	};

	/* Tree Construction */

	public Lib mtree = () -> {
		return new DefFunc("mtree", "px", "tag", "spos", "epos")
				.is("px.tree = ctree(tag, px.inputs, spos, epos, px.tree); true");
	};

	public Lib mlink = () -> {
		return new DefFunc("mlink", "px", "tag", "child", "prev").is("px.tree = clink(tag, child, prev); true");
	};

	public Lib newtree = () -> {
		return new DefFunc("newtree", "px", "spos", "e", "tag", "epos")
				.is("let pos = px.pos; px.tree = EmptyTree; e(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Lib foldtree = () -> {
		return new DefFunc("foldtree", "px", "spos", "label", "e", "tag", "epos").is(
				"let pos = px.pos; mlink(px, label, px.tree, EmptyTree) && e(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Lib linktree = () -> {
		return new DefFunc("linktree", "px", "tag", "e")
				.is("let tree = px.tree; e(px) && mlink(px, tag, px.tree, tree)");
	};

	public Lib tagtree = () -> {
		return new DefFunc("tagtree", "px", "tag").is("mlink(px, tag, EmptyTree, px.tree)");
	};

	public Lib detree = () -> {
		return new DefFunc("detree", "px", "e").is("let tree = px.tree; e(px) && mback3(px, px.pos, tree)");
	};

	public Lib mconsume1 = () -> {
		return new DefFunc("mconsume1", "px", "memo").is("px.pos = memo.mpos; memo.matched");
	};

	public Lib mconsume3 = () -> {
		return new DefFunc("mconsume3", "px", "memo").is("px.pos = memo.mpos; px.tree = memo.mtree; memo.matched");
	};

	public Lib mconsume7 = () -> {
		return new DefFunc("mconsume7", "px", "memo")
				.is("px.pos = memo.mpos; px.tree = memo.mtree; px.state = memo.mstate; memo.matched");
	};

	public Lib mstore1 = () -> {
		return new DefFunc("mstore3", "px", "memo", "key", "pos", "matched")
				.is("memo.key = key; memo.mpos = pos; memo.matched = matched; matched");
	};

	public Lib mstore3 = () -> {
		return new DefFunc("mstore3", "px", "memo", "key", "pos", "matched")
				.is("memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.matched = matched; matched");
	};

	public Lib mstore7 = () -> {
		return new DefFunc("mstore7", "px", "memo", "key", "pos", "res").is(
				"memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.mstate = px.store; memo.matched = matched; matched");
	};

	public Lib getkey = () -> {
		return new DefFunc("getkey", "pos", "mp").asType("key").is("pos * memosize + mp");
	};

	public Lib getmemo = () -> {
		return new DefFunc("getmemo", "px", "key").asType("memo").is("px.memos[key!(key % memolen)]");
	};

	public Lib memo1 = () -> {
		return new DefFunc("memo1", "px", "mp", "e").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); memo.key == key ? mconsume1(px, memo) ? mstore1(px, memo, key, pos, e(px))");
	};

	public Lib memo3 = () -> {
		return new DefFunc("memo3", "px", "mp", "e").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); memo.key == key ? mconsume3(px, memo) ? mstore3(px, memo, key, pos, e(px))");
	};

	public Lib memo7 = () -> {
		return new DefFunc("memo7", "px", "mp", "e").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); memo.key == key ? mconsume7(px, memo) ? mstore7(px, memo, key, pos, e(px))");
	};

	private static String[] runtimeFuncs1 = { //
			"mnext1", "neof", //
			"mback1", "mback3", "mback7", //
			"choice1", "choice3", "choice7", //
			"many1", "many3", "many7", //
			"many9", "many12", "many16", //
			"and1", "not1", "not3", "not7", //
			"mtree", "mlink", "newtree", "foldtree", "linktree", "tagtree", //
			"getkey", "getmemo", //
			"mconsume1", "mconsume3", "mconsume7", //
			"mstore1", "mstore3", "mstore7", //
			"memo1", "memo3", "memo7", //
	};

	public Expression dispatch(Expression eJumpIndex, List<Expression> exprs) {
		return new Dispatch(eJumpIndex, exprs);
	}

	class Dispatch extends Expression {
		Expression eJumpIndex;
		Expression[] exprs;

		public Dispatch(Expression eJumpIndex, List<Expression> exprs) {
			this.eJumpIndex = eJumpIndex;
			this.exprs = exprs.toArray(new Expression[exprs.size()]);
		}

		@Override
		public Expression ret() {
			for (int i = 0; i < this.exprs.length; i++) {
				this.exprs[i] = this.exprs[i].ret();
			}
			return this;
		}

		@Override
		public Expression deret() {
			for (int i = 0; i < this.exprs.length; i++) {
				this.exprs[i] = this.exprs[i].deret();
			}
			return this;
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined("switch")) {
				w.format(this.formatOf("switch"), this.eJumpIndex);
				w.incIndent();
				for (int i = 0; i < this.exprs.length; i++) {
					w.format(this.formatOf("case", "\t| %s => %s\n"), i, this.exprs[i]);
				}
				if (this.isDefined("default")) {
					w.format(this.formatOf("default", "\t| _ => %s\n"), this.exprs[0]);
				}
				w.decIndent();
				w.wIndent(this.formatOf("end switch", "end", ""));
				return;
			} else if (this.isDefined("Tfuncs")) {
				this.deret();
				new Apply(new GetIndex(new FuncValue(this.exprs), this.eJumpIndex), new Var("px")).ret().emit(w);
			} else {
				this.deret();
				Expression tail = this.exprs[0];
				for (int i = this.exprs.length - 1; i > 0; i--) {
					tail = new IfExpr(NezCC2.this.p("pos == $0", i), this.exprs[i], tail);
					if (i > 0) {
						tail = new Unary("group", tail);
					}
				}
				tail = new LetIn("pos", this.eJumpIndex).add(tail);
				tail.ret().emit(w);
			}
		}

		class FuncValue extends Expression {
			Expression[] exprs;

			FuncValue(Expression[] data) {
				this.exprs = data;
			}

			@Override
			void emit(Writer w0) {
				Writer w = new Writer();
				w.push(this.formatOf("array", "["));
				for (int i = 0; i < this.exprs.length; i++) {
					if (i > 0) {
						w.push(this.formatOf("delim array", "delim", " "));
					}
					w.push(new Lambda("px", this.exprs[i]));
				}
				w.push(this.formatOf("end array", "]"));
				w0.format(this.formatOf("constname", "%s",
						NezCC2.this.constName(this.typeOf("funcs"), "funcs", this.exprs.length, w.toString())));
			}
		}

	}

}
