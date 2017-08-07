package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.Template;

public final class TFuncRefCode extends CommonCode {
	String name;
	Template template;

	public TFuncRefCode(String name, Template tp) {
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
	public TCode applyCode(TEnv env, TCode... params) {
		return new TExprCode(this.name, params);
	}

}