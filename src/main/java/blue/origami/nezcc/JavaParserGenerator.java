package blue.origami.nezcc;

import java.io.IOException;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.ParserGenerator;
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
	}

	@Override
	protected void writeFooter() throws IOException {
		this.out.importResourceContent("/blue/origami/nezcc/javaparser-main.java");
		this.out.println("}");
	}

	@Override
	protected void defineConst(String typeName, String constName, String literal) {
		int loc = typeName.indexOf("[");
		if (loc > 0) { // T[10] a => T a[10]
			// constName = constName + typeName.substring(loc);
			typeName = typeName.substring(0, loc) + "[]";
		}
		this.L("static final %s %s = %s;", typeName, constName, literal);
	}

	@Override
	public void definePrototype(String funcName) {
	}

	@Override
	public void beginDefine(String funcName, Expression e) {
		this.L("static final <T> boolean %s(%s px) {", funcName, this.s("NezParserContext<T>"));
		this.L("// " + e);
		this.incIndent();
		// this.L("System.out.printf(\"=> %s pos=%%s\\n\", px.pos);", funcName);
		this.L("boolean r = %s;", this.s("true"));
	}

	@Override
	public void endDefine(String funcName) {
		// this.L("System.out.printf(\"<= %s pos=%%s, %%s\\n\", px.pos, r);",
		// funcName);
		this.L("return r;");
		this.decIndent();
		this.L("}");
	}

	@Override
	public void matchSucc() {
		this.L("r = %s;", this.s("true"));
	}

	@Override
	public void matchFail() {
		this.L("r = %s;", this.s("false"));
	}

	@Override
	public void matchAny() {
		this.L("r = !px.eof();");
		this.L("px.move(1);");
	}

	@Override
	public void matchByte(int uchar) {
		this.L("r = (px.read() == %s);", uchar);
	}

	@Override
	public void matchByteSet(ByteSet byteSet) {
		String constName = this.getConstName("boolean[256]", this.toLiteral(byteSet));
		this.L("r = %s[px.read()];", constName);
	}

	@Override
	public void matchNonTerminal(String func) {
		this.L("r = %s(px);", func);
	}

	@Override
	public void pushLoadPos(int uid) {
		this.L("%s %s = px.pos;", this.s("int"), this.v("pos", uid));
	}

	@Override
	public void loadTree(int uid) {
		this.L("%s %s = px.tree;", this.s("T"), this.v("tree", uid));
	}

	@Override
	public void loadTreeLog(int uid) {
		this.L("%s %s = px.treeLog;", this.s("TreeLog<T>"), this.v("treeLog", uid));
	}

	@Override
	public void loadSymbolTable(int uid) {
		this.L("%s %s = px.state;", this.s("SymbolTable"), this.v("state", uid));
	}

	@Override
	public void updatePos(int uid) {
		this.L("%s = px.pos;", this.v("pos", uid));
	}

	@Override
	public void updateTree(int uid) {
		this.L("%s = px.tree;", this.v("tree", uid));
	}

	@Override
	public void updateTreeLog(int uid) {
		this.L("%s = px.treeLog;", this.v("treeLog", uid));
	}

	@Override
	public void updateSymbolTable(int uid) {
		this.L("%s = px.state;", this.v("state", uid));
	}

	@Override
	public void storePos(int uid) {
		this.L("px.pos = %s;", this.v("pos", uid));

	}

	@Override
	public void storeTree(int uid) {
		this.L("px.tree = %s;", this.v("tree", uid));
	}

	@Override
	public void storeTreeLog(int uid) {
		this.L("px.treeLog = %s;", this.v("treeLog", uid));
	}

	@Override
	public void storeSymbolTable(int uid) {
		this.L("px.state = %s;", this.v("state", uid));
	}

	@Override
	public void beginIfSucc() {
		this.L("if(%s) {", this.s("r"));
		this.incIndent();
	}

	@Override
	public void beginIfFail() {
		this.L("if(!%s) {", this.s("r"));
		this.incIndent();
	}

	@Override
	public void orElse() {
		this.decIndent();
		this.L("} else {");
		this.incIndent();
	}

	@Override
	public void endIf() {
		this.decIndent();
		this.L("}");
	}

	@Override
	public void beginWhileSucc() {
		this.L("while(%s) {", this.s("r"));
		this.incIndent();

	}

	@Override
	public void endWhile() {
		this.decIndent();
		this.L("}");
	}

	@Override
	public void initCounter(int uid) {
		this.L("int %s = 0;", "c" + uid);
	}

	@Override
	public void countCounter(int uid) {
		this.L("%s++;", "c" + uid);

	}

	@Override
	public void checkCounter(int uid) {
		this.L("if(%s == 0) %s = %s;", "c" + uid, this.s("r"), this.s("false"));
	}

	/* Tree Operation */
	@Override
	public void tagTree(Symbol tag) {
		this.L("px.tagTree(%s);", this.toLiteral(tag));
	}

	@Override
	public void valueTree(String value) {
		this.L("px.valueTree(%s);", this.toLiteral(value));
	}

	@Override
	public void linkTree(int uid, Symbol label) {
		this.L("px.linkTree(%s);", this.toLiteral(label));
	}

	@Override
	public void foldTree(int beginShift, Symbol label) {
		this.L("px.foldTree(%d, %s);", beginShift, this.toLiteral(label));
	}

	@Override
	public void beginTree(int beginShift) {
		this.L("px.beginTree(%d);", beginShift);
	}

	@Override
	public void endTree(int endShift, Symbol tag, String value) {
		this.L("px.endTree(%d, %s, %s);", endShift, this.toLiteral(tag), this.toLiteral(value));
	}

	@Override
	public void callSymbolAction(SymbolAction action, Symbol label) {
		this.L("px.callSymbolAction(%s, label)", action, this.toLiteral(label));
	}

	@Override
	public void callSymbolAction(SymbolAction action, Symbol label, int uid) {
		this.L("px.callSymbolAction(%s, label, %s)", action, this.toLiteral(label), this.v("pos", uid));
	}

	@Override
	public void callSymbolPredicate(SymbolPredicate pred, Symbol label, Object option) {
		this.L("px.callSymbolPredicate(%s, label)", pred, this.toLiteral(label),
				this.toLiteral(Objects.toString(option)));
	}

	@Override
	public void callSymbolPredicate(SymbolPredicate pred, Symbol label, int uid, Object option) {
		this.L("px.callSymbolPredicate(%s, label)", pred, this.toLiteral(label), this.v("pos", uid),
				this.toLiteral(Objects.toString(option)));
	}

	@Override
	public void pushIndexMap(int uid, byte[] indexMap) {
		String constName = this.getConstName("int[256]", this.toLiteral(indexMap));
		this.L("int %s = %s[px.getbyte()];", this.v("d", uid), constName);
	}

	@Override
	public void beginSwitch(int uid) {
		this.L("switch(%s) {", this.v("d", uid));
		this.incIndent();
	}

	@Override
	public void endSwitch() {
		this.decIndent();
		this.L("}");

	}

	@Override
	public void beginCase(int uid, int i) {
		this.L("case %d: {", i);
		this.incIndent();

	}

	@Override
	public void endCase() {
		this.L("break;");
		this.decIndent();
		this.L("}");
	}

	private String toLiteral(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s, '"');
			return sb.toString();
		}
		return this.s("null");
	}

	private String toLiteral(Symbol s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
			return sb.toString();
		}
		return this.s("null");
	}

	private String toLiteral(byte[] indexMap) {
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

	private String toLiteral(ByteSet bs) {
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

}
