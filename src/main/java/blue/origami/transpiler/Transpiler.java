package blue.origami.transpiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blue.origami.Version;
import blue.origami.asm.AsmMapper;
import blue.origami.common.OConsole;
import blue.origami.common.ODebug;
import blue.origami.common.OFactory;
import blue.origami.common.OOption;
import blue.origami.parser.ParserCode.ParserErrorException;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.rule.CodeMapDecl;
import blue.origami.transpiler.target.SourceMapper;
import blue.origami.transpiler.target.SourceTypeMapper;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;
import origami.main.Oparse;
import origami.nez2.OStrings;
import origami.nez2.PEG;
import origami.nez2.ParseTree;
import origami.nez2.Parser;
import origami.nez2.Token;

public class Transpiler extends Env implements OFactory<Transpiler> {
	private CodeMapper codeMapper;
	boolean isFriendly = true;

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
			Language lang = options.newInstance(Language.class);
			String file = Oparse.pegFile(options, lang.getLangName() + ".opeg");
			PEG peg = new PEG();
			peg.load(file);
			Parser p = peg.getParser();
			this.initMe(peg, p, lang);
		} catch (IOException e) {
			OConsole.exit(1, e);
		}
	}

	public String getTargetName() {
		Class<?> c = this.getClass();
		return (c == Transpiler.class) ? "jvm" : c.getSimpleName();
	}

	public Transpiler initMe(PEG g, Parser p, Language lang) {
		this.lang = lang;
		this.add(PEG.class, g);
		this.add(Parser.class, p);
		this.lang.initMe(this);
		this.codeMapper = this.getCodeMapper();
		this.codeMapper.init();
		this.loadCodeMap(lang.getLangName() + ".codemap");
		return this;
	}

	public CodeMapper getCodeMapper() {
		Class<?> c = this.getClass();
		return (c == Transpiler.class) ? new AsmMapper(this) : new SourceMapper(this, new SourceTypeMapper(this));
	}

	private void loadCodeMap(String file) {
		final String target = this.getTargetName();
		final String base = Version.ResourcePath + "/codemap/" + target + "/";
		final String common = base.replace(target, "common");
		final String defaul = base.replace(target, "default");
		final CodeMapDecl decl = new CodeMapDecl();
		try {
			PEG g = new PEG();
			g.load(Version.ResourcePath + "/grammar/chibi.opeg");
			final Parser parser = g.getParser("CodeFile");
			try {
				this.load(parser, decl, common + file, false);
			} catch (Throwable e) {
			}
			try {
				this.load(parser, decl, base + file, false);
			} catch (Throwable e) {
				OConsole.exit(1, e);
			}
			try {
				this.load(parser, decl, defaul + file, true);
			} catch (Throwable e) {
			}
		} catch (IOException e) {
			OConsole.exit(1, e);
		}
	}

	private void load(Parser parser, CodeMapDecl decl, String path, boolean isDefault) throws Throwable {
		ParseTree t = parser.parseFile(path);
		decl.parseCodeMap(this, t);
	}

	public String getPath(String file) {
		final String target = this.getTargetName();
		return Version.ResourcePath + "/codemap/" + target + "/" + file;
	}

	public boolean loadScriptFile(String path) throws IOException {
		return this.loadScriptFile(Token.newFile(path));
	}

	public void eval(String script) {
		this.eval("<unknown>", 1, script);
	}

	public void eval(String source, int line, String script) {
		this.loadScriptFile(Token.newSource(source, script));
	}

	public boolean loadScriptFile(Token s) {
		try {
			this.emitCode(this, s);
			return true;
		} catch (Throwable e) {
			this.showThrowable(e);
			return false;
		}
	}

	public void testScriptFile(Token s) throws Throwable {
		this.emitCode(this, s);
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

	void emitCode(Env env0, Token sc) throws Throwable {
		Parser p = env0.get(Parser.class);
		ParseTree t = p.parse(sc);
		ODebug.showBlue(TFmt.Syntax_Tree, () -> {
			OConsole.println(t);
		});
		this.codeMapper.setup();
		FuncEnv env = env0.newFuncEnv(); //
		Code code = env.parseCode(env, t).asType(env, Ty.tVar(null));
		if (code.getType().isAmbigous()) {
			code = new ErrorCode(code, TFmt.ambiguous_type__S, code.getType());
		}
		if (code.showError(env)) {
			return;
		}
		this.codeMapper.emitTopLevel(env, code);
		Object result = this.codeMapper.wrapUp();
		if (this.codeMapper.isExecutable() && code.getType() != Ty.tVoid) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(OConsole.t(code.getType().memoed().toString()));
			sb.append(") ");
			OConsole.beginBold(sb);
			OStrings.appendQuoted(sb, result);
			OConsole.endBold(sb);
			OConsole.println(sb.toString());
		}
	}

	public Code testCode(String text) throws Throwable {
		Token s = Token.newSource("<test>", text);
		Parser p = this.get(Parser.class);
		ParseTree t = p.parse(s);
		this.codeMapper.setup();
		FuncEnv env = this.newFuncEnv(); //
		return this.parseCode(env, t).asType(env, Ty.tVar(null));
	}

	public Ty testType(String s) throws Throwable {
		return VarDomain.eliminateVar(this.testCode(s).getType().memoed());
	}

	public Object testEval(String s) throws Throwable {
		Code code = this.testCode(s);
		this.codeMapper.emitTopLevel(this, code);
		return this.codeMapper.wrapUp();
	}

	// Buffering

	public void addFunction(Env env, String name, FuncMap f) {
		this.add(name, f);
		if (f.isPublic()) {
			this.codeMapper.addFunction(name, f);
		}
	}

	public void addExample(String name, ParseTree tree) {
		if (tree.is("MultiExpr")) {
			for (ParseTree t : tree.asArray()) {
				this.codeMapper.addExample(name, t);
			}
		} else {
			this.codeMapper.addExample(name, tree);
		}
	}

	// ConstDecl

	int functionId = 1000;

	public CodeMap defineConst(boolean isPublic, String name, Ty type, Code expr) {
		Env env = this.newEnv();
		String lname = isPublic ? name : this.getLocalName(name);
		CodeMap tp = this.codeMapper.newConstMap(env, lname, type);
		this.add(name, tp);
		this.codeMapper.defineConst(this, isPublic, lname, type, expr);
		return tp;
	}

	private String getLocalName(String name) {
		String prefix = "v" + (this.functionId++); // this.getSymbol(name);
		return prefix + NameHint.safeName(name);
	}

	// FuncDecl

	public CodeMap newCodeMap(String name, Ty returnType, Ty... paramTypes) {
		final String lname = this.codeMapper.safeName(name);
		return this.codeMapper.newCodeMap(this, name, lname, returnType, paramTypes);
	}

	public CodeMap defineFunction2(boolean isPublic, String name, String nameId, Token[] paramNames, Ty[] paramTypes,
			Ty returnType, Code body) {
		final CodeMap cmap = this.codeMapper.newCodeMap(this, name, nameId, returnType, paramTypes);
		this.addCodeMap(name, cmap);
		final FuncEnv env = this.newFuncEnv(nameId, paramNames, paramTypes, returnType);
		Code code = env.typeCheck(body);
		if (code.showError(this)) {
			return cmap; // FIXME
		}
		this.codeMapper.defineFunction(this, isPublic, nameId, NameHint.names(paramNames), cmap.getParamTypes(),
				cmap.getReturnType(), code);
		return cmap;
	}

	private boolean shellMode = false;

	public boolean isShellMode() {
		return this.shellMode;
	}

	public void setShellMode(boolean shellMode) {
		this.shellMode = shellMode;
	}

	public void defineSyntax(String key, String value) {
		this.codeMapper.defineSyntax(key, value);
	}

}
