package blue.origami.transpiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import blue.origami.nez.ast.Source;
import blue.origami.nez.ast.Tree;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserSource;
import blue.origami.nez.peg.Grammar;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.transpiler.rule.UnaryExpr;
import blue.origami.util.OConsole;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.OTree;

public class Transpiler extends TEnv {
	private final OOption options;
	private final String target;
	private final TGenerator generator;

	public Transpiler(Grammar grammar, String target, OOption options) {
		super(null);
		this.target = "/blue/origami/konoha5/" + target + "/";
		this.options = options;
		this.generator = new TGenerator();
		this.initEnv(grammar);
		this.loadLibrary("init.kh");

	}

	private void initEnv(Grammar grammar) {
		Parser p = grammar.newParser();
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
		this.add("AndExpr", new BinaryExpr("&"));
		this.add("OrExpr", new BinaryExpr("|"));
		this.add("XorExpr", new BinaryExpr("^"));
		this.add("LShiftExpr", new BinaryExpr("<<"));
		this.add("RShiftExpr", new BinaryExpr(">>"));

		this.add("NotExpr", new UnaryExpr("!"));
		this.add("MinusExpr", new UnaryExpr("-"));
		this.add("PlusExpr", new UnaryExpr("+"));

		// type
		this.add("?", TType.tUntyped);
		this.add("Bool", TType.tBool);
		this.add("Int", TType.tInt);
		this.add("Float", TType.tFloat);
		this.add("String", TType.tString);
		this.add("Data", TType.tData);
		this.addTypeHint(this, "i,j,m,n", TType.tInt);
		this.addTypeHint(this, "x,y,z,w", TType.tFloat);
		this.addTypeHint(this, "s,t,u", TType.tString);
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

	public void shell(String source, int line, String script) {
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

	void showThrowable(Throwable e) {
		if (e instanceof InvocationTargetException) {
			this.showThrowable(((InvocationTargetException) e).getTargetException());
			return;
		}
		if (e instanceof TErrorCode) {
			OConsole.println(OConsole.bold("catching TErrorCode: "));
			OConsole.beginColor(OConsole.Red);
			OConsole.println(((TErrorCode) e).getLog());
			OConsole.endColor();
		} else {
			OConsole.println(OConsole.bold("Runtime Exception: " + e));
			OConsole.beginColor(OConsole.Yellow);
			e.printStackTrace();
			OConsole.endColor();
		}
	}

	void emitCode(TEnv env, Source sc) throws Throwable {
		Parser p = env.get(Parser.class);
		OTree defaultTree = new OTree();
		Tree<?> t = (OTree) p.parse(sc, 0, defaultTree, defaultTree);
		OConsole.beginColor(OConsole.Blue);
		OConsole.println(t);
		OConsole.endColor();
		this.generator.setup();
		this.generator.generateExpression(env, t);
		this.generator.wrapUp();
	}

	// ConstDecl

	int functionId = 0;

	public Template defineConst(boolean isPublic, String name, TType type, TCode expr) {
		TEnv env = this.newEnv();
		String lname = isPublic ? name : this.getLocalName(name);
		TCodeTemplate tp = this.newTemplate(env, lname, type);
		this.add(name, tp);
		this.generator.defineConst(this, isPublic, lname, type, expr);
		return tp;
	}

	// FuncDecl

	public Template defineFunction(boolean isPublic, String name, String[] paramNames, TType[] paramTypes,
			TType returnType, Tree<?> body) {
		final TEnv env = this.newEnv();
		final String lname = isPublic ? name : this.getLocalName(name);
		final boolean isUntyped = returnType.isUntyped();
		final TCodeTemplate tp = this.newTemplate(env, lname, returnType, paramTypes);
		this.add(name, tp);

		TFunctionContext fcx = new TFunctionContext();
		env.add(TFunctionContext.class, fcx);
		for (int i = 0; i < paramNames.length; i++) {
			env.add(paramNames[i], fcx.newVariable(paramNames[i], paramTypes[i]));
		}
		TCode code = env.typeTree(env, body);
		if (isUntyped) {
			TType ret = code.getType();
			if (ret.isUntyped()) {
				ODebug.trace("ERROR still untyped %s", tp);
			}
			returnType.accept(code);
			// ODebug.trace("typed %s", tp);
		} else {
			code = code.asType(env, returnType);
		}
		this.generator.defineFunction(this, isPublic, lname, paramNames, paramTypes, returnType, code);
		return tp;
	}

	private String getLocalName(String name) {
		String prefix = "f" + (this.functionId++); // this.getSymbol(name);
		return prefix + name;
	}

	private TCodeTemplate newTemplate(TEnv env, String lname, TType returnType) {
		String template = env.format("constname", "name", "%s", lname);
		return new TConstTemplate(lname, returnType, template);
	}

	private TCodeTemplate newTemplate(TEnv env, String lname, TType returnType, TType... paramTypes) {
		String param = "";
		if (paramTypes.length > 0) {
			String delim = env.getSymbolOrElse(",", ",");
			StringBuilder sb = new StringBuilder();
			sb.append("%s");
			for (int i = 1; i < paramTypes.length; i++) {
				sb.append(delim);
				sb.append("%s");
			}
			param = sb.toString();
		}
		String template = env.format("funccall", "%s(%s)", lname, param);
		return new TCodeTemplate(lname, returnType, paramTypes, template);
	}

}
