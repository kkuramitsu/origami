package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TSugarCode extends TypedCode0 {

	public TSugarCode() {
		super(TType.tUntyped);
	}

	protected TCode[] args(TCode... args) {
		return args;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return this.asType(env, TType.tUntyped).getTemplate(env);
	}

	@Override
	public String strOut(TEnv env) {
		return this.asType(env, TType.tUntyped).strOut(env);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		this.asType(env, TType.tUntyped).emitCode(env, sec);

	}

}