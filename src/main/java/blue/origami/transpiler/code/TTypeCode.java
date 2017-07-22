package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.TType;
import blue.origami.util.ODebug;

public class TTypeCode extends EmptyTypedCode {
	private TType value;

	public TTypeCode(TType value) {
		super(TType.tVoid);
		this.value = value;
	}

	public TType getTypeValue() {
		return this.value;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.get(this.value.toString(), Template.class);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.value);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		ODebug.TODO(this);
	}

}
