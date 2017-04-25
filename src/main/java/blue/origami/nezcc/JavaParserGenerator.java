package blue.origami.nezcc;

import java.io.IOException;
import java.util.Objects;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.peg.Expression;
import blue.nez.peg.expression.ByteSet;
import blue.nez.peg.expression.PLinkTree;
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
	protected void definePrototype(String funcName) {
	}

	@Override
	protected void beginDefine(String funcName, Expression e) {
		this.L("static final <T> boolean %s(%s px) {", funcName, this.s("NezParserContext<T>"));
		this.L("// " + e);
		this.incIndent();
		if (this.isDebug()) {
			this.L("System.out.printf(\"=> %s pos=%%s\\n\", px.pos);", funcName);
		}
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
		return this.Line("return %s;", pe);
	}

	// @Override
	// protected void jump(String pe) {
	// if (this.isDebug()) {
	// this.L("boolean r = %s;", pe);
	// this.L("System.out.printf(\"<= %s pos=%%s, %%s\\n\", px.pos, r);",
	// this.getCurrentFuncName());
	// this.L("return r;");
	// } else {
	// this.L("return %s;", pe);
	// }
	// }
	//
	// @Override
	// protected void jumpSucc() {
	// if (this.isDebug()) {
	// this.L("System.out.printf(\"<= %s pos=%%s, %s\\n\", px.pos);",
	// this.getCurrentFuncName(), this.s("true"));
	// }
	// this.L("return %s;", this.s("true"));
	// }
	//
	// @Override
	// protected void jumpFail() {
	// if (this.isDebug()) {
	// this.L("System.out.printf(\"<= %s pos=%%s, %s\\n\", px.pos);",
	// this.getCurrentFuncName(), this.s("false"));
	// }
	// this.L("return %s;", this.s("false"));
	// }

	@Override
	protected String matchSucc() {
		return this.s("true");
	}

	@Override
	protected String matchFail() {
		return this.s("false");
	}

	@Override
	protected String matchAny() {
		return String.format("(!px.eof()) %s (px.read() != 0)", this.s("&&"));
	}

	@Override
	protected String matchByte(int uchar) {
		return String.format("(px.read() == %s)", uchar);
	}

	@Override
	protected String matchByteSet(ByteSet byteSet) {
		String constName = this.getConstName("boolean[256]", this.toLiteral(byteSet));
		return String.format("%s[px.read()]", constName);
	}

	@Override
	protected String matchPair(String pe, String pe2) {
		return pe + " " + this.s("&&") + " " + pe2;
	}

	@Override
	protected String matchChoice(String pe, String pe2) {
		return pe + " " + this.s("||") + " (" + pe2 + ")";
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
		return this.Line("%s %s = px.state;", this.s("SymbolTable"), this.v("state", varid));
	}

	@Override
	protected String backPos(int varid) {
		return String.format("px.back(%s)", this.v("pos", varid));
	}

	@Override
	protected String backTree(int varid) {
		return String.format("px.back(%s)", this.v("tree", varid));
	}

	@Override
	protected String backTreeLog(int varid) {
		return String.format("px.back(%s)", this.v("treeLog", varid));
	}

	@Override
	protected String backSymbolTable(int varid) {
		return String.format("px.back(%s)", this.v("state", varid));
	}

	@Override
	protected String updatePos(int varid) {
		return this.Line("%s = px.pos;", this.v("pos", varid));
	}

	@Override
	protected String updateTree(int varid) {
		return this.Line("%s = px.tree;", this.v("tree", varid));
	}

	@Override
	protected String updateTreeLog(int varid) {
		return this.Line("%s = px.treeLog;", this.v("treeLog", varid));
	}

	@Override
	protected String updateSymbolTable(int varid) {
		return this.Line("%s = px.state;", this.v("state", varid));
	}

	// @Override
	// protected void storePos(int varid) {
	// this.L("px.pos = %s;", this.v("pos", varid));
	// }
	//
	// @Override
	// protected void storeTree(int varid) {
	// this.L("px.tree = %s;", this.v("tree", varid));
	// }
	//
	// @Override
	// protected void storeTreeLog(int varid) {
	// this.L("px.treeLog = %s;", this.v("treeLog", varid));
	// }
	//
	// @Override
	// protected void storeSymbolTable(int varid) {
	// this.L("px.state = %s;", this.v("state", varid));
	// }
	//
	// @Override
	// protected void beginIfSucc(String pe) {
	// this.L("if(%s) {", pe);
	// this.incIndent();
	// }
	//
	// @Override
	// protected void beginIfFail(String pe) {
	// this.L("if(!(%s)) {", pe);
	// this.incIndent();
	// }
	//
	// @Override
	// protected void orElse() {
	// this.decIndent();
	// this.L("} else {");
	// this.incIndent();
	// }
	//
	// @Override
	// protected void endIf() {
	// this.decIndent();
	// this.L("}");
	// }
	//
	// @Override
	// protected void beginWhileSucc(String pe) {
	// this.L("while(%s) {", pe);
	// this.incIndent();
	//
	// }
	//
	// @Override
	// protected void endWhile() {
	// this.decIndent();
	// this.L("}");
	// }

	@Override
	protected String initCountVar(int varid) {
		return this.Line("int %s = 0;", this.v("cnt", varid));
	}

	@Override
	protected String updateCountVar(int varid) {
		return this.Line("%s++;", this.v("cnt", varid));
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
	protected String callSymbolAction(SymbolAction action, Symbol label) {
		return String.format("px.callSymbolAction(%s, label)", action, this.toLiteral(label));
	}

	@Override
	protected String callSymbolAction(SymbolAction action, Symbol label, int varid) {
		return String.format("px.callSymbolAction(%s, label, %s)", action, this.toLiteral(label), this.v("pos", varid));
	}

	@Override
	protected String callSymbolPredicate(SymbolPredicate pred, Symbol label, Object option) {
		return String.format("px.callSymbolPredicate(%s, label)", pred, this.toLiteral(label),
				this.toLiteral(Objects.toString(option)));
	}

	@Override
	protected String callSymbolPredicate(SymbolPredicate pred, Symbol label, int varid, Object option) {
		return String.format("px.callSymbolPredicate(%s, label)", pred, this.toLiteral(label), this.v("pos", varid),
				this.toLiteral(Objects.toString(option)));
	}

	/* dispatch */

	// @Override
	// public String getFuncMap(PDispatch e) {
	// String cname = this.getFileBaseName();
	// StringBuilder sb = new StringBuilder();
	// sb.append("{");
	// sb.append(cname);
	// sb.append("::fail");
	// for (Expression sub : e) {
	// sb.append(", ");
	// sb.append(cname);
	// sb.append("::");
	// sb.append(this.getFuncName(sub));
	// }
	// sb.append("}");
	// return this.getConstName("ParserFunc<T>[]", sb.toString());
	// }
	//
	// @Override
	// public String matchFuncMap(String funcMap, String jumpIndex) {
	// return String.format("%s[%s].match(px)", funcMap, jumpIndex);
	// }

	@Override
	protected String fetchJumpIndex(byte[] indexMap) {
		String constName = this.getConstName("int[256]", this.toLiteral(indexMap));
		return String.format("%s[px.getbyte()]", constName);
	}

	// @Override
	// protected void beginSwitch(String pe) {
	// this.L("switch(%s) {", pe);
	// this.incIndent();
	// }
	//
	// @Override
	// protected void endSwitch() {
	// this.decIndent();
	// this.L("}");
	//
	// }
	//
	// @Override
	// protected void beginCase(int varid, int i) {
	// this.L("case %d: {", i);
	// this.incIndent();
	//
	// }
	//
	// @Override
	// protected void endCase() {
	// this.decIndent();
	// this.L("}");
	// }

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
	public String getCombinator(Expression e) {
		if (e instanceof PLinkTree) {
			return "pLink";
		}
		return null;
	}

	@Override
	public String matchCombinator(String combi, String f) {
		return String.format("%s(px, %s::%s)", combi, this.getFileBaseName(), f);
	}

}
