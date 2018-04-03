package blue.origami.transpiler;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.target.SyntaxMapper;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public abstract class CodeMapper implements CodeBuilder {

	protected final Transpiler transpiler;
	protected final SyntaxMapper syntax;

	protected CodeMapper(Transpiler transpiler, SyntaxMapper syntax) {
		this.transpiler = transpiler;
		this.syntax = syntax;
	}

	public void defineSyntax(String key, String value) {
		this.syntax.defineSyntax(key, value);
	}

	protected ArrayList<ParseTree> exampleList = null;
	protected ArrayList<FuncMap> funcList = null;

	public abstract void init();

	protected void setup() {
		this.funcList = null;
		this.exampleList = null;
	}

	public abstract void emitTopLevel(Env env, Code code);

	public boolean isExecutable() {
		return false;
	}

	protected abstract Object wrapUp();

	public String getUniqueConstName(String name) {
		return name;
	}

	public abstract CodeMap newConstMap(Env env, String lname, Ty ret);

	public abstract void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr);

	HashMap<String, Integer> arrowMap = new HashMap<>();

	public final String safeName(String name) {
		if (name.indexOf("->") > 0) {
			Integer n = this.arrowMap.get(name);
			if (n == null) {
				n = this.arrowMap.size();
				this.arrowMap.put(name, n);
			}
			return "c0nv" + n;
		}
		return name;
	}

	public String getUniqueFuncName(String name, Ty... paramTypes) {
		return name;
	}

	public abstract CodeMap newCodeMap(Env env, String sname, String lname, Ty returnType, Ty... paramTypes);

	public abstract void defineFunction(Env env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code);

	public void addFunction(String name, FuncMap f) {
		if (f.isPublic) {
			if (this.funcList == null) {
				this.funcList = new ArrayList<>(0);
			}
			this.funcList.add(f);
		}
	}

	public void addExample(String name, ParseTree t) {
		if (this.exampleList == null) {
			this.exampleList = new ArrayList<>(0);
		}
		this.exampleList.add(t);
	}

	public Code emitHeader(Env env, Code code) {
		if (this.funcList != null) {
			for (FuncMap f : this.funcList) {
				if (f.isExpired()) {
					continue;
				}
				f.generate(env);
			}
			this.funcList = null;
		}
		if (!code.isGenerative() && this.exampleList != null) {
			ArrayList<Code> asserts = new ArrayList<>();
			for (ParseTree t : this.exampleList) {
				Code body = env.parseCode(env, t).asType(env, Ty.tBool);
				asserts.add(new ExprCode("assert", body));
			}
			code = new MultiCode(asserts.toArray(new Code[asserts.size()])).asType(env, Ty.tVoid);
			this.exampleList = null;
		}
		return code;
	}

}
