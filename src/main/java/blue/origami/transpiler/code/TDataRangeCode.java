package blue.origami.transpiler.code;

import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;

public class TDataRangeCode extends TDataArrayCode {

	public TDataRangeCode(TCode start, TCode end) {
		super(start, end);
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		if (this.isUntyped()) {
			this.args[0] = this.args[0].asType(env, TType.tInt);
			this.args[1] = this.args[1].asType(env, TType.tInt);
			this.setType(TType.tArray(TType.tInt));
		}
		return super.asType(env, t);
	}

}