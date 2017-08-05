package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;

public class TSugarCode extends CommonCode {

	public TSugarCode() {
		super(TType.tUntyped);
	}

	protected TCode[] makeArgs(TCode... args) {
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

	@Override
	public void strOut(StringBuilder sb) {
		ODebug.TODO();
	}

}