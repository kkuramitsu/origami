package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public final class TFuncRefCode extends TypedCode0 {
	String name;
	Template template;

	public TFuncRefCode(String name, Template tp) {
		super(TType.tFunc(tp.getReturnType(), tp.getParamTypes()));
		this.name = name;
		this.template = tp;
	}

	public Template getRef() {
		return this.template;
	}

	@Override
	public TCode asType(TEnv env, TType ret) {
		return super.asType(env, ret);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("funcref", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.template.getName());
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushFuncRef(env, this);
	}

	@Override
	public TCode applyCode(TEnv env, TCode... params) {
		return new TExprCode(this.name, params);
	}

}