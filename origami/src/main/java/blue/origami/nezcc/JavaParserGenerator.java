package blue.origami.nezcc;

import java.io.IOException;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.peg.Expression;
import blue.nez.peg.expression.ByteSet;
import blue.origami.util.OStringUtils;

public class JavaParserGenerator extends ParserGenerator {
	@Override
	protected void writeHeader() throws IOException {
		this.out.open(this.getFileBaseName() + ".java");
		this.out.importResourceContent("/blue/origami/nezcc/javaparser-imports.java");
		this.out.println("class %s {", this.getFileBaseName());
		this.out.importResourceContent("/blue/origami/nezcc/javaparser-runtime.java");
		if (this.useLexicalOptimization()) {
			this.out.importResourceContent("/blue/origami/nezcc/javaparser-lexer.java");
		}
		this.out.importResourceContent("/blue/origami/nezcc/javaparser-combinator.java");
		if (this.isStateful()) {
			this.out.importResourceContent("/blue/origami/nezcc/javaparser-state.java");
		}
		this.defineConst("boolean", "STATE", "" + this.isStateful());
		if (this.isDebug()) {
			this.out.importResourceContent("/blue/origami/nezcc/javaparser-trace.java");
		}
	}

	@Override
	protected void writeFooter() throws IOException {
		this.out.importResourceContent("/blue/origami/nezcc/javaparser-main.java");
		this.out.println("}");
		this.out.showResourceContent("/blue/origami/nezcc/javaparser-man.txt", "$cmd$", this.getFileBaseName());
	}

	@Override
	protected void defineConst(String typeName, String constName, String literal) {
		int loc = typeName.indexOf("[");
		if (loc > 0) {
			typeName = typeName.substring(0, loc) + "[]";
		}
		this.L("static final %s %s = %s;", typeName, constName, literal);
	}

	@Override
	protected void definePrototype(String funcName) {
	}

	@Override
	protected void beginDefine(String funcName, Expression e) {
		this.L("static final <T> boolean %s(%s px) {", funcName, this.s("NezParserContext<T>"));
		this.L("// " + e);
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
		this.L("}");
	}

	@Override
	protected String result(String pe) {
		if (this.isDebug()) {
			return this.Line("return B(\"%s\", px) && E(\"%s\", px, %s);", this.getCurrentFuncName(),
					this.getCurrentFuncName(), pe);
		}
		return this.Line("return %s;", pe);
	}

	@Override
	protected String matchSucc() {
		return this.s("true");
	}

	@Override
	protected String matchFail() {
		return this.s("false");
	}

	@Override
	protected String move(int shift) {
		return this.Expr("px.move(%s)", shift);
	}

	@Override
	protected String matchAny() {
		return "(px.read() != 0)";
	}

	@Override
	protected String matchByte(int uchar) {
		// return String.format("(px.read() == %s)", uchar);
		return this.Expr("px.is(%s)", (byte) uchar);
	}

	@Override
	protected String matchByteSet(ByteSet bs) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.matchByte(uchar);
		} else {
			String constName = this.getConstName("boolean[256]", this.toLiteral(bs));
			return String.format("%s[px.read()]", constName);
		}
	}

	@Override
	protected String matchPair(String pe, String pe2) {
		return String.format("%s %s %s", pe, this.s("&&"), pe2);
	}

	@Override
	protected String matchChoice(String pe, String pe2) {
		return String.format("(%s) %s (%s)", pe, this.s("||"), pe2);
	}

	@Override
	protected String matchIf(String pe, String pe2, String pe3) {
		return String.format("(%s) ? (%s) : (%s)", pe, pe2, pe3);
	}

	@Override
	protected String matchLoop(String pe, String pe2, String pe3) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.Line("while(%s) {", pe));
		this.incIndent();
		sb.append(this.Line(pe2));
		this.decIndent();
		sb.append(this.Line("}"));
		sb.append(this.result(pe3));
		return sb.toString();
	}

	@Override
	protected String checkNonEmpty(int varid) {
		return String.format("(px.pos > %s)", this.v("pos", varid));
	}

	@Override
	protected String matchCase(String pe, String[] exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.Line("switch(%s) {", pe));
		this.incIndent();
		for (int i = 0; i < exprs.length; i++) {
			sb.append(this.Line("case %s: return %s;", i + 1, exprs[i]));
		}
		sb.append(this.Line("default: return %s;", this.s("false")));
		this.decIndent();
		sb.append(this.Line("}"));
		return sb.toString();
	}

	@Override
	protected String matchNonTerminal(String func) {
		return String.format("%s(px)", func);
	}

	@Override
	protected String initPosVar(int varid) {
		return this.Line("%s %s = px.pos;", this.s("int"), this.v("pos", varid));
	}

	@Override
	protected String initTreeVar(int varid) {
		return this.Line("%s %s = px.tree;", this.s("T"), this.v("tree", varid));
	}

	@Override
	protected String initLogVar(int varid) {
		return this.Line("%s %s = px.treeLog;", this.s("TreeLog<T>"), this.v("treeLog", varid));
	}

	@Override
	protected String initStateVar(int varid) {
		return this.Line("%s %s = px.state;", this.s("Object"), this.v("state", varid));
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
		return this.Line("int %s = 0;", this.v("cnt", varid));
	}

	@Override
	protected String updateCountVar(int varid) {
		return this.Expr("%s++;", this.v("cnt", varid));
	}

	@Override
	protected String checkCountVar(int varid) {
		return String.format("(%s > 0)", this.v("cnt", varid));
	}

	/* Tree Operation */
	@Override
	protected String tagTree(Symbol tag) {
		return String.format("px.tagTree(%s)", this.toLiteral(tag));
	}

	@Override
	protected String valueTree(String value) {
		return String.format("px.valueTree(%s)", this.toLiteral(value));
	}

	@Override
	protected String linkTree(int varid, Symbol label) {
		return String.format("px.linkTree(%s)", this.toLiteral(label));
	}

	@Override
	protected String foldTree(int beginShift, Symbol label) {
		return String.format("px.foldTree(%d, %s)", beginShift, this.toLiteral(label));
	}

	@Override
	protected String beginTree(int beginShift) {
		return String.format("px.beginTree(%d)", beginShift);
	}

	@Override
	protected String endTree(int endShift, Symbol tag, String value) {
		return String.format("px.endTree(%d, %s, %s)", endShift, this.toLiteral(tag), this.toLiteral(value));
	}

	@Override
	protected String callAction(SymbolAction action, Symbol label, Object thunk) {
		return String.format("callSymbolAction(px, %s, %s, -1, %s)", this.refFunc(action.toString()),
				this.toLiteral(label), this.toLiteral(Objects.toString(thunk)));
	}

	@Override
	protected String callAction(SymbolAction action, Symbol label, int varid, Object thunk) {
		return String.format("callSymbolAction(px, %s, %s, %s, %s)", this.refFunc(action.toString()),
				this.toLiteral(label), this.v("pos", varid), this.toLiteral(Objects.toString(thunk)));
	}

	@Override
	protected String callPredicate(SymbolPredicate pred, Symbol label, Object thunk) {
		return String.format("callSymbolPredicate(px, %s, %s, -1, %s)", this.refFunc(pred.toString()),
				this.toLiteral(label), this.toLiteral(Objects.toString(thunk)));
	}

	@Override
	protected String callPredicate(SymbolPredicate pred, Symbol label, int varid, Object thunk) {
		return String.format("callSymbolPredicate(px, %s, %s, %s, %s)", this.refFunc(pred.toString()),
				this.toLiteral(label), this.v("pos", varid), this.toLiteral(Objects.toString(thunk)));
	}

	/* dispatch */

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
		return String.format("%s::%s", this.getFileBaseName(), funcName);
	}

	@Override
	protected String defineLambda(String match) {
		String lambda = String.format("(px) -> %s", match);
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
	public String callCombinator(String combi, String f) {
		return String.format("%s(px, %s)", combi, f);
	}

	@Override
	public String callCombinator(String combi, Symbol label, String f) {
		return String.format("%s(px, %s, %s)", combi, f, this.toLiteral(label));
	}

	@Override
	public String callCombinator(String combi, int memoPoint, String f) {
		return String.format("%s(px, %s, %s)", combi, f, memoPoint);
	}

	@Override
	protected String memoDispatch(String lookup, String main) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.Line("switch(%s) {", lookup));
		this.incIndent();
		sb.append(this.Line("case 0: return %s;", main));
		sb.append(this.Line("case 1: return %s;", this.s("true")));
		sb.append(this.Line("default: return %s;", this.s("false")));
		this.decIndent();
		sb.append(this.Line("}"));
		return sb.toString();
	}

	@Override
	protected String memoLookup(int memoId, boolean withTree) {
		return String.format("px.memoLookup%s(%d)", withTree ? "Tree" : "", memoId);
	}

	@Override
	protected String memoSucc(int varid, int memoId, boolean withTree) {
		return String.format("px.memoSucc%s(%d, %s)", withTree ? "Tree" : "", memoId, this.v("pos", varid));
	}

	@Override
	protected String memoFail(int varid, int memoId) {
		return String.format("px.memoFail(%d, %s)", memoId, this.v("pos", varid));
	}

	/* Optimization */

	@Override
	protected String back2(int varid) {
		return this.Expr("px.back2(%s,%s)", this.v("pos", varid), this.v("state", varid));
	}

	@Override
	protected String back3(int varid) {
		return this.Expr("px.back3(%s,%s,%s)", this.v("pos", varid), this.v("tree", varid), this.v("treeLog", varid));
	}

	@Override
	protected String back4(int varid) {
		return this.Expr("px.back4(%s,%s,%s,%s)", this.v("pos", varid), this.v("tree", varid), this.v("treeLog", varid),
				this.v("state", varid));
	}

	@Override
	protected boolean useMultiBytes() {
		return true;
	}

	@Override
	protected String matchBytes(byte[] text) {
		return String.format("px.matchBytes(%s)", this.toMultiBytes(text));
	}

	protected String toMultiBytes(byte[] text) {
		StringBuilder sb = new StringBuilder();
		sb.append("t(\"");
		for (int i = 0; i < text.length; i++) {
			int ch = text[i];
			if (ch >= 0x20 && ch < 0x7e && ch != '\\' && ch != '"') {
				sb.append((char) ch);
			} else {
				sb.append(String.format("~%02x", ch & 0xff));
			}
		}
		sb.append("\")");
		return this.getConstName("byte[]", sb.toString());
	}

	@Override
	protected boolean useLexicalOptimization() {
		return true;
	}

	private String param(ByteSet bs) {
		int uchar = bs.getUnsignedByte();
		if (uchar == -1) {
			return this.getConstName("boolean[]", this.toLiteral(bs));
		}
		return "" + uchar;
	}

	private String sig(ByteSet bs) {
		return bs.getUnsignedByte() != -1 ? "B" : "C";
	}

	@Override
	protected String matchRepetition(ByteSet bs) {
		return String.format("pMany%s(px,%s)", this.sig(bs), this.param(bs));
	}

	@Override
	protected String matchAnd(ByteSet bs) {
		return String.format("pAnd%s(px,%s)", this.sig(bs), this.param(bs));
	}

	@Override
	protected String matchNot(ByteSet bs) {
		return String.format("pNot%s(px,%s)", this.sig(bs), this.param(bs));
	}

	@Override
	protected String matchOption(ByteSet bs) {
		return String.format("pOption%s(px,%s)", this.sig(bs), this.param(bs));
	}

}
