package blue.origami.nezcc;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.util.OStringUtils;

public abstract class ParserSourceGenerator extends ParserGenerator<StringBuilder, String> {

	protected void emitLine(StringBuilder block, String format, Object... args) {
		if (args.length == 0) {
			this.emitStmt(block, format);
		} else {
			this.emitStmt(block, String.format(format, args));
		}
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {

	}

	@Override
	protected void declConst(String typeName, String constName, int arraySize, String literal) {

		String decl = "";
		if (this.isDefined("const")) {
			decl = this.s("const") + " ";
		}
		if (typeName == null || this.useDynamicTyping()) {
			this.writeSection(String.format("%s%s = %s%s", decl, constName, literal, this.s(";")));
		} else {
			constName = this.arrayName(typeName, constName);
			typeName = this.arrayType(typeName, constName);
			this.writeSection(
					String.format("%s%s = %s%s", decl, this.formatParam(typeName, constName), literal, this.s(";")));
		}
	}

	protected String arrayName(String typeName, String constName) {
		return constName;
	}

	protected String arrayType(String typeName, String constName) {
		int loc = typeName.indexOf('[');
		if (loc >= 0) {
			return typeName.substring(0, loc) + "[]";
		}
		return typeName;
	}

	protected String formatSignature(String ret, String funcName, String[] params) {
		if (ret == null || this.useDynamicTyping()) {
			return String.format("%s %s(%s)", this.s("function"), funcName, this.emitParams(params));
		} else {
			return String.format("%s %s %s(%s)", this.s("function"), ret, funcName, this.emitParams(params));
		}
	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		// this.writeSection(String.format("%s %s", this.formatSignature(ret,
		// funcName, params), this.s(";")));
	}

	@Override
	protected void declFunc(int acc, String ret, String funcName, String[] params, Block<String> block) {
		this.writeSection(String.format("%s %s", this.formatSignature(ret, funcName, params), this.s("{")));
		this.incIndent();
		this.writeSection(this.formatFuncResult(block.block()));
		this.decIndent();
		this.writeSection(this.s("}"));
	}

	@Override
	protected String emitParams(String... params) {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (String p : params) {
			if (p.endsWith("?")) {
				continue;
			}
			if (c > 0) {
				sb.append(this.s(","));
			}
			c++;
			String t = this.T(p);
			if (t == null || this.useDynamicTyping()) {
				sb.append(p);
			} else {
				sb.append(this.formatParam(t, p));
			}
			String v = this.getSymbol("P" + p);
			if (v != null) {
				sb.append(v);
			}
		}
		return sb.toString();
	}

	protected String formatParam(String type, String name) {
		return String.format("%s %s", type, name);
	}

	protected void emitInits(StringBuilder block, String... fields) {
		for (String f : fields) {
			String n = f.replace("?", "");
			String v = this.emitInit(f);
			this.emitLine(block, "%s.%s = %s%s", this.s("this"), n, v, this.s(";"));
		}
	}

	protected String emitInit(String f) {
		if (f.endsWith("?")) {
			String n = f.replace("?", "");
			if (this.isDefined("I" + n)) {
				return this.s("I" + n);
			} else {
				return this.emitNull(null);
			}
		}
		return this.V(f);
	}

	protected String formatFuncResult(String expr) {
		if (expr.startsWith(" ") || expr.startsWith("\t")) {
			return expr;
		}
		if (this.isDebug()) {
			String funcName = this.vString(this.getCurrentFuncName());
			expr = this.emitAnd(this.emitFunc("B", funcName, this.V("px")),
					this.emitFunc("E", funcName, this.V("px"), expr));
		}
		return this.Indent(this.emitReturn(expr));
	}

	@Override
	protected StringBuilder beginBlock() {
		return new StringBuilder();
	}

	@Override
	protected void emitStmt(StringBuilder block, String expr) {
		String stmt = this.Indent(expr.trim());
		if (stmt.endsWith(")")) {
			stmt = stmt + this.s(";");
		}
		if (block.length() != 0) {
			block.append(this.s("\n"));
		}
		block.append(stmt);
	}

	@Override
	protected String endBlock(StringBuilder block) {
		return block.toString();
	}

	@Override
	protected String emitReturn(String expr) {
		return String.format("%s %s%s", this.s("return"), expr, this.s(";"));
	}

	@Override
	protected String emitVarDecl(boolean mutable, String name, String expr, String expr2) {
		String t = this.T(name);
		if (t == null) {
			return String.format("%s = %s%s", this.s(name), expr, this.s(";"));
		}
		return String.format("%s %s = %s%s", t, this.s(name), expr, this.s(";"));
	}

	@Override
	protected String emitUnsigned(String expr) {
		if (!this.isDefined("unsigned")) {
			return expr;
		}
		return this.format("unsigned", expr);
	}

	@Override
	protected String emitAssign(String name, String expr) {
		return String.format("%s = %s%s", this.s(name), expr, this.s(";"));
	}

	@Override
	protected String emitAssign2(String left, String expr) {
		return String.format("%s = %s%s", left, expr, this.s(";"));
	}

	@Override
	protected String emitIfStmt(String expr, boolean elseIf, Block<String> stmt) {
		StringBuilder sb = this.beginBlock();
		sb.append(String.format("%s(%s) %s", elseIf ? this.s("else if") : this.s("if"), expr, this.s("{")));
		this.incIndent();
		this.emitStmt(sb, stmt.block());
		this.decIndent();
		this.emitStmt(sb, this.s("}"));
		return this.endBlock(sb);
	}

	@Override
	protected String emitWhileStmt(String expr, Block<String> stmt) {
		StringBuilder sb = this.beginBlock();
		this.emitLine(sb, "%s(%s) %s", this.s("while"), expr, this.s("{"));
		this.incIndent();
		this.emitStmt(sb, stmt.block());
		this.decIndent();
		this.emitStmt(sb, this.s("}"));
		return this.endBlock(sb);
	}

	@Override
	protected String emitOp(String expr, String op, String expr2) {
		return String.format("%s %s %s", expr, this.s(op), expr2);
	}

	@Override
	protected String emitCast(String var, String expr) {
		String conv = this.getSymbol("C" + var);
		if (conv != null) {
			return String.format(conv, expr);
		}
		String t = this.T(var);
		if (t == null) {
			return expr;
		}
		return String.format("(%s)(%s)", this.T(var), expr);
	}

	@Override
	protected String emitNull(String name) {
		return this.s("null");
	}

	@Override
	protected String emitArrayLength(String a) {
		return this.emitFunc("len", a);
	}

	@Override
	protected String emitArrayIndex(String a, String index) {
		return String.format("%s[%s]", a, index);
	}

	@Override
	protected String emitNewArray(String type, String index) {
		return String.format("new %s[%s]", type, index);
	}

	@Override
	protected String emitChar(int uchar) {
		return "" + (uchar & 0xff);
	}

	@Override
	protected String emitGetter(String self, String name) {
		return String.format("%s%s%s", this.s(self), this.s("."), this.s(name));
	}

	@Override
	protected String emitSetter(String self, String name, String expr) {
		return String.format("%s%s%s = %s%s", this.s(self), this.s("."), this.s(name), expr, this.s(";"));
	}

	@Override
	protected String emitFunc(String func, List<String> params) {
		String fmt = this.getSymbol(func);
		if (fmt != null && fmt.indexOf("%s") >= 0) {
			return String.format(fmt, params.toArray(new Object[params.size()]));
		}
		StringBuilder sb = new StringBuilder();
		sb.append(this.s(func));
		sb.append("(");
		int c = 0;
		for (String p : params) {
			if (c > 0) {
				sb.append(this.s(","));
			}
			c++;
			sb.append(p);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String emitApply(String func, List<String> params) {
		return this.emitFunc(func, params);
	}

	@Override
	protected String emitNot(String expr) {
		return String.format("%s(%s)", this.s("!"), expr);
	}

	@Override
	protected String emitSucc() {
		return this.s("true");
	}

	@Override
	protected String emitFail() {
		return this.s("false");
	}

	@Override
	protected String emitAnd(String expr, String expr2) {
		String t = this.s("true");
		String f = this.s("false");
		if (expr.equals(f) || expr2.equals(f)) {
			return f;
		}
		if (expr.equals(t)) {
			return expr2;
		}
		if (expr2.equals(f)) {
			return expr;
		}
		return String.format("%s %s %s", expr, this.s("&&"), expr2);
	}

	@Override
	protected String emitOr(String expr, String expr2) {
		return String.format("(%s) %s (%s)", expr, this.s("||"), expr2);
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return String.format("%s ? %s : %s", expr, expr2, expr3);
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		if (this.isDefined("switch")) {
			StringBuilder block = this.beginBlock();
			this.emitLine(block, "%s(%s) %s", this.s("switch"), index, this.s("{"));
			this.incIndent();
			for (int i = 0; i < cases.size(); i++) {
				this.emitLine(block, "case %s: return %s;", i, cases.get(i));
			}
			// this.emitLine(block, "default: return %s;", cases.get(0));
			this.decIndent();
			this.emitStmt(block, this.s("}"));
			this.Return(block, this.emitFail());
			return this.endBlock(block);
		} else {
			StringBuilder block = this.beginBlock();
			this.emitVarDecl(block, false, "result", index);
			boolean elseIf = false;
			for (int i = cases.size() - 1; i > 0; i--) {
				final int n = i;
				this.emitIfStmt(block, this.emitOp(this.V("result"), "==", this.vInt(i)), elseIf, () -> {
					return this.emitReturn(cases.get(n));
				});
				elseIf = true;
			}
			this.emitStmt(block, this.emitReturn(cases.get(0)));
			return this.endBlock(block);
		}
	}

	private boolean isNotIncludeMemo(List<String> cases) {
		if (cases.size() == 3) {
			String expr = cases.get(2);
			return expr.indexOf("memoPoint") == -1;
		}
		return true;
	}

	protected String vFuncMap(List<String> cases) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.s("{["));
		int c = 0;
		for (String code : cases) {
			if (c > 0) {
				sb.append(this.s(","));
			}
			// if (code.equals(this.s("false"))) {
			// sb.append(this.funcFail(this));
			// } else if (code.equals(this.s("true"))) {
			// sb.append(this.funcSucc(this));
			// } else {
			sb.append(this.emitParserLambda(code));
			// }
			c++;
		}
		sb.append(this.s("]}"));
		return this.getConstName(this.T("f"), cases.size(), sb.toString());
	}

	@Override
	protected String emitAsm(String expr) {
		return this.s(expr);
	}

	@Override
	protected String vInt(int value) {
		return String.valueOf(value);
	}

	@Override
	protected String vString(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s, '"');
			return sb.toString();
		}
		return this.s("null");
	}

	@Override
	protected String vIndexMap(byte[] indexMap) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.s("{["));
		for (byte index : indexMap) {
			sb.append(index & 0xff);
			sb.append(", ");
		}
		sb.append(this.s("]}"));
		return this.getConstName(this.s("UInt8"), indexMap.length, sb.toString());
	}

	@Override
	protected String vValue(String s) {
		if (s != null) {
			byte[] buf = s.getBytes(Charset.forName("UTF-8"));
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("{["));
			for (byte b : buf) {
				sb.append(b);
				sb.append(",");
			}
			sb.append(this.s("]}"));
			return this.getConstName(this.s("Byte"), buf.length, sb.toString());
		}
		return this.emitNull(null);
	}

	@Override
	protected String vTag(Symbol s) {
		if (s != null) {
			return this.vString(Objects.toString(s));
		}
		return this.emitNull(null);
	}

	@Override
	protected String vLabel(Symbol s) {
		if (s != null) {
			return this.vString(Objects.toString(s));
		}
		return this.emitNull(null);
	}

	@Override
	protected String vThunk(Object s) {
		return this.s("null");
	}

	@Override
	protected String vByteSet(ByteSet bs) {
		if (this.isDefined("bitis")) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("{["));
			for (int i = 0; i < 8; i++) {
				if (i > 0) {
					sb.append(this.s(","));
				}
				sb.append(bs.bits()[i]);
			}
			sb.append(this.s("]}"));
			return this.getConstName(this.s("Int32"), 8, sb.toString());
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("{["));
			for (int i = 0; i < 256; i++) {
				if (i > 0) {
					sb.append(this.s(","));
				}
				sb.append(bs.is(i) ? this.s("true") : this.s("false"));
			}
			sb.append(this.s("]}"));
			return this.getConstName(this.s("Bool"), 256, sb.toString());
		}
	}

	@Override
	protected String emitFuncRef(String funcName) {
		return funcName;
		// return String.format("%s::%s", this.getFileBaseName(), funcName);
	}

	@Override
	protected String emitParserLambda(String match) {
		String lambda = String.format(this.s("lambda"), match);
		String p = "p" + this.varSuffix();
		return lambda.replace("px", p);
	}

	@Override
	public String V(String name) {
		return this.s(name);
	}

	@Override
	public String Const(String name) {
		return name;
	}

}
