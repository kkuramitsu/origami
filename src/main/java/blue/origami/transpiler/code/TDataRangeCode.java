package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.util.StringCombinator;

public class TDataRangeCode extends TDataArrayCode {

	public TDataRangeCode(TCode start, TCode end) {
		super(false, start, end);
	}

	@Override
	public TCode asType(TEnv env, Ty t) {
		if (this.isUntyped()) {
			this.args[0] = this.args[0].asType(env, Ty.tInt);
			this.args[1] = this.args[1].asType(env, Ty.tInt);
			this.setType(Ty.tImArray(Ty.tInt));
		}
		return super.asType(env, t);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.isMutable() ? "(" : "[");
		StringCombinator.append(sb, this.args[0]);
		sb.append(" to ");
		StringCombinator.append(sb, this.args[2]);
		sb.append(this.isMutable() ? ")" : "]");
	}

}