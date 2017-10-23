package blue.origami.transpiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blue.origami.asm.AsmMapper;
import blue.origami.nez.ast.Source;
import blue.origami.parser.Parser;
import blue.origami.parser.ParserSource;
import blue.origami.parser.ParserCode.ParserErrorException;
import blue.origami.parser.peg.Grammar;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.DataExpr;
import blue.origami.transpiler.rule.DataType;
import blue.origami.transpiler.rule.DictExpr;
import blue.origami.transpiler.rule.ListExpr;
import blue.origami.transpiler.rule.RangeExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.transpiler.rule.UnaryExpr;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.OStrings;

public class Transpiler extends TEnv {
	final OOption options;
	private final CodeMapLoader loader;
	private final CodeMapper generator;

	boolean isFriendly = true;

	public Transpiler(Grammar grammar, Parser p, String target, OOption options) {
		super(null);
		this.loader = new CodeMapLoader(this, target);
		this.options = options;
		this.generator = target.equals("jvm") ? new AsmMapper(this) : new SourceMapper(this);
		this.initEnv(grammar, p);
		this.loader.loadCodeMap("konoha5.codemap");
		this.generator.init();
	}

	public Transpiler(Grammar grammar, String target, OOption options) {
		this(grammar, grammar.newParser(), target, options);
	}

	public Transpiler(Grammar g, String target) {
		this(g, target, null);
	}

	private void initEnv(Grammar grammar, Parser p) {
		this.add(Parser.class, p);
		this.add(Grammar.class, grammar);
		// rule
		this.add("Source", new SourceUnit());
		this.add("AddExpr", new BinaryExpr("+"));
		this.add("SubExpr", new BinaryExpr("-"));
		this.add("CatExpr", new BinaryExpr("++"));
		this.add("PowExpr", new BinaryExpr("^"));
		this.add("MulExpr", new BinaryExpr("*"));
		this.add("DivExpr", new BinaryExpr("/"));
		this.add("ModExpr", new BinaryExpr("%"));
		this.add("EqExpr", new BinaryExpr("=="));
		this.add("NeExpr", new BinaryExpr("!="));
		this.add("LtExpr", new BinaryExpr("<"));
		this.add("LteExpr", new BinaryExpr("<="));
		this.add("GtExpr", new BinaryExpr(">"));
		this.add("GteExpr", new BinaryExpr(">="));
		this.add("LAndExpr", new BinaryExpr("&&"));
		this.add("LOrExpr", new BinaryExpr("||"));
		this.add("AndExpr", new BinaryExpr("&&"));
		this.add("OrExpr", new BinaryExpr("||"));
		this.add("XorExpr", new BinaryExpr("^^"));
		this.add("LShiftExpr", new BinaryExpr("<<"));
		this.add("RShiftExpr", new BinaryExpr(">>"));

		this.add("BindExpr", new BinaryExpr("flatMap"));
		this.add("ConsExpr", new BinaryExpr("cons"));
		this.add("OrElseExpr", new BinaryExpr("!?"));

		this.add("NotExpr", new UnaryExpr("!"));
		this.add("MinusExpr", new UnaryExpr("-"));
		this.add("PlusExpr", new UnaryExpr("+"));
		this.add("CmplExpr", new UnaryExpr("~"));

		this.add("DataListExpr", new ListExpr(true));
		this.add("RangeUntilExpr", new RangeExpr(false));
		this.add("DataDictExpr", new DictExpr(true));
		this.add("RecordExpr", new DataExpr(false));

		this.add("RecordType", new DataType(false));
		this.add("DataType", new DataType(true));

		// type
		// this.add("?", Ty.tUntyped0);
		this.add("Bool", Ty.tBool);
		this.add("Int", Ty.tInt);
		this.add("Float", Ty.tFloat);
		this.add("String", Ty.tString);
		this.add("a", VarDomain.var(0));
		this.add("b", VarDomain.var(1));
		this.add("c", VarDomain.var(2));
		this.add("d", VarDomain.var(3));
		this.add("e", VarDomain.var(4));
		this.add("f", VarDomain.var(5));
		// this.add("Data", Ty.tData());
		this.addNameDecl(this, "i,j,k,m,n", Ty.tInt);
		this.addNameDecl(this, "x,y,z,w", Ty.tFloat);
		this.addNameDecl(this, "s,t,u,name", Ty.tString);
	}

	public boolean loadScriptFile(String path) throws IOException {
		return this.loadScriptFile(ParserSource.newFileSource(path, null));
	}

	public void eval(String script) {
		this.eval("<unknown>", 1, script);
	}

	public void eval(String source, int line, String script) {
		this.loadScriptFile(ParserSource.newStringSource(source, line, script));
	}

	public boolean loadScriptFile(Source sc) {
		try {
			this.emitCode(this, sc);
			return true;
		} catch (Throwable e) {
			this.showThrowable(e);
			return false;
		}
	}

	public void testScriptFile(Source sc) throws Throwable {
		this.emitCode(this, sc);
	}

	public void verboseError(String msg, Runnable p) {
		OConsole.beginColor(OConsole.Red);
		OConsole.print("[" + msg + "] ");
		p.run();
		OConsole.endColor();
	}

	void showThrowable(Throwable e) {
		if (e instanceof Error) {
			OConsole.exit(1, e);
			return;
		}
		if (e instanceof ParserErrorException) {
			this.verboseError(TFmt.ParserError.toString(), () -> {
				OConsole.println(e);
			});
			return;
		}
		if (e instanceof InvocationTargetException) {
			this.showThrowable(((InvocationTargetException) e).getTargetException());
			return;
		}
		if (e instanceof ErrorCode) {
			this.verboseError("Error", () -> {
				OConsole.println(((ErrorCode) e).getLog());
			});
		} else {
			this.verboseError("RuntimeException", () -> {
				e.printStackTrace();
			});
		}
	}

	void emitCode(TEnv env, Source sc) throws Throwable {
		Parser p = env.get(Parser.class);
		AST t = (AST) p.parse(sc, 0, AST.TreeFunc, AST.TreeFunc);
		ODebug.showBlue(TFmt.Syntax_Tree, () -> {
			OConsole.println(t);
		});
		this.generator.setup();
		Code code = env.parseCode(env, t).asType(env, Ty.tUntyped());
		if (code.getType().isAmbigous()) {
			code = new ErrorCode(code, TFmt.ambiguous_type__S, code.getType());
		}
		if (code.showError(env)) {
			return;
		}
		this.generator.emitTopLevel(env, code);
		Object result = this.generator.wrapUp();

		if (code.getType() != Ty.tVoid) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(OConsole.t(code.getType().finalTy().toString()));
			sb.append(") ");
			OConsole.beginBold(sb);
			OStrings.appendQuoted(sb, result);
			OConsole.endBold(sb);
			OConsole.println(sb.toString());
		}
	}

	public Code testCode(String text) throws Throwable {
		Source sc = ParserSource.newStringSource("<test>", 1, text);
		Parser p = this.get(Parser.class);
		AST t = (AST) p.parse(sc, 0, AST.TreeFunc, AST.TreeFunc);
		this.generator.setup();
		return this.parseCode(this, t).asType(this, Ty.tUntyped());
	}

	public Ty testType(String s) throws Throwable {
		return this.testCode(s).getType().finalTy();
	}

	public Object testEval(String s) throws Throwable {
		Code code = this.testCode(s);
		this.generator.emitTopLevel(this, code);
		return this.generator.wrapUp();
	}

	// Buffering

	public void addFunction(TEnv env, String name, FuncMap f) {
		this.add(name, f);
		if (f.isPublic()) {
			this.generator.addFunction(name, f);
		}
	}

	public void addExample(String name, AST tree) {
		if (tree.is("MultiExpr")) {
			for (AST t : tree) {
				this.generator.addExample(name, t);
			}
		} else {
			this.generator.addExample(name, tree);
		}
	}

	// ConstDecl

	int functionId = 0;

	public CodeMap defineConst(boolean isPublic, String name, Ty type, Code expr) {
		TEnv env = this.newEnv();
		String lname = isPublic ? name : this.getLocalName(name);
		CodeMap tp = this.generator.newConstMap(env, lname, type);
		this.add(name, tp);
		this.generator.defineConst(this, isPublic, lname, type, expr);
		return tp;
	}

	// FuncDecl

	public CodeMap newCodeMap(String name, Ty returnType, Ty... paramTypes) {
		final String lname = this.generator.safeName(name);
		return this.generator.newCodeMap(this, name, lname, returnType, paramTypes);
	}

	public CodeMap defineFunction(String name, AST[] paramNames, Ty[] paramTypes, Ty returnType, Code body) {
		final TEnv env = this.newEnv();
		final String lname = this.generator.safeName(name);
		final CodeMap tp = this.generator.newCodeMap(env, name, lname, returnType, paramTypes);
		this.add(name, tp);
		FunctionContext fcx = new FunctionContext(null);
		FuncUnit fu = FuncUnit.wrap(null, paramNames, tp);
		Code code = fu.typeCheck(env, fcx, null, body);
		this.generator.defineFunction(this, false, lname, AST.names(paramNames), tp.getParamTypes(), tp.getReturnType(),
				code);
		return tp;
	}

	public CodeMap defineFunction(boolean isPublic, AST aname, int seq, AST[] paramNames, Ty[] paramTypes,
			Ty returnType, VarDomain dom, Code code0) {
		final String name = aname.getString();
		final String lname = isPublic ? name : this.getLocalName(name);
		final CodeMap tp = this.generator.newCodeMap(this, name, lname, returnType, paramTypes);
		this.add(name, tp);
		FunctionContext fcx = new FunctionContext(null);
		FuncUnit fu = FuncUnit.wrap(aname, paramNames, tp);

		Code code = fu.typeCheck(this, fcx, dom, code0);
		if (fu.getReturnType().isUnion()) {
			Ty ret = code.getType();
			// ODebug.trace("UNION %s => %s", fu.getReturnType(), ret);
			fu.setReturnType(ret);
			if (ret.isUnion()) {
				code = new ErrorCode(code.getSource(), TFmt.ambiguous_type__S, fu.getReturnType());
			}
		}
		if (code.showError(this)) {
			return tp;
		}
		this.generator.defineFunction(this, isPublic, lname, AST.names(paramNames), tp.getParamTypes(),
				tp.getReturnType(), code);
		return tp;
	}

	public CodeMap defineFunction(boolean isPublic, AST name, int seq, AST[] paramNames, Ty[] paramTypes, Ty returnType,
			VarDomain dom, AST body) {
		return this.defineFunction(isPublic, name, seq, paramNames, paramTypes, returnType, dom,
				this.parseCode(this, body));
	}

	// public CodeMap newCodeMap(String name, Ty ret, Ty[] pats) {
	// final String lname = this.getLocalName(name);
	// return this.generator.newCodeMap(this, name, lname, ret, pats);
	// }

	private String getLocalName(String name) {
		String prefix = "f" + (this.functionId++); // this.getSymbol(name);
		return prefix + NameHint.safeName(name);
	}

	private boolean shellMode = false;

	public boolean isShellMode() {
		return this.shellMode;
	}

	public void setShellMode(boolean shellMode) {
		this.shellMode = shellMode;
	}

}
