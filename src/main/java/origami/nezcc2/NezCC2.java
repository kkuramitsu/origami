package origami.nezcc2;

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
import origami.nez2.BitChar;
import origami.nez2.PEG;

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
		this.defineSymbol(" ", " ");
		this.defineSymbol("\\n", "\n");
		this.defineSymbol("\\t", "\t");

		if (options.is(MainOption.TreeConstruction, true)) {
			this.mask |= TREE;
		}
		String file = options.stringValue(MainOption.FromFile, "chibi.nezcc");
		int p = file.indexOf("+");
		int p2 = file.indexOf("-");
		if (p > 0 || p2 > 0) {
			p = (p != -1 && p2 != -1) ? Math.min(p, p2) : Math.max(p, p2);
			String s = file.substring(p).replace(".nezcc", "");
			file = file.substring(0, p) + ".nezcc";
			s = s.replace("+", ",").replace("-", ",!").substring(1);
			this.defineSymbol("localoptions", s);
		}
		if (!new File(file).isFile()) {
			file = Version.ResourcePath + "/syntax/" + file;
		}
		this.importNezccFile(file);
	}

	HashMap<String, String> formatMap = new HashMap<>();

	void importNezccFile(String path) {
		try {
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : NezCC2.class.getResourceAsStream(path);
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

	void undefineSymbol(String key) {
		this.formatMap.remove(key);
	}

	String s(String skey) {
		return this.formatMap.getOrDefault(skey, skey);
	}

	String T(String skey) {
		return this.formatMap.getOrDefault("T" + skey, skey);
	}

	void setupSymbols() {
		this.defineSymbol("Omutable", "1");
		this.defineSymbol("Ostrcmp", "1");
		for (String opt : this.s("options").split(",")) {
			if (opt.startsWith("!")) {
				this.undefineSymbol(opt.substring(1));
				this.undefineSymbol("O" + opt.substring(1));
			} else {
				this.defineSymbol("O" + opt, "1");
			}
		}
		this.defineSymbol("tab", "\t");
		this.defineSymbol("Tspos", this.s("Tpos"));
		this.defineSymbol("Tepos", this.s("Tpos"));
		this.defineSymbol("Tmp", this.s("Tpos"));
		this.defineSymbol("Tlabel", this.s("Ttag"));
		this.defineSymbol("Tchild", this.s("Ttree"));
		this.defineSymbol("Tprev", this.s("Ttree"));
		this.defineSymbol("Tpe4", this.s("Tpe"));
		this.defineSymbol("Tpe2", this.s("Tpe"));
		this.defineSymbol("Tpe3", this.s("Tpe"));
		this.defineSymbol("Tbm", this.s("Tbs")); // bitmap
		this.defineSymbol("Tch", this.s("Tpos")); // ch
		this.defineSymbol("Tch2", this.s("Tch")); // ch
		this.defineSymbol("Tch3", this.s("Tch")); // ch
		this.defineSymbol("Tch4", this.s("Tch")); // ch
		this.defineSymbol("Tns", this.s("Tmp"));
		this.defineSymbol("Tslen", this.s("Tpos"));
		this.defineSymbol("Tstext", this.s("Tinputs"));
		this.defineSymbol("Tetext", this.s("Tinputs"));

		for (String opt : this.s("localoptions").split(",")) {
			if (opt.startsWith("!")) {
				this.undefineSymbol(opt.substring(1));
				this.undefineSymbol("O" + opt.substring(1));
			} else {
				this.defineSymbol("O" + opt, "1");
			}
		}
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
			if (value instanceof ENode) {
				((ENode) value).emit(this);
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

	public abstract class ENode {
		abstract void emit(Writer w);

		void comment(Writer w, String msg) {
			w.push(String.format(this.formatOf("comment", ""), msg));
		}

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

		public ENode ret() {
			return new Return(this);
		}

		public ENode deret() {
			return this;
		}

		public ENode and(ENode next) {
			return new Infix(this, "&&", next);
		}

		public ENode or(ENode next) {
			return new Infix(this, "||", next);
		}

		public ENode add(ENode next) {
			return new Block(this, next);
		}
	}

	class Symbol extends ENode {
		String symbol;

		Symbol(String symbol) {
			this.symbol = symbol;
		}

		@Override
		void emit(Writer w) {
			w.push(this.formatOf(this.symbol, this.symbol));
		}
	}

	class FuncName extends ENode {
		String name;

		FuncName(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			w.push(this.name);
		}
	}

	class Var extends ENode {
		String name;

		Var(String name) {
			if (this.isDefined(name)) {
				this.name = NezCC2.this.s(name);
			} else if (this.isDefined("varname")) {
				this.name = String.format(NezCC2.this.s("varname"), name);
			} else {
				this.name = name;
			}
		}

		@Override
		void emit(Writer w) {
			w.push(this.formatOf(this.name, this.name));
		}
	}

	class Type extends ENode {
		String name;

		Type(String name) {
			this.name = name;
		}

		@Override
		void emit(Writer w) {
			w.push(this.typeOf(this.name));
		}

	}

	public class FuncDef extends ENode {
		String name;
		String ret;
		String[] params;
		ENode body;

		public FuncDef(String name, String... params) {
			this.name = name;
			this.params = params;
			this.ret = this.typeOf("matched");
			this.body = null;
		}

		@Override
		public FuncDef add(ENode e) {
			this.body = e.ret();
			return this;
		}

		public FuncDef is(String code, Object... a) {
			return this.add(NezCC2.this.p(code, a));
		}

		public FuncDef asType(String t) {
			this.ret = this.typeOf(t);
			return this;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("function", "%2$s(%3$s) :%1$s = "), this.ret, this.name, new Params(this.params));
			w.incIndent();
			w.format(this.formatOf("body function", "\n\t%s"), this.body);
			w.decIndent();
			w.format(this.formatOf("end function", "end", "\n"));
		}

		class Params extends ENode {
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

		class Param extends ENode {
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

	class RecFuncDef extends FuncDef {

		public RecFuncDef(String name, String... params) {
			super(name, params);
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("function rec", "function", "%2$s(%3$s) :%1$s = "), this.ret, this.name,
					new Params(this.params));
			w.incIndent();
			w.format(this.formatOf("body function", "\n\t%s"), this.body);
			w.decIndent();
			w.format(this.formatOf("end function", "end", "\n"));
		}

	}

	class Return extends ENode {
		ENode e;

		public Return(ENode e) {
			this.e = e;
		}

		@Override
		public ENode ret() {
			return this;
		}

		@Override
		public ENode deret() {
			return this.e;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("return", "%s"), this.e);
		}
	}

	class IfExpr extends ENode {
		ENode cnd;
		ENode thn;
		ENode els;

		public IfExpr(ENode cnd, ENode thn, ENode els) {
			this.cnd = cnd;
			this.thn = thn;
			this.els = els;
		}

		@Override
		public ENode ret() {
			if (this.isDefined("if")) {
				this.thn = this.thn.ret();
				this.els = this.els.ret();
				return this;
			}
			return super.ret();
		}

		@Override
		public ENode deret() {
			if (this.isDefined("if")) {
				this.thn = this.thn.deret();
				this.els = this.els.deret();
			}
			return this;
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined("if")) {
				w.format(this.formatOf("if"), this.cnd);
				w.incIndent();
				w.wIndent("");
				w.push(this.thn);
				w.decIndent();
				w.format(this.formatOf("else", "else"));
				w.incIndent();
				w.wIndent("");
				w.push(this.els);
				w.decIndent();
				w.format(this.formatOf("end if", "end", "}"));
			} else {
				w.format(this.formatOf("ifexpr", "%s ? %s : %s"), this.cnd, this.thn, this.els);
			}
		}
	}

	class WhileStmt extends ENode {
		ENode cnd;
		ENode thn;
		ENode els;

		public WhileStmt(ENode cnd, ENode thn, ENode els) {
			this.cnd = cnd;
			this.thn = thn;
			this.els = els;
		}

		@Override
		public ENode ret() {
			this.els = this.els.ret();
			return this;
		}

		@Override
		public ENode deret() {
			this.els = this.els.deret();
			return this;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("while", "while (%d) {"), this.cnd);
			w.incIndent();
			w.wIndent("");
			w.push(this.thn);
			w.decIndent();
			w.format(this.formatOf("end while", "end", "}"), "");
			w.wIndent("");
			w.push(this.els);
		}
	}

	class IntValue extends ENode {
		int value;

		public IntValue(int v) {
			this.value = v;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("int", "%s"), this.value);
		}
	}

	class CharValue extends ENode {
		int uchar;

		public CharValue(char uchar) {
			this.uchar = uchar;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("char", "%s"), this.uchar & 0xff);
		}

	}

	class StringValue extends ENode {
		String value;

		public StringValue(String sym) {
			this.value = sym;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("string", "str", "\"%s\""), this.value);
		}
	}

	class SymbolValue extends ENode {
		String sym;

		public SymbolValue(String sym) {
			this.sym = sym;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("symbol", "\"%s\""), this.sym);
		}
	}

	class IndexValue extends ENode {
		byte[] indexMap;

		IndexValue(byte[] data) {
			this.indexMap = data;
		}

		@Override
		void emit(Writer w0) {
			if (this.isDefined("Obase64")) {
				byte[] encoded = Base64.getEncoder().encode(this.indexMap);
				ENode index = NezCC2.this.apply("b64", new StringValue(new String(encoded)));
				w0.format(this.formatOf("constname", "%s", NezCC2.this.constName(this.typeOf("alt"), "alt",
						this.indexMap.length, index.toString(), null)));
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
						NezCC2.this.constName(this.typeOf("alt"), "alt", this.indexMap.length, w.toString(), null)));
			}
		}
	}

	ENode data(byte[] data) {

		class DataValue extends ENode {
			byte[] data;

			DataValue(byte[] data) {
				this.data = data;
			}

			@Override
			void emit(Writer w0) {
				if (this.isDefined("Obase64")) {
					byte[] encoded = Base64.getEncoder().encode(this.data);
					ENode index = NezCC2.this.apply("b64", new StringValue(new String(encoded)));
					w0.format(this.formatOf("constname", "%s", NezCC2.this.constName(this.typeOf("inputs"), "t",
							this.data.length, index.toString(), new String(this.data))));
				} else {
					Writer w = new Writer();
					w.push(this.formatOf("array", "["));
					for (int i = 0; i < this.data.length; i++) {
						if (i > 0) {
							w.push(this.formatOf("delim array", "delim", " "));
						}
						w.push(new IntValue(this.data[i] & 0xff));
					}
					w.push(this.formatOf("end array", "]"));
					w0.format(this.formatOf("constname", "%s", NezCC2.this.constName(this.typeOf("inputs"), "t",
							this.data.length, w.toString(), new String(this.data))));
				}
			}
		}
		return new DataValue(data);
	}

	class BitCharValue extends ENode {
		BitChar bs;

		public BitCharValue(BitChar bs) {
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
						NezCC2.this.constName(this.typeOf("bs"), "bs", 8, w.toString(), this.bs.toString())));
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
						NezCC2.this.constName(this.typeOf("bs"), "bs", 256, w.toString(), this.bs.toString())));
			}
			this.comment(w0, this.bs.toString());
		}
	}

	HashSet<String> usedNames = new HashSet<>();

	void used(String name) {
		this.usedNames.add(name);
	}

	class FuncRef extends ENode {
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

	static int uid = 0;

	class Lambda extends ENode {
		String name;
		ENode body;

		public Lambda(String name, ENode body) {
			this.name = name;
			this.body = body;
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined("funcref") && this.body instanceof Apply) {
				String name = ((Apply) this.body).left.toString();
				if (name.startsWith("e") && name.length() > 1 && Character.isDigit(name.charAt(1))) {
					// System.err.println("lamda->funcref: " + name);
					w.format(this.formatOf("funcref", "%s"), name);
					return;
				}
			}
			if (!this.isDefined("Oshadow")) {
				Writer w0 = new Writer();
				w0.format(this.formatOf("lambda", "\\%s %s"), this.name, this.body);
				w.push(w0.toString().replaceAll("px", "p" + (uid++)));
				return;
			}
			w.format(this.formatOf("lambda", "\\%s %s"), this.name, this.body);
		}
	}

	ArrayList<String> constList = new ArrayList<>();
	ArrayList<String> constList2 = new ArrayList<>();
	private HashMap<String, String> constNameMap = new HashMap<>();

	private String constName(String typeName, String prefix, int arraySize, String value, String comment) {
		String key = typeName + value;
		String constName = this.constNameMap.get(key);
		if (constName == null) {
			constName = prefix + this.constNameMap.size();
			this.constNameMap.put(key, constName);
			NezCC2.ENode c = new Const(typeName, constName, arraySize, value, comment);
			if (prefix.equals("jumptbl")) {
				this.constList2.add(c.toString());
			} else {
				this.constList.add(c.toString());
			}
		}
		return constName;
	}

	class Const extends ENode {
		String typeName;
		String constName;
		int arraySize;
		String value;
		String comment;

		public Const(String typeName, String constName, int arraySize, String value, String comment) {
			this.typeName = typeName;
			this.constName = constName;
			this.arraySize = arraySize;
			this.value = value;
			this.comment = comment;
		}

		@Override
		void emit(Writer w) {
			if (this.arraySize == -1) {
				w.format(this.formatOf("const", "%2$s = %3$s"), this.typeName, this.constName, this.value);
			} else {
				w.format(this.formatOf("const_array", "const", "%2$s = %3$s"), this.typeName, this.constName,
						this.value, this.arraySize);
			}
			if (this.comment != null) {
				this.comment(w, this.comment);
			}
		}
	}

	void declConst(String typeName, String constName, String value) {
		NezCC2.ENode c = new Const(typeName, constName, -1, value, null);
		this.constList.add(c.toString());
	}

	// Expression op(Expression left, String op, Expression right) {
	// return new Infix(left, op, right);
	// }

	class LetIn extends ENode {
		String name;
		ENode right;
		ENode next;

		LetIn(String name, ENode right) {
			this.name = name;
			this.right = right;
		}

		@Override
		public ENode ret() {
			this.next = this.next.ret();
			return this;
		}

		@Override
		public ENode deret() {
			this.next = this.next.deret();
			return this;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("let", "val", "var", "%s %s = %s\n\t%s"), new Type(this.name), new Var(this.name),
					this.right, this.next);
		}

		@Override
		public ENode add(ENode next) {
			if (this.next == null) {
				this.next = next;
			} else {
				this.next = this.next.add(next);
			}
			return this;
		}

	}

	class Getter extends ENode {
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

	class Block extends ENode {
		ENode[] sub;

		Block(ENode... sub) {
			this.sub = sub;
		}

		@Override
		public ENode ret() {
			this.sub[this.sub.length - 1] = this.sub[this.sub.length - 1].ret();
			return this;
		}

		@Override
		public ENode deret() {
			this.sub[this.sub.length - 1] = this.sub[this.sub.length - 1].deret();
			return this;
		}

		@Override
		void emit(Writer w) {
			// w.incIndent();
			int c = 0;
			for (ENode e : this.sub) {
				if (c > 0) {
					w.wIndent("");
				}
				w.format(this.formatOf("stmt", "%s\n"), e);
				c++;
			}
			// w.decIndent();
		}

		@Override
		public ENode add(ENode next) {
			ENode[] sub2 = new ENode[this.sub.length + 1];
			System.arraycopy(this.sub, 0, sub2, 0, this.sub.length);
			sub2[this.sub.length] = next;
			return new Block(sub2);
		}
	}

	ENode block(ENode... sub) {
		ENode first = sub[0];
		for (int i = 1; i < sub.length; i++) {
			first = first.add(sub[i]);
		}
		return first;
	}

	class Apply extends ENode {
		ENode left;
		ENode right;

		Apply(ENode left, ENode... right) {
			this.left = left;
			this.right = right.length == 1 ? right[0] : new Args(right);
		}

		Apply(String left, ENode[] right) {
			this(new FuncName(left), right);
			NezCC2.this.usedNames.add(left);
		}

		@Override
		void emit(Writer w) {
			String key = this.prefix(this.left) + "apply";
			if (this.isDefined(key)) {
				w.format(this.formatOf(key), this.left, this.right);
			} else {
				w.format(this.formatOf("apply", "%s(%s)"), this.left, this.right);
			}
		}

		String prefix(Object o) {
			String s = o.toString();
			for (int i = 1; i < s.length(); i++) {
				char c = s.charAt(i);
				if (Character.isDigit(c)) {
					return s.substring(0, i);
				}
			}
			return s;
		}

		class Args extends ENode {
			ENode[] args;

			Args(ENode... args) {
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

	class Macro extends ENode {
		String name;
		ENode e;

		Macro(String name, ENode e) {
			this.name = name;
			this.e = e;
		}

		@Override
		void emit(Writer w) {
			NezCC2.this.used(this.name);
			w.format(this.formatOf(this.name, "%s"), this.e);
		}
	}

	class GetIndex extends ENode {
		ENode left;
		ENode right;

		GetIndex(ENode left, ENode right) {
			this.left = left;
			this.right = right;
		}

		@Override
		void emit(Writer w) {
			w.format(this.formatOf("index", "%s[%s]"), this.left, this.right);
		}
	}

	class Infix extends ENode {
		String op;
		ENode left;
		ENode right;

		Infix(ENode left, String op, ENode right) {
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

	class Unary extends ENode {
		String op;
		ENode inner;

		Unary(String op, ENode inner) {
			this.op = op;
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
			if (c0 == '(' || c0 == '[' || c0 == '{') {
				level++;
			}
			if (c0 == ')' || c0 == ']' || c0 == '}') {
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
			if (c0 == '(' || c0 == '[' || c0 == '{') {
				level++;
			}
			if (c0 == ')' || c0 == ']' || c0 == '}') {
				level--;
			}
		}
		return -1;
	}

	private String[] flatSplit(String expr, char c) {
		ArrayList<String> l = new ArrayList<>();
		int p = this.flatIndexOf(expr, c);
		while (p != -1) {
			l.add(expr.substring(0, p).trim());
			expr = expr.substring(p + 1);
			p = this.flatIndexOf(expr, c);
		}
		l.add(expr.trim());
		return l.toArray(new String[l.size()]);
	}

	private String[] flatSplit2(String expr, char c) {
		int p = this.flatIndexOf(expr, c);
		assert (p != -1);
		return new String[] { expr.substring(0, p).trim(), expr.substring(p + 1) };
	}

	ENode unary(String expr, final ENode... args) {
		expr = expr.trim();
		// this.dump(expr, args);
		if (expr.startsWith("!")) {
			return new Unary("!", this.p_(expr.substring(1), args));
		}
		if (expr.startsWith("{") && expr.endsWith("}")) {
			return this.p_(expr.substring(1, expr.length() - 1), args);
		}
		if (expr.endsWith("]")) {
			int pos = this.flatIndexOf(expr, '[');
			ENode b = this.unary(expr.substring(0, pos), args);
			ENode e = this.p_(expr.substring(pos + 1, expr.length() - 1), args);
			return new GetIndex(b, e);
		}
		if (expr.endsWith(")")) {
			int pos = this.flatIndexOf(expr, '(');
			if (pos > 0) {
				String fname = expr.substring(0, pos).trim();
				String[] tokens = this.flatSplit(expr.substring(pos + 1, expr.length() - 1), ',');
				ENode[] a = Arrays.stream(tokens).map(s -> {
					return this.p_(s.trim(), args);
				}).toArray(ENode[]::new);
				if (fname.equals("while")) {
					assert a.length == 3;
					return new WhileStmt(a[0], a[1], a[2]);
				}
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
		if (expr.length() > 0 && Character.isDigit(expr.charAt(0))) {
			return new IntValue(Integer.parseInt(expr));
		}
		return new Var(expr);
	}

	@FunctionalInterface
	static interface Bin {
		ENode apply(ENode e, ENode e2);
	}

	ENode bin(String expr, String op, ENode[] args, Bin f) {
		assert (op.length() < 3);
		int pos = op.length() == 2 ? this.flatIndexOf(expr, op.charAt(0), op.charAt(1))
				: this.flatIndexOf(expr, op.charAt(0));
		if (pos > 0) {
			return f.apply(this.p_(expr.substring(0, pos).trim(), args),
					this.p_(expr.substring(pos + op.length()).trim(), args));
		}
		return null;
	}

	void dump(String expr, final ENode[] args) {
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

	ENode p_(String expr, final ENode[] args) {
		expr = expr.trim();
		// this.dump(expr, args);
		ENode p = null;
		// let n = expr ; ...
		int pos = this.flatIndexOf(expr, ';');
		if (pos > 0) {
			String[] tokens = this.flatSplit(expr, ';');
			ENode[] a = Arrays.stream(tokens).map(s -> this.p_(s.trim(), args)).toArray(ENode[]::new);
			return this.block(a);
		}
		if (expr.startsWith("let ")) {
			String[] t = this.flatSplit2(expr.substring(4), '=');
			return new LetIn(t[0], this.p_(t[1], args));
		}
		p = this.bin(expr, "==", args, (e, e2) -> new Infix(e, "==", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "!=", args, (e, e2) -> new Infix(e, "!=", e2));
		if (p != null) {
			return p;
		}
		p = this.bin(expr, "= ", args, (e, e2) -> new Infix(e, "=", e2));
		if (p != null) {
			return p;
		}
		pos = this.flatIndexOf(expr, '?');
		if (pos > 0) {
			String[] tokens = expr.split("\\?");
			ENode[] a = Arrays.stream(tokens).map(s -> this.p_(s.trim(), args)).toArray(ENode[]::new);
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

	private ENode[] filter(Object... args) {
		if (args instanceof ENode[]) {
			return (ENode[]) args;
		}
		return Arrays.stream(args).map(o -> {
			if (o instanceof ENode) {
				return (ENode) o;
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
			if (o instanceof BitChar) {
				return new BitCharValue((BitChar) o);
			}
			if (o instanceof byte[]) {
				return new IndexValue((byte[]) o);
			}
			if (o == null) {
				return new Var("EmptyTag");
			}
			return new Var(o.toString() + ":" + o.getClass().getSimpleName());
		}).toArray(ENode[]::new);
	}

	public ENode p(String expr, Object... args) {
		return this.p_(expr, this.filter(args));
	}

	public ENode apply(String func, Object... args) {
		if (func == null) {
			return new Lambda((String) args[0], (ENode) args[1]);
		}
		return new Apply(func, this.filter(args));
	}

	public FuncDef define(String name, String... params) {
		return new FuncDef(name, params);
	}

	/* */

	public void emit(PEG peg, String start, OWriter out) throws IOException {
		// Production start = g.getStartProduction();
		// if (Stateful.isStateful(start)) {
		// this.mask |= (TREE | STATE);
		// }
		this.setupSymbols();
		NezCC2Visitor2 pgv = new NezCC2Visitor2(this, this.mask);
		peg.generate(start, pgv);
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
		for (String cs : this.constList2) {
			out.println(cs);
		}
		out.println("");
		out.println("");
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
			e.printStackTrace();
			return "TODO " + key + " => " + e;
		}
	}

	interface Lib {
		ENode gen();
	}

	public Lib neof = () -> {
		return new FuncDef("neof", "px").is("px.pos < px.length");
	};

	public Lib succ = () -> {
		return new FuncDef("succ", "px").is("true");
	};

	public Lib fail = () -> {
		return new FuncDef("fail", "px").is("false");
	};

	public Lib mnext1 = () -> {
		return new FuncDef("mnext1", "px").is("px.pos = px.pos + 1; true");
	};

	public Lib mmov = () -> {
		return new FuncDef("mmov", "px", "pos").is("px.pos = px.pos + pos; px.pos < px.length");
	};

	public Lib match2 = () -> {
		return new FuncDef("match2", "px", "ch", "ch2").is("px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2");
	};

	public Lib match3 = () -> {
		return new FuncDef("match3", "px", "ch", "ch2", "ch3")
				.is("px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 && px.inputs[px.pos+2] == ch3");
	};

	public Lib match4 = () -> {
		return new FuncDef("match4", "px", "ch", "ch2", "ch3", "ch4").is(
				"px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 && px.inputs[px.pos+2] == ch3 && px.inputs[px.pos+3] == ch4");
	};

	public Lib matchmany = () -> {
		return new RecFuncDef("matchmany", "stext", "spos", "etext", "epos", "slen")
				.is("slen == 0 || (stext[spos] == etext[epos] && matchmany(stext, spos+1, etext, epos+1, slen-1))");
	};

	public Lib mback1 = () -> {
		return new FuncDef("mback1", "px", "pos").is("px.pos = errpos!(pos); true");
	};

	public Lib mback2 = () -> {
		return new FuncDef("mback2", "px", "tree").is("px.tree = tree; true");
	};

	public Lib mback3 = () -> {
		return new FuncDef("mback3", "px", "pos", "tree").is("px.pos = errpos!(pos); px.tree = tree; true");
	};

	public Lib mback4 = () -> {
		return new FuncDef("mback4", "px", "state").is("px.state = state; true");
	};

	public Lib mback7 = () -> {
		return new FuncDef("mback7", "px", "pos", "tree", "state")
				.is("px.pos = errpos!(pos); px.tree = tree; px.state = state; true");
	};

	public Lib maybe1 = () -> {
		return new FuncDef("maybe1", "px", "pe").is("let pos = px.pos; pe(px) || mback1(px, pos)");
	};

	public Lib maybe3 = () -> {
		return new FuncDef("maybe3", "px", "pe")
				.is("let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree)");
	};

	public Lib maybe7 = () -> {
		return new FuncDef("maybe7", "px", "pe").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state)");
	};

	public Lib or1 = () -> {
		return new FuncDef("or1", "px", "pe", "pe2").is("let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px)");
	};

	public Lib or3 = () -> {
		return new FuncDef("or3", "px", "pe", "pe2")
				.is("let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px)");
	};

	public Lib or7 = () -> {
		return new FuncDef("or7", "px", "pe", "pe2").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && pe2(px)");
	};

	public Lib oror1 = () -> {
		return new FuncDef("oror1", "px", "pe", "pe2", "pe3")
				.is("let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px) || mback1(px, pos) && pe3(px)");
	};

	public Lib oror3 = () -> {
		return new FuncDef("oror3", "px", "pe", "pe2", "pe2").is(
				"let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px) || mback3(px, pos, tree) && pe3(px)");
	};

	public Lib oror7 = () -> {
		return new FuncDef("oror7", "px", "pe", "pe2", "pe3").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && epe(px) || mback7(px, pos, tree, state) && ee2(px)");
	};

	public Lib ororor1 = () -> {
		return new FuncDef("ororor1", "px", "pe", "pe2", "pe3", "pe4").is(
				"let pos = px.pos; pe(px) || mback1(px, pos) && pe2(px) || mback1(px, pos) && pe3(px) || mback1(px, pos) && pe4(px)");
	};

	public Lib ororor3 = () -> {
		return new FuncDef("ororor3", "px", "pe", "pe2", "pe3", "pe4").is(
				"let pos = px.pos; let tree = px.tree; pe(px) || mback3(px, pos, tree) && pe2(px) || mback3(px, pos, tree) && pe3(px) || mback3(px, pos, tree) && pe4(px)");
	};

	public Lib ororor7 = () -> {
		return new FuncDef("ororor7", "px", "pe", "pe2", "pe3", "pe4").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) || mback7(px, pos, tree, state) && pe2(px) || mback7(px, pos, tree, state) && pe3(px) || mback7(px, pos, tree, state) && pe4(px)");
	};

	public Lib many1 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many1", "px", "pe")
					.is("let pos = px.pos; while(pe(px), {pos = px.pos}, mback1(px, pos))");
		}
		return new RecFuncDef("many1", "px", "pe").is("let pos = px.pos; pe(px) ? many1(px, pe) ? mback1(px, pos)");
	};

	public Lib many3 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many3", "px", "pe").is(
					"let pos = px.pos; let tree = px.tree; while(pe(px), {pos = px.pos; tree = px.tree}, mback3(px, pos, tree))");
		}
		return new RecFuncDef("many3", "px", "pe")
				.is("let pos = px.pos; let tree = px.tree; pe(px) ? many3(px, pe) ? mback3(px, pos, tree)");
	};

	public Lib many7 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many7", "px", "pe").is(
					"let pos = px.pos; let tree = px.tree; let state = px.state; while(pe(px), {pos = px.pos; tree = px.tree; state = px.state}, mback7(px, pos, tree, state))");
		}
		return new RecFuncDef("many7", "px", "pe").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) ? many7(px, pe) ? mback7(px, pos, tree, state)");
	};

	public Lib many9 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many9", "px", "pe")
					.is("let pos = px.pos; while(pe(px) && pos < px.pos, {pos = px.pos}, mback1(px, pos))");
		}
		return new RecFuncDef("many9", "px", "pe")
				.is("let pos = px.pos; pe(px) && pos < px.pos ? many9(px, pe) ? mback1(px, pos)");
	};

	public Lib many12 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many12", "px", "pe").is(
					"let pos = px.pos; let tree = px.tree; while(pe(px) && pos < px.pos, {pos = px.pos; tree = px.tree}, mback3(px, pos, tree))");
		}
		return new RecFuncDef("many12", "px", "pe").is(
				"let pos = px.pos; let tree = px.tree; pe(px) && pos < px.pos ? many12(px, pe) ? mback3(px, pos, tree)");
	};

	public Lib many16 = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("many16", "px", "pe").is(
					"let pos = px.pos; let tree = px.tree; let state = px.state; while(pe(px) && pos < px.pos, {pos = px.pos; tree = px.tree; state = px.state}, mback7(px, pos, tree, state))");
		}
		return new RecFuncDef("many16", "px", "pe").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; pe(px) && pos < px.pos ? many16(px, pe) ? mback7(px, pos, tree, state)");
	};

	public Lib manyany = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("manyany", "px").is("while(px.pos < px.length, {px.pos = px.pos + 1}, true)");
		}
		return new RecFuncDef("manyany", "px").is("px.pos < px.length && mnext1(px) && manyany(px, e)");
	};

	public Lib manychar = () -> {
		NezCC2.ENode index = this.p("px.inputs[px.pos]");
		if (this.isDefined("Obits32")) {
			index = this.apply("bits32", "bm", index);
		} else {
			index = this.p("$0[unsigned!($1)]", "bm", index);
		}
		if (this.isDefined("while")) {
			return new FuncDef("manychar", "px", "bm").is("while($0, {px.pos = px.pos + 1}, true)", index);
		}
		return new RecFuncDef("manychar", "px", "bm").is("$0 && mnext1(px) && manychar(px, bm)", index);
	};

	public Lib manystr = () -> {
		if (this.isDefined("while")) {
			return new FuncDef("manystr", "px", "stext", "slen")
					.is("while(matchmany(px.inputs, px.pos, stext, 0, slen), {px.pos = px.pos + slen}, true)");
		}
		return new FuncDef("manystr", "px", "stext", "slen")
				.is("matchmany(px.inputs, px.pos, stext, 0, slen) && mmov(px, slen) && manystr(px, stext, slen)");
	};

	public Lib and1 = () -> {
		return new FuncDef("and1", "px", "pe").is("let pos = px.pos; pe(px) && mback1(px, pos)");
	};

	public Lib not1 = () -> {
		return new FuncDef("not1", "px", "pe").is("let pos = px.pos; !pe(px) && mback1(px, pos)");
	};

	public Lib not3 = () -> {
		return new FuncDef("not3", "px", "pe")
				.is("let pos = px.pos; let tree = px.tree; !pe(px) && mback3(px, pos, tree)");
	};

	public Lib not7 = () -> {
		return new FuncDef("not7", "px", "pe").is(
				"let pos = px.pos; let tree = px.tree; let state = px.state; !pe(px) && mback7(px, pos, tree, state)");
	};

	public Lib minc = () -> {
		return new FuncDef("minc", "px").is("let pos = px.pos; px.pos = px.pos + 1; pos");
	};

	/* Tree Construction */

	public Lib mtree = () -> {
		return new FuncDef("mtree", "px", "tag", "spos", "epos")
				.is("px.tree = ctree(tag, px.inputs, spos, epos, px.tree); true");
	};

	public Lib mlink = () -> {
		return new FuncDef("mlink", "px", "tag", "child", "prev").is("px.tree = clink(tag, child, prev); true");
	};

	public Lib newtree = () -> {
		return new FuncDef("newtree", "px", "spos", "pe", "tag", "epos")
				.is("let pos = px.pos; px.tree = EmptyTree; pe(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Lib foldtree = () -> {
		return new FuncDef("foldtree", "px", "spos", "label", "pe", "tag", "epos").is(
				"let pos = px.pos; mlink(px, label, px.tree, EmptyTree) && pe(px) && mtree(px, tag, pos+spos, px.pos+epos)");
	};

	public Lib linktree = () -> {
		return new FuncDef("linktree", "px", "tag", "pe")
				.is("let tree = px.tree; pe(px) && mlink(px, tag, px.tree, tree)");
	};

	public Lib tagtree = () -> {
		return new FuncDef("tagtree", "px", "tag").is("mlink(px, tag, EmptyTree, px.tree)");
	};

	public Lib detree = () -> {
		return new FuncDef("detree", "px", "pe").is("let tree = px.tree; pe(px) && mback3(px, px.pos, tree)");
	};

	public Lib mconsume1 = () -> {
		return new FuncDef("mconsume1", "px", "memo").is("px.pos = memo.mpos; memo.matched");
	};

	public Lib mconsume3 = () -> {
		return new FuncDef("mconsume3", "px", "memo").is("px.pos = memo.mpos; px.tree = memo.mtree; memo.matched");
	};

	public Lib mconsume7 = () -> {
		return new FuncDef("mconsume7", "px", "memo")
				.is("px.pos = memo.mpos; px.tree = memo.mtree; px.state = memo.mstate; memo.matched");
	};

	public Lib mstore1 = () -> {
		return new FuncDef("mstore1", "px", "memo", "key", "pos", "matched")
				.is("memo.key = key; memo.mpos = pos; memo.matched = matched; matched");
	};

	public Lib mstore3 = () -> {
		return new FuncDef("mstore3", "px", "memo", "key", "pos", "matched")
				.is("memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.matched = matched; matched");
	};

	public Lib mstore7 = () -> {
		return new FuncDef("mstore7", "px", "memo", "key", "pos", "matched").is(
				"memo.key = key; memo.mpos = pos; memo.mtree = px.tree; memo.mstate = px.state; memo.matched = matched; matched");
	};

	public Lib getkey = () -> {
		return new FuncDef("getkey", "pos", "mp").asType("key").is("pos * memosize + mp");
	};

	public Lib getmemo = () -> {
		return new FuncDef("getmemo", "px", "key").asType("memo").is("px.memos[keyindex!(key % memolen)]");
	};

	public Lib memo1 = () -> {
		return new FuncDef("memo1", "px", "mp", "pe").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume1(px, memo) ? mstore1(px, memo, key, pos, pe(px))");
	};

	public Lib memo3 = () -> {
		return new FuncDef("memo3", "px", "mp", "pe").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume3(px, memo) ? mstore3(px, memo, key, pos, pe(px))");
	};

	public Lib memo7 = () -> {
		return new FuncDef("memo7", "px", "mp", "pe").is(
				"let pos = px.pos; let key = getkey(pos,mp); let memo = getmemo(px, key); (memo.key == key) ? mconsume7(px, memo) ? mstore7(px, memo, key, pos, pe(px))");
	};

	public Lib scope4 = () -> {
		return new FuncDef("scope4", "px", "pe").is("let state = px.state; pe(px) && mback4(px, state)");
	};

	public Lib mstate = () -> {
		return new FuncDef("mstate", "px", "ns", "pos").is("px.state = cstate(ns, pos, px.pos, px.state); true");
	};

	public Lib getstate = () -> {
		return new RecFuncDef("getstate", "state", "ns").asType("state")
				.is("(state == EmptyState || state.ns == ns) ? state ? getstate(state.sprev, ns)");
	};

	public Lib symbol4 = () -> {
		return new FuncDef("symbol4", "px", "ns", "pe").is("let pos = px.pos; pe(px) && mstate(px, ns, pos)");
	};

	public Lib smatch4 = () -> {
		return new FuncDef("smatch4", "px", "ns").is(
				"let state = getstate(px.state, ns); state != EmptyState && matchmany(px.inputs, px.pos, px.inputs, state.spos, state.slen) && mmov(px, state.slen)");
	};

	public Lib sexists4 = () -> {
		return new FuncDef("sexists4", "px", "ns").is("getstate(px.state, ns) != EmptyState");
	};

	public Lib rexists4 = () -> {
		return new RecFuncDef("rexists4", "px", "state", "stext", "slen").is(
				"state != EmptyState && (state.slen == slen && matchmany(stext, 0, px.inputs, state.spos, slen) || rexists4(px, getstate(state.sprev, state.ns), stext, slen))");
	};

	public Lib sequals4 = () -> {
		return new FuncDef("sequals", "px", "ns", "pe").is(
				"let pos = px.pos; let state = getstate(px.state, ns); state != EmptyState && pe(px) && state.slen == px.pos - pos && matchmany(px.inputs, pos, px.inputs, state.spos, px.pos - pos)");
	};

	public Lib smany = () -> {
		return new RecFuncDef("smany", "px", "state", "pos", "slen").is(
				"state != EmptyState && ((state.slen == slen && matchmany(px.inputs, pos, px.inputs, state.spos, slen)) || smany(px, getstate(state.sprev, state.ns), pos, slen))");
	};

	public Lib scontains4 = () -> {
		return new FuncDef("scontains", "px", "ns", "pe")
				.is("let pos = px.pos; pe(px) && smany(px, getstate(px.state, ns), pos, px.pos - pos)");
	};

	// curry function

	public Lib cadd = () -> {
		return new FuncDef("cadd", "pe", "pe2").is("\\px Let(pe(px), pe2(px') ?? Fail)");
	};

	public Lib cc1 = () -> {
		return new FuncDef("cc1", "ch").is("\\px px.inputs[px.pos] == ch ? Succ(pos, pos+1) ? Fail");
	};

	public Lib cc2 = () -> {
		return new FuncDef("cc2", "ch", "ch2")
				.is("\\px px.inputs[px.pos] == ch && px.inputs[px.pos+1] == ch2 ? Succ(pos, pos+2) ? Fail");
	};

	public Lib cor1 = () -> {
		return new FuncDef("cor1", "pe", "pe2").is("\\px Let(pos, pe(px), pxn ?? Back(pos, pos, pe2(px))");
	};

	public Lib cmany1 = () -> {
		return new FuncDef("cmany1", "pe").is("\\px Let(pos, pe(px), Rep(pos, pos) ?? Succ(pos, pos))");
	};

	public Lib cand1 = () -> {
		return new FuncDef("cand1", "pe").is("\\px Let(pos, pe(px), Succ(pos, pos) ?? Fail)");
	};

	public Lib cnot1 = () -> {
		return new FuncDef("cnot1", "pe").is("\\px Let(pos, pe(px), Fail ?? Succ(pos, pos))");
	};

	public Lib ctree1 = () -> {
		return new FuncDef("ctree1", "pe", "tag").is("\\px Let(pos, pe(px), Succ(tree, ctree) ?? Fail)");
	};

	public Lib clink1 = () -> {
		return new FuncDef("clink1", "pe", "tag").is("\\px Let(tree, pe(px), Succ(tree, clink) ?? Fail)");
	};

	public Lib ctag1 = () -> {
		return new FuncDef("ctag1", "tag").is("\\px Succ(tree, clink)");
	};

	private static String[] runtimeFuncs1 = { //
			"mnext1", "mmov", "neof", "succ", "fail", //
			"match2", "match3", "match4", "matchmany", //
			"mback1", "mback2", "mback3", "mback4", "mback7", //
			"maybe1", "maybe3", "maybe7", //
			"or1", "or3", "or7", //
			"oror1", "oror3", "oror7", //
			"ororor1", "ororor3", "ororor7", //
			"many1", "many3", "many7", "manyany", "manychar", "manystr", //
			"many9", "many12", "many16", //
			"and1", "not1", "not3", "not7", //
			"mtree", "mlink", "newtree", "foldtree", "linktree", "tagtree", //
			"getkey", "getmemo", //
			"mconsume1", "mconsume3", "mconsume7", //
			"mstore1", "mstore3", "mstore7", //
			"memo1", "memo3", "memo7", //
			"mstate", "symbol4", "scope4", "getstate", "sreset4", "sremove", //
			"smatch4", "sexists4", "rexists4", "sequals4", "smany", "scontains4",//
	};

	public ENode dispatch(ENode eJumpIndex, List<ENode> exprs) {
		return new Dispatch(eJumpIndex, exprs);
	}

	class Dispatch extends ENode {
		ENode eJumpIndex;
		ENode[] exprs;

		public Dispatch(ENode eJumpIndex, List<ENode> exprs) {
			this.eJumpIndex = eJumpIndex;
			this.exprs = exprs.toArray(new ENode[exprs.size()]);
		}

		@Override
		public ENode ret() {
			if (!this.isDefined("Ojumptable")) {
				for (int i = 0; i < this.exprs.length; i++) {
					this.exprs[i] = this.exprs[i].ret();
				}
				return this;
			}
			return super.ret();
		}

		@Override
		public ENode deret() {
			for (int i = 0; i < this.exprs.length; i++) {
				this.exprs[i] = this.exprs[i].deret();
			}
			return this;
		}

		@Override
		void emit(Writer w) {
			if (this.isDefined("Ojumptable")) {
				new Apply(new GetIndex(new FuncValue(this.exprs), this.eJumpIndex), new Var("px")).emit(w);
				return;
			}
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
				w.format(this.formatOf("end switch", "end", ""));
				return;
			} else {
				this.deret();
				ENode tail = this.exprs[0];
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

		class FuncValue extends ENode {
			ENode[] exprs;

			FuncValue(ENode[] data) {
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
					w.push(this.exprs[i]);
				}
				w.push(this.formatOf("end array", "]"));
				w0.format(this.formatOf("constname", "%s", NezCC2.this.constName(this.typeOf("jumptbl"), "jumptbl",
						this.exprs.length, w.toString(), null)));
			}
		}

	}

}
