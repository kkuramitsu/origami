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
			if (fromTy.getParamType() == Ty.tAnyRef || toTy.getParamType() == Ty.tAnyRef) {
				return CastCode.BESTCAST;
			}
		}
		return CastCode.STUPID;
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		if (toTy.isGeneric(Ty.tOption)) {
			if (fromTy.getParamType() == Ty.tAnyRef || toTy.getParamType() == Ty.tAnyRef) {
				return new CodeMap(CastCode.BESTCAST, "anycast", "%s", this, toTy);
			}
		}
		return null;
	}

}