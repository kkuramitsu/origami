package blue.origami.nezcc;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.peg.expression.ByteSet;
import blue.origami.util.OStringUtils;

public abstract class ParserSourceGenerator extends AbstractParserGenerator<String> {

	protected boolean isDynamicTyping() {
		return this.T("pos") == null;
	}

	protected String emitLine(String block, String format, Object... args) {
		if (args.length == 0) {
			return this.emitStmt(block, format);
		}
		return this.emitStmt(block, String.format(format, args));
	}

	@Override
	protected void declConst(String typeName, String constName, String literal) {
		String decl = "";
		if (this.isDefinedSymbol("const")) {
			decl = this.s("const") + " ";
		}
		if (typeName == null || this.isDynamicTyping()) {
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
		if (ret == null || this.isDynamicTyping()) {
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
	protected void declFunc(String ret, String funcName, String[] params, Block<String> block) {
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
			if (t == null || this.isDynamicTyping()) {
				sb.append(p);
			} else {
				sb.append(this.formatParam(t, p));
			}
		}
		return sb.toString();
	}

	protected String formatParam(String type, String name) {
		return String.format("%s %s", type, name);
	}

	protected String emitInit(String f) {
		if (f.endsWith("?")) {
			String n = f.replace("?", "");
			if (this.isDefinedSymbol("I" + n)) {
				return this.s("I" + n);
			} else {
				return this.emitNull();
			}
		}
		return this.V(f);
	}

	protected String formatFuncResult(String expr) {
		if (expr.startsWith(" ") || expr.startsWith("\t")) {
			return expr;
		}
		if (this.isDebug()) {
			// return this.Indent("return B(\"%s\", px) && E(\"%s\", px, %s);",
			// this.getCurrentFuncName(),
			// this.getCurrentFuncName(), pe);
		}
		return this.Indent(this.emitReturn(expr));
	}

	@Override
	protected String beginBlock() {
		return "";
	}

	@Override
	protected String emitStmt(String block, String expr) {
		String stmt = this.Indent(expr.trim());
		if (stmt.endsWith(")")) {
			stmt = stmt + this.s(";");
		}
		if (!block.equals("")) {
			stmt = block + this.s("\n") + stmt;
		}
		return stmt;
	}

	@Override
	protected String endBlock(String block) {
		return block;
	}

	@Override
	protected String emitReturn(String expr) {
		return String.format("%s %s%s", this.s("return"), expr, this.s(";"));
	}

	@Override
	protected String emitVarDecl(boolean mutable, String name, String expr) {
		String t = this.T(name);
		if (t == null) {
			return String.format("%s = %s%s", this.s(name), expr, this.s(";"));
		}
		return String.format("%s %s = %s%s", t, this.s(name), expr, this.s(";"));
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
		String block = this.beginBlock();
		block = this.emitLine(block, "%s(%s) %s", elseIf ? this.s("else if") : this.s("if"), expr, this.s("{"));
		this.incIndent();
		block = this.emitStmt(block, stmt.block());
		this.decIndent();
		block = this.emitStmt(block, this.s("}"));
		return this.endBlock(block);
	}

	@Override
	protected String emitWhileStmt(String expr, Block<String> stmt) {
		String block = this.beginBlock();
		block = this.emitLine(block, "%s(%s) %s", this.s("while"), expr, this.s("{"));
		this.incIndent();
		block = this.emitStmt(block, stmt.block());
		this.decIndent();
		block = this.emitStmt(block, this.s("}"));
		return this.endBlock(block);
	}

	@Override
	protected String emitOp(String expr, String op, String expr2) {
		return String.format("%s %s %s", expr, this.s(op), expr2);
	}

	@Override
	protected String emitCast(String var, String expr) {
		String t = this.T(var);
		if (t == null) {
			return expr;
		}
		return String.format("(%s)%s", this.T(var), expr);
	}

	@Override
	protected String emitNull() {
		return this.s("null");
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
		return "" + (byte) uchar;
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
	protected String emitApply(String func) {
		return String.format("%s(%s)", func, this.s("px"));
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
		return String.format("%s %s %s", expr, this.s("&&"), expr2);
	}

	@Override
	protected String emitOr(String expr, String expr2) {
		return String.format("(%s) %s (%s)", expr, this.s("||"), expr2);
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return String.format("(%s) ? (%s) : (%s)", expr, expr2, expr3);
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		if (this.useFuncMap()) {
			String funcMap = this.vFuncMap(cases);
			return this.emitArrayIndex(funcMap, index) + "(px)";
		} else {
			String block = this.beginBlock();
			block = this.emitLine(block, "%s(%s) %s", this.s("switch"), index, this.s("{"));
			this.incIndent();
			for (int i = 1; i < cases.size(); i++) {
				block = this.emitLine(block, "case %s: return %s;", i, cases.get(i));
			}
			block = this.emitLine(block, "default: return %s;", cases.get(0));
			this.decIndent();
			block = this.emitStmt(block, this.s("}"));
			return this.endBlock(block);
		}
	}

	protected String vFuncMap(List<String> cases) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.s("{["));
		int c = 0;
		for (String code : cases) {
			if (c > 0) {
				sb.append(this.s(","));
			}
			if (code.equals(this.s("false"))) {
				sb.append(this.funcFail(this));
			} else if (code.equals(this.s("true"))) {
				sb.append(this.funcSucc(this));
			} else {
				// if(code.endsWith("(px)") && code.indexOf(" ") == -1) {
				//
				// }
				// else {
				sb.append(this.emitParserLambda(code));
				// }
			}
			c++;
		}
		sb.append(this.s("]}"));
		return this.getConstName(this.T("funcMap"), sb.toString());
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
		return this.getConstName(this.T("indexMap"), sb.toString());
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
			return this.getConstName(this.T("inputs"), sb.toString());
		}
		return this.emitNull();
	}

	@Override
	protected String vTag(Symbol s) {
		if (s != null) {
			return this.vString(Objects.toString(s));
		}
		return this.emitNull();
	}

	@Override
	protected String vLabel(Symbol s) {
		if (s != null) {
			return this.vString(Objects.toString(s));
		}
		return this.emitNull();
	}

	@Override
	protected String vThunk(Object s) {
		return this.s("null");
	}

	@Override
	protected String vByteSet(ByteSet bs) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.s("{["));
		if (this.isDefinedSymbol("bitis")) {
			for (int i = 0; i < 8; i++) {
				if (i > 0) {
					sb.append(this.s(","));
				}
				sb.append(bs.bits()[i]);
			}
		} else {
			for (int i = 0; i < 256; i++) {
				if (i > 0) {
					sb.append(this.s(","));
				}
				sb.append(bs.is(i) ? this.s("true") : this.s("false"));
			}
		}
		sb.append(this.s("]}"));
		return this.getConstName(this.T("byteSet"), sb.toString());
	}

	@Override
	protected String emitFuncRef(String funcName) {
		return funcName;
		// return String.format("%s::%s", this.getFileBaseName(), funcName);
	}

	@Override
	protected String emitParserLambda(String match) {
		String lambda = String.format("(px) %s %s", this.s("lambda"), match);
		String p = "p" + this.varSuffix();
		return lambda.replace("px", p);
	}

	@Override
	public String V(String name) {
		return this.s(name);
	}

}
