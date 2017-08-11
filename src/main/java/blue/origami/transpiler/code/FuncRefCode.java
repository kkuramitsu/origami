package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;

public final class FuncRefCode extends CommonCode {
	String name;
	Template template;

	public FuncRefCode(String name, Template tp) {
		super(Ty.tFunc(tp.getReturnType(), tp.getParamTypes()));
		this.name = name;
		this.template = tp;
	}

	public Template getRef() {
		return this.template;
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushFuncRef(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

	@Override
	public Code applyCode(TEnv env, Code... params) {
		return new ExprCode(this.name, params);
	}

}