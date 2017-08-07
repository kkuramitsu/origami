package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.Template;

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
	public Template getTemplate(TEnv env) {
		return env.getTemplate("funcref", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.name);
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