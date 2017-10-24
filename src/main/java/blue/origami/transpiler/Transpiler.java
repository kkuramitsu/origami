package blue.origami.transpiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blue.origami.asm.AsmMapper;
import blue.origami.common.OConsole;
import blue.origami.common.ODebug;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.common.OSource;
import blue.origami.common.OStrings;
import blue.origami.main.MainOption;
import blue.origami.parser.Parser;
import blue.origami.parser.ParserCode.ParserErrorException;
import blue.origami.parser.ParserSource;
import blue.origami.parser.peg.Grammar;
import blue.origami.parser.peg.SourceGrammar;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.target.SourceMapper;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public class Transpiler extends Env implements OFactory<Transpiler> {
	private CodeMapLoader loader;
	private CodeMapper generator;

	boolean isFriendly = true;

	public Transpiler(Grammar g, Parser p) {
		super(null);
		this.initEnv(g, p, new Language());
	}

	private void initEnv(Grammar g, Parser p, Language lang) {
		this.loader = new CodeMapLoader(this, this.getTargetName());
		this.generator = this.getCodeMapper();
		this.add(Grammar.class, g);
		this.add(Parser.class, p);
		lang.init(this);
		this.loader.loadCodeMap("konoha5.codemap");
		this.generator.init();

	}

	public Transpiler() {
		super(null);
	}

	@Override
	public final Class<?> keyClass() {
		return Transpiler.class;
	}

	@Override
	public final Transpiler clone() {
		return this.newClone();
	}

	@Override
	public void init(OOption options) {
		try {
			String file = options.stringValue(MainOption.GrammarFile, "konoha5.opeg");
			Grammar g = SourceGrammar.loadFile(file, options.stringList(MainOption.GrammarPath));
			Parser p = g.newParser(options);
			this.initEnv(g, p, options.newInstance(Language.class));
		} catch (IOException e) {
			OConsole.exit(1, e);
		}

	}

	public String getTargetName() {
		Class<?> c = this.getClass();
		return (c == Transpiler.class) ? "jvm" : c.getSimpleName();
	}

	public CodeMapper getCodeMapper() {
		Class<?> c = this.getClass();
		return (c == Transpiler.class) ? new AsmMapper(this) : new SourceMapper(this);
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

	public boolean loadScriptFile(OSource sc) {
		try {
			this.emitCode(this, sc);
			return true;
		} catch (Throwable e) {
			this.showThrowable(e);
			return false;
		}
	}

	public void testScriptFile(OSource sc) throws Throwable {
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

	void emitCode(Env env, OSource sc) throws Throwable {
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
		OSource sc = ParserSource.newStringSource("<test>", 1, text);
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

	public void addFunction(Env env, String name, FuncMap f) {
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
		Env env = this.newEnv();
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
		final Env env = this.newEnv();
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
