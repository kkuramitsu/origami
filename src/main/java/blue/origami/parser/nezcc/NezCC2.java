package blue.origami.parser.nezcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;

import blue.origami.common.OConsole;

public class NezCC2 {
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
					int loc = line.indexOf('=');
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
			w.format(this.formatOf("funcbody", "\n\t%s"), this.body);
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
			w.push(this.value);
		}

	}

	class CharValue extends Expression {
		int uchar;

		public CharValue(char charValue) {
			// TODO Auto-generated constructor stub
		}

		@Override
		void emit(Writer w) {
			// TODO Auto-generated method stub

		}

	}

	Expression op(Expression left, String op, Expression right) {
		return new Infix(left, op, right);
	}

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

	Expression unary(String expr, Expression... args) {
		expr = expr.trim();
		if (expr.startsWith("$")) {
			return args[Integer.parseInt(expr.substring(1))];
		}
		int pos = expr.indexOf('.');
		if (pos > 0) {
			return new Getter(expr.substring(0, pos), expr.substring(pos + 1));
		}
		if (expr.endsWith(")")) {
			pos = expr.indexOf('(');
			if (pos > 0) {
				String[] tokens = expr.substring(pos + 1, expr.length() - 1).split(",");
				Expression[] a = Arrays.stream(tokens).map(s -> this.p_(s, args)).toArray(Expression[]::new);
				return new Apply(expr.substring(0, pos), a);
			}
			return this.p_(expr.substring(1, expr.length() - 1), args);
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

	public Expression neof = new DefFunc("neof", "px").is(this.p("px.pos < px.length"));

}
