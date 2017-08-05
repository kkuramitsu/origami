package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;

public class TIntCode extends CommonCode implements TValueCode {
	private int value;

	public TIntCode(int value) {
		super(TType.tInt);
		this.value = value;
	}

	@Override
	public Object getValue() {
		return this.value;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("0:Int", "%d");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushInt(env, this);
	}

	public static class TCharCode extends TIntCode {

		public TCharCode(int value) {
			super(value);
			this.setType(TType.tChar);
		}

	}
}
