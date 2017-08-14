package blue.origami.transpiler;

import java.util.ArrayList;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.MultiCode;

public abstract class Generator {
	protected boolean isVerbose = false;

	public void setVerbose(boolean debug) {
		this.isVerbose = debug;
	}

	public boolean isVerbose() {
		return this.isVerbose;
	}

	protected abstract void setup();

	protected abstract Object wrapUp();

	public abstract void emit(TEnv env, Code code);

	public abstract CodeTemplate newConstTemplate(TEnv env, String lname, Ty ret);

	public abstract void defineConst(Transpiler env, boolean isPublic, String name, Ty type, Code expr);

	public abstract CodeTemplate newFuncTemplate(TEnv env, String lname, Ty returnType, Ty... paramTypes);

	public abstract void defineFunction(TEnv env, boolean isPublic, String name, String[] paramNames, Ty[] paramTypes,
			Ty returnType, Code code);

	protected ArrayList<TFunction> funcList = null;

	public void addFunction(String name, TFunction f) {
		if (f.isPublic) {
			if (this.funcList == null) {
				this.funcList = new ArrayList<>(0);
			}
			this.funcList.add(f);
		}
	}

	protected ArrayList<Tree<?>> exampleList = null;

	public void addExample(String name, Tree<?> t) {
		if (this.exampleList == null) {
			this.exampleList = new ArrayList<>(0);
		}
		this.exampleList.add(t);
	}

	public Code emitHeader(TEnv env, Code code) {
		if (this.funcList != null) {
			for (TFunction f : this.funcList) {
				if (f.isExpired()) {
					continue;
				}
				f.generate(env);
			}
			this.funcList = null;
		}
		if (code.isEmpty() && this.exampleList != null) {
			ArrayList<Code> asserts = new ArrayList<>();
			for (Tree<?> t : this.exampleList) {
				Code body = env.parseCode(env, t).asType(env, Ty.tBool);
				asserts.add(new ExprCode("assert", body));
			}
			code = new MultiCode(asserts.toArray(new Code[asserts.size()])).asType(env, Ty.tVoid);
			this.exampleList = null;
		}
		return code;
	}

}
