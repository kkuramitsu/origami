package blue.origami.nezcc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class RustParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.useUnsignedByte(true);
		this.defineSymbol("\t", "  ");
		this.defineSymbol("null", "ptr::null()");
		this.defineSymbol("true", "true");
		this.defineSymbol("false", "false");
		this.defineSymbol(".", ".");
		this.defineSymbol("const", "const");
		this.defineSymbol("function", "fn");
		this.defineSymbol("switch", "match");
		this.defineSymbol("while", "while");

		//
		this.defineVariable("matched", "i32");
		this.defineVariable("px", "struct NezParserContext*");
		this.defineVariable("inputs", "const unsigned char*");
		this.defineVariable("length", "size_t");
		this.usePointer(true);
		this.defineVariable("pos", "const unsigned char*");
		this.defineVariable("treeLog", "struct TreeLog*");
		this.defineVariable("tree", "void*");
		this.defineVariable("state", "struct State*");
		this.defineVariable("memos", "struct MemoEntry*");

		this.defineVariable("newFunc", "TreeFunc");
		this.defineVariable("setFunc", "TreeSetFunc");
		this.defineVariable("f", "ParserFunc");

		this.defineVariable("c", "unsigned char");
		this.defineVariable("cnt", "u32");
		this.defineVariable("shift", "u32");
		this.defineVariable("indexMap", "&str");
		this.defineVariable("byteSet", "[i32;8]");
		this.defineVariable("s", "const int*");

		this.defineVariable("op", "i32");
		this.defineVariable("label", "const char*");
		this.defineVariable("tag", "const char*");
		this.defineVariable("value", "const unsigned char*");
		this.defineSymbol("len", "strlen(%s)");
		this.defineVariable("data", "const void*");

		this.defineVariable("m", "struct MemoEntry*");
		this.defineVariable("key", "u64");
		this.defineVariable("memoPoint", "i32");
		this.defineVariable("result", "i32");

		// defined
		this.defineSymbol("]}", "]");
		this.defineSymbol("{[", "[");
		this.defineSymbol("match", "match_");
		this.defineSymbol("bitis", "bitis");
		this.defineSymbol("neof", "neof");
		this.defineSymbol("getbyte", "getbyte");
		this.defineSymbol("nextbyte", "nextbyte");
		this.defineSymbol("initMemo", "initMemo");
		this.defineSymbol("longkey", "longkey");
		this.defineSymbol("getMemo", "getMemo");
		this.defineSymbol("memcmp", "(memcmp(%s,%s,%s)==0)");
		this.defineSymbol("matchBytes", "matchBytes");
		this.defineSymbol("extract", "/*%s*/%s");
		this.defineSymbol("case %s: return %s;", "%s => %s,");

		// link freelist
		this.defineSymbol("UtreeLog", "unuseTreeLog");
	}

	@Override
	void makeMatchFunc(ParserGenerator<StringBuilder, String> pg) {
		/* match(px, ch) */
		this.defFunc(pg, this.T("matched"), "match_", "px", "c", () -> {
			String expr = pg.emitFunc("nextbyte", pg.V("px"));
			expr = pg.emitOp(expr, "==", pg.V("c"));
			return (expr);
		});
		/* backpos(px, pos) */
		this.defFunc(pg, this.T("pos"), "backpos", "px", "pos", () -> {
			StringBuilder block = pg.beginBlock();
			pg.emitIfStmt(block, pg.emitOp(pg.emitGetter("px.head_pos"), "<", pg.V("pos")), false, () -> {
				return pg.emitSetter("px.head_pos", pg.V("pos"));
			});
			pg.Return(block, pg.V("pos"));
			return pg.endBlock(block);
		});
	}

	@Override
	protected void writeHeader() throws IOException {
		this.open(this.getFileBaseName() + ".rs");
	}

	@Override
	protected void writeFooter() throws IOException {

	}

	@Override
	protected String formatParam(String type, String name) {
		return String.format("%s: %s", name, type);
	}

	@Override
	protected String emitVarDecl(boolean mutable, String name, String expr) {
		String t = this.T(name);
		String mut = "";
		if (mutable == true) {
			mut = "mut";
		}

		if (t == null) {
			return String.format("let %s %s = %s%s", mut, this.s(name), expr, this.s(";"));
		}
		return String.format("let %s %s: %s = %s%s", mut, this.s(name), t, expr, this.s(";"));
	}

	@Override
	protected void declConst(String typeName, String constName, String literal) {
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

	@Override
	protected String formatSignature(String ret, String funcName, String[] params) {
		if (ret == null || this.useDynamicTyping()) {
			return String.format("%s %s(%s)", this.s("function"), funcName, this.emitParams(params));
		} else {
			return String.format("%s %s(%s) -> %s", this.s("function"), funcName, this.emitParams(params), ret);
		}
	}

	@Override
	protected void declStruct(String typeNameA, String... fields) {
		StringBuilder block = this.beginBlock();
		String typeName = typeNameA.replace("*", "").replace("struct ", "");
		this.emitLine(block, "%s %s %s", this.s("struct"), typeName, this.s("{"));
		this.incIndent();
		for (String f : fields) {
			String n = f.replace("?", "");
			this.emitLine(block, "%s: %s%s", n, this.T(n), this.s(","));
		}
		this.decIndent();
		this.emitLine(block, "}");
		this.defineSymbol(typeNameA, "new" + typeName);
		this.emitLine(block, this.formatSignature(typeNameA, "new" + typeName, fields) + "{");
		this.incIndent();
		this.emitLine(block, "let mut this = %s {", typeNameA);
		for (String f : fields) {
			String n = f.replace("?", "");
			String v = this.emitInit(f);
			this.emitLine(block, "%s: %s,", n, v);
		}
		this.emitLine(block, this.s("}"));
		this.emitLine(block, this.s("&this"));
		this.decIndent();
		this.emitLine(block, this.s("}"));
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return String.format("if %s { %s } else{ %s }", expr, expr2, expr3);
	}

	@Override
	protected String emitWhileStmt(String expr, Block<String> stmt) {
		StringBuilder sb = this.beginBlock();
		this.emitLine(sb, "%s %s %s", this.s("while"), expr, this.s("{"));
		this.incIndent();
		this.emitStmt(sb, stmt.block());
		this.decIndent();
		this.emitStmt(sb, this.s("}"));
		return this.endBlock(sb);
	}

	@Override
	protected String arrayType(String typeName, String constName) {
		int loc = typeName.indexOf('[');
		if (loc >= 0) {
			return typeName.substring(loc);
		}
		return typeName;
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		this.writeSection(String.format("typedef %s (*%s)(%s);", ret, typeName, this.emitParams(params)));
	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		this.writeSection(String.format("%s %s", this.formatSignature(ret, funcName, params), this.s(";")));
	}

	String quote(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		sb.append("\"");
		for (byte ch : bytes) {
			if (ch >= 043 && ch < 126) {
				sb.append((char) ch);
			} else {
				sb.append(String.format("\\x%02x", ch & 0xff));
			}
		}
		sb.append("\"");
		return sb.toString();
	}

	@Override
	protected String matchBytes(byte[] bytes, boolean proceed) {
		String expr;
		if (bytes.length <= 8) {
			expr = String.format("match%d(px->pos,(const unsigned char*)%s)", bytes.length, this.quote(bytes));
		} else {
			expr = String.format("(memcmp((void*)px->pos,%s,%d)==0)", this.quote(bytes), bytes.length);
		}
		if (proceed) {
			expr = this.emitAnd(expr, this.emitMove(this.vInt(bytes.length)));
		}
		return expr;
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		StringBuilder block = this.beginBlock();
		this.emitLine(block, "%s %s %s", this.s("match"), index, this.s("{"));
		this.incIndent();
		for (int i = 0; i < cases.size(); i++) {
			this.emitLine(block, "%s => %s,", i, cases.get(i));
		}
		this.emitLine(block, "_ => %s,", cases.get(0));
		this.decIndent();
		this.emitStmt(block, this.s("}"));
		this.Return(block, this.emitFail());
		return this.endBlock(block);
	}

	@Override
	protected String vValue(String s) {
		if (s != null) {
			byte[] buf = s.getBytes(Charset.forName("UTF-8"));
			return this.getConstName(this.T("indexMap"), this.quote(buf));
		}
		return this.emitNull();
	}

}
