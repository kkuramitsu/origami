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
import java.util.HashMap;
import java.util.HashSet;

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
import blue.origami.parser.peg.Typestate;

public class NezCC2 implements OFactory<NezCC2> {
	private boolean treeConstruction = true;

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
		this.treeConstruction = options.is(MainOption.TreeConstruction, true);
		String file0 = options.stringValue(MainOption.GrammarFile, "parser.opeg");
		String base = SourcePosition.extractFileBaseName(file0);
		this.defineSymbol("base", base);
		this.defineSymbol("nezcc", "nezcc/2.0");
		this.defineSymbol("space", " ");

		String[] files = options.stringList(MainOption.InputFiles);
		for (String file : files) {
			if (!file.endsWith(".nezcc")) {
				continue;
			}
			if (!new File(file).isFile()) {
				file = Version.ResourcePath + "/nezcc/" + file;
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
					name = line.substring(0, loc - 1).trim();
					String value = line.substring(loc + 1).trim();
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
		// this.defineSymbol("tab", " ");
		// if (this.isDefined("eq")) {
		// this.defineSymbol("==", this.s("eq"));
		// }
		// if (this.isDefined("ne")) {
		// this.defineSymbol("!=", this.s("ne"));
		// }
		// if (this.isDefined("options")) {
		// for (String opt : this.s("options").split(",")) {
		// this.defineSymbol("O" + opt, opt);
		// }
		// }
		//
		// if (this.isDefined("Array")) {
		// this.defineSymbol("Byte[]", this.format("Array", this.s("Byte")));
		// }
		// this.defineSymbol("Int8", this.s("Byte"));
		// this.defineSymbol("Symbol", this.s("String"));
		//
		// if (!this.isDefined("Tpx")) {
		// String t = this.format("structname", "NezParserContext");
		// this.defineVariable("px", t);
		// }
		// if (!this.isDefined("TtreeLog")) {
		// String t = this.format("structname", "TreeLog");
		// if (this.isDefined("Option")) {
		// this.defineVariable("tcur", t);
		// t = this.format("Option", t);
		// }
		// this.defineVariable("treeLog", t);
		// }
		// if (!this.isDefined("Tstate")) {
		// String t = this.format("structname", "State");
		// if (this.isDefined("Option")) {
		// this.defineVariable("scur", t);
		// t = this.format("Option", t);
		// }
		// this.defineVariable("state", t);
		// }
		// this.defineVariable("tcur", this.T("treeLog"));
		// this.defineVariable("scur", this.T("state"));
		// if (this.isDefined("functype")) {
		// if (this.isAliasFuncType()) { // alias version
		// this.defineVariable("newFunc", this.format("structname", "TreeFunc"));
		// this.defineVariable("setFunc", this.format("structname", "TreeSetFunc"));
		// this.defineVariable("f", this.format("structname", "ParserFunc"));
		// }
		// } else {
		// this.defineVariable("newFunc", this.s("TreeFunc"));
		// this.defineVariable("setFunc", this.s("TreeSetFunc"));
		// this.defineVariable("f", this.s("ParserFunc"));
		// }
		//
		// this.defineVariable("matched", this.s("Bool"));
		// this.defineVariable("inputs", this.s("Byte[]"));
		// this.defineVariable("pos", this.s("Int"));
		// this.defineVariable("headpos", this.T("pos"));
		// this.defineSymbol("backpos", "backpos");
		// this.defineVariable("length", this.s("Int"));
		// this.defineVariable("tree", this.s("Tree"));
		// this.defineVariable("c", this.s("Int"));
		// this.defineVariable("n", this.s("Int"));
		// this.defineVariable("cnt", this.s("Int"));
		// this.defineVariable("shift", this.s("Int"));
		//
		// if (this.isDefined("Int32")) {
		// this.defineVariable("bits", this.format("Array", this.s("Int32")));
		// } else {
		// this.defineVariable("bits", this.format("Array", this.s("Bool")));
		// }
		//
		// this.defineVariable("label", this.s("Symbol"));
		// this.defineVariable("tag", this.s("Symbol"));
		// this.defineVariable("value", this.T("inputs"));
		//
		// this.defineVariable("lop", this.s("Int"));
		// this.defineVariable("lpos", this.T("length"));
		// this.defineVariable("ltree", this.T("tree")); // haskell
		//
		// if (!this.isDefined("m")) {
		// String t = this.format("structname", "MemoEntry");
		// this.defineVariable("m", t);
		// }
		//
		// if (this.isDefined("MemoList")) {
		// this.defineVariable("memos", this.format("MemoList", this.T("m")));
		// } else {
		// this.defineVariable("memos", this.format("Array", this.T("m")));
		// }
		//
		// this.defineVariable("subtrees", this.s("TreeList"));
		// // this.defineSymbol("TreeList.empty", this.s("null"));
		// // this.defineSymbol("TreeList.cons", "%3$s");
		//
		// if (this.isDefined("Int64")) {
		// this.defineVariable("key", this.s("Int64"));
		// } else {
		// this.defineVariable("key", this.s("Int"));
		// }
		// this.defineVariable("mpoint", this.s("Int"));
		// this.defineVariable("result", this.s("Int"));
		// this.defineVariable("text", this.s("String"));
		//
		// this.defineVariable("label", this.s("Symbol"));
		// this.defineVariable("tag", this.s("Symbol"));
		// this.defineVariable("ntag", this.T("cnt"));
		// this.defineVariable("ntag0", this.T("cnt"));
		// this.defineVariable("nlabel", this.T("cnt"));
		// this.defineVariable("value", this.T("inputs"));
		// this.defineVariable("nvalue", this.T("cnt"));
		// this.defineVariable("spos", this.T("cnt"));
		// this.defineVariable("epos", this.T("cnt"));
		// this.defineVariable("shift", this.T("cnt"));
		// this.defineVariable("length", this.T("cnt"));
		//
		// this.defineVariable("memoPoint", this.T("cnt"));
		// this.defineVariable("result", this.T("cnt"));
		//
		// this.defineVariable("mpos", this.T("pos")); // haskell
		// this.defineVariable("mtree", this.T("tree")); // haskell
		// this.defineVariable("mstate", this.T("state")); // haskell
		//
		// this.defineVariable("lprev", this.T("treeLog"));
		// this.defineVariable("lnext", this.T("treeLog"));
		// this.defineVariable("sprev", this.T("state"));
		//
		// this.defineVariable("epos", this.T("pos"));
		// this.defineVariable("child", this.T("tree"));
		// this.defineVariable("f2", this.T("f"));
		// this.defineSymbol("Intag", "0");
		// this.defineSymbol("Ikey", "-1");
		// this.defineSymbol("Icnt", "0");
		// this.defineSymbol("Ipos", "0");
		// this.defineSymbol("Ilop", "0");
		// this.defineSymbol("Ilpos", "0");
		// this.defineSymbol("Iheadpos", "0");
		// this.defineSymbol("Iresult", "0");
		// if (this.isDefined("paraminit")) {
		// this.defineSymbol("PInewFunc", this.emitFuncRef("newAST"));
		// this.defineSymbol("PIsetFunc", this.emitFuncRef("subAST"));
		// }
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

		// void pushLine(String line) {
		// this.sb.append(line + "\n");
		// }
		//
		// void pushIndentLine(String line) {
		// this.sb.append(this.Indent(" ", line) + "\n");
		// }

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
							System.out.printf("n=%s  %d,%s\n", format, n, args.length);
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
			return NezCC2.this.formatMap.get("T" + key);
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

		public Expression and(Expression next) {
			return new Infix(this, "&&", next);
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
			w.push(this.formatOf(this.name, this.name));
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

		public DefFunc is(Expression e) {
			this.body = e;
			return this;
		}

		public DefFunc asType(String t) {
			this.ret = this.typeOf(t);
			return this;
		}

		@Override
		void emit(Writer w) {
			// w.push("" + this.value);
			// String funcType = "";
			// if (this.isDefined("functype") && !this.isAliasFuncType()) {
			// ArrayList<String> l = new ArrayList<>();
			// for (String p : this.params) {
			// l.add(this.format("functypeparam", this.T(p), p));
			// }
			// funcType = this.format("functype", ret, funcName,
			// this.emitList("functypeparams", l));
			// }
			// String f = "function" + acc;
			// if (!this.isDefined(f)) {
			// f = "function";
			// }
			// this.writeSection(this.format(f, ret, this.funcName(funcName),
			// this.emitParams(this.params), funcType));
			w.format(this.formatOf("function", "%s %s(%s) = "), this.ret, this.name, new Params(this.params));
			w.incIndent();
			w.format(this.formatOf("body function", "\n\t%s"), this.body);
			w.decIndent();
			w.push(this.formatOf("end function", "end", ""));
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
			w.format(this.formatOf("int", "%d"), this.value);
		}
	}

	class CharValue extends Expression {
		int uchar;

		public CharValue(char uchar) {
			this.uchar = uchar;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("char", "%d"), this.uchar & 0xff);
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

		public IndexValue(byte[] data) {
			this.indexMap = data;
		}

		@Override
		void emit(Writer w0) {
			// if (this.isDefined("base64")) {
			// byte[] encoded = Base64.getEncoder().encode(this.indexMap);
			// return this.getConstName(this.s("Int8"), "choice", encoded.length,
			// this.format("base64", new String(encoded)));
			// }
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

	class ByteSetValue extends Expression {
		ByteSet bs;

		public ByteSetValue(ByteSet bs) {
			this.bs = bs;
		}

		@Override
		void emit(Writer w0) {
			Writer w = new Writer();
			w.push(this.formatOf("array", "["));
			for (int i = 0; i < 8; i++) {
				if (i > 0) {
					w.push(this.formatOf("delim array", "delim", " "));
				}
				w.push(new IntValue(this.bs.bits()[i]));
			}
			w.push(this.formatOf("end array", "]"));
			w0.format(
					this.formatOf("constname", "%s", NezCC2.this.constName(this.typeOf("bs"), "bs", 8, w.toString())));
		}

	}

	HashSet<String> usedNames = new HashSet<>();

	class FuncRef extends Expression {
		String name;

		public FuncRef(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			NezCC2.this.usedNames.add(this.name);
			w.format(this.formatOf("funcref", "%s"), this.name);
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
		// if (typeName == null) {
		// return typeLiteral;
		// }
		String key = typeName + value;
		String constName = this.constNameMap.get(key);
		if (constName == null) {
			constName = prefix + this.constNameMap.size();
			this.constNameMap.put(key, constName);
			NezCC2.Expression c = new Const(typeName, constName, arraySize, value);
			this.constList.add(c.toString());
			// this.declConst(typeName, constName, arraySize, typeLiteral);
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

		LetIn(String name, Expression right, Expression next) {
			this.name = name;
			this.right = right;
			this.next = next;
		}

		public LetIn(Expression e, Expression e2) {
			// TODO Auto-generated constructor stub
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("let", "val", "var", "%s %s = %s\n\t%s"), new Type(this.name), new Var(this.name),
					this.right, this.next);
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

	class Mutate extends Expression {
		String base;
		String[] fields;
		Expression[] values;

		Mutate(String base, Object... args) {
			this.base = base;
			this.fields = new String[args.length / 2];
			this.values = new Expression[args.length / 2];
			for (int i = 0; i < args.length / 2; i++) {
				this.fields[i] = (String) args[i * 2];
				this.values[i] = (Expression) args[i * 2 + 1];
			}
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined("mut")) {
				// Haskell
			}
			if (this.base == null) {
				for (int i = 0; i < this.fields.length; i++) {
					if (i > 0) {
						w.format(this.formatOf(";", "\n\t"));
					}
					w.format(this.formatOf("assign", "%s = %s"), new Var(this.fields[i]), this.values[i]);
				}
			} else {
				for (int i = 0; i < this.fields.length; i++) {
					if (i > 0) {
						w.format(this.formatOf(";", "\n\t"));
					}
					w.format(this.formatOf("setter", "%s.%s = %s"), new Var(this.base),
							this.fieldOf(this.base, this.fields[i]), this.values[i]);
				}
			}
		}
	}

	class Apply extends Expression {
		Expression left;
		Expression right;

		Apply(Expression left, Expression... right) {
			this.left = left;
			this.right = right.length == 1 ? right[0] : new Args(right);
		}

		Apply(String left, Expression... right) {
			this(new FuncName(left), right);
			NezCC2.this.usedNames.add(left);
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("apply", "%s(%s)"), this.left, this.right);
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

	Expression unary(String expr, Expression... args) {
		expr = expr.trim();
		int pos = expr.indexOf('.');
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
		if (expr.endsWith(")")) {
			pos = expr.indexOf('(');
			if (pos > 0) {
				String[] tokens = expr.substring(pos + 1, expr.length() - 1).split(",");
				Expression[] a = Arrays.stream(tokens).map(s -> this.p_(s, args)).toArray(Expression[]::new);
				return new Apply(expr.substring(0, pos), a);
			}
			return new Unary("group", this.p_(expr.substring(1, expr.length() - 1), args));
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
		int pos = expr.indexOf(op);
		if (pos > 0) {
			return f.apply(this.p_(expr.substring(0, pos).trim(), args), this.p_(expr.substring(pos + 1).trim(), args));
		}
		return null;
	}

	Expression p_(String expr, Expression[] args) {
		Expression p = null;
		// let n = expr ; ...
		if (expr.startsWith("let ")) {
			return this.bin(expr, ";", args, (e, e2) -> new LetIn(e, e2));
		}
		p = this.bin(expr, "= ", args, (e, e2) -> new Infix(e, "=", e2));
		if (p != null) {
			return p;
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
		return this.unary(expr, args);
	}

	private Expression[] filter(Object... args) {
		if (args instanceof Object[]) {
			return (Expression[]) args;
		}
		return Arrays.stream(args).map(o -> {
			if (o instanceof Expression) {
				return (Expression) o;
			}
			if (o instanceof Character) {
				return new CharValue(((Character) o).charValue());
			}
			if (o instanceof Integer) {
				return new IntValue(((Integer) o).intValue());
			}
			if (o instanceof Symbol) {
				return new SymbolValue(o.toString());
			}
			if (o instanceof byte[]) {
				return new IndexValue((byte[]) o);
			}
			return new Var(o.toString());
		}).toArray(Expression[]::new);
	}

	public Expression p(String expr, Object... args) {
		return this.p_(expr, this.filter(args));
	}

	public Expression apply(String func, Object... args) {
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
		NezCC2Visitor2 pgv = new NezCC2Visitor2();
		// this.grammar = g;
		Production start = g.getStartProduction();
		if (this.treeConstruction) {
			this.treeConstruction = Typestate.compute(start) == Typestate.Tree;
		}
		// this.isBinary = g.isBinaryGrammar();
		// this.isStateful = Stateful.isStateful(start);
		// this.log("tree: %s", this.treeConstruction);
		// this.log("stateful: %s", this.isStateful);
		this.setupSymbols();
		pgv.start(g, this);
		ArrayList<String> funcList = pgv.sortFuncList("start");

		if (this.isDefined("Dhead")) {
			out.println(this.formatMap.get("Dhead"));
		}
		for (String cs : this.constList) {
			out.println(cs);
		}
		for (String fn : this.usedNames) {
			out.println(this.getDefined(fn));
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
		if (this.isDefined("Dmain")) {
			out.println(this.formatMap.get("Dmain"));
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
		return new DefFunc("neof", "px").is(this.p("px.pos < px.length"));
	};

}
