package blue.origami.transpiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.ast.Tree;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserCode.ParserErrorException;
import blue.origami.nez.parser.ParserSource;
import blue.origami.nez.peg.Grammar;
import blue.origami.transpiler.asm.AsmGenerator;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.DataExpr;
import blue.origami.transpiler.rule.DataType;
import blue.origami.transpiler.rule.DictExpr;
import blue.origami.transpiler.rule.ListExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.transpiler.rule.UnaryExpr;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.CodeTree;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.StringCombinator;

public class Transpiler extends TEnv {
	final OOption options;
	private final String target;
	private final Generator generator;

	boolean isFriendly = true;

	public Transpiler(Grammar grammar, Parser p, String target, OOption options) {
		super(null);
		this.target = "/blue/origami/konoha5/" + target + "/";
		this.options = options;
		this.generator = target.equals("jvm") ? new AsmGenerator(this) : new SourceGenerator(this);
		this.initEnv(grammar, p);
		this.loadLibrary("init.kh");
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
		this.add("MulExpr", new BinaryExpr("*"));
		this.add("DivExpr", new BinaryExpr("/"));
		this.add("ModExpr", new BinaryExpr("%"));
		this.add("EqExpr", new BinaryExpr("=="));
		this.add("NeExpr", new BinaryExpr("!="));
		this.add("LtExpr", new BinaryExpr("<"));
		this.add("LteExpr", new BinaryExpr("<="));
		this.add("GtExpr", new BinaryExpr(">"));
		this.add("GteExpr", new BinaryExpr(">="));
		this.add("AndExpr", new BinaryExpr("&&"));
		this.add("OrExpr", new BinaryExpr("||"));
		this.add("XorExpr", new BinaryExpr("^^"));
		this.add("LShiftExpr", new BinaryExpr("<<"));
		this.add("RShiftExpr", new BinaryExpr(">>"));

		this.add("NotExpr", new UnaryExpr("!"));
		this.add("MinusExpr", new UnaryExpr("-"));
		this.add("PlusExpr", new UnaryExpr("+"));
		this.add("CmplExpr", new UnaryExpr("~"));
		this.add("DataListExpr", new ListExpr(true));
		this.add("DataDictExpr", new DictExpr(true));
		this.add("RecordExpr", new DataExpr(false));

		this.add("RecordType", new DataType(false));
		this.add("MutableRecordType", new DataType(true));

		// type
		this.add("?", Ty.tUntyped0);
		this.add("Bool", Ty.tBool);
		this.add("Int", Ty.tInt);
		this.add("Float", Ty.tFloat);
		this.add("String", Ty.tString);
		this.add("Data", Ty.tData());
		this.addNameDecl(this, "i,j,k,m,n", Ty.tInt);
		this.addNameDecl(this, "x,y,z,w", Ty.tFloat);
		this.addNameDecl(this, "s,t,u,name", Ty.tString);
	}

	private void loadLibrary(String file) {
		try {
			String path = this.target + file;
			File f = new File(path);
			InputStream s = f.isFile() ? new FileInputStream(path) : Transpiler.class.getResourceAsStream(path);
			if (s == null) {
				System.out.println("FIXME: unsupported " + path);
			}
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
					int loc = line.indexOf(" =");
					if (loc <= 0) {
						continue;
					}
					name = line.substring(0, loc).trim();
					String value = line.substring(loc + 2).trim();
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

	boolean isVerbose = false;

	public void setVerbose(boolean debug) {
		this.isVerbose = debug;
		this.generator.setVerbose(debug);
	}

	public void verbose(String msg, Runnable p) {
		if (this.isVerbose) {
			OConsole.beginColor(OConsole.Blue);
			OConsole.print("[" + msg + "] ");
			p.run();
			OConsole.endColor();
		}
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
			this.verboseError("ParserError", () -> {
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
		CodeTree defaultTree = new CodeTree();
		Tree<?> t = (CodeTree) p.parse(sc, 0, defaultTree, defaultTree);
		this.verbose("Tree", () -> {
			OConsole.println(t);
		});
		this.generator.setup();
		Code code = env.parseCode(env, t).asType(env, Ty.tUntyped());
		this.generator.emit(env, code);
		Object result = this.generator.wrapUp();
		if (code.getType() != Ty.tVoid) {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			StringCombinator.append(sb, code.getType());
			sb.append(") ");
			OConsole.beginBold(sb);
			StringCombinator.appendQuoted(sb, result);
			OConsole.endBold(sb);
			OConsole.println(sb.toString());
		}
	}

	// parsedName
	// HashSet<String> nameMap = new HashSet<>();

	public void addParsedName1(String name) {
		// TODO Auto-generated method stub

	}

	// Buffering

	public void addFunction(String name, TFunction f) {
		this.add(name, f);
		this.generator.addFunction(name, f);
	}

	public void addExample(String name, Tree<?> tree) {
		if (tree.is(Symbol.unique("MultiExpr"))) {
			for (Tree<?> t : tree) {
				this.generator.addExample(name, t);
			}
		} else {
			this.generator.addExample(name, tree);
		}
	}

	// ConstDecl

	int functionId = 0;

	public Template defineConst(boolean isPublic, String name, Ty type, Code expr) {
		TEnv env = this.newEnv();
		String lname = isPublic ? name : this.getLocalName(name);
		CodeTemplate tp = this.generator.newConstTemplate(env, lname, type);
		this.add(name, tp);
		this.generator.defineConst(this, isPublic, lname, type, expr);
		return tp;
	}

	// FuncDecl

	public Template defineFunction(String name, String[] paramNames, Ty[] paramTypes, Ty returnType, Code body) {
		final TEnv env = this.newEnv();
		final String lname = this.generator.safeName(name);
		final CodeTemplate tp = this.generator.newFuncTemplate(env, lname, returnType, paramTypes);
		this.add(name, tp);
		FunctionContext fcx = new FunctionContext();
		env.add(FunctionContext.class, fcx);
		for (int i = 0; i < paramNames.length; i++) {
			env.add(paramNames[i], fcx.newVariable(paramNames[i], paramTypes[i]));
		}
		Code code = env.catchCode(() -> body.asType(env, returnType));
		tp.nomAll();
		this.generator.defineFunction(this, false, lname, paramNames, tp.getParamTypes(), tp.getReturnType(), code);
		return tp;
	}

	public Template defineFunction(boolean isPublic, String name, VarDomain dom, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Tree<?> body) {
		final TEnv env = this.newEnv();
		final String lname = isPublic ? name : this.getLocalName(name);
		final CodeTemplate tp = this.generator.newFuncTemplate(env, lname, returnType, paramTypes);
		this.add(name, tp);
		FunctionContext fcx = new FunctionContext();
		env.add(FunctionContext.class, fcx);
		for (int i = 0; i < paramNames.length; i++) {
			env.add(paramNames[i], fcx.newVariable(paramNames[i], paramTypes[i]));
		}
		Code code0 = env.parseCode(env, body);
		Code code = env.catchCode(() -> code0.asType(env, returnType));
		// int untyped = code.countUntyped(0);
		// if (untyped > 0) {
		// ODebug.trace("untyped node=%d", untyped);
		// }
		ODebug.trace("Typed Error=%s %s", code.hasErrorCode(), tp);
		if (dom != null) {
			dom.check();
		}
		tp.nomAll();
		this.verbose("Typed", () -> {
			OConsole.println("%s %s", lname, tp.getFuncType());
		});
		assert (!returnType.isUntyped());
		this.generator.defineFunction(this, isPublic, lname, paramNames, tp.getParamTypes(), tp.getReturnType(), code);
		return tp;
	}

	private String getLocalName(String name) {
		String prefix = "f" + (this.functionId++); // this.getSymbol(name);
		return prefix + NameHint.safeName(name);
	}

}
