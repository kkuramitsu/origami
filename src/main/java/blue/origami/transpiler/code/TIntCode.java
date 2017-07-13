package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TTemplate;
import blue.origami.transpiler.TType;

public class TIntCode extends TTypedCode {
	private int value;

	public TIntCode(int value) {
		super(TType.tInt);
		this.value = value;
	}

	@Override
	public TTemplate getTemplate(TEnv env) {
		return env.get("0:Int", TTemplate.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.push(this.strOut(env));
	}

}