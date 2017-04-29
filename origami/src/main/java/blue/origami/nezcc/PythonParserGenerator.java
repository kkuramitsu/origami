package blue.origami.nezcc;

import java.io.IOException;

import blue.nez.ast.Symbol;
import blue.nez.peg.Expression;
import blue.nez.peg.expression.ByteSet;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class PythonParserGenerator extends JavaParserGenerator {

	@Override
	public void init(OOption options) {
		super.init(options);
		this.defineSymbol("&&", "and");
		this.defineSymbol("||", "or");
		this.defineSymbol("true", "True");
		this.defineSymbol("false", "False");
		this.defineSymbol("null", "None");
	}

	@Override
	protected void writeHeader() throws IOException {
		this.out.open("pynez.py");
		this.out.importResourceContent("/blue/origami/nezcc/pynez.py");
		this.out.open(this.getFileBaseName() + ".py");

		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-imports.java");
		// this.out.println("class %s {", this.getFileBaseName());
		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-runtime.java");
		// if (this.useLexicalOptimization()) {
		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-lexer.java");
		// }
		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-combinator.java");
		// if (this.isDebug()) {
		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-trace.java");
		// }
	}

	@Override
	protected void writeFooter() throws IOException {
		// this.out.importResourceContent("/blue/origami/nezcc/javaparser-main.java");
		// this.out.println("}");
	}

	@Override
	protected void defineConst(String typeName, String constName, String literal) {
		this.L("%s = %s;", constName, literal);
	}

	@Override
	protected void definePrototype(String funcName) {
	}

	@Override
	protected void beginDefine(String funcName, Expression e) {
		this.L("def %s(px):", funcName);
		this.L("# " + e);
		this.incIndent();
	}

	@Override
	protected void endDefine(String funcName, String pe) {
		if (pe.startsWith(" ") || pe.startsWith("\t")) {
			this.L(pe.trim());
		} else {
			this.L(this.result(pe).trim());
		}
		this.decIndent();
		this.L("");
	}

	@Override
	protected String result(String pe) {
		if (this.isDebug()) {
			return this.Line("return B(\"%s\", px) && E(\"%s\", px, %s)", this.getCurrentFuncName(),
					this.getCurrentFuncName(), pe);
		}
		return this.Line("return %s", pe);
	}

	@Override
	protected String matchAny() {
		return "(px.read() != 0)";
	}

	@Override
	protected String matchByte(int uchar) {
		// return String.format("(px.read() == %s)", uchar);
		return this.Expr("px.is(%d)", (byte) uchar);
	}

	@Override
	protected String matchByteSet(ByteSet byteSet) {
		String constName = this.getConstName("boolean[256]", this.toLiteral(byteSet));
		return String.format("%s[px.read()]", constName);
	}

	@Override
	protected String matchIf(String pe, String pe2, String pe3) {
		return String.format("(%s) ? (%s) : (%s)", pe, pe2, pe3);
	}

	@Override
	protected String matchLoop(String pe, String pe2, String pe3) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.Line("while %s:", pe));
		this.incIndent();
		sb.append(this.Line(pe2));
		this.decIndent();
		sb.append(this.result(pe3));
		return sb.toString();
	}

	@Override
	protected String matchCase(String pe, String[] exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(this.Expr("lambda px: %s", this.s("false")));
		for (int i = 0; i < exprs.length; i++) {
			sb.append(this.Expr(", lambda px: %s;", exprs[i]));
		}
		sb.append("]");
		String constName = this.getConstName("func[]", sb.toString());
		return this.Expr("%s[%s](px)", constName, pe);
	}

	@Override
	protected String matchNonTerminal(String func) {
		return String.format("%s(px)", func);
	}

	@Override
	protected String initPosVar(int varid) {
		return this.Line("%s = px.pos", this.v("pos", varid));
	}

	@Override
	protected String initTreeVar(int varid) {
		return this.Line("%s = px.tree", this.v("tree", varid));
	}

	@Override
	protected String initLogVar(int varid) {
		return this.Line("%s = px.treeLog", this.v("treeLog", varid));
	}

	@Override
	protected String initStateVar(int varid) {
		return this.Line("%s = px.state", this.v("state", varid));
	}

	@Override
	protected String backPos(int varid) {
		return String.format("px.back(%s)", this.v("pos", varid));
	}

	@Override
	protected String backTree(int varid) {
		return String.format("px.backT(%s)", this.v("tree", varid));
	}

	@Override
	protected String backTreeLog(int varid) {
		return String.format("px.backL(%s)", this.v("treeLog", varid));
	}

	@Override
	protected String backState(int varid) {
		return String.format("px.backS(%s)", this.v("state", varid));
	}

	@Override
	protected String updatePos(int varid) {
		return this.Expr("%s = px.pos;", this.v("pos", varid));
	}

	@Override
	protected String updateTree(int varid) {
		return this.Expr("%s = px.tree;", this.v("tree", varid));
	}

	@Override
	protected String updateTreeLog(int varid) {
		return this.Expr("%s = px.treeLog;", this.v("treeLog", varid));
	}

	@Override
	protected String updateState(int varid) {
		return this.Expr("%s = px.state;", this.v("state", varid));
	}

	@Override
	protected String initCountVar(int varid) {
		return this.Line("%s = 0;", this.v("cnt", varid));
	}

	@Override
	protected String updateCountVar(int varid) {
		return this.Line("%s += 1", this.v("cnt", varid));
	}

	@Override
	protected String checkCountVar(int varid) {
		return String.format("(%s > 0)", this.v("cnt", varid));
	}

	@Override
	protected String fetchJumpIndex(byte[] indexMap) {
		String constName = this.getConstName("int[256]", this.toLiteral(indexMap));
		return String.format("%s[px.getbyte()]", constName);
	}

	@Override
	protected String toLiteral(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s, '"');
			return sb.toString();
		}
		return this.s("null");
	}

	@Override
	protected String toLiteral(Symbol s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
			return sb.toString();
		}
		return this.s("null");
	}

	@Override
	protected String toLiteral(byte[] indexMap) {
		boolean overflow = false;
		int last = 0;
		for (int i = 0; i < 256; i++) {
			if (indexMap[i] != 0) {
				last = i;
			}
			if (indexMap[i] > 35) {
				overflow = true;
			}
		}
		StringBuilder sb = new StringBuilder();
		if (overflow) {
			sb.append("{");
			for (byte index : indexMap) {
				sb.append(index & 0xff);
				sb.append(", ");
			}
			sb.append("}");
		} else {
			sb.append("indexMap(\"");
			for (int i = 0; i <= last; i++) {
				if (indexMap[i] > 10) {
					sb.append((char) ('A' + (indexMap[i] - 10)));
				} else {
					sb.append((char) ('0' + indexMap[i]));
				}
			}
			sb.append("\")");
		}
		return sb.toString();
	}

	@Override
	protected String toLiteral(ByteSet bs) {
		int last = 0;
		for (int i = 0; i < 256; i++) {
			if (bs.is(i)) {
				last = i;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("boolMap(\"");
		for (int i = 0; i <= last; i++) {
			sb.append(bs.is(i) ? "1" : "0");
		}
		sb.append("\")");
		return sb.toString();
	}

	@Override
	protected boolean supportedLambdaFunction() {
		return true;
	}

	@Override
	protected String refFunc(String funcName) {
		return String.format("%s", this.getFileBaseName(), funcName);
	}

	@Override
	protected String defineLambda(String match) {
		String lambda = String.format("lambda px : %s", match);
		String p = "p" + (int) (Math.random() * 100000);
		return lambda.replace("px", p);
	}

	@Override
	protected String getRepetitionCombinator() {
		return "pMany";
	}

	@Override
	protected String getOptionCombinator() {
		return "pOption";
	}

	@Override
	protected String getAndCombinator() {
		return "pAnd";
	}

	@Override
	protected String getNotCombinator() {
		return "pNot";
	}

	@Override
	protected String getLinkCombinator() {
		return "pLink";
	}

	@Override
	protected String getMemoCombinator() {
		return "pMemo";
	}

	@Override
	protected boolean useMultiBytes() {
		return false;
	}

}
