package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TSourceCode extends TArgCode {

	public TSourceCode(TCode... args) {
		super(TType.tUnit, TTemplate.Null, args);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		for (TCode a : this.args) {
			a.emitCode(env, sec);
		}
	}
}