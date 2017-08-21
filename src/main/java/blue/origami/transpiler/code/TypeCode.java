package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public class TypeCode extends CommonCode {
	private Ty value;

	public TypeCode(Ty value) {
		super(Ty.tVoid);
		this.value = value;
	}

	public Ty getTypeValue() {
		return this.value;
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
