package blue.origami.nezcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class GeneratorGenerator extends ParserGenerator<StringBuilder, String> {

	@Override
	public void init(OOption options) {
		super.init(options);
		this.defineSymbol("base", this.getFileBaseName());
		String[] files = options.stringList(ParserOption.InputFiles);
		for (String file : files) {
			if (!file.endsWith(".nezcc")) {
				continue;
			}
			if (!new File(file).isFile()) {
				file = "/blue/origami/nezcc/" + file;
			}
			this.importNezccFile(file);
		}
	}

	private void importNezccFile(String path) {
		try {
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : GeneratorGenerator.class.getResourceAsStream(path);
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
						text.append(line);
						text.append("\n");
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			OConsole.exit(1, e);
		}
	}

	@Override
	String makeLib(String name) {
		if (!this.hasLib(name)) {
			String def = "def " + name;
			if (this.isDefined(def)) {
				this.definedLib(name);
				SourceSection sec = this.openSection(this.RuntimeLibrary);
				this.writeSection(this.s(def));
				this.closeSection(sec);
				return name;
			}
			super.makeLib(name);
		}
		return name;
	}

	boolean check(String name) {
		String def = "def " + name;
		if (this.isDefined(def)) {
			this.defineSymbol(name, name);
			this.makeLib(name);
			return true;
		}
		return false;
	}

	@Override
	protected void initSymbols() {
		this.importNezccFile("/blue/origami/nezcc/default.nezcc");
		this.defineSymbol("\t", "  ");

		this.defineSymbol("Byte[]", this.format("Array", this.s("Byte")));
		this.defineSymbol("Int8", this.s("Byte"));
		this.defineSymbol("Int32", this.s("Int"));
		this.defineSymbol("Symbol", this.s("String"));

		this.defineVariable("px", this.format("structname", "NezParserContext"));
		this.defineVariable("treeLog", this.format("structname", "TreeLog"));
		this.defineVariable("state", this.format("structname", "State"));
		if (this.isDefined("functype")) {
			if (this.isAliasFuncType()) { // alias version
				this.defineVariable("newFunc", this.format("structname", "TreeFunc"));
				this.defineVariable("setFunc", this.format("structname", "TreeSetFunc"));
				this.defineVariable("f", this.format("structname", "ParserFunc"));
			}
		} else {
			this.defineVariable("newFunc", this.s("TreeFunc"));
			this.defineVariable("setFunc", this.s("TreeSetFunc"));
			this.defineVariable("f", this.s("ParserFunc"));
		}

		this.defineVariable("matched", this.s("Bool"));
		this.defineVariable("inputs", this.s("Byte[]"));
		this.defineVariable("pos", this.s("Int"));
		this.defineVariable("length", this.s("Int"));
		this.defineVariable("tree", this.s("Tree"));
		this.defineVariable("c", this.s("Int"));
		this.defineVariable("cnt", this.s("Int"));
		this.defineVariable("shift", this.s("Int"));

		if (this.check("bitis")) {
			this.defineVariable("s", this.format("Array", this.s("Int32")));
		} else {
			this.defineVariable("s", this.format("Array", this.s("Bool")));
		}

		this.defineVariable("op", this.s("Int32"));
		this.defineVariable("label", this.s("Symbol"));
		this.defineVariable("tag", this.s("Symbol"));
		this.defineVariable("value", this.T("inputs"));

		this.defineVariable("m", this.typeparam("MemoEntry"));
		if (this.isDefined("List")) {
			this.defineVariable("memos", this.format("List", this.T("m")));
		} else {
			this.defineVariable("memos", this.format("Array", this.T("m")));
		}
		this.defineVariable("key", this.s("Int64"));
		this.defineVariable("memoPoint", this.s("Int32"));
		this.defineVariable("result", this.s("Int32"));
		this.defineVariable("text", this.s("String"));
	}

	private String typeparam(String t) {
		if (this.isDefined("typeparam")) {
			return this.format("typeparam", t, this.s("Tree"));
		}
		return t;
	}

	@Override
	protected void writeHeader() throws IOException {
		if (this.isDefined("extension")) {
			this.open(this.getFileBaseName() + "." + this.s("extension"));
		}
		if (this.isDefined("imports")) {
			this.writeLine(this.s("imports"));
		}
		if (this.isDefined("module")) {
			this.writeLine("");
			this.writeLine(this.s("module"));
		}
		if (this.isDefined("libs")) {
			this.writeLine("");
			this.writeLine(this.s("libs"));
		}
		this.writeLine("");
	}

	@Override
	protected void writeFooter() throws IOException {
		if (this.isDefined("ast")) {
			this.writeLine(this.s("ast"));
		} else {
			this.lib = new SourceSection();
			this.makeLib("parse");
			this.writeLine(this.lib.toString());
		}
		if (this.isDefined("exports")) {
			this.writeLine(this.s("exports"));
		}
		if (this.isDefined("main")) {
			this.writeLine(this.s("main"));
		}
		if (this.isDefined("module") && this.isDefined("end module")) {
			this.writeLine(this.s("end module"));
		}
		if (this.isDefined("man")) {
			OConsole.println(OConsole.color(OConsole.Cyan, this.s("man")));
		}
	}

	protected void emitLine(StringBuilder block, String format, Object... args) {
		if (args.length == 0) {
			this.emitStmt(block, format);
		} else {
			this.emitStmt(block, String.format(format, args));
		}
	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		if (this.isDefined("def " + typeName)) {
			this.writeSection(this.s("def " + typeName));
			return;
		}
		StringBuilder block = this.beginBlock();
		String record = "";
		if (this.isDefined("record")) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			for (String f : fields) {
				if (c > 0) {
					sb.append(this.s("records"));
				}
				sb.append(this.format("record", this.T(f), f));
				c++;
			}
			record = sb.toString();
		}
		this.emitLine(block, this.format("struct", typeName, record));
		this.incIndent();
		if (this.isDefined("field")) {
			for (String f : fields) {
				f = f.replace("?", "");
				this.emitLine(block, this.format("field", this.T(f), f));
			}
		}
		if (this.isDefined("constructor")) {
			this.emitLine(block, this.format("constructor", typeName, this.emitParams(fields)));
			this.incIndent();
			this.emitInits(block, fields);
			this.decIndent();
			this.emitLine(block, this.format("end constructor"));
		}
		this.decIndent();
		this.emitLine(block, this.format("end struct", typeName));
		this.writeSection(this.endBlock(block));
		if (this.isDefined("new")) {
			this.defineSymbol(typeName, this.format("new", typeName));
		}
		//
		if (this.isDefined("malloc")) {
			String var = this.varname(typeName);
			ArrayList<String> params = new ArrayList<>();
			for (String f : fields) {
				if (f.endsWith("?")) {
					continue;
				}
				params.add(f);
			}
			this.defFunc(this, this.T(var), typeName, params.toArray(new String[params.size()]), () -> {
				StringBuilder block2 = this.beginBlock();
				// this.emitLine(block, this.format("newfunc", typeName,
				// this.emitParams(fields)));
				this.emitVarDecl(block2, true, var, this.format("malloc", typeName));
				for (String f : fields) {
					String n = f.replace("?", "");
					String v = this.emitInit(f);
					this.emitLine(block2, this.format("setter", this.V(var), n, v));
				}
				this.Return(block2, this.V(var));
				return this.endBlock(block2);
			});
		}
	}

	private String varname(String typeName) {
		switch (typeName) {
		case "NezParserContext":
			return "px";
		case "TreeLog":
			return "treeLog";
		case "State":
			return "state";
		case "MemoEntry":
			return "m";
		case "ParserFunc":
			return "f";
		case "TreeFunc":
			return "newFunc";
		case "TreeSetFunc":
			return "setFunc";
		}
		return null; // undefined to be errored
	}

	private boolean isAliasFuncType() {
		if (this.isDefined("functype")) {
			String functype = this.s("functype");
			return functype.indexOf("%2$s") > 0;
		}
		return false;
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		if (this.isDefined("functype")) {
			ArrayList<String> l = new ArrayList<>();
			for (String p : params) {
				l.add(this.format("functypeparam", this.T(p), p));
			}
			String decl = this.format("functype", ret, typeName, this.emitList("functypeparams", l));
			if (this.isAliasFuncType()) {
				this.writeSection(decl);
			} else {
				System.out.println(typeName + " => " + decl);
				this.defineVariable(this.varname(typeName), decl);
			}
		}
	}

	@Override
	protected void declConst(String typeName, String constName, int arraySize, String literal) {
		if (arraySize == -1) {
			this.writeSection(this.format("const", typeName, constName, literal));
		} else if (this.isDefined("const_array")) {
			this.writeSection(this.format("const_array", typeName, constName, arraySize, literal));
		} else {
			this.writeSection(this.format("const", this.format("Array", typeName), constName, literal));
		}
	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		if (this.isDefined("prototype")) {
			this.writeSection(this.format("prototype", ret, funcName, this.emitParams(params)));
		}
	}

	@Override
	protected void declFunc(int acc, String ret, String funcName, String[] params, Block<String> block) {
		this.writeSection(this.format("function", ret, funcName, this.emitParams(params)));
		this.incIndent();
		this.writeSection(this.formatFuncResult(block.block()));
		this.decIndent();
		this.writeSection(this.s("end function"));
	}

	@Override
	protected String emitParams(String... params) {
		ArrayList<String> l = new ArrayList<>();
		for (String p : params) {
			if (p.endsWith("?")) {
				continue;
			}
			String t = this.T(p);
			l.add(this.format("param", t, this.V(p)));
		}
		return this.emitList("params", l);
	}

	protected String emitList(String delim, List<String> list) {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (String p : list) {
			if (c > 0) {
				sb.append(this.s(delim));
			}
			sb.append(p);
			c++;
		}
		return sb.toString();
	}

	protected void emitInits(StringBuilder block, String... fields) {
		for (String f : fields) {
			String n = f.replace("?", "");
			String v = this.emitInit(f);
			this.emitLine(block, this.format("init", n, v));
		}
	}

	protected String emitInit(String f) {
		if (f.endsWith("?")) {
			String n = f.replace("?", "");
			if (this.isDefined("I" + n)) {
				return this.s("I" + n);
			} else {
				return this.emitNull(n);
			}
		}
		return this.V(f);
	}

	protected String formatFuncResult(String expr) {
		if (expr.startsWith(" ") || expr.startsWith("\t")) {
			return expr;
		}
		// if (this.isDebug()) {
		// String funcName = this.vString(this.getCurrentFuncName());
		// expr = this.emitAnd(this.emitFunc("B", funcName, this.V("px")),
		// this.emitFunc("E", funcName, this.V("px"), expr));
		// }
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
		if (this.isDefined("return")) {
			return this.format("return", expr);
		}
		return expr + this.s(";");
	}

	@Override
	protected String emitVarDecl(boolean mutable, String name, String expr) {
		String t = this.T(name);
		if (mutable) {
			return this.format("var", t, this.V(name), expr);
		} else {
			return this.format("val", t, this.V(name), expr);
		}
	}

	@Override
	protected String emitAssign(String name, String expr) {
		return this.format("assign", this.V(name), expr);
	}

	@Override
	protected String emitAssign2(String left, String expr) {
		return this.format("assign", left, expr);
	}

	@Override
	protected String emitIfStmt(String expr, boolean elseIf, Block<String> stmt) {
		StringBuilder sb = this.beginBlock();
		if (this.isDefined("else if")) {
			sb.append(this.format(elseIf ? "else if" : "if", expr));
		} else {
			sb.append(this.format("if", expr));
		}
		this.incIndent();
		this.emitStmt(sb, stmt.block());
		this.decIndent();
		this.emitStmt(sb, this.format("end if"));
		return this.endBlock(sb);
	}

	@Override
	protected String emitWhileStmt(String expr, Block<String> stmt) {
		StringBuilder sb = this.beginBlock();
		this.emitLine(sb, this.s("while"), expr);
		this.incIndent();
		this.emitStmt(sb, stmt.block());
		this.decIndent();
		this.emitStmt(sb, this.s("end while"));
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
		if (t != null && this.isDefined("cast")) {
			return this.format("cast", t, expr);
		}
		return expr;
	}

	@Override
	protected String emitNull(String name) {
		String key = "N" + name;
		if (this.isDefined(key)) {
			return this.s(key);
		}
		return this.s("null");
	}

	@Override
	protected String emitArrayLength(String a) {
		return this.format("Array.size", a);
	}

	@Override
	protected String emitArrayIndex(String a, String index) {
		if (a.endsWith("memos") && this.isDefined("List")) {
			return this.format("List.get", a, index);
		}
		return this.format("Array.get", a, index);
	}

	@Override
	protected String emitNewArray(String type, String index) {
		if (type.startsWith("MemoEntry") && this.isDefined("List")) {
			return this.format("List.new", type, index);
		}
		return this.format("Array.new", type, index);
	}

	@Override
	protected String emitChar(int uchar) {
		return "" + (uchar & 0xff);
	}

	@Override
	protected String emitGetter(String self, String name) {
		return this.format("getter", this.s(self), this.s(name));
	}

	@Override
	protected String emitSetter(String self, String name, String expr) {
		return this.format("setter", this.s(self), this.s(name), expr);
	}

	@Override
	protected String emitFunc(String func, List<String> params) {
		String fmt = this.getSymbol(func);
		if (fmt != null && fmt.indexOf("%s") >= 0) {
			return String.format(fmt, params.toArray(new Object[params.size()]));
		}
		StringBuilder sb = new StringBuilder();
		String delim = this.isDefined("args") ? this.s("args") : " ";
		int c = 0;
		for (String p : params) {
			if (c > 0) {
				sb.append(delim);
			}
			c++;
			sb.append(p);
		}
		return this.format("funccall", this.s(func), sb.toString());
	}

	@Override
	protected String emitApply(String func, List<String> params) {
		if (this.isDefined("apply")) {
			func = this.format("apply", func);
		}
		return this.emitFunc(func, params);
	}

	@Override
	protected String emitNot(String expr) {
		return this.format("not", expr);
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
		return this.format("and", expr, expr2);
	}

	@Override
	protected String emitOr(String expr, String expr2) {
		return this.format("or", expr, expr2);
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return this.format("ifexpr", expr, expr2, expr3);
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		if (this.isDefined("switch")) {
			StringBuilder block = this.beginBlock();
			this.emitLine(block, this.format("switch", index));
			this.incIndent();
			if (this.isDefined("default")) {
				for (int i = 1; i < cases.size(); i++) {
					this.emitLine(block, this.format("case", i, this.emitReturn(cases.get(i))));
				}
				this.emitLine(block, this.format("default", this.emitReturn(cases.get(0))));
			} else {
				for (int i = 0; i < cases.size(); i++) {
					this.emitLine(block, this.format("case", i, this.emitReturn(cases.get(i))));
				}
			}
			this.decIndent();
			this.emitStmt(block, this.s("end switch"));
			if (this.isDefined("return")) {
				this.Return(block, this.emitFail());
			}
			return this.endBlock(block);
		} else if (this.useLambda() && this.isNotIncludeMemo(cases)) {
			String funcMap = this.vFuncMap(cases);
			return this.emitApply(this.emitArrayIndex(funcMap, index), this.V("px"));
		} else {
			StringBuilder block = this.beginBlock();
			this.emitVarDecl(block, false, "result", index);
			boolean elseIf = false;
			for (int i = 1; i < cases.size(); i++) {
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
		this.makeLib("fTrue");
		this.makeLib("fFalse");
		StringBuilder sb = new StringBuilder();
		String delim = this.s("arrays");
		sb.append(this.s("array"));
		int c = 0;
		for (String code : cases) {
			if (c > 0) {
				sb.append(delim);
			}
			if (code.equals(this.s("false"))) {
				sb.append(this.emitFuncRef("fFalse"));
			} else if (code.equals(this.s("true"))) {
				sb.append(this.emitFuncRef("fTrue"));
			} else {
				sb.append(this.emitParserLambda(code));
			}
			c++;
		}
		sb.append(this.s("end array"));
		return this.getConstName(this.T("f"), this.Const("funcmap"), cases.size(), sb.toString());
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
		if (this.isDefined("base64")) {
			byte[] encoded = Base64.getEncoder().encode(indexMap);
			return this.getConstName(this.s("Int8"), this.Const("choice"), encoded.length,
					this.format("base64", new String(encoded)));
		}
		StringBuilder sb = new StringBuilder();
		String delim = this.s("arrays");
		sb.append(this.s("array"));
		for (int i = 0; i < indexMap.length; i++) {
			if (i > 0) {
				sb.append(delim);
			}
			sb.append(indexMap[i] & 0xff);
		}
		sb.append(this.s("end array"));
		return this.getConstName(this.s("Int8"), this.Const("choice"), indexMap.length, sb.toString());
	}

	protected ArrayList<Symbol> symbolList = new ArrayList<>();
	protected HashMap<Symbol, Integer> symbolMap = new HashMap<>();
	protected ArrayList<byte[]> valueList = new ArrayList<>();
	protected HashMap<byte[], Integer> valueMap = new HashMap<>();

	@Override
	protected void declSymbolTables() {
		String delim = this.s("arrays");
		if (this.symbolList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			sb.append(this.emitNull("tag"));
			for (Symbol s : this.symbolList) {
				sb.append(delim);
				sb.append(this.vString(s.getSymbol()));
			}
			sb.append(this.s("end array"));
			this.declConst(this.s("Symbol"), "SYMBOLs", this.symbolList.size() + 1, sb.toString());
		}
		if (this.valueList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			sb.append(this.emitNull("value"));
			for (byte[] s : this.valueList) {
				sb.append(delim);
				sb.append(this.rawValue(s));
			}
			sb.append(this.s("end array"));
			this.declConst(this.T("value"), "VALUEs", this.valueList.size() + 1, sb.toString());
		}
		if (this.valueList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			sb.append(this.vInt(0));
			for (byte[] s : this.valueList) {
				sb.append(delim);
				sb.append(this.vInt(s.length));
			}
			sb.append(this.s("end array"));
			this.declConst(this.T("length"), "LENGTHs", this.valueList.size() + 1, sb.toString());
		}
	}

	protected String rawValue(byte[] buf) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.s("array"));
		int c = 0;
		for (byte b : buf) {
			if (c > 0) {
				sb.append(this.s("arrays"));
			}
			sb.append(b);
			c++;
		}
		sb.append(this.s("end array"));
		return this.getConstName(this.s("Byte"), this.Const("data"), buf.length, sb.toString());
	}

	@Override
	protected String matchBytes(byte[] bytes, boolean proceed) {
		String expr;
		if (bytes.length <= 8) {
			if (this.check("next" + bytes.length)) {
				expr = this.emitFunc("next" + bytes.length, this.V("px"), this.rawValue(bytes));
				if (!proceed) {
					expr = this.emitAnd(expr, this.emitMove(this.vInt(-bytes.length)));
				}
				return expr;
			}
		} else {
			if (this.check("nextN")) {
				expr = this.emitFunc("nextN", this.V("px"), this.rawValue(bytes), this.vInt(bytes.length));
				if (!proceed) {
					expr = this.emitAnd(expr, this.emitMove(this.vInt(-bytes.length)));
				}
				return expr;
			}
		}
		return super.matchBytes(bytes, proceed);
	}

	@Override
	protected String vTag(Symbol s) {
		if (s != null) {
			Integer n = this.symbolMap.get(s);
			if (n == null) {
				this.symbolList.add(s);
				n = this.symbolList.size();
				this.symbolMap.put(s, n);
			}
			return this.vInt(n);
		}
		return this.vInt(0);
	}

	@Override
	protected String vLabel(Symbol s) {
		if (s != null) {
			Integer n = this.symbolMap.get(s);
			if (n == null) {
				this.symbolList.add(s);
				n = this.symbolList.size();
				this.symbolMap.put(s, n);
			}
			return this.vInt(n);
		}
		return this.vInt(0);
	}

	protected String vValue(byte[] s) {
		if (s != null) {
			Integer n = this.valueMap.get(s);
			if (n == null) {
				this.valueList.add(s);
				n = this.valueList.size();
				this.valueMap.put(s, n);
			}
			return this.vInt(n);
		}
		return this.vInt(0);
	}

	@Override
	protected String vValue(String s) {
		if (s != null) {
			return this.vValue(s.getBytes(Charset.forName("UTF-8")));
		}
		return this.vInt(0);
	}

	@Override
	protected String vThunk(Object s) {
		return this.s("null");
	}

	@Override
	protected String vByteSet(ByteSet bs) {
		String delim = this.s("arrays");
		if (this.isDefined("bitis")) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			for (int i = 0; i < 8; i++) {
				if (i > 0) {
					sb.append(delim);
				}
				sb.append(bs.bits()[i]);
			}
			sb.append(this.s("end array"));
			return this.getConstName(this.s("Int32"), this.Const("charset"), 8, sb.toString());
		} else {
			String literal;
			if (this.isDefined("bools")) {
				int last = 0;
				for (int i = 0; i < 256; i++) {
					if (bs.is(i)) {
						last = i;
					}
				}
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <= last; i++) {
					sb.append(bs.is(i) ? "1" : "0");
				}
				literal = this.format("bools", sb.toString());
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(this.s("array"));
				for (int i = 0; i < 256; i++) {
					if (i > 0) {
						sb.append(delim);
					}
					sb.append(bs.is(i) ? this.s("true") : this.s("false"));
				}
				sb.append(this.s("end array"));
				literal = sb.toString();
			}
			return this.getConstName(this.s("Bool"), this.Const("charset"), 256, literal);
		}
	}

	@Override
	protected String emitFuncRef(String funcName) {
		if (this.isDefined("funcref")) {
			return this.format("funcref", funcName);
		}
		return funcName;
	}

	@Override
	protected String emitParserLambda(String match) {
		String px = this.V("px");
		String lambda = String.format(this.s("lambda"), px, match);
		String p = "p" + this.varSuffix();
		return lambda.replace("px", p);
	}

	@Override
	public String V(String name) {
		if (this.isDefined(name)) {
			return this.s(name);
		}
		if (this.isDefined("varname")) {
			return this.format("varname", name);
		}
		return name;
	}

	@Override
	protected void declTree() {
	}

	@Override
	protected String emitInputString(String text) {
		if (this.isDefined("zero")) {
			return this.format("zero", text);
		}
		return String.format("(%s + \"\\0\")", text);
	}

	@Override
	protected String emitNewToken(String tag, String inputs, String pos, String epos) {
		if (this.isDefined("pair")) {
			String sub = this.format("subbytes", inputs, pos, epos);
			return this.format("pair", tag, this.format("toString", sub));
		}
		return this.emitNull("tree");
	}

	@Override
	protected String emitNewTree(String tag, String nsubs) {
		if (this.isDefined("pair")) {
			return this.format("pair", tag, this.emitNewArray("pair", nsubs));
		}
		return this.emitNull("tree");
	}

	@Override
	protected String emitSetTree(String parent, String n, String label, String child) {
		if (this.isDefined("pair")) {
			StringBuilder block = this.beginBlock();
			String left = this.emitArrayIndex(this.format("second", parent), n);
			String right = this.format("pair", label, child);
			this.emitLine(block, this.emitAssign2(left, right));
			this.emitStmt(block, this.emitReturn(parent));
			return (this.endBlock(block));
		}
		return this.emitNull("tree");
	}

	@Override
	public String Const(String name) {
		return this.format("constname", name);
	}

	@Override
	protected String emitUnsigned(String expr) {
		if (!this.isDefined("Byte->Int")) {
			return expr;
		}
		return this.format("Byte->Int", expr);
	}

}
