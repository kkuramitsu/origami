package blue.origami.nezcc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.peg.expression.ByteSet;
import blue.origami.util.OStringUtils;

public class HaskellParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.defineSymbol("\t", "  ");
		this.defineSymbol("null", "None");
		this.defineSymbol("true", "True");
		this.defineSymbol("false", "False");
		this.defineSymbol("!", "not");
		this.defineSymbol("this", "self");
		this.defineSymbol("{", ":");
		this.defineSymbol("}", "");
		this.defineSymbol(";", "");
		this.defineSymbol("{[", "[");
		this.defineSymbol("]}", "]");
		this.defineSymbol("function", "def");
		this.defineSymbol("lambda", "\\");

		//
		this.defineSymbol("Tmatched", "Bool");
		this.defineSymbol("Tpx", "NezParserContext");
		this.defineSymbol("Tinputs", "Data.ByteString");
		this.defineSymbol("Tlength", "Int");
		this.defineSymbol("Tpos", "Int");
		this.defineSymbol("Ipos", "0");
		this.defineSymbol("TtreeLog", "TreeLog");
		this.defineSymbol("Ttree", "a");
		this.defineSymbol("Tstate", "State");
		this.defineSymbol("Tmemos", "MemoEntry");

		this.defineSymbol("TnewFunc", "TreeFunc");
		this.defineSymbol("TsetFunc", "TreeSetFunc");
		this.defineSymbol("Tf", "(NezParserContext -> Bool)");

		this.defineSymbol("px.newTree", "(newFunc px)");
		this.defineSymbol("px.setTree", "(setFunc px)");

		this.defineSymbol("Tch", "Word8");
		this.defineSymbol("Tcnt", "Int");
		this.defineSymbol("Tshift", "Int");
		this.defineSymbol("TindexMap", "Data.ByteString");
		this.defineSymbol("TbyteSet", "[Bool]");

		this.defineSymbol("Top", "Int");
		this.defineSymbol("Iop", "0");
		this.defineSymbol("Tlabel", "String");
		this.defineSymbol("Ttag", "String");
		this.defineSymbol("Tvalue", "String");
		this.defineSymbol("value.length", "(length value)");
		this.defineSymbol("Tdata", "void*");

		this.defineSymbol("Tm", "MemoEntry");
		this.defineSymbol("Tkey", "Int");
		this.defineSymbol("TmemoPoint", "Int");
		this.defineSymbol("Tresult", "Int");
		this.defineSymbol("Iresult", "0");

	}

	@Override
	protected void writeHeader() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeFooter() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("data %s = %s {", typeName, typeName));
		int c = 0;
		for (String f : fields) {
			String n = f.replace("?", "");
			if (c > 0) {
				sb.append(", ");
			}
			sb.append(String.format("%s :: %s", n, this.T(n)));
			c++;
		}
		sb.append(String.format("} deriving (Show)"));
		this.writeSection(sb.toString());
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declConst(String typeName, String constName, String literal) {
		this.writeSection(String.format("%s :: %s", constName, typeName));
		this.writeSection(String.format("%s = %s", constName, literal));
	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		// this.writeSection(String.format("%s %s", this.formatSignature(ret,
		// funcName, params), this.s(";")));
	}

	@Override
	protected void declFunc(String ret, String funcName, String[] params, Block<String> block) {
		StringBuilder sb = new StringBuilder();
		sb.append(funcName);
		sb.append(" :: ");
		for (String p : params) {
			String t = this.T(p);
			sb.append(t);
			sb.append(" -> ");
		}
		sb.append(ret);
		sb.append("\n");
		sb.append(funcName);
		for (String p : params) {
			sb.append(" ");
			sb.append(p);
		}
		sb.append(" = ");
		sb.append(block.block());
		this.writeSection(sb.toString());
	}

	@Override
	protected String emitParams(String... params) {
		return null;
	}

	@Override
	protected String formatParam(String type, String name) {
		return String.format("%s %s", type, name);
	}

	@Override
	protected String emitInit(String f) {
		if (f.endsWith("?")) {
			String n = f.replace("?", "");
			if (this.isDefined("I" + n)) {
				return this.s("I" + n);
			} else {
				return this.emitNull();
			}
		}
		return this.V(f);
	}

	@Override
	protected String beginBlock() {
		return "";
	}

	@Override
	protected String emitStmt(String block, String expr) {
		String stmt = this.Indent(expr.trim());
		if (!block.equals("")) {
			stmt = block + this.s(" ") + stmt;
		}
		return stmt;
	}

	@Override
	protected String endBlock(String block) {
		return block;
	}

	@Override
	protected String emitReturn(String expr) {
		return expr;
	}

	@Override
	protected String emitVarDecl(String block, boolean mutable, String name, String expr) {
		// String t = this.T(name);
		this.emitLine(block, "let %s = %s in", this.s(name), expr);
		return block;
	}

	@Override
	protected String emitAssign(String name, String expr) {
		return String.format("%s <- %s", this.s(name), expr);
	}

	@Override
	protected String emitAssign2(String left, String expr) {
		return String.format("%s <- %s", left, expr);
	}

	@Override
	protected String emitIfStmt(String block, String expr, boolean elseIf, Block<String> stmt) {
		block = this.emitStmt(block, String.format("%s(%s) %s", this.s("if"), expr, this.s("{")));
		this.incIndent();
		block = this.emitStmt(block, stmt.block());
		this.decIndent();
		block = this.emitStmt(block, this.s("}"));
		return block;
	}

	@Override
	protected String emitWhileStmt(String block, String expr, Block<String> stmt) {
		block = this.emitStmt(block, String.format("%s(%s) %s", this.s("while"), expr, this.s("{")));
		this.incIndent();
		block = this.emitStmt(block, stmt.block());
		this.decIndent();
		block = this.emitStmt(block, this.s("}"));
		return block;
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
	protected String emitGetter(String self, String name) {
		return String.format("(%s %s)", this.s(name), this.s(self));
	}

	@Override
	protected String emitSetter(String self, String name, String expr) {
		return String.format("%s%s%s = %s%s", this.s(self), this.s("."), this.s(name), expr, this.s(";"));
	}

	@Override
	protected String emitFunc(String func, List<String> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(this.s(func));
		int c = 0;
		for (String p : params) {
			sb.append(" ");
			sb.append(p);
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected String emitNot(String expr) {
		return String.format("(not %s)", expr);
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
		return String.format("(if %s then %s else %s)", expr, expr2, expr3);
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("case %s of {", index));
		for (int i = 0; i < cases.size(); i++) {
			sb.append(String.format("%s -> %s", i, cases.get(i)));
			sb.append(String.format("; ", i, cases.get(i)));
		}
		sb.append("}");
		return sb.toString();
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
		if (this.isDefined("bitis")) {
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
	protected String emitParserLambda(String match) {
		String lambda = String.format("(%spx -> %s)", this.s("lambda"), match);
		String p = "p" + this.varSuffix();
		return lambda.replace("px", p);
	}

}
