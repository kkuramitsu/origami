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
import blue.origami.transpiler.rule.AddExpr;
import blue.origami.transpiler.rule.BinaryExpr;
import blue.origami.transpiler.rule.IntExpr;
import blue.origami.transpiler.rule.SourceUnit;
import blue.origami.util.OConsole;
import blue.origami.util.OLog;
import blue.origami.util.OOption;
import blue.origami.util.OTree;

public class Transpiler extends TEnv {
	private final OOption options;
	private final String target;

	public Transpiler(Grammar grammar, String target, OOption options) {
		super(null);
		this.target = "/blue/origami/konoha5/" + target + "/";
		this.options = options;
		this.initEnv(grammar);
		this.loadLibrary("init.kh");
	}

	private void initEnv(Grammar grammar) {
		Parser p = grammar.newParser();
		this.add(Parser.class, p);
		this.add(Grammar.class, grammar);
		// rule
		this.add("Source", new SourceUnit());
		this.add("IntExpr", new IntExpr());
		this.add("AddExpr", new AddExpr());
		this.add("MulExpr", new BinaryExpr("*"));
		// type
		this.add("?", TType.tUntyped);
		this.add("Bool", TType.tBool);
		this.add("Int", TType.tInt);
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
		TCode code = env.typeTree(env, t);
		code.emitCode(env, new SourceSection());
	}

}

class SourceSection implements TCodeSection {

	StringBuilder sb = new StringBuilder();
	int indent = 0;

	public void incIndent() {
		this.indent++;
	}

	public void decIndent() {
		assert (this.indent > 0);
		this.indent--;
	}

	public String Indent(String tab, String stmt) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append(tab);
		}
		sb.append(stmt);
		return sb.toString();
	}

	@Override
	public void push(String t) {
		System.out.print(t);
		this.sb.append(t);
	}

	@Override
	public void push(TCode t) {
		// TODO Auto-generated method stub
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	@Override
	public void pushLog(OLog log) {
		System.out.println(log);
	}

}
