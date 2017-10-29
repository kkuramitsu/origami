package blue.origami.transpiler.type;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.CastCode;

public class OptionTy extends SimpleTy {

	public OptionTy() {
		super("Option", 1);
	}

	@Override
	public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		if (toTy.isGeneric(Ty.tOption)) {
			if (fromTy.getParamType().isAny() || toTy.getParamType().isAny()) {
				return CastCode.BESTCAST;
			}
		}
		return CastCode.STUPID;
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		if (toTy.isGeneric(Ty.tOption)) {
			if (fromTy.getParamType().isAny() || toTy.getParamType().isAny()) {
				return new CodeMap(CastCode.BESTCAST, "anycast", "%s", this, toTy);
			}
		}
		return null;
	}

}