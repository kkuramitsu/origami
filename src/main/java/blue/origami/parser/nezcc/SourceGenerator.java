package blue.origami.parser.nezcc;

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
import java.util.function.Supplier;

import blue.origami.Version;
import blue.origami.common.OCommonWriter;
import blue.origami.common.OConsole;
import blue.origami.common.OOption;
import blue.origami.common.OStringUtils;
import blue.origami.common.Symbol;
import blue.origami.main.MainOption;
import blue.origami.parser.peg.ByteSet;

public class SourceGenerator extends ParserGenerator<StringBuilder, String> {

	@Override
	public void init(OOption options) {
		super.init(options);
		this.defineSymbol("base", this.getFileBaseName());
		String[] files = options.stringList(MainOption.InputFiles);
		this.initDefaultSymbols();
		for (String file : files) {
			if (!file.endsWith(".nezcc")) {
				continue;
			}
			if (!new File(file).isFile()) {
				file = Version.ResourcePath + "/nezcc/" + file;
			}
			this.importNezccFile(file);
		}
	}

	private void importNezccFile(String path) {
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

	@Override
	protected boolean check(String name) {
		String def = "def " + name;
		if (this.isDefined(def)) {
			this.defineSymbol(name, name);
			this.makeLib(name);
			return true;
		}
		return false;
	}

	protected void initDefaultSymbols() {
		this.defineSymbol("nezcc", "nezcc/1.0.1");
		this.defineSymbol("space", " ");
	}

	@Override
	protected void setupSymbols() {
		if (this.isDefined("include")) {
			this.importNezccFile(this.s("include"));
		}
		this.importNezccFile(Version.ResourcePath + "/nezcc/default.nezcc");

		this.defineSymbol("tab", "  ");
		if (this.isDefined("eq")) {
			this.defineSymbol("==", this.s("eq"));
		}
		if (this.isDefined("ne")) {
			this.defineSymbol("!=", this.s("ne"));
		}
		if (this.isDefined("options")) {
			for (String opt : this.s("options").split(",")) {
				this.defineSymbol("O" + opt, opt);
			}
		}

		if (this.isDefined("Array")) {
			this.defineSymbol("Byte[]", this.format("Array", this.s("Byte")));
		}
		this.defineSymbol("Int8", this.s("Byte"));
		this.defineSymbol("Symbol", this.s("String"));

		if (!this.isDefined("Tpx")) {
			String t = this.format("structname", "NezParserContext");
			this.defineVariable("px", t);
		}
		if (!this.isDefined("TtreeLog")) {
			String t = this.format("structname", "TreeLog");
			if (this.isDefined("Option")) {
				this.defineVariable("tcur", t);
				t = this.format("Option", t);
			}
			this.defineVariable("treeLog", t);
		}
		if (!this.isDefined("Tstate")) {
			String t = this.format("structname", "State");
			if (this.isDefined("Option")) {
				this.defineVariable("scur", t);
				t = this.format("Option", t);
			}
			this.defineVariable("state", t);
		}
		this.defineVariable("tcur", this.T("treeLog"));
		this.defineVariable("scur", this.T("state"));
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
		this.defineVariable("headpos", this.T("pos"));
		this.defineSymbol("backpos", "backpos");
		this.defineVariable("length", this.s("Int"));
		this.defineVariable("tree", this.s("Tree"));
		this.defineVariable("c", this.s("Int"));
		this.defineVariable("n", this.s("Int"));
		this.defineVariable("cnt", this.s("Int"));
		this.defineVariable("shift", this.s("Int"));

		if (this.isDefined("Int32")) {
			this.defineVariable("bits", this.format("Array", this.s("Int32")));
		} else {
			this.defineVariable("bits", this.format("Array", this.s("Bool")));
		}

		this.defineVariable("label", this.s("Symbol"));
		this.defineVariable("tag", this.s("Symbol"));
		this.defineVariable("value", this.T("inputs"));

		this.defineVariable("lop", this.s("Int"));
		this.defineVariable("lpos", this.T("length"));
		this.defineVariable("ltree", this.T("tree")); // haskell

		if (!this.isDefined("m")) {
			String t = this.format("structname", "MemoEntry");
			this.defineVariable("m", t);
		}

		if (this.isDefined("MemoList")) {
			this.defineVariable("memos", this.format("MemoList", this.T("m")));
		} else {
			this.defineVariable("memos", this.format("Array", this.T("m")));
		}

		this.defineVariable("subtrees", this.s("TreeList"));
		// this.defineSymbol("TreeList.empty", this.s("null"));
		// this.defineSymbol("TreeList.cons", "%3$s");

		if (this.isDefined("Int64")) {
			this.defineVariable("key", this.s("Int64"));
		} else {
			this.defineVariable("key", this.s("Int"));
		}
		this.defineVariable("mpoint", this.s("Int"));
		this.defineVariable("result", this.s("Int"));
		this.defineVariable("text", this.s("String"));

		this.defineVariable("label", this.s("Symbol"));
		this.defineVariable("tag", this.s("Symbol"));
		this.defineVariable("ntag", this.T("cnt"));
		this.defineVariable("ntag0", this.T("cnt"));
		this.defineVariable("nlabel", this.T("cnt"));
		this.defineVariable("value", this.T("inputs"));
		this.defineVariable("nvalue", this.T("cnt"));
		this.defineVariable("spos", this.T("cnt"));
		this.defineVariable("epos", this.T("cnt"));
		this.defineVariable("shift", this.T("cnt"));
		this.defineVariable("length", this.T("cnt"));

		this.defineVariable("memoPoint", this.T("cnt"));
		this.defineVariable("result", this.T("cnt"));

		this.defineVariable("mpos", this.T("pos")); // haskell
		this.defineVariable("mtree", this.T("tree")); // haskell
		this.defineVariable("mstate", this.T("state")); // haskell

		this.defineVariable("lprev", this.T("treeLog"));
		this.defineVariable("lnext", this.T("treeLog"));
		this.defineVariable("sprev", this.T("state"));

		this.defineVariable("epos", this.T("pos"));
		this.defineVariable("child", this.T("tree"));
		this.defineVariable("f2", this.T("f"));
		this.defineSymbol("Intag", "0");
		this.defineSymbol("Ikey", "-1");
		this.defineSymbol("Icnt", "0");
		this.defineSymbol("Ipos", "0");
		this.defineSymbol("Ilop", "0");
		this.defineSymbol("Ilpos", "0");
		this.defineSymbol("Iheadpos", "0");
		this.defineSymbol("Iresult", "0");
		if (this.isDefined("paraminit")) {
			this.defineSymbol("PInewFunc", this.emitFuncRef("newAST"));
			this.defineSymbol("PIsetFunc", this.emitFuncRef("subAST"));
		}
	}

	private OCommonWriter out = new OCommonWriter();

	protected void open(String file) throws IOException {
		this.out.open(file);
	}

	protected void writeCode(String symbol) {
		if (this.isDefined(symbol)) {
			this.out.p(this.s(symbol));
			this.out.println();
		}
	}

	@Override
	protected void writeSourceSection(SourceSection section) {
		this.out.p(section);
	}

	@Override
	protected void writeHeader() throws IOException {
		if (this.isDefined("extension")) {
			this.open(this.getFileBaseName() + "." + this.s("extension"));
		}
		this.writeCode("imports");
		this.writeCode("module");
		this.writeCode("libs");
	}

	@Override
	protected void writeFooter() throws IOException {
		this.lib = new SourceSection();
		this.makeLib("parse");
		this.writeSourceSection(this.lib);
		if (this.isDefined("TList")) {
			if (this.isDefined("main")) {
				this.lib = new SourceSection();
				this.makeLib("AST");
				this.makeLib("newAST2");
				this.writeSourceSection(this.lib);
				this.writeCode("main");
			}
		} else {
			if (this.isDefined("main")) {
				this.lib = new SourceSection();
				this.makeLib("AST");
				this.makeLib("newAST");
				this.makeLib("subAST");
				this.writeSourceSection(this.lib);
				this.writeCode("main");
			}
		}
		if (this.isDefined("module")) {
			this.writeCode("end module");
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
		if (this.isDefined("object")) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			for (String f : fields) {
				if (c > 0) {
					sb.append(",");
				}
				sb.append(f);
				c++;
			}
			this.defineSymbol("fields " + typeName, sb.toString());
		}
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
			this.defFunc(this, 0, this.T(var), typeName, params.toArray(new String[params.size()]), () -> {
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
				// System.out.println(typeName + " => " + decl);
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
	protected void declFunc(int acc, String ret, String funcName, String[] params, Supplier<String> block) {
		String funcType = "";
		if (this.isDefined("functype") && !this.isAliasFuncType()) {
			ArrayList<String> l = new ArrayList<>();
			for (String p : params) {
				l.add(this.format("functypeparam", this.T(p), p));
			}
			funcType = this.format("functype", ret, funcName, this.emitList("functypeparams", l));
		}
		String f = "function" + acc;
		if (!this.isDefined(f)) {
			f = "function";
		}
		this.writeSection(this.format(f, ret, this.funcName(funcName), this.emitParams(params), funcType));
		this.incIndent();
		this.writeSection(this.formatFuncResult(block.get()));
		this.decIndent();
		this.writeSection(this.s("end function"));
	}

	protected String emitParams(String... params) {
		ArrayList<String> l = new ArrayList<>();
		for (String p : params) {
			if (p.endsWith("?")) {
				continue;
			}
			String t = this.T(p);
			String pinit = this.s("PI", p);
			if (pinit != null && this.isDefined("paraminit")) {
				l.add(this.format("paraminit", t, this.V(p), pinit));
			} else {
				l.add(this.format("param", t, this.V(p)));
			}
		}
		return this.emitList("params", l);
	}

	protected String emitList(String delim, List<String> list) {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		delim = this.s(delim);
		if (delim.length() == 0) {
			delim = " ";
		}
		for (String p : list) {
			if (c > 0) {
				sb.append(delim);
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
		StringBuilder sb = new StringBuilder();
		if (this.isDefined("do")) {
			sb.append(this.s("do"));
		}
		return sb;
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
	protected void Setter2(StringBuilder block, String base, String name, String expr, String name2, String expr2) {
		if (this.isDefined("setter2")) {
			this.emitStmt(block, this.emitFunc("setter2", base, name, expr, name2, expr2));
		} else {
			super.Setter2(block, base, name, expr, name2, expr2);
		}
	}

	@Override
	protected void Setter3(StringBuilder block, String base, String name, String expr, String name2, String expr2,
			String name3, String expr3) {
		if (this.isDefined("setter3")) {
			this.emitStmt(block, this.emitFunc("setter3", base, name, expr, name2, expr2, name3, expr3));
		} else {
			super.Setter3(block, base, name, expr, name2, expr2, name3, expr3);
		}
	}

	@Override
	protected void Setter4(StringBuilder block, String base, String name, String expr, String name2, String expr2,
			String name3, String expr3, String name4, String expr4) {
		if (this.isDefined("setter4")) {
			this.emitStmt(block, this.emitFunc("setter4", base, name, expr, name2, expr2, name3, expr3, name4, expr4));
		} else {
			super.Setter4(block, base, name, expr, name2, expr2, name3, expr3, name4, expr4);
		}
	}

	@Override
	protected void Setter5(StringBuilder block, String base, String name, String expr, String name2, String expr2,
			String name3, String expr3, String name4, String expr4, String name5, String expr5) {
		if (this.isDefined("setter5")) {
			this.emitStmt(block,
					this.emitFunc("setter5", base, name, expr, name2, expr2, name3, expr3, name4, expr4, name5, expr5));
		} else {
			super.Setter5(block, base, name, expr, name2, expr2, name3, expr3, name4, expr4, name5, expr5);
		}
	}

	@Override
	protected void emitBack2(StringBuilder block, String... vars) {
		if (vars.length == 2 && this.isDefined("setter2")) {
			this.emitStmt(block, this.emitFunc("setter2", "px", vars[0], this.V(vars[0]), vars[1], this.V(vars[1])));
		} else if (vars.length == 3 && this.isDefined("setter3")) {
			this.emitStmt(block, this.emitFunc("setter3", "px", vars[0], this.V(vars[0]), vars[1], this.V(vars[1]),
					vars[2], this.V(vars[2])));
		} else if (vars.length == 4 && this.isDefined("setter4")) {
			this.emitStmt(block, this.emitFunc("setter4", "px", vars[0], this.V(vars[0]), vars[1], this.V(vars[1]),
					vars[2], this.V(vars[2]), vars[3], this.V(vars[3])));
		} else if (vars.length == 5 && this.isDefined("setter5")) {
			this.emitStmt(block, this.emitFunc("setter5", "px", vars[0], this.V(vars[0]), vars[1], this.V(vars[1]),
					vars[2], this.V(vars[2]), vars[3], this.V(vars[3]), vars[4], this.V(vars[4])));
		} else {
			super.emitBack2(block, vars);
		}
	}

	@Override
	protected String emitBlockExpr(String type, List<String> exprs) {
		StringBuilder sb = new StringBuilder();
		sb.append(this.format("block", type));
		for (int i = 0; i < exprs.size() - 1; i++) {
			String e = exprs.get(i);
			sb.append(e);
			sb.append(this.s("blocks"));
		}
		sb.append(this.format("end block", exprs.get(exprs.size() - 1)));
		return sb.toString();
	}

	@Override
	protected String emitReturn(String expr) {
		if (this.isDefined("return")) {
			return this.format("return", expr);
		}
		return expr + this.s(";");
	}

	@Override
	protected String emitVarDecl(boolean mutable, String name, String expr, String expr2) {
		String t = this.T(name);
		if (expr2 == null) {
			if (mutable) {
				return this.format("var", t, this.V(name), expr);
			} else {
				return this.format("val", t, this.V(name), expr);
			}
		} else {
			return this.format("letin", t, this.V(name), expr, expr2);
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
	protected String emitIfStmt(String expr, boolean elseIf, Supplier<String> stmt) {
		StringBuilder sb = this.beginBlock();
		if (this.isDefined("else if")) {
			sb.append(this.format(elseIf ? "else if" : "if", expr));
		} else {
			sb.append(this.format("if", expr));
		}
		this.incIndent();
		this.emitStmt(sb, stmt.get());
		this.decIndent();
		this.emitStmt(sb, this.format("end if"));
		return this.endBlock(sb);
	}

	@Override
	protected String emitWhileStmt(String expr, Supplier<String> stmt) {
		StringBuilder sb = this.beginBlock();
		this.emitLine(sb, this.s("while"), expr);
		this.incIndent();
		this.emitStmt(sb, stmt.get());
		this.decIndent();
		this.emitStmt(sb, this.s("end while"));
		return this.endBlock(sb);
	}

	@Override
	protected String emitGroup(String expr) {
		if (this.isDefined("group")) {
			return this.format("group", expr);
		}
		return "(" + expr + ")";
	}

	@Override
	protected String emitOp(String expr, String op, String expr2) {
		if (this.isDefined(op)) {
			return this.format(op, expr, expr2);
		}
		return String.format("%s %s %s", expr, op, expr2);
	}

	@Override
	protected String emitConv(String var, String expr) {
		if (this.isDefined(var)) {
			return this.format(var, expr);
		}
		String t = this.T(var);
		if (t != null && this.isDefined("cast")) {
			return this.format("cast", t, expr);
		}
		return expr;
	}

	@Override
	protected String emitNull(String name) {
		if (this.isDefined("Option")) {
			return this.s("None");
		}
		return this.s("null");
	}

	@Override
	protected String emitIsNull(String expr) {
		if (this.isDefined("Option")) {
			return this.format("Option.isNone", expr);
		}
		return super.emitIsNull(expr);
	}

	@Override
	protected String emitArrayLength(String a) {
		if ((a.endsWith("inputs") || a.endsWith("value")) && this.isDefined("Byte[].size")) {
			return this.format("Byte[].size", a);
		}
		return this.format("Array.size", a);
	}

	@Override
	protected String emitArrayIndex(String a, String index) {
		if ((a.indexOf("inputs") != -1 || a.indexOf("value") != -1) && this.isDefined("Byte[].get")) {
			return this.format("Byte[].get", a, index);
		}
		if (a.indexOf("memos") != -1 && this.isDefined("MemoList.get")) {
			return this.format("MemoList.get", a, this.emitConv("Array.start", index));
		}
		return this.format("Array.get", a, this.emitConv("Array.start", index));
	}

	@Override
	protected String emitNewArray(String type, String index) {
		if (type.startsWith("MemoEntry") && this.isDefined("MemoList")) {
			return this.format("MemoList.new", type, index);
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
	protected String emitNew(String typeName, List<String> params) {
		String value = null;
		if (this.isDefined("object")) {
			ArrayList<String> l = new ArrayList<>();
			String[] names = this.s("fields " + typeName).split(",");
			for (int i = 0; i < params.size(); i++) {
				l.add(this.format("objectparam", names[i], params.get(i)));
			}
			value = this.format("object", typeName, this.emitList("objectparams", l));
		} else {
			value = this.emitFunc(typeName, params);
		}
		if (this.isDefined("Some") && (typeName.equals("TreeLog") || typeName.equals("State"))) {
			value = this.format("Some", value);
		}
		return value;
	}

	protected String funcName(String func) {
		if (this.isDefined(func)) {
			return this.s(func);
		}
		if (this.isDefined("Osnake")) {
			return func.toLowerCase();
		}
		return func;
	}

	@Override
	protected String emitFunc(String func, List<String> params) {
		if (this.isDefined(func)) {
			String fmt = this.getSymbol(func);
			if (fmt.indexOf("%s") >= 0 || fmt.indexOf("%2$s") >= 0 || func.indexOf(".") > 0) {
				try {
					return String.format(fmt, params.toArray(new Object[params.size()]));
				} catch (Exception e) {
					System.out.println("ERROR " + func);
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		String delim = this.s("args");
		if (delim.length() == 0) {
			delim = " ";
		}
		int c = 0;
		for (String p : params) {
			if (c > 0) {
				sb.append(delim);
			}
			c++;
			sb.append(p);
		}
		return this.format("funccall", this.funcName(func), sb.toString());
	}

	@Override
	protected String emitApply(String func, List<String> params) {
		if (this.isDefined("apply")) {
			func = this.format("apply", func);
		}
		return this.emitFunc(this.funcName(func), params);
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
	protected String emitIf(String expr, String t, String expr2, String expr3) {
		if (this.isDefined("ifexpr")) {
			return this.format("ifexpr", expr, expr2, expr3);
		}
		if (t.equals("then")) {
			return expr2;
		}
		if (t.equals("else")) {
			return expr3;
		}
		String funcName = "ifexpr";
		if (this.check("ifexpr" + t)) {
			funcName = "ifexpr" + t;
		}
		return this.emitFunc(funcName, expr, this.format("lambda", "", expr2), this.format("lambda", "", expr3));
	}

	@Override
	protected String emitDispatch(String index, List<String> cases) {
		if (this.isDefined("switch")) {
			StringBuilder block = this.beginBlock();
			this.emitLine(block, this.format("switch", index));
			if (this.isDefined("default")) {
				this.incIndent();
				for (int i = 1; i < cases.size(); i++) {
					this.emitLine(block, this.format("case", i, this.emitReturn(cases.get(i))));
				}
				this.emitLine(block, this.format("default", this.emitReturn(cases.get(0))));
				this.decIndent();
				this.emitStmt(block, this.s("end switch"));

			} else {
				this.incIndent();
				for (int i = 0; i < cases.size(); i++) {
					this.emitLine(block, this.format("case", i, this.emitReturn(cases.get(i))));
				}
				this.decIndent();
				this.emitStmt(block, this.s("end switch"));
				if (this.isDefined("return")) {
					this.Return(block, this.emitFail());
				}
			}
			return this.endBlock(block);
		} else if (this.useLambda() && this.isNotIncludeMemo(cases)) {
			StringBuilder block = this.beginBlock();
			this.emitLine(block, this.format("const", this.s("Bool"), "funcMap", this.vFuncMap(cases)));
			this.emitStmt(block, this.emitReturn(
					this.emitApply(this.emitGroup(this.emitArrayIndex(this.V("funcMap"), index)), this.V("px"))));
			return this.endBlock(block);
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
		// this.makeLib("ftrue");
		// this.makeLib("ffalse");
		StringBuilder sb = new StringBuilder();
		String delim = this.s("arrays");
		if (delim.length() == 0) {
			delim = " ";
		}
		sb.append(this.s("array"));
		int c = 0;
		for (String code : cases) {
			if (c > 0) {
				sb.append(delim);
			}
			if (code.equals(this.s("false"))) {
				sb.append(this.emitFuncRef("ffalse"));
			} else if (code.equals(this.s("true"))) {
				sb.append(this.emitFuncRef("ftrue"));
			} else {
				sb.append(this.emitParserLambda(code));
			}
			c++;
		}
		sb.append(this.s("end array"));
		return sb.toString();
		// return this.getConstName(this.T("f"), "funcmap", cases.size(),
		// sb.toString());
	}

	@Override
	protected String emitAsm(String expr) {
		return this.s(expr);
	}

	@Override
	protected String vInt(int value) {
		if (this.isDefined("int")) {
			return this.format("int", String.valueOf(value));
		}
		return String.valueOf(value);
	}

	@Override
	protected String vString(String s) {
		if (s == null) {
			return this.s("null");
		}
		if (this.isDefined("String.u")) {
			StringBuilder sb = new StringBuilder();
			char quote = 0;
			if (this.isDefined("String.quote")) {
				quote = this.s("String.quote").charAt(0);
			}
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c >= 32 && c <= 126 && c != quote) {
					sb.append(c);
				} else {
					sb.append(this.format("String.u", (int) c));
				}
			}
			return this.format("String''", sb.toString());
		}
		StringBuilder sb = new StringBuilder();
		OStringUtils.formatStringLiteral(sb, '"', s, '"');
		return sb.toString();
	}

	@Override
	protected String vIndexMap(byte[] indexMap) {
		if (this.isDefined("base64")) {
			byte[] encoded = Base64.getEncoder().encode(indexMap);
			return this.getConstName(this.s("Int8"), "choice", encoded.length,
					this.format("base64", new String(encoded)));
		}
		StringBuilder sb = new StringBuilder();
		String delim = this.s("arrays");
		if (delim.length() == 0) {
			delim = " ";
		}
		sb.append(this.s("array"));
		for (int i = 0; i < indexMap.length; i++) {
			if (i > 0) {
				sb.append(delim);
			}
			if (this.isDefined("Int8''")) {
				sb.append(this.format("Int8''", indexMap[i] & 0xff));
			} else if (this.isDefined("arraypair")) {
				sb.append(this.format("arraypair", i, indexMap[i] & 0xff));
			} else {
				sb.append(indexMap[i] & 0xff);
			}
		}
		sb.append(this.s("end array"));
		return this.getConstName(this.s("Int8"), "choice", indexMap.length, sb.toString());
	}

	protected ArrayList<Symbol> symbolList = new ArrayList<>();
	protected HashMap<Symbol, Integer> symbolMap = new HashMap<>();
	protected ArrayList<byte[]> valueList = new ArrayList<>();
	protected HashMap<byte[], Integer> valueMap = new HashMap<>();

	@Override
	protected void declSymbolTables() {
		String delim = this.s("arrays");
		if (delim.length() == 0) {
			delim = " ";
		}
		if (this.symbolList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			sb.append(this.s("array"));
			if (this.isDefined("arraypair")) {
				sb.append(this.format("arraypair", c, this.vString("")));
			} else {
				sb.append(this.vString(""));
			}
			for (Symbol s : this.symbolList) {
				c++;
				sb.append(delim);
				if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", c, this.vString(s.getSymbol())));
				} else {
					sb.append(this.vString(s.getSymbol()));
				}
			}
			sb.append(delim);
			if (this.isDefined("arraypair")) {
				sb.append(this.format("arraypair", c, this.vString("err")));
			} else {
				sb.append(this.vString("err"));
			}
			sb.append(this.s("end array"));
			this.declConst(this.s("Symbol"), "nezsymbols", this.symbolList.size() + 2, sb.toString());
			this.declConst(this.s("Int"), "nezerror", -1, "" + (this.symbolList.size() + 1));
		}
		if (this.valueList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			sb.append(this.s("array"));
			if (this.isDefined("arraypair")) {
				sb.append(this.format("arraypair", c, this.rawValue(new byte[0])));
			} else {
				sb.append(this.rawValue(new byte[0]));
			}
			// sb.append(this.rawValue(new byte[0]));
			for (byte[] s : this.valueList) {
				sb.append(delim);
				c++;
				if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", c, this.rawValue(s)));
				} else {
					sb.append(this.rawValue(s));
				}
			}
			sb.append(this.s("end array"));
			this.declConst(this.T("value"), "nezvalues", this.valueList.size() + 1, sb.toString());
		}
		if (this.valueList.size() >= 0) {
			StringBuilder sb = new StringBuilder();
			int c = 0;
			sb.append(this.s("array"));
			if (this.isDefined("arraypair")) {
				sb.append(this.format("arraypair", c, this.vInt(0)));
			} else {
				sb.append(this.vInt(0));
			}
			for (byte[] s : this.valueList) {
				sb.append(delim);
				c++;
				if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", c, this.vInt(s.length)));
				} else {
					sb.append(this.vInt(s.length));
				}
			}
			sb.append(this.s("end array"));
			this.declConst(this.T("length"), "nezvaluesizes", this.valueList.size() + 1, sb.toString());
		}
	}

	protected String rawValue(byte[] buf) {
		if (this.isDefined("Byte[]''")) {
			StringBuilder sb = new StringBuilder();
			String fmt = this.s("Byte[].quote");
			byte quote = (byte) fmt.charAt(0);
			for (byte b : buf) {
				if (b >= 32 && b <= 126 && b != quote) {
					sb.append((char) b);
				} else {
					sb.append(this.format("Byte[].esc", (int) b));
				}
			}
			return this.format("Byte[]''", sb.toString());
		} else {
			String delim = this.s("arrays");
			if (delim.length() == 0) {
				delim = " ";
			}
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			int c = 0;
			for (byte b : buf) {
				if (c > 0) {
					sb.append(delim);
				}
				if (this.isDefined("Byte''")) {
					sb.append(this.format("Byte''", b & 0xff));
				} else if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", c, b & 0xff));
				} else {
					sb.append(b & 0xff);
				}
				c++;
			}
			sb.append(this.s("end array"));
			return this.getConstName(this.s("Byte"), "data", buf.length, sb.toString());
		}
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
			if (this.check("nextn")) {
				expr = this.emitFunc("nextn", this.V("px"), this.rawValue(bytes), this.vInt(bytes.length));
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

	// @Override
	// protected String vThunk(Object s) {
	// return this.s("null");
	// }

	@Override
	protected String vByteSet(ByteSet bs) {
		String delim = this.s("arrays");
		if (this.isDefined("Int32")) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));

			for (int i = 0; i < 8; i++) {
				if (i > 0) {
					sb.append(delim);
				}
				if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", i, bs.bits()[i]));
				} else {
					sb.append(bs.bits()[i]);
				}
			}
			sb.append(this.s("end array"));
			return this.getConstName(this.s("Int32"), "charset", 8, sb.toString());
		} else if (this.isDefined("bools")) {
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
			String literal = this.format("bools", sb.toString());
			return this.getConstName(this.s("Bool"), "charset", 256, literal);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(this.s("array"));
			for (int i = 0; i < 256; i++) {
				if (i > 0) {
					sb.append(delim);
				}
				if (this.isDefined("arraypair")) {
					sb.append(this.format("arraypair", i, bs.is(i) ? this.s("true") : this.s("false")));
				} else {
					sb.append(bs.is(i) ? this.s("true") : this.s("false"));
				}
			}
			sb.append(this.s("end array"));
			String literal = sb.toString();
			return this.getConstName(this.s("Bool"), "charset", 256, literal);
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
	public String Const(String name) {
		return this.format("constname", name);
	}

	@Override
	protected String emitUnsigned(String expr) {
		return this.emitConv("Byte->Int", expr);
	}

	@Override
	protected void emitIfElse(StringBuilder block, String expr, Supplier<String> stmt, Supplier<String> stmt2) {
		StringBuilder sb = this.beginBlock();
		sb.append(this.format("if", expr));
		this.incIndent();
		this.emitStmt(sb, stmt.get());
		this.decIndent();
		sb.append(this.format("else"));
		this.incIndent();
		this.emitStmt(sb, stmt2.get());
		this.decIndent();
		this.emitStmt(sb, this.format("end if"));
		this.emitStmt(block, this.endBlock(sb));
	}

	@Override
	protected void emitAssignIf(StringBuilder block, String name, String pe, String pe2, String pe3) {
		if (this.isDefined("else")) {
			StringBuilder sb = this.beginBlock();
			sb.append(this.format("if", pe));
			this.incIndent();
			this.Assign(sb, name, pe2);
			this.decIndent();
			sb.append(this.format("else"));
			this.incIndent();
			this.Assign(sb, name, pe3);
			this.decIndent();
			this.emitStmt(sb, this.format("end if"));
			this.emitStmt(block, this.endBlock(sb));
		} else {
			this.Assign(block, name, this.emitIf(pe, "", pe2, pe3));
		}
	}

}
