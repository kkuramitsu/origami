package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class TTypeCode extends CommonCode {
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
		return env.getTemplate(this.value.toString(), "%s");
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		ODebug.TODO(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		StringCombinator.append(sb, this.value);
	}

}
